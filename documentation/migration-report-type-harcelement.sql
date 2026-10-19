-- =============================================================================
-- Migration : type de signalement HARCELEMENT (harcèlement sexuel)
-- Idempotent — ne force pas report_type_id = 8
-- =============================================================================

USE signalement;

INSERT INTO report_type (
    code, label, label_fr, label_ar, label_en, description, active, priority, icon
)
SELECT
    'HARCELEMENT',
    'Harcèlement sexuel',
    'Harcèlement sexuel',
    'تحرش جنسي',
    'Sexual harassment',
    'Harcèlement sexuel',
    1,
    2,
    'crisis_alert'
WHERE NOT EXISTS (
    SELECT 1 FROM report_type WHERE code = 'HARCELEMENT'
);

-- Contrôle
SELECT report_type_id, code, label_fr, label_ar, label_en, priority, icon, active
FROM report_type
WHERE code = 'HARCELEMENT';
