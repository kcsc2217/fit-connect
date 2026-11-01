package com.fitness.user.domain

import com.fitness.common.entity.BaseTimeEntity
import com.fitness.common.type.Equipment
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.io.Serializable

/** 사용자가 쓸 수 있는 장비. 후보 종목 필터의 입력. */
@Entity
@Table(name = "user_equipment")
class UserEquipment(

    @EmbeddedId
    val id: UserEquipmentId,

) : BaseTimeEntity()

// 복합 키. kotlin-jpa 플러그인이 기본 생성자를 만들 수 있도록 @IdClass 대신 @Embeddable을 쓴다.
@Embeddable
data class UserEquipmentId(

    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val equipment: Equipment,

) : Serializable
