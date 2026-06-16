package com.tappy.pos.repository.product;

import com.tappy.pos.model.entity.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByDeletedFalseOrderByName();
    List<Category> findByParentIdAndDeletedFalse(Long parentId);
    Optional<Category> findByIdAndDeletedFalse(Long id);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.deleted = false")
    List<Category> findAllActiveWithParent();

    /**
     * Single-pass stats query for the category list screen.
     * Returns one row per non-deleted category: [categoryId, productCount, outOfStockCount, revenueThisMonth].
     *
     * - productCount      : distinct non-deleted products linked to the category.
     * - outOfStockCount   : subset of those products whose total inventory quantity ≤ 0.
     * - revenueThisMonth  : sum of order_items.amount for COMPLETED SELL orders in [fromDt, toDt).
     *
     * The inventory sub-query aggregates all batches per product so a product with multiple
     * warehouse batches is counted out-of-stock only when the combined quantity hits zero.
     *
     * The order sub-query is pre-aggregated per product before the join so the revenue rows
     * do not get multiplied by the product-category or inventory joins.
     */
    @Query(value = """
            SELECT
                c.id                                                                   AS cat_id,
                COUNT(DISTINCT p.id)                                                   AS product_count,
                COUNT(DISTINCT CASE WHEN COALESCE(inv.qty, 0) <= 0 THEN p.id END)     AS out_of_stock_count,
                COALESCE(SUM(oi_rev.revenue), 0)                                       AS revenue_this_month
            FROM category c
            LEFT JOIN product_category pc       ON pc.category_id = c.id
            LEFT JOIN product p                 ON p.id = pc.product_id  AND p.deleted = false
            LEFT JOIN (
                SELECT product_id, SUM(quantity_in_stock) AS qty
                FROM inventory
                GROUP BY product_id
            ) inv                               ON inv.product_id = p.id
            LEFT JOIN (
                SELECT oi.product_id, SUM(oi.amount) AS revenue
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.deleted = false
                  AND o.status  = 'COMPLETED'
                  AND o.order_type = 'SELL'
                  AND o.completed_at >= :fromDt
                  AND o.completed_at <  :toDt
                  AND o.tenant_id = current_setting('app.current_tenant', true)
                GROUP BY oi.product_id
            ) oi_rev                            ON oi_rev.product_id = p.id
            WHERE c.deleted = false
              AND c.tenant_id = current_setting('app.current_tenant', true)
            GROUP BY c.id
            """, nativeQuery = true)
    List<Object[]> findAllCategoryStats(
            @Param("fromDt") LocalDateTime fromDt,
            @Param("toDt")   LocalDateTime toDt);
}
