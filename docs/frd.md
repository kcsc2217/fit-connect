# 📑 [FRD] 기능 요구사항 정의서 (Functional Requirement Document)

| 문서 버전 | 작성일 | 작성자 | 주요 목적 |
| :--- | :--- | :--- | :--- |
| **v1.3** | 2026. 09. 13 | 백엔드 엔지니어 | 트레이너 셀프 세일즈, 소셜 로그인, 대용량 트래픽 동시성 제어, Kafka 비동기, LLM 연동 명세 |

---

## 1. 🔑 인증 & 유저/트레이너 셀프 세일즈 도메인 (Auth & User Admin)

### F-101. 카카오 / 구글 소셜 로그인 및 회원가입
* **기능 설명:** 카카오톡 및 구글 OAuth 2.0을 통해 인증을 수행하며, 최초 로그인 시 사용자 계정을 자동 생성한다.
* **주요 엔드포인트:** `POST /api/v1/auth/social-login`
* **입력 데이터 (Request DTO):**
  * `provider` (Enum: `KAKAO`, `GOOGLE`)
  * `oauthToken` (String, 소셜 SDK에서 전달받은 Access Token)
* **출력 데이터 (Response DTO):**
  * `accessToken` (JWT Access Token - 만료 1시간)
  * `refreshToken` (JWT Refresh Token - 만료 2주, Redis 저장)
  * `isNewUser` (Boolean, 최초 신규 가입 여부)
  * `user`: `{ userId, email, nickname, role }`
* **비즈니스 로직:**
  1. 전달받은 `oauthToken`으로 소셜 공급자 API를 호출해 유저 식별자(`providerId`), `email`, `nickname` 수집.
  2. `(provider, providerId)` 기준 DB 조회 후, 계정이 없으면 신규 가입 (기본 역할: `ROLE_USER`).
  3. JWT Access/Refresh Token 발급 및 Refresh Token은 Redis에 저장.

### F-102. 유저 역할 및 목표 설정 (역할 전환)
* **기능 설명:** 신규 가입 유저가 자신의 목적(일반 수강생 or 트레이너)을 선택하거나 프로필 목표를 설정한다.
* **주요 엔드포인트:** `PATCH /api/v1/users/me/profile`
* **입력 데이터:**
  * `role` (Enum: `ROLE_USER`, `ROLE_TRAINER`)
  * `targetGoal` (Enum: `DIET`, `MUSCLE`, `MAINTAIN`)

### F-103. 트레이너 셀프 세일즈 프로필 등록 및 수정 (퍼스널 브랜딩)
* **기능 설명:** `ROLE_TRAINER` 역할을 가진 유저가 자신을 세일즈하고 영업할 수 있는 포트폴리오 프로필을 등록한다.
* **주요 엔드포인트:** `POST /api/v1/trainers` / `PUT /api/v1/trainers/me`
* **입력 데이터:**
  * `name` (트레이너 활동명)
  * `specialty` (전문 분야 - 예: 바디프로필, 재활/체형교정, 다이어트, 대회준비)
  * `profileBio` (세일즈 홍보 멘트 및 이력 사항)
  * `instagramUrl` (인스타그램/유튜브 포트폴리오 링크)
  * `locationInfo` (수업 진행 장소 - 예: 강남/서초 일대 협의, 자택 방문, 제휴 짐)
* **출력 데이터:** `trainerId`, `name`, `specialty`, `profileBio`, `instagramUrl`, `locationInfo`, `createdAt`

### F-104. 선착순 PT 수강신청 클래스 생성
* **기능 설명:** 트레이너가 자신이 세일즈할 수강신청 클래스(오프라인 1:1 PT, 원데이 클래스, 온라인 코칭 등)를 개설하고 정원을 설정한다.
* **주요 엔드포인트:** `POST /api/v1/pt-classes`
* **입력 데이터:** `trainerId`, `title` (클래스명), `classType` (Enum: `OFFLINE_PT`, `ONEDAY_CLASS`, `ONLINE_COACHING`), `totalCapacity` (모집 정원), `openTime` (오픈 일시)
* **출력 데이터:** `classId`, `title`, `classType`, `totalCapacity`, `openTime`
* **비즈니스 로직 & 캐시 프리워밍 (Pre-warming):**
  * DB 저장 성공 시, 스파이크 트래픽 대비 **Redis 데이터 프리워밍**을 실행한다.
    * Redis Key 1: `class:{id}:capacity` ➔ `totalCapacity` (원자적 수량 차감용)
    * Redis Key 2: `class:{id}:info` ➔ 클래스 상세 JSON (조회 캐싱용, TTL 1시간)

---

## 2. ⚡ 대용량 트래픽 & 선착순 수강신청 도메인 (High-Concurrency Engine)

### F-201. 수강신청 오픈 클래스 상세 및 잔여 수량 조회 (High-Read Traffic)
* **기능 설명:** 정각 오픈 전 트레이너 프로필과 클래스 상세 정보를 확인하고 새로고침하는 조회를 DB 접근 없이 처리한다.
* **주요 엔드포인트:** `GET /api/v1/pt-classes/{classId}`
* **처리 흐름 (Redis Cache-Aside Pattern):**
  1. Redis에서 `class:{classId}:info` Key 조회.
  2. **Cache Hit:** Redis 인메모리 데이터를 즉시 응답 (DB 접근 0건).
  3. **Cache Miss:** DB에서 조회 후 Redis에 캐시 적재(TTL 1시간) 및 응답 반환.

### F-202. 선착순 PT 수강신청 요청 (Core Spike Traffic Engine)
* **기능 설명:** 인기 트레이너의 수강신청 정각 오픈 시 밀려드는 동시 요청을 DB 락 없이 원자적으로 수량 차감 및 검증한다.
* **주요 엔드포인트:** `POST /api/v1/pt-classes/{classId}/reservations`
* **입력 데이터:** Header의 JWT Authentication (`userId`)
* **핵심 백엔드 로직 (Redis Lua Script 연산):**
  ```lua
  -- Redis Lua Script 원자적 실행 로직
  -- 1. 잔여 수량 Key (class:{classId}:capacity) 검증 (0 이하 시 'SLOT_EXHAUSTED' 즉시 반환 -> Fail-Fast)
  -- 2. 유저 중복 신청 Key (class:{classId}:user:{userId}) 검증 (존재 시 'DUPLICATE' 즉시 반환)
  -- 3. 조건 통과 시 수량 차감(DECR) 및 중복 신청 Key 등록(SETNX)