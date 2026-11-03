package com.fitness.common.exception

/** 우리 코드가 의도해서 던지는 예외. 기능별 예외는 이 클래스를 상속하거나 ErrorCode만 바꿔 던진다. */
open class BusinessException(
    val errorCode: ErrorCode,
    detail: String = errorCode.title,
) : RuntimeException(detail)
