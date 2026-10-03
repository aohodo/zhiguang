CREATE TABLE IF NOT EXISTS content_actions (
    id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id BIGINT UNSIGNED NOT NULL,
    action_type VARCHAR(16) NOT NULL,
    action_status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_action_user_target (user_id, entity_type, entity_id, action_type),
    KEY idx_action_user_list (user_id, action_type, action_status, updated_at, id),
    KEY idx_action_target_count (entity_type, entity_id, action_type, action_status),
    CONSTRAINT fk_content_actions_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
