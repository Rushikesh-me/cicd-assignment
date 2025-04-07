// Location: productservice/src/main/java/com/tus/product/model/Product.java
package com.tus.product.model;

import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private BigDecimal price; // This will store the ORIGINAL price

    private String appliedCouponCode; // Stores the coupon code applied (persistent)

    @Transient
    private String couponCode; // Used for input during POST/PUT requests

    @Transient
    private BigDecimal discountedPrice; // Calculated price after discount (not stored in DB)

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    // Getter/Setter for the applied coupon code (stored in DB)
    public String getAppliedCouponCode() {
        return appliedCouponCode;
    }

    public void setAppliedCouponCode(String appliedCouponCode) {
        this.appliedCouponCode = appliedCouponCode;
    }

    // Getter/Setter for the input coupon code (transient)
    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    // Getter/Setter for the calculated discounted price (transient)
    public BigDecimal getDiscountedPrice() {
        // Calculate on the fly if not set (useful for GET requests)
        if (this.discountedPrice == null) {
             // Default to original price if no discount logic applied yet
            return this.price;
        }
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }
}