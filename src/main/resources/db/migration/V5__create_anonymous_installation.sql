CREATE TABLE anonymous_installation (
    installation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    anonymous_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    refresh_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (installation_id),
    UNIQUE KEY uk_anonymous_installation_id (anonymous_id)
);
