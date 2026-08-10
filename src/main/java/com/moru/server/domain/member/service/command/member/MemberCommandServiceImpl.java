package com.moru.server.domain.member.service.command.member;

import com.moru.server.domain.member.converter.MemberConverter;
import com.moru.server.domain.member.dto.MemberResponseDTO;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.routine.entity.RoutineTTS;
import com.moru.server.domain.routine.event.RoutineTtsVoiceChangedEvent;
import com.moru.server.domain.routine.repository.RoutineTTSRepository;
import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.domain.tts.repository.TTSRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService {

    private final MemberRepository memberRepository;
    private final TTSRepository ttsRepository;
    private final RoutineTTSRepository routineTTSRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${google.tts.enabled:false}")
    private boolean ttsEnabled;

     @Override
    public MemberResponseDTO.TtsUpdateResponse updateTts(Long memberId, Long ttsId) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));

        TTS tts = ttsRepository.findById(ttsId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.TTS_NOT_FOUND));

        member.updateVoiceType(tts);

        requestRegeneration(memberId, tts.getName(), member.getVoiceSelectionVersion());

        return MemberConverter.toTtsUpdateResponse(member);
    }

    private void requestRegeneration(Long memberId, String voiceName, Long voiceVersion) {
        if (!ttsEnabled) {
            // 처리할 워커가 없으므로 PENDING 으로 돌리면 영구히 멈춘다. 기존 음원을 그대로 둔다.
            log.info("[TTS] 기능이 비활성화되어 재합성을 건너뜀. memberId={}", memberId);
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "재합성 이벤트는 트랜잭션 안에서 발행해야 한다. memberId=" + memberId);
        }

        List<RoutineTTS> targets = routineTTSRepository.findAllByMemberId(memberId);
        for (RoutineTTS target : targets) {
            target.markPending();
            eventPublisher.publishEvent(
                    new RoutineTtsVoiceChangedEvent(target.getId(), voiceName, voiceVersion));
        }

        log.info("[TTS] 목소리 변경으로 재합성 요청. memberId={}, 건수={}, voiceVersion={}",
                memberId, targets.size(), voiceVersion);
    }
}
