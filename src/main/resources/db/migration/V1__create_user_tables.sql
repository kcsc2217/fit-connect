-- user 모듈: users, social_account, user_profile, user_equipment, device_token
-- 기준: docs/db-schema.md 3.1. 시각은 UTC DATETIME(6), 상태값은 VARCHAR + 애플리케이션 enum.
-- 외래 키는 같은 모듈 안에서만 건다.

CREATE TABLE users
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    status       VARCHAR(20) NOT NULL COMMENT 'ONBOARDING, ACTIVE, WITHDRAWN',
    nickname     VARCHAR(30) NOT NULL,
    withdrawn_at DATETIME(6) NULL COMMENT '탈퇴 시각',
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE social_account
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL COMMENT 'KAKAO, GOOGLE, APPLE',
    provider_user_id VARCHAR(100) NOT NULL COMMENT '소셜 서비스의 사용자 식별자',
    email            VARCHAR(255) NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_social_account_provider_user (provider, provider_user_id),
    KEY idx_social_account_user (user_id),
    CONSTRAINT fk_social_account_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_profile
(
    user_id         BIGINT       NOT NULL,
    height_cm       DECIMAL(5, 1) NOT NULL,
    weight_kg       DECIMAL(5, 1) NOT NULL COMMENT '최신 체중',
    birth_year      SMALLINT     NOT NULL,
    sex             VARCHAR(10)  NOT NULL COMMENT 'MALE, FEMALE',
    goal            VARCHAR(20)  NOT NULL COMMENT 'CUT, BULK, MAINTAIN',
    experience      VARCHAR(20)  NOT NULL COMMENT 'BEGINNER, INTERMEDIATE, ADVANCED',
    split_type      VARCHAR(20)  NOT NULL COMMENT 'FULL_BODY, TWO_SPLIT, THREE_SPLIT',
    workout_days    TINYINT      NOT NULL COMMENT '요일 비트마스크 (월=1 ... 일=64)',
    alarm_time      TIME         NOT NULL COMMENT '사용자 시간대 기준 알림 시각',
    timezone        VARCHAR(40)  NOT NULL,
    session_minutes SMALLINT     NOT NULL COMMENT '선호 운동 시간',
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id),
    KEY idx_user_profile_alarm_time (alarm_time),
    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_equipment
(
    user_id    BIGINT      NOT NULL,
    equipment  VARCHAR(30) NOT NULL COMMENT 'BARBELL, DUMBBELL, MACHINE, CABLE, BENCH, RACK, DIP_BAR, PULLUP_BAR, KETTLEBELL, BODYWEIGHT',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, equipment),
    CONSTRAINT fk_user_equipment_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE device_token
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    token        VARCHAR(255) NOT NULL COMMENT 'FCM 토큰',
    platform     VARCHAR(10)  NOT NULL COMMENT 'ANDROID, IOS',
    last_seen_at DATETIME(6)  NOT NULL COMMENT '앱 실행 시 갱신',
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_token_token (token),
    KEY idx_device_token_user (user_id),
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
