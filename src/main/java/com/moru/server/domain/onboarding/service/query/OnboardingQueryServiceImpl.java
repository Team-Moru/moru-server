package com.moru.server.domain.onboarding.service.query;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.onboarding.dto.OnboardingResponseDTO;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingQueryServiceImpl implements OnboardingQueryService {

    private final MemberRepository memberRepository;

    @Override
    public OnboardingResponseDTO.StatusResponse getOnboardingStatus(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));

        return new OnboardingResponseDTO.StatusResponse(member.getOnboardingCompleted());
    }
}
