package com.vatlieu.repository;

import com.vatlieu.entity.StockTransaction;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
  /** Tồn kho hiện tại (đơn vị gốc). Trả về null nếu chưa có giao dịch. */
  @Query("select sum(t.qtyChange) from StockTransaction t where t.product.id = :pid")
  BigDecimal sumQty(@Param("pid") Long productId);
}
