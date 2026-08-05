-- Adds the tables this Spring Boot app owns, on top of the existing
-- asana_mirror schema (projects/tasks/custom_fields/etc. from the Node sync).
-- Run once: mysql -u root -p asana_mirror < db/init-app-tables.sql

USE asana_mirror;

CREATE TABLE IF NOT EXISTS app_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  role ENUM('SUPER_USER', 'ADMIN', 'USER') NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  last_login_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS sync_config (
  id BIGINT PRIMARY KEY,
  cron_expression VARCHAR(100) NOT NULL DEFAULT '0 0 3 * * *',
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  last_run_status VARCHAR(500),
  last_run_at VARCHAR(100)
);

-- Seed the first Super User so you can log in at all.
-- Password below is "ChangeMe123!" - CHANGE IT immediately after your first
-- login (there's no self-service reset flow built yet, only what Super
-- Users can do for other users via PUT /api/users/{id}).
INSERT INTO app_users (email, password_hash, full_name, role, enabled)
VALUES (
  'roberto.iniguez@accessparks.com',
  '$2b$10$LOC0xl/cIvY2FbCNdr/yN.jJEHoSlSRLT/Ia5cxkGPS2OJoKOny9W',
  'Roberto Iniguez',
  'SUPER_USER',
  TRUE
)
ON DUPLICATE KEY UPDATE email = email; -- no-op if already seeded
