package com.moru.server.domain.member.converter;

import com.moru.server.domain.member.dto.MemberResponseDTO;
import com.moru.server.domain.member.entity.Member;

public class MemberConverter {

    public static MemberResponseDTO.ProfileResponse toProfileResponse(Member member) {
        return MemberResponseDTO.ProfileResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .loginType(member.getLoginType().name())
                .profileImageKey(member.getProfileImageKey())
                .ttsId(member.getVoiceType() != null ? member.getVoiceType().getId() : null)
                .build();
    }
}
