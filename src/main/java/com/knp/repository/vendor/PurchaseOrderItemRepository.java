package com.knp.repository.vendor;

import com.knp.model.entity.vendor.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    @Query("SELECT i FROM PurchaseOrderItem i WHERE i.purchaseOrder.id = :poId AND i.deleted = false")
    List<PurchaseOrderItem> findByPurchaseOrderId(@Param("poId") Long poId);
}
