-- Ranking aggregation job storage and duplicate guards.
-- Profiles using Hibernate ddl-auto=validate will not create these objects.

CREATE TABLE IF NOT EXISTS ranking_aggregation_job (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ranking_type VARCHAR(20) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    period_start DATETIME(6) NOT NULL,
    period_end DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    next_retry_at DATETIME(6) NULL,
    locked_by VARCHAR(100) NULL,
    locked_until DATETIME(6) NULL,
    last_error VARCHAR(1000) NULL,
    started_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ranking_aggregation_job_period
        UNIQUE (ranking_type, target_type, period_start, period_end),
    INDEX idx_ranking_aggregation_job_pickup
        (status, next_retry_at, locked_until, attempt_count)
);

ALTER TABLE user_ranking
    ADD CONSTRAINT uk_user_ranking_period_user
    UNIQUE (ranking_type, calculated_at, user_id);

ALTER TABLE department_ranking
    ADD CONSTRAINT uk_department_ranking_period_department
    UNIQUE (ranking_type, calculated_at, department);

-- Rollback, only if the ranking aggregation job feature is removed:
-- ALTER TABLE department_ranking DROP INDEX uk_department_ranking_period_department;
-- ALTER TABLE user_ranking DROP INDEX uk_user_ranking_period_user;
-- DROP TABLE IF EXISTS ranking_aggregation_job;
