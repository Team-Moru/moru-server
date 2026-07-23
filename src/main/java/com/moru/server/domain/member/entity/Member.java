package com.moru.server.domain.member.entity;

import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.entity.enums.Role;
import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_login_type_oauth_id",
                        columnNames = {"login_type", "oauth_id"}
                )
        }
)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oauth_id", nullable = false, length = 255)
    private String oauthId;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tts_id")
    private TTS voiceType;

    @Column(name = "profile_image_key", length = 500)
    private String profileImageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false)
    private LoginType loginType;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    public void updateVoiceType(TTS voiceType) {
        this.voiceType = voiceType;
    }
}
