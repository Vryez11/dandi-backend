-- V0.33__make_meals_icon_id_nullable.sql

-- 분석이 끝나기 전(WAITING·ANALYZING)이거나 실패(FAILED)한 식사는 아이콘을 NULL로 두기 위해 NULL을 허용한다.
-- 엔티티는 다음 배포에서 Long?으로 바꾼다. 그 전까지 코드는 항상 값을 쓰므로 이 변경만으로는 동작이 바뀌지 않는다.
-- 기존 미완료 식사의 icon_id(기본값 1)는 여기서 NULL로 바꾸지 않는다. 지금 엔티티는 primitive long이라
-- NULL 행을 읽으면 실패하므로, 엔티티를 바꾼 코드가 배포된 뒤 별도 마이그레이션으로 정리한다.
-- FK(fk_meal_icon)는 NULL을 검사하지 않으므로 그대로 유지한다.
ALTER TABLE meals
    MODIFY COLUMN icon_id BIGINT NULL;
