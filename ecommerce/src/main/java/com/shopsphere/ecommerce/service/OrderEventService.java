package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.OrderEvent;
import com.shopsphere.ecommerce.repository.OrderEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Writes/reads the order timeline. Joins the caller's transaction. */
@Service
public class OrderEventService {

    private final OrderEventRepository repository;

    public OrderEventService(OrderEventRepository repository) {
        this.repository = repository;
    }

    public void record(Order order, String type, String message) {
        repository.save(new OrderEvent(order, type, message));
    }

    public List<OrderEvent> timeline(Long orderId) {
        return repository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId);
    }
}
