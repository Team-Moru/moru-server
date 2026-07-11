package com.moru.server.domain.member.dto;

import com.moru.server.domain.member.entity.enums.LoginType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

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
            String displayName
    ) {
    }
}
