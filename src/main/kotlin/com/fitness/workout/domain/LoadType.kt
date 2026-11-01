package com.fitness.workout.domain

/** 부하 방식. 점진적 과부하 규칙의 무게 증가폭을 정한다 (케이블은 MACHINE, 케틀벨은 DUMBBELL로 분류) */
enum class LoadType {
    BARBELL,
    DUMBBELL,
    MACHINE,
    BODYWEIGHT,
}
