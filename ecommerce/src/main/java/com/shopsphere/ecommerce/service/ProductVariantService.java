package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.ProductVariant;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.repository.ProductRepository;
import com.shopsphere.ecommerce.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    public ProductVariantService(
            ProductVariantRepository productVariantRepository,
            ProductRepository productRepository) {

        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
    }

    public ProductVariant createVariant(ProductVariant variant) {

        Long productId = variant.getProduct().getId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + productId));

        variant.setProduct(product);

        return productVariantRepository.save(variant);
    }

    public List<ProductVariant> getAllVariants() {
        return productVariantRepository.findAll();
    }

    public Optional<ProductVariant> getVariantById(Long id) {
        return productVariantRepository.findById(id);
    }

    public ProductVariant updateVariant(
            Long id, ProductVariant updatedVariant) {

        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product variant not found with id: " + id));

        Long productId = updatedVariant.getProduct().getId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + productId));

        variant.setVariantName(updatedVariant.getVariantName());
        variant.setPrice(updatedVariant.getPrice());
        variant.setStock(updatedVariant.getStock());
        variant.setProduct(product);

        return productVariantRepository.save(variant);
    }

    public void deleteVariant(Long id) {
        productVariantRepository.deleteById(id);
    }
}