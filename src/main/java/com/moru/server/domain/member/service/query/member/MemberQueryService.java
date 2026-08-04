package com.moru.server.domain.member.service.query.member;

import com.moru.server.domain.member.dto.MemberResponseDTO;

import java.time.LocalDate;

public interface MemberQueryService {

    MemberResponseDTO.ProfileResponse getProfile(Long memberId);

    MemberResponseDTO.StreakResponse getStreak(Long memberId);

    MemberResponseDTO.StreakResponse getStreak(Long memberId, LocalDate baseDate);
}
