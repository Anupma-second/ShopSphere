package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.ProductVariant;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductService productService;

    public ProductVariantService(
            ProductVariantRepository productVariantRepository,
            ProductService productService) {

        this.productVariantRepository = productVariantRepository;
        this.productService = productService;
    }

    public ProductVariant createVariant(ProductVariant variant, User user) {

        variant.setProduct(manageableProductOf(variant, user));

        return productVariantRepository.save(variant);
    }

    public List<ProductVariant> getAllVariants() {
        return productVariantRepository.findAll();
    }

    public Optional<ProductVariant> getVariantById(Long id) {
        return productVariantRepository.findById(id);
    }

    public ProductVariant updateVariant(
            Long id, ProductVariant updatedVariant, User user) {

        ProductVariant variant = findVariant(id);

        // must own the current product AND the one it is being moved to
        productService.requireCanManage(variant.getProduct(), user);
        Product product = manageableProductOf(updatedVariant, user);

        variant.setVariantName(updatedVariant.getVariantName());
        variant.setPrice(updatedVariant.getPrice());
        variant.setStock(updatedVariant.getStock());
        variant.setProduct(product);

        return productVariantRepository.save(variant);
    }

    public void deleteVariant(Long id, User user) {

        ProductVariant variant = findVariant(id);
        productService.requireCanManage(variant.getProduct(), user);

        productVariantRepository.delete(variant);
    }

    private ProductVariant findVariant(Long id) {
        return productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product variant not found with id: " + id));
    }

    private Product manageableProductOf(ProductVariant variant, User user) {

        if (variant.getProduct() == null || variant.getProduct().getId() == null) {
            throw new BadRequestException("product.id is required");
        }

        return productService.getManageableProduct(variant.getProduct().getId(), user);
    }
}