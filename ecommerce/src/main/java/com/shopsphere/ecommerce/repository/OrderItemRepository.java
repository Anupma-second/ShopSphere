package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.OrderItem;
import com.shopsphere.ecommerce.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // ---- Seller views: only the lines that belong to this seller ----

    @Query("select oi from OrderItem oi join fetch oi.order o "
            + "where oi.product.seller.id = :sellerId "
            + "order by o.orderDate desc")
    List<OrderItem> findSellerItems(@Param("sellerId") Long sellerId);

    @Query("select oi from OrderItem oi join fetch oi.order o "
            + "where oi.product.seller.id = :sellerId and o.status = :status "
            + "order by o.orderDate desc")
    List<OrderItem> findSellerItemsByStatus(@Param("sellerId") Long sellerId,
                                            @Param("status") OrderStatus status);

    @Query("select oi from OrderItem oi "
            + "where oi.order.id = :orderId and oi.product.seller.id = :sellerId")
    List<OrderItem> findSellerItemsInOrder(@Param("orderId") Long orderId,
                                           @Param("sellerId") Long sellerId);

    /** Lines in the order that are NOT this seller's (store items or other sellers). */
    @Query("select count(oi) from OrderItem oi join oi.product p left join p.seller s "
            + "where oi.order.id = :orderId and (s is null or s.id <> :sellerId)")
    long countItemsNotFromSeller(@Param("orderId") Long orderId,
                                 @Param("sellerId") Long sellerId);

    // ---- Seller stats ----

    @Query("select coalesce(sum(oi.price * oi.quantity), 0) from OrderItem oi "
            + "where oi.product.seller.id = :sellerId and oi.order.status in :statuses")
    Double sellerRevenue(@Param("sellerId") Long sellerId,
                         @Param("statuses") Collection<OrderStatus> statuses);

    @Query("select coalesce(sum(oi.quantity), 0) from OrderItem oi "
            + "where oi.product.seller.id = :sellerId and oi.order.status in :statuses")
    Long sellerUnitsSold(@Param("sellerId") Long sellerId,
                         @Param("statuses") Collection<OrderStatus> statuses);

    @Query("select count(distinct oi.order.id) from OrderItem oi "
            + "where oi.product.seller.id = :sellerId and oi.order.status in :statuses")
    long sellerOrderCount(@Param("sellerId") Long sellerId,
                          @Param("statuses") Collection<OrderStatus> statuses);

    /** Rows: [productId, productName, unitsSold, revenue] */
    @Query("select oi.product.id, oi.product.name, sum(oi.quantity), sum(oi.price * oi.quantity) "
            + "from OrderItem oi "
            + "where oi.product.seller.id = :sellerId and oi.order.status in :statuses "
            + "group by oi.product.id, oi.product.name "
            + "order by sum(oi.quantity) desc")
    List<Object[]> sellerTopProducts(@Param("sellerId") Long sellerId,
                                     @Param("statuses") Collection<OrderStatus> statuses,
                                     Pageable pageable);

    // ---- Store-wide stats (admin) ----

    /** Rows: [productId, productName, unitsSold, revenue] */
    @Query("select oi.product.id, oi.product.name, sum(oi.quantity), sum(oi.price * oi.quantity) "
            + "from OrderItem oi where oi.order.status in :statuses "
            + "group by oi.product.id, oi.product.name "
            + "order by sum(oi.quantity) desc")
    List<Object[]> topProducts(@Param("statuses") Collection<OrderStatus> statuses,
                               Pageable pageable);
}