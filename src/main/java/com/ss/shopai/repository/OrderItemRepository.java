package com.ss.shopai.repository;

import com.ss.shopai.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.user.id = :userId")
    List<OrderItem> findByUserId(@Param("userId") Long userId);

    @Query("SELECT oi.product.id, SUM(oi.quantity) as totalQty FROM OrderItem oi " +
            "WHERE oi.order.orderStatus <> 'CANCELLED' GROUP BY oi.product.id ORDER BY totalQty DESC")
    List<Object[]> findBestSellingProductIds(org.springframework.data.domain.Pageable pageable);

    boolean existsByOrder_User_IdAndProduct_Id(Long userId, Long productId);
}