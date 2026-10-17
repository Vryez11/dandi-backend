-- V0.37__create_coins.sql

-- 코인 잔액을 profiles에서 분리한다. 사용자당 한 행이다.
--
-- profiles.coin은 여기서 지우지 않는다. blue/green 전환 중에는 옛 버전이 아직 profiles.coin을 매핑하고
-- 있어(ddl-auto: validate) 같은 배포에서 지우면 옛 버전이 뜨지 못한다.
-- 코드가 coins를 읽도록 바뀐 배포 뒤에 별도 마이그레이션으로 지운다.
CREATE TABLE coins
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL UNIQUE,
    balance    INT       NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_coins_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 코인 변동 내역. 추가만 하고 수정·삭제하지 않으므로 updated_at / deleted_at은 두지 않는다.
--
-- quantity는 부호가 있다(+ 지급, - 차감). balance_after는 변동 직후 잔액이다.
-- source_id는 변동을 일으킨 행의 id이고, 어느 테이블의 행인지는 category가 정한다
-- (MAIL_CLAIM → mails.id, ROOM_ITEM_PURCHASE → owned_room_items.id).
-- 여러 테이블을 가리키므로 FK는 걸지 않는다.
CREATE TABLE coin_histories
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    category      VARCHAR(30) NOT NULL,
    quantity      INT         NOT NULL,
    balance_after INT         NOT NULL,
    source_id     BIGINT      NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_coin_histories_user_id_id (user_id, id),
    CONSTRAINT fk_coin_histories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 기존 사용자의 잔액을 옮긴다. 지금까지 코드가 profiles.coin을 바꾼 적이 없어 내역 없이 잔액만 옮긴다.
INSERT INTO coins (user_id, balance)
SELECT user_id, coin
FROM profiles;
