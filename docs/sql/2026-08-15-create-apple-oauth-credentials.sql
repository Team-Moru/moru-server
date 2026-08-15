CREATE TABLE IF NOT EXISTS apple_oauth_credentials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    encrypted_refresh_token TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_apple_oauth_credentials_member UNIQUE (member_id),
    CONSTRAINT fk_apple_oauth_credentials_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);
