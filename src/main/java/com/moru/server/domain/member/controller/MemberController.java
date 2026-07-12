package com.moru.server.domain.member.controller;

import com.moru.server.domain.member.dto.MemberRequestDTO;
import com.moru.server.domain.member.dto.MemberResponseDTO;
import com.moru.server.domain.member.service.command.member.MemberCommandService;
import com.moru.server.domain.member.service.query.member.MemberQueryService;
import com.moru.server.global.response.ApiResponse;
import com.moru.server.global.security.auth.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "멤버 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me/profile")
    public ApiResponse<MemberResponseDTO.ProfileResponse> getMyProfile(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(memberQueryService.getProfile(member.memberId()));
    }

    @Operation(summary = "내 목소리 타입 변경", description = "현재 로그인한 사용자의 TTS 목소리 타입을 변경합니다.")
    @PatchMapping("/me/tts")
    public ApiResponse<MemberResponseDTO.TtsUpdateResponse> updateMyTts(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody MemberRequestDTO.TtsUpdateRequest request
    ) {
        return ApiResponse.onSuccess(memberCommandService.updateTts(member.memberId(), request.ttsId()));
    }

    @Operation(summary = "내 스트릭 조회", description = "현재 로그인한 사용자의 루틴 연속 달성 기록을 조회합니다.")
    @GetMapping("/me/streak")
    public ApiResponse<MemberResponseDTO.StreakResponse> getMyStreak(
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.onSuccess(memberQueryService.getStreak(member.memberId()));
    }
}
