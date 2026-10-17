-- =============================================================================
-- Migration : table dédiée passenger_preference
-- Prérequis : colonnes passenger.notifications / passenger.language (ou patcher)
-- =============================================================================

USE signalement;

CREATE TABLE IF NOT EXISTS passenger_preference (
    preference_id    BIGINT NOT NULL AUTO_INCREMENT,
    passenger_id     BIGINT NOT NULL,
    language         VARCHAR(2) NOT NULL DEFAULT 'FR',
    notify_platform  TINYINT(1) NOT NULL DEFAULT 1,
    notify_email     TINYINT(1) NOT NULL DEFAULT 1,
    notify_sms       TINYINT(1) NOT NULL DEFAULT 0,
    notify_whatsapp  TINYINT(1) NOT NULL DEFAULT 0,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                     ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (preference_id),
    UNIQUE KEY uk_passenger_preference_passenger (passenger_id),
    CONSTRAINT fk_passenger_preference_passenger
        FOREIGN KEY (passenger_id) REFERENCES passenger (passenger_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Backfill depuis les colonnes legacy (comptes inscrits uniquement)
INSERT INTO passenger_preference (
    passenger_id, language, notify_platform, notify_email, notify_sms, notify_whatsapp,
    created_at, updated_at)
SELECT
    p.passenger_id,
    CASE
        WHEN UPPER(TRIM(p.language)) IN ('FR', 'AR', 'EN') THEN UPPER(TRIM(p.language))
        ELSE 'FR'
    END,
    CASE WHEN p.notifications IS NOT NULL AND JSON_CONTAINS(p.notifications, '1', '$') THEN 1
         WHEN p.notifications IS NULL THEN 1 ELSE 0 END,
    CASE WHEN p.notifications IS NOT NULL AND JSON_CONTAINS(p.notifications, '2', '$') THEN 1
         WHEN p.notifications IS NULL THEN 1 ELSE 0 END,
    CASE WHEN p.notifications IS NOT NULL AND JSON_CONTAINS(p.notifications, '3', '$') THEN 1 ELSE 0 END,
    CASE WHEN p.notifications IS NOT NULL AND JSON_CONTAINS(p.notifications, '4', '$') THEN 1 ELSE 0 END,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM passenger p
WHERE (p.password_hash IS NOT NULL AND p.password_hash <> '')
   OR (p.google_subject IS NOT NULL AND p.google_subject <> '')
ON DUPLICATE KEY UPDATE passenger_id = passenger_id;
