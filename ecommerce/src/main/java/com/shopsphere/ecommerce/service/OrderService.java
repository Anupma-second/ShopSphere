package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.AddressResponse;
import com.shopsphere.ecommerce.dto.CheckoutRequest;
import com.shopsphere.ecommerce.dto.OrderItemResponse;
import com.shopsphere.ecommerce.dto.OrderResponse;
import com.shopsphere.ecommerce.entity.*;
import com.shopsphere.ecommerce.exception.AddressNotFoundException;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.EmptyCartException;
import com.shopsphere.ecommerce.exception.OrderCancellationException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    // Statuses an ADMIN may move an order to (PENDING -> CONFIRMED only
    // happens through a verified payment, never by hand).
    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(
            OrderRepository orderRepository,
            AddressRepository addressRepository,
            CartItemRepository cartItemRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            PaymentRepository paymentRepository) {

        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
    }

    // ---------- Mapping (one place, used everywhere) ----------

    private OrderResponse mapToResponse(Order order) {

        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice()))
                .toList();

        Address a = order.getAddress();

        AddressResponse address = (a == null) ? null : new AddressResponse(
                a.getFullName(), a.getPhone(), a.getAddressLine(),
                a.getCity(), a.getState(), a.getPostalCode(), a.getCountry());

        return new OrderResponse(
                order.getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getOrderDate(),
                items,
                address);
    }

    // ---------- Customer reads ----------

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(User user) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(Long id, User user) {
        return mapToResponse(findOwnedOrder(id, user));
    }

    // ---------- Admin reads ----------

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderAdmin(Long id) {
        return mapToResponse(orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found")));
    }

    // ---------- Checkout ----------

    /**
     * One transaction: if anything fails (e.g. stock runs out on the 3rd
     * item) the stock already reserved for the 1st and 2nd item is rolled
     * back and the cart is left untouched.
     */
    @Transactional
    public OrderResponse checkout(CheckoutRequest request, User user) {

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());

        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        if (request.getAddressId() == null) {
            throw new BadRequestException("addressId is required");
        }

        // FIX: the address must belong to the logged-in user
        Address address = addressRepository
                .findByIdAndUserIdAndDeletedFalse(request.getAddressId(), user.getId())
                .orElseThrow(() -> new AddressNotFoundException(
                        "Address not found with id: " + request.getAddressId()));

        double totalAmount = 0;

        for (CartItem cartItem : cartItems) {
            totalAmount += cartItem.getProduct().getPrice() * cartItem.getQuantity();
        }

        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {

            Product product = cartItem.getProduct();

            // FIX: atomic reserve - fails cleanly instead of overselling
            int reserved = productRepository.decrementStock(
                    product.getId(), cartItem.getQuantity());

            if (reserved == 0) {
                throw new BadRequestException(
                        "Not enough stock for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            savedOrder.getItems().add(orderItemRepository.save(orderItem));
        }

        cartItemRepository.deleteAll(cartItems);

        return mapToResponse(savedOrder);
    }

    // ---------- Cancel ----------

    @Transactional
    public void cancelOrder(Long orderId, User user) {

        Order order = findOwnedOrder(orderId, user);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderCancellationException(
                    "Only unpaid orders can be cancelled here. "
                            + "Please contact support for paid orders.");
        }

        cancelAndRestock(order);
    }

    // ---------- Admin status change ----------

    @Transactional
    public OrderResponse updateStatusAsAdmin(Long id, OrderStatus newStatus) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Set<OrderStatus> allowed = ADMIN_TRANSITIONS.get(order.getStatus());

        if (allowed == null || !allowed.contains(newStatus)) {
            throw new BadRequestException(
                    "Cannot change order from " + order.getStatus()
                            + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            cancelAndRestock(order);
        } else {
            order.setStatus(newStatus);
            orderRepository.save(order);
        }

        return mapToResponse(order);
    }

    // ---------- Housekeeping (called by OrderCleanupScheduler) ----------

    /** Cancels unpaid orders older than the timeout and returns their stock. */
    @Transactional
    public int cancelExpiredPendingOrders(int timeoutMinutes) {

        List<Order> stale = orderRepository.findByStatusAndOrderDateBefore(
                OrderStatus.PENDING,
                LocalDateTime.now().minusMinutes(timeoutMinutes));

        stale.forEach(this::cancelAndRestock);

        return stale.size();
    }

    // ---------- Internals ----------

    private Order findOwnedOrder(Long id, User user) {
        // not found and "someone else's" look identical on purpose
        return orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    /** Single place that cancels an order, so stock is never forgotten. */
    private void cancelAndRestock(Order order) {

        for (OrderItem item : order.getItems()) {
            productRepository.incrementStock(
                    item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if ("PAID".equals(payment.getStatus())) {
                // money was taken - flagged until refunds are automated
                payment.setStatus("REFUND_REQUIRED");
            } else {
                payment.setStatus("CANCELLED");
            }
            paymentRepository.save(payment);
        });
    }
}
