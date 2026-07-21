ALTER TABLE meal
    DROP COLUMN image_url,
    ADD COLUMN image_key VARCHAR(512),
    ADD COLUMN icon_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_meal_icon FOREIGN KEY (icon_id) REFERENCES icon (id);
