
 package com.tus.product.controller;

 import com.tus.product.model.Coupon;
 import com.tus.product.model.Product;
 import com.tus.product.repos.CouponRepo;
 import com.tus.product.repos.ProductRepo;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
 import org.mockito.Mock;
 import org.mockito.junit.jupiter.MockitoExtension;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.server.ResponseStatusException;

 import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

     // Add these test methods to ProductRestControllerUnitTest.java

 @Test
 void getProductById_WhenExists_ShouldReturnProduct() {
     // Arrange
     Long productId = 1L;
     Product existingProduct = new Product();
     existingProduct.setId(productId);
     existingProduct.setName("Found Product");
     existingProduct.setPrice(new BigDecimal("50.00"));
     // Assume no coupon applied initially for simplicity in mock setup
     existingProduct.setAppliedCouponCode(null);

     // Mock repository response
     when(productRepo.findById(productId)).thenReturn(Optional.of(existingProduct));
     // Mock coupon repo for the populateTransientFields helper method if needed
     // In this case, no applied code, so findByCode won't be called by populateTransientFields

     // Act
     ResponseEntity<Product> response = productRestController.getProductById(productId);

     // Assert
     assertEquals(HttpStatus.OK, response.getStatusCode());
     assertNotNull(response.getBody());
     assertEquals(productId, response.getBody().getId());
     assertEquals("Found Product", response.getBody().getName());
     // The populateTransientFields method will set discountedPrice = price if no coupon
     assertEquals(0, new BigDecimal("50.00").compareTo(response.getBody().getDiscountedPrice()));
     verify(productRepo, times(1)).findById(productId);
     verify(couponRepo, never()).findByCode(anyString()); // Verify findByCode wasn't called
 }

  @Test
 void getProductById_WhenExistsWithAppliedCoupon_ShouldPopulateTransientFields() {
     // Arrange
     Long productId = 2L;
     Product existingProduct = new Product();
     existingProduct.setId(productId);
     existingProduct.setName("Discounted Product");
     existingProduct.setPrice(new BigDecimal("100.00"));
     existingProduct.setAppliedCouponCode("POPULATE10"); // Coupon was applied when saved

     Coupon appliedCoupon = new Coupon();
     appliedCoupon.setCode("POPULATE10");
     appliedCoupon.setDiscount(new BigDecimal("10.00"));

     // Mock repository responses
     when(productRepo.findById(productId)).thenReturn(Optional.of(existingProduct));
     when(couponRepo.findByCode("POPULATE10")).thenReturn(appliedCoupon); // Mock for populateTransientFields

     // Act
     ResponseEntity<Product> response = productRestController.getProductById(productId);

     // Assert
     assertEquals(HttpStatus.OK, response.getStatusCode());
     Product responseBody = response.getBody();
     assertNotNull(responseBody);
     assertEquals(productId, responseBody.getId());
     assertEquals("Discounted Product", responseBody.getName());
     assertEquals(0, new BigDecimal("100.00").compareTo(responseBody.getPrice())); // Original price
     assertEquals("POPULATE10", responseBody.getAppliedCouponCode()); // Verify applied code is still there (though not directly used)
     assertEquals(0, new BigDecimal("90.00").compareTo(responseBody.getDiscountedPrice())); // Verify calculated discounted price
     assertEquals("POPULATE10", responseBody.getCouponCode()); // Verify transient couponCode field is populated

     verify(productRepo, times(1)).findById(productId);
     verify(couponRepo, times(1)).findByCode("POPULATE10"); // Verify findByCode was called by helper
 }


 @Test
 void getProductById_WhenNotFound_ShouldReturnNotFound() {
     // Arrange
     Long productId = 99L;
     when(productRepo.findById(productId)).thenReturn(Optional.empty());

     // Act
     ResponseEntity<Product> response = productRestController.getProductById(productId);

     // Assert
     assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
     verify(productRepo, times(1)).findById(productId);
 }

 @Test
 void getAllProducts_ShouldReturnProductList() {
     // Arrange
     Product p1 = new Product(); p1.setId(1L); p1.setName("P1"); p1.setPrice(BigDecimal.TEN);
     Product p2 = new Product(); p2.setId(2L); p2.setName("P2"); p2.setPrice(BigDecimal.ONE); p2.setAppliedCouponCode("CODE1");

     Coupon c1 = new Coupon(); c1.setCode("CODE1"); c1.setDiscount(new BigDecimal("0.1"));

     when(productRepo.findAll()).thenReturn(List.of(p1, p2));
     // Mock coupon repo for populateTransientFields on p2
     when(couponRepo.findByCode("CODE1")).thenReturn(c1);

     // Act
     List<Product> products = productRestController.getAllProducts();

     // Assert
     assertEquals(2, products.size());
     // Check population of transient fields happened
     assertEquals(0, BigDecimal.TEN.compareTo(products.get(0).getDiscountedPrice())); // P1 - no discount
     assertEquals(0, new BigDecimal("0.9").compareTo(products.get(1).getDiscountedPrice())); // P2 - discount applied
     assertEquals("CODE1", products.get(1).getCouponCode()); // Check transient coupon code

     verify(productRepo, times(1)).findAll();
     verify(couponRepo, times(1)).findByCode("CODE1"); // Called for p2
 }


 @Test
 void updateProduct_WhenExistsAndValidCoupon_ShouldUpdateAndApplyDiscount() {
     // Arrange
     Long productId = 1L;
     Product existingProduct = new Product(); // Product currently in DB
     existingProduct.setId(productId);
     existingProduct.setName("Old Name");
     existingProduct.setPrice(new BigDecimal("100.00"));

     Product productDetails = new Product(); // New details from request
     productDetails.setName("New Name Updated");
     productDetails.setDescription("Updated Desc");
     productDetails.setPrice(new BigDecimal("120.00")); // Price update
     productDetails.setCouponCode("UPDATE15"); // New coupon code in update request

     Coupon updateCoupon = new Coupon();
     updateCoupon.setCode("UPDATE15");
     updateCoupon.setDiscount(new BigDecimal("15.00"));

     when(productRepo.findById(productId)).thenReturn(Optional.of(existingProduct));
     when(couponRepo.findByCode("UPDATE15")).thenReturn(updateCoupon);
     when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
         Product saved = invocation.getArgument(0);
         // Simulate state passed to save: updated fields + applied code
         assertEquals("New Name Updated", saved.getName());
         assertEquals(0, new BigDecimal("120.00").compareTo(saved.getPrice()));
         assertEquals("UPDATE15", saved.getAppliedCouponCode());
         // Simulate transient fields for response
         saved.setDiscountedPrice(new BigDecimal("105.00")); // 120 - 15
         saved.setCouponCode("UPDATE15");
         return saved;
     });

     // Act
     ResponseEntity<Product> response = productRestController.updateProduct(productId, productDetails);

     // Assert
     assertEquals(HttpStatus.OK, response.getStatusCode());
     Product responseBody = response.getBody();
     assertNotNull(responseBody);
     assertEquals(productId, responseBody.getId());
     assertEquals("New Name Updated", responseBody.getName());
     assertEquals(0, new BigDecimal("120.00").compareTo(responseBody.getPrice()));
     assertEquals("UPDATE15", responseBody.getAppliedCouponCode());
     assertEquals(0, new BigDecimal("105.00").compareTo(responseBody.getDiscountedPrice()));
     assertEquals("UPDATE15", responseBody.getCouponCode());

     verify(productRepo, times(1)).findById(productId);
     verify(couponRepo, times(1)).findByCode("UPDATE15");
     verify(productRepo, times(1)).save(any(Product.class));
 }

 @Test
 void updateProduct_WhenExistsAndNoCouponInUpdate_ShouldUpdateAndRemoveDiscount() {
    // Arrange
    Long productId = 1L;
    Product existingProduct = new Product(); // Product currently in DB with old coupon
    existingProduct.setId(productId);
    existingProduct.setName("Old Name");
    existingProduct.setPrice(new BigDecimal("100.00"));
    existingProduct.setAppliedCouponCode("OLD_COUPON"); // Previously had a coupon

    Product productDetails = new Product(); // New details from request, NO coupon
    productDetails.setName("New Name Updated");
    productDetails.setPrice(new BigDecimal("110.00")); // Price update
    productDetails.setCouponCode(null); // Explicitly no coupon code in update

    when(productRepo.findById(productId)).thenReturn(Optional.of(existingProduct));
    // No couponRepo.findByCode should be called if input couponCode is null/empty
    when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
        Product saved = invocation.getArgument(0);
        // Simulate state passed to save: updated fields, null applied code
        assertEquals("New Name Updated", saved.getName());
        assertEquals(0, new BigDecimal("110.00").compareTo(saved.getPrice()));
        assertNull(saved.getAppliedCouponCode()); // Coupon should be removed
        // Simulate transient fields for response
        saved.setDiscountedPrice(new BigDecimal("110.00")); // Discounted = new original price
        saved.setCouponCode(null);
        return saved;
    });

    // Act
    ResponseEntity<Product> response = productRestController.updateProduct(productId, productDetails);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Product responseBody = response.getBody();
    assertNotNull(responseBody);
    assertEquals(productId, responseBody.getId());
    assertEquals("New Name Updated", responseBody.getName());
    assertEquals(0, new BigDecimal("110.00").compareTo(responseBody.getPrice()));
    assertNull(responseBody.getAppliedCouponCode()); // Ensure coupon was removed
    assertEquals(0, new BigDecimal("110.00").compareTo(responseBody.getDiscountedPrice())); // Discounted price is original
    assertNull(responseBody.getCouponCode()); // No input code in response

    verify(productRepo, times(1)).findById(productId);
    verify(couponRepo, never()).findByCode(anyString()); // Coupon repo not called
    verify(productRepo, times(1)).save(any(Product.class));
}


 @Test
 void updateProduct_WhenExistsAndInvalidCoupon_ShouldThrowException() {
     // Arrange
     Long productId = 1L;
     Product existingProduct = new Product();
     existingProduct.setId(productId);
     existingProduct.setPrice(new BigDecimal("100.00"));

     Product productDetails = new Product();
     productDetails.setCouponCode("INVALID_UPDATE");

     when(productRepo.findById(productId)).thenReturn(Optional.of(existingProduct));
     when(couponRepo.findByCode("INVALID_UPDATE")).thenReturn(null); // Coupon not found

     // Act & Assert
     ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
         productRestController.updateProduct(productId, productDetails);
     });

     assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
     assertTrue(exception.getReason().contains("Coupon code not found: INVALID_UPDATE"));

     verify(productRepo, times(1)).findById(productId);
     verify(couponRepo, times(1)).findByCode("INVALID_UPDATE");
     verify(productRepo, never()).save(any(Product.class)); // Save not called
 }

 @Test
 void updateProduct_WhenNotFound_ShouldReturnNotFound() {
     // Arrange
     Long productId = 99L;
     Product productDetails = new Product(); // Details irrelevant as product not found

     when(productRepo.findById(productId)).thenReturn(Optional.empty());

     // Act
     ResponseEntity<Product> response = productRestController.updateProduct(productId, productDetails);

     // Assert
     assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
     verify(productRepo, times(1)).findById(productId);
     verify(couponRepo, never()).findByCode(anyString());
     verify(productRepo, never()).save(any(Product.class));
 }

 @Test
 void deleteProduct_WhenExists_ShouldReturnOk() {
     // Arrange
     Long productId = 1L;
     Product existingProduct = new Product();
     existingProduct.setId(productId);
     when(productRepo.findById(productId)).thenReturn(Optional.of(existingProduct));
     doNothing().when(productRepo).delete(existingProduct); // Mock void method

     // Act
     ResponseEntity<Void> response = productRestController.deleteProduct(productId);

     // Assert
     assertEquals(HttpStatus.OK, response.getStatusCode());
     verify(productRepo, times(1)).findById(productId);
     verify(productRepo, times(1)).delete(existingProduct);
 }

 @Test
 void deleteProduct_WhenNotFound_ShouldReturnNotFound() {
     // Arrange
     Long productId = 99L;
     when(productRepo.findById(productId)).thenReturn(Optional.empty());

     // Act
     ResponseEntity<Void> response = productRestController.deleteProduct(productId);

     // Assert
     assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
     verify(productRepo, times(1)).findById(productId);
     verify(productRepo, never()).delete(any(Product.class));
 }

 // Test case for discount exceeding price
 @Test
 void createProduct_WithCouponDiscountGreaterThanPrice_ShouldResultInZeroPrice() {
     // Arrange
     productInput.setPrice(new BigDecimal("5.00")); // Price lower than discount
     productInput.setCouponCode("SAVE10"); // Coupon gives 10.00 discount

     when(couponRepo.findByCode("SAVE10")).thenReturn(validCoupon); // validCoupon has 10.00 discount
     when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
         Product saved = invocation.getArgument(0);
         saved.setId(1L);
         // Simulate controller logic for response
         saved.setDiscountedPrice(BigDecimal.ZERO); // Should cap at zero
         saved.setCouponCode("SAVE10");
         return saved;
     });

     // Act
     ResponseEntity<Product> response = productRestController.create(productInput);

     // Assert
     assertEquals(HttpStatus.CREATED, response.getStatusCode());
     Product responseBody = response.getBody();
     assertNotNull(responseBody);
     assertEquals(0, new BigDecimal("5.00").compareTo(responseBody.getPrice())); // Original price stored
     assertEquals(0, BigDecimal.ZERO.compareTo(responseBody.getDiscountedPrice())); // Discounted price capped at 0
     assertEquals("SAVE10", responseBody.getAppliedCouponCode());

     verify(couponRepo, times(1)).findByCode("SAVE10");
     verify(productRepo, times(1)).save(any(Product.class));
     // Verify the saved product had the correct original price and applied code
     ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
     verify(productRepo).save(productCaptor.capture());
     assertEquals(0, new BigDecimal("5.00").compareTo(productCaptor.getValue().getPrice()));
     assertEquals("SAVE10", productCaptor.getValue().getAppliedCouponCode());
 }
 }