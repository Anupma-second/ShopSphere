package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Category;
import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.CategoryNotFoundException;
import com.shopsphere.ecommerce.exception.ForbiddenException;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.repository.CategoryRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    // Only these fields may be used in ?sort=... (anything else -> 400)
    private static final Set<String> SORTABLE = Set.of("id", "name", "price", "stock");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // ---------- Ownership ----------

    /** Admin may manage any product; a seller only their own. */
    public void requireCanManage(Product product, User user) {

        if (user.getRole() == Role.ADMIN) {
            return;
        }

        boolean ownsIt = user.getRole() == Role.SELLER
                && product.getSeller() != null
                && product.getSeller().getId().equals(user.getId());

        if (!ownsIt) {
            throw new ForbiddenException("You can only manage your own products");
        }
    }

    /** Loads a product and checks the current user may change it. */
    public Product getManageableProduct(Long productId, User user) {

        Product product = findProduct(productId);
        requireCanManage(product, user);
        return product;
    }

    // ---------- Create / update / delete ----------

    public Product createProduct(Product product, User user) {

        product.setCategory(resolveCategory(product));

        // Sellers own what they create; admin-created products belong to the store.
        product.setSeller(user.getRole() == Role.SELLER ? user : null);

        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product updatedProduct, User user) {

        Product product = getManageableProduct(id, user);

        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        product.setCategory(resolveCategory(updatedProduct));

        return productRepository.save(product);
    }

    public Product updateStock(Long id, int stock, User user) {

        if (stock < 0) {
            throw new BadRequestException("Stock cannot be negative");
        }

        Product product = getManageableProduct(id, user);
        product.setStock(stock);

        return productRepository.save(product);
    }

    public void deleteProduct(Long id, User user) {
        productRepository.delete(getManageableProduct(id, user));
    }

    // ---------- Reads ----------

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /** Public catalogue search. Every filter is optional. */
    @Transactional(readOnly = true)
    public Page<Product> search(String q, Long categoryId,
                                Double minPrice, Double maxPrice,
                                boolean inStockOnly, Pageable pageable) {

        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("description")), like)));
        }

        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }

        if (minPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        if (inStockOnly) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThan(root.get("stock"), 0));
        }

        return productRepository.findAll(spec, safePageable(pageable));
    }

    @Transactional(readOnly = true)
    public Page<Product> getSellerProducts(User seller, Pageable pageable) {
        return productRepository.findBySeller_Id(seller.getId(), safePageable(pageable));
    }

    public List<Product> getLowStock(User seller, int threshold) {
        return productRepository
                .findBySeller_IdAndStockLessThanEqualOrderByStockAsc(seller.getId(), threshold);
    }

    // ---------- Internals ----------

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with id: " + id));
    }

    private Category resolveCategory(Product product) {

        if (product.getCategory() == null || product.getCategory().getId() == null) {
            throw new BadRequestException("category.id is required");
        }

        Long categoryId = product.getCategory().getId();

        return categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new CategoryNotFoundException(
                                "Category not found with id: " + categoryId));
    }

    /** Caps page size and rejects sorting by unknown/internal fields. */
    private Pageable safePageable(Pageable pageable) {

        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BadRequestException(
                        "Cannot sort by '" + order.getProperty()
                                + "'. Allowed: " + SORTABLE);
            }
        }

        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);

        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }
}