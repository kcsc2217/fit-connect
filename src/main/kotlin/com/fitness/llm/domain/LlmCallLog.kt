package com.fitness.llm.domain

import com.fitness.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/** LLM 호출 로그. 쓰기 전용이라 저장 후 수정하지 않는다. */
@Entity
@Table(
    name = "llm_call_log",
    indexes = [
        Index(name = "idx_llm_call_log_feature_created", columnList = "feature, created_at"),
        Index(name = "idx_llm_call_log_prompt_created", columnList = "prompt_version, created_at"),
    ],
)
class LlmCallLog(

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val feature: LlmFeature,

    // 평가 실행은 NULL
    val userId: Long?,

    // 결과가 저장된 대상 (daily_routine.id 등)
    val refId: Long?,

    @Column(nullable = false, length = 30)
    val promptVersion: String,

    @Column(nullable = false, length = 60)
    val model: String,

    @Column(nullable = false)
    val inputTokens: Int,

    @Column(nullable = false)
    val outputTokens: Int,

    @Column(nullable = false)
    val latencyMs: Int,

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    val retryCount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val validationResult: ValidationResult,

    failureReason: String? = null,

    // 기본 루틴으로 대체됐는지
    @Column(nullable = false)
    val fallbackUsed: Boolean,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    // 외부 응답에서 와서 길이를 알 수 없다. 넘치면 저장 자체가 실패해 로그가 사라지므로 잘라 둔다
    @Column(length = FAILURE_REASON_MAX)
    val failureReason: String? = failureReason?.let(::truncate)

    companion object {
        const val FAILURE_REASON_MAX = 300

        // take()는 UTF-16 단위라 이모지(서로게이트 쌍)를 반으로 자를 수 있다. 짝 없는 앞쪽이 남으면 한 칸 더 뺀다
        private fun truncate(value: String): String {
            val cut = value.take(FAILURE_REASON_MAX)
            return if (cut.length == FAILURE_REASON_MAX && cut.last().isHighSurrogate()) cut.dropLast(1) else cut
        }
    }
}
