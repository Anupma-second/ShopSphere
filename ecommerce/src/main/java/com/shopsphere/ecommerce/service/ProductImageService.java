package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.ProductImage;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.ProductImageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductService productService;

    public ProductImageService(
            ProductImageRepository productImageRepository,
            ProductService productService) {

        this.productImageRepository = productImageRepository;
        this.productService = productService;
    }

    public ProductImage createImage(ProductImage productImage, User user) {

        if (productImage.getProduct() == null || productImage.getProduct().getId() == null) {
            throw new BadRequestException("product.id is required");
        }

        String url = productImage.getImageUrl() == null ? "" : productImage.getImageUrl().trim();

        if (!(url.startsWith("https://") || url.startsWith("http://"))) {
            throw new BadRequestException("Image URL must start with http:// or https://");
        }
        if (url.length() > 255) {
            throw new BadRequestException("Image URL is too long (max 255 characters)");
        }

        Product product = productService.getManageableProduct(
                productImage.getProduct().getId(), user);

        productImage.setImageUrl(url);
        productImage.setProduct(product);
        return productImageRepository.save(productImage);
    }

    public List<ProductImage> getAllImages() {
        return productImageRepository.findAll();
    }

    public Optional<ProductImage> getImageById(Long id) {
        return productImageRepository.findById(id);
    }

    public void deleteImage(Long id, User user) {

        ProductImage image = productImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product image not found with id: " + id));

        productService.requireCanManage(image.getProduct(), user);

        productImageRepository.delete(image);
    }
}