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
                    .title("나만의 트레이너 쌤 API")
                    .description("기록을 아는 AI 트레이너가 오늘의 운동과 식단을 짜 주는 앱의 백엔드")
                    .version("v0.0.1")
            )
}