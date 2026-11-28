UPDATE tts
SET label = CASE name
        WHEN 'Orus' THEN '온기'
        WHEN 'Leda' THEN '산뜻'
        WHEN 'Kore' THEN '단정'
        WHEN 'Alnilam' THEN '여유'
    END,
    google_voice_name = CONCAT('ko-KR-Chirp3-HD-', name),
    is_pro_only = b'0',
    v2_enabled = b'1',
    selection_version = selection_version + 1
WHERE name IN ('Orus', 'Leda', 'Kore', 'Alnilam');

UPDATE tts
SET v2_enabled = b'0'
WHERE name NOT IN ('Orus', 'Leda', 'Kore', 'Alnilam');
