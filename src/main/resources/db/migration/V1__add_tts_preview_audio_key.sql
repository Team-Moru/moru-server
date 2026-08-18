SET @preview_audio_key_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tts'
      AND column_name = 'preview_audio_key'
);

SET @add_preview_audio_key_sql = IF(
    @preview_audio_key_exists = 0,
    'ALTER TABLE `tts` ADD COLUMN `preview_audio_key` VARCHAR(500) NULL',
    'SELECT 1'
);

PREPARE add_preview_audio_key_statement FROM @add_preview_audio_key_sql;
EXECUTE add_preview_audio_key_statement;
DEALLOCATE PREPARE add_preview_audio_key_statement;

UPDATE `tts`
SET `preview_audio_key` = CASE `name`
    WHEN 'Leda' THEN 'tts/previews/v1/leda.mp3'
    WHEN 'Kore' THEN 'tts/previews/v1/kore.mp3'
    WHEN 'Despina' THEN 'tts/previews/v1/despina.mp3'
    WHEN 'Charon' THEN 'tts/previews/v1/charon.mp3'
    WHEN 'Orus' THEN 'tts/previews/v1/orus.mp3'
    WHEN 'Alnilam' THEN 'tts/previews/v1/alnilam.mp3'
    ELSE `preview_audio_key`
END
WHERE (`preview_audio_key` IS NULL OR `preview_audio_key` = '')
  AND `name` IN ('Leda', 'Kore', 'Despina', 'Charon', 'Orus', 'Alnilam');
