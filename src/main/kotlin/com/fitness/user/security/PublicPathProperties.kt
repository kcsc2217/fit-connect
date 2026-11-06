package com.fitness.user.security

import org.springframework.boot.context.properties.ConfigurationProperties

/** 환경별로 추가로 공개할 경로 (예: 테스트 전용 컨트롤러 경로). 운영은 비어 있다 */
@ConfigurationProperties("fitness.security")
data class PublicPathProperties(
    val extraPublicPaths: List<String> = emptyList(),
)
