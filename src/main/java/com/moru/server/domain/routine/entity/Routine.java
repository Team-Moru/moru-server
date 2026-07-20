package com.moru.server.domain.routine.entity;

import java.util.ArrayList;
import java.util.List;

import com.moru.server.domain.routine.entity.enums.RoutineType;
import com.moru.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "routine")
public class Routine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RoutineType type;

    @Column(name = "timer")
    private Integer timer;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_group_id", nullable = false)
    private RoutineGroup routineGroup;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoutineExecution> executions = new ArrayList<>();

    public void updateOrder(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}