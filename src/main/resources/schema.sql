-- Schema explicite InnoDB (évite errno 150 avec Hibernate ddl-auto)
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reply;
DROP TABLE IF EXISTS report_history;
DROP TABLE IF EXISTS attachment;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS transport_support;
DROP TABLE IF EXISTS report_type;
DROP TABLE IF EXISTS passenger;
DROP TABLE IF EXISTS report_status;
DROP TABLE IF EXISTS support_type;
DROP TABLE IF EXISTS app_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE support_type (
    support_type_id BIGINT NOT NULL AUTO_INCREMENT,
    code            VARCHAR(50)  NOT NULL,
    label           VARCHAR(150) NOT NULL,
    PRIMARY KEY (support_type_id),
    UNIQUE KEY uk_support_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report_type (
    report_type_id BIGINT NOT NULL AUTO_INCREMENT,
    code           VARCHAR(50)  NOT NULL,
    label          VARCHAR(150) NOT NULL,
    description    VARCHAR(500) NULL,
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
    PRIMARY KEY (passenger_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_user (
    user_id       BIGINT NOT NULL AUTO_INCREMENT,
    uuid          VARCHAR(36)  NOT NULL,
    username      VARCHAR(100) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_app_user_uuid (uuid),
    UNIQUE KEY uk_app_user_username (username),
    UNIQUE KEY uk_app_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE transport_support (
    transport_support_id BIGINT NOT NULL AUTO_INCREMENT,
    uuid                 VARCHAR(36)  NOT NULL,
    reference            VARCHAR(50)  NOT NULL,
    label                VARCHAR(150) NOT NULL,
    qr_code_url          VARCHAR(500) NULL,
    qr_date_creation     DATETIME(6)  NULL,
    qr_date_impression   DATETIME(6)  NULL,
    qr_status            VARCHAR(30)  NULL,
    support_status       VARCHAR(30)  NOT NULL,
    support_type_id      BIGINT       NOT NULL,
    PRIMARY KEY (transport_support_id),
    UNIQUE KEY uk_transport_support_uuid (uuid),
    UNIQUE KEY uk_transport_support_reference (reference),
    CONSTRAINT fk_transport_support_type
        FOREIGN KEY (support_type_id) REFERENCES support_type (support_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE report (
    report_id            BIGINT NOT NULL AUTO_INCREMENT,
    uuid                 VARCHAR(36)  NOT NULL,
    reference            VARCHAR(40)  NOT NULL,
    creation_date        DATETIME(6)  NOT NULL,
    description          TEXT         NOT NULL,
    priority             VARCHAR(30)  NULL,
    closure_date         DATETIME(6)  NULL,
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
    reply_id   BIGINT NOT NULL AUTO_INCREMENT,
    uuid       VARCHAR(36) NOT NULL,
    message    TEXT        NOT NULL,
    reply_date DATETIME(6) NOT NULL,
    email_sent BIT(1)      NOT NULL DEFAULT 0,
    report_id  BIGINT      NOT NULL,
    user_id    BIGINT      NULL,
    PRIMARY KEY (reply_id),
    UNIQUE KEY uk_reply_uuid (uuid),
    CONSTRAINT fk_reply_report
        FOREIGN KEY (report_id) REFERENCES report (report_id),
    CONSTRAINT fk_reply_user
        FOREIGN KEY (user_id) REFERENCES app_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
