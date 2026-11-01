package com.fitness.user.domain

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

/** FCM 푸시 토큰. 한 사용자가 여러 기기를 쓸 수 있다. FCM이 만료 응답을 주면 행을 지운다. */
@Entity
@Table(
    name = "device_token",
    uniqueConstraints = [UniqueConstraint(name = "uk_device_token_token", columnNames = ["token"])],
    indexes = [Index(name = "idx_device_token_user", columnList = "user_id")],
)
class DeviceToken(

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val platform: Platform,

    // 앱 실행 시 갱신
    @Column(nullable = false)
    var lastSeenAt: Instant,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
