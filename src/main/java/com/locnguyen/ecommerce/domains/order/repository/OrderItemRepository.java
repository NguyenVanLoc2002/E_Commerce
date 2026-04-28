package com.locnguyen.ecommerce.domains.order.repository;

import com.locnguyen.ecommerce.domains.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.UUID;
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
