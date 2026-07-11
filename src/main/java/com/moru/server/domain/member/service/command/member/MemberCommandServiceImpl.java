package com.moru.server.domain.member.service.command.member;

import com.moru.server.domain.member.converter.MemberConverter;
import com.moru.server.domain.member.dto.MemberResponseDTO;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.domain.tts.repository.TTSRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService {

    private final MemberRepository memberRepository;
    private final TTSRepository ttsRepository;

    @Override
    public MemberResponseDTO.TtsUpdateResponse updateTts(Long memberId, Long ttsId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));

        TTS tts = ttsRepository.findById(ttsId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.TTS_NOT_FOUND));

        member.updateVoiceType(tts);

        return MemberConverter.toTtsUpdateResponse(member);
    }
}
