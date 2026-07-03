package com.moru.server.domain.routine.entity;

import com.moru.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "routine_execution")
public class RoutineExecution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "executed_date", nullable = false)
    private LocalDate executedDate;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "duration_second")
    private Integer durationSecond;

    @Column(name = "member_input", length = 500)
    private String memberInput;

    @Column(name = "ai_response", length = 500)
    private String aiResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    public void complete(Integer durationSecond) {
        this.isCompleted = true;
        this.durationSecond = durationSecond;
    }

    public void fail(Integer durationSecond) {
        this.isCompleted = false;
        this.durationSecond = durationSecond;
    }

    public void recordInput(String memberInput) {
        this.memberInput = memberInput;
    }

    public void recordAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }
}
