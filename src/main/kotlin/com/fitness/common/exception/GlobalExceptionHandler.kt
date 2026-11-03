package com.fitness.common.exception

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.method.ParameterErrors
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

/**
 * 모든 예외를 ProblemDetail(RFC 9457) + code로 응답한다 (docs/decisions/0003-error-response-format.md).
 * Spring MVC 예외는 부모 클래스가 ProblemDetail을 만들고, 여기서는 code·title·instance만 채운다.
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    // 우리 코드가 던진 예외: ErrorCode의 상태와 detail을 그대로 쓴다
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException, request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        respond(problem(e.errorCode, e.message, request.requestURI))

    // 예상하지 못한 예외: 내부 메시지(SQL, 스택 등)를 숨기고 로그로만 남긴다
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        log.error("처리하지 못한 예외: {} {}", request.method, request.requestURI, e)
        return respond(problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.title, request.requestURI))
    }

    // @Valid 검증 실패: 어떤 필드가 왜 틀렸는지 목록으로 준다
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val errors = ex.bindingResult.fieldErrors.map {
            FieldErrorDetail(it.field, it.defaultMessage ?: "올바르지 않은 값입니다")
        }
        return respond(invalidInput(errors, request.path()))
    }

    // 본문을 읽지 못함. Kotlin DTO는 필드가 빠지거나 타입이 틀리면 @Valid 전에 여기서 실패하므로,
    // Jackson이 알려 준 필드 경로를 꺼내 검증 실패와 같은 형식(INVALID_INPUT)으로 맞춘다
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val cause = ex.cause
        if (cause is MismatchedInputException && cause.path.isNotEmpty()) {
            // @Valid 경로와 같은 형식(items[0].name)으로: 인덱스 앞에는 "."을 붙이지 않는다
            val field = cause.path.withIndex().joinToString("") { (i, ref) ->
                ref.fieldName?.let { if (i == 0) it else ".$it" } ?: "[${ref.index}]"
            }
            val errors = listOf(FieldErrorDetail(field, "값이 없거나 형식이 올바르지 않습니다"))
            return respond(invalidInput(errors, request.path()))
        }
        return respond(problem(ErrorCode.INVALID_REQUEST, "요청 본문을 읽을 수 없습니다", request.path()))
    }

    // 메서드 검증 실패(Spring 6.1): 리스트 본문의 @Valid, @RequestParam의 제약 등.
    // 원소 검증 실패는 "[1].name", 파라미터 값 검증 실패는 "page"처럼 @Valid와 같은 형식의 경로로 준다
    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val errors = ex.allValidationResults.flatMap { result ->
            if (result is ParameterErrors) {
                val prefix = result.containerIndex?.let { "[$it]." } ?: ""
                result.fieldErrors.map {
                    FieldErrorDetail(prefix + it.field, it.defaultMessage ?: "올바르지 않은 값입니다")
                }
            } else {
                val name = result.methodParameter.parameterName ?: "unknown"
                result.resolvableErrors.map {
                    FieldErrorDetail(name, it.defaultMessage ?: "올바르지 않은 값입니다")
                }
            }
        }
        return respond(invalidInput(errors, request.path()))
    }

    // 나머지 Spring MVC 예외(404, 405, 타입 불일치 등): 부모가 만든 ProblemDetail에 code·title·instance를 채운다.
    // 5xx(응답 직렬화 실패 등)는 서버 버그라 에러 로그를 남기고, Spring의 내부 문구를 숨긴다
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val response = super.handleExceptionInternal(ex, body, headers, statusCode, request)
        val problem = response?.body as? ProblemDetail ?: return response
        val code = ErrorCode.fromStatus(statusCode.value())
        if (statusCode.is5xxServerError) {
            log.error("처리하지 못한 예외: {}", request.path(), ex)
            problem.detail = code.title
        }
        problem.title = code.title
        problem.instance = URI.create(request.path())
        problem.setProperty("code", code.name)
        return response
    }

    private fun problem(code: ErrorCode, detail: String?, path: String): ProblemDetail =
        ProblemDetail.forStatusAndDetail(code.status, detail ?: code.title).apply {
            title = code.title
            instance = URI.create(path)
            setProperty("code", code.name)
        }

    private fun invalidInput(errors: List<FieldErrorDetail>, path: String): ProblemDetail =
        problem(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.title, path).apply {
            setProperty("errors", errors)
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> respond(problem: ProblemDetail): ResponseEntity<T> =
        ResponseEntity.status(problem.status).body(problem) as ResponseEntity<T>

    private fun WebRequest.path(): String = (this as ServletWebRequest).request.requestURI
}
