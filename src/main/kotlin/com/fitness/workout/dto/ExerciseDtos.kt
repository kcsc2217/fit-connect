package com.fitness.workout.dto

import com.fitness.workout.domain.BodyPart
import com.fitness.workout.domain.Exercise
import com.fitness.workout.domain.LoadType

/** 종목 사전 한 항목. 앱이 기기에 저장해 두고 오프라인에서도 종목을 고른다 */
data class ExerciseResponse(
    val id: Long,
    val code: String,
    val nameKo: String,
    val bodyPart: BodyPart,
    val loadType: LoadType,
    val defaultRestSec: Short,
) {
    companion object {
        fun from(e: Exercise) = ExerciseResponse(e.id!!, e.code, e.nameKo, e.bodyPart, e.loadType, e.defaultRestSec)
    }
}
