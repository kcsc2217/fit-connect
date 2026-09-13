package com.fitness.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Fitness Platform API")
                    .description("PT Class First-Come-First-Served Reservation & Fitness App")
                    .version("v0.0.1")
            )
}