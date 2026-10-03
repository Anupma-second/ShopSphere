package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.ProductImage;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.repository.ProductImageRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    public ProductImageService(
            ProductImageRepository productImageRepository,
            ProductRepository productRepository) {

        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
    }

    public ProductImage createImage(ProductImage productImage) {

        Long productId = productImage.getProduct().getId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + productId));

        productImage.setProduct(product);

        return productImageRepository.save(productImage);
    }

    public List<ProductImage> getAllImages() {
        return productImageRepository.findAll();
    }

    public Optional<ProductImage> getImageById(Long id) {
        return productImageRepository.findById(id);
    }

    public void deleteImage(Long id) {
        productImageRepository.deleteById(id);
    }
}