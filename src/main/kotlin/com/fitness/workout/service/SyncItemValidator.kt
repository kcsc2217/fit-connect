package com.fitness.workout.service

import com.fitness.workout.dto.FailReason
import com.fitness.workout.dto.SessionChange
import com.fitness.workout.dto.SetChange
import org.springframework.stereotype.Component

/** 동기화 항목 값 검증. 틀리면 실패 이유를, 맞으면 null을 돌려준다 (명세 "값 검증") */
@Component
class SyncItemValidator {

    fun session(s: SessionChange): FailReason? = TODO("011 구현")

    fun set(t: SetChange): FailReason? = TODO("011 구현")
}
