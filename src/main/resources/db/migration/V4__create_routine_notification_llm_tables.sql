-- routine 모듈: daily_routine, routine_item / notification 모듈: push_log / llm 모듈: llm_call_log
-- 기준: docs/db-schema.md 3.3~3.5. 외래 키는 같은 모듈 안에서만 건다 (user_id, exercise_id, ref_id는 모듈 간 참조라 FK 없음).

CREATE TABLE daily_routine
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL COMMENT '모듈 간 참조, FK 없음',
    routine_date      DATE         NOT NULL COMMENT '사용자 시간대 기준 날짜',
    status            VARCHAR(20)  NOT NULL COMMENT 'PENDING, GENERATED, FALLBACK, FAILED, SKIPPED',
    source            VARCHAR(20)  NOT NULL COMMENT 'LLM, FALLBACK, REGENERATED',
    split_day         VARCHAR(20)  NOT NULL COMMENT '오늘 분할 부위 (예: LOWER)',
    coach_note        VARCHAR(300) NULL COMMENT 'LLM 코칭 문구',
    estimated_minutes SMALLINT     NULL,
    prompt_version    VARCHAR(30)  NULL COMMENT '예: routine-v3',
    regen_count       TINYINT      NOT NULL COMMENT '"다른 루틴" 요청 횟수 (하루 2회 제한)',
    feedback          VARCHAR(10)  NULL COMMENT 'EASY, OK, HARD (운동 후 난이도)',
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_routine_user_date (user_id, routine_date),
    KEY idx_daily_routine_date_status (routine_date, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE routine_item
(
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    routine_id              BIGINT        NOT NULL,
    exercise_id             BIGINT        NOT NULL COMMENT 'workout 모듈 소유, FK 없음',
    item_order              SMALLINT      NOT NULL,
    sets                    SMALLINT      NOT NULL COMMENT '서버 규칙 계산값',
    reps                    SMALLINT      NOT NULL COMMENT '서버 규칙 계산값',
    target_weight_kg        DECIMAL(6, 2) NOT NULL COMMENT '서버 규칙 계산값',
    rest_sec                SMALLINT      NOT NULL,
    alternative_exercise_id BIGINT        NULL COMMENT '장비가 없을 때 대체 종목, FK 없음',
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_routine_item_routine_order (routine_id, item_order),
    CONSTRAINT fk_routine_item_routine FOREIGN KEY (routine_id) REFERENCES daily_routine (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE push_log
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL COMMENT '모듈 간 참조, FK 없음',
    type         VARCHAR(30) NOT NULL COMMENT 'DAILY_ROUTINE, CHALLENGE_PROMOTED',
    ref_id       BIGINT      NOT NULL COMMENT 'type별 대상 id (루틴·챌린지). NULL이면 유니크가 중복을 못 막으므로 NOT NULL',
    scheduled_at DATETIME(6) NOT NULL COMMENT '발송 예정 시각',
    sent_at      DATETIME(6) NULL,
    result       VARCHAR(20) NOT NULL COMMENT 'PENDING, SENT, FAILED, TOKEN_EXPIRED',
    opened_at    DATETIME(6) NULL COMMENT '알림을 눌러 앱을 연 시각 (열람률 지표)',
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_push_log_user_type_ref (user_id, type, ref_id),
    KEY idx_push_log_result_scheduled (result, scheduled_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE llm_call_log
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    feature           VARCHAR(30)  NOT NULL COMMENT 'ROUTINE (DIET, COACH_CHAT은 2차)',
    user_id           BIGINT       NULL COMMENT '평가 실행은 NULL',
    ref_id            BIGINT       NULL COMMENT '결과가 저장된 대상 (daily_routine.id 등)',
    prompt_version    VARCHAR(30)  NOT NULL,
    model             VARCHAR(60)  NOT NULL,
    input_tokens      INT          NOT NULL,
    output_tokens     INT          NOT NULL,
    latency_ms        INT          NOT NULL,
    retry_count       TINYINT      NOT NULL,
    validation_result VARCHAR(20)  NOT NULL COMMENT 'PASS, SCHEMA_ERROR, RULE_VIOLATION, TIMEOUT',
    failure_reason    VARCHAR(300) NULL COMMENT '검증 실패 상세 (300자 초과분은 엔티티가 자름)',
    fallback_used     BOOLEAN      NOT NULL COMMENT '기본 루틴으로 대체됐는지',
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_llm_call_log_feature_created (feature, created_at),
    KEY idx_llm_call_log_prompt_created (prompt_version, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
