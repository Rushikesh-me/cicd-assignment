 package com.tus.product;

 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.tus.product.model.Coupon;
 import com.tus.product.model.Product;
 import com.tus.product.repos.CouponRepo;
 import com.tus.product.repos.ProductRepo;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
 import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.http.MediaType;
 import org.springframework.test.web.servlet.MockMvc;
 import org.springframework.test.web.servlet.MvcResult;
 import org.springframework.transaction.annotation.Transactional; // Important for test isolation

 import java.math.BigDecimal;

 import static org.hamcrest.Matchers.is;
 import static org.hamcrest.Matchers.notNullValue;
 import static org.junit.jupiter.api.Assertions.*;
 import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
 import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

 @SpringBootTest // Loads the full application context
 @AutoConfigureMockMvc // Configures MockMvc for calling controllers
 @Transactional // Rollback transactions after each test
 class ProductIntegrationTest {

     @Autowired
     private MockMvc mockMvc;

     @Autowired
     private ObjectMapper objectMapper; // For converting objects to JSON

     @Autowired
     private ProductRepo productRepo;

     @Autowired
     private CouponRepo couponRepo;

     @BeforeEach
     void setUpDatabase() {
         // Clean up potentially existing data if @Transactional is not sufficient
         productRepo.deleteAll();
         couponRepo.deleteAll();

         // Pre-load a coupon for testing discounts
         Coupon coupon = new Coupon();
         coupon.setCode("DISCOUNT20");
         coupon.setDiscount(new BigDecimal("20.00"));
         coupon.setExpDate("2026-01-01");
         couponRepo.save(coupon);
     }

     @Test
     void testCreateProduct_WithoutCoupon_Integration() throws Exception {
         Product newProduct = new Product();
         newProduct.setName("Integration Laptop");
         newProduct.setDescription("From integration test");
         newProduct.setPrice(new BigDecimal("1200.00"));
         // No coupon code set

         mockMvc.perform(post("/productapi/products")
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(objectMapper.writeValueAsString(newProduct)))
                 .andExpect(status().isCreated())
                 .andExpect(jsonPath("$.id", notNullValue()))
                 .andExpect(jsonPath("$.name", is("Integration Laptop")))
                 .andExpect(jsonPath("$.price", is(1200.00))) // Check original price
                 .andExpect(jsonPath("$.appliedCouponCode").isEmpty()) // No coupon applied
                 .andExpect(jsonPath("$.discountedPrice", is(1200.00))); // Discounted = original

         // Verify in DB
         assertEquals(1, productRepo.count());
         Product savedProduct = productRepo.findAll().get(0);
         assertEquals("Integration Laptop", savedProduct.getName());
         assertNull(savedProduct.getAppliedCouponCode());
         assertEquals(0, new BigDecimal("1200.00").compareTo(savedProduct.getPrice()));
     }

     @Test
     void testCreateProduct_WithValidCoupon_Integration() throws Exception {
         Product newProduct = new Product();
         newProduct.setName("Integration Keyboard");
         newProduct.setDescription("Mechanical");
         newProduct.setPrice(new BigDecimal("150.00"));
         newProduct.setCouponCode("DISCOUNT20"); // Use pre-loaded coupon

         MvcResult result = mockMvc.perform(post("/productapi/products")
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(objectMapper.writeValueAsString(newProduct)))
                 .andExpect(status().isCreated())
                 .andExpect(jsonPath("$.id", notNullValue()))
                 .andExpect(jsonPath("$.name", is("Integration Keyboard")))
                 .andExpect(jsonPath("$.price", is(150.00))) // Original price
                 .andExpect(jsonPath("$.appliedCouponCode", is("DISCOUNT20"))) // Applied code
                 .andExpect(jsonPath("$.discountedPrice", is(130.00))) // Discounted price
                 .andExpect(jsonPath("$.couponCode", is("DISCOUNT20"))) // Input code in response
                 .andReturn();

         // Verify in DB
         assertEquals(1, productRepo.count());
         Product savedProduct = productRepo.findAll().get(0);
         assertEquals("Integration Keyboard", savedProduct.getName());
         assertEquals("DISCOUNT20", savedProduct.getAppliedCouponCode()); // Check applied code stored
         assertEquals(0, new BigDecimal("150.00").compareTo(savedProduct.getPrice())); // Original price stored
     }

      @Test
     void testCreateProduct_WithInvalidCoupon_Integration() throws Exception {
         Product newProduct = new Product();
         newProduct.setName("Integration Mouse");
         newProduct.setPrice(new BigDecimal("50.00"));
         newProduct.setCouponCode("FAKECOUPON");

         mockMvc.perform(post("/productapi/products")
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(objectMapper.writeValueAsString(newProduct)))
                 .andExpect(status().isBadRequest()); // Expecting 400 Bad Request

         // Verify nothing was saved
         assertEquals(0, productRepo.count());
     }
 }