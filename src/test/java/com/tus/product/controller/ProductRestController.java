
 package com.tus.product.controller;

 import com.tus.product.model.Coupon;
 import com.tus.product.model.Product;
 import com.tus.product.repos.CouponRepo;
 import com.tus.product.repos.ProductRepo;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.mockito.InjectMocks;
 import org.mockito.Mock;
 import org.mockito.junit.jupiter.MockitoExtension;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.server.ResponseStatusException;

 import java.math.BigDecimal;

 import static org.junit.jupiter.api.Assertions.*;
 import static org.mockito.ArgumentMatchers.any;
 import static org.mockito.ArgumentMatchers.anyString;
 import static org.mockito.Mockito.*;

 @ExtendWith(MockitoExtension.class)
 class ProductRestControllerUnitTest {

     @Mock
     private ProductRepo productRepo;

     @Mock
     private CouponRepo couponRepo;

     @InjectMocks // Injects mocks into this controller instance
     private ProductRestController productRestController;

     private Product productInput;
     private Coupon validCoupon;

     @BeforeEach
     void setUp() {
         productInput = new Product();
         productInput.setName("Test Product");
         productInput.setDescription("Test Description");
         productInput.setPrice(new BigDecimal("100.00"));

         validCoupon = new Coupon();
         validCoupon.setId(1L);
         validCoupon.setCode("SAVE10");
         validCoupon.setDiscount(new BigDecimal("10.00"));
         validCoupon.setExpDate("2025-12-31");
     }

     @Test
     void createProduct_NoCoupon_ShouldSucceed() {
         // Arrange
         when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
             Product saved = invocation.getArgument(0);
             saved.setId(1L); // Simulate saving
             // Controller calculates discountedPrice before returning, mimic that
             saved.setDiscountedPrice(saved.getPrice());
             return saved;
         });

         // Act
         ResponseEntity<Product> response = productRestController.create(productInput);

         // Assert
         assertNotNull(response);
         assertEquals(HttpStatus.CREATED, response.getStatusCode());
         assertNotNull(response.getBody());
         assertEquals(productInput.getName(), response.getBody().getName());
         assertEquals(productInput.getPrice(), response.getBody().getPrice());
         assertNull(response.getBody().getAppliedCouponCode()); // No coupon applied
         assertEquals(productInput.getPrice(), response.getBody().getDiscountedPrice()); // Discounted = Original
         assertNull(response.getBody().getCouponCode()); // Input coupon code was null

         verify(couponRepo, never()).findByCode(anyString()); // Ensure coupon repo wasn't called
         verify(productRepo, times(1)).save(any(Product.class));
     }

     @Test
     void createProduct_WithValidCoupon_ShouldApplyDiscount() {
         // Arrange
         productInput.setCouponCode("SAVE10"); // Set input coupon code

         when(couponRepo.findByCode("SAVE10")).thenReturn(validCoupon);
         when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
             Product saved = invocation.getArgument(0);
             saved.setId(1L); // Simulate saving
             // The controller calculates this *after* save but *before* returning
             // Simulate the state passed to save() first
             assertEquals(new BigDecimal("100.00"), saved.getPrice());
             assertEquals("SAVE10", saved.getAppliedCouponCode());
             // Now simulate the transient fields being set for the response
             saved.setDiscountedPrice(new BigDecimal("90.00"));
             saved.setCouponCode("SAVE10"); // Controller sets input code in response
             return saved;
         });

         // Act
         ResponseEntity<Product> response = productRestController.create(productInput);

         // Assert
         assertNotNull(response);
         assertEquals(HttpStatus.CREATED, response.getStatusCode());
         Product responseBody = response.getBody();
         assertNotNull(responseBody);
         assertEquals(1L, responseBody.getId());
         assertEquals("Test Product", responseBody.getName());
         assertEquals(new BigDecimal("100.00"), responseBody.getPrice()); // Original price
         assertEquals("SAVE10", responseBody.getAppliedCouponCode()); // Applied code correct
         assertEquals(new BigDecimal("90.00"), responseBody.getDiscountedPrice()); // Discount applied
         assertEquals("SAVE10", responseBody.getCouponCode()); // Input coupon code shown in response

         verify(couponRepo, times(1)).findByCode("SAVE10");
         verify(productRepo, times(1)).save(any(Product.class));
     }

     @Test
     void createProduct_WithInvalidCoupon_ShouldThrowException() {
         // Arrange
         productInput.setCouponCode("INVALID");
         when(couponRepo.findByCode("INVALID")).thenReturn(null); // Coupon not found

         // Act & Assert
         ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
             productRestController.create(productInput);
         });

         assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
         assertTrue(exception.getReason().contains("Coupon code not found: INVALID"));

         verify(couponRepo, times(1)).findByCode("INVALID");
         verify(productRepo, never()).save(any(Product.class)); // Ensure save wasn't called
     }
 }