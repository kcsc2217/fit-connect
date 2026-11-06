# 006 인증 1-A: JWT 발급·갱신·로그아웃과 내 정보 조회 구현 계획

- 명세: docs/specs/auth-jwt.md (확정 2026-11-04)
- 크기: M
- 상태: 승인됨 (2026-11-05)

> 아래 코드는 계획 시점 초안이다. 테스트·구현 중 세부가 바뀔 수 있고, 크게 바뀌면 "변경 이력"에 남긴다.

## Context

모든 기능 API는 "누가 요청했는지(userId)"를 알아야 한다. 그래서 첫 기능 API로 인증을 만든다. 1-A에서는 우리 토큰 체계를 만든다. access(JWT)를 발급하고 검증하며, refresh(Redis)를 교체하고 폐기한다. 소셜 신원 확인은 인터페이스 뒤에 숨기고, 개발용 확인기 하나만 둔다. 카카오·구글 OIDC 검증은 1-B에서 그 인터페이스에 구현을 추가하는 것으로 끝나게 한다.

## 접근

- Spring Security는 **세션 없이(STATELESS)** 쓴다. 우리가 만든 `JwtAuthenticationFilter`가 요청마다 access를 검증한다.
  - 필터는 예외를 던지지 않는다. 검증에 성공하면 SecurityContext에 userId를 넣는다.
  - 실패하면 이유(만료인지 무효인지)를 요청 속성에 남기고 다음 필터로 넘긴다.
  - 인증이 필요한 경로라면 Spring Security가 EntryPoint를 부른다. EntryPoint는 남겨진 이유를 보고 401 ProblemDetail(`TOKEN_EXPIRED` 또는 `UNAUTHORIZED`)을 쓴다.
- JWT 라이브러리는 **jjwt 0.12**를 쓴다.
  - 만료와 그 밖의 무효를 예외 타입(`ExpiredJwtException`)으로 깔끔하게 구분할 수 있다.
  - Spring의 Nimbus `JwtDecoder`는 만료를 오류 문자열로만 알려 준다. 1-B의 JWKS 검증에는 Nimbus를 쓸 수 있다 (확인 필요 1).
- refresh는 JWT가 아니라 `"{userId}.{tokenId}"` 형태의 랜덤 값이다. Redis 키는 `refresh:{userId}:{tokenId}`이다.
  - 갱신할 때 **`DEL`의 반환값(지운 개수)**으로 "꺼내면서 지우기"를 한 번에 한다.
  - 같은 토큰으로 동시에 요청해도 `DEL`이 1을 돌려주는 요청은 하나뿐이다.
- 같은 소셜 계정의 첫 로그인이 동시에 들어올 수 있다.
  - 가입은 트랜잭션 하나로 한다. `social_account` 유니크 제약 위반이 나면 그 트랜잭션은 통째로 롤백된다 (users 행도 함께 사라짐).
  - 롤백된 쪽은 **새 트랜잭션에서 다시 조회**한다. 그래서 `AuthService`에는 트랜잭션을 걸지 않는다.
- 개발용 로그인은 **프로파일이 아니라 설정값 `fitness.auth.dev-login.enabled=true`**로 켠다. 명세의 "local·test에서만"을 더 안전하게 구현하는 방법이다.
  - `application.yml`의 기본 활성 프로파일이 `local`이다. 운영에서 프로파일 지정을 빠뜨리면 local 조건이 켜진다.
  - 이 설정값은 `application-local.yml`(jar에서 제외)과 `application-test.yml`에만 둔다. 그러면 운영 jar에는 이 값이 들어갈 길이 없다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| build.gradle.kts | 수정 | `spring-boot-starter-security`, jjwt(api/impl/jackson) 0.12.6 |
| FitnessApplication.kt | 수정 | `@ConfigurationPropertiesScan`, 기본 UserDetailsService 자동설정 제외 |
| application.yml | 수정 | `fitness.jwt.secret: ${JWT_SECRET:}` (비면 시작 실패) |
| test/resources/application-test.yml | 수정 | 테스트 전용 서명 키, 개발 로그인 켬, `/test-support/**` 공개 |
| application-local.yml (미커밋) | 수정 | 로컬 서명 키(랜덤 생성, 출력하지 않음), 개발 로그인 켬 |
| common/exception/ErrorCode.kt | 수정 | UNAUTHORIZED, TOKEN_EXPIRED, INVALID_REFRESH_TOKEN, INVALID_SOCIAL_TOKEN, UNSUPPORTED_PROVIDER, FORBIDDEN, 401·403 매핑 |
| common/exception/ProblemDetails.kt | 신규 | `problemDetail(code, detail, path)` 생성 함수 (핸들러·필터가 같이 씀) |
| common/exception/GlobalExceptionHandler.kt | 수정 | private `problem()` → 위 함수 사용 |
| config/ClockConfig.kt | 신규 | `Clock` 빈 (UTC). 토큰 시각을 테스트에서 고정하려고 |
| config/SwaggerConfig.kt | 수정 | Bearer 인증 스킴 |
| user/domain/AuthProvider.kt | 수정 | `DEV` 추가 |
| user/domain/RefreshToken.kt | 신규 | refresh 값 객체 (발급·파싱, toString 가림) |
| user/security/JwtProperties.kt | 신규 | `fitness.jwt.*` (secret, accessTtl 30분, refreshTtl 14일) |
| user/security/JwtProvider.kt | 신규 | access 발급·검증 (HS256 고정, 만료/무효 구분) |
| user/security/JwtAuthenticationFilter.kt | 신규 | Bearer 추출 → 검증 → SecurityContext |
| user/security/SecurityConfig.kt | 신규 | 필터 체인, 공개 경로, 401·403 처리기 |
| user/security/PublicPathProperties.kt | 신규 | `fitness.security.extra-public-paths` (테스트용 경로 추가) |
| user/security/ProblemDetailWriter.kt | 신규 | 필터 단계에서 ProblemDetail JSON 쓰기 |
| user/security/ProblemDetailAuthenticationEntryPoint.kt | 신규 | 401 |
| user/security/ProblemDetailAccessDeniedHandler.kt | 신규 | 403 |
| user/client/SocialTokenVerifier.kt | 신규 | 인터페이스 + `SocialIdentity` |
| user/client/SocialTokenVerifiers.kt | 신규 | provider → 확인기 선택, 없으면 UNSUPPORTED_PROVIDER |
| user/client/DevTokenVerifier.kt | 신규 | 개발용 (설정값으로만 켜짐) |
| user/repository/RefreshTokenRepository.kt | 신규 | Redis 저장·원자 삭제 |
| user/repository/SocialAccountRepository.kt | 수정 | `findAllByUserId` 추가 |
| user/service/UserAccountService.kt | 신규 | 소셜 계정으로 회원 찾기, 가입(트랜잭션) |
| user/service/AuthService.kt | 신규 | 로그인·갱신·로그아웃 |
| user/service/UserService.kt | 신규 | 내 정보 조회 |
| user/controller/AuthController.kt, UserController.kt | 신규 | API 4개 |
| user/dto/*.kt | 신규 | LoginRequest, RefreshTokenRequest, TokenResponse, LoginResponse, MeResponse, AccountInfo |
| test: GlobalExceptionHandlerTest.kt | 수정 | 404 테스트를 인증된 요청으로 |
| test: 새 테스트 8개 파일 | 신규 | 아래 테스트 목록 |
| docs/decisions/0004-auth-token.md | 신규 | ADR: access JWT(HS256, 30분) + 불투명 refresh(Redis, 교체), 재사용 감지 시 그 요청만 401 |
| CLAUDE.md, docs/db-schema.md | 수정 | 인증 규칙(JWT_SECRET, 개발 로그인 설정, 통합 테스트는 롤백 없이 고유 ID), AuthProvider에 DEV |

## 데이터 변경

- 테이블 변경 없음. `AuthProvider`에 `DEV`를 추가한다. `provider`는 VARCHAR(20)이라 마이그레이션이 필요 없다.
- V1의 컬럼 주석 `'KAKAO, GOOGLE, APPLE'`은 적용된 마이그레이션이라 고치지 않는다. 대신 db-schema.md에 적는다.
- Redis 키 `refresh:{userId}:{tokenId}`, 값은 userId, TTL 14일 (db-schema 4장 그대로).

## API

| 메서드 | 경로 | 인증 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/v1/auth/login | 없음 | `{provider, token}` | 200 LoginResponse | 400 INVALID_INPUT·UNSUPPORTED_PROVIDER, 401 INVALID_SOCIAL_TOKEN |
| POST | /api/v1/auth/refresh | 없음 | `{refreshToken}` | 200 TokenResponse | 400 INVALID_INPUT, 401 INVALID_REFRESH_TOKEN |
| POST | /api/v1/auth/logout | 없음 | `{refreshToken}` | 204 | 400 INVALID_INPUT |
| GET | /api/v1/users/me | Bearer | - | 200 MeResponse | 401 UNAUTHORIZED·TOKEN_EXPIRED, 404 NOT_FOUND |

```json
// LoginResponse (TokenResponse는 status 없이 나머지 4개)
{
  "accessToken": "eyJ...",
  "accessTokenExpiresAt": "2026-11-04T01:30:00Z",
  "refreshToken": "42.Xq3...(43자)",
  "refreshTokenExpiresAt": "2026-11-18T01:00:00Z",
  "status": "ONBOARDING"
}
// MeResponse
{ "id": 42, "nickname": "회원482913", "status": "ONBOARDING", "createdAt": "2026-11-04T01:00:00Z", "providers": ["DEV"] }
```

- 공개 경로: `/api/v1/auth/**`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error`. 테스트에서만 `/test-support/**`를 추가한다.
- 그 밖의 경로는 모두 인증이 필요하다. 그래서 **인증 없이 없는 경로를 부르면 404가 아니라 401**이다 (경로 존재 여부를 숨긴다).

---

## 1절. 판단이 들어간 코드 (꼭 봐 주세요)

### 1-1. JwtProvider: access 발급과 검증 (알고리즘 고정, 만료/무효 구분)

**Kotlin**
```kotlin
package com.fitness.user.security

/** access 검증 결과. 필터가 예외 없이 분기하도록 값으로 돌려준다 */
sealed interface AccessTokenResult {
    data class Valid(val userId: Long) : AccessTokenResult
    data object Expired : AccessTokenResult
    data object Invalid : AccessTokenResult
}

data class IssuedToken(val value: String, val expiresAt: Instant)

@Component
class JwtProvider(
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    // 시작할 때 키를 검증한다: 비었거나 짧으면 앱이 뜨지 않는다 (운영에서 JWT_SECRET 누락을 바로 알게)
    private val key: SecretKey = run {
        val bytes = try {
            Decoders.BASE64.decode(properties.secret)
        } catch (e: RuntimeException) {
            throw IllegalStateException("fitness.jwt.secret은 Base64 값이어야 합니다")
        }
        check(bytes.size >= MIN_KEY_BYTES) { "fitness.jwt.secret은 디코드 후 32바이트 이상이어야 합니다 (현재 ${bytes.size})" }
        Keys.hmacShaKeyFor(bytes)
    }

    private val parser: JwtParser = Jwts.parser()
        .verifyWith(key)
        .clockSkewSeconds(CLOCK_SKEW_SECONDS)
        .clock { Date.from(clock.instant()) }
        .build()

    fun issueAccessToken(userId: Long): IssuedToken {
        val now = clock.instant()
        val expiresAt = now.plus(properties.accessTtl)
        val token = Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key, Jwts.SIG.HS256)
            .compact()
        return IssuedToken(token, expiresAt)
    }

    fun parse(token: String): AccessTokenResult = try {
        val jws = parser.parseSignedClaims(token)
        val userId = jws.payload.subject?.toLongOrNull()
        when {
            // 헤더의 alg를 믿지 않는다: 같은 키로 HS512 서명한 토큰도 거절
            jws.header.algorithm != Jwts.SIG.HS256.id -> AccessTokenResult.Invalid
            userId == null -> AccessTokenResult.Invalid
            else -> AccessTokenResult.Valid(userId)
        }
    } catch (e: ExpiredJwtException) {
        AccessTokenResult.Expired
    } catch (e: JwtException) {          // 서명 불일치, 형식 오류, alg=none(서명 없는 토큰) 등
        AccessTokenResult.Invalid
    } catch (e: IllegalArgumentException) { // 빈 문자열
        AccessTokenResult.Invalid
    }

    companion object {
        private const val MIN_KEY_BYTES = 32
        private const val CLOCK_SKEW_SECONDS = 5L
    }
}
```

**Java**
```java
public sealed interface AccessTokenResult permits Valid, Expired, Invalid {}
public record Valid(long userId) implements AccessTokenResult {}
public final class Expired implements AccessTokenResult { public static final Expired INSTANCE = new Expired(); }
public final class Invalid implements AccessTokenResult { public static final Invalid INSTANCE = new Invalid(); }

@Component
public class JwtProvider {
    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;
    private final JwtParser parser;

    public JwtProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(properties.getSecret());
        } catch (RuntimeException e) {
            throw new IllegalStateException("fitness.jwt.secret은 Base64 값이어야 합니다");
        }
        if (bytes.length < 32) throw new IllegalStateException("... 32바이트 이상 ...");
        this.key = Keys.hmacShaKeyFor(bytes);
        this.parser = Jwts.parser().verifyWith(key).clockSkewSeconds(5)
                .clock(() -> Date.from(clock.instant())).build();
    }

    public AccessTokenResult parse(String token) {
        try {
            Jws<Claims> jws = parser.parseSignedClaims(token);
            Long userId = parseLongOrNull(jws.getPayload().getSubject());
            if (!Jwts.SIG.HS256.getId().equals(jws.getHeader().getAlgorithm())) return Invalid.INSTANCE;
            if (userId == null) return Invalid.INSTANCE;
            return new Valid(userId);
        } catch (ExpiredJwtException e) {
            return Expired.INSTANCE;
        } catch (JwtException | IllegalArgumentException e) {
            return Invalid.INSTANCE;
        }
    }
    // issueAccessToken은 빌더 호출이 Kotlin과 같아 생략
}
```

**Kotlin 문법 설명**
- `sealed interface`: 구현 클래스를 이 파일 안으로 제한한다. 그래서 `when`이 모든 경우를 다뤘는지 컴파일러가 확인한다. Java 17의 `sealed ... permits`와 같다.
- `data object Expired`: 상태 없는 싱글턴. Java에서 `INSTANCE` 상수를 둔 final 클래스에 해당한다. `data`가 붙으면 `toString()`이 `Expired`로 찍힌다.
- `private val key = run { ... }`: 블록을 실행하고 마지막 식을 값으로 쓴다. 생성자 안에서 필드를 계산하는 코드에 해당한다. 필드 초기화는 생성자 실행 중에 선언 순서대로 일어난다.
- `check(조건) { 메시지 }`: 조건이 거짓이면 `IllegalStateException`을 던진다. 비슷한 `require`는 `IllegalArgumentException`을 던진다.
- `val x = try { ... } catch (...) { ... }`: Kotlin에서는 try도 식이라 값을 돌려준다.
- `?.toLongOrNull()`: subject가 null이거나 숫자가 아니면 null이 된다. Java에서는 try로 `Long.parseLong`을 감싸야 하는 코드다.
- `.clock { Date.from(...) }`: 마지막 인자로 넘기는 람다다. jjwt의 `Clock` 인터페이스(SAM)로 변환된다. Java의 `() -> ...`에 해당한다.
- `companion object { const val }`: Java의 `private static final` 상수.

**판단 포인트**
- **알고리즘 고정:** `verifyWith(key)`만 쓰면 jjwt는 키 길이가 충분할 때 헤더의 HS384/HS512도 받아 준다. 그래서 헤더 alg를 HS256과 직접 비교한다. 서명이 없는 토큰(`alg: none`)은 `parseSignedClaims`가 거절한다.
  - 확인 테스트: `HS256이 아닌 알고리즘으로 서명한 토큰은 무효로 판정한다`, `서명 없는 토큰으로 내 정보를 조회하면 401 UNAUTHORIZED로 응답한다`.
- **만료/무효 구분:** 명세 결정 4에 따라 앱은 `TOKEN_EXPIRED`일 때만 갱신을 시도한다.
  - 만료는 서명이 맞는 토큰에만 판정된다. jjwt는 서명을 검증한 뒤에 만료를 본다. 그래서 위조 토큰이 `Expired`로 판정되어 갱신을 유도하는 일은 없다.
  - 확인 테스트: `만료된 access로 내 정보를 조회하면 401 TOKEN_EXPIRED로 응답한다`.
- **claim은 sub·iat·exp만:** JWT는 서명만 되어 있고 암호화되지 않는다. 누구나 디코드해서 내용을 볼 수 있으므로 개인정보를 넣지 않는다.
  - 확인 테스트: `발급한 access에는 sub·iat·exp 외의 claim이 없다`.
- **시작할 때 키 검증:** 운영에서 `JWT_SECRET`을 빠뜨리면 첫 요청이 아니라 배포 시점에 실패하게 한다.
  - 확인 테스트: `서명 키가 32바이트보다 짧으면 생성할 때 실패한다`.
- **`Clock` 주입:** 만료 테스트를 위해서다. 30분을 기다리지 않고, 과거 시각에 고정한 Clock으로 토큰을 만든다.
- **시계 오차 허용 5초:** 서버 여러 대 사이의 작은 시각 차이만 흡수한다.

### 1-2. JwtAuthenticationFilter + EntryPoint: 필터는 던지지 않고, 401은 EntryPoint가 쓴다

**Kotlin**
```kotlin
// JwtAuthenticationFilter.kt
/** 빈으로 등록하지 않는다 (@Component를 붙이면 서블릿 필터로도 자동 등록돼 요청마다 두 번 돈다) */
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val token = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX.length)

        if (token != null) {
            when (val result = jwtProvider.parse(token)) {
                is AccessTokenResult.Valid -> SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken.authenticated(result.userId, null, emptyList())
                AccessTokenResult.Expired -> request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED)
                AccessTokenResult.Invalid -> request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.UNAUTHORIZED)
            }
        }
        chain.doFilter(request, response)
    }

    companion object {
        const val AUTH_ERROR_ATTRIBUTE = "fitness.authError"
        private const val BEARER_PREFIX = "Bearer "
    }
}

// ProblemDetailAuthenticationEntryPoint.kt
@Component
class ProblemDetailAuthenticationEntryPoint(
    private val writer: ProblemDetailWriter,
) : AuthenticationEntryPoint {

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        val code = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE) as? ErrorCode
            ?: ErrorCode.UNAUTHORIZED
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        writer.write(response, problemDetail(code, code.title, request.requestURI))
    }
}
```

**Java**
```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTH_ERROR_ATTRIBUTE = "fitness.authError";
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;

        if (token != null) {
            AccessTokenResult result = jwtProvider.parse(token);
            switch (result) {
                case Valid v -> SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(v.userId(), null, List.of()));
                case Expired e -> request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);
                case Invalid i -> request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.UNAUTHORIZED);
            }
        }
        chain.doFilter(request, response);
    }
}

// EntryPoint.commence
Object attr = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
ErrorCode code = (attr instanceof ErrorCode c) ? c : ErrorCode.UNAUTHORIZED;
```

**Kotlin 문법 설명**
- `?.takeIf { 조건 }?.substring(...)`: 값이 조건을 만족하면 그대로 쓰고, 아니면 null로 바꾼다. 중간에 null이 되면 이후 호출은 건너뛴다. Java의 삼항 연산자와 null 검사를 체인으로 쓴 것이다.
- `when (val result = ...) { is Valid -> ...; Expired -> ... }`: 값을 받는 동시에 타입별로 분기한다. `is Valid` 분기 안에서는 `result`가 자동으로 `Valid`로 캐스트된다(스마트 캐스트). Java 21의 패턴 매칭 switch와 같다.
- `as? ErrorCode ?: ErrorCode.UNAUTHORIZED`: 캐스트에 실패하면 null이 되고, null이면 기본값을 쓴다.
- Kotlin에는 checked exception이 없어서 `throws ServletException, IOException`을 쓰지 않는다.

**판단 포인트**
- **필터는 던지지 않는다:** 공개 경로(`/auth/refresh`)에 만료된 access 헤더가 같이 와도 요청이 막히지 않아야 한다. 막을지는 인가 규칙(`authenticated()`)이 정하고, 막힌 경우에만 EntryPoint가 응답을 쓴다.
  - 필터에서 예외를 던지면 `@RestControllerAdvice`까지 가지 못한다. 필터는 DispatcherServlet 앞에서 돌기 때문이다.
- **401 이유 전달은 요청 속성으로 한다:** EntryPoint가 받는 `AuthenticationException`은 "인증 없음"만 알려 준다. 만료인지 무효인지는 필터가 남긴 값으로 구분한다.
- **DB·Redis 조회 없음:** 명세의 성능 규칙대로 서명과 만료만 본다. 회원 존재 확인은 필요한 API(`/me`)에서 한다.
- **필터를 빈으로 만들지 않는다:** Spring Boot는 `Filter` 타입 빈을 서블릿 필터로 자동 등록한다. `@Component`를 붙이면 Security 체인 안과 밖에서 두 번 실행된다. 그래서 SecurityConfig 안에서 `new`로 만든다.
- **`WWW-Authenticate: Bearer`:** 401 응답에 이 헤더를 붙이는 것이 HTTP 표준(RFC 6750)이다.
- 확인 테스트:
  - `토큰 없이 내 정보를 조회하면 401 UNAUTHORIZED ProblemDetail로 응답한다`
  - `다른 키로 서명한 access로 내 정보를 조회하면 401 UNAUTHORIZED로 응답한다`
  - `공개 경로는 만료된 access 헤더가 있어도 처리된다`

### 1-3. SecurityConfig: 세션 없음, 공개 경로, 401·403 처리기

**Kotlin**
```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val entryPoint: ProblemDetailAuthenticationEntryPoint,
    private val accessDeniedHandler: ProblemDetailAccessDeniedHandler,
    private val publicPaths: PublicPathProperties,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }            // 쿠키 세션이 없어 CSRF 대상이 아니다
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }          // 우리 로그아웃은 /api/v1/auth/logout
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(*PUBLIC_PATHS).permitAll()
                if (publicPaths.extra.isNotEmpty()) {
                    it.requestMatchers(*publicPaths.extra.toTypedArray()).permitAll()
                }
                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(entryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    companion object {
        private val PUBLIC_PATHS = arrayOf(
            "/api/v1/auth/**",
            "/actuator/health",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error",
        )
    }
}

/** 테스트 전용 컨트롤러(/test-support/**)처럼 환경별로 추가로 공개할 경로. 운영은 비어 있다 */
@ConfigurationProperties("fitness.security")
data class PublicPathProperties(
    val extraPublicPaths: List<String> = emptyList(),
) {
    val extra: List<String> get() = extraPublicPaths
}
```

**Java**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .httpBasic(b -> b.disable())
        .formLogin(f -> f.disable())
        .logout(l -> l.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> {
            auth.requestMatchers(PUBLIC_PATHS).permitAll();
            if (!publicPaths.getExtra().isEmpty()) {
                auth.requestMatchers(publicPaths.getExtra().toArray(String[]::new)).permitAll();
            }
            auth.anyRequest().authenticated();
        })
        .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

**Kotlin 문법 설명**
- `{ it.disable() }`: 파라미터가 하나인 람다는 이름을 생략하고 `it`으로 쓴다. Java의 `x -> x.disable()`.
- `*PUBLIC_PATHS`: 스프레드 연산자. 배열을 가변 인자(`String...`)로 펼쳐 넘긴다. Java는 배열을 그대로 넘기면 되지만, Kotlin은 `*`를 붙여야 한다.
- `.toTypedArray()`: `List<String>`을 `Array<String>`(Java의 `String[]`)으로 바꾼다.
- `val extra: List<String> get() = ...`: 필드 없이 getter만 있는 프로퍼티. Java의 `getExtra()` 메서드.

**판단 포인트**
- **허용 목록 방식:** 공개 경로만 나열하고 나머지는 전부 인증을 요구한다. 새 API를 만들 때 인증을 빠뜨려도 기본이 "막힘"이다.
- **`/error` 공개:** 서블릿 오류 처리가 `/error`로 넘어갈 때 원래 응답이 401로 덮이지 않게 한다.
- **`/test-support/**`는 설정값으로 테스트에서만 연다:** 운영 코드에 테스트 경로를 하드코딩하지 않는다.
- **403:** 1-A에는 권한 구분이 없어 403이 날 경로가 없다. 그래도 형식은 맞춰 둔다. 확인 테스트는 처리기 단위 테스트다.
- **401이 404보다 먼저 난다:** 기존 `없는 경로를 호출하면 404 NOT_FOUND로 응답한다`(`/api/v1/nothing`)는 이제 401이 된다. 그 테스트는 인증된 요청으로 바꾸고, 인증 없는 경우는 새 테스트로 확인한다.
- `FitnessApplication`에서 `UserDetailsServiceAutoConfiguration`을 제외한다. 그러지 않으면 쓰지 않는 기본 계정과 비밀번호가 시작 로그에 찍힌다.

### 1-4. refresh 교체: 값 형식과 원자적 삭제

**Kotlin**
```kotlin
// user/domain/RefreshToken.kt
/** refresh 값 "{userId}.{tokenId}". tokenId는 32바이트 랜덤(Base64url 43자)이라 추측할 수 없다 */
class RefreshToken private constructor(
    val userId: Long,
    val tokenId: String,
) {
    val value: String get() = "$userId.$tokenId"

    override fun toString() = "RefreshToken(userId=$userId, tokenId=***)"   // 로그에 값이 남지 않게

    companion object {
        private val random = SecureRandom()
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val TOKEN_ID = Regex("^[A-Za-z0-9_-]{43}$")

        fun issue(userId: Long): RefreshToken {
            val bytes = ByteArray(32).also { random.nextBytes(it) }
            return RefreshToken(userId, encoder.encodeToString(bytes))
        }

        /** 형식이 틀리면 null. Redis를 조회하기 전에 걸러낸다 */
        fun parse(value: String): RefreshToken? {
            val parts = value.split(".")
            if (parts.size != 2) return null
            val userId = parts[0].toLongOrNull() ?: return null
            if (!TOKEN_ID.matches(parts[1])) return null
            return RefreshToken(userId, parts[1])
        }
    }
}

// user/repository/RefreshTokenRepository.kt
@Repository
class RefreshTokenRepository(
    private val redis: StringRedisTemplate,
) {
    fun save(token: RefreshToken, ttl: Duration) {
        redis.opsForValue().set(key(token), token.userId.toString(), ttl)
    }

    /** 있으면 지우고 true. 같은 토큰으로 동시에 불러도 true는 한 번뿐이다 (Redis DEL은 원자적) */
    fun consume(token: RefreshToken): Boolean = redis.delete(key(token))

    private fun key(token: RefreshToken) = "refresh:${token.userId}:${token.tokenId}"
}

// user/service/AuthService.kt (일부)
fun refresh(rawToken: String): TokenResponse {
    val token = RefreshToken.parse(rawToken) ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
    if (!refreshTokenRepository.consume(token)) {
        throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)   // 만료·로그아웃·이미 교체됨
    }
    return issueTokens(token.userId)
}

fun logout(rawToken: String) {
    val token = RefreshToken.parse(rawToken) ?: return   // 형식이 틀려도 204 (없는 토큰과 같게)
    refreshTokenRepository.consume(token)
}
```

**Java**
```java
public final class RefreshToken {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern TOKEN_ID = Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private final long userId;
    private final String tokenId;

    private RefreshToken(long userId, String tokenId) { ... }

    public String getValue() { return userId + "." + tokenId; }

    public static RefreshToken issue(long userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new RefreshToken(userId, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public static RefreshToken parse(String value) {   // 실패 시 null
        String[] parts = value.split("\\.");
        if (parts.length != 2) return null;
        Long userId;
        try { userId = Long.parseLong(parts[0]); } catch (NumberFormatException e) { return null; }
        if (!TOKEN_ID.matcher(parts[1]).matches()) return null;
        return new RefreshToken(userId, parts[1]);
    }
}

// RefreshTokenRepository.consume
public boolean consume(RefreshToken token) {
    return Boolean.TRUE.equals(redis.delete(key(token)));
}
```

**Kotlin 문법 설명**
- `class X private constructor(...)`: 생성자를 막고 `issue`/`parse`로만 만들게 한다. Java의 private 생성자 + static 팩터리.
- `"$userId.$tokenId"`, `"${token.userId}"`: 문자열 템플릿. 변수 하나면 `$x`, 식이면 `${...}`.
- `ByteArray(32).also { random.nextBytes(it) }`: 객체를 만들고 그 객체로 작업 하나를 한 뒤 객체 자신을 돌려준다. `apply`와 비슷하지만 `this` 대신 `it`으로 받는다.
- `?: return null`: null이면 함수를 바로 끝낸다. Kotlin에서는 `return`도 식이라 엘비스 오른쪽에 쓸 수 있다.
- `value.split(".")`: Kotlin의 `split`은 정규식이 아니라 문자열로 자른다. Java의 `split`은 정규식이라 `"\\."`로 써야 한다.
- 이 클래스는 일부러 `data class`로 만들지 않았다. data class는 `toString()`을 자동으로 만드는데, 그러면 tokenId가 그대로 드러난다.

**판단 포인트**
- **GET 후 DEL이 아니라 DEL 하나:** GET → 확인 → DEL로 나누면, 두 요청이 모두 GET에 성공한 뒤 둘 다 새 토큰을 받는 틈이 생긴다. DEL은 지운 개수를 돌려주고 Redis는 명령을 하나씩 실행한다. 그래서 1을 받는 요청은 하나뿐이다. 값 자체는 쓸 일이 없어서 GETDEL이나 Lua 스크립트도 필요 없다.
  - 확인 테스트: `같은 refresh로 동시에 갱신하면 하나만 성공한다`.
- **교체 순서 (옛 토큰 삭제 → 새 토큰 저장):** 새 토큰 저장이 실패하면 사용자는 다시 로그인해야 한다. 대신 옛 토큰이 살아남아 두 번 쓰이는 일은 없다. 보안 쪽을 택했다.
- **재사용 감지 시 그 요청만 401 (명세 결정 1):** 사용자별 refresh 목록을 관리하지 않는다.
- **값에 userId를 넣는 이유:** 키 `refresh:{userId}:{tokenId}`를 만들려면 userId가 필요하다. 탈퇴할 때 `refresh:{userId}:*`를 한꺼번에 지울 수도 있다. userId를 바꿔 보내면 키가 달라져 없는 토큰이 된다.
- **로그아웃은 항상 204:** 이미 없거나 형식이 틀려도 같은 결과를 준다. 같은 요청을 다시 보내도 결과가 같고(멱등), 어떤 토큰이 유효한지도 알려 주지 않는다.
- **로그아웃 후에도 access는 최대 30분 유효하다:** 명세대로다. 앱이 기기에서 지운다.

### 1-5. 첫 로그인 동시 요청: 가입 트랜잭션 분리 + 유니크 위반 시 재조회

**Kotlin**
```kotlin
// user/service/UserAccountService.kt
@Service
class UserAccountService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
) {
    @Transactional(readOnly = true)
    fun find(identity: SocialIdentity): AccountInfo? {
        val account = socialAccountRepository.findByProviderAndProviderUserId(identity.provider, identity.providerUserId)
            ?: return null
        val user = userRepository.findByIdOrNull(account.userId)
            ?: error("social_account(${account.id})의 회원이 없습니다")   // FK가 있어 생기면 안 되는 상황
        return AccountInfo(user.id!!, user.status)
    }

    /** users와 social_account를 한 트랜잭션으로 만든다. 유니크 위반이면 둘 다 롤백된다 */
    @Transactional
    fun register(identity: SocialIdentity): AccountInfo {
        val user = userRepository.save(User(nickname = defaultNickname()))
        socialAccountRepository.saveAndFlush(
            SocialAccount(userId = user.id!!, provider = identity.provider, providerUserId = identity.providerUserId, email = identity.email),
        )
        return AccountInfo(user.id!!, user.status)
    }

    private fun defaultNickname() = "회원" + Random.nextInt(100_000, 1_000_000)
}

// user/service/AuthService.kt (일부): 트랜잭션 없음
fun login(request: LoginRequest): LoginResponse {
    val identity = socialTokenVerifiers.verify(request.provider, request.token)
    val account = userAccountService.find(identity) ?: registerOrFind(identity)
    return LoginResponse.of(issueTokens(account.userId), account.status)
}

// 같은 소셜 계정의 첫 로그인이 동시에 오면 한쪽은 유니크 위반으로 롤백된다. 그쪽은 먼저 만들어진 회원을 다시 읽는다
private fun registerOrFind(identity: SocialIdentity): AccountInfo = try {
    userAccountService.register(identity)
} catch (e: DataIntegrityViolationException) {
    userAccountService.find(identity) ?: throw e   // 유니크가 아닌 다른 제약 위반이면 그대로 500
}
```

**Java**
```java
// AuthService
public LoginResponse login(LoginRequest request) {
    SocialIdentity identity = socialTokenVerifiers.verify(request.getProvider(), request.getToken());
    AccountInfo account = userAccountService.find(identity);
    if (account == null) account = registerOrFind(identity);
    return LoginResponse.of(issueTokens(account.userId()), account.status());
}

private AccountInfo registerOrFind(SocialIdentity identity) {
    try {
        return userAccountService.register(identity);
    } catch (DataIntegrityViolationException e) {
        AccountInfo found = userAccountService.find(identity);
        if (found == null) throw e;
        return found;
    }
}

// UserAccountService.find
SocialAccount account = socialAccountRepository.findByProviderAndProviderUserId(...);
if (account == null) return null;
User user = userRepository.findById(account.getUserId())
        .orElseThrow(() -> new IllegalStateException("..."));
```

**Kotlin 문법 설명**
- `findByIdOrNull(id)`: Spring Data의 Kotlin 확장 함수. `Optional` 대신 nullable을 돌려준다. Java의 `findById(id).orElse(null)`.
- `error("...")`: `IllegalStateException`을 던지는 표준 함수.
- `user.id!!`: "여기서 null일 수 없다"고 단언한다. null이면 NPE가 난다. 저장 후의 IDENTITY id처럼 확실한 곳에만 쓴다.
- `100_000`: 숫자 사이 밑줄은 읽기용이다. Java 7+와 같다.
- `Random.nextInt(a, b)`: `kotlin.random.Random`. a 이상 b 미만.

**판단 포인트**
- **`AuthService.login`에 `@Transactional`을 걸지 않는다.**
  - 바깥에 트랜잭션이 있으면 `register`가 그 트랜잭션에 합류한다. 유니크 위반이 나면 바깥 트랜잭션이 rollback-only로 표시되고, catch 뒤 재조회를 해도 커밋할 때 `UnexpectedRollbackException`이 난다.
  - MySQL REPEATABLE READ에서는 같은 트랜잭션 안의 재조회가 처음 읽은 시점을 기준으로 보여서, 상대가 커밋한 행이 안 보일 수도 있다.
  - 그래서 `find`와 `register`를 각자 트랜잭션으로 돌린다. Redis 저장도 DB 트랜잭션 밖이라 묶을 이유가 없다.
- **`saveAndFlush`:** 유니크 위반이 커밋 시점이 아니라 `register` 안에서 바로 드러나게 한다. 그래야 롤백이 확실하다.
- **롤백되면 users 행도 사라진다:** 가입 경쟁에서 진 요청이 빈 회원을 남기지 않는다.
  - 확인 테스트: `같은 소셜 계정으로 동시에 처음 로그인해도 회원 행은 하나만 늘어난다`, `같은 소셜 계정으로 동시에 처음 로그인하면 모두 같은 회원으로 토큰을 받는다`.
- **기본 닉네임:** `회원` + 6자리 숫자. 겹쳐도 된다 (닉네임에 유니크 제약이 없다). 명세 결정 2.

### 1-6. 소셜 신원 확인기 선택과 개발용 확인기 켜기

**Kotlin**
```kotlin
// user/client/SocialTokenVerifier.kt
interface SocialTokenVerifier {
    val provider: AuthProvider
    /** 토큰이 유효하면 신원을, 아니면 INVALID_SOCIAL_TOKEN BusinessException */
    fun verify(token: String): SocialIdentity
}

data class SocialIdentity(val provider: AuthProvider, val providerUserId: String, val email: String?)

// user/client/SocialTokenVerifiers.kt
@Component
class SocialTokenVerifiers(verifiers: ObjectProvider<SocialTokenVerifier>) {

    // 1-A 운영에는 확인기가 하나도 없다. List 주입은 빈이 0개면 시작에 실패하므로 ObjectProvider로 받는다
    private val byProvider: Map<AuthProvider, SocialTokenVerifier> =
        verifiers.orderedStream().toList().associateBy { it.provider }

    fun verify(provider: AuthProvider, token: String): SocialIdentity {
        val verifier = byProvider[provider]
            ?: throw BusinessException(ErrorCode.UNSUPPORTED_PROVIDER, "지원하지 않는 로그인 방식입니다: $provider")
        return verifier.verify(token)
    }
}

// user/client/DevTokenVerifier.kt
/**
 * 개발·테스트용: token 값을 그대로 소셜 사용자 ID로 쓴다.
 * 설정 fitness.auth.dev-login.enabled=true일 때만 빈이 생긴다 (application-local.yml, application-test.yml에만 있음).
 */
@Component
@ConditionalOnProperty("fitness.auth.dev-login.enabled", havingValue = "true")
class DevTokenVerifier : SocialTokenVerifier {
    override val provider = AuthProvider.DEV

    override fun verify(token: String): SocialIdentity {
        if (token.isBlank() || token.length > MAX_ID_LENGTH) throw BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN)
        return SocialIdentity(AuthProvider.DEV, token, email = null)
    }

    companion object {
        private const val MAX_ID_LENGTH = 100   // social_account.provider_user_id VARCHAR(100)
    }
}
```

**Java**
```java
@Component
public class SocialTokenVerifiers {
    private final Map<AuthProvider, SocialTokenVerifier> byProvider;

    public SocialTokenVerifiers(ObjectProvider<SocialTokenVerifier> verifiers) {
        this.byProvider = verifiers.orderedStream()
                .collect(Collectors.toMap(SocialTokenVerifier::getProvider, Function.identity()));
    }

    public SocialIdentity verify(AuthProvider provider, String token) {
        SocialTokenVerifier verifier = byProvider.get(provider);
        if (verifier == null) throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER, "지원하지 않는 로그인 방식입니다: " + provider);
        return verifier.verify(token);
    }
}

@Component
@ConditionalOnProperty(name = "fitness.auth.dev-login.enabled", havingValue = "true")
public class DevTokenVerifier implements SocialTokenVerifier {
    @Override public AuthProvider getProvider() { return AuthProvider.DEV; }
    ...
}
```

**Kotlin 문법 설명**
- 인터페이스의 `val provider: AuthProvider`: 구현체가 반드시 제공해야 하는 프로퍼티. Java의 `AuthProvider getProvider();` 추상 메서드.
- `override val provider = AuthProvider.DEV`: 그 프로퍼티를 값으로 구현한다. 타입은 추론된다.
- `associateBy { it.provider }`: 리스트를 "키 → 원소" Map으로 만든다. Java의 `Collectors.toMap(..., identity())`.
- `email = null`: 이름 붙인 인자. 순서와 상관없이 파라미터 이름으로 넘긴다.

**판단 포인트**
- **프로파일 대신 설정값으로 켠다 (명세 표현과 다른 점):**
  - `application.yml`의 기본 활성 프로파일이 `local`이다. 그래서 `@Profile("local")`이면, 운영에서 프로파일 지정을 빠뜨렸을 때 개발 로그인이 열린다. 아무 토큰으로나 로그인할 수 있게 된다.
  - 설정값은 jar에서 빠지는 `application-local.yml`과 테스트 설정에만 있다. 그래서 운영에서는 켤 방법이 환경변수를 일부러 넣는 것뿐이다.
  - 확인 테스트: `개발 로그인 설정이 없으면 DevTokenVerifier 빈이 생기지 않는다`.
- **확인기가 없는 provider는 400 `UNSUPPORTED_PROVIDER`:** 1-A에서는 KAKAO·GOOGLE·APPLE이 모두 여기에 해당한다. 명세의 "계획에서 정함" 항목은 `UNSUPPORTED_PROVIDER`로 정한다. enum에 없는 문자열은 Jackson 단계에서 400 `INVALID_INPUT`이 된다.
- **1-B:** `KakaoIdTokenVerifier`, `GoogleIdTokenVerifier`를 `SocialTokenVerifier`로 추가하기만 하면 된다. 로그인 흐름은 바뀌지 않는다.

---

## 2절. 전체 코드 (정형 코드, 훑어보기용)

> 1절에서 설명한 문법만 쓰는 파일은 Java 버전을 생략했다. 처음 나오는 문법만 아래에 설명한다.

### build.gradle.kts (추가분)
```kotlin
// Security & JWT
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
```

### FitnessApplication.kt
```kotlin
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@ConfigurationPropertiesScan
class FitnessApplication
```
- `exclude = [A::class]`: 어노테이션 안의 배열은 `[...]`로 쓴다. Java의 `exclude = {A.class}`.

### application.yml (추가분)
```yaml
fitness:
  jwt:
    # 운영은 환경변수 JWT_SECRET (Base64, 디코드 후 32바이트 이상. 예: openssl rand -base64 48).
    # 비어 있으면 앱이 시작하지 않는다. access 30분·refresh 14일은 JwtProperties 기본값.
    secret: ${JWT_SECRET:}
```

### application-test.yml (추가분)
```yaml
fitness:
  jwt:
    secret: <구현 시 openssl rand -base64 48로 만든 값>   # 테스트 전용 키. 운영·로컬과 무관하다
  auth:
    dev-login:
      enabled: true                    # 개발용 로그인(DEV) 켬
  security:
    extra-public-paths: /test-support/**   # 테스트 전용 컨트롤러는 인증 없이
```

### application-local.yml (미커밋, 제가 덧붙임)
- 기존 내용은 읽어서 출력하지 않는다. `fitness:` 키가 이미 있는지만 `grep -c`로 확인한다.
- 그다음 `fitness.jwt.secret`(`openssl rand -base64 48`로 만들어 파일에 바로 쓰고 화면에 찍지 않음)과 `fitness.auth.dev-login.enabled: true`를 끝에 덧붙인다.

### common/exception/ErrorCode.kt (추가분)
```kotlin
UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다"),
INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"),
INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "소셜 로그인 정보를 확인할 수 없습니다"),
UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식입니다"),
FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),

// fromStatus
status == 401 -> UNAUTHORIZED
status == 403 -> FORBIDDEN
```

### common/exception/ProblemDetails.kt
```kotlin
package com.fitness.common.exception

/** ProblemDetail + code (ADR 0003). 예외 핸들러와 보안 필터 단계(401·403)가 같은 형식을 쓰도록 한곳에서 만든다 */
fun problemDetail(code: ErrorCode, detail: String?, path: String): ProblemDetail =
    ProblemDetail.forStatusAndDetail(code.status, detail ?: code.title).apply {
        title = code.title
        instance = URI.create(path)
        setProperty("code", code.name)
    }
```
- 클래스 밖에 선언한 함수(최상위 함수). Java에서는 `ProblemDetailsKt.problemDetail(...)` static 메서드로 보인다. `GlobalExceptionHandler`의 private `problem()`은 이 함수 호출로 바꾼다.

### config/ClockConfig.kt
```kotlin
@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
```

### config/SwaggerConfig.kt (추가분)
```kotlin
.components(Components().addSecuritySchemes("bearer",
    SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
```

### user/domain/AuthProvider.kt
```kotlin
enum class AuthProvider {
    KAKAO,
    GOOGLE,
    APPLE,
    DEV, // 개발·테스트 전용 로그인. fitness.auth.dev-login.enabled일 때만 쓰인다
}
```

### user/security/JwtProperties.kt
```kotlin
@ConfigurationProperties("fitness.jwt")
data class JwtProperties(
    val secret: String,
    val accessTtl: Duration = Duration.ofMinutes(30),
    val refreshTtl: Duration = Duration.ofDays(14),
)
```
- Spring Boot 3은 생성자 파라미터로 설정값을 바인딩한다. 기본값이 있는 파라미터는 설정이 없을 때 그 값을 쓴다. Java에서는 `record JwtProperties(String secret, @DefaultValue("30m") Duration accessTtl, ...)`에 해당한다.

### user/security/ProblemDetailWriter.kt
```kotlin
@Component
class ProblemDetailWriter(private val objectMapper: ObjectMapper) {
    fun write(response: HttpServletResponse, problem: ProblemDetail) {
        response.status = problem.status
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.outputStream, problem)
    }
}
```
- Spring Boot의 ObjectMapper에는 ProblemDetail용 Jackson 설정(mixin)이 들어 있다. 그래서 `code`가 `properties` 안이 아니라 최상위 필드로 나온다. 필터 단계 응답도 핸들러 응답과 같은 모양이 된다.

### user/security/ProblemDetailAccessDeniedHandler.kt
```kotlin
@Component
class ProblemDetailAccessDeniedHandler(private val writer: ProblemDetailWriter) : AccessDeniedHandler {
    override fun handle(request: HttpServletRequest, response: HttpServletResponse, e: AccessDeniedException) {
        writer.write(response, problemDetail(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.title, request.requestURI))
    }
}
```

### user/repository/SocialAccountRepository.kt (추가분)
```kotlin
fun findAllByUserId(userId: Long): List<SocialAccount>
```

### user/service/AuthService.kt (1절에 없는 나머지)
```kotlin
@Service
class AuthService(
    private val socialTokenVerifiers: SocialTokenVerifiers,
    private val userAccountService: UserAccountService,
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) {
    // login, registerOrFind, refresh, logout: 1-4, 1-5 참고

    private fun issueTokens(userId: Long): TokenResponse {
        val access = jwtProvider.issueAccessToken(userId)
        val refresh = RefreshToken.issue(userId)
        refreshTokenRepository.save(refresh, jwtProperties.refreshTtl)
        return TokenResponse(
            accessToken = access.value,
            accessTokenExpiresAt = access.expiresAt,
            refreshToken = refresh.value,
            refreshTokenExpiresAt = clock.instant().plus(jwtProperties.refreshTtl),
        )
    }
}
```

### user/service/UserService.kt
```kotlin
@Service
class UserService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
) {
    @Transactional(readOnly = true)
    fun getMe(userId: Long): MeResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다")
        val providers = socialAccountRepository.findAllByUserId(userId).map { it.provider }
        return MeResponse.of(user, providers)
    }
}
```

### user/dto/*.kt
```kotlin
data class LoginRequest(
    val provider: AuthProvider,
    @field:NotBlank
    val token: String,
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String,
)

data class TokenResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
)

data class LoginResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val status: UserStatus,
) {
    companion object {
        fun of(tokens: TokenResponse, status: UserStatus) = LoginResponse(
            tokens.accessToken, tokens.accessTokenExpiresAt, tokens.refreshToken, tokens.refreshTokenExpiresAt, status,
        )
    }
}

data class MeResponse(
    val id: Long,
    val nickname: String,
    val status: UserStatus,
    val createdAt: Instant,
    val providers: List<AuthProvider>,
) {
    companion object {
        fun of(user: User, providers: List<AuthProvider>) =
            MeResponse(user.id!!, user.nickname, user.status, user.createdAt!!, providers)
    }
}

/** 서비스 간 전달용 (응답 아님) */
data class AccountInfo(val userId: Long, val status: UserStatus)
```

### user/controller/AuthController.kt, UserController.kt
```kotlin
@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    @Operation(summary = "로그인·가입", description = "소셜 토큰으로 로그인한다. 처음이면 ONBOARDING 회원으로 가입한다")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse = authService.login(request)

    @Operation(summary = "토큰 갱신", description = "refresh로 새 access·refresh를 받는다. 쓴 refresh는 폐기된다")
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): TokenResponse = authService.refresh(request.refreshToken)

    @Operation(summary = "로그아웃", description = "refresh를 폐기한다. 이미 없어도 204")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: RefreshTokenRequest) = authService.logout(request.refreshToken)
}

@Tag(name = "회원")
@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    @Operation(summary = "내 정보 조회", security = [SecurityRequirement(name = "bearer")])
    @GetMapping("/me")
    fun me(@Parameter(hidden = true) @AuthenticationPrincipal userId: Long): MeResponse = userService.getMe(userId)
}
```
- `@AuthenticationPrincipal userId: Long`: 필터가 SecurityContext에 넣은 principal(userId)을 그대로 받는다. 서비스에는 `Long`만 넘기므로 Spring Security 타입이 서비스로 새지 않는다.

### 테스트 대표 코드 (나머지는 같은 형태)
```kotlin
@IntegrationTest
class UserControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val jwtProperties: JwtProperties,
) {
    @Test
    fun `만료된 access로 내 정보를 조회하면 401 TOKEN_EXPIRED로 응답한다`() {
        // given
        val pastClock = Clock.fixed(Instant.now().minus(Duration.ofMinutes(31)), ZoneOffset.UTC)
        val expired = JwtProvider(jwtProperties, pastClock).issueAccessToken(1L).value

        // when
        val result = mockMvc.get("/api/v1/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $expired")
        }

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("TOKEN_EXPIRED") }
        }
    }
}

// 동시성 테스트: 시작 신호(CountDownLatch)로 스레드를 한꺼번에 출발시킨다
@Test
fun `같은 refresh로 동시에 갱신하면 하나만 성공한다`() {
    // given
    val refresh = authService.login(LoginRequest(AuthProvider.DEV, uniqueId())).refreshToken
    val start = CountDownLatch(1)
    val pool = Executors.newFixedThreadPool(THREADS)
    val futures = (1..THREADS).map {
        pool.submit<Result<TokenResponse>> { start.await(); runCatching { authService.refresh(refresh) } }
    }

    // when
    start.countDown()
    val results = futures.map { it.get(10, TimeUnit.SECONDS) }

    // then
    assertThat(results.count { it.isSuccess }).isEqualTo(1)
}
```
- `runCatching { }`: 예외를 던지지 않고 `Result`(성공 값 또는 예외)로 감싼다. Java에서는 try/catch로 성공 여부를 직접 담아야 하는 코드다.
- `(1..THREADS).map { }`: 1부터 THREADS까지 각각 실행해 리스트로 만든다. Java의 `IntStream.rangeClosed(1, n).mapToObj(...)`.

---

## 정합성·동시성·멱등성

| 지점 | 깨질 수 있는 상황 | 막는 방법 | 확인 테스트 |
| --- | --- | --- | --- |
| refresh 교체 | 같은 refresh로 동시 갱신 → 둘 다 새 토큰 | Redis `DEL` 반환값 하나로 판정 (1-4) | 동시 갱신 하나만 성공 |
| 첫 로그인 | 같은 소셜 계정 동시 가입 → 회원 2명 | `social_account` 유니크 + 가입 트랜잭션 롤백 + 새 트랜잭션 재조회 (1-5) | 회원 행 1개, 모두 같은 회원 |
| 로그아웃 | 재시도 | 항상 204 (멱등) | 이미 로그아웃한 refresh 재로그아웃 204 |
| 서버 메모리 | 여러 대 | 로그인 상태는 refresh(Redis)뿐이고, access 검증은 서명만 본다 | - |

**통합 테스트의 데이터 정리:** 지금까지처럼 테스트마다 롤백(`@Transactional`)하지 않는다.
- 동시성 테스트는 여러 스레드가 실제로 커밋해야 하고, MockMvc 요청도 서비스 트랜잭션을 실제로 커밋해야 실제 동작과 같다.
- 대신 각 테스트가 고유한 소셜 ID(`UUID`)를 써서 서로 영향을 주지 않는다. DB는 테스트 컨텍스트가 뜰 때 Flyway clean으로 비워진다.
- Redis 키는 TTL로 사라진다.
- 005 계획의 "롤백 방식은 인증에서 정한다"를 여기서 정하고, CLAUDE.md에 적는다.

## 실패·대체 경로

- **Redis 장애:** 로그인·갱신은 500으로 실패한다 (refresh를 저장할 곳이 없다). access 검증은 Redis를 쓰지 않으므로 다른 API는 계속 동작한다. 대체 저장소는 두지 않는다.
- **새 refresh 저장 실패 (옛 토큰은 이미 삭제됨):** 사용자는 다시 로그인한다. 보안 쪽을 택했다 (1-4).
- **외부 OAuth 호출:** 1-A에는 없다. 1-B에서 타임아웃·JWKS 캐시와 함께 다룬다.

## 테스트 목록

**통합 `AuthControllerTest` (`@IntegrationTest`, MockMvc)**
- [ ] `처음 로그인하면 ONBOARDING 회원으로 가입하고 토큰을 발급한다` (status ONBOARDING, 토큰 4필드 존재)
- [ ] `같은 소셜 계정으로 다시 로그인하면 기존 회원으로 로그인한다` (두 응답 access의 sub가 같음)
- [ ] `확인기가 없는 provider로 로그인하면 400 UNSUPPORTED_PROVIDER로 응답한다` (KAKAO)
- [ ] `빈 토큰으로 로그인하면 400 INVALID_INPUT으로 응답한다`
- [ ] `refresh로 갱신하면 새 access와 다른 refresh를 발급한다`
- [ ] `갱신에 쓴 refresh로 다시 갱신하면 401 INVALID_REFRESH_TOKEN으로 응답한다`
- [ ] `형식이 틀린 refresh로 갱신하면 401 INVALID_REFRESH_TOKEN으로 응답한다`
- [ ] `로그아웃한 refresh로 갱신하면 401 INVALID_REFRESH_TOKEN으로 응답한다`
- [ ] `이미 로그아웃한 refresh로 다시 로그아웃해도 204로 응답한다`
- [ ] `공개 경로는 만료된 access 헤더가 있어도 처리된다` (만료 헤더 + /auth/refresh → 200)

**통합 `UserControllerTest`**
- [ ] `로그인해서 받은 access로 내 정보를 조회한다` (providers = [DEV])
- [ ] `토큰 없이 내 정보를 조회하면 401 UNAUTHORIZED ProblemDetail로 응답한다` (content-type problem+json, WWW-Authenticate)
- [ ] `다른 키로 서명한 access로 내 정보를 조회하면 401 UNAUTHORIZED로 응답한다`
- [ ] `만료된 access로 내 정보를 조회하면 401 TOKEN_EXPIRED로 응답한다`
- [ ] `서명 없는 토큰으로 내 정보를 조회하면 401 UNAUTHORIZED로 응답한다` (alg=none)
- [ ] `인증 없이 없는 경로를 호출하면 404가 아니라 401로 응답한다`

**통합 `AuthServiceConcurrencyTest`**
- [ ] `같은 refresh로 동시에 갱신하면 하나만 성공한다`
- [ ] `같은 소셜 계정으로 동시에 처음 로그인하면 모두 같은 회원으로 토큰을 받는다`
- [ ] `같은 소셜 계정으로 동시에 처음 로그인해도 회원 행은 하나만 늘어난다`

**통합 `RefreshTokenStorageTest`** (Redis 원본 값 직접 확인)
- [ ] `로그인하면 refresh가 14일 TTL로 Redis에 저장된다` (`getExpire`로 TTL 확인)

**단위 테스트**
- [ ] `JwtProviderTest`: `발급한 access에는 sub·iat·exp 외의 claim이 없다`
- [ ] `JwtProviderTest`: `서명 키가 32바이트보다 짧으면 생성할 때 실패한다`
- [ ] `JwtProviderTest`: `HS256이 아닌 알고리즘으로 서명한 토큰은 무효로 판정한다` (64바이트 키로 HS512 서명)
- [ ] `RefreshTokenTest`: `점이 없거나 tokenId 형식이 틀린 값은 파싱하지 않는다`
- [ ] `RefreshTokenTest`: `문자열로 바꿔도 tokenId가 드러나지 않는다`
- [ ] `SocialTokenVerifiersTest` (Mockk): `확인기가 없는 provider면 UNSUPPORTED_PROVIDER 예외를 던진다`
- [ ] `DevTokenVerifierConditionTest` (`ApplicationContextRunner`): `개발 로그인 설정이 없으면 DevTokenVerifier 빈이 생기지 않는다`
- [ ] `ProblemDetailAccessDeniedHandlerTest`: `권한이 없으면 403 FORBIDDEN ProblemDetail을 쓴다`

**기존 테스트 수정**
- [ ] `GlobalExceptionHandlerTest`의 `없는 경로를 호출하면 404 NOT_FOUND로 응답한다`: 유효한 access 헤더를 붙인다.

## 작업 순서

1. 승인되면 이 계획을 `docs/plans/006-auth-jwt.md`에 저장하고, ADR 0004를 작성한다.
2. 의존성을 추가하고 `application-test.yml`·`application-local.yml`을 설정한다. 그다음 시그니처만 있는 뼈대를 만든다 (컴파일용: ErrorCode, AuthProvider.DEV, DTO, 빈 서비스).
3. **위 테스트 목록을 작성하고, 구현이 없어서 실패하는 것을 확인한다.**
4. 구현한다. 순서: JwtProvider → 필터·SecurityConfig·처리기 → RefreshToken·저장소 → 확인기 → UserAccountService·AuthService → UserService → 컨트롤러 → Swagger. 그다음 테스트를 통과시킨다.
5. CLAUDE.md와 db-schema.md를 갱신한다.
6. `./gradlew test` 전체 통과 → `/review-change` → 커밋(main) → ai-log 기록.

## 검증

- `./gradlew test`: 기존 63개 + 새 테스트 통과. 로컬 MySQL과 Redis가 켜져 있어야 한다.
- `./gradlew bootRun` 후 curl로 확인한다. 끝나면 제가 띄운 프로세스만 종료한다.
  - `POST /api/v1/auth/login {"provider":"DEV","token":"me"}` → 토큰
  - `GET /api/v1/users/me`에 Bearer 헤더 → 200
  - 헤더 없이 → 401 `UNAUTHORIZED`
  - refresh 두 번 → 두 번째 401
- Swagger UI(`/swagger-ui/index.html`)에서 Authorize 버튼과 /me 호출을 확인한다.

## 확인 필요

1. **JWT 라이브러리 jjwt:** 1-B에서 카카오·구글 id_token을 JWKS로 검증할 때 jjwt를 계속 쓸 수도 있다. 다만 키 회전을 다루는 Nimbus(`spring-security-oauth2-jose`)가 더 편할 수 있다. 1-B를 계획할 때 정한다.
2. **개발 로그인을 프로파일이 아니라 설정값으로 켜기 (1-6):** 명세의 "local·test 프로파일에서만"과 표현이 다르다. 승인하시면 명세 문구도 이 방식으로 고친다.

## 변경 이력
- 2026-11-05: 승인. 개발 로그인을 프로파일 대신 설정값(`fitness.auth.dev-login.enabled`)으로 켜는 안을 확정하고 명세 문구를 맞췄다.
- 2026-11-05: 테스트 작성 중 추가. `DevTokenVerifierConditionTest`에 반대 경우(`개발 로그인 설정을 켜면 DevTokenVerifier 빈이 생긴다`)를 더해, 조건 테스트가 늘 통과하는 빈 테스트가 아님을 확인한다. 요청·응답 DTO는 `user/dto/AuthDtos.kt` 한 파일과 `MeResponse.kt`로 묶었다. 기존 404 테스트 이름은 `인증된 요청으로 없는 경로를 호출하면 404 NOT_FOUND로 응답한다`로 바꿨다.
- 2026-11-05: 구현 중 변경. `PublicPathProperties`는 `extra` getter 없이 `extraPublicPaths`를 바로 쓴다. 필터 단계 응답(401·403)은 Content-Type에 charset을 붙이지 않는다 (예외 핸들러 응답과 같은 `application/problem+json`, 테스트로 확인). KDoc의 `/test-support/**`가 Kotlin 중첩 주석으로 읽혀 컴파일이 깨져 주석 문구를 바꿨다.
- 2026-11-05: 리뷰(/review-change) 지적 8건 중 중간 1건만 고쳤다. 운영 기준 설정(application.yml)만 읽어 개발 로그인 빈이 생기지 않는지 확인하는 테스트를 추가했다 (application.yml에 설정을 넣으면 실패하는 것을 확인). 낮음 7건(동시 가입 ERROR 로그와 소셜 ID 노출, 명세 예외 상황 테스트 보강, exp 없는 토큰 허용, provider_user_id 대소문자 무시 콜레이션, /swagger-ui.html 401, ASYNC 재디스패치 인증, 단일 시나리오 규칙)은 사용자 결정으로 고치지 않았다. provider_user_id 콜레이션은 1-B 전에 다시 본다.
