package com.moru.server.domain.member.service.query.member;

import com.moru.server.domain.member.dto.MemberResponseDTO;

public interface MemberQueryService {

    MemberResponseDTO.ProfileResponse getProfile(Long memberId);

    MemberResponseDTO.StreakResponse getStreak(Long memberId);
}
