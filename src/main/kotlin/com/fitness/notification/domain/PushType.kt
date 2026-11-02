package com.fitness.notification.domain

/** 알림 종류. 종류마다 push_log.ref_id가 가리키는 대상이 다르다 */
enum class PushType {
    // ref_id = daily_routine.id
    DAILY_ROUTINE,

    // ref_id = challenge.id
    CHALLENGE_PROMOTED,
}
