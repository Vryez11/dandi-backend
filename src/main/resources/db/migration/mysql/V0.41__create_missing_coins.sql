-- V0.41__create_missing_coins.sql

-- coins 행이 없는 사용자에게 행을 만든다.
-- V0.37은 그때 있던 사용자만 옮겼고, 가입 시 coins 행을 만드는 코드는 그 뒤 배포에서 들어갔다.
-- 그 사이에 가입한 사용자와, 그 배포 중 구버전으로 가입한 사용자가 대상이다.
--
-- 이 마이그레이션이 도는 배포의 구버전은 가입 시 coins 행을 만드는 버전이라 다시 빠지는 사용자는 없다.
-- 잔액은 V0.37처럼 profiles.coin에서 옮긴다. V0.42가 이 컬럼을 지우므로 그보다 먼저 실행돼야 한다.
INSERT INTO coins (user_id, balance)
SELECT p.user_id, p.coin
FROM profiles p
WHERE NOT EXISTS (SELECT 1 FROM coins c WHERE c.user_id = p.user_id);
