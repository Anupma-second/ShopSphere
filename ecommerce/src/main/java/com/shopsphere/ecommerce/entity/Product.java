package com.shopsphere.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // Who sells this product. Null = sold by the store itself (admin-created).
    // Never serialized directly - User holds the password hash.
    @ManyToOne
    @JoinColumn(name = "seller_id")
    private User seller;

    // Photos, first one = main image. Deleting a product deletes its photos.
    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    @JsonIgnore
    private List<ProductImage> imageEntities = new ArrayList<>();

    // Read-only, calculated by the database every time the product is loaded
    @Formula("(select avg(r.rating) from reviews r where r.product_id = id)")
    private Double averageRating;

    @Formula("(select count(*) from reviews r where r.product_id = id)")
    private Integer reviewCount;

    /** What the API returns for each photo. */
    public record Image(Long id, String imageUrl) {
    }

    public Product() {
    }

    public Product(String name, String description, Double price, Integer stock, Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @JsonIgnore
    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    // Read-only JSON fields so the frontend can show "Sold by ..."
    public Long getSellerId() {
        return seller == null ? null : seller.getId();
    }

    public String getSellerName() {
        return seller == null ? null : seller.getName();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<Image> getImages() {
        return imageEntities.stream()
                .map(i -> new Image(i.getId(), i.getImageUrl()))
                .toList();
    }

    /** Average star rating (1-5), or null when there are no reviews yet. */
    public Double getAverageRating() {
        return averageRating == null ? null : Math.round(averageRating * 10) / 10.0;
    }

    public int getReviewCount() {
        return reviewCount == null ? 0 : reviewCount;
    }
}