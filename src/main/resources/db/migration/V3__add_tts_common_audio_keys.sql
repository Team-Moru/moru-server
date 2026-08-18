SET @done_audio_key_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tts'
      AND column_name = 'done_audio_key'
);

SET @add_done_audio_key_sql = IF(
    @done_audio_key_exists = 0,
    'ALTER TABLE `tts` ADD COLUMN `done_audio_key` VARCHAR(500) NULL',
    'SELECT 1'
);

PREPARE add_done_audio_key_statement FROM @add_done_audio_key_sql;
EXECUTE add_done_audio_key_statement;
DEALLOCATE PREPARE add_done_audio_key_statement;

SET @remind_audio_key_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tts'
      AND column_name = 'remind_audio_key'
);

SET @add_remind_audio_key_sql = IF(
    @remind_audio_key_exists = 0,
    'ALTER TABLE `tts` ADD COLUMN `remind_audio_key` VARCHAR(500) NULL',
    'SELECT 1'
);

PREPARE add_remind_audio_key_statement FROM @add_remind_audio_key_sql;
EXECUTE add_remind_audio_key_statement;
DEALLOCATE PREPARE add_remind_audio_key_statement;

SET @selection_version_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tts'
      AND column_name = 'selection_version'
);

SET @add_selection_version_sql = IF(
    @selection_version_exists = 0,
    'ALTER TABLE `tts` ADD COLUMN `selection_version` INT NOT NULL DEFAULT 1',
    'SELECT 1'
);

PREPARE add_selection_version_statement FROM @add_selection_version_sql;
EXECUTE add_selection_version_statement;
DEALLOCATE PREPARE add_selection_version_statement;

UPDATE `tts`
SET
    `done_audio_key` = CASE
        WHEN `done_audio_key` IS NULL OR `done_audio_key` = '' THEN CASE `name`
            WHEN 'Leda' THEN 'tts/common/v1/leda-done.mp3'
            WHEN 'Kore' THEN 'tts/common/v1/kore-done.mp3'
            WHEN 'Despina' THEN 'tts/common/v1/despina-done.mp3'
            WHEN 'Charon' THEN 'tts/common/v1/charon-done.mp3'
            WHEN 'Orus' THEN 'tts/common/v1/orus-done.mp3'
            WHEN 'Alnilam' THEN 'tts/common/v1/alnilam-done.mp3'
            ELSE `done_audio_key`
        END
        ELSE `done_audio_key`
    END,
    `remind_audio_key` = CASE
        WHEN `remind_audio_key` IS NULL OR `remind_audio_key` = '' THEN CASE `name`
            WHEN 'Leda' THEN 'tts/common/v1/leda-remind.mp3'
            WHEN 'Kore' THEN 'tts/common/v1/kore-remind.mp3'
            WHEN 'Despina' THEN 'tts/common/v1/despina-remind.mp3'
            WHEN 'Charon' THEN 'tts/common/v1/charon-remind.mp3'
            WHEN 'Orus' THEN 'tts/common/v1/orus-remind.mp3'
            WHEN 'Alnilam' THEN 'tts/common/v1/alnilam-remind.mp3'
            ELSE `remind_audio_key`
        END
        ELSE `remind_audio_key`
    END
WHERE `name` IN ('Leda', 'Kore', 'Despina', 'Charon', 'Orus', 'Alnilam');
