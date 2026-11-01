package com.fitness.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * 모든 테이블의 created_at, updated_at (DATETIME(6), UTC).
 * 시각은 시간대가 없는 Instant로 다룬다. 사용자 기준 날짜가 필요하면 별도 DATE 컬럼을 둔다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant? = null
        protected set
}
