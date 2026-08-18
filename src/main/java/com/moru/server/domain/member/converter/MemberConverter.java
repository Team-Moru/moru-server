package com.moru.server.domain.member.converter;

import com.moru.server.domain.member.dto.MemberResponseDTO;
import com.moru.server.domain.member.entity.Member;

import java.util.List;

public class MemberConverter {

    public static MemberResponseDTO.ProfileResponse toProfileResponse(Member member) {
        return MemberResponseDTO.ProfileResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .loginType(member.getLoginType())
                .profileImageKey(member.getProfileImageKey())
                .ttsId(member.getVoiceType() != null ? member.getVoiceType().getId() : null)
                .build();
    }

    public static MemberResponseDTO.TtsUpdateResponse toTtsUpdateResponse(Member member) {
        return MemberResponseDTO.TtsUpdateResponse.builder()
                .memberId(member.getId())
                .ttsId(member.getVoiceType().getId())
                .voiceCode(member.getVoiceType().getName())
                .displayName(member.getVoiceType().getLabel())
                .selectionVersion(member.getVoiceSelectionVersion())
                .build();
    }

    public static MemberResponseDTO.StreakResponse toStreakResponse(
            Long currentStreak, Long maxStreak, List<Boolean> weeklyStatus
    ) {
        return MemberResponseDTO.StreakResponse.builder()
                .currentStreak(currentStreak)
                .maxStreak(maxStreak)
                .weeklyStatus(weeklyStatus)
                .build();
    }
}
