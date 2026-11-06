package com.fitness.user.controller

import com.fitness.user.dto.LoginRequest
import com.fitness.user.dto.LoginResponse
import com.fitness.user.dto.RefreshTokenRequest
import com.fitness.user.dto.TokenResponse
import com.fitness.user.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    @Operation(summary = "로그인·가입", description = "소셜 토큰으로 로그인한다. 처음이면 ONBOARDING 회원으로 가입한다")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse = authService.login(request)

    @Operation(summary = "토큰 갱신", description = "refresh로 새 access·refresh를 받는다. 쓴 refresh는 폐기된다")
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): TokenResponse =
        authService.refresh(request.refreshToken)

    @Operation(summary = "로그아웃", description = "refresh를 폐기한다. 이미 없어도 204")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: RefreshTokenRequest) {
        authService.logout(request.refreshToken)
    }
}
