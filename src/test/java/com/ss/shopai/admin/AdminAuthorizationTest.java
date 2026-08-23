package com.ss.shopai.admin;

import com.ss.shopai.BaseIntegrationTest;
import com.ss.shopai.dto.request.CategoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminAuthorizationTest extends BaseIntegrationTest {

    @Test
    void adminDashboard_accessibleToAdmin() throws Exception {
        String adminToken = loginAsAdmin();

        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalProducts").isNumber());
    }

    @Test
    void adminDashboard_rejectedForRegularUser() throws Exception {
        String userToken = registerAndGetToken("Not Admin", "notadmin1@example.com", "Password123");

        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDashboard_rejectedWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminUserManagement_accessibleToAdminOnly() throws Exception {
        String adminToken = loginAsAdmin();
        String userToken = registerAndGetToken("Managed User", "managed1@example.com", "Password123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCategoryManagement_createAndDelete() throws Exception {
        String adminToken = loginAsAdmin();

        CategoryRequest request = CategoryRequest.builder()
                .name("Admin Auth Test Category " + System.nanoTime())
                .active(true)
                .build();

        String response = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long categoryId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void adminOrderManagement_rejectedForRegularUser() throws Exception {
        String userToken = registerAndGetToken("Order Blocker", "orderblocker1@example.com", "Password123");

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReviewManagement_rejectedForRegularUser() throws Exception {
        String userToken = registerAndGetToken("Review Blocker", "reviewblocker1@example.com", "Password123");

        mockMvc.perform(get("/api/admin/reviews")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void toggleUserEnabled_disablesAccount_thenLoginFails() throws Exception {
        String adminToken = loginAsAdmin();
        registerAndGetToken("Toggle User", "toggleuser1@example.com", "Password123");

        // Find the newly created user's id via admin search
        String searchResponse = mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .param("keyword", "toggleuser1@example.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long targetUserId = objectMapper.readTree(searchResponse)
                .path("data").path("content").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/admin/users/{id}/toggle-enabled", targetUserId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        String loginBody = objectMapper.writeValueAsString(
                com.ss.shopai.dto.request.LoginRequest.builder()
                        .email("toggleuser1@example.com")
                        .password("Password123")
                        .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isBadRequest());
    }
}