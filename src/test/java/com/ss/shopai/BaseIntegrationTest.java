package com.ss.shopai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.shopai.dto.request.LoginRequest;
import com.ss.shopai.dto.request.RegisterRequest;
import com.ss.shopai.dto.response.ApiResponse;
import com.ss.shopai.dto.response.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Common scaffolding for all integration tests: a live Spring context, MockMvc, and
 * reusable helpers to register/login a regular user and log in the seeded admin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String ADMIN_EMAIL = "admin@shopai.com";
    protected static final String ADMIN_PASSWORD = "Admin@123";

    @BeforeEach
    void baseSetup() {
        // Intentionally empty — subclasses seed their own data per test as needed.
        // DataInitializer already guarantees the admin account exists on context startup.
    }

    protected String registerAndGetToken(String fullName, String email, String password) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName(fullName)
                .email(email)
                .password(password)
                .phoneNumber("9876543210")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return extractAccessToken(result);
    }

    protected String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return extractAccessToken(result);
    }

    protected String loginAsAdmin() throws Exception {
        return loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );
        if (response.getData() == null) {
            throw new IllegalStateException("Auth call did not return a token. Raw response: " + json);
        }
        return response.getData().getAccessToken();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}