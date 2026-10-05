-- =============================================================================
-- Migration préférences de profil voyageur
-- Table : passenger
--
-- Colonnes ajoutées (toutes deux optionnelles / NULL par défaut) :
--   - notifications JSON      : liste d'identifiants de notifications souscrites (ex. [1,3,8])
--   - language     VARCHAR(2) : langue préférée du voyageur (FR, AR ou EN)
--
-- La table passenger contient des contacts anonymes non seekers : aucun
-- backfill n'est appliqué, les lignes existantes restent à NULL et le client
-- applique sa langue par défaut.
--
-- Ces colonnes sont également créées automatiquement au démarrage des deux APIs
-- par DatabaseSchemaPatcher (idempotent) — ce script n'est donc obligatoire que
-- pour appliquer la migration hors application (ou avant un rollback).
--
-- Pre-requis : backup complet de la base
-- =============================================================================

USE signalement;

ALTER TABLE passenger
    ADD COLUMN notifications JSON NULL COMMENT 'Identifiants de notifications souscrites (ex. [1,3,8])',
    ADD COLUMN language VARCHAR(2) NULL COMMENT 'Langue preferee du voyageur (FR, AR, EN)';

-- -----------------------------------------------------------------------------
-- Controles
-- -----------------------------------------------------------------------------

SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'passenger'
  AND COLUMN_NAME IN ('notifications', 'language')
ORDER BY COLUMN_NAME;

-- Valeurs hors liste fermee (doit etre vide)
SELECT passenger_id, language
FROM passenger
WHERE language IS NOT NULL AND language NOT IN ('FR', 'AR', 'EN');

-- JSON invalide (MySQL refuse l'ecriture : controle de coherence)
SELECT passenger_id, notifications
FROM passenger
WHERE notifications IS NOT NULL AND JSON_TYPE(notifications) <> 'ARRAY';

-- -----------------------------------------------------------------------------
-- Rollback
-- -----------------------------------------------------------------------------
-- ALTER TABLE passenger DROP COLUMN notifications, DROP COLUMN language;
