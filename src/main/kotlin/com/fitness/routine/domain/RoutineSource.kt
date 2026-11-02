package com.fitness.routine.domain

/** 루틴을 만든 경로 */
enum class RoutineSource {
    LLM,
    FALLBACK,
    REGENERATED,
}
