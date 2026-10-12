package com.dandi.nyummy.config.openapi

import com.dandi.nyummy.exception.ErrorResponse
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    init {
        // enum을 필드마다 인라인으로 펼치지 않고 이름 있는 스키마(#/components/schemas/InquiryCategory 등)로 뽑아
        // Swagger UI 하단 Schemas에서 enum 값과 설명을 한곳에서 볼 수 있게 한다.
        ModelResolver.enumsAsRef = true
    }

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .components(
            Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                )
                .schemas(ModelConverters.getInstance().read(ErrorResponse::class.java)),
        )
        .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
}
