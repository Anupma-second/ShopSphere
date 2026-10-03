package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {
}