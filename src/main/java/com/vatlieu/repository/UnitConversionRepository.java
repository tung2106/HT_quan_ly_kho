package com.vatlieu.repository;

import com.vatlieu.entity.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {
    List<UnitConversion> findByProductIdOrderByUnitNameAsc(Long productId);
    Optional<UnitConversion> findByProductIdAndUnitNameIgnoreCase(Long productId, String unitName);
}
