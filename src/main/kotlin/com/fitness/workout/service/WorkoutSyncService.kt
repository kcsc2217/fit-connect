package com.fitness.workout.service

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.common.exception.InvalidInputException
import com.fitness.user.service.UserService
import com.fitness.workout.dto.FailReason
import com.fitness.workout.dto.ItemResult
import com.fitness.workout.dto.ItemType
import com.fitness.workout.dto.Outcome
import com.fitness.workout.dto.SyncRequest
import com.fitness.workout.dto.SyncResponse
import com.fitness.workout.repository.WorkoutSyncJdbcRepository
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 운동 기록 묶음 동기화 (계획 011).
 * 항목마다 (user_id, client_id)로 upsert하고, 수정 시각이 더 새로운 쪽이 이긴다(last-write-wins).
 * 삭제된 행(tombstone)은 되살리지 않는다. 한 묶음은 한 트랜잭션이고, 검증 실패는 그 항목만 FAILED로 돌려준다.
 */
@Service
class WorkoutSyncService(
    private val repo: WorkoutSyncJdbcRepository,
    private val validator: SyncItemValidator,
    private val exerciseService: ExerciseService,
    private val userService: UserService,
    private val clock: Clock,
    private val redissonClient: RedissonClient,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${fitness.workout.sync-lock-wait:5s}") private val lockWait: Duration,
) {
    /**
     * 항목마다 upsert(last-write-wins, tombstone)하고 요청 순서대로 결과를 돌려준다.
     *
     * 같은 회원의 동기화는 Redis 락으로 줄 세운다. 락 없이 같은 항목이 동시에 오면, 중복 키로 실패한 INSERT가
     * 기존 행에 공유(S) 잠금을 남기고 이어지는 UPDATE가 배타(X) 잠금을 기다리며 서로 막혀 MySQL 데드락이 난다
     * (10건 동시 전송 중 8건 실패로 확인). 락은 트랜잭션 시작 전에 잡고 커밋 뒤에 푼다: 커밋 전에 풀면 다음 요청이
     * 아직 커밋되지 않은 행과 다시 부딪힌다. 다른 회원끼리는 키가 달라 기다리지 않는다. DB 유니크는 최후 안전망이다.
     */
    fun sync(userId: Long, request: SyncRequest): SyncResponse {
        val total = request.sessions.size + request.sets.size
        if (total !in 1..MAX_ITEMS) {
            throw InvalidInputException("items", "한 번에 1~${MAX_ITEMS}건만 보낼 수 있습니다")
        }
        val lock = redissonClient.getLock(lockKey(userId))
        // 시간을 정하지 않으면 Redisson watchdog이 30초마다 연장하고, 서버가 죽으면 30초 뒤 풀린다
        if (!lock.tryLock(lockWait.toMillis(), TimeUnit.MILLISECONDS)) throw BusinessException(ErrorCode.SYNC_BUSY)
        try {
            return transactionTemplate.execute { syncInTransaction(userId, request) }!!
        } finally {
            lock.unlock()
        }
    }

    private fun syncInTransaction(userId: Long, request: SyncRequest): SyncResponse {
        userService.requireRecordable(userId)

        val now = clock.instant()
        val results = HashMap<Pair<ItemType, UUID>, ItemResult>()

        // 1) 세션 먼저: 같은 묶음의 세트가 이 세션들을 가리킬 수 있다.
        //    client_id 순으로 처리해 겹치는 묶음끼리 잠금 순서를 맞춘다 (데드락 줄이기)
        val existingSessions = repo.findSessionClientIds(userId, request.sessions.map { it.clientId })
        for (s in request.sessions.sortedBy { it.clientId }) {
            val reason = validator.session(s)
            val outcome = reason?.let { Outcome.FAILED } ?: upsert(
                exists = s.clientId in existingSessions,
                insert = { repo.insertSession(userId, s, clamp(s.clientUpdatedAt, now), now) },
                update = { repo.updateSession(userId, s, clamp(s.clientUpdatedAt, now), now) },
            )
            results[ItemType.SESSION to s.clientId] = ItemResult(s.clientId, ItemType.SESSION, outcome, reason)
        }

        // 2) 세트: 가리키는 세션의 서버 id와 활성 종목을 한 번에 읽어 둔다
        val sessionIds = repo.findSessionIds(userId, request.sets.map { it.sessionClientId }.toSet())
        val activeExercises = if (request.sets.isEmpty()) emptySet() else exerciseService.activeIds()
        val existingSets = repo.findSetClientIds(userId, request.sets.map { it.clientId })
        for (t in request.sets.sortedBy { it.clientId }) {
            val sessionId = sessionIds[t.sessionClientId]
            val reason = validator.set(t) ?: when {
                t.exerciseId !in activeExercises -> FailReason.UNKNOWN_EXERCISE
                sessionId == null -> FailReason.SESSION_NOT_FOUND
                else -> null
            }
            val outcome = reason?.let { Outcome.FAILED } ?: upsert(
                exists = t.clientId in existingSets,
                insert = { repo.insertSet(userId, sessionId!!, t, clamp(t.clientUpdatedAt, now), now) },
                update = { repo.updateSet(userId, t, clamp(t.clientUpdatedAt, now), now) },
            )
            results[ItemType.SET to t.clientId] = ItemResult(t.clientId, ItemType.SET, outcome, reason)
        }

        // 요청 순서대로 (세션들, 세트들)
        val ordered = request.sessions.map { results.getValue(ItemType.SESSION to it.clientId) } +
            request.sets.map { results.getValue(ItemType.SET to it.clientId) }
        return SyncResponse(ordered)
    }

    /**
     * 없을 것 같으면 INSERT를 먼저 시도하고, 다른 요청이 먼저 넣었으면(중복 키) 조건부 UPDATE로 넘어간다.
     * MySQL은 유니크 위반을 그 문장만 되돌리므로, 예외를 잡고 같은 트랜잭션에서 계속 진행할 수 있다.
     */
    private fun upsert(exists: Boolean, insert: () -> Unit, update: () -> Int): Outcome {
        if (!exists) {
            try {
                insert()
                return Outcome.APPLIED
            } catch (e: DuplicateKeyException) {
                // 경쟁: 다른 요청이 같은 항목을 방금 넣었다. 아래 조건부 UPDATE로 판정한다
            }
        }
        return if (update() == 1) Outcome.APPLIED else Outcome.IGNORED
    }

    // 미래로 틀어진 기기 시계가 이후 모든 수정을 이기지 않게, 서버 시각보다 5분 넘게 미래면 서버 시각으로 낮춘다
    private fun clamp(clientUpdatedAt: Instant, now: Instant): Instant =
        if (clientUpdatedAt > now.plus(MAX_CLOCK_SKEW)) now else clientUpdatedAt

    companion object {
        const val MAX_ITEMS = 100

        fun lockKey(userId: Long) = "workout-sync:$userId"
        private val MAX_CLOCK_SKEW: Duration = Duration.ofMinutes(5)
    }
}
