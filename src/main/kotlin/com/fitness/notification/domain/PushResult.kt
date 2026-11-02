package com.fitness.notification.domain

/** 발송 결과 */
enum class PushResult {
    PENDING,
    SENT,
    FAILED,
    TOKEN_EXPIRED,
}
