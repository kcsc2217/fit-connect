package com.fitness.common.exception

/**
 * 서비스에서 하는 입력 검증 실패 (현재 시각·시간대처럼 DTO 어노테이션으로 볼 수 없는 값).
 * `@Valid` 실패와 같은 모양(400 INVALID_INPUT + errors[{field, message}])으로 응답한다.
 */
class InvalidInputException(
    val field: String,
    val reason: String,
) : BusinessException(ErrorCode.INVALID_INPUT)
