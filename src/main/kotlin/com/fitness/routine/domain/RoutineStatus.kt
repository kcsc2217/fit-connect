package com.fitness.routine.domain

/** 루틴 생성 상태. "오늘은 쉴래요"는 SKIPPED */
enum class RoutineStatus {
    PENDING,
    GENERATED,
    FALLBACK,
    FAILED,
    SKIPPED,
}
