// Location: src/test/java/com/tus/product/ProductE2ETest.java
 package com.tus.product;

 import com.tus.product.model.Coupon; // Using model just for setting up data or request
 import com.tus.product.model.Product; // Using model just for request body/response parsing
 import com.tus.product.repos.CouponRepo;
 import com.tus.product.repos.ProductRepo;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.boot.test.web.client.TestRestTemplate;
 import org.springframework.boot.test.web.server.LocalServerPort; // Inject random port
 import org.springframework.http.*;
 import org.springframework.test.annotation.DirtiesContext; // Reset context between tests if needed

 import java.math.BigDecimal;
 import java.util.Map;

 import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Start server on random port
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Reset DB state etc.
 class ProductE2ETest {

     @LocalServerPort
     private int port;

     @Autowired
     private TestRestTemplate restTemplate;

     @Autowired // Inject repos only to setup test data, not part of the E2E validation logic itself
     private ProductRepo productRepo;
     @Autowired
     private CouponRepo couponRepo;

     private String productBaseUrl;
     private String couponBaseUrl;


     @BeforeEach
     void setUp() {
         productBaseUrl = "http://localhost:" + port + "/productapi/products";
         couponBaseUrl = "http://localhost:" + port + "/couponapi/coupons";

         // Clean up repos before test
         productRepo.deleteAll();
         couponRepo.deleteAll();

         // Setup required data (e.g., a coupon)
         Coupon coupon = new Coupon();
         coupon.setCode("E2ECOUPON");
         coupon.setDiscount(new BigDecimal("15.00"));
         coupon.setExpDate("2027-01-01");
         couponRepo.save(coupon);
     }

     @Test
     void testCreateProductE2E_WithValidCoupon() {
         // Arrange
         Product requestBody = new Product();
         requestBody.setName("E2E Monitor");
         requestBody.setPrice(new BigDecimal("300.00"));
         requestBody.setCouponCode("E2ECOUPON");

         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_JSON);
         HttpEntity<Product> request = new HttpEntity<>(requestBody, headers);

         // Act
         ResponseEntity<Product> response = restTemplate.postForEntity(productBaseUrl, request, Product.class);

         // Assert
         assertEquals(HttpStatus.CREATED, response.getStatusCode());
         Product createdProduct = response.getBody();
         assertNotNull(createdProduct);
         assertNotNull(createdProduct.getId());
         assertEquals("E2E Monitor", createdProduct.getName());
         assertEquals(0, new BigDecimal("300.00").compareTo(createdProduct.getPrice()));
         assertEquals("E2ECOUPON", createdProduct.getAppliedCouponCode());
         assertEquals(0, new BigDecimal("285.00").compareTo(createdProduct.getDiscountedPrice())); // 300 - 15
         assertEquals("E2ECOUPON", createdProduct.getCouponCode());
     }

     @Test
     void testCreateProductE2E_WithoutCoupon() {
         // Arrange
         Product requestBody = new Product();
         requestBody.setName("E2E Desk");
         requestBody.setPrice(new BigDecimal("250.00"));

         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_JSON);
         HttpEntity<Product> request = new HttpEntity<>(requestBody, headers);

         // Act
         ResponseEntity<Product> response = restTemplate.postForEntity(productBaseUrl, request, Product.class);

         // Assert
         assertEquals(HttpStatus.CREATED, response.getStatusCode());
         Product createdProduct = response.getBody();
         assertNotNull(createdProduct);
         assertEquals("E2E Desk", createdProduct.getName());
         assertEquals(0, new BigDecimal("250.00").compareTo(createdProduct.getPrice()));
         assertNull(createdProduct.getAppliedCouponCode());
         assertEquals(0, new BigDecimal("250.00").compareTo(createdProduct.getDiscountedPrice()));
         assertNull(createdProduct.getCouponCode());
     }

      @Test
      void testCreateProductE2E_WithInvalidCoupon() {
          // Arrange
          Product requestBody = new Product();
          requestBody.setName("E2E Chair");
          requestBody.setPrice(new BigDecimal("180.00"));
          requestBody.setCouponCode("NOSUCHCODE");

          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_JSON);
          HttpEntity<Product> request = new HttpEntity<>(requestBody, headers);

          // Act
          ResponseEntity<Map> response = restTemplate.postForEntity(productBaseUrl, request, Map.class); // Expect error response

          // Assert
          assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
          assertNotNull(response.getBody());
          //if response.getBody().get("message") is not null, check if it contains the expected error message
          if (response.getBody().get("message") != null) {
              assertTrue(response.getBody().get("message").toString().contains("Coupon code not found: NOSUCHCODE"));
          }
      }
     
      // Add to ProductE2ETest.java

 @Test
 void testDeleteProductE2E_WhenExists() {
     // Arrange: Create a product first to delete it
     Product p = new Product();
     p.setName("ToDelete"); p.setPrice(BigDecimal.TEN);
     Product createdProduct = productRepo.save(p);
     Long productId = createdProduct.getId();

     // Act
     ResponseEntity<Void> response = restTemplate.exchange(
         productBaseUrl + "/" + productId,
         HttpMethod.DELETE,
         null, // No request body for delete
         Void.class
     );

     // Assert
     assertEquals(HttpStatus.OK, response.getStatusCode());

     // Verify it's actually deleted
     assertFalse(productRepo.findById(productId).isPresent());
 }

 @Test
 void testDeleteProductE2E_WhenNotFound() {
      // Arrange: Ensure ID 999 does not exist
     Long productId = 999L;
     productRepo.deleteById(productId); // Just in case

      // Act
     ResponseEntity<Void> response = restTemplate.exchange(
         productBaseUrl + "/" + productId,
         HttpMethod.DELETE,
         null,
         Void.class
     );

      // Assert
     assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
 }

     // Add more E2E tests for GET, PUT, DELETE for both Products and Coupons...
 }