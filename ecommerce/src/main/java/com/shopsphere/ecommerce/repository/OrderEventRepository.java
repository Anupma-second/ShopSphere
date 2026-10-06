package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}
