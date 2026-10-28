-- V0.49__add_profiles_tutorial_completed_at.sql

-- 튜토리얼을 마친 시각. NULL이면 아직 튜토리얼을 하지 않은 사용자다.
--
-- 기존 사용자는 가입할 때 기본 아이템을 모두 받았으므로 튜토리얼을 마친 것으로 보고 가입 시각으로 채운다.
-- profiles에는 생성 시각이 없어 users.created_at을 쓴다.
-- 이 배포부터 튜토리얼 API 배포 전까지 가입한 사용자도 기본 아이템을 모두 받지만 값은 NULL로 남는다.
-- 이들은 튜토리얼 API를 배포할 때 NULL인 행만 다시 채운다.
--
-- nullable 컬럼 추가라 구버전은 이 컬럼을 모른 채 그대로 동작하고, 롤백해도 남은 컬럼이 기동을 막지 않는다.
-- ALGORITHM = INSTANT를 명시해 두면 INSTANT가 불가능할 때 테이블을 복사하지 않고 실패한다.
ALTER TABLE profiles
    ADD COLUMN tutorial_completed_at TIMESTAMP NULL,
    ALGORITHM = INSTANT;

UPDATE profiles p
    JOIN users u ON u.id = p.user_id
SET p.tutorial_completed_at = u.created_at;
