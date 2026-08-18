package com.moru.server.domain.tts.entity;

import com.moru.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "tts")
public class TTS extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "google_voice_name", length = 100)
    private String googleVoiceName;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "preview_audio_key", length = 500)
    private String previewAudioKey;

    @Column(name = "done_audio_key", length = 500)
    private String doneAudioKey;

    @Column(name = "remind_audio_key", length = 500)
    private String remindAudioKey;

    @Column(name = "selection_version", nullable = false)
    @Builder.Default
    private Integer selectionVersion = 1;

    @Column(name = "is_pro_only", nullable = false)
    @Builder.Default
    private Boolean isProOnly = false;
}
