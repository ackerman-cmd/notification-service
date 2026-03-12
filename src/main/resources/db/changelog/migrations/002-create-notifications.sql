-- liquibase formatted sql

-- changeset ackerman:002-create-notifications
CREATE TABLE notification_service.notifications
(
    id                UUID         PRIMARY KEY,
    user_id           UUID         NOT NULL,
    channel           VARCHAR(20)  NOT NULL,
    type              VARCHAR(50)  NOT NULL,
    recipient         VARCHAR(255) NOT NULL,
    subject           VARCHAR(500),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error             TEXT,
    retry_count       INT          NOT NULL DEFAULT 0,
    deduplication_key VARCHAR(255),
    payload           TEXT         NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    sent_at           TIMESTAMP
);

-- changeset notification-service:002-create-notifications-indexes
CREATE INDEX idx_notifications_user_id ON notification_service.notifications (user_id);
CREATE INDEX idx_notifications_status ON notification_service.notifications (status);
CREATE INDEX idx_notifications_deduplication ON notification_service.notifications (deduplication_key);
CREATE INDEX idx_notifications_created_at ON notification_service.notifications (created_at);
