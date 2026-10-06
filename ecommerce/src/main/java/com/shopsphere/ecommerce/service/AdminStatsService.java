package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.AdminStatsResponse;
import com.shopsphere.ecommerce.dto.AdminStatsResponse.DailyRevenue;
import com.shopsphere.ecommerce.dto.TopProductResponse;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.repository.OrderItemRepository;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import com.shopsphere.ecommerce.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AdminStatsService {

    private static final int DAYS = 30;

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public AdminStatsService(UserRepository userRepository,
                             OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {

        Map<Role, Long> usersByRole = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            usersByRole.put(role, userRepository.countByRole(role));
        }

        Map<OrderStatus, Long> ordersByStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status, 0L);
        }
        for (Object[] row : orderRepository.countGroupedByStatus()) {
            if (row[0] != null) {
                ordersByStatus.put((OrderStatus) row[0], ((Number) row[1]).longValue());
            }
        }

        List<DailyRevenue> daily = dailyRevenue();
        double last30 = daily.stream().mapToDouble(DailyRevenue::revenue).sum();

        List<TopProductResponse> top = orderItemRepository
                .topProducts(SellerService.PAID_STATUSES, PageRequest.of(0, 5))
                .stream()
                .map(TopProductResponse::fromRow)
                .toList();

        return new AdminStatsResponse(
                usersByRole,
                ordersByStatus,
                productRepository.count(),
                productRepository.countByStockLessThanEqual(SellerService.DEFAULT_LOW_STOCK),
                orderRepository.sumRevenue(SellerService.PAID_STATUSES),
                last30,
                daily,
                top);
    }

    /** One entry per day for the last 30 days, including days with no sales. */
    private List<DailyRevenue> dailyRevenue() {

        LocalDate today = LocalDate.now();
        LocalDate first = today.minusDays(DAYS - 1);

        Map<LocalDate, double[]> buckets = new TreeMap<>();   // [revenue, orders]
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            buckets.put(d, new double[2]);
        }

        List<Object[]> rows = orderRepository.revenueRowsSince(
                SellerService.PAID_STATUSES, first.atStartOfDay());

        for (Object[] row : rows) {
            LocalDate day = ((LocalDateTime) row[0]).toLocalDate();
            double[] bucket = buckets.get(day);
            if (bucket != null && row[1] != null) {
                bucket[0] += ((Number) row[1]).doubleValue();
                bucket[1] += 1;
            }
        }

        List<DailyRevenue> result = new ArrayList<>();
        buckets.forEach((day, b) -> result.add(new DailyRevenue(day, b[0], (long) b[1])));
        return result;
    }
}
