package com.fitness.ptclass.domain

import com.fitness.common.entity.BaseTimeEntity
import com.fitness.trainer.domain.Trainer
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "pt_class")
class PtClass(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    var trainer: Trainer,

    @Column(nullable = false)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "class_type", nullable = false)
    var classType: ClassType,

    @Column(name = "total_capacity", nullable = false)
    var totalCapacity: Int,

    @Column(name = "open_time", nullable = false)
    var openTime: LocalDateTime,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}