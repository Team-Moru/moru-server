package com.moru.server.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moru.server.domain.member.dto.AuthResponseDTO;
import com.moru.server.domain.member.entity.enums.LoginType;
import com.moru.server.domain.member.service.MemberDeletionSnapshotReader.MemberDeletionSnapshot;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawalServiceTest {

    @Mock
    private MemberWithdrawalLock withdrawalLock;

    @Mock
    private MemberDeletionSnapshotReader snapshotReader;

    @Mock
    private AppleAccountRevocationService appleAccountRevocationService;

    @Mock
    private MemberAssetDeletionService memberAssetDeletionService;

    @Mock
    private MemberDeletionTransactionService deletionTransactionService;

    @Mock
    private MemberRedisDataCleaner redisDataCleaner;

    @InjectMocks
    private MemberWithdrawalService withdrawalService;

    @Test
    void deletesMemberDataInRequiredOrder() {
        MemberDeletionSnapshot snapshot = snapshot(LoginType.APPLE);
        when(withdrawalLock.tryAcquire(1L)).thenReturn(Optional.of("lock-token"));
        when(snapshotReader.findByMemberId(1L)).thenReturn(Optional.of(snapshot));
        when(withdrawalLock.tryAcquireSocialIdentity(LoginType.APPLE, "apple-oauth-id"))
                .thenReturn(Optional.of("social-lock-token"));

        AuthResponseDTO.WithdrawalResponse response = withdrawalService.withdraw(1L);

        assertThat(response.status()).isEqualTo(AuthResponseDTO.WithdrawalStatus.COMPLETED);
        InOrder order = inOrder(
                withdrawalLock,
                snapshotReader,
                appleAccountRevocationService,
                memberAssetDeletionService,
                deletionTransactionService,
                redisDataCleaner
        );
        order.verify(withdrawalLock).tryAcquire(1L);
        order.verify(snapshotReader).findByMemberId(1L);
        order.verify(withdrawalLock)
                .tryAcquireSocialIdentity(LoginType.APPLE, "apple-oauth-id");
        order.verify(appleAccountRevocationService).revokeIfRequired(1L, LoginType.APPLE);
        order.verify(memberAssetDeletionService)
                .deleteMemberAssets(1L, "profiles/profile.png", List.of("tts/audio.mp3"));
        order.verify(deletionTransactionService).deleteMemberData(1L);
        order.verify(redisDataCleaner).clearMemberData(1L);
        order.verify(withdrawalLock).releaseSocialIdentity(
                LoginType.APPLE,
                "apple-oauth-id",
                "social-lock-token"
        );
        order.verify(withdrawalLock).release(1L, "lock-token");
    }

    @Test
    void returnsCompletedWhenMemberWasAlreadyDeleted() {
        when(withdrawalLock.tryAcquire(1L)).thenReturn(Optional.of("lock-token"));
        when(snapshotReader.findByMemberId(1L)).thenReturn(Optional.empty());

        AuthResponseDTO.WithdrawalResponse response = withdrawalService.withdraw(1L);

        assertThat(response.status()).isEqualTo(AuthResponseDTO.WithdrawalStatus.COMPLETED);
        verify(redisDataCleaner).clearMemberData(1L);
        verifyNoInteractions(
                appleAccountRevocationService,
                memberAssetDeletionService,
                deletionTransactionService
        );
        verify(withdrawalLock).release(1L, "lock-token");
    }

    @Test
    void rejectsConcurrentWithdrawalRequest() {
        when(withdrawalLock.tryAcquire(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalService.withdraw(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode()).isEqualTo(ErrorStatus.WITHDRAWAL_IN_PROGRESS));

        verify(snapshotReader, never()).findByMemberId(1L);
    }

    @Test
    void doesNotStartDeletionWhileSocialLoginOwnsIdentityLock() {
        when(withdrawalLock.tryAcquire(1L)).thenReturn(Optional.of("lock-token"));
        when(snapshotReader.findByMemberId(1L)).thenReturn(Optional.of(snapshot(LoginType.APPLE)));
        when(withdrawalLock.tryAcquireSocialIdentity(LoginType.APPLE, "apple-oauth-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalService.withdraw(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode()).isEqualTo(ErrorStatus.WITHDRAWAL_IN_PROGRESS));

        verifyNoInteractions(
                appleAccountRevocationService,
                memberAssetDeletionService,
                deletionTransactionService,
                redisDataCleaner
        );
        verify(withdrawalLock).release(1L, "lock-token");
    }

    @Test
    void doesNotDeleteFilesOrDatabaseWhenAppleRevokeFails() {
        when(withdrawalLock.tryAcquire(1L)).thenReturn(Optional.of("lock-token"));
        when(snapshotReader.findByMemberId(1L)).thenReturn(Optional.of(snapshot(LoginType.APPLE)));
        when(withdrawalLock.tryAcquireSocialIdentity(LoginType.APPLE, "apple-oauth-id"))
                .thenReturn(Optional.of("social-lock-token"));
        doThrow(new BusinessException(ErrorStatus.APPLE_REVOKE_FAILED))
                .when(appleAccountRevocationService)
                .revokeIfRequired(1L, LoginType.APPLE);

        assertThatThrownBy(() -> withdrawalService.withdraw(1L))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(memberAssetDeletionService, deletionTransactionService, redisDataCleaner);
        verify(withdrawalLock).releaseSocialIdentity(
                LoginType.APPLE,
                "apple-oauth-id",
                "social-lock-token"
        );
        verify(withdrawalLock).release(1L, "lock-token");
    }

    @Test
    void doesNotDeleteDatabaseWhenS3DeletionFails() {
        MemberDeletionSnapshot snapshot = snapshot(LoginType.GOOGLE);
        when(withdrawalLock.tryAcquire(1L)).thenReturn(Optional.of("lock-token"));
        when(snapshotReader.findByMemberId(1L)).thenReturn(Optional.of(snapshot));
        when(withdrawalLock.tryAcquireSocialIdentity(LoginType.GOOGLE, "google-oauth-id"))
                .thenReturn(Optional.of("social-lock-token"));
        doThrow(new BusinessException(ErrorStatus.MEMBER_ASSET_DELETE_FAILED))
                .when(memberAssetDeletionService)
                .deleteMemberAssets(1L, "profiles/profile.png", List.of("tts/audio.mp3"));

        assertThatThrownBy(() -> withdrawalService.withdraw(1L))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(deletionTransactionService, redisDataCleaner);
        verify(withdrawalLock).releaseSocialIdentity(
                LoginType.GOOGLE,
                "google-oauth-id",
                "social-lock-token"
        );
        verify(withdrawalLock).release(1L, "lock-token");
    }

    private MemberDeletionSnapshot snapshot(LoginType loginType) {
        return new MemberDeletionSnapshot(
                1L,
                loginType,
                loginType.name().toLowerCase() + "-oauth-id",
                "profiles/profile.png",
                List.of("tts/audio.mp3")
        );
    }
}
