package com.fitness.user.controller

import com.fitness.user.dto.MeResponse
import com.fitness.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "회원")
@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    // userId는 JwtAuthenticationFilter가 SecurityContext에 넣은 principal. 서비스에는 Long만 넘긴다
    @Operation(summary = "내 정보 조회", security = [SecurityRequirement(name = "bearer")])
    @GetMapping("/me")
    fun me(@Parameter(hidden = true) @AuthenticationPrincipal userId: Long): MeResponse = userService.getMe(userId)
}
