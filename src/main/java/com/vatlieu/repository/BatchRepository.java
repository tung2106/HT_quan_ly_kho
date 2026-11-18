package com.vatlieu.repository;

import com.vatlieu.entity.Batch;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchRepository extends JpaRepository<Batch, Long> {
  @Query("select sum(b.qtyLeft * b.unitCost) from Batch b where b.product.id = :id")
  BigDecimal remainingValue(@Param("id") Long id);

  @Modifying
  @Query(
    value = """
    UPDATE product SET avg_cost=(SELECT COALESCE(ROUND(SUM(qty_left*unit_cost)/NULLIF(SUM(qty_left),0),2),0)
    FROM batch WHERE product_id=:id) WHERE id=:id
    """,
    nativeQuery = true
  )
  void recalculateAverageCost(@Param("id") long id);
}
