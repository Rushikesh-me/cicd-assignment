// Location: productservice/src/main/java/com/tus/product/controller/ProductRestController.java
package com.tus.product.controller;

import java.util.List;
import java.math.BigDecimal;
import java.util.stream.Collectors; // Import Collectors

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.tus.product.model.Coupon;
import com.tus.product.model.Product;
import com.tus.product.repos.ProductRepo;
import com.tus.product.repos.CouponRepo;

@RestController
@RequestMapping("/productapi")
public class ProductRestController {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CouponRepo couponRepo;

    // Create Product
    @PostMapping(value = "/products")
    public ResponseEntity<Product> create(@RequestBody Product product) {
        String inputCouponCode = product.getCouponCode(); // Get code from request body
        BigDecimal originalPrice = product.getPrice();
        BigDecimal calculatedDiscountedPrice = originalPrice; // Default to original price
        String appliedCode = null; // Code to be stored

        // Check if a coupon code is provided in the input
        if (inputCouponCode != null && !inputCouponCode.isEmpty()) {
            Coupon coupon = couponRepo.findByCode(inputCouponCode);
            if (coupon != null) {
                // Apply discount logic
                BigDecimal discount = coupon.getDiscount();
                // Calculate discounted price, ensure it doesn't go below zero
                calculatedDiscountedPrice = originalPrice.subtract(discount).max(BigDecimal.ZERO);
                appliedCode = coupon.getCode(); // Store the valid code
            } else {
                // Handle case where coupon code is provided but not found
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon code not found: " + inputCouponCode);
            }
        }

        // Set persistent and transient fields before saving
        product.setPrice(originalPrice); // Ensure original price is set
        product.setAppliedCouponCode(appliedCode); // Set the code that was applied (or null)
        // Note: Transient fields 'discountedPrice' and 'couponCode' are not saved by JPA

        // Save the product with original price and applied coupon code
        Product savedProduct = productRepo.save(product);

        // Populate transient fields for the response object
        savedProduct.setDiscountedPrice(calculatedDiscountedPrice);
        savedProduct.setCouponCode(inputCouponCode); // Show the input code in response

        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    // Get All Products
    @GetMapping(value = "/products")
    public List<Product> getAllProducts() {
        List<Product> products = productRepo.findAll();
        // Populate transient fields for each product before returning
        return products.stream()
                       .map(this::populateTransientFields)
                       .collect(Collectors.toList());
    }

    // Get Product by ID
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepo.findById(id)
            .map(this::populateTransientFields) // Populate transient fields
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update Product (Example - includes similar logic to create)
    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
         return productRepo.findById(id)
            .map(existingProduct -> {
                // Update basic fields
                existingProduct.setName(productDetails.getName());
                existingProduct.setDescription(productDetails.getDescription());
                existingProduct.setPrice(productDetails.getPrice()); // Update original price

                // Coupon logic for update
                String inputCouponCode = productDetails.getCouponCode();
                BigDecimal originalPrice = existingProduct.getPrice(); // Use the updated price
                BigDecimal calculatedDiscountedPrice = originalPrice;
                String appliedCode = null;

                if (inputCouponCode != null && !inputCouponCode.isEmpty()) {
                    Coupon coupon = couponRepo.findByCode(inputCouponCode);
                    if (coupon != null) {
                        BigDecimal discount = coupon.getDiscount();
                        calculatedDiscountedPrice = originalPrice.subtract(discount).max(BigDecimal.ZERO);
                        appliedCode = coupon.getCode();
                    } else {
                       throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon code not found: " + inputCouponCode);
                    }
                } else {
                     // If no coupon code in update, clear applied code if necessary
                     // Keep calculatedDiscountedPrice = originalPrice
                }

                existingProduct.setAppliedCouponCode(appliedCode); // Update applied code

                Product savedProduct = productRepo.save(existingProduct);

                 // Populate transient fields for the response object
                savedProduct.setDiscountedPrice(calculatedDiscountedPrice);
                savedProduct.setCouponCode(inputCouponCode); // Show input code in response

                return ResponseEntity.ok(savedProduct);
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

     // Delete Product (Example - no changes needed here)
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
         return productRepo.findById(id)
            .map(product -> {
                productRepo.delete(product);
                return ResponseEntity.ok().<Void>build();
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- Helper method to populate transient fields for GET responses ---
    private Product populateTransientFields(Product product) {
        BigDecimal discounted = product.getPrice(); // Default to original price
        String appliedCode = product.getAppliedCouponCode();
        product.setCouponCode(appliedCode); // Display the applied code as 'couponCode' in response

        if (appliedCode != null && !appliedCode.isEmpty()) {
            Coupon coupon = couponRepo.findByCode(appliedCode);
            if (coupon != null) {
                BigDecimal discount = coupon.getDiscount();
                discounted = product.getPrice().subtract(discount).max(BigDecimal.ZERO);
            }
            // Optional: Handle case where appliedCode exists but coupon was deleted
            // else { logger.warn("Coupon code {} applied to product {} not found.", appliedCode, product.getId()); }
        }
        product.setDiscountedPrice(discounted);
        return product;
    }
}