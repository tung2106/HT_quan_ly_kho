package com.vatlieu.repository;

import com.vatlieu.entity.UnitConversion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {
  List<UnitConversion> findByProductIdOrderByUnitNameAsc(Long productId);
  Optional<UnitConversion> findByProductIdAndUnitNameIgnoreCase(Long productId, String unitName);
}
