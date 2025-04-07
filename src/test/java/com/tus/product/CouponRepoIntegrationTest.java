
 package com.tus.product;

 import com.tus.product.model.Coupon;
import com.tus.product.repos.CouponRepo;

import org.junit.jupiter.api.Test;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

 import java.math.BigDecimal;

 import static org.junit.jupiter.api.Assertions.*;

 @DataJpaTest // Configures H2, JPA, and only loads repository/entity components
 class CouponRepoIntegrationTest {

     @Autowired
     private CouponRepo couponRepo;

     @Test
     void findByCode_WhenCouponExists_ShouldReturnCoupon() {
         // Arrange
         Coupon coupon = new Coupon();
         coupon.setCode("TESTCODE");
         coupon.setDiscount(new BigDecimal("5.00"));
         coupon.setExpDate("2025-12-31");
         couponRepo.save(coupon); // Save directly using the repo

         // Act
         Coupon found = couponRepo.findByCode("TESTCODE");

         // Assert
         assertNotNull(found);
         assertEquals("TESTCODE", found.getCode());
         assertEquals(0, new BigDecimal("5.00").compareTo(found.getDiscount())); // Use compareTo for BigDecimal
     }

     @Test
     void findByCode_WhenCouponDoesNotExist_ShouldReturnNull() {
         // Arrange (No coupon saved)

         // Act
         Coupon found = couponRepo.findByCode("NONEXISTENT");

         // Assert
         assertNull(found);
     }
 }