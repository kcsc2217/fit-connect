# 0003 성공 응답은 감싸지 않고, 에러는 ProblemDetail + code로 응답

- 상태: 채택
- 날짜: 2026-11-03
- 관련: docs/plans/005-common-error-response.md

## 배경
첫 API를 만들기 전에 모든 API가 따를 응답 형식을 정해야 한다. 나중에 바꾸면 서버의 모든 API와 앱(Flutter)의 에러 처리를 함께 고쳐야 한다.
앱은 에러 종류에 따라 다르게 동작해야 한다 (입력 오류는 필드 옆에 표시, 정원 마감은 대기 신청 안내 등). 그래서 사람이 읽는 문구와 별개로 앱이 분기할 수 있는 안정적인 값이 필요하다.

## 선택지
### A. 성공 응답을 공통 봉투로 감쌈 (`{success, data, error}`)
- 장점: 모든 응답 모양이 같다. 국내 프로젝트에서 흔한 형식이다.
- 단점: 성공 여부가 HTTP 상태 코드와 `success` 필드에 중복된다. 모든 컨트롤러·테스트·앱 파싱 코드에 감싸고 푸는 코드가 늘어난다.

### B. 성공 응답은 DTO 그대로, 에러는 직접 정의한 형식 (`{code, message}`)
- 장점: 단순하다.
- 단점: 형식을 우리가 정의·문서화해야 한다. Spring MVC가 던지는 예외(404, 405, 타입 불일치 등)를 모두 직접 그 형식으로 바꿔야 한다.

### C. 성공 응답은 DTO 그대로, 에러는 ProblemDetail(RFC 9457) + `code` 확장 필드
- 장점: HTTP API 에러의 표준 형식이다 (`type`, `title`, `status`, `detail`, `instance`, `Content-Type: application/problem+json`). Spring Boot 3가 기본 지원해, Spring MVC 예외는 `ResponseEntityExceptionHandler`가 이미 이 형식으로 만든다. 표준이 허용하는 확장 필드로 `code`·`errors`를 더할 수 있다.
- 단점: 국내 프로젝트에서는 덜 익숙하다. 필드가 A·B보다 많다.

## 결정
C. 성공 응답은 감싸지 않는다. 에러는 ProblemDetail에 우리 에러 코드(`code`, `ErrorCode` enum 이름)를 더하고, 입력 오류는 `errors`(필드별 `field`, `message`)를 더한다.

## 이유
- 성공·실패 구분은 HTTP 상태 코드가 이미 하므로 봉투로 중복하지 않는다.
- 표준 형식이라 Spring 기본 처리와 맞물려 직접 바꿀 예외가 적고, "왜 이 형식인가"를 표준으로 설명할 수 있다.
- 앱은 문구(`detail`)가 아니라 `code`로 분기한다. 문구를 고쳐도 앱이 깨지지 않는다.

## 결과
- 모든 예외는 `common.exception.GlobalExceptionHandler`가 ProblemDetail + code로 바꾼다. 우리 코드는 `BusinessException(ErrorCode)`를 던진다.
- 도메인 에러 코드(예: 챌린지 정원 마감)는 그 기능을 만들 때 `ErrorCode`에 추가한다.
- 예상하지 못한 예외는 500 `INTERNAL_ERROR`로 응답하고 내부 메시지를 숨긴다. 엔티티 `require` 실패(`IllegalArgumentException`)도 서버 버그로 보고 500으로 둔다 (입력 검증은 요청 DTO의 `@Valid`가 맡는다).
- Spring Security의 401·403은 필터 단계에서 나므로 이 핸들러를 거치지 않는다. 인증을 만들 때 같은 형식으로 맞춘다.
- 다시 검토할 조건: 앱 외의 클라이언트(웹, 외부 연동)가 생겨 다른 형식을 요구할 때.
