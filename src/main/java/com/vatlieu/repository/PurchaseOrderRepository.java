package com.vatlieu.repository;

import com.vatlieu.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByOrderDateDescIdDesc();
}
