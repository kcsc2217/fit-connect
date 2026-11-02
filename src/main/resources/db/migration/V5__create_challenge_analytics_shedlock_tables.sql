-- challenge 모듈: challenge, challenge_participation / analytics 모듈: event_log / 공통: shedlock
-- 기준: docs/db-schema.md 3.6~3.7. 외래 키는 같은 모듈 안에서만 건다 (user_id는 모듈 간 참조라 FK 없음).

CREATE TABLE challenge
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    title             VARCHAR(100) NOT NULL,
    starts_on         DATE         NOT NULL,
    ends_on           DATE         NOT NULL,
    capacity          INT          NOT NULL COMMENT '정원',
    waitlist_capacity INT          NOT NULL COMMENT '대기 정원 (정원의 50%)',
    applied_count     INT          NOT NULL COMMENT '현재 신청 수 (DB 락 실험용)',
    open_at           DATETIME(6)  NOT NULL COMMENT '일반 신청 오픈 시각',
    status            VARCHAR(20)  NOT NULL COMMENT 'SCHEDULED, OPEN, IN_PROGRESS, FINISHED',
    version           INT          NOT NULL COMMENT '낙관적 락 실험용',
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE challenge_participation
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    challenge_id    BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL COMMENT '모듈 간 참조, FK 없음',
    status          VARCHAR(20) NOT NULL COMMENT 'HOLD, APPLIED, WAITING, CANCELED, EXPIRED',
    wait_seq        INT         NULL COMMENT '대기 순번',
    hold_expires_at DATETIME(6) NULL COMMENT '임시 확보 만료 시각 (10분, 승급자는 30분)',
    applied_at      DATETIME(6) NULL COMMENT '확정 시각',
    version         INT         NOT NULL COMMENT '낙관적 락',
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_challenge_participation_challenge_user (challenge_id, user_id),
    KEY idx_challenge_participation_waiting (challenge_id, status, wait_seq),
    KEY idx_challenge_participation_hold_expires (status, hold_expires_at),
    CONSTRAINT fk_challenge_participation_challenge FOREIGN KEY (challenge_id) REFERENCES challenge (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE event_log
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NULL COMMENT '로그인 전 이벤트는 NULL',
    name        VARCHAR(50) NOT NULL COMMENT 'APP_OPEN, PUSH_OPEN, ROUTINE_VIEW, SESSION_COMPLETE 등',
    properties  JSON        NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_event_log_name_occurred (name, occurred_at),
    KEY idx_event_log_user_occurred (user_id, occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ShedLock(JdbcTemplateLockProvider) 기본 형식. 라이브러리가 직접 읽고 써서 엔티티가 없다
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
