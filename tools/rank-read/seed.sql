-- Run with the mysql batch client without --force in an isolated rank_lab_* schema.
-- The schema must have exactly one registered USER and no study sessions.
SET @users = CASE DATABASE()
    WHEN 'rank_lab_1000' THEN 1000
    WHEN 'rank_lab_10000' THEN 10000
    WHEN 'rank_lab_50000' THEN 50000
END;
SET @day = CURRENT_DATE();
SET @guard = IF(@users IS NOT NULL AND CURRENT_TIME() >= '03:00:00'
    AND (SELECT COUNT(*) FROM `user`) = 1
    AND (SELECT COUNT(*) FROM `user` WHERE role = 'USER' AND deleted_at IS NULL) = 1
    AND NOT EXISTS (SELECT 1 FROM study_session),
    'SELECT ''Fixture preconditions met''', 'STOP_INVALID_RANK_LAB_FIXTURE_PRECONDITIONS');
PREPARE fixture_guard FROM @guard;
EXECUTE fixture_guard;
DEALLOCATE PREPARE fixture_guard;

CREATE TEMPORARY TABLE rank_numbers (n INT PRIMARY KEY);
INSERT INTO rank_numbers (n)
WITH RECURSIVE digits AS (
    SELECT 0 AS d UNION ALL SELECT d + 1 FROM digits WHERE d < 9
)
SELECT a.d + 10*b.d + 100*c.d + 1000*d.d + 10000*e.d + 1
FROM digits a CROSS JOIN digits b CROSS JOIN digits c CROSS JOIN digits d CROSS JOIN digits e
WHERE a.d + 10*b.d + 100*c.d + 1000*d.d + 10000*e.d < @users;

START TRANSACTION;
INSERT INTO `user` (role, nickname, public_id, created_at, updated_at)
SELECT 'USER', CONCAT('ranklab-', n), CONCAT('ranklab-', n), NOW(), NOW()
FROM rank_numbers WHERE n < @users ORDER BY n;

INSERT INTO study_session (user_id, start_time, end_time, total_millis, status)
SELECT u.id, TIMESTAMP(DATE_SUB(@day, INTERVAL days.n DAY), '12:00:00'),
       TIMESTAMP(DATE_SUB(@day, INTERVAL days.n DAY), '12:00:00') + INTERVAL (300 + MOD(u.id * 97 + days.n * 29, 1800)) SECOND,
       (300 + MOD(u.id * 97 + days.n * 29, 1800)) * 1000, 'FINISHED'
FROM `user` u CROSS JOIN (SELECT n FROM rank_numbers WHERE n <= 30) days;

INSERT INTO study_session (user_id, start_time, end_time, total_millis, status)
SELECT u.id, TIMESTAMP(@day) + INTERVAL (slots.n - 1) HOUR,
       TIMESTAMP(@day) + INTERVAL (slots.n - 1) HOUR + INTERVAL (300 + MOD(u.id * 97 + slots.n * 29, 1800)) SECOND,
       (300 + MOD(u.id * 97 + slots.n * 29, 1800)) * 1000, 'FINISHED'
FROM `user` u CROSS JOIN (SELECT n FROM rank_numbers WHERE n <= 2) slots;

INSERT INTO study_session (user_id, start_time, status)
SELECT id, TIMESTAMP(@day, '02:00:00') + INTERVAL MOD(id * 97, 3600) SECOND, 'STARTED'
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS user_number FROM `user`) ranked
WHERE MOD(user_number, 20) = 0
ORDER BY id;
COMMIT;

SELECT COUNT(*) AS ranking_users FROM `user`;
SELECT COUNT(*) AS all_sessions, SUM(end_time IS NULL) AS active_sessions,
       SUM(start_time >= @day AND end_time IS NOT NULL) AS finished_today
FROM study_session;
