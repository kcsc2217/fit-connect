package com.fitness.workout.service

import com.fitness.common.type.Equipment
import com.fitness.workout.domain.BodyPart
import com.fitness.workout.domain.Exercise
import com.fitness.workout.repository.ExerciseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 종목 사전의 공개 창구. 다른 모듈(routine 등)은 저장소가 아니라 이 서비스를 쓴다. */
@Service
@Transactional(readOnly = true)
class ExerciseService(
    private val exerciseRepository: ExerciseRepository,
) {

    /**
     * LLM에 넘길 후보 종목: 사용자가 필요한 장비를 모두 가진 활성 종목. 맨몸 종목은 항상 포함된다.
     * 사용자 장비는 호출하는 쪽이 user 모듈에서 받아 넘긴다 (user 테이블을 조인하지 않음).
     */
    fun findCandidates(bodyParts: Collection<BodyPart>, equipments: Collection<Equipment>): List<Exercise> {
        if (bodyParts.isEmpty()) return emptyList()
        // 장비 목록이 비면 NOT IN ()이 잘못된 SQL이 되므로 맨몸 종목 쿼리로 분기
        return if (equipments.isEmpty()) {
            exerciseRepository.findActiveBodyweight(bodyParts)
        } else {
            exerciseRepository.findActiveOwning(bodyParts, equipments)
        }
    }
}
