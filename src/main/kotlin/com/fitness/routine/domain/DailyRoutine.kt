package com.fitness.routine.domain

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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate

/**
 * 오늘의 루틴. 사용자당 하루 1개.
 * feedback을 여기 두는 이유: prompt_version과 같은 행에 있어 프롬프트 버전별 사용자 반응을 바로 비교할 수 있다.
 */
@Entity
@Table(
    name = "daily_routine",
    // 같은 날 루틴 중복 생성을 DB가 차단 (생성 스케줄러가 여러 대에서 돌거나 재시도돼도 1개)
    uniqueConstraints = [UniqueConstraint(name = "uk_daily_routine_user_date", columnNames = ["user_id", "routine_date"])],
    // 생성 대상·실패 조회
    indexes = [Index(name = "idx_daily_routine_date_status", columnList = "routine_date, status")],
)
class DailyRoutine(

    // 모듈 간 참조라 FK 없음
    @Column(nullable = false)
    val userId: Long,

    // 사용자 시간대 기준 날짜
    @Column(nullable = false)
    val routineDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: RoutineStatus,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var source: RoutineSource,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var splitDay: SplitDay,

    // LLM 코칭 문구. 길이 초과는 자르지 않고 LLM 응답 검증 실패로 처리한다
    @Column(length = 300)
    var coachNote: String? = null,

    var estimatedMinutes: Short? = null,

    // 예: routine-v3
    @Column(length = 30)
    var promptVersion: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    var feedback: RoutineFeedback? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    // "다른 루틴" 요청 횟수 (하루 2회 제한). 한도 검사는 재생성 기능에서
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    var regenCount: Int = 0
}
