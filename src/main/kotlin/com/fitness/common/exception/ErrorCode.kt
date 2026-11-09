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

    // 인증 (docs/decisions/0004-auth-token.md). 앱은 TOKEN_EXPIRED일 때만 refresh로 갱신한다
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "소셜 로그인 정보를 확인할 수 없습니다"),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식입니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    SOCIAL_LOGIN_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인에 잠시 연결할 수 없습니다"), // 공개키 조회 실패 (ADR 0005)
    ;

    companion object {
        // Spring이 직접 처리하는 예외(경로 없음, 메서드 불일치 등)는 상태 코드로 우리 코드를 고른다
        fun fromStatus(status: Int): ErrorCode = when {
            status == 401 -> UNAUTHORIZED
            status == 403 -> FORBIDDEN
            status == 404 -> NOT_FOUND
            status == 405 -> METHOD_NOT_ALLOWED
            status in 400..499 -> INVALID_REQUEST
            else -> INTERNAL_ERROR
        }
    }
}
