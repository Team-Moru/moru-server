package com.moru.server.domain.member.service.command.member;

import com.moru.server.domain.member.dto.MemberResponseDTO;

public interface MemberCommandService {

    MemberResponseDTO.TtsUpdateResponse updateTts(Long memberId, Long ttsId);
}
