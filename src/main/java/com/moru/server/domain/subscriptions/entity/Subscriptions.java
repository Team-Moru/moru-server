package com.moru.server.domain.subscriptions.entity;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.subscriptions.entity.enums.Plan;
import com.moru.server.domain.subscriptions.entity.enums.Store;
import com.moru.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "subscriptions")
public class Subscriptions extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    @Builder.Default
    private Plan plan = Plan.FREE;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "store", length = 20)
    private Store store;

    @Column(name = "store_transaction_id", length = 255)
    private String storeTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public boolean isActive() {
        return this.plan == Plan.PRO
                && (this.expiresAt == null || this.expiresAt.isAfter(LocalDateTime.now()));
    }

    public void renew(LocalDateTime expiresAt, Store store, String storeTransactionId) {
        this.plan = Plan.PRO;
        this.expiresAt = expiresAt;
        this.store = store;
        this.storeTransactionId = storeTransactionId;
    }
}
