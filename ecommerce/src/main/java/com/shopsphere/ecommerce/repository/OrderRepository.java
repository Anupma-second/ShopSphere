package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findByStatusAndOrderDateBefore(
            OrderStatus status, LocalDateTime before);

    List<Order> findAllByOrderByOrderDateDesc();
}
