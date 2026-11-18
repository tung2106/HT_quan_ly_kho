package com.vatlieu.service;

import com.vatlieu.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@WarehouseAccess
public class PurchaseTrashService {

  private final JdbcTemplate jdbc;
  private final BatchRepository batches;
  private final CurrentUser current;

  @Transactional
  public void setTrashed(long id, boolean trash) {
    // Cùng khóa phiếu với thanh toán, sau đó khóa sản phẩm theo thứ tự của luồng xuất.
    var states = jdbc.queryForList(
      "SELECT trashed FROM purchase_order WHERE id=? FOR UPDATE",
      Boolean.class,
      id
    );
    if (states.isEmpty()) throw new IllegalArgumentException("Không tìm thấy phiếu nhập.");
    if (states.get(0) == trash) return;
    if (
      Boolean.TRUE.equals(
        jdbc.queryForObject(
          """
          SELECT paid_amount <> 0 OR EXISTS (SELECT 1 FROM cash_payment WHERE purchase_id=?)
          FROM purchase_order WHERE id=?
          """,
          Boolean.class,
          id,
          id
        )
      )
    ) {
      throw new IllegalArgumentException(
        "Phiếu đã trả tiền. Hãy giữ phiếu để đối chiếu lịch sử thanh toán."
      );
    }
    var productIds = jdbc.queryForList(
      """
      SELECT id FROM product WHERE id IN (SELECT product_id FROM purchase_item WHERE order_id=?)
      ORDER BY id FOR UPDATE
      """,
      Long.class,
      id
    );
    if (
      Boolean.TRUE.equals(
        jdbc.queryForObject(
          """
          SELECT EXISTS (SELECT 1 FROM stock_transaction t
            JOIN batch b ON b.id=t.batch_id JOIN purchase_item i ON i.id=b.purchase_item_id
            WHERE i.order_id=? AND t.tx_type IN ('SALE','WASTE'))
          """,
          Boolean.class,
          id
        )
      )
    ) {
      throw new IllegalArgumentException(
        "Lô trên phiếu đã xuất bán hoặc ghi hao hụt. Không thể chuyển phiếu vào Thùng rác."
      );
    }
    if (
      productIds.isEmpty() ||
      Boolean.TRUE.equals(
        jdbc.queryForObject(
          """
          SELECT EXISTS (SELECT 1 FROM purchase_item i LEFT JOIN batch b ON b.purchase_item_id=i.id
            WHERE i.order_id=? GROUP BY i.id
            HAVING COUNT(b.id) = 0 OR SUM(b.qty_in) <> i.qty
              OR BOOL_OR(b.product_id <> i.product_id OR b.qty_left <> CASE WHEN ? THEN b.qty_in ELSE 0 END))
          """,
          Boolean.class,
          id,
          trash
        )
      )
    ) {
      throw new IllegalArgumentException(
        "Tồn lô chưa khớp với phiếu nhập. Cần đối chiếu trước khi tiếp tục."
      );
    }
    // Giữ các bút toán gốc, ghi bút toán đối ứng cho từng lô với đúng giá vốn nhập.
    jdbc.update(
      """
      WITH changed AS (
        UPDATE batch b SET qty_left=CASE WHEN ? THEN 0 ELSE b.qty_in END
        FROM purchase_item i WHERE i.id=b.purchase_item_id AND i.order_id=?
        RETURNING b.id,b.product_id,b.qty_in,b.unit_cost
      )
      INSERT INTO stock_transaction(batch_id,product_id,qty_change,unit_cost,tx_type,ref_id,created_by,note)
      SELECT id,product_id,CASE WHEN ? THEN -qty_in ELSE qty_in END,unit_cost,'ADJUSTMENT',?,?,? FROM changed
      """,
      trash,
      id,
      trash,
      id,
      current.id(),
      trash ? "Chuyển phiếu nhập vào Thùng rác" : "Khôi phục phiếu nhập"
    );
    productIds.forEach(batches::recalculateAverageCost);
    jdbc.update("UPDATE purchase_order SET trashed=? WHERE id=?", trash, id);
  }
}
