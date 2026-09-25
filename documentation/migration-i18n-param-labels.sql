-- =============================================================================
-- Migration i18n V1 — label_fr / label_ar / label_en
-- Tables : support_type, report_type, report_nature, report_status
--
-- Regles :
--   - Conserve la colonne `label` (miroir FR) — NE PAS DROP en V1
--   - Conserve IDs et FK
--   - Traductions AR : propositions techniques a valider metier avant PROD
--
-- Pre-requis : backup complet de la base
-- =============================================================================

USE signalement;

-- -----------------------------------------------------------------------------
-- 1) ALTER TABLE + COMMENT
-- -----------------------------------------------------------------------------

ALTER TABLE support_type
    ADD COLUMN label_fr VARCHAR(150) NULL COMMENT 'Libelle du type de support en francais',
    ADD COLUMN label_ar VARCHAR(150) NULL COMMENT 'Libelle du type de support en arabe',
    ADD COLUMN label_en VARCHAR(150) NULL COMMENT 'Libelle du type de support en anglais';

ALTER TABLE report_type
    ADD COLUMN label_fr VARCHAR(150) NULL COMMENT 'Libelle du type de signalement en francais',
    ADD COLUMN label_ar VARCHAR(150) NULL COMMENT 'Libelle du type de signalement en arabe',
    ADD COLUMN label_en VARCHAR(150) NULL COMMENT 'Libelle du type de signalement en anglais';

ALTER TABLE report_nature
    ADD COLUMN label_fr VARCHAR(150) NULL COMMENT 'Libelle de la nature en francais',
    ADD COLUMN label_ar VARCHAR(150) NULL COMMENT 'Libelle de la nature en arabe',
    ADD COLUMN label_en VARCHAR(150) NULL COMMENT 'Libelle de la nature en anglais';

ALTER TABLE report_status
    ADD COLUMN label_fr VARCHAR(100) NULL COMMENT 'Libelle du statut en francais',
    ADD COLUMN label_ar VARCHAR(100) NULL COMMENT 'Libelle du statut en arabe',
    ADD COLUMN label_en VARCHAR(100) NULL COMMENT 'Libelle du statut en anglais';

ALTER TABLE support_type
    MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT 'Code metier unique independant de la langue',
    MODIFY COLUMN label VARCHAR(150) NOT NULL COMMENT 'Libelle historique (miroir FR = label_fr)';

ALTER TABLE report_type
    MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT 'Code metier unique independant de la langue',
    MODIFY COLUMN label VARCHAR(150) NOT NULL COMMENT 'Libelle historique (miroir FR = label_fr)';

ALTER TABLE report_nature
    MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT 'Code metier unique independant de la langue',
    MODIFY COLUMN label VARCHAR(150) NOT NULL COMMENT 'Libelle historique (miroir FR = label_fr)';

ALTER TABLE report_status
    MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT 'Code metier unique independant de la langue',
    MODIFY COLUMN label VARCHAR(100) NOT NULL COMMENT 'Libelle historique (miroir FR = label_fr)';

-- -----------------------------------------------------------------------------
-- 2) Copier FR existant vers label_fr
-- -----------------------------------------------------------------------------

UPDATE support_type SET label_fr = label WHERE label_fr IS NULL OR TRIM(label_fr) = '';
UPDATE report_type SET label_fr = label WHERE label_fr IS NULL OR TRIM(label_fr) = '';
UPDATE report_nature SET label_fr = label WHERE label_fr IS NULL OR TRIM(label_fr) = '';
UPDATE report_status SET label_fr = label WHERE label_fr IS NULL OR TRIM(label_fr) = '';

-- -----------------------------------------------------------------------------
-- 3) Traductions EN / AR par code (catalogue seed projet)
--    AR : a valider metier avant mise en production
-- -----------------------------------------------------------------------------

-- support_type
UPDATE support_type SET label_en = 'Bus',     label_ar = 'حافلة'   WHERE code = 'BUS';
UPDATE support_type SET label_en = 'Metro',   label_ar = 'مترو'    WHERE code = 'METRO';
UPDATE support_type SET label_en = 'Train',   label_ar = 'قطار'    WHERE code = 'TRAIN';
UPDATE support_type SET label_en = 'Station', label_ar = 'محطة'    WHERE code = 'STATION';

-- report_type (6 natures voyageur)
UPDATE report_type SET label_en = 'Complaint',  label_ar = 'شكوى'      WHERE code = 'COMPLAINT';
UPDATE report_type SET label_en = 'Assault',    label_ar = 'اعتداء'    WHERE code = 'ASSAULT';
UPDATE report_type SET label_en = 'Incident',   label_ar = 'حادث'      WHERE code = 'INCIDENT';
UPDATE report_type SET label_en = 'Suggestion', label_ar = 'اقتراح'    WHERE code = 'SUGGESTION';
UPDATE report_type SET label_en = 'Thank you',  label_ar = 'شكر'       WHERE code = 'THANKS';
UPDATE report_type SET label_en = 'Other',      label_ar = 'أخرى'      WHERE code = 'OTHER';

-- report_nature
UPDATE report_nature SET label_en = 'Assault',        label_ar = 'اعتداء'       WHERE code = 'AGRESSION';
UPDATE report_nature SET label_en = 'Cleanliness',    label_ar = 'نظافة'        WHERE code = 'PROPRETE';
UPDATE report_nature SET label_en = 'Security',       label_ar = 'أمن'          WHERE code = 'SECURITE';
UPDATE report_nature SET label_en = 'Maintenance',    label_ar = 'صيانة'        WHERE code = 'MAINTENANCE';
UPDATE report_nature SET label_en = 'Information',    label_ar = 'معلومة'       WHERE code = 'INFORMATION';
UPDATE report_nature SET label_en = 'Behavior',       label_ar = 'سلوك'         WHERE code = 'COMPORTEMENT';
UPDATE report_nature SET label_en = 'Delay',          label_ar = 'تأخير'        WHERE code = 'RETARD';
UPDATE report_nature SET label_en = 'Accessibility',  label_ar = 'إمكانية الوصول' WHERE code = 'ACCESSIBILITE';
UPDATE report_nature SET label_en = 'Other',          label_ar = 'أخرى'         WHERE code = 'AUTRE';

-- report_status
UPDATE report_status SET label_en = 'New',         label_ar = 'جديد'       WHERE code = 'NEW';
UPDATE report_status SET label_en = 'In progress', label_ar = 'قيد المعالجة' WHERE code = 'IN_PROGRESS';
UPDATE report_status SET label_en = 'Resolved',    label_ar = 'محلول'      WHERE code = 'RESOLVED';
UPDATE report_status SET label_en = 'Closed',      label_ar = 'مغلق'       WHERE code = 'CLOSED';

-- Normaliser label_fr / label pour codes connus (accents FR corrects)
UPDATE support_type SET label_fr = 'Bus', label = 'Bus' WHERE code = 'BUS';
UPDATE support_type SET label_fr = 'Métro', label = 'Métro' WHERE code = 'METRO';
UPDATE support_type SET label_fr = 'Train', label = 'Train' WHERE code = 'TRAIN';
UPDATE support_type SET label_fr = 'Station', label = 'Station' WHERE code = 'STATION';

UPDATE report_type SET label_fr = 'Réclamation', label = 'Réclamation' WHERE code = 'COMPLAINT';
UPDATE report_type SET label_fr = 'Agression', label = 'Agression' WHERE code = 'ASSAULT';
UPDATE report_type SET label_fr = 'Incident', label = 'Incident' WHERE code = 'INCIDENT';
UPDATE report_type SET label_fr = 'Suggestion', label = 'Suggestion' WHERE code = 'SUGGESTION';
UPDATE report_type SET label_fr = 'Remerciement', label = 'Remerciement' WHERE code = 'THANKS';
UPDATE report_type SET label_fr = 'Autre', label = 'Autre' WHERE code = 'OTHER';

UPDATE report_nature SET label_fr = 'Agression', label = 'Agression' WHERE code = 'AGRESSION';
UPDATE report_nature SET label_fr = 'Propreté', label = 'Propreté' WHERE code = 'PROPRETE';
UPDATE report_nature SET label_fr = 'Sécurité', label = 'Sécurité' WHERE code = 'SECURITE';
UPDATE report_nature SET label_fr = 'Maintenance', label = 'Maintenance' WHERE code = 'MAINTENANCE';
UPDATE report_nature SET label_fr = 'Information', label = 'Information' WHERE code = 'INFORMATION';
UPDATE report_nature SET label_fr = 'Comportement', label = 'Comportement' WHERE code = 'COMPORTEMENT';
UPDATE report_nature SET label_fr = 'Retard', label = 'Retard' WHERE code = 'RETARD';
UPDATE report_nature SET label_fr = 'Accessibilité', label = 'Accessibilité' WHERE code = 'ACCESSIBILITE';
UPDATE report_nature SET label_fr = 'Autre', label = 'Autre' WHERE code = 'AUTRE';

UPDATE report_status SET label_fr = 'Nouveau', label = 'Nouveau' WHERE code = 'NEW';
UPDATE report_status SET label_fr = 'En cours', label = 'En cours' WHERE code = 'IN_PROGRESS';
UPDATE report_status SET label_fr = 'Résolu', label = 'Résolu' WHERE code = 'RESOLVED';
UPDATE report_status SET label_fr = 'Clôturé', label = 'Clôturé' WHERE code = 'CLOSED';

-- Lignes custom (hors catalogue) : au minimum label_fr = label, EN/AR = FR en fallback temporaire
UPDATE support_type SET label_en = COALESCE(NULLIF(TRIM(label_en), ''), label_fr),
                    label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), label_fr)
WHERE label_en IS NULL OR TRIM(label_en) = '' OR label_ar IS NULL OR TRIM(label_ar) = '';

UPDATE report_type SET label_en = COALESCE(NULLIF(TRIM(label_en), ''), label_fr),
                   label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), label_fr)
WHERE label_en IS NULL OR TRIM(label_en) = '' OR label_ar IS NULL OR TRIM(label_ar) = '';

UPDATE report_nature SET label_en = COALESCE(NULLIF(TRIM(label_en), ''), label_fr),
                     label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), label_fr)
WHERE label_en IS NULL OR TRIM(label_en) = '' OR label_ar IS NULL OR TRIM(label_ar) = '';

UPDATE report_status SET label_en = COALESCE(NULLIF(TRIM(label_en), ''), label_fr),
                     label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), label_fr)
WHERE label_en IS NULL OR TRIM(label_en) = '' OR label_ar IS NULL OR TRIM(label_ar) = '';

-- -----------------------------------------------------------------------------
-- 4) Controles
-- -----------------------------------------------------------------------------

SELECT 'support_type' AS t, support_type_id AS id, code, label, label_fr, label_ar, label_en FROM support_type;
SELECT 'report_type' AS t, report_type_id AS id, code, label, label_fr, label_ar, label_en FROM report_type;
SELECT 'report_nature' AS t, report_nature_id AS id, code, label, label_fr, label_ar, label_en FROM report_nature;
SELECT 'report_status' AS t, status_id AS id, code, label, label_fr, label_ar, label_en FROM report_status;

-- Manquants AR
SELECT 'support_type' AS t, code FROM support_type WHERE label_ar IS NULL OR TRIM(label_ar) = ''
UNION ALL SELECT 'report_type', code FROM report_type WHERE label_ar IS NULL OR TRIM(label_ar) = ''
UNION ALL SELECT 'report_nature', code FROM report_nature WHERE label_ar IS NULL OR TRIM(label_ar) = ''
UNION ALL SELECT 'report_status', code FROM report_status WHERE label_ar IS NULL OR TRIM(label_ar) = '';

-- Manquants EN
SELECT 'support_type' AS t, code FROM support_type WHERE label_en IS NULL OR TRIM(label_en) = ''
UNION ALL SELECT 'report_type', code FROM report_type WHERE label_en IS NULL OR TRIM(label_en) = ''
UNION ALL SELECT 'report_nature', code FROM report_nature WHERE label_en IS NULL OR TRIM(label_en) = ''
UNION ALL SELECT 'report_status', code FROM report_status WHERE label_en IS NULL OR TRIM(label_en) = '';

-- Doublons code (doit etre vide)
SELECT code, COUNT(*) AS total FROM support_type GROUP BY code HAVING COUNT(*) > 1;
SELECT code, COUNT(*) AS total FROM report_type GROUP BY code HAVING COUNT(*) > 1;
SELECT code, COUNT(*) AS total FROM report_nature GROUP BY code HAVING COUNT(*) > 1;
SELECT code, COUNT(*) AS total FROM report_status GROUP BY code HAVING COUNT(*) > 1;

-- =============================================================================
-- Rollback soft (NE PAS executer en routine — reseynchronise label depuis label_fr)
-- =============================================================================
-- UPDATE support_type SET label = label_fr WHERE label_fr IS NOT NULL;
-- UPDATE report_type SET label = label_fr WHERE label_fr IS NOT NULL;
-- UPDATE report_nature SET label = label_fr WHERE label_fr IS NOT NULL;
-- UPDATE report_status SET label = label_fr WHERE label_fr IS NOT NULL;
--
-- DROP colonnes (UNIQUEMENT apres periode de validation PROD) :
-- ALTER TABLE support_type DROP COLUMN label_fr, DROP COLUMN label_ar, DROP COLUMN label_en;
-- ALTER TABLE report_type DROP COLUMN label_fr, DROP COLUMN label_ar, DROP COLUMN label_en;
-- ALTER TABLE report_nature DROP COLUMN label_fr, DROP COLUMN label_ar, DROP COLUMN label_en;
-- ALTER TABLE report_status DROP COLUMN label_fr, DROP COLUMN label_ar, DROP COLUMN label_en;
