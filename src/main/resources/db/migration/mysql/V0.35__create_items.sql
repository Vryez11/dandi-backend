-- V0.35__create_items.sql

-- 아이템(상품 정의). 지금은 마이룸 아이템뿐이지만 이후 고양이 치장 같은 아이템도 이 테이블에 둔다.
-- 우편 보상·상점 구매·코인 내역이 어떤 아이템이든 이 id 하나로 가리키게 하고,
-- 도메인 고유 속성은 확장 테이블(room_items, 이후 cat_items)에 둔다.
--
-- 이미지 컬럼은 두지 않는다. 이미지 URL은 서버가 CDN 고정 경로에 id를 이어 붙여 만든다.
-- price가 NULL이면 상점에서 팔지 않는 아이템(보상 전용)이다.
CREATE TABLE items
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    price      INT          NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 마이룸 아이템 전용 속성. items와 1:1이므로 item_id가 PK이자 FK다.
CREATE TABLE room_items
(
    item_id  BIGINT      NOT NULL PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    CONSTRAINT fk_room_items_item FOREIGN KEY (item_id) REFERENCES items (id)
);

-- 보유 아이템. 같은 아이템을 여러 개 사면 한 개마다 한 행이며, 좌표·장착 여부를 따로 가진다.
-- id가 API의 roomItemId다. 상품을 가리키는 컬럼은 roomItemId와 헷갈리지 않도록 item_id로 둔다.
-- FK를 items가 아니라 room_items에 걸어 마이룸 아이템이 아닌 아이템은 넣을 수 없게 한다.
--
-- 삭제는 deleted_at 소프트 삭제이고 복구 API로 되돌린다. 복구 기한은 없다.
--
-- 마이룸 정렬(장착 먼저 → 최신순)과 커서 조회를 위해 (user_id, is_equipped, id) 인덱스를 둔다.
-- 카테고리는 room_items와 조인해 거른다. 사용자당 보유 수가 작아 category를 여기에 중복해 두지 않는다.
CREATE TABLE owned_room_items
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT    NOT NULL,
    item_id     BIGINT    NOT NULL,
    is_equipped BOOLEAN   NOT NULL DEFAULT FALSE,
    x           FLOAT     NOT NULL DEFAULT 0,
    y           FLOAT     NOT NULL DEFAULT 0,
    z_index     INT       NOT NULL DEFAULT 0,
    scale       FLOAT     NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP NULL,
    INDEX idx_owned_room_items_user_id_is_equipped_id (user_id, is_equipped, id),
    CONSTRAINT fk_owned_room_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_owned_room_items_room_item FOREIGN KEY (item_id) REFERENCES room_items (item_id)
);
