package com.fitness.challenge.domain

/** 신청 상태. 취소 후 재신청은 새 행이 아니라 이 상태를 바꾼다 */
enum class ParticipationStatus {
    HOLD,
    APPLIED,
    WAITING,
    CANCELED,
    EXPIRED,
}
