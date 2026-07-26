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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;


    @Column(name = "content", nullable = false, length = 255)
    private String content;


    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "tts_status", nullable = false)
    @Builder.Default
    private TtsStatus ttsStatus = TtsStatus.PENDING;


    @Column(name = "s3_url")
    private String s3Url;

    public void markCompleted(String s3Url) {
        this.ttsStatus = TtsStatus.COMPLETED;
        this.s3Url = s3Url;
    }

    public void markFailed() {
        this.ttsStatus = TtsStatus.FAILED;
    }
}
