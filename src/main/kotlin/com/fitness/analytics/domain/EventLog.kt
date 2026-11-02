package com.fitness.analytics.domain

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
import java.time.Instant

/** 제품 지표용 이벤트 기록. 쓰기 전용이라 저장 후 수정하지 않는다. */
@Entity
@Table(
    name = "event_log",
    indexes = [
        Index(name = "idx_event_log_name_occurred", columnList = "name, occurred_at"),
        Index(name = "idx_event_log_user_occurred", columnList = "user_id, occurred_at"),
    ],
)
class EventLog(

    // 로그인 전 이벤트는 NULL
    val userId: Long?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val name: EventName,

    // 이벤트마다 키가 달라 JSON으로 둔다
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    val properties: Map<String, Any?>? = null,

    @Column(nullable = false)
    val occurredAt: Instant,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
