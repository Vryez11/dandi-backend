-- V0.45__create_streaks.sql

-- 식사 기록의 연속 일수. 사용자당 한 행이다.
--
-- 행은 가입 때가 아니라 첫 식사 기록 때 만들고, 행이 없으면 연속 일수를 0으로 본다.
-- 기존 사용자의 행은 채우지 않는다. 지금까지 홈의 연속 일수는 모두 0이었으므로 0부터 시작해도 줄어드는 사용자가 없다.
--
-- last_meal_at은 마지막으로 식사를 기록한(생성한) 서버 시각이다. 날짜가 아니라 시각으로 두고 읽을 때 날짜로 바꾼다.
-- 날짜를 정하는 시간대가 바뀌어도 저장된 값을 고치지 않아도 된다.
-- cat.last_meal_at과 이름이 같지만 그쪽은 분석이 끝났을 때 갱신한다.
--
-- user_id의 유니크 인덱스가 fk_streaks_user의 인덱스를 겸하므로 FK 인덱스는 따로 생기지 않는다.
--
-- 테이블 추가라 구버전은 이 테이블을 모른 채 그대로 동작하고, 롤백해도 남은 테이블이 기동을 막지 않는다.
CREATE TABLE streaks
(
    id           BIGINT    AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT    NOT NULL,
    streak_days  INT       NOT NULL,
    last_meal_at TIMESTAMP NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_streaks_user_id UNIQUE (user_id),
    CONSTRAINT fk_streaks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
