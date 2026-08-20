-- Schema explicite InnoDB (évite errno 150 avec Hibernate ddl-auto)
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reply;
DROP TABLE IF EXISTS report_history;
DROP TABLE IF EXISTS attachment;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS transport_support;
DROP TABLE IF EXISTS district;
DROP TABLE IF EXISTS report_type;
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

CREATE TABLE support_type (
    support_type_id BIGINT NOT NULL AUTO_INCREMENT,
    code            VARCHAR(50)  NOT NULL,
    label           VARCHAR(150) NOT NULL,
    PRIMARY KEY (support_type_id),
    UNIQUE KEY uk_support_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE district (
    district_id     BIGINT NOT NULL AUTO_INCREMENT,
    code_district   VARCHAR(10)  NOT NULL,
    libelle_district VARCHAR(45) NOT NULL,
    etat            INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (district_id),
    UNIQUE KEY uk_district_code (code_district),
    KEY idx_district_etat (etat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report_type (
    report_type_id BIGINT       NOT NULL AUTO_INCREMENT,
    code           VARCHAR(50)  NOT NULL,
    label          VARCHAR(150) NOT NULL,
    description    VARCHAR(500) NULL,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (report_type_id),
    UNIQUE KEY uk_report_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report_status (
    status_id     BIGINT NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50)  NOT NULL,
    label         VARCHAR(100) NOT NULL,
    display_order INT          NOT NULL,
    PRIMARY KEY (status_id),
    UNIQUE KEY uk_report_status_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE passenger (
    passenger_id   BIGINT NOT NULL AUTO_INCREMENT,
    name           VARCHAR(150) NULL,
    email          VARCHAR(255) NULL,
    phone_number   VARCHAR(30)  NULL,
    email_verified BIT(1)       NOT NULL DEFAULT 0,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    password_hash  VARCHAR(255) NULL,
    PRIMARY KEY (passenger_id),
    KEY idx_passenger_active (active),
    KEY idx_passenger_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    UNIQUE KEY uk_app_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role (
    role_id     BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES role (role_id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    transport_support_id BIGINT       NOT NULL,
    report_type_id       BIGINT       NOT NULL,
    passenger_id         BIGINT       NOT NULL,
    status_id            BIGINT       NOT NULL,
    PRIMARY KEY (report_id),
    UNIQUE KEY uk_report_uuid (uuid),
    UNIQUE KEY uk_report_reference (reference),
    CONSTRAINT fk_report_support
        FOREIGN KEY (transport_support_id) REFERENCES transport_support (transport_support_id),
    CONSTRAINT fk_report_type
        FOREIGN KEY (report_type_id) REFERENCES report_type (report_type_id),
    CONSTRAINT fk_report_passenger
        FOREIGN KEY (passenger_id) REFERENCES passenger (passenger_id),
    CONSTRAINT fk_report_status
        FOREIGN KEY (status_id) REFERENCES report_status (status_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
