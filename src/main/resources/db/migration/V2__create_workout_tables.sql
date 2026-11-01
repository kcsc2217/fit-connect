-- workout 모듈: exercise, exercise_equipment, workout_session, workout_set, personal_record, weekly_volume
-- 기준: docs/db-schema.md 3.2. 외래 키는 같은 모듈 안에서만 건다 (user_id는 모듈 간 참조라 FK 없음).

CREATE TABLE exercise
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    code             VARCHAR(50)  NOT NULL COMMENT '예: BACK_SQUAT. LLM 출력과 평가에서 사용',
    name_ko          VARCHAR(50)  NOT NULL,
    name_en          VARCHAR(100) NOT NULL,
    body_part        VARCHAR(20)  NOT NULL COMMENT 'CHEST, BACK, SHOULDER, LEGS, ARMS, CORE',
    load_type        VARCHAR(20)  NOT NULL COMMENT 'BARBELL, DUMBBELL, MACHINE, BODYWEIGHT (과부하 증가폭)',
    min_level        VARCHAR(20)  NOT NULL COMMENT '권장 최소 경력',
    compound         BOOLEAN      NOT NULL COMMENT '다관절 운동 여부',
    default_rest_sec SMALLINT     NOT NULL,
    active           BOOLEAN      NOT NULL COMMENT '비활성 종목은 후보에서 제외',
    source_id        VARCHAR(100) NULL COMMENT 'free-exercise-db 원본 ID',
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercise_code (code),
    KEY idx_exercise_body_part_active (body_part, active)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE exercise_equipment
(
    exercise_id BIGINT      NOT NULL,
    equipment   VARCHAR(30) NOT NULL COMMENT '필요한 장비. 맨몸 종목은 행 없음',
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (exercise_id, equipment),
    CONSTRAINT fk_exercise_equipment_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE workout_session
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL COMMENT '모듈 간 참조, FK 없음',
    client_id          BINARY(16)   NOT NULL COMMENT '앱이 만든 UUID',
    routine_id         BIGINT       NULL COMMENT '어떤 루틴으로 한 운동인지, FK 없음',
    started_at         DATETIME(6)  NOT NULL,
    ended_at           DATETIME(6)  NULL COMMENT '종료 시 세션 종료 이벤트 발행',
    condition_note     VARCHAR(200) NULL COMMENT '컨디션 메모',
    deleted            BOOLEAN      NOT NULL COMMENT 'tombstone',
    client_updated_at  DATETIME(6)  NOT NULL COMMENT '충돌 판정 기준 (last-write-wins)',
    server_received_at DATETIME(6)  NOT NULL COMMENT '기기 시계 어긋남 확인용',
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_session_user_client (user_id, client_id),
    KEY idx_workout_session_user_started (user_id, started_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE workout_set
(
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL COMMENT '멱등성 범위·최근 기록 조회용으로 의도적으로 둠',
    client_id         BINARY(16)    NOT NULL COMMENT '앱이 만든 UUID',
    session_id        BIGINT        NOT NULL,
    exercise_id       BIGINT        NOT NULL,
    exercise_order    SMALLINT      NOT NULL COMMENT '세션 안 종목 순서',
    set_order         SMALLINT      NOT NULL COMMENT '종목 안 세트 순서',
    weight_kg         DECIMAL(6, 2) NOT NULL COMMENT '맨몸은 0',
    reps              SMALLINT      NOT NULL,
    rpe               DECIMAL(3, 1) NULL COMMENT '체감 강도',
    completed         BOOLEAN       NOT NULL,
    deleted           BOOLEAN       NOT NULL COMMENT 'tombstone',
    performed_at      DATETIME(6)   NOT NULL COMMENT '세트 수행 시각',
    client_updated_at DATETIME(6)   NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_set_user_client (user_id, client_id),
    KEY idx_workout_set_session (session_id),
    KEY idx_workout_set_user_exercise_performed (user_id, exercise_id, performed_at),
    CONSTRAINT fk_workout_set_session FOREIGN KEY (session_id) REFERENCES workout_session (id),
    CONSTRAINT fk_workout_set_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE personal_record
(
    user_id        BIGINT        NOT NULL,
    exercise_id    BIGINT        NOT NULL,
    best_weight_kg DECIMAL(6, 2) NOT NULL COMMENT '최고 무게',
    est_1rm        DECIMAL(6, 2) NOT NULL COMMENT '추정 1RM',
    source_set_id  BIGINT        NOT NULL COMMENT '기록을 만든 세트',
    achieved_at    DATETIME(6)   NOT NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (user_id, exercise_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE weekly_volume
(
    user_id    BIGINT         NOT NULL,
    week_start DATE           NOT NULL COMMENT '사용자 시간대 기준 주 시작일(월요일)',
    body_part  VARCHAR(20)    NOT NULL,
    volume_kg  DECIMAL(12, 2) NOT NULL COMMENT '무게 × 반복 수 합계',
    created_at DATETIME(6)    NOT NULL,
    updated_at DATETIME(6)    NOT NULL,
    PRIMARY KEY (user_id, week_start, body_part)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
