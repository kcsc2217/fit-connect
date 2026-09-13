# 📊 [ERD] 데이터베이스 개체 관계 도면 (Entity Relationship Diagram)

> **문서 버전:** v1.3  
> **최종 수정일:** 2026. 09. 13  
> **관련 문서:** `docs/frd.md`

---

## 1. 🖼️ Mermaid ERD 다이어그램

```mermaid
erDiagram
    USERS ||--o| TRAINER : "1:1 관계 (ROLE_TRAINER 전환 시)"
    USERS ||--o{ RESERVATION : "1:N 관계 (수강신청)"
    USERS ||--o{ DIET_LOG : "1:N 관계 (식단 기록)"
    TRAINER ||--o{ PT_CLASS : "1:N 관계 (수업 개설)"
    PT_CLASS ||--o{ RESERVATION : "1:N 관계 (신청 내역)"

    USERS {
        bigint id PK "회원 고유 ID"
        string email "이메일"
        string nickname "닉네임"
        string provider "소셜 공급자 (KAKAO, GOOGLE)"
        string provider_id "소셜 고유 ID (UK)"
        string role "권한 (ROLE_USER, ROLE_TRAINER)"
        string target_goal "운동 목표 (DIET, MUSCLE, MAINTAIN)"
        datetime created_at "가입 일시"
    }

    TRAINER {
        bigint id PK "트레이너 고유 ID"
        bigint user_id FK "회원 ID (FK)"
        string name "활동명"
        string specialty "전문 분야 (바디프로필, 재활 등)"
        text profile_bio "세일즈 홍보 멘트 및 이력"
        string instagram_url "인스타그램/유튜브 링크"
        string location_info "수업 가능 지역/장소"
        datetime created_at "등록 일시"
    }

    PT_CLASS {
        bigint id PK "클래스 고유 ID"
        bigint trainer_id FK "트레이너 ID (FK)"
        string title "클래스명"
        string class_type "수업 형태 (OFFLINE_PT, ONEDAY_CLASS 등)"
        int total_capacity "모집 정원"
        datetime open_time "선착순 오픈 일시"
        datetime created_at "개설 일시"
    }

    RESERVATION {
        bigint id PK "예약 고유 ID"
        bigint pt_class_id FK "클래스 ID (FK)"
        bigint user_id FK "회원 ID (FK)"
        string status "예약 상태 (PENDING, SUCCESS, CANCELLED)"
        datetime created_at "신청 일시"
    }

    DIET_LOG {
        bigint id PK "식단 기록 ID"
        bigint user_id FK "회원 ID (FK)"
        string meal_type "식사 종류 (BREAKFAST, LUNCH, DINNER, SNACK)"
        text content "음식 기록 텍스트"
        text ai_feedback "LLM 분석 결과 및 코칭"
        datetime created_at "작성 일시"
    }