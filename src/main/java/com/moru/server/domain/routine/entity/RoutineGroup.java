package com.moru.server.domain.routine.entity;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "routine_group")
public class RoutineGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "alarm_days", length = 100)
    private String alarmDays;

    @Column(name = "alarm_time")
    private LocalTime alarmTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public void updateInfo(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void updateAlarm(String alarmDays, LocalTime alarmTime) {
        this.alarmDays = alarmDays;
        this.alarmTime = alarmTime;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
