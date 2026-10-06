package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.AddressResponse;
import com.shopsphere.ecommerce.dto.CheckoutRequest;
import com.shopsphere.ecommerce.dto.CouponQuote;
import com.shopsphere.ecommerce.dto.OrderEventResponse;
import com.shopsphere.ecommerce.dto.OrderItemResponse;
import com.shopsphere.ecommerce.dto.OrderResponse;
import com.shopsphere.ecommerce.dto.PaymentInfoResponse;
import com.shopsphere.ecommerce.entity.*;
import com.shopsphere.ecommerce.exception.AddressNotFoundException;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.EmptyCartException;
import com.shopsphere.ecommerce.exception.OrderCancellationException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.*;
import com.shopsphere.ecommerce.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final OrderEventService eventService;
    private final CouponService couponService;

    public OrderService(
            OrderRepository orderRepository,
            AddressRepository addressRepository,
            CartItemRepository cartItemRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            PaymentRepository paymentRepository,
            OrderEventService eventService,
            CouponService couponService) {

        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.eventService = eventService;
        this.couponService = couponService;
    }

    // ---------- Mapping (one place, used everywhere) ----------

    private OrderResponse mapToResponse(Order order, boolean withTimeline) {

        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice()))
                .toList();

        OrderResponse response = new OrderResponse(
                order.getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getOrderDate(),
                items,
                addressOf(order));

        paymentRepository.findByOrderId(order.getId())
                .ifPresent(p -> response.setPayment(PaymentInfoResponse.from(p)));

        response.setCarrier(order.getCarrier());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setShippedAt(order.getShippedAt());
        response.setDeliveredAt(order.getDeliveredAt());
        response.setCancelledAt(order.getCancelledAt());

        // older orders have no subtotal stored: it equals the total
        response.setSubtotalAmount(order.getSubtotalAmount() != null
                ? order.getSubtotalAmount() : order.getTotalAmount());
        response.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : 0);
        response.setCouponCode(order.getCouponCode());

        response.setCanPay(order.getStatus() == OrderStatus.PENDING);
        response.setCanCancel(order.getStatus() == OrderStatus.PENDING
                || order.getStatus() == OrderStatus.CONFIRMED);

        if (withTimeline) {
            response.setTimeline(buildTimeline(order));
        }

        return response;
    }

    // Uses the address copied at checkout; falls back to the linked address
    // for orders placed before snapshots existed.
    // package-private: SellerService shows the same address
    static AddressResponse addressOf(Order order) {

        if (order.getShipFullName() != null) {
            return new AddressResponse(
                    order.getShipFullName(), order.getShipPhone(),
                    order.getShipAddressLine(), order.getShipCity(),
                    order.getShipState(), order.getShipPostalCode(),
                    order.getShipCountry());
        }

        Address a = order.getAddress();

        return (a == null) ? null : new AddressResponse(
                a.getFullName(), a.getPhone(), a.getAddressLine(),
                a.getCity(), a.getState(), a.getPostalCode(), a.getCountry());
    }

    private List<OrderEventResponse> buildTimeline(Order order) {

        List<OrderEventResponse> timeline = new ArrayList<>(
                eventService.timeline(order.getId()).stream()
                        .map(e -> new OrderEventResponse(
                                e.getType(), e.getMessage(), e.getCreatedAt()))
                        .toList());

        // old orders have no events yet - show at least when they were placed
        if (timeline.isEmpty()) {
            timeline.add(new OrderEventResponse(
                    OrderEvent.ORDER_PLACED, "Order placed", order.getOrderDate()));
        }

        return timeline;
    }

    // ---------- Customer reads ----------

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(User user) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(user.getId())
                .stream()
                .map(o -> mapToResponse(o, false))   // list stays light
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(Long id, User user) {
        return mapToResponse(findOwnedOrder(id, user), true);
    }

    // ---------- Admin reads ----------

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc()
                .stream()
                .map(o -> mapToResponse(o, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderAdmin(Long id) {
        return mapToResponse(orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found")), true);
    }

    // ---------- Checkout ----------

    @Transactional
    public OrderResponse checkout(CheckoutRequest request, User user) {

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());

        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        if (request.getAddressId() == null) {
            throw new BadRequestException("addressId is required");
        }

        Address address = addressRepository
                .findByIdAndUserIdAndDeletedFalse(request.getAddressId(), user.getId())
                .orElseThrow(() -> new AddressNotFoundException(
                        "Address not found with id: " + request.getAddressId()));

        double subtotal = cartSubtotal(cartItems);
        double totalAmount = subtotal;

        Order order = new Order();
        order.setUser(user);
        order.snapshotAddress(address);          // NEW: copy, not just a link

        // Coupon: checked and one use taken here. If anything below fails,
        // the whole transaction (including that use) is rolled back.
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponService.Applied applied = couponService.redeem(request.getCouponCode(), subtotal, user);
            totalAmount = Math.round((subtotal - applied.discount()) * 100) / 100.0;
            order.setSubtotalAmount(subtotal);
            order.setDiscountAmount(applied.discount());
            order.setCouponCode(applied.coupon().getCode());
        }

        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {

            Product product = cartItem.getProduct();

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

        eventService.record(savedOrder, OrderEvent.ORDER_PLACED,
                "Order placed for " + Money.inr(totalAmount)
                        + (savedOrder.getCouponCode() != null
                        ? " (coupon " + savedOrder.getCouponCode() + " saved "
                        + Money.inr(savedOrder.getDiscountAmount()) + ")"
                        : "")
                        + ". Waiting for payment.");

        return mapToResponse(savedOrder, true);
    }

    /** "Apply" button at checkout: what would this coupon do to my cart right now? */
    @Transactional(readOnly = true)
    public CouponQuote previewCoupon(String code, User user) {

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());

        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        return couponService.quote(code, cartSubtotal(cartItems), user);
    }

    private static double cartSubtotal(List<CartItem> cartItems) {
        double subtotal = 0;
        for (CartItem cartItem : cartItems) {
            subtotal += cartItem.getProduct().getPrice() * cartItem.getQuantity();
        }
        return Math.round(subtotal * 100) / 100.0;
    }

    // ---------- Cancel (customer) ----------

    /**
     * Allowed while the order is PENDING (unpaid) or CONFIRMED (paid, not
     * shipped yet). For a paid order the payment is flagged REFUND_REQUIRED;
     * the controller then triggers the actual Razorpay refund.
     */
    @Transactional
    public void cancelOrder(Long orderId, User user) {

        Order order = findOwnedOrder(orderId, user);

        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderCancellationException(
                    order.getStatus() == OrderStatus.CANCELLED
                            ? "This order is already cancelled."
                            : "This order has already been shipped and can no longer be cancelled.");
        }

        cancelAndRestock(order, "Cancelled by customer");
    }

    // ---------- Admin status change ----------

    @Transactional
    public OrderResponse updateStatusAsAdmin(
            Long id, OrderStatus newStatus, String carrier, String trackingNumber) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Set<OrderStatus> allowed = ADMIN_TRANSITIONS.get(order.getStatus());

        if (allowed == null || !allowed.contains(newStatus)) {
            throw new BadRequestException(
                    "Cannot change order from " + order.getStatus()
                            + " to " + newStatus);
        }

        switch (newStatus) {

            case SHIPPED -> {
                order.setStatus(OrderStatus.SHIPPED);
                order.setShippedAt(LocalDateTime.now());
                if (carrier != null && !carrier.isBlank()) {
                    order.setCarrier(carrier.trim());
                }
                if (trackingNumber != null && !trackingNumber.isBlank()) {
                    order.setTrackingNumber(trackingNumber.trim());
                }
                orderRepository.save(order);

                StringBuilder msg = new StringBuilder("Your order has been shipped");
                if (order.getCarrier() != null) {
                    msg.append(" via ").append(order.getCarrier());
                }
                msg.append(".");
                if (order.getTrackingNumber() != null) {
                    msg.append(" Tracking number: ").append(order.getTrackingNumber());
                }
                eventService.record(order, OrderEvent.ORDER_SHIPPED, msg.toString());
            }

            case DELIVERED -> {
                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveredAt(LocalDateTime.now());
                orderRepository.save(order);
                eventService.record(order, OrderEvent.ORDER_DELIVERED,
                        "Your order has been delivered.");
            }

            case CANCELLED -> cancelAndRestock(order, "Cancelled by store");

            default -> throw new BadRequestException(
                    "Status " + newStatus + " cannot be set manually");
        }

        return mapToResponse(order, true);
    }

    // ---------- Housekeeping (called by OrderCleanupScheduler) ----------

    /** Cancels unpaid orders older than the timeout and returns their stock. */
    @Transactional
    public int cancelExpiredPendingOrders(int timeoutMinutes) {

        List<Order> stale = orderRepository.findByStatusAndOrderDateBefore(
                OrderStatus.PENDING,
                LocalDateTime.now().minusMinutes(timeoutMinutes));

        stale.forEach(o -> cancelAndRestock(o, "Payment was not completed in time"));

        return stale.size();
    }

    // ---------- Internals ----------

    private Order findOwnedOrder(Long id, User user) {
        return orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    /** Single place that cancels an order, so stock/refund are never forgotten. */
    private void cancelAndRestock(Order order, String reason) {

        for (OrderItem item : order.getItems()) {
            productRepository.incrementStock(
                    item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason);
        orderRepository.save(order);

        // the coupon can be used again
        couponService.release(order.getCouponCode());

        eventService.record(order, OrderEvent.ORDER_CANCELLED,
                "Order cancelled. " + reason + ".");

        // lock the payment row so a webhook can't confirm it at the same time
        paymentRepository.lockByOrderId(order.getId()).ifPresent(payment -> {

            if (PaymentStatus.PAID.equals(payment.getStatus())) {

                payment.setStatus(PaymentStatus.REFUND_REQUIRED);
                eventService.record(order, OrderEvent.REFUND_INITIATED,
                        "Refund of " + Money.inr(payment.getAmount())
                                + " will be sent to your original payment method.");

            } else if (PaymentStatus.CREATED.equals(payment.getStatus())
                    || PaymentStatus.FAILED.equals(payment.getStatus())) {

                payment.setStatus(PaymentStatus.CANCELLED);
            }

            paymentRepository.save(payment);
        });
    }
}