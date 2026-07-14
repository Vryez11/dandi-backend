-- 1. 새로운 독립 테이블 생성 (ICON)
CREATE TABLE icon
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    image_url VARCHAR(512) NOT NULL
);

-- 2. REFRESH_TOKEN 테이블 생성
CREATE TABLE refresh_token
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    refresh_token VARCHAR(512) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    TIMESTAMP    NOT NULL,
    user_id       BIGINT       NOT NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 3. PROFILE 테이블 생성 (자체 id를 PK로 설정, user_id는 Unique FK 설정)
CREATE TABLE profile
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT    NOT NULL UNIQUE,
    nickname      VARCHAR(100),
    birth         DATETIME,
    gender        TINYINT,
    height        INT,
    weight        INT,
    updated_at    TIMESTAMP          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    coin          INT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 4. USERS 테이블 정리 (profile 테이블로 분리된 기존 칼럼들 삭제)
ALTER TABLE users
    DROP COLUMN age,
    DROP COLUMN gender,
    DROP COLUMN height,
    DROP COLUMN weight,
    DROP COLUMN last_login_at,
    DROP COLUMN coin;

-- 5. CAT 테이블에 updated_at 칼럼 추가
ALTER TABLE cat
    ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 6. MEAL 테이블 칼럼 및 soft delete 관련 칼럼 추가
-- (피드백 반영: icon_id 외래키 및 관계 추가 없음)
ALTER TABLE meal
    ADD COLUMN meal_at    TIMESTAMP AFTER created_at,
    ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER meal_at,
    ADD COLUMN deleted_at TIMESTAMP AFTER updated_at,
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER status;