package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

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
}
