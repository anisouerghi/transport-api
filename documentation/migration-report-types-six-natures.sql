-- =============================================================================
-- Migration — 6 types de signalement (natures voyageur via report_type)
-- Codes : COMPLAINT, ASSAULT, INCIDENT, SUGGESTION, THANKS, OTHER
--
-- Impact :
--   - INSERT uniquement les codes manquants (idempotent)
--   - Ne modifie PAS report_nature
--   - Ne touche PAS aux FK report.report_type_id existantes
--   - Admin /report-types affichera les nouvelles lignes actives
--
-- Pre-requis : backup complet de la base
-- =============================================================================

USE signalement;

-- -----------------------------------------------------------------------------
-- 1) Creer les codes manquants
-- -----------------------------------------------------------------------------

INSERT INTO report_type (code, label, label_fr, label_ar, label_en, description, active)
SELECT 'COMPLAINT', 'Réclamation', 'Réclamation', 'شكوى', 'Complaint',
       'Réclamation voyageur', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM report_type WHERE code = 'COMPLAINT');

INSERT INTO report_type (code, label, label_fr, label_ar, label_en, description, active)
SELECT 'ASSAULT', 'Agression', 'Agression', 'اعتداء', 'Assault',
       'Signalement d''agression ou de violence', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM report_type WHERE code = 'ASSAULT');

INSERT INTO report_type (code, label, label_fr, label_ar, label_en, description, active)
SELECT 'INCIDENT', 'Incident', 'Incident', 'حادث', 'Incident',
       'Incident technique ou securite', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM report_type WHERE code = 'INCIDENT');

INSERT INTO report_type (code, label, label_fr, label_ar, label_en, description, active)
SELECT 'SUGGESTION', 'Suggestion', 'Suggestion', 'اقتراح', 'Suggestion',
       'Suggestion d''amelioration', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM report_type WHERE code = 'SUGGESTION');

INSERT INTO report_type (code, label, label_fr, label_ar, label_en, description, active)
SELECT 'THANKS', 'Remerciement', 'Remerciement', 'شكر', 'Thank you',
       'Remerciement', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM report_type WHERE code = 'THANKS');

INSERT INTO report_type (code, label, label_fr, label_ar, label_en, description, active)
SELECT 'OTHER', 'Autre', 'Autre', 'أخرى', 'Other',
       'Autre nature de signalement', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM report_type WHERE code = 'OTHER');

-- -----------------------------------------------------------------------------
-- 2) Completer i18n si colonnes vides (ne reecrit pas les libelles deja saisis)
-- -----------------------------------------------------------------------------

UPDATE report_type SET
    label_fr = COALESCE(NULLIF(TRIM(label_fr), ''), label, 'Réclamation'),
    label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), 'شكوى'),
    label_en = COALESCE(NULLIF(TRIM(label_en), ''), 'Complaint'),
    label = COALESCE(NULLIF(TRIM(label), ''), 'Réclamation')
WHERE code = 'COMPLAINT';

UPDATE report_type SET
    label_fr = COALESCE(NULLIF(TRIM(label_fr), ''), label, 'Agression'),
    label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), 'اعتداء'),
    label_en = COALESCE(NULLIF(TRIM(label_en), ''), 'Assault'),
    label = COALESCE(NULLIF(TRIM(label), ''), 'Agression')
WHERE code = 'ASSAULT';

UPDATE report_type SET
    label_fr = COALESCE(NULLIF(TRIM(label_fr), ''), label, 'Incident'),
    label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), 'حادث'),
    label_en = COALESCE(NULLIF(TRIM(label_en), ''), 'Incident'),
    label = COALESCE(NULLIF(TRIM(label), ''), 'Incident')
WHERE code = 'INCIDENT';

UPDATE report_type SET
    label_fr = COALESCE(NULLIF(TRIM(label_fr), ''), label, 'Suggestion'),
    label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), 'اقتراح'),
    label_en = COALESCE(NULLIF(TRIM(label_en), ''), 'Suggestion'),
    label = COALESCE(NULLIF(TRIM(label), ''), 'Suggestion')
WHERE code = 'SUGGESTION';

UPDATE report_type SET
    label_fr = COALESCE(NULLIF(TRIM(label_fr), ''), label, 'Remerciement'),
    label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), 'شكر'),
    label_en = COALESCE(NULLIF(TRIM(label_en), ''), 'Thank you'),
    label = COALESCE(NULLIF(TRIM(label), ''), 'Remerciement')
WHERE code = 'THANKS';

UPDATE report_type SET
    label_fr = COALESCE(NULLIF(TRIM(label_fr), ''), label, 'Autre'),
    label_ar = COALESCE(NULLIF(TRIM(label_ar), ''), 'أخرى'),
    label_en = COALESCE(NULLIF(TRIM(label_en), ''), 'Other'),
    label = COALESCE(NULLIF(TRIM(label), ''), 'Autre')
WHERE code = 'OTHER';

-- -----------------------------------------------------------------------------
-- 3) Controles
-- -----------------------------------------------------------------------------

SELECT code, label, label_fr, label_ar, label_en, active
FROM report_type
WHERE code IN ('COMPLAINT', 'ASSAULT', 'INCIDENT', 'SUGGESTION', 'THANKS', 'OTHER')
ORDER BY FIELD(code, 'COMPLAINT', 'ASSAULT', 'INCIDENT', 'SUGGESTION', 'THANKS', 'OTHER');
