package com.robert.instrumentresultsservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI instrumentResultsOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Instrument Results Service API")
                .version("v1")
                .description("API for creating instrument runs and submitting measurements."));
    }
}
