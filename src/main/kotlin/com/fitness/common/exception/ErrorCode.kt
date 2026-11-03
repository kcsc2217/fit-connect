package com.fitness.common.exception

import org.springframework.http.HttpStatus

/** 앱이 분기에 쓰는 에러 코드. 기능을 만들 때 도메인 코드(예: CHALLENGE_FULL)를 여기에 추가한다. */
enum class ErrorCode(
    val status: HttpStatus,
    val title: String,
) {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 메서드입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),
    ;

    companion object {
        // Spring이 직접 처리하는 예외(경로 없음, 메서드 불일치 등)는 상태 코드로 우리 코드를 고른다
        fun fromStatus(status: Int): ErrorCode = when {
            status == 404 -> NOT_FOUND
            status == 405 -> METHOD_NOT_ALLOWED
            status in 400..499 -> INVALID_REQUEST
            else -> INTERNAL_ERROR
        }
    }
}
