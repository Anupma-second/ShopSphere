package com.shopsphere.ecommerce.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 *
 * To call protected endpoints: POST /api/auth/login, copy "accessToken",
 * click "Authorize" (top right) and paste it (without "Bearer ").
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ShopSphere API",
                version = "v1",
                description = "Multi-vendor e-commerce marketplace: catalogue, cart, "
                        + "orders, Razorpay payments, seller and admin dashboards."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}