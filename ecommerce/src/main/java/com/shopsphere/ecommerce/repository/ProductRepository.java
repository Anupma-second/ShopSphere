package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    /**
     * Atomically reserves stock. Returns 1 if there was enough stock,
     * 0 if not - so two customers can never buy the last item together.
     */
    @Modifying
    @Query("update Product p set p.stock = p.stock - :qty "
            + "where p.id = :id and p.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("update Product p set p.stock = p.stock + :qty where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);

    // ---- Seller ----

    Page<Product> findBySeller_Id(Long sellerId, Pageable pageable);

    List<Product> findBySeller_IdAndStockLessThanEqualOrderByStockAsc(
            Long sellerId, int threshold);

    long countBySeller_Id(Long sellerId);

    long countBySeller_IdAndStockLessThanEqual(Long sellerId, int threshold);

    // ---- Admin ----

    long countByStockLessThanEqual(int threshold);
}