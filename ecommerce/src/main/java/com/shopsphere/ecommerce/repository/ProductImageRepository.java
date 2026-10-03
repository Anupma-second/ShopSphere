package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository
        extends JpaRepository<ProductImage, Long> {
}
