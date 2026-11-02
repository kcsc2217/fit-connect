package com.fitness.support

import com.fitness.analytics.domain.EventLog
import com.fitness.analytics.domain.EventName
import com.fitness.challenge.domain.Challenge
import com.fitness.challenge.domain.ChallengeParticipation
import com.fitness.challenge.domain.ParticipationStatus
import java.time.Instant
import java.time.LocalDate

/** 챌린지 신청과 제품 지표 이벤트 픽스처 */
object ChallengeFixtures {

    private val startsOn: LocalDate = LocalDate.of(2026, 12, 1)
    private val now: Instant = Instant.parse("2026-11-01T23:00:00Z")

    fun challenge(
        startsOn: LocalDate = this.startsOn,
        endsOn: LocalDate = this.startsOn.plusDays(13),
        openAt: Instant = now,
    ) = Challenge(
        title = "2주 스쿼트 챌린지",
        startsOn = startsOn,
        endsOn = endsOn,
        capacity = 100,
        waitlistCapacity = 50,
        openAt = openAt,
    )

    fun participation(challengeId: Long, userId: Long) = ChallengeParticipation(
        challengeId = challengeId,
        userId = userId,
        status = ParticipationStatus.APPLIED,
        appliedAt = now,
    )

    fun event(properties: Map<String, Any?>? = null) = EventLog(
        userId = 1L,
        name = EventName.ROUTINE_VIEW,
        properties = properties,
        occurredAt = now,
    )
}
