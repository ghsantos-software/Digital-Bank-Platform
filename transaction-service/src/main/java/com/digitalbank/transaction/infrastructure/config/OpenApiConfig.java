package com.digitalbank.transaction.infrastructure.config;

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

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Service API")
                        .description("""
                                Financial transaction processing microservice — Digital Bank Platform.

                                **Authentication:** Bearer JWT via Keycloak.
                                Obtain a token:
                                ```
                                POST http://localhost:8180/realms/digital-bank/protocol/openid-connect/token
                                Content-Type: application/x-www-form-urlencoded

                                grant_type=password&client_id=digital-bank-client
                                  &username=customer1&password=Customer@123
                                ```
                                Copy the `access_token` and paste it in the **Authorize** button above.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Digital Bank Platform")
                                .url("https://github.com/your-repo/digital-bank-platform")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Provide the JWT token from Keycloak")));
    }
}
