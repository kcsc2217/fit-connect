# 007 인증 1-B: 카카오·구글 소셜 로그인 (OIDC id_token 검증) 구현 계획

- 명세: docs/specs/auth-social-oidc.md (확정 2026-11-06)
- 크기: M
- 상태: 승인됨 (2026-11-09)

> 아래 코드는 계획 시점 초안이다. 테스트·구현 중 세부가 바뀔 수 있고, 크게 바뀌면 "변경 이력"에 남긴다.

## Context

1-A(006)는 개발용 로그인(DEV)만 있어 실제 사용자가 로그인할 수 없다. 1-B는 앱이 카카오·구글 SDK로 받은 id_token을 서버가 검증해, 1-A의 토큰 발급 흐름으로 넘긴다. 로그인 API와 토큰 체계는 바꾸지 않고, 1-A의 `SocialTokenVerifier`에 구현을 추가한다. 006 리뷰 지적 5(`provider_user_id`가 대소문자·끝 공백을 무시)도 실제 provider가 붙기 전에 여기서 고친다.

## 접근

- **검증기는 하나, 설정은 둘:** 카카오와 구글은 둘 다 표준 OIDC(RS256 + JWKS)라서 다른 점이 `iss` 값, 공개키 주소, 허용 `aud`뿐이다. 그래서 `OidcIdTokenVerifier` 클래스 하나를 만들고 `OidcProviderSettings`(카카오용, 구글용)로 두 개의 빈을 만든다.
- **Nimbus를 쓰되, 공개키 소스는 직접 조립한다:**
  - Spring의 기본 빌더(`NimbusJwtDecoder.withJwkSetUri`)는 Nimbus의 `RemoteJWKSet`을 쓴다. 이 클래스는 **모르는 `kid`가 올 때마다 공개키를 다시 받는다 (제한 없음).**
  - 그러면 `kid`를 아무렇게나 바꾼 토큰을 연달아 보내는 것만으로 우리 서버가 카카오를 반복 호출하게 된다. 카카오는 "지나치게 빈번한 요청은 차단될 수 있다"고 안내한다.
  - 그래서 Spring Security 6.3.3에 들어 있는 Nimbus 9.37.3의 `JWKSourceBuilder`로 직접 조립한다. 캐시 1시간, 다시 받기는 30초에 한 번, 1회 재시도, 장애 시 이전 키를 12시간 사용, 연결·읽기 타임아웃 각 1초를 둔다.
  - 이렇게 만든 공개키 소스를 `NimbusJwtDecoder(JWTProcessor)` 생성자에 넘긴다.
- **오류 분류는 예외 타입으로:** Spring의 `NimbusJwtDecoder`는 토큰 문제를 `BadJwtException`으로, 공개키 조회 실패를 그냥 `JwtException`으로 던진다 (6.3.3 소스로 확인). 앞의 것은 401 `INVALID_SOCIAL_TOKEN`, 뒤의 것은 503 `SOCIAL_LOGIN_UNAVAILABLE`로 바꾼다. 단, "다시 받기 제한에 걸림"(`RateLimitReachedException`)은 키가 정말 없는 경우라 401이다.
- **설정이 없으면 확인기를 만들지 않는다:** 허용 `aud`(카카오 앱 키, 구글 클라이언트 ID)가 비어 있으면 빈을 등록하지 않는다. 그러면 1-A의 `SocialTokenVerifiers`가 400 `UNSUPPORTED_PROVIDER`를 준다 (명세 규칙).
- **대소문자·끝 공백 구분:** `provider_user_id` 콜레이션을 `utf8mb4_0900_bin`으로 바꾼다. 명세 초안의 `utf8mb4_bin`은 MySQL 8에서 PAD SPACE라 끝 공백을 여전히 무시한다. `utf8mb4_0900_bin`은 NO PAD라 끝 공백도 구분한다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| build.gradle.kts | 수정 | `spring-security-oauth2-jose` (버전은 Spring Boot가 관리, Nimbus 9.37.3 포함) |
| application.yml | 수정 | `fitness.auth.kakao.audiences: ${KAKAO_APP_KEYS:}`, `google.audiences: ${GOOGLE_CLIENT_IDS:}` |
| test/resources/application-test.yml | 수정 | 테스트용 허용 aud (`test-kakao-app-key`, `test-google-client-id`) |
| db/migration/V6__social_account_provider_user_id_bin.sql | 신규 | `provider_user_id` 콜레이션 → `utf8mb4_0900_bin` |
| user/domain/SocialAccount.kt | 수정 | `providerUserId`에 `columnDefinition`으로 같은 콜레이션 (로컬 `create`용) |
| common/exception/ErrorCode.kt | 수정 | `SOCIAL_LOGIN_UNAVAILABLE(503)` |
| user/client/OidcProviderSettings.kt | 신규 | provider별 iss·공개키 주소·aud·캐시/타임아웃 값, 카카오·구글 기본값 |
| user/client/OidcJwtDecoders.kt | 신규 | 설정으로 `JwtDecoder` 조립 (공개키 소스 + RS256 + 검증 규칙) |
| user/client/OidcIdTokenVerifier.kt | 신규 | `SocialTokenVerifier` 구현. 검증 후 `sub` → `SocialIdentity`, 예외 분류 |
| user/client/SocialLoginProperties.kt | 신규 | `fitness.auth.kakao/google.*` (audiences, jwkSetUri 덮어쓰기) |
| user/client/SocialLoginConfig.kt | 신규 | 카카오·구글 확인기 빈, aud가 있을 때만 (`@Conditional`) |
| test: support/FakeJwksServer.kt | 신규 | JDK 내장 HttpServer로 만든 가짜 공개키 서버 (키 교체, 장애, 지연, 요청 수 세기) |
| test: support/IdTokens.kt | 신규 | 테스트용 RSA 키와 id_token 만들기 (Nimbus) |
| test: support/FakeOidcInitializer.kt + IntegrationTest.kt | 신규·수정 | 통합 테스트 컨텍스트가 가짜 공개키 서버 주소를 쓰게 함 |
| test: 새 테스트 4개 파일 + 기존 1개 수정 | 신규·수정 | 아래 테스트 목록 |
| docs/decisions/0005-social-login-id-token.md | 신규 | ADR: id_token 검증 방식, 재사용 위험 수용, 공개키 캐시·제한 설정 |
| docs/specs/auth-social-oidc.md | 수정 | 콜레이션 `utf8mb4_bin` → `utf8mb4_0900_bin` |
| CLAUDE.md, docs/db-schema.md | 수정 | 환경변수 `KAKAO_APP_KEYS`·`GOOGLE_CLIENT_IDS`, 가짜 공개키 서버 테스트 방법, 콜레이션 |

## 데이터 변경

```sql
-- V6__social_account_provider_user_id_bin.sql
-- 소셜 사용자 식별자(id_token sub)는 대소문자와 끝 공백까지 구분한다 (계획 007, 006 리뷰 지적 5).
-- utf8mb4_bin은 PAD SPACE라 끝 공백을 무시하므로 NO PAD인 utf8mb4_0900_bin을 쓴다.
ALTER TABLE social_account
    MODIFY provider_user_id VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL
        COMMENT '소셜 서비스의 사용자 식별자 (id_token sub, 대소문자·끝 공백 구분)';
```
- 유니크 제약 `uk_social_account_provider_user (provider, provider_user_id)`는 그대로다. 비교 규칙만 바뀐다.
- 운영 데이터는 아직 없다. 있다면 대소문자만 다른 기존 행은 이제 서로 다른 값으로 본다 (충돌이 줄어드는 방향이라 실패하지 않는다).
- 새 테이블·Redis 키 없음. `email`은 저장하지 않는다 (명세).

## API

변경 없음. `POST /api/v1/auth/login {"provider": "KAKAO" | "GOOGLE", "token": "<id_token>"}`

| 상황 | 응답 |
| --- | --- |
| 검증 통과 | 200 LoginResponse (1-A와 같음) |
| 서명·iss·aud·exp·sub·알고리즘 문제, 키 없음 | 401 `INVALID_SOCIAL_TOKEN` |
| 공개키를 받을 수 없음 (그쪽 장애·타임아웃, 캐시된 키도 없음) | 503 `SOCIAL_LOGIN_UNAVAILABLE` |
| 허용 aud가 설정되지 않은 provider, `APPLE` | 400 `UNSUPPORTED_PROVIDER` |

---

## 1절. 판단이 들어간 코드 (꼭 봐 주세요)

### 1-1. OidcIdTokenVerifier: 검증과 오류 분류 (401 vs 503)

**Kotlin**
```kotlin
package com.fitness.user.client

/** 카카오·구글 id_token 확인기. provider별 차이(iss, 공개키 주소, aud)는 decoder 설정에 들어 있다 */
class OidcIdTokenVerifier(
    override val provider: AuthProvider,
    private val decoder: JwtDecoder,
) : SocialTokenVerifier {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun verify(token: String): SocialIdentity {
        val jwt = try {
            decoder.decode(token)
        } catch (e: BadJwtException) {
            // 서명·형식·iss·aud·exp·sub 문제 (JwtValidationException도 BadJwtException이다)
            throw BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN)
        } catch (e: JwtException) {
            // 다시 받기 제한에 걸림 = 30초 안에 이미 다시 받았는데도 그 kid가 없음 → 토큰 문제
            if (e.hasCause<RateLimitReachedException>()) throw BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN)
            // 그 밖의 JwtException = 공개키를 받지 못함 (회사 쪽 장애·타임아웃). 토큰 원문은 남기지 않는다
            log.warn("{} 공개키를 가져오지 못했습니다: {}", provider, e.message)
            throw BusinessException(ErrorCode.SOCIAL_LOGIN_UNAVAILABLE)
        }
        // sub 형식은 decoder의 검증 규칙이 이미 확인했다 (1-2)
        return SocialIdentity(provider, jwt.subject, email = null) // 이메일은 저장하지 않는다 (명세)
    }

    private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean =
        generateSequence(this as Throwable?) { it.cause }.any { it is T }
}
```

**Java**
```java
public class OidcIdTokenVerifier implements SocialTokenVerifier {
    private static final Logger log = LoggerFactory.getLogger(OidcIdTokenVerifier.class);
    private final AuthProvider provider;
    private final JwtDecoder decoder;

    @Override
    public SocialIdentity verify(String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (BadJwtException e) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        } catch (JwtException e) {
            if (hasCause(e, RateLimitReachedException.class)) throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            log.warn("{} 공개키를 가져오지 못했습니다: {}", provider, e.getMessage());
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_UNAVAILABLE);
        }
        return new SocialIdentity(provider, jwt.getSubject(), null);
    }

    private static boolean hasCause(Throwable e, Class<? extends Throwable> type) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (type.isInstance(t)) return true;
        }
        return false;
    }
}
```

**Kotlin 문법 설명**
- `catch (e: BadJwtException)` 다음 `catch (e: JwtException)`: Java처럼 위에서부터 맞는 첫 catch가 실행된다. `BadJwtException`은 `JwtException`의 하위 타입이라 먼저 둔다.
- `inline fun <reified T : Throwable> Throwable.hasCause()`: 타입 인자 `T`를 실행 중에도 알 수 있게 하는 함수(reified). 그래서 `it is T`처럼 타입 검사에 쓸 수 있다. Java에서는 `Class<T>`를 인자로 넘겨야 한다.
- `generateSequence(첫값) { 다음값 }`: null이 나올 때까지 이어지는 시퀀스. 여기서는 예외 → 원인 → 원인의 원인을 차례로 훑는다. Java의 for 루프와 같다.

**판단 포인트**
- **401과 503을 나누는 기준은 예외 타입:** Spring Security 6.3.3의 `NimbusJwtDecoder` 소스를 보면 토큰 문제는 `BadJwtException`이다. 공개키 조회 실패(`RemoteKeySourceException`, `KeySourceException`)는 그냥 `JwtException`으로 감싼다. 문구 비교가 아니라 타입으로 나눈다.
  - 확인 테스트: `공개키 서버에 연결할 수 없으면 SOCIAL_LOGIN_UNAVAILABLE 예외를 던진다`, `다른 키로 서명한 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`.
- **다시 받기 제한은 401:** 제한에 걸렸다는 것은 30초 안에 이미 최신 키를 받아 왔는데도 그 `kid`가 없다는 뜻이라 토큰 문제다. 503으로 두면 공격자가 장애처럼 보이게 만들 수 있다.
  - 확인 테스트: `없는 kid로 서명한 토큰이 연달아 와도 공개키 서버를 반복해서 부르지 않는다`.
- **로그:** 503일 때만 WARN을 남기고 토큰 원문은 남기지 않는다. 401은 사용자 입력 문제라 로그를 남기지 않는다 (005의 4xx 규칙).

### 1-2. OidcJwtDecoders: 공개키 소스와 검증 규칙 조립

**Kotlin**
```kotlin
package com.fitness.user.client

object OidcJwtDecoders {

    private const val JWK_SET_SIZE_LIMIT = 50 * 1024 // 50KB
    private const val CLOCK_SKEW_SECONDS = 30L
    private const val MAX_SUBJECT_LENGTH = 100 // social_account.provider_user_id VARCHAR(100)

    fun create(settings: OidcProviderSettings): JwtDecoder {
        val retriever = DefaultResourceRetriever(
            settings.connectTimeout.toMillis().toInt(),
            settings.readTimeout.toMillis().toInt(),
            JWK_SET_SIZE_LIMIT,
        )
        // 모르는 kid가 와도 다시 받기는 rateLimited 간격에 한 번. 장애 때는 받아 둔 키를 outageTolerant 동안 쓴다
        val jwkSource = JWKSourceBuilder.create<SecurityContext>(URL(settings.jwkSetUri), retriever)
            .cache(settings.cacheTtl.toMillis(), settings.readTimeout.toMillis() * 2)
            .refreshAheadCache(false) // 백그라운드 스레드를 만들지 않는다
            .rateLimited(settings.refetchMinInterval.toMillis())
            .retrying(true)
            .outageTolerant(settings.outageTolerance.toMillis())
            .build()

        val processor = DefaultJWTProcessor<SecurityContext>().apply {
            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource) // RS256만 받는다
            jwtClaimsSetVerifier = null // 클레임 검사는 아래 Spring 검증기 한곳에서만 한다
        }
        return NimbusJwtDecoder(processor).apply { setJwtValidator(validator(settings)) }
    }

    private fun validator(settings: OidcProviderSettings): OAuth2TokenValidator<Jwt> =
        DelegatingOAuth2TokenValidator(
            JwtTimestampValidator(Duration.ofSeconds(CLOCK_SKEW_SECONDS)),
            // JwtTimestampValidator는 exp가 없으면 통과시킨다. 만료 없는 토큰을 따로 거절한다 (006 리뷰 지적 4와 같은 구멍)
            rule("exp 없음") { it.expiresAt != null },
            // iss는 URL이 아닐 수도 있어(구글 accounts.google.com) 문자열로 비교한다
            rule("허용되지 않은 발급자") { it.getClaimAsString(JwtClaimNames.ISS) in settings.issuers },
            rule("허용되지 않은 앱") { jwt -> jwt.audience.orEmpty().any { it in settings.audiences } },
            rule("sub 형식 오류") { val sub = it.subject; !sub.isNullOrBlank() && sub.length <= MAX_SUBJECT_LENGTH },
        )

    private fun rule(description: String, check: (Jwt) -> Boolean) = OAuth2TokenValidator<Jwt> { jwt ->
        if (check(jwt)) OAuth2TokenValidatorResult.success()
        else OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", description, null))
    }
}
```

**Java**
```java
public final class OidcJwtDecoders {
    public static JwtDecoder create(OidcProviderSettings s) throws MalformedURLException {
        DefaultResourceRetriever retriever = new DefaultResourceRetriever(
                (int) s.getConnectTimeout().toMillis(), (int) s.getReadTimeout().toMillis(), 50 * 1024);
        JWKSource<SecurityContext> jwkSource = JWKSourceBuilder.<SecurityContext>create(new URL(s.getJwkSetUri()), retriever)
                .cache(s.getCacheTtl().toMillis(), s.getReadTimeout().toMillis() * 2)
                .refreshAheadCache(false)
                .rateLimited(s.getRefetchMinInterval().toMillis())
                .retrying(true)
                .outageTolerant(s.getOutageTolerance().toMillis())
                .build();

        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
        processor.setJWTClaimsSetVerifier(null);

        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ofSeconds(30)),
                rule("exp 없음", jwt -> jwt.getExpiresAt() != null),
                rule("허용되지 않은 발급자", jwt -> s.getIssuers().contains(jwt.getClaimAsString(JwtClaimNames.ISS))),
                rule("허용되지 않은 앱", jwt -> jwt.getAudience() != null
                        && jwt.getAudience().stream().anyMatch(s.getAudiences()::contains)),
                rule("sub 형식 오류", jwt -> jwt.getSubject() != null && !jwt.getSubject().isBlank()
                        && jwt.getSubject().length() <= 100)));
        return decoder;
    }

    private static OAuth2TokenValidator<Jwt> rule(String description, Predicate<Jwt> check) {
        return jwt -> check.test(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
```

**Kotlin 문법 설명**
- `object OidcJwtDecoders`: 인스턴스가 하나뿐인 싱글턴. 안의 함수는 Java의 static 메서드처럼 `OidcJwtDecoders.create(...)`로 부른다.
- `JWKSourceBuilder.create<SecurityContext>(...)`: 제네릭 static 메서드의 타입 인자를 명시한다. Java의 `JWKSourceBuilder.<SecurityContext>create(...)`.
- `jwsKeySelector = ...` (apply 블록 안): Java setter `setJWSKeySelector(...)`를 프로퍼티 대입처럼 쓴다.
- `check: (Jwt) -> Boolean`: 함수 타입 파라미터. Java의 `Predicate<Jwt>`.
- `OAuth2TokenValidator<Jwt> { jwt -> ... }`: Java 인터페이스(메서드 하나)를 람다로 구현한다 (SAM 변환).
- `in settings.issuers`: `contains` 호출. `x in set` == `set.contains(x)`.
- `jwt.audience.orEmpty()`: null이면 빈 리스트. Java의 `list == null ? List.of() : list`.

**판단 포인트**
- **RS256만:** `JWSVerificationKeySelector(RS256, ...)`는 헤더 `alg`가 RS256이 아니면 맞는 키가 없다고 본다. 그래서 공개키를 HS256 비밀키로 쓰는 공격(알고리즘 혼동)과 `alg: none`을 막는다.
  - 확인 테스트: `HS256으로 서명한 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`.
- **`exp` 필수:** Spring의 `JwtTimestampValidator`는 `exp`가 없으면 검사를 건너뛴다. 1-A 리뷰에서 우리 토큰에도 같은 구멍이 지적됐다 (그때는 낮음으로 두었다). 외부 토큰은 우리가 만든 것이 아니므로 반드시 막는다.
  - 확인 테스트: `exp가 없는 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`.
- **공개키 소스 설정값과 이유:**
  | 설정 | 값 | 이유 |
  | --- | --- | --- |
  | 캐시 | 1시간 | 요청마다 외부를 부르지 않는다 (p95 300ms). 카카오 권고 |
  | 모르는 kid → 다시 받기 | 30초에 한 번 | 키 교체에는 대응하고, 아무 kid로 카카오를 반복 호출하게 만드는 공격은 막는다 |
  | 재시도 | 1회 | 일시적 네트워크 오류 흡수 |
  | 장애 허용 | 12시간 | 받아 둔 키로 계속 검증한다. 회사 쪽 장애가 우리 로그인 장애로 바로 번지지 않게 |
  | 타임아웃 | 연결 1초, 읽기 1초 | 장애 때 로그인 요청이 오래 붙잡히지 않게 |
  | 크기 제한 | 50KB | 비정상적으로 큰 응답을 받지 않게 |
- **캐시는 서버 메모리:** 로그인 상태가 아니라 회사가 공개한 키의 사본이다. 서버마다 따로 받아도 결과가 같으므로 "서버 메모리 상태 금지" 원칙과 충돌하지 않는다 (명세).
- **`jwtClaimsSetVerifier = null`:** Nimbus 자체의 클레임 검사를 끈다. 그러면 iss·aud·exp 판단이 Spring 검증기 한곳에만 있다. Spring의 기본 빌더도 같은 방식으로 끈다.

### 1-3. SocialLoginConfig: 허용 aud가 있을 때만 확인기 등록

**Kotlin**
```kotlin
package com.fitness.user.client

@Configuration
class SocialLoginConfig(private val properties: SocialLoginProperties) {

    @Bean
    @Conditional(OnKakaoConfigured::class)
    fun kakaoIdTokenVerifier(): SocialTokenVerifier =
        OidcIdTokenVerifier(AuthProvider.KAKAO, OidcJwtDecoders.create(OidcProviderSettings.kakao(properties.kakao)))

    @Bean
    @Conditional(OnGoogleConfigured::class)
    fun googleIdTokenVerifier(): SocialTokenVerifier =
        OidcIdTokenVerifier(AuthProvider.GOOGLE, OidcJwtDecoders.create(OidcProviderSettings.google(properties.google)))
}

/** 허용 aud가 하나라도 있으면 true. 환경변수가 비면("") 빈 문자열 하나로 바인딩될 수 있어 공백은 빼고 센다 */
abstract class OnAudiencesConfigured(private val prefix: String) : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        Binder.get(context.environment)
            .bind("$prefix.audiences", Bindable.listOf(String::class.java))
            .map { audiences -> audiences.any { it.isNotBlank() } }
            .orElse(false)
}

class OnKakaoConfigured : OnAudiencesConfigured("fitness.auth.kakao")
class OnGoogleConfigured : OnAudiencesConfigured("fitness.auth.google")
```

**Java**
```java
public abstract class OnAudiencesConfigured implements Condition {
    private final String prefix;
    protected OnAudiencesConfigured(String prefix) { this.prefix = prefix; }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Binder.get(context.getEnvironment())
                .bind(prefix + ".audiences", Bindable.listOf(String.class))
                .map(audiences -> audiences.stream().anyMatch(a -> !a.isBlank()))
                .orElse(false);
    }
}

public class OnKakaoConfigured extends OnAudiencesConfigured {
    public OnKakaoConfigured() { super("fitness.auth.kakao"); }
}
```

**Kotlin 문법 설명**
- `abstract class X(private val prefix: String) : Condition`: 생성자 파라미터를 가진 추상 클래스. `class OnKakaoConfigured : OnAudiencesConfigured("...")`는 부모 생성자에 값을 넘기며 상속한다. Java의 `super("...")`.
- `"$prefix.audiences"`: 문자열 템플릿. 변수 이름 뒤의 `.`은 템플릿에 포함되지 않는다.

**판단 포인트**
- **설정이 비면 빈을 만들지 않는다:** 허용 aud가 빈 확인기를 만들면 모든 토큰이 "허용되지 않은 앱"으로 401이 된다. 그러면 배포 설정 누락이 "사용자 토큰 문제"로 보인다. 빈을 만들지 않으면 1-A의 `SocialTokenVerifiers`가 400 `UNSUPPORTED_PROVIDER`로 응답해 원인이 드러난다 (명세 규칙).
- **`@ConditionalOnProperty`를 쓰지 않는 이유:** 목록 설정은 YAML 목록(`audiences[0]`)으로도, 쉼표 문자열로도 들어올 수 있다. 문자열 비교 조건은 목록 형태를 놓친다. 그래서 `Binder`로 실제 바인딩 결과를 본다.
  - 확인 테스트: `허용 aud가 비어 있으면 카카오 확인기를 등록하지 않는다`, `허용 aud를 쉼표 목록으로 설정하면 카카오 확인기를 등록한다`.
- 운영에서 `KAKAO_APP_KEYS`를 빠뜨리면 카카오 로그인은 400이다. 배포 체크리스트에 넣는다 (CLAUDE.md).

### 1-4. V6 마이그레이션 + 엔티티 콜레이션

**Kotlin (SocialAccount.kt 수정분)**
```kotlin
// 대소문자·끝 공백을 구분한다 (V6). 로컬 ddl-auto=create도 같은 콜레이션을 만들도록 columnDefinition에 적는다
@Column(nullable = false, length = 100, columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin")
val providerUserId: String,
```
(SQL은 "데이터 변경" 절)

**판단 포인트**
- **`utf8mb4_0900_bin` (명세의 `utf8mb4_bin`에서 바꿈):** MySQL 8에서 `utf8mb4_bin`은 PAD SPACE라 `'abc'`와 `'abc '`를 같게 본다. `utf8mb4_0900_bin`은 NO PAD라 둘을 다르게 본다. 명세 규칙("대소문자와 끝 공백을 구분한다")을 지키려면 이쪽이어야 한다. 승인되면 명세 문구도 고친다.
- **`ddl-auto=validate`와 충돌하지 않는다:** validate는 타입(VARCHAR)만 비교하고 콜레이션은 보지 않는다. 콜레이션이 실제로 적용됐는지는 DB 원본(information_schema)을 읽는 테스트가 확인한다 (CLAUDE.md "DB 원본 값 직접 확인" 원칙).
  - 확인 테스트: `provider_user_id 컬럼은 utf8mb4_0900_bin 콜레이션이다`, `대소문자만 다른 소셜 ID는 서로 다른 계정으로 저장된다`, `끝 공백만 다른 소셜 ID로 조회하면 찾지 못한다`.

---

## 2절. 전체 코드 (정형 코드, 훑어보기용)

> 1절에서 설명한 문법만 쓰는 파일은 Java 버전을 생략했다.

### build.gradle.kts (추가분)
```kotlin
// 소셜 id_token 검증 (Nimbus JOSE+JWT 포함). 우리 토큰은 jjwt (ADR 0004, 0005)
implementation("org.springframework.security:spring-security-oauth2-jose")
```
`spring-boot-starter-oauth2-resource-server`는 넣지 않는다. 넣으면 리소스 서버 자동설정이 켜져 우리 JWT 필터와 겹친다.

### application.yml (추가분)
```yaml
fitness:
  auth:
    # 허용할 앱 식별자(id_token의 aud). 쉼표로 여러 개. 비어 있으면 그 소셜 로그인은 꺼진다(400 UNSUPPORTED_PROVIDER)
    kakao:
      audiences: ${KAKAO_APP_KEYS:}      # 카카오 앱 키 (SDK를 초기화한 키)
    google:
      audiences: ${GOOGLE_CLIENT_IDS:}   # 구글 OAuth 클라이언트 ID (앱이 id_token을 받을 때 쓰는 것)
```
앱 키·클라이언트 ID는 비밀값이 아니다 (앱에 들어 있다). 다만 환경마다 달라 환경변수로 받는다.

### application-test.yml (추가분)
```yaml
fitness:
  auth:
    kakao:
      audiences: test-kakao-app-key
    google:
      audiences: test-google-client-id
    # jwk-set-uri는 FakeOidcInitializer가 가짜 공개키 서버 주소로 넣는다
```

### common/exception/ErrorCode.kt (추가분)
```kotlin
SOCIAL_LOGIN_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인에 잠시 연결할 수 없습니다"),
```
`fromStatus`는 바꾸지 않는다. 503은 우리 코드가 `BusinessException`으로만 던진다.

### user/client/SocialLoginProperties.kt
```kotlin
@ConfigurationProperties("fitness.auth")
data class SocialLoginProperties(
    val kakao: Provider = Provider(),
    val google: Provider = Provider(),
) {
    data class Provider(
        val audiences: List<String> = emptyList(),
        val jwkSetUri: String? = null, // 테스트에서 가짜 공개키 서버로 바꿀 때만 쓴다
    )
}
```
- `data class` 안의 `data class`: Java의 static 중첩 record. 설정 `fitness.auth.kakao.audiences`가 `kakao.audiences`로 바인딩된다.

### user/client/OidcProviderSettings.kt
```kotlin
data class OidcProviderSettings(
    val provider: AuthProvider,
    val issuers: Set<String>,
    val audiences: Set<String>,
    val jwkSetUri: String,
    val connectTimeout: Duration = Duration.ofSeconds(1),
    val readTimeout: Duration = Duration.ofSeconds(1),
    val cacheTtl: Duration = Duration.ofHours(1),
    val refetchMinInterval: Duration = Duration.ofSeconds(30),
    val outageTolerance: Duration = Duration.ofHours(12),
) {
    companion object {
        // 값은 각 회사의 OpenID 설정 문서(.well-known/openid-configuration)로 확인 (2026-11-06)
        private const val KAKAO_ISSUER = "https://kauth.kakao.com"
        private const val KAKAO_JWK_SET_URI = "https://kauth.kakao.com/.well-known/jwks.json"
        private val GOOGLE_ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")
        private const val GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs"

        fun kakao(p: SocialLoginProperties.Provider) = OidcProviderSettings(
            provider = AuthProvider.KAKAO,
            issuers = setOf(KAKAO_ISSUER),
            audiences = p.audiences.filter { it.isNotBlank() }.toSet(),
            jwkSetUri = p.jwkSetUri ?: KAKAO_JWK_SET_URI,
        )

        fun google(p: SocialLoginProperties.Provider) = OidcProviderSettings(
            provider = AuthProvider.GOOGLE,
            issuers = GOOGLE_ISSUERS,
            audiences = p.audiences.filter { it.isNotBlank() }.toSet(),
            jwkSetUri = p.jwkSetUri ?: GOOGLE_JWK_SET_URI,
        )
    }
}
```

### db/migration/V6, SocialAccount.kt
1절 1-4와 "데이터 변경" 절.

### 테스트 지원 코드

**support/FakeJwksServer.kt**: JDK 내장 `com.sun.net.httpserver.HttpServer`를 쓰고, 의존성을 추가하지 않는다.
```kotlin
/** 가짜 공개키(JWKS) 서버. 키 교체·장애·지연을 흉내 내고 요청 수를 센다 */
class FakeJwksServer : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    @Volatile var keys: List<RSAKey> = emptyList()
    @Volatile var failing = false              // true면 500
    @Volatile var delay: Duration = Duration.ZERO
    val requestCount = AtomicInteger()

    init {
        server.createContext("/jwks") { exchange ->
            requestCount.incrementAndGet()
            Thread.sleep(delay.toMillis())
            val body = JWKSet(keys.map { it.toPublicJWK() }).toString().toByteArray()
            exchange.sendResponseHeaders(if (failing) 500 else 200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
    }

    val uri: String get() = "http://127.0.0.1:${server.address.port}/jwks"

    override fun close() = server.stop(0)
}
```
- `@Volatile`: 다른 스레드(서버 스레드)가 바로 최신 값을 보게 한다. Java의 `volatile` 필드와 같다.
- `AutoCloseable` + `close()`: 테스트에서 `use { }`(Java의 try-with-resources)로 쓴다.

**support/IdTokens.kt**
```kotlin
/** 테스트용 RSA 키와 id_token. 기본값은 "카카오가 우리 앱에 발급한 유효한 토큰" */
object IdTokens {
    fun rsaKey(kid: String = "kid-" + UUID.randomUUID()): RSAKey = RSAKeyGenerator(2048).keyID(kid).generate()

    fun sign(
        key: RSAKey,
        sub: String? = "kakao-" + UUID.randomUUID(),
        iss: String = "https://kauth.kakao.com",
        aud: String = "test-kakao-app-key",
        exp: Instant? = Instant.now().plus(Duration.ofHours(1)),
    ): String {
        val claims = JWTClaimsSet.Builder().issuer(iss).audience(aud).issueTime(Date())
            .apply { sub?.let { subject(it) }; exp?.let { expirationTime(Date.from(it)) } }
            .build()
        return SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(), claims)
            .apply { sign(RSASSASigner(key)) }
            .serialize()
    }

    fun signHs256(secret: ByteArray, kid: String): String = // 알고리즘 혼동 공격 흉내
        SignedJWT(JWSHeader.Builder(JWSAlgorithm.HS256).keyID(kid).build(),
            JWTClaimsSet.Builder().issuer("https://kauth.kakao.com").audience("test-kakao-app-key").subject("x")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600))).build())
            .apply { sign(MACSigner(secret)) }
            .serialize()
}
```
- `sub?.let { subject(it) }`: sub가 null이 아닐 때만 실행한다. Java의 `if (sub != null) builder.subject(sub);`.

**support/FakeOidcInitializer.kt + IntegrationTest.kt 수정**
```kotlin
/** 통합 테스트용 가짜 카카오·구글 공개키 서버. 테스트 JVM 안에서 한 번만 띄운다 */
object FakeOidcProviders {
    val kakaoKey: RSAKey = IdTokens.rsaKey("kakao-test-kid")
    val googleKey: RSAKey = IdTokens.rsaKey("google-test-kid")
    val kakao = FakeJwksServer().apply { keys = listOf(kakaoKey) }
    val google = FakeJwksServer().apply { keys = listOf(googleKey) }
}

class FakeOidcInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(context: ConfigurableApplicationContext) {
        TestPropertyValues.of(
            "fitness.auth.kakao.jwk-set-uri=${FakeOidcProviders.kakao.uri}",
            "fitness.auth.google.jwk-set-uri=${FakeOidcProviders.google.uri}",
        ).applyTo(context)
    }
}

// IntegrationTest.kt에 한 줄 추가 (모든 통합 테스트가 같은 컨텍스트를 공유한다)
@ContextConfiguration(initializers = [FakeOidcInitializer::class])
```
- `@DynamicPropertySource` 대신 initializer를 쓰는 이유: `@DynamicPropertySource`는 테스트 클래스마다 적어야 하고, 클래스마다 설정이 달라지면 컨텍스트가 따로 떠서 Flyway clean이 반복된다.

### 테스트 대표 코드
```kotlin
class OidcIdTokenVerifierTest {

    private val server = FakeJwksServer()
    private val key = IdTokens.rsaKey()

    @AfterEach
    fun tearDown() = server.close()

    private fun verifier(settings: OidcProviderSettings = kakaoSettings()) =
        OidcIdTokenVerifier(settings.provider, OidcJwtDecoders.create(settings))

    private fun kakaoSettings() = OidcProviderSettings.kakao(
        SocialLoginProperties.Provider(audiences = listOf("test-kakao-app-key"), jwkSetUri = server.uri),
    )

    @Test
    fun `없는 kid로 서명한 토큰이 연달아 와도 공개키 서버를 반복해서 부르지 않는다`() {
        // given
        server.keys = listOf(key)
        val verifier = verifier()
        val unknownKidTokens = (1..5).map { IdTokens.sign(IdTokens.rsaKey()) } // 서버에 없는 kid 5개

        // when
        unknownKidTokens.forEach { runCatching { verifier.verify(it) } }

        // then: 처음 받기 1번 + kid가 없어 다시 받기 1번. 나머지는 30초 제한에 걸린다
        assertThat(server.requestCount.get()).isLessThanOrEqualTo(2)
    }
}
```

---

## 정합성·동시성·멱등성

- **동시 첫 로그인:** 1-A의 처리(유니크 제약 + 새 트랜잭션 재조회)를 그대로 탄다. 1-B는 `sub`를 넘기기만 한다.
- **공개키 캐시 동시 접근:** Nimbus의 캐시 소스가 내부 잠금으로 동시에 들어온 요청 중 하나만 다시 받게 한다. 직접 잠금을 만들지 않는다.
- **재사용(replay):** 명세 결정에 따라 막지 않는다. id_token 수명 안에서는 같은 토큰으로 여러 번 로그인할 수 있다. 위험과 이유를 ADR 0005에 남긴다.
- **콜레이션 변경:** 비교가 더 엄격해지는 방향이다. 그래서 기존 유니크 값끼리 새로 충돌하는 일은 없다.

## 실패·대체 경로

| 상황 | 동작 |
| --- | --- |
| 처음 받기 실패 (캐시 없음) | 1초 타임아웃 + 1회 재시도 후 503 `SOCIAL_LOGIN_UNAVAILABLE`, WARN 로그 |
| 받아 둔 키가 있는데 회사 쪽 장애 | 캐시가 만료돼도 12시간 동안은 받아 둔 키로 계속 검증 (로그인 정상) |
| 회사가 키를 교체 | 모르는 kid → 즉시 다시 받아 검증. 30초 안에 두 번째 교체가 오면 그 30초 동안 401 (드묾) |
| 비정상 응답 (50KB 초과, JSON 아님) | 공개키를 받지 못한 것으로 보고 503 |
| 앱 설정과 서버 aud 불일치 | 모든 로그인 401. 배포 확인 항목으로 둔다 (CLAUDE.md) |

외부 호출은 공개키 조회뿐이고 로그인 요청마다 일어나지 않는다. DB 트랜잭션 안에서 부르지 않는다 (확인은 `AuthService.login`의 트랜잭션 밖에서 일어난다).

## 테스트 목록

**단위 `OidcIdTokenVerifierTest`** (가짜 공개키 서버 + 실제 Nimbus decoder, Spring 없이)
- [ ] `카카오가 우리 앱에 발급한 유효한 id_token이면 sub로 신원을 돌려준다`
- [ ] `다른 키로 서명한 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`
- [ ] `HS256으로 서명한 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다` (알고리즘 혼동)
- [ ] `발급자가 카카오가 아닌 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다` (구글 iss 토큰을 카카오로)
- [ ] `허용 목록에 없는 앱의 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다` (aud)
- [ ] `만료된 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`
- [ ] `exp가 없는 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`
- [ ] `sub가 100자를 넘으면 INVALID_SOCIAL_TOKEN 예외를 던진다`
- [ ] `구글 확인기는 https 없는 발급자 accounts.google.com도 받는다`
- [ ] `공개키 서버에 연결할 수 없으면 SOCIAL_LOGIN_UNAVAILABLE 예외를 던진다`
- [ ] `공개키 서버가 응답하지 않으면 몇 초 안에 SOCIAL_LOGIN_UNAVAILABLE 예외를 던진다` (지연 5초, 3초 안에 끝남)
- [ ] `키가 교체돼 새 kid로 서명한 토큰이 오면 공개키를 다시 받아 검증한다`
- [ ] `없는 kid로 서명한 토큰이 연달아 와도 공개키 서버를 반복해서 부르지 않는다`
- [ ] `공개키를 받은 뒤 공개키 서버가 장애여도 받아 둔 키로 검증한다`

**단위 `SocialLoginConfigTest`** (`ApplicationContextRunner`)
- [ ] `허용 aud가 비어 있으면 카카오 확인기를 등록하지 않는다` (`KAKAO_APP_KEYS` 미설정 = `application.yml` 그대로)
- [ ] `허용 aud를 쉼표 목록으로 설정하면 카카오 확인기를 등록한다`

**통합 `SocialLoginTest`** (`@IntegrationTest`, MockMvc, 가짜 공개키 서버)
- [ ] `카카오 id_token으로 처음 로그인하면 ONBOARDING 회원으로 가입하고 토큰을 발급한다`
- [ ] `구글 id_token으로 로그인한 회원의 내 정보에는 GOOGLE 계정이 연결돼 있다`
- [ ] `위조된 카카오 id_token으로 로그인하면 401 INVALID_SOCIAL_TOKEN으로 응답한다`
- [ ] `같은 sub라도 카카오와 구글로 로그인하면 서로 다른 회원이 된다`

**저장소 `SocialAccountCollationTest`** (`@RepositoryTest`)
- [ ] `provider_user_id 컬럼은 utf8mb4_0900_bin 콜레이션이다` (information_schema 원본 확인)
- [ ] `대소문자만 다른 소셜 ID는 서로 다른 계정으로 저장된다`
- [ ] `끝 공백만 다른 소셜 ID로 조회하면 찾지 못한다`

**기존 테스트 수정**
- [ ] `AuthControllerTest`의 `확인기가 없는 provider로 로그인하면 400 UNSUPPORTED_PROVIDER로 응답한다`: KAKAO가 이제 테스트에서 켜져 있으므로 `APPLE`로 바꾼다.

## 작업 순서

1. 승인되면 이 계획을 `docs/plans/007-auth-social-oidc.md`에 저장하고 ADR 0005를 쓴다. 명세의 콜레이션 문구를 고친다.
2. 의존성을 추가하고 테스트 지원 코드(가짜 공개키 서버, IdTokens, initializer)와 설정을 넣는다. 컴파일용 뼈대(ErrorCode, 설정 클래스, `TODO()` 본문)를 만든다.
3. **테스트를 작성하고 구현이 없어서 실패하는 것을 확인한다.**
4. 구현한다: V6·엔티티 → OidcProviderSettings·OidcJwtDecoders → OidcIdTokenVerifier → SocialLoginConfig. 테스트를 통과시킨다.
5. CLAUDE.md와 db-schema.md를 갱신한다.
6. `./gradlew test` 전체 통과 → `/review-change` → 커밋 → ai-log 기록.

## 검증

- `./gradlew test`: 기존 93개 + 새 테스트 통과 (로컬 MySQL·Redis 필요. 가짜 공개키 서버는 테스트가 띄운다).
- 실제 카카오·구글 공개키 주소에 연결해 키 목록을 받을 수 있는지 한 번 확인한다. 실제 서명 토큰은 앱이 있어야 받을 수 있어 확인하지 않는다 (명세 범위 밖, 착수 계획 5단계).
- `bootRun`(로컬, aud 미설정)에서 `{"provider":"KAKAO", ...}` 로그인 → 400 `UNSUPPORTED_PROVIDER`. 제가 띄운 프로세스만 종료한다.

## 확인 필요

1. **콜레이션 `utf8mb4_0900_bin`:** 명세의 `utf8mb4_bin`과 다르다. 이유는 1-4에 적었다. 승인하시면 명세도 고친다.
2. **공개키 캐시·제한 값** (1시간, 30초, 12시간, 1초): 공식 권장 값이 없어 정한 값이다. 운영하며 조정할 수 있게 `OidcProviderSettings` 한곳에 모았다.

## 변경 이력
- 2026-11-09: 승인. 콜레이션을 명세의 `utf8mb4_bin` 대신 `utf8mb4_0900_bin`(NO PAD)으로 확정하고 명세 문구를 맞췄다.
- 2026-11-09: 구현 중 변경. `URL(String)`이 JDK 20부터 deprecated라 `URI(...).toURL()`을 쓴다. Nimbus는 "다시 받기 제한 간격 < 캐시 수명"을 요구해, 장애 허용 테스트는 캐시 200ms·제한 100ms로 둔다 (운영 값 30초 < 1시간은 그대로). Nimbus의 다시 받기 제한은 간격마다 2회(처음 받기 포함)라 키 교체 직후에도 새 kid를 받을 수 있다. 실제 카카오·구글 공개키 주소 확인: 둘 다 RS256 키 2개, 구글 Cache-Control max-age 약 5.4시간(캐시 1시간이 그 안), 카카오는 Cache-Control 없음.
- 2026-11-09: 리뷰(/review-change) 지적 7건 중 중간 2건을 고쳤다. (1) Nimbus는 실패한 공개키 받기도 다시 받기 제한 횟수로 세어, 캐시가 빈 상태의 장애에서 세 번째 요청부터 `RateLimitReachedException`이 나 401이 됐다 (실행 재현). 실제 HTTP 받기를 `FailureTrackingJWKSetSource`로 감싸 마지막 받기 결과를 기록하고, 제한에 걸렸어도 마지막 받기가 실패였으면 503으로 나눈다. `OidcJwtDecoders.create`는 이 상태를 함께 가진 `OidcJwtDecoder`를 돌려준다. (2) 다시 받기 제한 테스트의 요청 수 단언을 `<= 2`에서 `== 2`로 바꾸고, "제한에 걸린 없는 kid 토큰은 401", "받은 적 없이 장애가 이어지면 제한에 걸려도 503" 테스트를 추가했다 (401 분기를 지우면 실패하는 것을 확인). 낮음 5건(캐시 갱신 대기 2초가 재시도 최악 소요보다 짧음, 장애 허용 테스트가 캐시 만료를 확인하지 않음, 설정 테스트가 셸 환경변수에 영향받음, 타임아웃 테스트 5초 지연, CLAUDE.md 경로 표기)은 사용자 결정으로 고치지 않았다.
