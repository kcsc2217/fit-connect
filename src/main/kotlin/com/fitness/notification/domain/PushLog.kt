package com.fitness.notification.domain

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
import java.time.Instant

/** 알림 예약·발송 기록. */
@Entity
@Table(
    name = "push_log",
    // 같은 대상에 알림이 두 번 가지 않게. ref_id가 NULL이면 MySQL 유니크가 중복을 못 막으므로 NOT NULL
    uniqueConstraints = [UniqueConstraint(name = "uk_push_log_user_type_ref", columnNames = ["user_id", "type", "ref_id"])],
    // 발송 대상 조회
    indexes = [Index(name = "idx_push_log_result_scheduled", columnList = "result, scheduled_at")],
)
class PushLog(

    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: PushType,

    // 종류별 대상 id (루틴·챌린지). 모듈 간 참조라 FK 없음
    @Column(nullable = false)
    val refId: Long,

    @Column(nullable = false)
    var scheduledAt: Instant,

    var sentAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var result: PushResult = PushResult.PENDING,

    // 알림을 눌러 앱을 연 시각 (열람률 지표)
    var openedAt: Instant? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
