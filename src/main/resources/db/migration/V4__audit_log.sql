-- Orchestrate — V4__audit_log.sql
--
-- Журнал аудиту бізнес-операцій. Один рядок = один виклик методу, позначеного @AuditAction.
-- Рядок пишеться в окремій транзакції (REQUIRES_NEW), тому переживає відкат бізнес-транзакції.
-- user_id свідомо без FK: запис історичний і має лишатися навіть після видалення користувача,
-- а також для анонімних викликів (NULL).

CREATE TABLE audit_log (
    id            UUID                     NOT NULL,
    occurred_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id       UUID,
    action        VARCHAR(100)             NOT NULL,
    method_name   VARCHAR(255)             NOT NULL,
    arguments     VARCHAR(4000),
    result        VARCHAR(4000),
    success       BOOLEAN                  NOT NULL,
    error_type    VARCHAR(255),
    error_message VARCHAR(2000),
    duration_ms   BIGINT                   NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT chk_audit_log_duration CHECK (duration_ms >= 0),
    CONSTRAINT chk_audit_log_outcome CHECK (
        (success = TRUE AND error_type IS NULL AND error_message IS NULL)
        OR (success = FALSE AND error_type IS NOT NULL)
    )
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at);
CREATE INDEX idx_audit_log_user ON audit_log (user_id);
CREATE INDEX idx_audit_log_action ON audit_log (action);
