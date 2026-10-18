-- V0.42__delete_profiles_coin.sql

-- 잔액을 coins로 옮긴 뒤 남은 profiles.coin을 지운다.
-- 코드는 이전 배포에서 이 컬럼의 매핑을 지웠고, 이 배포의 구버전도 그 버전이라 blue/green 전환 중에도 안전하다.
--
-- ALGORITHM = INSTANT를 명시해 두면 INSTANT가 불가능할 때 테이블을 복사하지 않고 실패한다.
ALTER TABLE profiles
    DROP COLUMN coin,
    ALGORITHM = INSTANT;
