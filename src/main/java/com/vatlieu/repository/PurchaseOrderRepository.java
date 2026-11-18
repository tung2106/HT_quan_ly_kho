package com.vatlieu.repository;

import com.vatlieu.entity.PurchaseOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
  List<PurchaseOrder> findByTrashedOrderByOrderDateDescIdDesc(boolean trashed);
}
