// Location: src/test/java/com/tus/product/controller/CouponRestControllerUnitTest.java
 package com.tus.product.controller;

 import com.tus.product.model.Coupon;
 import com.tus.product.repos.CouponRepo;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.mockito.InjectMocks;
 import org.mockito.Mock;
 import org.mockito.junit.jupiter.MockitoExtension;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;

 import java.math.BigDecimal;
 import java.util.List;
 import java.util.Optional;

 import static org.junit.jupiter.api.Assertions.*;
 import static org.mockito.ArgumentMatchers.any;
 import static org.mockito.Mockito.*;

 @ExtendWith(MockitoExtension.class)
 class CouponRestControllerUnitTest {

     @Mock
     private CouponRepo couponRepo;

     @InjectMocks
     private CouponRestController couponRestController;

     private Coupon couponInput;
     private Coupon existingCoupon;

     @BeforeEach
     void setUp() {
         couponInput = new Coupon();
         couponInput.setCode("NEWCODE");
         couponInput.setDiscount(new BigDecimal("5.50"));
         couponInput.setExpDate("2026-12-31");

         existingCoupon = new Coupon();
         existingCoupon.setId(1L);
         existingCoupon.setCode("EXISTING");
         existingCoupon.setDiscount(BigDecimal.TEN);
         existingCoupon.setExpDate("2025-12-31");
     }

     @Test
     void createCoupon_ShouldSucceed() {
         // Arrange
         when(couponRepo.save(any(Coupon.class))).thenAnswer(invocation -> {
             Coupon saved = invocation.getArgument(0);
             saved.setId(1L); // Simulate DB assigning ID
             return saved;
         });

         // Act
         ResponseEntity<Coupon> response = couponRestController.create(couponInput);

         // Assert
         assertEquals(HttpStatus.CREATED, response.getStatusCode());
         assertNotNull(response.getBody());
         assertNotNull(response.getBody().getId());
         assertEquals("NEWCODE", response.getBody().getCode());
         verify(couponRepo, times(1)).save(couponInput);
     }

     @Test
     void getCouponByCode_WhenExists_ShouldReturnCoupon() {
         // Arrange
         when(couponRepo.findByCode("EXISTING")).thenReturn(existingCoupon);

         // Act
         ResponseEntity<Coupon> response = couponRestController.getCouponByCouponCode("EXISTING");

         // Assert
         assertEquals(HttpStatus.OK, response.getStatusCode());
         assertEquals(existingCoupon, response.getBody());
         verify(couponRepo, times(1)).findByCode("EXISTING");
     }

     @Test
     void getCouponByCode_WhenNotFound_ShouldReturnNotFound() {
         // Arrange
         when(couponRepo.findByCode("NONEXISTENT")).thenReturn(null);

         // Act
         ResponseEntity<Coupon> response = couponRestController.getCouponByCouponCode("NONEXISTENT");

         // Assert
         assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
         verify(couponRepo, times(1)).findByCode("NONEXISTENT");
     }

     @Test
     void getAllCoupons_ShouldReturnList() {
         // Arrange
         when(couponRepo.findAll()).thenReturn(List.of(existingCoupon, couponInput));

         // Act
         List<Coupon> coupons = couponRestController.getAllCoupons();

         // Assert
         assertEquals(2, coupons.size());
         verify(couponRepo, times(1)).findAll();
     }

     @Test
     void updateCoupon_WhenExists_ShouldUpdateAndReturnOk() {
         // Arrange
         Long couponId = 1L;
         Coupon updatedDetails = new Coupon();
         updatedDetails.setCode("UPDATEDCODE");
         updatedDetails.setDiscount(new BigDecimal("99.00"));
         updatedDetails.setExpDate("2027-01-01");

         when(couponRepo.findById(couponId)).thenReturn(Optional.of(existingCoupon));
         when(couponRepo.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return updated

         // Act
         ResponseEntity<Coupon> response = couponRestController.updateCoupon(couponId, updatedDetails);

         // Assert
         assertEquals(HttpStatus.OK, response.getStatusCode());
         assertNotNull(response.getBody());
         assertEquals(couponId, response.getBody().getId());
         assertEquals("UPDATEDCODE", response.getBody().getCode()); // Check updated fields
         assertEquals(0, new BigDecimal("99.00").compareTo(response.getBody().getDiscount()));
         assertEquals("2027-01-01", response.getBody().getExpDate());

         verify(couponRepo, times(1)).findById(couponId);
         verify(couponRepo, times(1)).save(any(Coupon.class)); // Use any() or capture argument
     }

      @Test
     void updateCoupon_WhenNotFound_ShouldReturnNotFound() {
         // Arrange
         Long couponId = 99L;
         Coupon updatedDetails = new Coupon(); // Details irrelevant

         when(couponRepo.findById(couponId)).thenReturn(Optional.empty());

         // Act
         ResponseEntity<Coupon> response = couponRestController.updateCoupon(couponId, updatedDetails);

         // Assert
         assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
         verify(couponRepo, times(1)).findById(couponId);
         verify(couponRepo, never()).save(any(Coupon.class));
     }


     @Test
     void deleteCouponById_WhenExists_ShouldReturnOk() {
         // Arrange
         Long couponId = 1L;
         when(couponRepo.findById(couponId)).thenReturn(Optional.of(existingCoupon));
         doNothing().when(couponRepo).delete(existingCoupon);

         // Act
         ResponseEntity<Void> response = couponRestController.deleteCoupon(couponId);

         // Assert
         assertEquals(HttpStatus.OK, response.getStatusCode());
         verify(couponRepo, times(1)).findById(couponId);
         verify(couponRepo, times(1)).delete(existingCoupon);
     }

     @Test
     void deleteCouponById_WhenNotFound_ShouldReturnNotFound() {
         // Arrange
         Long couponId = 99L;
         when(couponRepo.findById(couponId)).thenReturn(Optional.empty());

         // Act
         ResponseEntity<Void> response = couponRestController.deleteCoupon(couponId);

         // Assert
         assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
         verify(couponRepo, times(1)).findById(couponId);
         verify(couponRepo, never()).delete(any(Coupon.class));
     }

       @Test
     void deleteCouponByCode_WhenExists_ShouldReturnOk() {
         // Arrange
         String couponCode = "EXISTING";
         when(couponRepo.findByCode(couponCode)).thenReturn(existingCoupon);
         doNothing().when(couponRepo).delete(existingCoupon);

         // Act
         ResponseEntity<Void> response = couponRestController.deleteCouponByCode(couponCode);

         // Assert
         assertEquals(HttpStatus.OK, response.getStatusCode());
         verify(couponRepo, times(1)).findByCode(couponCode);
         verify(couponRepo, times(1)).delete(existingCoupon);
     }

     @Test
     void deleteCouponByCode_WhenNotFound_ShouldReturnNotFound() {
         // Arrange
          String couponCode = "NONEXISTENT";
         when(couponRepo.findByCode(couponCode)).thenReturn(null);

         // Act
         ResponseEntity<Void> response = couponRestController.deleteCouponByCode(couponCode);

         // Assert
         assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
         verify(couponRepo, times(1)).findByCode(couponCode);
         verify(couponRepo, never()).delete(any(Coupon.class));
     }
 }