---
name: new-api
description: 작업 절차 4단계. 승인된 계획(docs/plans)에 따라 API 하나를 정해진 구조(컨트롤러, 서비스, DTO, 통합 테스트, Swagger 문서)로 구현한다. "API 만들어", "구현 시작", 계획 승인 후 구현 단계에서 사용.
---

# API 구현 (4단계)

인자: 계획 파일 또는 API (예: `POST /api/v1/workouts/sync`).

## 시작 전 확인

- **M/L:** `docs/plans/`에 해당 계획이 있고 상태가 **승인됨**인지 확인한다. 아니면 구현하지 말고 `/plan-feature`를 안내한다.
- **S:** 이 대화에서 채팅 계획(고칠 파일, 내용, 테스트)이 승인됐는지 확인한다. 아니면 먼저 채팅으로 계획을 말하고 승인받는다.
- 계획의 테스트가 이미 작성돼 있는지 확인한다. 없으면 테스트부터 쓴다.
- M/L은 계획의 **핵심 코드 스케치**를 따른다. 스케치와 다르게 구현해야 하면 이유를 말하고 승인받는다.

## 구조

`com.fitness.<domain>` 아래 레이어별로 둔다.

| 레이어 | 파일 | 규칙 |
| --- | --- | --- |
| controller | `<Name>Controller.kt` | `@RestController`, 경로 `/api/v1/...`. 요청 DTO에 `@Valid`. 로직 없음, 서비스 호출과 응답 변환만. Swagger `@Tag`, `@Operation` |
| service | `<Name>Service.kt` | 비즈니스 로직, `@Transactional`(조회는 `readOnly = true`). 다른 도메인은 그 도메인의 Service로만 접근 |
| dto | `<Name>Request.kt`, `<Name>Response.kt` | `data class`. 검증은 `jakarta.validation` 어노테이션(`@field:NotNull` 등). 응답은 `companion object { fun from(entity) }`로 변환 |
| domain | 엔티티 | 기존 엔티티 재사용. `BaseTimeEntity` 상속. API 응답으로 직접 반환 금지 |
| repository | `<Name>Repository.kt` | Spring Data JPA. 복잡한 조회는 QueryDSL |
| test | `src/test/kotlin/...` | 통합: `@SpringBootTest` + `@AutoConfigureMockMvc` + `MockMvc`. 단위: Mockk. Given-When-Then |

- 공통 응답 포맷·예외 처리는 `common/response`, `common/exception`에 있으면 사용하고, 없으면 처음 필요할 때 만든다 (계획에 포함됐을 때만).
- 외부 협력자(LLM, FCM, OAuth 클라이언트)는 통합 테스트에서 `@MockkBean`으로 대체한다.

## 절차

1. 계획의 작업 순서대로 구현한다. 훅이 수정마다 컴파일을 확인하니, 에러가 오면 바로 고친다.
2. `./gradlew test --tests "<테스트 클래스>"`로 해당 테스트를 통과시킨다.
3. 테스트를 통과시키려고 테스트를 고치지 않는다. 테스트가 틀렸다고 판단되면 이유를 말하고 승인받는다.
4. 계획에서 벗어난 변경이 필요하면 멈추고 계획 수정을 제안한다.
5. 끝나면 변경 파일 목록, 테스트 결과를 보고한다. M/L이면 `/review-change`를 제안하고, 커밋 전에 diff 패널에서 실제 코드를 확인하라고 안내한다.
