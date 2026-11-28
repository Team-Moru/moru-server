UPDATE tts
SET countdown_audio_prefix = CONCAT('tts/common/v2/countdown/', LOWER(name)),
    selection_version = selection_version + 1
WHERE v2_enabled = b'1'
  AND name IN ('Orus', 'Leda', 'Kore', 'Alnilam');
