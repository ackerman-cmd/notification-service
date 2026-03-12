CREATE SCHEMA IF NOT EXISTS notification_service;

GRANT ALL PRIVILEGES ON SCHEMA notification_service TO admin;

ALTER USER admin SET search_path TO notification_service;