package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.OrderItem;
import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.repository.OrderItemRepository;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderItemService(
            OrderItemRepository orderItemRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository) {

        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public OrderItem createOrderItem(OrderItem orderItem) {

        Long orderId = orderItem.getOrder().getId();
        Long productId = orderItem.getProduct().getId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order not found with id: " + orderId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + productId));

        orderItem.setOrder(order);
        orderItem.setProduct(product);

        return orderItemRepository.save(orderItem);
    }

    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }

    public Optional<OrderItem> getOrderItemById(Long id) {
        return orderItemRepository.findById(id);
    }

    public OrderItem updateOrderItem(
            Long id,
            OrderItem updatedOrderItem) {

        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Order item not found with id: " + id));

        orderItem.setQuantity(updatedOrderItem.getQuantity());
        orderItem.setPrice(updatedOrderItem.getPrice());

        return orderItemRepository.save(orderItem);
    }

    public void deleteOrderItem(Long id) {
        orderItemRepository.deleteById(id);
    }
}