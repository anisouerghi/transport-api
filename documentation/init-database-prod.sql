-- =============================================================================
-- Initialisation base MySQL — signalement
-- À exécuter UNE FOIS sur le serveur MySQL (utilisateur avec droits CREATE).
--
-- Nom de base = celui utilisé par admin-api / public-api par défaut :
--   jdbc:mysql://...:3306/signalement?...
--
-- Contenu :
--   1) CREATE DATABASE signalement
--   2) Schéma (tables + FK)
--   3) INSERT référentiels (statuts, types, natures, district, permissions, rôles, menus)
--
-- Compte admin :
--   Après ce script, démarrer admin-api (dev ou prod).
--   SecurityDataInitializer crée : admin / admin123 (rôle ADMIN).
-- =============================================================================

CREATE DATABASE IF NOT EXISTS signalement
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE signalement;

-- Workbench: Connection > Advanced > Character Set = utf8mb4 (sinon erreurs accents)
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reply;
DROP TABLE IF EXISTS report_history;
DROP TABLE IF EXISTS attachment;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS transport_support;
DROP TABLE IF EXISTS district;
DROP TABLE IF EXISTS report_nature;
DROP TABLE IF EXISTS report_type;
DROP TABLE IF EXISTS passenger_otp_challenge;
DROP TABLE IF EXISTS passenger;
DROP TABLE IF EXISTS report_status;
DROP TABLE IF EXISTS support_type;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS app_menu;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS app_user;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- Tables
-- -----------------------------------------------------------------------------

CREATE TABLE support_type (
    support_type_id BIGINT NOT NULL AUTO_INCREMENT,
    code            VARCHAR(50)  NOT NULL,
    label           VARCHAR(150) NOT NULL,
    PRIMARY KEY (support_type_id),
    UNIQUE KEY uk_support_type_code (code)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE district (
    district_id      BIGINT NOT NULL AUTO_INCREMENT,
    code_district    VARCHAR(10)  NOT NULL,
    libelle_district VARCHAR(45) NOT NULL,
    etat             INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (district_id),
    UNIQUE KEY uk_district_code (code_district),
    KEY idx_district_etat (etat)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report_type (
    report_type_id BIGINT       NOT NULL AUTO_INCREMENT,
    code           VARCHAR(50)  NOT NULL,
    label          VARCHAR(150) NOT NULL,
    description    VARCHAR(500) NULL,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (report_type_id),
    UNIQUE KEY uk_report_type_code (code)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report_nature (
    report_nature_id BIGINT       NOT NULL AUTO_INCREMENT,
    code             VARCHAR(50)  NOT NULL,
    label            VARCHAR(150) NOT NULL,
    description      VARCHAR(500) NULL,
    active           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (report_nature_id),
    UNIQUE KEY uk_report_nature_code (code)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report_status (
    status_id     BIGINT NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50)  NOT NULL,
    label         VARCHAR(100) NOT NULL,
    display_order INT          NOT NULL,
    PRIMARY KEY (status_id),
    UNIQUE KEY uk_report_status_code (code)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE passenger (
    passenger_id         BIGINT NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(150) NULL,
    email                VARCHAR(255) NULL,
    phone_number         VARCHAR(30)  NULL,
    email_verified       BIT(1)       NOT NULL DEFAULT 0,
    active               TINYINT(1)   NOT NULL DEFAULT 1,
    password_hash        VARCHAR(255) NULL,
    google_subject       VARCHAR(255) NULL,
    auth_provider        VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    profile_picture_url  VARCHAR(512) NULL,
    last_ip              VARCHAR(64)  NULL,
    last_user_agent      VARCHAR(512) NULL,
    last_browser         VARCHAR(50)  NULL,
    latitude             DOUBLE NULL,
    longitude            DOUBLE NULL,
    gps_accuracy         DOUBLE NULL,
    gps_captured_at      DATETIME(6) NULL,
    last_auth_at         DATETIME(6) NULL,
    PRIMARY KEY (passenger_id),
    UNIQUE KEY uk_passenger_google_subject (google_subject(191)),
    KEY idx_passenger_active (active),
    KEY idx_passenger_email (email(191))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE passenger_otp_challenge (
    challenge_id   BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id VARCHAR(36)  NOT NULL,
    passenger_id   BIGINT       NOT NULL,
    otp_hash       VARCHAR(255) NOT NULL,
    attempt_count  INT          NOT NULL DEFAULT 0,
    send_count     INT          NOT NULL DEFAULT 1,
    expires_at     DATETIME(6)  NOT NULL,
    last_sent_at   DATETIME(6)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (challenge_id),
    UNIQUE KEY uk_otp_transaction (transaction_id),
    KEY idx_otp_challenge_passenger (passenger_id),
    KEY idx_otp_challenge_status (status)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_user (
    user_id       BIGINT NOT NULL AUTO_INCREMENT,
    uuid          VARCHAR(36)  NOT NULL,
    username      VARCHAR(100) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_date  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_app_user_uuid (uuid),
    UNIQUE KEY uk_app_user_username (username),
    UNIQUE KEY uk_app_user_email (email(191))
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role (
    role_id     BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permission (
    permission_id BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(80)  NOT NULL,
    label         VARCHAR(150) NOT NULL,
    description   VARCHAR(500) NULL,
    module_code   VARCHAR(80)  NOT NULL,
    module_label  VARCHAR(150) NOT NULL,
    action_code   VARCHAR(40)  NOT NULL,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (permission_id),
    UNIQUE KEY uk_permission_code (code),
    UNIQUE KEY uk_permission_module_action (module_code, action_code),
    KEY idx_permission_module (module_code)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (role_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES role (role_id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission (permission_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_menu (
    menu_id         BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(50)  NOT NULL,
    label           VARCHAR(150) NOT NULL,
    url             VARCHAR(255) NOT NULL,
    icon            VARCHAR(80)  NULL,
    display_order   INT          NOT NULL DEFAULT 0,
    permission_code VARCHAR(80)  NULL,
    active          TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (menu_id),
    UNIQUE KEY uk_app_menu_code (code)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE transport_support (
    transport_support_id BIGINT       NOT NULL AUTO_INCREMENT,
    uuid                 VARCHAR(36)  NOT NULL,
    reference            VARCHAR(50)  NOT NULL,
    label                VARCHAR(150) NOT NULL,
    qr_code_url          VARCHAR(500) NULL,
    qr_code_path         VARCHAR(500) NULL,
    qr_date_creation     DATETIME(6)  NULL,
    qr_date_impression   DATETIME(6)  NULL,
    qr_status            VARCHAR(30)  NULL,
    support_status       VARCHAR(30)  NOT NULL,
    support_type_id      BIGINT       NOT NULL,
    district_id          BIGINT       NOT NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (transport_support_id),
    UNIQUE KEY uk_transport_support_uuid (uuid),
    UNIQUE KEY uk_transport_support_reference (reference),
    KEY idx_transport_support_reference (reference),
    KEY idx_transport_support_uuid (uuid),
    KEY idx_transport_support_support_status (support_status),
    KEY idx_transport_support_qr_status (qr_status),
    KEY idx_transport_support_district (district_id),
    CONSTRAINT fk_transport_support_type
        FOREIGN KEY (support_type_id) REFERENCES support_type (support_type_id),
    CONSTRAINT fk_transport_support_district
        FOREIGN KEY (district_id) REFERENCES district (district_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report (
    report_id            BIGINT NOT NULL AUTO_INCREMENT,
    uuid                 VARCHAR(36)  NOT NULL,
    reference            VARCHAR(40)  NOT NULL,
    creation_date        DATETIME(6)  NOT NULL,
    description          TEXT         NOT NULL,
    priority             VARCHAR(30)  NULL,
    closure_date         DATETIME(6)  NULL,
    publish              TINYINT(1)   NOT NULL DEFAULT 0,
    publish_date         DATETIME(6)  NULL,
    send_email           TINYINT(1)   NOT NULL DEFAULT 0,
    send_email_date      DATETIME(6)  NULL,
    public_response      TINYINT(1)   NOT NULL DEFAULT 0,
    public_response_date DATETIME(6)  NULL,
    transport_support_id BIGINT       NULL,
    report_type_id       BIGINT,
    nature_id            BIGINT       NULL,
    passenger_id         BIGINT       NULL,
    status_id            BIGINT       NOT NULL,
    PRIMARY KEY (report_id),
    UNIQUE KEY uk_report_uuid (uuid),
    UNIQUE KEY uk_report_reference (reference),
    KEY idx_report_nature (nature_id),
    CONSTRAINT fk_report_support
        FOREIGN KEY (transport_support_id) REFERENCES transport_support (transport_support_id),
    CONSTRAINT fk_report_type
        FOREIGN KEY (report_type_id) REFERENCES report_type (report_type_id),
    CONSTRAINT fk_report_nature
        FOREIGN KEY (nature_id) REFERENCES report_nature (report_nature_id),
    CONSTRAINT fk_report_passenger
        FOREIGN KEY (passenger_id) REFERENCES passenger (passenger_id),
    CONSTRAINT fk_report_status
        FOREIGN KEY (status_id) REFERENCES report_status (status_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attachment (
    attachment_id BIGINT NOT NULL AUTO_INCREMENT,
    uuid          VARCHAR(36)  NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    file_type     VARCHAR(100) NULL,
    report_id     BIGINT       NOT NULL,
    PRIMARY KEY (attachment_id),
    UNIQUE KEY uk_attachment_uuid (uuid),
    CONSTRAINT fk_attachment_report
        FOREIGN KEY (report_id) REFERENCES report (report_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report_history (
    history_id    BIGINT NOT NULL AUTO_INCREMENT,
    old_status_id BIGINT NULL,
    new_status_id BIGINT NOT NULL,
    comments      VARCHAR(1000) NULL,
    action_date   DATETIME(6)   NOT NULL,
    report_id     BIGINT        NOT NULL,
    user_id       BIGINT        NULL,
    PRIMARY KEY (history_id),
    CONSTRAINT fk_history_old_status
        FOREIGN KEY (old_status_id) REFERENCES report_status (status_id),
    CONSTRAINT fk_history_new_status
        FOREIGN KEY (new_status_id) REFERENCES report_status (status_id),
    CONSTRAINT fk_history_report
        FOREIGN KEY (report_id) REFERENCES report (report_id),
    CONSTRAINT fk_history_user
        FOREIGN KEY (user_id) REFERENCES app_user (user_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reply (
    reply_id         BIGINT NOT NULL AUTO_INCREMENT,
    uuid             VARCHAR(36) NOT NULL,
    message          TEXT        NOT NULL,
    reply_date       DATETIME(6) NOT NULL,
    email_sent       BIT(1)      NOT NULL DEFAULT 0,
    public_response  TINYINT(1)  NOT NULL DEFAULT 1,
    report_id        BIGINT      NOT NULL,
    user_id          BIGINT      NULL,
    PRIMARY KEY (reply_id),
    UNIQUE KEY uk_reply_uuid (uuid),
    CONSTRAINT fk_reply_report
        FOREIGN KEY (report_id) REFERENCES report (report_id),
    CONSTRAINT fk_reply_user
        FOREIGN KEY (user_id) REFERENCES app_user (user_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_log (
    audit_log_id      BIGINT       NOT NULL AUTO_INCREMENT,
    action_date       DATETIME(6)  NOT NULL,
    user_id           BIGINT       NULL,
    username          VARCHAR(100) NULL,
    user_full_name    VARCHAR(150) NULL,
    ip_address        VARCHAR(64)  NULL,
    action_type       VARCHAR(40)  NOT NULL,
    module            VARCHAR(40)  NOT NULL,
    entity_name       VARCHAR(100) NULL,
    entity_id         VARCHAR(100) NULL,
    old_value         TEXT         NULL,
    new_value         TEXT         NULL,
    description       VARCHAR(2000) NULL,
    user_agent        VARCHAR(500) NULL,
    browser           VARCHAR(120) NULL,
    operating_system  VARCHAR(120) NULL,
    result            VARCHAR(20)  NOT NULL,
    PRIMARY KEY (audit_log_id),
    KEY idx_audit_log_action_date (action_date),
    KEY idx_audit_log_module (module),
    KEY idx_audit_log_action_type (action_type),
    KEY idx_audit_log_user_id (user_id),
    KEY idx_audit_log_result (result),
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES app_user (user_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- INSERT - referentiels metier (ASCII only : compatible Workbench sans utf8mb4 client)
-- -----------------------------------------------------------------------------

INSERT INTO report_status (code, label, display_order) VALUES
('NEW', 'Nouveau', 1),
('IN_PROGRESS', 'En cours', 2),
('RESOLVED', 'Resolu', 3),
('CLOSED', 'Cloture', 4);

INSERT INTO support_type (code, label) VALUES
('BUS', 'Bus'),
('METRO', 'Metro'),
('TRAIN', 'Train'),
('STATION', 'Station');

INSERT INTO report_type (code, label, description, active) VALUES
('COMPLAINT', 'Reclamation', 'Reclamation voyageur', 1),
('ASSAULT', 'Agression', 'Signalement d''agression ou de violence', 1),
('INCIDENT', 'Incident', 'Incident technique ou securite', 1),
('SUGGESTION', 'Suggestion', 'Suggestion d''amelioration', 1),
('THANKS', 'Remerciement', 'Remerciement', 1),
('OTHER', 'Autre', 'Autre nature de signalement', 1);

INSERT INTO report_nature (code, label, description, active) VALUES
('AGRESSION', 'Agression', 'Signalements lies a une agression ou violence', 1),
('PROPRETE', 'Proprete', 'Signalements lies a la proprete des vehicules et stations', 1),
('SECURITE', 'Securite', 'Signalements lies a la securite des voyageurs et des biens', 1),
('MAINTENANCE', 'Maintenance', 'Signalements lies a la maintenance ou aux pannes', 1),
('INFORMATION', 'Information', 'Demandes ou manques d''information', 1),
('COMPORTEMENT', 'Comportement', 'Signalements lies au comportement (voyageurs ou agents)', 1),
('RETARD', 'Retard', 'Signalements lies aux retards et perturbations', 1),
('ACCESSIBILITE', 'Accessibilite', 'Signalements lies a l''accessibilite', 1),
('AUTRE', 'Autre', 'Autres natures non listees', 1);

INSERT INTO district (code_district, libelle_district, etat) VALUES
('A', 'TUNIS II (CHARGUIA)', 1);

-- -----------------------------------------------------------------------------
-- INSERT — permissions (catalogue SecurityDataInitializer)
-- -----------------------------------------------------------------------------

INSERT INTO permission (code, label, description, module_code, module_label, action_code, active) VALUES
('DASHBOARD_VIEW', 'Consulter le tableau de bord', 'Consulter le tableau de bord', 'DASHBOARD', 'Dashboard', 'VIEW', 1),

('REPORT_VIEW', 'Consulter les signalements', 'Consulter les signalements', 'REPORT', 'Signalements', 'VIEW', 1),
('REPORT_SEARCH', 'Rechercher les signalements', 'Rechercher les signalements', 'REPORT', 'Signalements', 'SEARCH', 1),
('REPORT_EXPORT', 'Exporter les signalements', 'Exporter les signalements', 'REPORT', 'Signalements', 'EXPORT', 1),
('REPORT_PRINT', 'Imprimer un signalement', 'Imprimer un signalement', 'REPORT', 'Signalements', 'PRINT', 1),
('REPORT_REPLY', 'Repondre a un signalement', 'Repondre a un signalement', 'REPORT', 'Signalements', 'REPLY', 1),
('REPORT_ASSIGN', 'Affecter un signalement', 'Affecter un signalement', 'REPORT', 'Signalements', 'ASSIGN', 1),
('REPORT_CLOSE', 'Cloturer / changer le statut', 'Cloturer / changer le statut', 'REPORT', 'Signalements', 'CLOSE', 1),
('REPORT_EDIT', 'Modifier un signalement', 'Modifier un signalement', 'REPORT', 'Signalements', 'EDIT', 1),
('REPORT_UPDATE_PRIORITY', 'Definir / modifier la priorite', 'Definir / modifier la priorite', 'REPORT', 'Signalements', 'UPDATE_PRIORITY', 1),
('REPORT_ASSIGN_NATURE', 'Affecter une nature a un signalement', 'Affecter une nature a un signalement', 'REPORT', 'Signalements', 'ASSIGN_NATURE', 1),

('NATURE_VIEW', 'Consulter les natures', 'Consulter les natures', 'NATURE', 'Natures des signalements', 'VIEW', 1),
('NATURE_ADD', 'Creer une nature', 'Creer une nature', 'NATURE', 'Natures des signalements', 'ADD', 1),
('NATURE_EDIT', 'Modifier une nature', 'Modifier une nature', 'NATURE', 'Natures des signalements', 'EDIT', 1),
('NATURE_DELETE', 'Supprimer une nature', 'Supprimer une nature', 'NATURE', 'Natures des signalements', 'DELETE', 1),
('NATURE_SEARCH', 'Rechercher les natures', 'Rechercher les natures', 'NATURE', 'Natures des signalements', 'SEARCH', 1),
('NATURE_ACTIVATE', 'Activer une nature', 'Activer une nature', 'NATURE', 'Natures des signalements', 'ACTIVATE', 1),
('NATURE_DEACTIVATE', 'Desactiver une nature', 'Desactiver une nature', 'NATURE', 'Natures des signalements', 'DEACTIVATE', 1),

('REPORT_TYPE_VIEW', 'Consulter les types', 'Consulter les types', 'REPORT_TYPE', 'Types de signalement', 'VIEW', 1),
('REPORT_TYPE_ADD', 'Creer un type', 'Creer un type', 'REPORT_TYPE', 'Types de signalement', 'ADD', 1),
('REPORT_TYPE_EDIT', 'Modifier un type', 'Modifier un type', 'REPORT_TYPE', 'Types de signalement', 'EDIT', 1),
('REPORT_TYPE_DELETE', 'Supprimer un type', 'Supprimer un type', 'REPORT_TYPE', 'Types de signalement', 'DELETE', 1),
('REPORT_TYPE_SEARCH', 'Rechercher les types', 'Rechercher les types', 'REPORT_TYPE', 'Types de signalement', 'SEARCH', 1),
('REPORT_TYPE_ACTIVATE', 'Activer un type', 'Activer un type', 'REPORT_TYPE', 'Types de signalement', 'ACTIVATE', 1),
('REPORT_TYPE_DEACTIVATE', 'Desactiver un type', 'Desactiver un type', 'REPORT_TYPE', 'Types de signalement', 'DEACTIVATE', 1),

('SUPPORT_TYPE_VIEW', 'Consulter les types de support', 'Consulter les types de support', 'SUPPORT_TYPE', 'Types de support', 'VIEW', 1),
('SUPPORT_TYPE_ADD', 'Creer un type de support', 'Creer un type de support', 'SUPPORT_TYPE', 'Types de support', 'ADD', 1),
('SUPPORT_TYPE_EDIT', 'Modifier un type de support', 'Modifier un type de support', 'SUPPORT_TYPE', 'Types de support', 'EDIT', 1),
('SUPPORT_TYPE_DELETE', 'Supprimer un type de support', 'Supprimer un type de support', 'SUPPORT_TYPE', 'Types de support', 'DELETE', 1),
('SUPPORT_TYPE_SEARCH', 'Rechercher les types de support', 'Rechercher les types de support', 'SUPPORT_TYPE', 'Types de support', 'SEARCH', 1),

('TRANSPORT_SUPPORT_VIEW', 'Consulter les supports', 'Consulter les supports', 'TRANSPORT_SUPPORT', 'Supports transport', 'VIEW', 1),
('TRANSPORT_SUPPORT_ADD', 'Creer un support', 'Creer un support', 'TRANSPORT_SUPPORT', 'Supports transport', 'ADD', 1),
('TRANSPORT_SUPPORT_EDIT', 'Modifier un support', 'Modifier un support', 'TRANSPORT_SUPPORT', 'Supports transport', 'EDIT', 1),
('TRANSPORT_SUPPORT_DELETE', 'Supprimer un support', 'Supprimer un support', 'TRANSPORT_SUPPORT', 'Supports transport', 'DELETE', 1),
('TRANSPORT_SUPPORT_SEARCH', 'Rechercher les supports', 'Rechercher les supports', 'TRANSPORT_SUPPORT', 'Supports transport', 'SEARCH', 1),
('TRANSPORT_SUPPORT_PRINT', 'Imprimer / telecharger le QR', 'Imprimer / telecharger le QR', 'TRANSPORT_SUPPORT', 'Supports transport', 'PRINT', 1),
('TRANSPORT_SUPPORT_ACTIVATE', 'Activer un support', 'Activer un support', 'TRANSPORT_SUPPORT', 'Supports transport', 'ACTIVATE', 1),
('TRANSPORT_SUPPORT_DEACTIVATE', 'Desactiver un support', 'Desactiver un support', 'TRANSPORT_SUPPORT', 'Supports transport', 'DEACTIVATE', 1),

('USER_VIEW', 'Consulter les utilisateurs', 'Consulter les utilisateurs', 'USER', 'Utilisateurs', 'VIEW', 1),
('USER_ADD', 'Creer un utilisateur', 'Creer un utilisateur', 'USER', 'Utilisateurs', 'ADD', 1),
('USER_EDIT', 'Modifier un utilisateur', 'Modifier un utilisateur', 'USER', 'Utilisateurs', 'EDIT', 1),
('USER_DELETE', 'Supprimer un utilisateur', 'Supprimer un utilisateur', 'USER', 'Utilisateurs', 'DELETE', 1),
('USER_SEARCH', 'Rechercher les utilisateurs', 'Rechercher les utilisateurs', 'USER', 'Utilisateurs', 'SEARCH', 1),
('USER_EXPORT', 'Exporter les utilisateurs', 'Exporter les utilisateurs', 'USER', 'Utilisateurs', 'EXPORT', 1),
('USER_ACTIVATE', 'Activer un utilisateur', 'Activer un utilisateur', 'USER', 'Utilisateurs', 'ACTIVATE', 1),
('USER_DEACTIVATE', 'Desactiver un utilisateur', 'Desactiver un utilisateur', 'USER', 'Utilisateurs', 'DEACTIVATE', 1),

('ROLE_VIEW', 'Consulter les roles', 'Consulter les roles', 'ROLE', 'Roles', 'VIEW', 1),
('ROLE_ADD', 'Creer un role', 'Creer un role', 'ROLE', 'Roles', 'ADD', 1),
('ROLE_EDIT', 'Modifier un role', 'Modifier un role', 'ROLE', 'Roles', 'EDIT', 1),
('ROLE_DELETE', 'Supprimer un role', 'Supprimer un role', 'ROLE', 'Roles', 'DELETE', 1),
('ROLE_SEARCH', 'Rechercher les roles', 'Rechercher les roles', 'ROLE', 'Roles', 'SEARCH', 1),
('ROLE_ACTIVATE', 'Activer un role', 'Activer un role', 'ROLE', 'Roles', 'ACTIVATE', 1),
('ROLE_DEACTIVATE', 'Desactiver un role', 'Desactiver un role', 'ROLE', 'Roles', 'DEACTIVATE', 1),

('PERMISSION_VIEW', 'Consulter les permissions', 'Consulter les permissions', 'PERMISSION', 'Permissions', 'VIEW', 1),
('PERMISSION_SEARCH', 'Rechercher les permissions', 'Rechercher les permissions', 'PERMISSION', 'Permissions', 'SEARCH', 1),

('AUDIT_VIEW', 'Consulter le journal d''audit', 'Consulter le journal d''audit', 'AUDIT', 'Journal d''audit', 'VIEW', 1),
('AUDIT_SEARCH', 'Rechercher dans l''audit', 'Rechercher dans l''audit', 'AUDIT', 'Journal d''audit', 'SEARCH', 1),
('AUDIT_EXPORT', 'Exporter l''audit', 'Exporter l''audit', 'AUDIT', 'Journal d''audit', 'EXPORT', 1),

('STATUS_VIEW', 'Consulter les statuts', 'Consulter les statuts', 'STATUS', 'Statuts', 'VIEW', 1),
('STATUS_ADD', 'Creer un statut', 'Creer un statut', 'STATUS', 'Statuts', 'ADD', 1),
('STATUS_EDIT', 'Modifier un statut', 'Modifier un statut', 'STATUS', 'Statuts', 'EDIT', 1),
('STATUS_DELETE', 'Supprimer un statut', 'Supprimer un statut', 'STATUS', 'Statuts', 'DELETE', 1),

('PASSENGER_VIEW', 'Consulter les voyageurs', 'Consulter les voyageurs', 'PASSENGER', 'Voyageurs', 'VIEW', 1),
('PASSENGER_SEARCH', 'Rechercher les voyageurs', 'Rechercher les voyageurs', 'PASSENGER', 'Voyageurs', 'SEARCH', 1),
('PASSENGER_ACTIVATE', 'Activer un voyageur', 'Activer un voyageur', 'PASSENGER', 'Voyageurs', 'ACTIVATE', 1),
('PASSENGER_DEACTIVATE', 'Desactiver un voyageur', 'Desactiver un voyageur', 'PASSENGER', 'Voyageurs', 'DEACTIVATE', 1),

('REPORT_STATISTICS_VIEW', 'Consulter les rapports et statistiques', 'Consulter les rapports et statistiques', 'REPORT_STATISTICS', 'Rapports & Statistiques', 'VIEW', 1);

-- -----------------------------------------------------------------------------
-- INSERT - roles
-- -----------------------------------------------------------------------------

INSERT INTO role (code, label, description, active) VALUES
('ADMIN', 'Administrateur', 'Acces complet', 1),
('AGENT', 'Agent', 'Traitement des signalements', 1),
('RESPONSABLE', 'Responsable', 'Supervision', 1);

-- ADMIN = toutes les permissions
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'ADMIN';

-- AGENT
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permission p ON p.code IN (
  'DASHBOARD_VIEW',
  'REPORT_VIEW', 'REPORT_SEARCH', 'REPORT_REPLY', 'REPORT_CLOSE', 'REPORT_EDIT',
  'REPORT_UPDATE_PRIORITY', 'REPORT_ASSIGN_NATURE',
  'TRANSPORT_SUPPORT_VIEW', 'TRANSPORT_SUPPORT_SEARCH', 'TRANSPORT_SUPPORT_PRINT',
  'PASSENGER_VIEW', 'PASSENGER_SEARCH',
  'STATUS_VIEW',
  'NATURE_VIEW'
)
WHERE r.code = 'AGENT';

-- RESPONSABLE
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permission p ON p.code IN (
  'DASHBOARD_VIEW',
  'REPORT_VIEW', 'REPORT_SEARCH', 'REPORT_EXPORT', 'REPORT_PRINT',
  'REPORT_REPLY', 'REPORT_ASSIGN', 'REPORT_CLOSE', 'REPORT_EDIT',
  'REPORT_UPDATE_PRIORITY', 'REPORT_ASSIGN_NATURE',
  'REPORT_TYPE_VIEW', 'REPORT_TYPE_ADD', 'REPORT_TYPE_EDIT', 'REPORT_TYPE_SEARCH',
  'REPORT_TYPE_ACTIVATE', 'REPORT_TYPE_DEACTIVATE',
  'NATURE_VIEW', 'NATURE_ADD', 'NATURE_EDIT', 'NATURE_SEARCH',
  'NATURE_ACTIVATE', 'NATURE_DEACTIVATE',
  'SUPPORT_TYPE_VIEW', 'SUPPORT_TYPE_ADD', 'SUPPORT_TYPE_EDIT', 'SUPPORT_TYPE_SEARCH',
  'TRANSPORT_SUPPORT_VIEW', 'TRANSPORT_SUPPORT_ADD', 'TRANSPORT_SUPPORT_EDIT',
  'TRANSPORT_SUPPORT_SEARCH', 'TRANSPORT_SUPPORT_PRINT',
  'TRANSPORT_SUPPORT_ACTIVATE', 'TRANSPORT_SUPPORT_DEACTIVATE',
  'PASSENGER_VIEW', 'PASSENGER_SEARCH', 'PASSENGER_ACTIVATE', 'PASSENGER_DEACTIVATE',
  'REPORT_STATISTICS_VIEW',
  'AUDIT_VIEW', 'AUDIT_SEARCH', 'AUDIT_EXPORT',
  'STATUS_VIEW'
)
WHERE r.code = 'RESPONSABLE';

-- -----------------------------------------------------------------------------
-- INSERT — menus admin
-- -----------------------------------------------------------------------------

INSERT INTO app_menu (code, label, url, icon, display_order, permission_code, active) VALUES
('DASHBOARD', 'Dashboard', '/dashboard', 'cilSpeedometer', 10, 'DASHBOARD_VIEW', 1),
('REPORTS', 'Signalements', '/reports', 'cilList', 20, 'REPORT_VIEW', 1),
('PASSENGERS', 'Voyageurs', '/passengers', 'cilPeople', 25, 'PASSENGER_VIEW', 1),
('STATISTICS', 'Rapports & Statistiques', '/statistics', 'cilChart', 28, 'REPORT_STATISTICS_VIEW', 1),
('TRANSPORT_SUPPORTS', 'Supports', '/transport-supports', 'cilList', 30, 'TRANSPORT_SUPPORT_VIEW', 1),
('SUPPORT_TYPES', 'Types de support', '/support-types', 'cilList', 40, 'SUPPORT_TYPE_VIEW', 1),
('REPORT_TYPES', 'Types de signalement', '/report-types', 'cilSpeech', 50, 'REPORT_TYPE_VIEW', 1),
('REPORT_NATURES', 'Natures des signalements', '/report-natures', 'cilTags', 55, 'NATURE_VIEW', 1),
('USERS', 'Utilisateurs', '/users', 'cilUser', 60, 'USER_VIEW', 1),
('ROLES', 'Roles', '/roles', 'cilLockLocked', 70, 'ROLE_VIEW', 1),
('PERMISSIONS', 'Permissions', '/permissions', 'cilLockLocked', 80, 'PERMISSION_VIEW', 1),
('AUDIT', 'Journal d''audit', '/audit-logs', 'cilHistory', 90, 'AUDIT_VIEW', 1);

-- -----------------------------------------------------------------------------
-- Utilisateur admin : créé au 1er démarrage de admin-api
-- (SecurityDataInitializer → admin / admin123)
-- Ne pas insérer de password_hash à la main ici.
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
-- Vérifications
-- -----------------------------------------------------------------------------

SELECT 'report_status' AS t, COUNT(*) AS n FROM report_status
UNION ALL SELECT 'support_type', COUNT(*) FROM support_type
UNION ALL SELECT 'report_type', COUNT(*) FROM report_type
UNION ALL SELECT 'report_nature', COUNT(*) FROM report_nature
UNION ALL SELECT 'district', COUNT(*) FROM district
UNION ALL SELECT 'permission', COUNT(*) FROM permission
UNION ALL SELECT 'role', COUNT(*) FROM role
UNION ALL SELECT 'role_permission', COUNT(*) FROM role_permission
UNION ALL SELECT 'app_menu', COUNT(*) FROM app_menu;
