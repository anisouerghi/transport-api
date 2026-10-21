-- Migration optionnelle : notifications plateforme (cloche voyageur).
-- Sur les bases déjà initialisées, DatabaseSchemaPatcher crée aussi cette table au démarrage.

CREATE TABLE IF NOT EXISTS passenger_notification (
    notification_id BIGINT       NOT NULL AUTO_INCREMENT,
    passenger_id    BIGINT       NOT NULL,
    report_id       BIGINT       NOT NULL,
    event_type      VARCHAR(40)  NOT NULL,
    public_status   VARCHAR(150) NULL,
    message         VARCHAR(500) NOT NULL,
    dedup_key       VARCHAR(180) NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    read_at         DATETIME(6)  NULL,
    PRIMARY KEY (notification_id),
    UNIQUE KEY uk_passenger_notification_dedup (passenger_id, dedup_key),
    KEY idx_passenger_notification_inbox (passenger_id, created_at),
    KEY idx_passenger_notification_unread (passenger_id, read_at),
    CONSTRAINT fk_passenger_notification_passenger
        FOREIGN KEY (passenger_id) REFERENCES passenger (passenger_id),
    CONSTRAINT fk_passenger_notification_report
        FOREIGN KEY (report_id) REFERENCES report (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
