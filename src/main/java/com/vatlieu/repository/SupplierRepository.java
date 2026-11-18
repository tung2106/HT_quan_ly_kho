package com.vatlieu.repository;

import com.vatlieu.entity.Supplier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
  List<Supplier> findAllByOrderByNameAsc();
  List<Supplier> findByActiveTrueOrderByNameAsc();
  List<Supplier> findByActiveFalseOrderByNameAsc();
}
