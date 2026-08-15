package com.moru.server.domain.member.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.service.MemberDeletionSnapshotReader.MemberDeletionSnapshot;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {

    private static final String WITHDRAWAL_COMPLETE_MESSAGE = "회원 탈퇴가 완료되었습니다.";

    private final MemberWithdrawalLock withdrawalLock;
    private final MemberDeletionSnapshotReader snapshotReader;
    private final AppleAccountRevocationService appleAccountRevocationService;
    private final MemberAssetDeletionService memberAssetDeletionService;
    private final MemberDeletionTransactionService deletionTransactionService;
    private final MemberRedisDataCleaner redisDataCleaner;

    public AuthResponseDTO.WithdrawalResponse withdraw(Long memberId) {
        String lockToken = withdrawalLock.tryAcquire(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.WITHDRAWAL_IN_PROGRESS));

        try {
            Optional<MemberDeletionSnapshot> snapshot = snapshotReader.findByMemberId(memberId);
            if (snapshot.isEmpty()) {
                redisDataCleaner.clearMemberData(memberId);
                return completedResponse();
            }

            MemberDeletionSnapshot memberData = snapshot.get();
            appleAccountRevocationService.revokeIfRequired(memberId, memberData.loginType());
            memberAssetDeletionService.deleteMemberAssets(
                    memberId,
                    memberData.profileImageKey(),
                    memberData.ttsKeys()
            );
            deletionTransactionService.deleteMemberData(memberId);
            redisDataCleaner.clearMemberData(memberId);
            return completedResponse();
        } finally {
            withdrawalLock.release(memberId, lockToken);
        }
    }

    private AuthResponseDTO.WithdrawalResponse completedResponse() {
        return AuthResponseDTO.WithdrawalResponse.builder()
                .status(AuthResponseDTO.WithdrawalStatus.COMPLETED)
                .message(WITHDRAWAL_COMPLETE_MESSAGE)
                .build();
    }
}
