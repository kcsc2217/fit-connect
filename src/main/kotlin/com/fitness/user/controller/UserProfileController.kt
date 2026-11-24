package com.fitness.user.controller

import com.fitness.user.dto.ProfileRequest
import com.fitness.user.dto.ProfileResponse
import com.fitness.user.service.UserProfileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "회원")
@RestController
@RequestMapping("/api/v1/users/me/profile")
class UserProfileController(private val userProfileService: UserProfileService) {

    @Operation(
        summary = "온보딩 프로필 제출(덮어쓰기)",
        description = "프로필·장비를 덮어쓰고 회원이 ONBOARDING이면 ACTIVE로 바꾼다. 같은 요청을 다시 보내도 결과가 같다.",
        security = [SecurityRequirement(name = "bearer")],
    )
    @PutMapping
    fun submit(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: ProfileRequest,
    ): ProfileResponse = userProfileService.submit(userId, request)

    @Operation(summary = "내 프로필 조회", security = [SecurityRequirement(name = "bearer")])
    @GetMapping
    fun get(@Parameter(hidden = true) @AuthenticationPrincipal userId: Long): ProfileResponse =
        userProfileService.get(userId)
}
