package com.shopsphere.ecommerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_variants")
public class ProductVariant extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String variantName;

    private Double price;

    private Integer stock;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public ProductVariant() {
    }

    public ProductVariant(String variantName, Double price,
                          Integer stock, Product product) {
        this.variantName = variantName;
        this.price = price;
        this.stock = stock;
        this.product = product;
    }

    public Long getId() {
        return id;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
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

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
