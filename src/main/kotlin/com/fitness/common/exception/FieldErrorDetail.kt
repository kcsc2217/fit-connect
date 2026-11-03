package com.fitness.common.exception

/** 검증에 실패한 필드 하나. ProblemDetail의 errors 배열 원소. */
data class FieldErrorDetail(
    val field: String,
    val message: String,
)
