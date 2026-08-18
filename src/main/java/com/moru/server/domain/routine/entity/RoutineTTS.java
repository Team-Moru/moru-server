package com.moru.server.domain.routine.entity;

import com.moru.server.domain.routine.entity.enums.TtsStatus;
import com.moru.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "routine_tts")
public class RoutineTTS extends BaseEntity {


    public static final int CONTENT_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;


    @Column(name = "content", nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    @Column(name = "tts_intro", length = CONTENT_MAX_LENGTH)
    private String ttsIntro;

    @Column(name = "tts_done", length = CONTENT_MAX_LENGTH)
    private String ttsDone;


    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "tts_status", nullable = false)
    @Builder.Default
    private TtsStatus ttsStatus = TtsStatus.PENDING;


    @Column(name = "s3_url")
    private String s3Url;

    @Column(name = "voice_version")
    private Long voiceVersion;

    public void markCompleted(String s3Url, String ttsIntro, String ttsDone, Long voiceVersion) {
        this.ttsStatus = TtsStatus.COMPLETED;
        this.s3Url = s3Url;
        this.ttsIntro = truncate(ttsIntro);
        this.ttsDone = truncate(ttsDone);
        this.voiceVersion = voiceVersion;
    }

    public void markFailed() {
        this.ttsStatus = TtsStatus.FAILED;
    }


    public void markPending() {
        this.ttsStatus = TtsStatus.PENDING;
    }

    public static String truncate(String text) {
        if (text == null || text.length() <= CONTENT_MAX_LENGTH) {
            return text;
        }
        int end = Character.isHighSurrogate(text.charAt(CONTENT_MAX_LENGTH - 1))
                ? CONTENT_MAX_LENGTH - 1
                : CONTENT_MAX_LENGTH;
        return text.substring(0, end);
    }
}
