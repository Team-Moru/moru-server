package com.moru.server.domain.onboarding.service.command;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.onboarding.dto.OnboardingRequestDTO;
import com.moru.server.domain.onboarding.dto.OnboardingResponseDTO;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingCommandServiceImpl implements OnboardingCommandService {

    private final MemberRepository memberRepository;
    private final RoutineGroupRepository routineGroupRepository;

    @Override
    public OnboardingResponseDTO.StatusResponse completeOnboarding(
            Long memberId,
            OnboardingRequestDTO.CompleteRequest request
    ) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));

        if (Boolean.TRUE.equals(member.getOnboardingCompleted())) {
            return new OnboardingResponseDTO.StatusResponse(true);
        }

        boolean ownsRoutineGroup = routineGroupRepository
                .existsByIdAndMember_Id(request.routineGroupId(), memberId);
        if (!ownsRoutineGroup) {
            throw new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND);
        }

        member.completeOnboarding();
        return new OnboardingResponseDTO.StatusResponse(true);
    }
}
