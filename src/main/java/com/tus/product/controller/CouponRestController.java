package com.tus.product.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Use wildcard or specific imports
import com.tus.product.model.Coupon; // Updated import
import com.tus.product.repos.CouponRepo; // Updated import

@RestController
@RequestMapping("/couponapi") // Or choose a different base path if needed, e.g., /productapi/coupons
public class CouponRestController {

    @Autowired
    CouponRepo repo;

    // --- Coupon CRUD Operations ---

    // Create Coupon
    @PostMapping(value = "/coupons")
    public ResponseEntity<Coupon> create(@RequestBody Coupon coupon) {
         // Add validation if necessary (e.g., check if code already exists)
        return new ResponseEntity<>(repo.save(coupon), HttpStatus.CREATED); // Use CREATED status
    }

    // Get Coupon by Code
    @GetMapping("/coupons/{code}")
    public ResponseEntity<Coupon> getCouponByCouponCode(@PathVariable String code) {
        Coupon coupon = repo.findByCode(code);
        if (coupon != null) {
            return ResponseEntity.ok(coupon);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Get All Coupons
    @GetMapping(value = "/coupons")
    public List<Coupon> getAllCoupons() {
        return repo.findAll();
    }

    // Update Coupon (Example - adapt as needed)
    @PutMapping("/coupons/{id}")
    public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @RequestBody Coupon couponDetails) {
         return repo.findById(id)
            .map(existingCoupon -> {
                existingCoupon.setCode(couponDetails.getCode());
                existingCoupon.setDiscount(couponDetails.getDiscount());
                existingCoupon.setExpDate(couponDetails.getExpDate());
                // Add validation
                return ResponseEntity.ok(repo.save(existingCoupon));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

     // Delete Coupon (Example - adapt as needed)
    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        return repo.findById(id)
            .map(coupon -> {
                repo.delete(coupon);
                return ResponseEntity.ok().<Void>build();
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

     // Optional: Delete Coupon by Code
    @DeleteMapping("/coupons/code/{code}")
    public ResponseEntity<Void> deleteCouponByCode(@PathVariable String code) {
         Coupon coupon = repo.findByCode(code);
         if (coupon != null) {
             repo.delete(coupon);
              return ResponseEntity.ok().<Void>build();
         } else {
             return ResponseEntity.notFound().build();
         }
     }
}