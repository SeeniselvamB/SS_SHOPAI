package com.ss.shopai.product;

import com.ss.shopai.BaseIntegrationTest;
import com.ss.shopai.dto.request.CategoryRequest;
import com.ss.shopai.dto.request.ProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerTest extends BaseIntegrationTest {

    private String adminToken;
    private Long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAsAdmin();

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .name("Test Category " + System.nanoTime())
                .description("A category for product tests")
                .active(true)
                .build();

        String response = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        categoryId = objectMapper.readTree(response).path("data").path("id").asLong();
    }

    @Test
    void adminCreatesProduct_thenPublicCanViewIt() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Integration Test Widget")
                .sku("TEST-SKU-" + System.nanoTime())
                .brand("TestBrand")
                .description("A widget created during integration testing")
                .price(new BigDecimal("199.99"))
                .discountPercentage(new BigDecimal("10.00"))
                .stockQuantity(50)
                .categoryId(categoryId)
                .active(true)
                .build();

        String response = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.discountedPrice").value(179.99))
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(response).path("data").path("id").asLong();

        // Publicly viewable without auth
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Integration Test Widget"))
                .andExpect(jsonPath("$.data.inStock").value(true));
    }

    @Test
    void nonAdminCannotCreateProduct() throws Exception {
        String userToken = registerAndGetToken("Regular User", "regularuser1@example.com", "Password123");

        ProductRequest request = ProductRequest.builder()
                .name("Should Not Be Created")
                .sku("SHOULD-FAIL-" + System.nanoTime())
                .price(new BigDecimal("10.00"))
                .stockQuantity(1)
                .categoryId(categoryId)
                .build();

        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchProducts_returnsPagedResults() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageSize").value(5));
    }

    @Test
    void updateStock_reflectsInProductResponse() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Stock Test Product")
                .sku("STOCK-TEST-" + System.nanoTime())
                .price(new BigDecimal("50.00"))
                .stockQuantity(10)
                .categoryId(categoryId)
                .build();

        String response = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/admin/products/{id}/stock", productId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockQuantity\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(3));
    }

    @Test
    void getAllCategories_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(greaterThanOrEqualTo(1))));
    }
}