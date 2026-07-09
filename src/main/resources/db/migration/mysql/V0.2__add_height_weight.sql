ALTER TABLE users
    ADD COLUMN height INT AFTER gender,
    ADD COLUMN weight INT AFTER height;

ALTER TABLE cat RENAME COLUMN body TO weight;


