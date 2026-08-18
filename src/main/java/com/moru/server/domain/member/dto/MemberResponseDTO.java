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
    @Schema(description = "목소리 타입 변경 응답")
    public record TtsUpdateResponse(
            @Schema(description = "멤버 고유번호", example = "1")
            Long memberId,

            @Schema(description = "목소리 타입 ID", example = "1")
            Long ttsId,

            @Schema(description = "TTS 목소리 코드", example = "MINSEO")
            String voiceCode,

            @Schema(description = "TTS 목소리 표시 이름", example = "민서")
            String displayName,

            @Schema(description = """
                    이번 변경으로 올라간 최신 목소리 선택 버전. 목소리를 바꿀 때마다 1씩 증가한다.
                    이 응답 시점에 회원의 모든 루틴 TTS 는 PENDING 으로 초기화되며,
                    재합성이 끝난 스텝은 TTS 조회 응답의 selectionVersion 이 이 값과 같아진다.
                    두 값이 같고 ttsStatus 가 COMPLETED 인 스텝만 새 목소리로 만들어진 음원이다.""",
                    example = "3")
            Long selectionVersion
    ) {
    }

    @Builder
    @Schema(description = "스트릭 조회 응답")
    public record StreakResponse(
            @Schema(description = "현재 연속 달성 일수", example = "5")
            Long currentStreak,

            @Schema(description = "최고 연속 달성 일수", example = "12")
            Long maxStreak,

            @Schema(description = "이번 주(월~일) 달성 여부. true=완료, false=미완료 또는 아직 안 지난 날짜")
            List<Boolean> weeklyStatus
    ) {
    }
}
