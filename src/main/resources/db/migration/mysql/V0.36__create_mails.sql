-- V0.36__create_mails.sql

-- 우편. 보상(코인·아이템·보상 상자)을 받는 우편함의 한 건이다.
--
-- 보상 내용은 category에 따라 다르다.
--   COIN       : coin_quantity만 채운다.
--   ITEM       : mail_items만 채운다.
--   REWARD_BOX : 둘 다 채울 수 있다.
--
-- 이미지 컬럼은 두지 않는다. 서버가 대표 이미지 URL을 만들어 내려준다.
-- COIN·REWARD_BOX는 category별 고정 URL이고, ITEM은 representative_item_id로 만든다.
-- 대표 아이템은 우편을 만들 때 정해 저장해서, 목록 조회가 mail_items를 읽지 않게 한다.
--
-- 받은 상태는 컬럼을 따로 두지 않고 claimed_at != NULL로 계산한다.
-- expires_at이 NULL이면 만료되지 않는 우편이다.
-- 삭제는 deleted_at 소프트 삭제이고 복구 API로 되돌린다.
--
-- 목록은 최신순 커서 조회(id < cursor)라 (user_id, id) 인덱스를 둔다.
CREATE TABLE mails
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT       NOT NULL,
    category               VARCHAR(20)  NOT NULL,
    title                  VARCHAR(100) NOT NULL,
    content                VARCHAR(255) NOT NULL,
    coin_quantity          INT          NULL,
    representative_item_id BIGINT       NULL,
    claimed_at             TIMESTAMP    NULL,
    expires_at             TIMESTAMP    NULL,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at             TIMESTAMP    NULL,
    INDEX idx_mails_user_id_id (user_id, id),
    CONSTRAINT fk_mails_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_mails_representative_item FOREIGN KEY (representative_item_id) REFERENCES items (id)
);

-- 우편에 든 아이템. 받으면 quantity만큼 보유 아이템이 생긴다.
-- 우편 내용은 만든 뒤 바뀌지 않으므로 updated_at / deleted_at은 두지 않는다.
CREATE TABLE mail_items
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_id  BIGINT NOT NULL,
    item_id  BIGINT NOT NULL,
    quantity INT    NOT NULL,
    CONSTRAINT fk_mail_items_mail FOREIGN KEY (mail_id) REFERENCES mails (id) ON DELETE CASCADE,
    CONSTRAINT fk_mail_items_item FOREIGN KEY (item_id) REFERENCES items (id)
);
