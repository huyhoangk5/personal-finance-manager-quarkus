package com.finance.pfm.config;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@OpenAPIDefinition(
        info = @Info(
                title = "Personal Finance Manager API",
                version = "1.0.0",
                description = "API quan ly tai chinh ca nhan voi JWT authentication",
                contact = @Contact(name = "Finance Manager")
        )
)
@SecurityScheme(
        securitySchemeName = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Nhap JWT access token dang Bearer"
)
public class OpenApiConfig {
}
