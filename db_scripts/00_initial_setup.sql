-- Set to configure a local db
DROP USER IF EXISTS 'palveo_user'@'localhost';
DROP DATABASE IF EXISTS palveo_db;

CREATE DATABASE palveo_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'palveo_user'@'localhost' IDENTIFIED BY 'P@lve0';

GRANT ALL PRIVILEGES ON palveo_db.* TO 'palveo_user'@'localhost';
FLUSH PRIVILEGES;

SELECT 'Script 00: Palveo database and user setup completed successfully.' AS Status;