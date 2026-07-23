-- Optionnel : reset manuel avant premier démarrage
DROP DATABASE IF EXISTS transport_reporting;
CREATE DATABASE transport_reporting
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Puis : mvn spring-boot:run
-- Le fichier src/main/resources/schema.sql crée toutes les tables InnoDB + FK.
