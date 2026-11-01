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

/** 소셜 계정. 한 사용자가 여러 소셜 계정을 연결할 수 있다. */
@Entity
@Table(
    name = "social_account",
    uniqueConstraints = [
        // 같은 소셜 계정으로 중복 가입 방지
        UniqueConstraint(name = "uk_social_account_provider_user", columnNames = ["provider", "provider_user_id"]),
    ],
    indexes = [Index(name = "idx_social_account_user", columnList = "user_id")],
)
class SocialAccount(

    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: AuthProvider,

    @Column(nullable = false, length = 100)
    val providerUserId: String,

    var email: String? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
