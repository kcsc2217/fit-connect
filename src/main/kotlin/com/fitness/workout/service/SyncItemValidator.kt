package com.fitness.workout.service

import com.fitness.workout.dto.FailReason
import com.fitness.workout.dto.SessionChange
import com.fitness.workout.dto.SetChange
import org.springframework.stereotype.Component
import java.math.BigDecimal

/** 동기화 항목 값 검증. 틀리면 실패 이유를, 맞으면 null을 돌려준다 (명세 "값 검증") */
@Component
class SyncItemValidator {

    fun session(s: SessionChange): FailReason? = when {
        s.endedAt != null && s.endedAt < s.startedAt -> FailReason.INVALID_VALUE
        (s.conditionNote?.length ?: 0) > MAX_NOTE_LENGTH -> FailReason.INVALID_VALUE
        else -> null
    }

    fun set(t: SetChange): FailReason? = when {
        // DECIMAL(6,2): 소수 셋째 자리는 DB가 조용히 반올림하므로 막는다
        t.weightKg < BigDecimal.ZERO || t.weightKg > MAX_WEIGHT || t.weightKg.scale() > 2 -> FailReason.INVALID_VALUE
        t.reps !in REPS_RANGE -> FailReason.INVALID_VALUE
        t.rpe != null && !validRpe(t.rpe) -> FailReason.INVALID_VALUE
        t.exerciseOrder !in ORDER_RANGE || t.setOrder !in ORDER_RANGE -> FailReason.INVALID_VALUE
        else -> null
    }

    // 1.0~10.0, 0.5 단위
    private fun validRpe(rpe: BigDecimal) =
        rpe >= BigDecimal.ONE && rpe <= BigDecimal.TEN && rpe.remainder(HALF).signum() == 0

    companion object {
        private val MAX_WEIGHT = BigDecimal("999.99")
        private val HALF = BigDecimal("0.5")
        private val REPS_RANGE = 1..999
        private val ORDER_RANGE = 0..Short.MAX_VALUE // SMALLINT
        private const val MAX_NOTE_LENGTH = 200
    }
}
