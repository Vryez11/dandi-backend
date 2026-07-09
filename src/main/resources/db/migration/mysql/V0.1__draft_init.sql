-- 1. 유저 테이블 (users)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    age INT,
    gender TINYINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    coin INT NOT NULL DEFAULT 0
);

-- 2. 고양이 캐릭터 테이블 (cat)
CREATE TABLE cat (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     user_id BIGINT NOT NULL UNIQUE,
     name VARCHAR(100) NOT NULL,
     love INT NOT NULL DEFAULT 0,
     exp INT NOT NULL DEFAULT 0,
     body TINYINT NOT NULL DEFAULT 0,
     CONSTRAINT fk_cat_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 3. 식사 기록 테이블 (history)
CREATE TABLE history (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     user_id BIGINT NOT NULL,
     name VARCHAR(100) NOT NULL,
     image_url VARCHAR(512),
     carbs INT,
     protein INT,
     fat INT,
     score INT,
     calory INT,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);