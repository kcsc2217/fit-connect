package com.fitness.common.exception

import org.springframework.http.ProblemDetail
import java.net.URI

/** ProblemDetail + code (ADR 0003). 예외 핸들러와 보안 필터 단계(401·403)가 같은 형식을 쓰도록 한곳에서 만든다 */
fun problemDetail(code: ErrorCode, detail: String?, path: String): ProblemDetail =
    ProblemDetail.forStatusAndDetail(code.status, detail ?: code.title).apply {
        title = code.title
        instance = URI.create(path)
        setProperty("code", code.name)
    }
