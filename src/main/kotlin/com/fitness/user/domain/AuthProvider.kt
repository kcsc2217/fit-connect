package com.fitness.user.domain

/** 소셜 로그인 제공자 */
enum class AuthProvider {
    KAKAO,
    GOOGLE,
    APPLE,
    DEV, // 개발·테스트 전용 로그인. fitness.auth.dev-login.enabled일 때만 쓰인다
}
