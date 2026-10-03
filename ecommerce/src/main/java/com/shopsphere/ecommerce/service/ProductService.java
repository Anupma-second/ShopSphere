package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.entity.Category;
import com.shopsphere.ecommerce.exception.CategoryNotFoundException;
import com.shopsphere.ecommerce.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,  CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product createProduct(Product product) {

        Long categoryId = product.getCategory().getId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new CategoryNotFoundException(
                                "Category not found with id: " + categoryId));

        product.setCategory(category);

        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product updateProduct(Long id, Product updatedProduct) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id));

        Long categoryId = updatedProduct.getCategory().getId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new CategoryNotFoundException(
                                "Category not found with id: " + categoryId));

        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        product.setCategory(category);

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}