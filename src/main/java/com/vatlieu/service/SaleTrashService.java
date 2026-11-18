package com.vatlieu.service;

import com.vatlieu.repository.BatchRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleTrashService {

  private final JdbcTemplate jdbc;
  private final BatchRepository batches;
  private final CurrentUser current;

  private record ProductQty(long id, BigDecimal qty) {}

  private record SoldLot(
    long batch,
    long product,
    BigDecimal qty,
    BigDecimal cost,
    BigDecimal left,
    BigDecimal received
  ) {}

  @Transactional
  @PreAuthorize("hasRole('OWNER')")
  public void setTrashed(long id, boolean trash) {
    // Khóa cùng phiếu với luồng thu tiền, rồi khóa sản phẩm theo thứ tự của luồng xuất.
    var state = jdbc.queryForList(
      "SELECT trashed, paid_amount FROM outbound_order WHERE id=? AND kind='SALE' FOR UPDATE",
      id
    );
    if (state.isEmpty()) throw new IllegalArgumentException("Không tìm thấy phiếu xuất bán.");
    if (Boolean.valueOf(trash).equals(state.get(0).get("trashed"))) return;
    if (
      ((BigDecimal) state.get(0).get("paid_amount")).signum() != 0 ||
      Boolean.TRUE.equals(
        jdbc.queryForObject(
          "SELECT EXISTS(SELECT 1 FROM cash_payment WHERE outbound_id=?)",
          Boolean.class,
          id
        )
      )
    ) {
      throw new IllegalArgumentException("Phiếu đã thu tiền khách nên không thể xóa.");
    }
    var products = jdbc.query(
      """
      SELECT p.id,i.qty FROM product p JOIN (
        SELECT product_id,SUM(qty) AS qty FROM outbound_item WHERE order_id=? GROUP BY product_id
      ) i ON i.product_id=p.id ORDER BY p.id FOR UPDATE OF p
      """,
      (rs, n) -> new ProductQty(rs.getLong("id"), rs.getBigDecimal("qty")),
      id
    );
    // Chỉ dùng các dòng SALE ban đầu. Cùng một lô có thể xuất trên nhiều dòng của một phiếu.
    var lots = jdbc.query(
      """
      SELECT b.id,b.product_id,-SUM(t.qty_change) AS qty,b.unit_cost,b.qty_left,b.qty_in
      FROM stock_transaction t JOIN batch b ON b.id=t.batch_id AND b.product_id=t.product_id AND b.unit_cost=t.unit_cost
      WHERE t.ref_id=? AND t.tx_type='SALE'
      GROUP BY b.id ORDER BY b.product_id,b.id
      """,
      (rs, n) ->
        new SoldLot(
          rs.getLong("id"),
          rs.getLong("product_id"),
          rs.getBigDecimal("qty"),
          rs.getBigDecimal("unit_cost"),
          rs.getBigDecimal("qty_left"),
          rs.getBigDecimal("qty_in")
        ),
      id
    );
    var allocated = new HashMap<Long, BigDecimal>();
    for (var lot : lots) {
      allocated.merge(lot.product(), lot.qty(), BigDecimal::add);
      if (
        lot.qty().signum() <= 0 ||
        (trash && lot.left().add(lot.qty()).compareTo(lot.received()) > 0)
      ) {
        throw new IllegalArgumentException(
          "Số lượng lô chưa khớp với phiếu bán. Cần đối chiếu trước khi tiếp tục."
        );
      }
      if (!trash && lot.left().compareTo(lot.qty()) < 0) throw new IllegalArgumentException(
        "Lô #" + lot.batch() + " không còn đủ hàng để khôi phục. Phiếu vẫn ở Thùng rác."
      );
    }
    if (
      products.isEmpty() ||
      allocated.size() != products.size() ||
      products
        .stream()
        .anyMatch(p -> p.qty().compareTo(allocated.getOrDefault(p.id(), BigDecimal.ZERO)) != 0)
    ) {
      throw new IllegalArgumentException(
        "Chi tiết lô không khớp với phiếu bán. Cần đối chiếu trước khi tiếp tục."
      );
    }
    long actor = current.id();
    for (var lot : lots) {
      BigDecimal delta = trash ? lot.qty() : lot.qty().negate();
      jdbc.update("UPDATE batch SET qty_left=qty_left+? WHERE id=?", delta, lot.batch());
      jdbc.update(
        """
        INSERT INTO stock_transaction(batch_id,product_id,qty_change,unit_cost,tx_type,ref_id,created_by,note)
        VALUES (?,?,?,?,'ADJUSTMENT',?,?,?)
        """,
        lot.batch(),
        lot.product(),
        delta,
        lot.cost(),
        id,
        actor,
        trash ? "Chuyển phiếu bán vào Thùng rác" : "Khôi phục phiếu bán"
      );
    }
    products.forEach(p -> batches.recalculateAverageCost(p.id()));
    jdbc.update("UPDATE outbound_order SET trashed=? WHERE id=?", trash, id);
  }
}
