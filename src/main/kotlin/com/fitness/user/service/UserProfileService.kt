package com.fitness.user.service

import com.fitness.user.dto.ProfileRequest
import com.fitness.user.dto.ProfileResponse
import org.springframework.stereotype.Service

/** 온보딩 프로필 제출·조회 (계획 009) */
@Service
class UserProfileService {

    /** 프로필·장비를 덮어쓰고, 회원이 ONBOARDING이면 ACTIVE로 바꾼다. 같은 요청을 다시 보내도 결과가 같다 */
    fun submit(userId: Long, request: ProfileRequest): ProfileResponse = TODO("009 구현")

    /** 온보딩 전이면 NOT_FOUND */
    fun get(userId: Long): ProfileResponse = TODO("009 구현")
}
