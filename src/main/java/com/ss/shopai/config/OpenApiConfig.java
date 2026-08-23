package com.ss.shopai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI shopAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopAI E-Commerce API")
                        .description("Backend REST API for ShopAI - a full-featured e-commerce platform " +
                                "with admin management, cart, wishlist, orders, payments, reviews, " +
                                "recommendations, and product comparison. Also prepared for future " +
                                "Voice Agent integration (Intent -> Search -> Recommendation -> Comparison).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Seeniselvam B")
                                .email("support@shopai.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT access token here (without the word 'Bearer')")));
    }
}