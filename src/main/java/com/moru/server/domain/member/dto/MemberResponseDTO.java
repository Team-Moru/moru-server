package com.moru.server.domain.member.dto;

import com.moru.server.domain.member.entity.enums.LoginType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public record MemberResponseDTO() {

    @Builder
    @Schema(description = "내 프로필 조회 응답")
    public record ProfileResponse(
            @Schema(description = "멤버 고유번호", example = "1")
            Long memberId,

            @Schema(description = "닉네임", example = "모루유저")
            String nickname,

            @Schema(description = "소셜 로그인 방식", example = "KAKAO")
            LoginType loginType,

            @Schema(description = "프로필 이미지 키", nullable = true)
            String profileImageKey,

            @Schema(description = "목소리 타입 ID", example = "1")
            Long ttsId
    ) {
    }

    @Builder
    @Schema(description = "스트릭 조회 응답")
    public record StreakResponse(
            @Schema(description = "현재 연속 달성 일수", example = "5")
            Long currentStreak,

            @Schema(description = "최고 연속 달성 일수", example = "12")
            Long maxStreak,

            @Schema(description = "이번 주(월~일) 달성 여부. true=완료, false=미완료, null=아직 안 지난 날짜")
            List<Boolean> weeklyStatus
    ) {
    }
}
