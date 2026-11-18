package com.vatlieu.repository;

import com.vatlieu.entity.Batch;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchRepository extends JpaRepository<Batch, Long> {
  @Query("select sum(b.qtyLeft * b.unitCost) from Batch b where b.product.id = :id")
  BigDecimal remainingValue(@Param("id") Long id);
}
