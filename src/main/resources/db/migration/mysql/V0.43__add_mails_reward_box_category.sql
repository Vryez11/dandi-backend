-- V0.43__add_mails_reward_box_category.sql

-- 보상 상자 우편(REWARD_BOX)의 등급. 다른 우편 종류는 NULL이다.
-- 등급을 뽑는 규칙(설정)이 바뀌어도 이미 보낸 상자의 등급은 그대로여야 하므로,
-- coin_quantity로 계산하지 않고 지급할 때의 값을 저장한다.
--
-- NULL 허용 컬럼 추가라 구버전은 이 컬럼을 모른 채 그대로 동작한다.
ALTER TABLE mails
    ADD COLUMN reward_box_category VARCHAR(20) NULL;
