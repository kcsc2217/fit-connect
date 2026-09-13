package com.fitness.trainer.domain

import com.fitness.common.entity.BaseTimeEntity
import com.fitness.user.domain.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "trainer")
class Trainer(

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User,

    @Column(nullable = false)
    var name: String,

    var specialty: String? = null,

    @Column(name = "profile_bio", columnDefinition = "TEXT")
    var profileBio: String? = null,

    @Column(name = "instagram_url")
    var instagramUrl: String? = null,

    @Column(name = "location_info")
    var locationInfo: String? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}