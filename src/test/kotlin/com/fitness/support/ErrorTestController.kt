package com.fitness.support

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** GlobalExceptionHandler 검증용. src/test에만 있어 운영 앱에는 포함되지 않는다. */
@RestController
@RequestMapping("/test-support/errors")
class ErrorTestController {

    @GetMapping("/business")
    fun business(): String = throw BusinessException(ErrorCode.NOT_FOUND, "종목을 찾을 수 없습니다: 999")

    @GetMapping("/unexpected")
    fun unexpected(): String = throw IllegalStateException("내부 상태 노출 금지: users 테이블")

    @PostMapping("/validation")
    fun validation(@Valid @RequestBody request: SampleRequest): SampleRequest = request

    // 리스트 본문의 @Valid는 Spring 6.1의 메서드 검증(HandlerMethodValidationException)으로 처리된다
    @PostMapping("/validation-list")
    fun validationList(@Valid @RequestBody requests: List<SampleRequest>): Int = requests.size

    @PostMapping("/nested")
    fun nested(@Valid @RequestBody request: NestedRequest): NestedRequest = request

    @PostMapping("/primitive")
    fun primitive(@RequestBody request: PrimitiveRequest): PrimitiveRequest = request

    @GetMapping("/page")
    fun page(@RequestParam @Min(1) page: Int): Int = page

    @GetMapping("/items/{id}")
    fun item(@PathVariable id: Long): Long = id

    // 경로에 {id}가 없어 MissingPathVariableException(500)이 난다: Spring이 처리하는 5xx 예외 재현용
    @GetMapping("/missing-path-variable")
    fun missingPathVariable(@PathVariable id: Long): Long = id

    data class SampleRequest(
        @field:NotBlank
        val name: String,
        @field:Min(1)
        val count: Int,
    )

    data class NestedRequest(
        @field:Valid
        val items: List<SampleRequest>,
    )

    // 제약이 없는 원시 타입 필드: 빠지면 0으로 채워지지 않고 실패해야 한다
    data class PrimitiveRequest(
        val reps: Int,
    )
}
