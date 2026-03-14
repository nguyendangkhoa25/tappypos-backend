package com.knp.repository;

import com.knp.model.entity.BuybackOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BuybackOrderRepository extends JpaRepository<BuybackOrder, Long> {

    @Query("SELECT o FROM BuybackOrder o WHERE o.deleted = false ORDER BY o.createdAt DESC")
    Page<BuybackOrder> findAllActive(Pageable pageable);

    @Query("SELECT o FROM BuybackOrder o WHERE o.deleted = false AND o.status = :status ORDER BY o.createdAt DESC")
    Page<BuybackOrder> findByStatus(@Param("status") BuybackOrder.OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM BuybackOrder o WHERE o.deleted = false AND o.type = :type ORDER BY o.createdAt DESC")
    Page<BuybackOrder> findByType(@Param("type") BuybackOrder.OrderType type, Pageable pageable);

    @Query("SELECT o FROM BuybackOrder o WHERE o.deleted = false AND o.type = :type AND o.status = :status ORDER BY o.createdAt DESC")
    Page<BuybackOrder> findByTypeAndStatus(
            @Param("type") BuybackOrder.OrderType type,
            @Param("status") BuybackOrder.OrderStatus status,
            Pageable pageable);

    @Query("SELECT MAX(o.id) FROM BuybackOrder o")
    Long findMaxId();
}
