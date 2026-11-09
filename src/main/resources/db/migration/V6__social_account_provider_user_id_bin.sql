-- 소셜 사용자 식별자(id_token sub)는 대소문자와 끝 공백까지 구분한다 (계획 007, 006 리뷰 지적 5).
-- utf8mb4_bin은 PAD SPACE라 끝 공백을 무시하므로 NO PAD인 utf8mb4_0900_bin을 쓴다.
-- 유니크 제약 uk_social_account_provider_user (provider, provider_user_id)는 그대로이고 비교 규칙만 바뀐다.
ALTER TABLE social_account
    MODIFY provider_user_id VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL
        COMMENT '소셜 서비스의 사용자 식별자 (id_token sub, 대소문자·끝 공백 구분)';
