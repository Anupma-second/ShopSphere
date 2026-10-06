package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.OrderItemResponse;
import com.shopsphere.ecommerce.dto.SellerOrderResponse;
import com.shopsphere.ecommerce.dto.SellerStatsResponse;
import com.shopsphere.ecommerce.dto.TopProductResponse;
import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.OrderItem;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.ConflictException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.OrderItemRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SellerService {

    /** Orders whose money has been received (and not refunded). */
    public static final Set<OrderStatus> PAID_STATUSES =
            Set.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    // A seller ships and delivers; cancelling (and refunding) stays with the admin.
    private static final Set<OrderStatus> SELLER_SETTABLE =
            Set.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    public static final int DEFAULT_LOW_STOCK = 5;

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public SellerService(OrderItemRepository orderItemRepository,
                         ProductRepository productRepository,
                         OrderService orderService) {
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
    }

    // ---------- Orders ----------

    /** Orders containing this seller's products, newest first. status is optional. */
    @Transactional(readOnly = true)
    public List<SellerOrderResponse> getOrders(User seller, OrderStatus status) {

        List<OrderItem> items = (status == null)
                ? orderItemRepository.findSellerItems(seller.getId())
                : orderItemRepository.findSellerItemsByStatus(seller.getId(), status);

        // group the seller's lines by order, keeping newest-first order
        Map<Order, List<OrderItem>> byOrder = new LinkedHashMap<>();
        for (OrderItem item : items) {
            byOrder.computeIfAbsent(item.getOrder(), o -> new ArrayList<>()).add(item);
        }

        return byOrder.entrySet().stream()
                .map(e -> toResponse(e.getKey(), e.getValue(), seller))
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerOrderResponse getOrder(User seller, Long orderId) {

        List<OrderItem> items = sellerItemsOrThrow(orderId, seller);
        return toResponse(items.get(0).getOrder(), items, seller);
    }

    /**
     * Mark an order SHIPPED or DELIVERED. Only allowed when every item in
     * the order is this seller's - mixed orders are handled by the admin.
     * The normal transition rules (CONFIRMED -> SHIPPED -> DELIVERED) apply.
     */
    @Transactional
    public SellerOrderResponse updateOrderStatus(User seller, Long orderId,
                                                 OrderStatus status,
                                                 String carrier, String trackingNumber) {

        if (!SELLER_SETTABLE.contains(status)) {
            throw new BadRequestException(
                    "Sellers can only set status to SHIPPED or DELIVERED");
        }

        sellerItemsOrThrow(orderId, seller);

        if (orderItemRepository.countItemsNotFromSeller(orderId, seller.getId()) > 0) {
            throw new ConflictException(
                    "This order also contains items from other sellers; "
                            + "the store admin will update its status.");
        }

        orderService.updateStatusAsAdmin(orderId, status, carrier, trackingNumber);

        return getOrder(seller, orderId);
    }

    // ---------- Stats ----------

    @Transactional(readOnly = true)
    public SellerStatsResponse getStats(User seller) {

        Long id = seller.getId();

        List<TopProductResponse> top = orderItemRepository
                .sellerTopProducts(id, PAID_STATUSES, PageRequest.of(0, 5))
                .stream()
                .map(TopProductResponse::fromRow)
                .toList();

        return new SellerStatsResponse(
                productRepository.countBySeller_Id(id),
                productRepository.countBySeller_IdAndStockLessThanEqual(id, DEFAULT_LOW_STOCK),
                orderItemRepository.sellerOrderCount(id, PAID_STATUSES),
                orderItemRepository.sellerOrderCount(id, Set.of(OrderStatus.CONFIRMED)),
                orderItemRepository.sellerUnitsSold(id, PAID_STATUSES),
                orderItemRepository.sellerRevenue(id, PAID_STATUSES),
                top);
    }

    // ---------- Internals ----------

    /** 404 (not 403) when the seller has nothing in the order - don't leak that it exists. */
    private List<OrderItem> sellerItemsOrThrow(Long orderId, User seller) {

        List<OrderItem> items =
                orderItemRepository.findSellerItemsInOrder(orderId, seller.getId());

        if (items.isEmpty()) {
            throw new ResourceNotFoundException("Order not found");
        }

        return items;
    }

    private SellerOrderResponse toResponse(Order order, List<OrderItem> items, User seller) {

        List<OrderItemResponse> lines = items.stream()
                .map(i -> new OrderItemResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getPrice()))
                .toList();

        double subtotal = items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        boolean onlyThisSeller =
                orderItemRepository.countItemsNotFromSeller(order.getId(), seller.getId()) == 0;

        return new SellerOrderResponse(
                order.getId(),
                order.getStatus(),
                order.getOrderDate(),
                order.getUser() == null ? null : order.getUser().getName(),
                OrderService.addressOf(order),
                lines,
                subtotal,
                order.getCarrier(),
                order.getTrackingNumber(),
                onlyThisSeller && (order.getStatus() == OrderStatus.CONFIRMED
                        || order.getStatus() == OrderStatus.SHIPPED));
    }
}
