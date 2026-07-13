package com.moru.server.domain.tts.repository;

import com.moru.server.domain.tts.entity.TTS;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TTSRepository extends JpaRepository<TTS, Long> {
}
