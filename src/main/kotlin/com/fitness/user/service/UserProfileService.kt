package com.fitness.user.service

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.common.exception.InvalidInputException
import com.fitness.common.type.Equipment
import com.fitness.user.domain.UserEquipment
import com.fitness.user.domain.UserEquipmentId
import com.fitness.user.domain.UserProfile
import com.fitness.user.domain.WorkoutDays
import com.fitness.user.dto.ProfileRequest
import com.fitness.user.dto.ProfileResponse
import com.fitness.user.repository.UserEquipmentRepository
import com.fitness.user.repository.UserProfileRepository
import com.fitness.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId

/** 온보딩 프로필 제출·조회 (계획 009) */
@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userEquipmentRepository: UserEquipmentRepository,
    private val clock: Clock,
) {

    /**
     * 프로필·장비를 덮어쓰고, 회원이 ONBOARDING이면 ACTIVE로 바꾼다. 같은 요청을 다시 보내도 결과가 같다.
     * 셋을 한 트랜잭션으로 묶어 "프로필은 있는데 ONBOARDING" 같은 부분 저장이 생기지 않게 한다.
     */
    @Transactional
    fun submit(userId: Long, request: ProfileRequest): ProfileResponse {
        validate(request)
        val days = request.workoutDays.rejectNulls("workoutDays", "운동 요일")
        val equipment = request.equipment.rejectNulls("equipment", "장비").toSet()

        // 같은 회원의 동시 제출을 줄 세운다 (SELECT ... FOR UPDATE). 회원 행은 로그인 때 만들어져 항상 있다.
        // 잠그지 않으면 동시 첫 제출이 둘 다 "프로필 없음"을 보고 INSERT해 PK 충돌이 난다.
        // 주의: 이 잠금보다 앞에 일반 조회(잠금 없는 SELECT)를 두지 않는다. MySQL REPEATABLE READ는 첫 일반 조회에서
        // 읽기 스냅샷을 잡으므로, 앞에 조회가 있으면 둘째 요청이 첫째의 커밋 전 스냅샷으로 "프로필 없음"을 보게 된다
        val user = userRepository.findByIdForUpdate(userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다")
        user.completeOnboarding()

        val workoutDays = WorkoutDays.toMask(days)
        val profile = userProfileRepository.findByIdOrNull(userId)
            ?.apply {
                update(
                    request.heightCm, request.weightKg, request.birthYear, request.sex, request.goal,
                    request.experience, request.splitType, workoutDays, request.alarmTime, request.timezone,
                    request.sessionMinutes,
                )
            }
            ?: userProfileRepository.save(request.toEntity(userId, workoutDays))

        replaceEquipment(userId, equipment)
        return ProfileResponse.of(profile, equipment, user.status)
    }

    @Transactional(readOnly = true)
    fun get(userId: Long): ProfileResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다")
        val profile = userProfileRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "프로필이 아직 없습니다")
        val equipment = userEquipmentRepository.findAllByIdUserId(userId).map { it.id.equipment }
        return ProfileResponse.of(profile, equipment, user.status)
    }

    // 전부 지우고 다시 넣으면 flush 때 INSERT가 DELETE보다 먼저 실행되어 같은 PK가 충돌한다. 차이만 반영한다
    private fun replaceEquipment(userId: Long, wanted: Set<Equipment>) {
        val current = userEquipmentRepository.findAllByIdUserId(userId)
        userEquipmentRepository.deleteAll(current.filter { it.id.equipment !in wanted })
        val have = current.map { it.id.equipment }.toSet()
        userEquipmentRepository.saveAll((wanted - have).map { UserEquipment(UserEquipmentId(userId, it)) })
    }

    // DTO 어노테이션으로 볼 수 없는 값: 올해(사용자 시간대 기준), 시간대 해석, 5분 단위, 선택지
    private fun validate(request: ProfileRequest) {
        val zone = try {
            ZoneId.of(request.timezone)
        } catch (e: DateTimeException) {
            throw InvalidInputException("timezone", "올바른 시간대가 아닙니다")
        }
        val thisYear = LocalDate.now(clock.withZone(zone)).year
        if (request.birthYear !in (thisYear - MAX_AGE)..(thisYear - MIN_AGE)) {
            throw InvalidInputException("birthYear", "나이는 ${MIN_AGE}세에서 ${MAX_AGE}세 사이여야 합니다")
        }
        if (request.alarmTime.minute % ALARM_STEP_MINUTES != 0 || request.alarmTime.second != 0 ||
            request.alarmTime.nano != 0
        ) {
            throw InvalidInputException("alarmTime", "알림 시각은 ${ALARM_STEP_MINUTES}분 단위여야 합니다")
        }
        if (request.sessionMinutes.toInt() !in SESSION_MINUTES) {
            throw InvalidInputException("sessionMinutes", "운동 시간은 ${SESSION_MINUTES.joinToString()}분 중 하나여야 합니다")
        }
    }

    private fun <T : Any> List<T?>.rejectNulls(field: String, label: String): List<T> =
        if (any { it == null }) throw InvalidInputException(field, "$label 목록에 빈 값이 있습니다") else filterNotNull()

    private fun ProfileRequest.toEntity(userId: Long, workoutDays: Int) = UserProfile(
        userId = userId,
        heightCm = heightCm,
        weightKg = weightKg,
        birthYear = birthYear,
        sex = sex,
        goal = goal,
        experience = experience,
        splitType = splitType,
        workoutDays = workoutDays,
        alarmTime = alarmTime,
        timezone = timezone,
        sessionMinutes = sessionMinutes,
    )

    companion object {
        private const val MIN_AGE = 14
        private const val MAX_AGE = 100
        private const val ALARM_STEP_MINUTES = 5
        private val SESSION_MINUTES = setOf(30, 45, 60, 90)
    }
}
