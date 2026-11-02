package com.fitness.llm.domain

/** LLM 응답 검증 결과 */
enum class ValidationResult {
    PASS,
    SCHEMA_ERROR,
    RULE_VIOLATION,
    TIMEOUT,
}
