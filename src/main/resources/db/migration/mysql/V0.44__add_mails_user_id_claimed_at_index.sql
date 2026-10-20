-- V0.44__add_mails_user_id_claimed_at_index.sql

-- 안 받은 우편만 찾는 조회(홈 우편함 배지의 존재 확인, 모두 받기의 잠금 조회)가
-- (user_id, id) 인덱스로 사용자의 우편 전체를 읽고 있어 (user_id, claimed_at) 복합 인덱스를 추가한다.
-- 다 받은 사용자는 결과가 없어 끝까지 읽게 되는데, 우편은 식사마다 쌓이므로 그 범위가 계속 늘어난다.
-- deleted_at은 넣지 않는다. 받은 우편만 삭제할 수 있어 안 받은 행에는 삭제된 행이 없다.
-- expires_at도 넣지 않는다. 만료된 채 안 받은 행만 범위에 남고, 그 수는 받지 않고 넘긴 상자 수만큼이다.
-- idx_mails_user_id_id는 목록 커서 조회와 fk_mails_user가 쓰므로 그대로 둔다.
--
-- 인덱스 추가라 구버전은 이 인덱스를 모른 채 그대로 동작한다.
ALTER TABLE mails
    ADD INDEX idx_mails_user_id_claimed_at (user_id, claimed_at),
    ALGORITHM = INPLACE, LOCK = NONE;
