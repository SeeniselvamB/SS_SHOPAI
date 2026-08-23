package com.ss.shopai.cart;

import com.ss.shopai.BaseIntegrationTest;
import com.ss.shopai.dto.request.AddToCartRequest;
import com.ss.shopai.dto.request.CategoryRequest;
import com.ss.shopai.dto.request.ProductRequest;
import com.ss.shopai.dto.request.UpdateCartItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartControllerTest extends BaseIntegrationTest {

    private String userToken;
    private Long productId;

    @BeforeEach
    void setUp() throws Exception {
        userToken = registerAndGetToken("Cart User", "cartuser1@example.com", "Password123");
        String adminToken = loginAsAdmin();

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .name("Cart Test Category " + System.nanoTime())
                .active(true)
                .build();

        String categoryResponse = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = objectMapper.readTree(categoryResponse).path("data").path("id").asLong();

        ProductRequest productRequest = ProductRequest.builder()
                .name("Cart Test Product")
                .sku("CART-TEST-" + System.nanoTime())
                .price(new BigDecimal("100.00"))
                .stockQuantity(5)
                .categoryId(categoryId)
                .build();

        String productResponse = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        productId = objectMapper.readTree(productResponse).path("data").path("id").asLong();
    }

    @Test
    void addItem_thenGetCart_reflectsQuantityAndSubtotal() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(productId)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(200.00));

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    void addItem_exceedingStock_returnsBadRequest() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(productId)
                .quantity(999)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCartItem_changesQuantity() throws Exception {
        AddToCartRequest addRequest = AddToCartRequest.builder().productId(productId).quantity(1).build();

        String response = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long cartItemId = objectMapper.readTree(response).path("data").path("items").get(0).path("id").asLong();

        UpdateCartItemRequest updateRequest = UpdateCartItemRequest.builder().quantity(3).build();

        mockMvc.perform(put("/api/cart/items/{itemId}", cartItemId)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));
    }

    @Test
    void removeItem_emptiesCart() throws Exception {
        AddToCartRequest addRequest = AddToCartRequest.builder().productId(productId).quantity(1).build();

        String response = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long cartItemId = objectMapper.readTree(response).path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(delete("/api/cart/items/{itemId}", cartItemId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void cart_withoutAuth_isRejected() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }
}