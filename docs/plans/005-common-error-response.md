# 005 공통 기반: 에러 응답·예외 처리·통합 테스트 지원 구현 계획

- 명세: 없음 (사용자 기능이 아니라 PRD에 뽑을 요구사항이 없어 사용자 결정으로 생략. 001~004처럼 계획부터)
- 크기: M
- 상태: 승인됨 (2026-11-03)

> 아래 코드는 계획 시점 초안이다. 테스트·구현 중 세부가 바뀔 수 있고, 크게 바뀌면 "변경 이력"에 남긴다.

## Context

엔티티·마이그레이션(001~004)이 끝나 기능(API) 구현을 시작한다. 첫 API를 만들기 전에 **모든 API가 따를 에러 응답 형식**과 **API 통합 테스트 방식**을 정해 둔다. 나중에 바꾸면 모든 API와 앱(Flutter)의 에러 처리를 고쳐야 하기 때문이다.

사용자 결정:
- 성공 응답은 **감싸지 않는다** (DTO를 그대로 JSON으로). 성공·실패는 HTTP 상태 코드로 구분.
- 에러 응답은 **ProblemDetail(RFC 9457) + `code` 필드**. Spring Boot 3 기본 지원 형식에 앱이 분기할 우리 에러 코드를 더한다.
- 결정과 이유는 ADR 0003으로 남긴다.

## 접근

Spring의 `ResponseEntityExceptionHandler`를 상속한 `GlobalExceptionHandler` 하나로 모든 예외를 ProblemDetail로 바꾼다. Spring MVC가 던지는 예외(경로 없음, 메서드 불일치, JSON 파싱 실패 등)는 부모 클래스가 이미 ProblemDetail을 만들어 주므로, 우리는 `code`·`title`·`instance`만 채운다. 우리 코드가 던지는 예외는 `BusinessException(ErrorCode)` 하나로 통일한다. 예상하지 못한 예외는 500으로 바꾸고 내부 메시지를 숨긴 채 로그만 남긴다.

아직 API가 없으므로 **테스트 전용 컨트롤러**(src/test에만 존재)로 각 예외 경로를 MockMvc 통합 테스트로 확인한다. 이때 앞으로 모든 API 테스트가 쓸 `@IntegrationTest` 어노테이션을 함께 만든다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| common/exception/ErrorCode.kt | 신규 | 에러 코드 enum (상태, 제목) + 상태 코드 → 코드 변환 |
| common/exception/BusinessException.kt | 신규 | 우리 코드가 던지는 예외의 부모 |
| common/exception/FieldErrorDetail.kt | 신규 | 검증 실패 필드 하나 (`field`, `message`) |
| common/exception/GlobalExceptionHandler.kt | 신규 | 모든 예외 → ProblemDetail + code |
| test/.../support/IntegrationTest.kt | 신규 | `@SpringBootTest` + MockMvc + `local,test` 프로파일 + Flyway 묶음 |
| test/.../support/ErrorTestController.kt | 신규 | 테스트 전용 컨트롤러 (예외를 일부러 던짐) |
| test/.../common/exception/GlobalExceptionHandlerTest.kt | 신규 | 에러 응답 통합 테스트 8개 |
| docs/decisions/0003-error-response-format.md | 신규 | ADR: 성공 응답 비봉투, 에러는 ProblemDetail + code |
| CLAUDE.md | 수정 | 패키지 구조의 `common/response` 삭제·`exception` 설명, `@IntegrationTest` 규칙, 통합 테스트는 Redis도 필요 |
| .claude/skills/new-api/SKILL.md | 수정 | "공통 응답 포맷" 문구를 ErrorCode·BusinessException 사용으로 |

## 데이터 변경

없음.

## API

새 API 없음. 모든 API의 에러 응답 형식이 다음으로 정해진다.

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "입력값이 올바르지 않습니다",
  "status": 400,
  "detail": "입력값이 올바르지 않습니다",
  "instance": "/api/v1/users/me/profile",
  "code": "INVALID_INPUT",
  "errors": [
    { "field": "heightCm", "message": "must be greater than 0" }
  ]
}
```

| 상황 | status | code | errors |
| --- | --- | --- | --- |
| 우리 코드의 `BusinessException` | ErrorCode의 상태 | ErrorCode 이름 | - |
| `@Valid` 검증 실패 | 400 | INVALID_INPUT | 필드별 목록 |
| JSON 필드 누락·타입 불일치 | 400 | INVALID_INPUT | 해당 필드 하나 |
| JSON 문법 오류 | 400 | INVALID_REQUEST | - |
| 경로 변수·쿼리 파라미터 타입 불일치 | 400 | INVALID_REQUEST | - |
| 없는 경로 | 404 | NOT_FOUND | - |
| 지원하지 않는 메서드 | 405 | METHOD_NOT_ALLOWED | - |
| 그 밖의 예외 | 500 | INTERNAL_ERROR | - (내부 메시지 숨김) |

---

## 1절. 판단이 들어간 코드 (꼭 봐 주세요)

### 1-1. ErrorCode: 우리 에러 코드 목록과 상태 코드 변환

**Kotlin**
```kotlin
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
```

**Java**
```java
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 메서드입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() { return status; }
    public String getTitle() { return title; }

    public static ErrorCode fromStatus(int status) {
        if (status == 404) return NOT_FOUND;
        if (status == 405) return METHOD_NOT_ALLOWED;
        if (status >= 400 && status <= 499) return INVALID_REQUEST;
        return INTERNAL_ERROR;
    }
}
```

**Kotlin 문법 설명**
- `enum class X(val a: A, val b: B)`: enum 생성자와 필드(+ getter)를 한 줄로 선언한다. Java의 private final 필드 + 생성자 + getter.
- 마지막 상수 뒤 `;`: enum 상수 목록 뒤에 본문(companion object)이 오면 세미콜론이 필요하다. Java와 같다.
- `companion object { fun f() }`: 클래스에 속한 정적 멤버. Java의 `static` 메서드. 호출도 `ErrorCode.fromStatus(404)`로 같다.
- `when { 조건 -> 값 }`: if-else if 체인을 식으로 쓴 것. 값을 바로 반환한다.
- `status in 400..499`: 범위 검사. Java의 `status >= 400 && status <= 499`.

**판단 포인트**
- 처음에는 **공통 코드 5개만** 둔다. 도메인 코드(`CHALLENGE_FULL`, `ROUTINE_REGEN_LIMIT` 등)는 그 기능을 만들 때 추가한다. 쓰지 않는 코드를 미리 만들지 않는다.
- `code`는 enum 이름 문자열(`"NOT_FOUND"`)로 내보낸다. 앱은 `detail` 문구가 아니라 `code`로 분기한다 (문구는 바뀔 수 있으므로).
- 확인 테스트: `없는 경로를 호출하면 404 NOT_FOUND로 응답한다`, `지원하지 않는 HTTP 메서드로 호출하면 405 METHOD_NOT_ALLOWED로 응답한다`.

### 1-2. GlobalExceptionHandler: 예외별 처리 분기

**Kotlin**
```kotlin
package com.fitness.common.exception

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
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
            val field = cause.path.joinToString(".") { it.fieldName ?: "[${it.index}]" }
            return respond(invalidInput(listOf(FieldErrorDetail(field, "값이 없거나 형식이 올바르지 않습니다")), request.path()))
        }
        return respond(problem(ErrorCode.INVALID_REQUEST, "요청 본문을 읽을 수 없습니다", request.path()))
    }

    // 나머지 Spring MVC 예외(404, 405, 타입 불일치 등): 부모가 만든 ProblemDetail에 code·title·instance를 채운다
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

    private fun <T> respond(problem: ProblemDetail): ResponseEntity<T> =
        @Suppress("UNCHECKED_CAST")
        (ResponseEntity.status(problem.status).body(problem) as ResponseEntity<T>)

    private fun WebRequest.path(): String = (this as ServletWebRequest).request.requestURI
}
```

**Java (핵심 부분)**
```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException e, HttpServletRequest request) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(problem(e.getErrorCode(), e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("처리하지 못한 예외: {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(500)
                .body(problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getTitle(), request.getRequestURI()));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        if (ex.getCause() instanceof MismatchedInputException cause && !cause.getPath().isEmpty()) {
            String field = cause.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            ProblemDetail body = invalidInput(List.of(new FieldErrorDetail(field, "값이 없거나 형식이 올바르지 않습니다")), path);
            return ResponseEntity.status(400).body(body);
        }
        return ResponseEntity.status(400).body(problem(ErrorCode.INVALID_REQUEST, "요청 본문을 읽을 수 없습니다", path));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response == null || !(response.getBody() instanceof ProblemDetail problem)) {
            return response;
        }
        ErrorCode code = ErrorCode.fromStatus(statusCode.value());
        problem.setTitle(code.getTitle());
        problem.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
        problem.setProperty("code", code.name());
        return response;
    }

    private ProblemDetail problem(ErrorCode code, String detail, String path) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(code.getStatus(), detail != null ? detail : code.getTitle());
        p.setTitle(code.getTitle());
        p.setInstance(URI.create(path));
        p.setProperty("code", code.name());
        return p;
    }
    // handleMethodArgumentNotValid, invalidInput은 같은 방식이라 생략
}
```

**Kotlin 문법 설명**
- `class A : B()`: `extends B`. 괄호는 부모 생성자 호출이다.
- `override fun`: Java의 `@Override`. Kotlin은 키워드가 필수라 빠뜨리면 컴파일 오류가 난다.
- `fun f(...): T = 식`: 본문이 식 하나인 함수. `{ return 식; }`과 같다.
- `it.defaultMessage ?: "..."`: 엘비스 연산자. 왼쪽이 null이면 오른쪽 값. Java의 `x != null ? x : "..."`.
- `response?.body as? ProblemDetail ?: return response`: `?.`는 null이면 멈추고 null, `as?`는 캐스트 실패 시 예외 대신 null. 셋을 합쳐 "ProblemDetail이 아니면 그대로 반환". Java의 `instanceof` 패턴 + null 검사.
- `.apply { title = ... }`: 객체를 만든 직후 그 객체의 setter들을 호출하고 객체 자신을 반환한다. Java에서 `p.setTitle(...); return p;`를 한 블록으로 쓴 것.
- `private fun WebRequest.path()`: 확장 함수. WebRequest 클래스를 고치지 않고 메서드를 붙인 것처럼 쓴다. Java에서는 `private static String path(WebRequest r)` 유틸 메서드.
- `listOf(...)`, `map { }`, `joinToString(".") { }`: Java의 `List.of`, `stream().map()`, `Collectors.joining(".")`.

**판단 포인트**
- **`ResponseEntityExceptionHandler` 상속:** Spring MVC 예외 15종 정도(404, 405, 415, 타입 불일치 등)를 부모가 알맞은 상태 코드의 ProblemDetail로 만든다. 직접 하나씩 잡으면 빠뜨리기 쉽다.
- **`Exception` 핸들러는 가장 넓은 타입이라 마지막에만 걸린다.** Spring은 가장 가까운 타입의 핸들러를 고르므로, 부모가 처리하는 MVC 예외나 `BusinessException`을 가로채지 않는다.
- **`IllegalArgumentException`(엔티티 `require` 실패)을 400으로 바꾸지 않는다.** 입력 검증은 요청 DTO의 `@Valid`가 맡는다. 엔티티 검증까지 내려왔다면 서버 버그이므로 500으로 드러나게 둔다. 라이브러리 내부의 IAE가 400으로 숨겨지는 것도 막는다.
- **Kotlin DTO의 필드 누락 처리:** Kotlin의 non-null 필드는 JSON에서 빠지면 `@Valid`까지 가지 못하고 Jackson 단계에서 실패한다. 이걸 그냥 두면 "필드가 빠졌다"는 정보 없이 INVALID_REQUEST가 된다. 그래서 Jackson 예외의 필드 경로를 꺼내 검증 실패와 같은 INVALID_INPUT + errors 형식으로 맞춘다. 확인 테스트: `필수 필드가 빠진 요청은 400 INVALID_INPUT과 빠진 필드 이름을 응답한다`.
- **500은 내부 메시지를 숨긴다:** SQL, 테이블 이름 같은 정보가 응답으로 새지 않게 한다. 확인 테스트: `예상하지 못한 예외가 나면 500 INTERNAL_ERROR로 응답하고 내부 메시지를 노출하지 않는다`.
- **4xx는 로그를 남기지 않는다:** 사용자 입력 오류라서 에러 로그로 남기면 알림이 시끄러워진다. 500만 error 로그.

### 1-3. @IntegrationTest: 앞으로 모든 API 테스트가 쓸 묶음

**Kotlin**
```kotlin
package com.fitness.support

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * API 통합 테스트 공통 설정.
 * 앱 전체를 띄우고 MockMvc로 요청한다. DB는 @RepositoryTest와 같은 fitness_test + Flyway.
 * Redisson이 시작할 때 Redis에 연결하므로 로컬 Redis도 켜져 있어야 한다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local", "test")
@Import(TestFlywayConfig::class)
annotation class IntegrationTest
```

**Java**
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Import(TestFlywayConfig.class)
public @interface IntegrationTest {}
```

**Kotlin 문법 설명**
- `annotation class X`: Java의 `@interface X`. 여러 어노테이션을 하나로 묶는 메타 어노테이션.
- `TestFlywayConfig::class`: Java의 `TestFlywayConfig.class`.

**판단 포인트**
- 기존 `@RepositoryTest`와 같은 DB 설정(`local,test` 프로파일, 매 컨텍스트마다 Flyway clean → migrate)을 쓴다. 테스트 DB가 하나로 유지된다.
- 트랜잭션 롤백(`@Transactional`)은 **지금 넣지 않는다.** 이번 테스트는 DB에 쓰지 않는다. DB에 쓰는 첫 API(인증)에서 롤백 방식과 함께 정한다.
- **로컬 Redis 필요:** `redisson-spring-boot-starter`가 앱 시작 시 Redis에 연결한다. 통합 테스트도 MySQL과 함께 Redis가 켜져 있어야 통과한다. CLAUDE.md에 적는다. (CI에서 Redis를 어떻게 띄울지는 CI 작업에서 정한다.)

---

## 2절. 전체 코드 (정형 코드, 훑어보기용)

### BusinessException.kt
```kotlin
package com.fitness.common.exception

/** 우리 코드가 의도해서 던지는 예외. 기능별 예외는 이 클래스를 상속하거나 ErrorCode만 바꿔 던진다. */
open class BusinessException(
    val errorCode: ErrorCode,
    detail: String = errorCode.title,
) : RuntimeException(detail)
```
**Java** (처음 나오는 문법이라 붙임)
```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getTitle());
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
```
- `open class`: Kotlin 클래스는 기본이 `final`(상속 불가)이다. 상속을 허용하려면 `open`을 붙인다. Java는 반대로 기본이 상속 가능.
- `detail: String = errorCode.title`: 기본값 있는 파라미터. Java에서는 생성자 오버로딩 두 개에 해당한다.
- `val`이 없는 생성자 파라미터(`detail`)는 필드가 되지 않고 부모 생성자에 넘기는 데만 쓰인다.

### FieldErrorDetail.kt
```kotlin
package com.fitness.common.exception

/** 검증에 실패한 필드 하나. ProblemDetail의 errors 배열 원소. */
data class FieldErrorDetail(
    val field: String,
    val message: String,
)
```
(`data class` = Java `record`, 이미 설명한 문법이라 Java 생략)

### ErrorTestController.kt (src/test 전용)
```kotlin
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

    @GetMapping("/items/{id}")
    fun item(@PathVariable id: Long): Long = id

    data class SampleRequest(
        @field:NotBlank
        val name: String,
        @field:Min(1)
        val count: Int,
    )
}
```
- `fun f(): String = throw ...`: `throw`도 식이라 본문 자리에 바로 쓸 수 있다. Java의 `{ throw new ...; }`.
- `@field:NotBlank`: 어노테이션을 생성자 파라미터가 아니라 **필드**에 붙이라는 지정. 검증 어노테이션은 필드에 있어야 동작한다. Java에는 이 구분이 없다.
- 컨트롤러 안의 `data class`는 Java의 static 중첩 record에 해당한다.

### GlobalExceptionHandlerTest.kt (대표 2개, 나머지는 같은 형태)
```kotlin
package com.fitness.common.exception

import com.fitness.support.IntegrationTest
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@IntegrationTest
class GlobalExceptionHandlerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `비즈니스 예외가 나면 에러 코드의 상태와 code를 담은 ProblemDetail로 응답한다`() {
        // given
        val path = "/test-support/errors/business"

        // when
        val result = mockMvc.get(path)

        // then
        result.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.code") { value("NOT_FOUND") }
            jsonPath("$.detail") { value("종목을 찾을 수 없습니다: 999") }
            jsonPath("$.instance") { value(path) }
        }
    }

    @Test
    fun `요청 본문 검증에 실패하면 400 INVALID_INPUT과 필드별 오류 목록을 응답한다`() {
        // given
        val body = """{"name": "", "count": 0}"""

        // when
        val result = mockMvc.post("/test-support/errors/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
            jsonPath("$.errors[*].field") { value(containsInAnyOrder("name", "count")) }
        }
    }
}
```
- `mockMvc.get(path)`, `andExpect { status { ... } }`: Spring이 제공하는 **Kotlin MockMvc DSL**. Java의 `mockMvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"))`와 같다.
- `"""..."""`: 여러 줄·따옴표를 이스케이프 없이 쓰는 원시 문자열. Java 15+의 텍스트 블록(`"""`)과 같다.
- `class X @Autowired constructor(private val mockMvc: MockMvc)`: 생성자 주입. Java의 `@Autowired` 생성자 + private final 필드.

---

## 정합성·동시성·멱등성

해당 없음 (상태를 저장하지 않는 예외 변환 코드).

## 실패·대체 경로

- 예외 처리 중 또 예외가 나면 Spring 기본 에러 처리(`/error`)로 넘어간다. 이번 범위에서는 따로 다루지 않는다.
- Spring Security 도입 후 401·403은 필터 단계에서 나므로 이 핸들러를 거치지 않는다. 인증 작업(006 예정)에서 같은 ProblemDetail + code 형식으로 맞춘다.

## 테스트 목록

모두 `GlobalExceptionHandlerTest` (`@IntegrationTest`, MockMvc 통합).

- [ ] `비즈니스 예외가 나면 에러 코드의 상태와 code를 담은 ProblemDetail로 응답한다` — 404, `application/problem+json`, code·detail·instance
- [ ] `요청 본문 검증에 실패하면 400 INVALID_INPUT과 필드별 오류 목록을 응답한다` — `{"name":"","count":0}` → errors 필드 name, count
- [ ] `필수 필드가 빠진 요청은 400 INVALID_INPUT과 빠진 필드 이름을 응답한다` — `{"count":1}` → errors[0].field = name
- [ ] `JSON 문법이 잘못된 요청은 400 INVALID_REQUEST로 응답한다` — `{"name":`
- [ ] `경로 변수 타입이 맞지 않으면 400 INVALID_REQUEST로 응답한다` — `/items/abc`
- [ ] `없는 경로를 호출하면 404 NOT_FOUND로 응답한다`
- [ ] `지원하지 않는 HTTP 메서드로 호출하면 405 METHOD_NOT_ALLOWED로 응답한다` — `DELETE /business`
- [ ] `예상하지 못한 예외가 나면 500 INTERNAL_ERROR로 응답하고 내부 메시지를 노출하지 않는다` — 응답 본문에 "users 테이블"이 없음

## 작업 순서

1. 승인되면 계획을 `docs/plans/005-common-error-response.md`에 저장, ADR 0003 작성
2. `IntegrationTest`, `ErrorTestController`, `ErrorCode`·`BusinessException`·`FieldErrorDetail` 뼈대(컴파일용) 작성
3. **테스트 8개 작성 → 핸들러가 없어 실패하는 것 확인** (Spring 기본 응답이라 code가 없음)
4. `GlobalExceptionHandler` 구현 → 테스트 통과
5. CLAUDE.md, new-api 스킬 문구 갱신
6. `./gradlew test` 전체 통과 → `/review-change` → 커밋 (main) → ai-log 기록

## 검증

- `./gradlew test`: 기존 49개 + 새 8개 통과 (로컬 MySQL·Redis 실행 상태)
- `./gradlew bootRun` 후 없는 경로 호출: `curl -i localhost:8080/api/v1/nothing` → 404, `application/problem+json`, `"code":"NOT_FOUND"` (제가 띄운 프로세스만 종료)

## 확인 필요

- 에러 메시지 언어: `title`·`detail`은 한국어로 둔다. `@Valid` 기본 메시지는 지금은 Spring 기본(영어, 예: "must not be blank")이다. 한국어로 바꿀지(각 어노테이션에 `message` 지정 또는 `ValidationMessages.properties`)는 첫 실제 API에서 정해도 된다.

## 변경 이력
- 2026-11-03: 리뷰 지적 5건 반영 (지적 6건 중 5번 제외). 테스트 8개 → 14개.
  - **계획 전제 수정:** "Kotlin DTO는 필드가 빠지면 Jackson 단계에서 실패한다"는 객체 타입(`String` 등)에만 맞았다. 원시 타입(`Int`, `Boolean`)은 빠지면 0·false로 채워져 통과했다. `application.yml`에 `spring.jackson.deserialization.fail-on-null-for-primitives: true`를 추가했다. 테스트: `원시 타입 필드가 빠진 요청은 0으로 채우지 않고 400 INVALID_INPUT과 빠진 필드 이름을 응답한다`.
  - **메서드 검증 실패 처리 추가:** 리스트 본문의 `@Valid`와 `@RequestParam @Min` 같은 파라미터 제약은 Spring 6.1에서 `HandlerMethodValidationException`으로 온다. `handleHandlerMethodValidationException`을 오버라이드해 400 `INVALID_INPUT` + errors로 맞췄다. 경로는 원소면 `[1].name`, 파라미터면 `page`다. 테스트 2개.
  - **Spring이 처리하는 5xx:** `handleExceptionInternal`에서 5xx면 error 로그를 남기고 detail을 `INTERNAL_ERROR.title`로 덮어쓴다. 테스트: `Spring이 처리하는 5xx 예외는 ...` (MissingPathVariableException으로 재현).
  - **필드 경로 형식 통일:** Jackson 경로를 `items.[0].name`에서 `items[0].name`으로 바꿔 `@Valid` 경로와 맞췄다. 테스트: `중첩 리스트의 필드가 빠지면 ...`.
  - **JSON 필드 타입 불일치 테스트 추가:** 동작은 원래 맞았고, 회귀 방지용이다.
  - **반영하지 않음 (5번):** `CompletionException` 등으로 감싸진 `BusinessException`이 500으로 나가는 문제. 지금은 비동기 경로가 없어 비동기 호출(LLM·FCM)을 처음 넣는 작업에서 다룬다.
  - 위 표의 "JSON 필드 누락·타입 불일치" 행에 원시 타입 누락이 포함된다. "`@Valid` 검증 실패" 행에 리스트 본문·쿼리 파라미터 제약 위반이 포함된다.
