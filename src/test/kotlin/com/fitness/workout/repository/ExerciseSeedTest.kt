package com.fitness.workout.repository

import com.fitness.support.RepositoryTest
import com.fitness.workout.domain.BodyPart
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

/**
 * 초기 종목 데이터(V3)가 원본 CSV(docs/data/)와 정확히 같은지 확인한다.
 * "장비 행이 없으면 맨몸 종목"이라는 규칙 때문에 장비 행이 빠지면 오류 없이 누구에게나 추천되므로,
 * 개수가 아니라 내용 전체를 CSV와 비교한다. CSV를 바꾸면 기대값도 함께 바뀐다.
 */
@RepositoryTest
class ExerciseSeedTest @Autowired constructor(
    private val exerciseRepository: ExerciseRepository,
    private val exerciseEquipmentRepository: ExerciseEquipmentRepository,
) {

    @Test
    fun `초기 종목은 CSV의 종목과 정확히 같다`() {
        // given
        val expected = readCsv("docs/data/exercises.csv").map { it[0] }.toSet()

        // when: 전부 엔티티로 읽는다 (enum 값에 오타가 있으면 여기서 실패)
        val actual = exerciseRepository.findAll().map { it.code }.toSet()

        // then
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `초기 필요 장비는 CSV와 정확히 같다`() {
        // given
        val expected = readCsv("docs/data/exercise_equipment.csv").map { it[0] to it[1] }.toSet()
        val codeById = exerciseRepository.findAll().associate { it.id!! to it.code }

        // when
        val actual = exerciseEquipmentRepository.findAll()
            .map { codeById.getValue(it.id.exerciseId) to it.id.equipment.name }
            .toSet()

        // then
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `초기 종목은 모든 부위를 포함한다`() {
        // given: 테스트 DB는 시작할 때 V3까지 마이그레이션된다

        // when
        val bodyParts = exerciseRepository.findAll().map { it.bodyPart }.toSet()

        // then
        assertThat(bodyParts).containsExactlyInAnyOrder(*BodyPart.entries.toTypedArray())
    }

    // 테스트는 프로젝트 루트에서 실행된다. 첫 줄(헤더)은 건너뛴다. 값에 쉼표가 없는 단순 CSV만 다룬다
    private fun readCsv(path: String): List<List<String>> =
        File(path).readLines().drop(1).filter { it.isNotBlank() }.map { it.split(",") }
}
