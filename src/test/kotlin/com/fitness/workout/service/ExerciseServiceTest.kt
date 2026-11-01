package com.fitness.workout.service

import com.fitness.common.type.Equipment
import com.fitness.support.RepositoryTest
import com.fitness.support.WorkoutFixtures
import com.fitness.workout.domain.BodyPart
import com.fitness.workout.domain.Exercise
import com.fitness.workout.domain.ExerciseEquipment
import com.fitness.workout.domain.ExerciseEquipmentId
import com.fitness.workout.repository.ExerciseEquipmentRepository
import com.fitness.workout.repository.ExerciseRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

/**
 * 후보 종목 필터: 필요한 장비를 모두 가진 활성 종목만 LLM에 넘긴다.
 * 테스트 DB에는 초기 종목도 들어 있으므로, 이 테스트가 만든 TEST_ 종목의 포함 여부만 본다.
 */
@RepositoryTest
@Import(ExerciseService::class)
class ExerciseServiceTest @Autowired constructor(
    private val exerciseService: ExerciseService,
    private val exerciseRepository: ExerciseRepository,
    private val exerciseEquipmentRepository: ExerciseEquipmentRepository,
) {

    @Test
    fun `필요한 장비 중 하나라도 없으면 후보에서 빠진다`() {
        // given
        val barbellOnly = save("TEST_BARBELL_ONLY", Equipment.BARBELL)
        val barbellAndBench = save("TEST_BARBELL_BENCH", Equipment.BARBELL, Equipment.BENCH)

        // when
        val candidates = exerciseService.findCandidates(listOf(BodyPart.CHEST), listOf(Equipment.BARBELL))

        // then
        assertThat(candidates.map { it.code })
            .contains(barbellOnly.code)
            .doesNotContain(barbellAndBench.code)
    }

    @Test
    fun `비활성 종목은 후보에서 빠진다`() {
        // given
        val inactive = save("TEST_INACTIVE", Equipment.BARBELL, active = false)

        // when
        val candidates = exerciseService.findCandidates(listOf(BodyPart.CHEST), listOf(Equipment.BARBELL))

        // then
        assertThat(candidates.map { it.code }).doesNotContain(inactive.code)
    }

    @Test
    fun `보유 장비가 없으면 맨몸 종목만 후보가 된다`() {
        // given
        val bodyweight = save("TEST_BODYWEIGHT")
        val barbell = save("TEST_NEEDS_BARBELL", Equipment.BARBELL)

        // when
        val candidates = exerciseService.findCandidates(listOf(BodyPart.CHEST), emptyList())

        // then
        assertThat(candidates.map { it.code })
            .contains(bodyweight.code)
            .doesNotContain(barbell.code)
    }

    @Test
    fun `부위 목록이 비면 빈 목록을 돌려준다`() {
        // given
        save("TEST_ANY_PART", Equipment.BARBELL)

        // when
        val candidates = exerciseService.findCandidates(emptyList(), listOf(Equipment.BARBELL))

        // then
        assertThat(candidates).isEmpty()
    }

    @Test
    fun `장비가 있어도 맨몸 종목은 후보에 포함된다`() {
        // given
        val bodyweight = save("TEST_BODYWEIGHT_WITH_GEAR")

        // when
        val candidates = exerciseService.findCandidates(listOf(BodyPart.CHEST), listOf(Equipment.BARBELL))

        // then
        assertThat(candidates.map { it.code }).contains(bodyweight.code)
    }

    @Test
    fun `요청하지 않은 부위의 종목은 후보에서 빠진다`() {
        // given
        val back = save("TEST_BACK_BARBELL", Equipment.BARBELL, bodyPart = BodyPart.BACK)

        // when
        val candidates = exerciseService.findCandidates(listOf(BodyPart.CHEST), listOf(Equipment.BARBELL))

        // then
        assertThat(candidates.map { it.code }).doesNotContain(back.code)
    }

    @Test
    fun `보유 장비가 없을 때도 요청하지 않은 부위의 맨몸 종목은 빠진다`() {
        // given
        val backBodyweight = save("TEST_BACK_BODYWEIGHT", bodyPart = BodyPart.BACK)

        // when
        val candidates = exerciseService.findCandidates(listOf(BodyPart.CHEST), emptyList())

        // then
        assertThat(candidates.map { it.code }).doesNotContain(backBodyweight.code)
    }

    @Test
    fun `보유 장비가 없을 때 비활성 맨몸 종목은 빠진다`() {
        // given
        val inactiveBodyweight = save("TEST_INACTIVE_BODYWEIGHT", active = false)

        // when
        val candidates = exerciseService.findCandidates(listOf(BodyPart.CHEST), emptyList())

        // then
        assertThat(candidates.map { it.code }).doesNotContain(inactiveBodyweight.code)
    }

    private fun save(
        code: String,
        vararg equipments: Equipment,
        active: Boolean = true,
        bodyPart: BodyPart = BodyPart.CHEST,
    ): Exercise {
        val exercise = exerciseRepository.saveAndFlush(WorkoutFixtures.exercise(code, bodyPart = bodyPart, active = active))
        equipments.forEach { exerciseEquipmentRepository.saveAndFlush(ExerciseEquipment(ExerciseEquipmentId(exercise.id!!, it))) }
        return exercise
    }
}
