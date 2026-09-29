package com.example.certificates.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI certificateServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Certificate Service API")
                .description("Issues and looks up immutable training certificates.")
                .version("1.0.0")
                .license(new License().name("Proprietary")));
    }
}
