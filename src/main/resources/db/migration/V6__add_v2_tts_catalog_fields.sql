ALTER TABLE tts
    ADD COLUMN countdown_audio_prefix VARCHAR(500) NULL,
    ADD COLUMN v2_enabled BIT NOT NULL DEFAULT b'1';
