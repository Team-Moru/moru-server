package com.moru.server.domain.member.entity;

import com.moru.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(
        name = "member_term",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_term",
                        columnNames = {"member_id", "term_id"}
                )
        }
)
public class MemberTerm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_agreed", nullable = false)
    private Boolean isAgreed;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
