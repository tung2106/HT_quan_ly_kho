package com.vatlieu.repository;

import com.vatlieu.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findAllByOrderByNameAsc();
    List<Supplier> findByActiveTrueOrderByNameAsc();
    List<Supplier> findByActiveFalseOrderByNameAsc();
}
