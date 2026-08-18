CREATE TABLE IF NOT EXISTS `tts` (
    `is_pro_only` BIT(1) NOT NULL,
    `selection_version` INT NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `updated_at` DATETIME(6) NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `description` VARCHAR(100) NULL,
    `google_voice_name` VARCHAR(100) NULL,
    `label` VARCHAR(100) NOT NULL,
    `done_audio_key` VARCHAR(500) NULL,
    `preview_audio_key` VARCHAR(500) NULL,
    `remind_audio_key` VARCHAR(500) NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `members` (
    `onboarding_completed` BIT(1) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `tts_id` BIGINT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `nickname` VARCHAR(50) NULL,
    `profile_image_key` VARCHAR(500) NULL,
    `oauth_id` VARCHAR(255) NOT NULL,
    `login_type` ENUM('APPLE', 'GOOGLE', 'KAKAO', 'NAVER') NOT NULL,
    `role` ENUM('ADMIN', 'MEMBER') NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_login_type_oauth_id` (`login_type`, `oauth_id`),
    KEY `fk_members_tts` (`tts_id`),
    CONSTRAINT `fk_members_tts` FOREIGN KEY (`tts_id`) REFERENCES `tts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `terms` (
    `is_required` BIT(1) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `updated_at` DATETIME(6) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `content` TEXT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `routine_group` (
    `alarm_time` TIME NULL,
    `is_active` BIT(1) NOT NULL,
    `is_template` BIT(1) NOT NULL,
    `weather_notification_enabled` BIT(1) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `alarm_days` VARCHAR(100) NULL,
    `description` VARCHAR(100) NULL,
    `title` VARCHAR(100) NOT NULL,
    `goal_type` ENUM('HABIT', 'HEALTH', 'STABILITY', 'VITALITY') NULL,
    PRIMARY KEY (`id`),
    KEY `fk_routine_group_member` (`member_id`),
    CONSTRAINT `fk_routine_group_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `routine` (
    `order_index` INT NOT NULL,
    `timer` INT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `routine_group_id` BIGINT NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `type` ENUM('CHECK', 'INPUT', 'TIMER') NOT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_routine_routine_group` (`routine_group_id`),
    CONSTRAINT `fk_routine_routine_group` FOREIGN KEY (`routine_group_id`) REFERENCES `routine_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `routine_execution` (
    `actual_wake_time` TIME NULL,
    `duration_second` INT NULL,
    `executed_date` DATE NOT NULL,
    `is_completed` BIT(1) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `routine_id` BIGINT NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `ai_response` VARCHAR(500) NULL,
    `member_input` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    KEY `fk_routine_execution_routine` (`routine_id`),
    CONSTRAINT `fk_routine_execution_routine` FOREIGN KEY (`routine_id`) REFERENCES `routine` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `routine_tts` (
    `order_index` INT NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `routine_id` BIGINT NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `content` VARCHAR(255) NOT NULL,
    `s3_url` VARCHAR(255) NULL,
    `tts_done` VARCHAR(255) NULL,
    `tts_intro` VARCHAR(255) NULL,
    `tts_status` ENUM('COMPLETED', 'FAILED', 'PENDING') NOT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_routine_tts_routine` (`routine_id`),
    CONSTRAINT `fk_routine_tts_routine` FOREIGN KEY (`routine_id`) REFERENCES `routine` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `member_term` (
    `is_agreed` BIT(1) NOT NULL,
    `agreed_at` DATETIME(6) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `term_id` BIGINT NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_term` (`member_id`, `term_id`),
    KEY `fk_member_term_term` (`term_id`),
    CONSTRAINT `fk_member_term_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`),
    CONSTRAINT `fk_member_term_term` FOREIGN KEY (`term_id`) REFERENCES `terms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `subscriptions` (
    `created_at` DATETIME(6) NOT NULL,
    `expires_at` DATETIME(6) NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `started_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `store_transaction_id` VARCHAR(255) NULL,
    `plan` ENUM('FREE', 'PRO') NOT NULL,
    `store` ENUM('APP_STORE', 'GOOGLE_PLAYSTORE') NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subscriptions_member` (`member_id`),
    CONSTRAINT `fk_subscriptions_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `apple_oauth_credentials` (
    `created_at` DATETIME(6) NOT NULL,
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `encrypted_refresh_token` TEXT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_apple_oauth_credentials_member` (`member_id`),
    CONSTRAINT `fk_apple_oauth_credentials_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
