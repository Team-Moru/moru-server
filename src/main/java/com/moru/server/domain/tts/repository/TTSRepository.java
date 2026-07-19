package com.moru.server.domain.tts.repository;

import com.moru.server.domain.tts.entity.TTS;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TTSRepository extends JpaRepository<TTS, Long> {
    List<TTS> findAllByOrderByIdAsc();
}
