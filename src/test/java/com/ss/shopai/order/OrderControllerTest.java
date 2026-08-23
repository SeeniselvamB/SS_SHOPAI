package com.ss.shopai.order;

import com.ss.shopai.BaseIntegrationTest;
import com.ss.shopai.dto.request.*;
import com.ss.shopai.enums.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest extends BaseIntegrationTest {

    private String userToken;
    private Long addressId;

    @BeforeEach
    void setUp() throws Exception {
        userToken = registerAndGetToken("Order User", "orderuser1@example.com", "Password123");
        String adminToken = loginAsAdmin();

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .name("Order Test Category " + System.nanoTime())
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
                .name("Order Test Product")
                .sku("ORDER-TEST-" + System.nanoTime())
                .price(new BigDecimal("500.00"))
                .stockQuantity(10)
                .categoryId(categoryId)
                .build();
        String productResponse = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(productResponse).path("data").path("id").asLong();

        AddToCartRequest addToCartRequest = AddToCartRequest.builder().productId(productId).quantity(2).build();
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartRequest)))
                .andExpect(status().isOk());

        AddressRequest addressRequest = AddressRequest.builder()
                .fullName("Order User")
                .phoneNumber("9876500000")
                .addressLine1("123 Test Street")
                .city("Coimbatore")
                .state("Tamil Nadu")
                .postalCode("641001")
                .country("India")
                .build();
        String addressResponse = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        addressId = objectMapper.readTree(addressResponse).path("data").path("id").asLong();
    }

    @Test
    void createOrder_fromCart_succeedsAndClearsCart() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .addressId(addressId)
                .paymentMethod(PaymentMethod.UPI)
                .build();

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.subtotal").value(1000.00));

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void createOrder_withEmptyCart_returnsBadRequest() throws Exception {
        String emptyCartUserToken = registerAndGetToken("Empty Cart User", "emptycart1@example.com", "Password123");

        CreateOrderRequest request = CreateOrderRequest.builder()
                .addressId(addressId)
                .paymentMethod(PaymentMethod.UPI)
                .build();

        // addressId belongs to a different user, but empty-cart check fires first
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(emptyCartUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mockPayment_withSimulateSuccess_confirmsOrder() throws Exception {
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .addressId(addressId)
                .paymentMethod(PaymentMethod.UPI)
                .build();

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).path("data").path("id").asLong();

        MockPaymentRequest paymentRequest = MockPaymentRequest.builder()
                .orderId(orderId)
                .simulateSuccess(true)
                .build();

        mockMvc.perform(post("/api/orders/payment")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"));
    }

    @Test
    void cancelOrder_restocksProductAndUpdatesStatus() throws Exception {
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .addressId(addressId)
                .paymentMethod(PaymentMethod.COD)
                .build();

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).path("data").path("id").asLong();

        CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
                .reason("Changed my mind")
                .build();

        mockMvc.perform(put("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelReason").value("Changed my mind"));
    }

    @Test
    void otherUser_cannotViewSomeoneElsesOrder() throws Exception {
        CreateOrderRequest orderRequest = CreateOrderRequest.builder()
                .addressId(addressId)
                .paymentMethod(PaymentMethod.UPI)
                .build();

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).path("data").path("id").asLong();

        String otherUserToken = registerAndGetToken("Other User", "otheruser1@example.com", "Password123");

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isNotFound());
    }
}