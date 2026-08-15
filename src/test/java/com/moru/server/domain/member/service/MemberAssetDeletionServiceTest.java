package com.moru.server.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

class MemberAssetDeletionServiceTest {

    private static final String BUCKET = "moru-test-assets";

    @Test
    void deletesLegacyKeysAndMemberPrefixKeys() {
        S3Client s3Client = mock(S3Client.class);
        ObjectProvider<S3Client> provider = providerOf(s3Client);
        MemberAssetDeletionService service = new MemberAssetDeletionService(provider, BUCKET, true);
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(
                        listResponse("tts/members/1/new.mp3"),
                        ListObjectsV2Response.builder().isTruncated(false).build()
                );
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());

        service.deleteMemberAssets(
                1L,
                "profiles/legacy-profile.png",
                List.of("tts/legacy-tts.mp3")
        );

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(captor.capture());
        List<String> deletedKeys = captor.getValue().delete().objects().stream()
                .map(object -> object.key())
                .toList();
        assertThat(deletedKeys).containsExactlyInAnyOrder(
                "profiles/legacy-profile.png",
                "tts/legacy-tts.mp3",
                "tts/members/1/new.mp3"
        );
    }

    @Test
    void skipsS3WhenFeatureIsDisabled() {
        S3Client s3Client = mock(S3Client.class);
        MemberAssetDeletionService service = new MemberAssetDeletionService(providerOf(s3Client), BUCKET, false);

        service.deleteMemberAssets(1L, "profiles/profile.png", List.of("tts/audio.mp3"));

        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    void rejectsObjectKeyOutsideManagedPrefixes() {
        MemberAssetDeletionService service = new MemberAssetDeletionService(
                providerOf(mock(S3Client.class)),
                BUCKET,
                true
        );

        assertThatThrownBy(() -> service.deleteMemberAssets(1L, "other/private.txt", List.of()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getBaseCode())
                                .isEqualTo(ErrorStatus.MEMBER_ASSET_DELETE_FAILED));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<S3Client> providerOf(S3Client s3Client) {
        ObjectProvider<S3Client> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(s3Client);
        return provider;
    }

    private ListObjectsV2Response listResponse(String key) {
        return ListObjectsV2Response.builder()
                .contents(S3Object.builder().key(key).build())
                .isTruncated(false)
                .build();
    }
}
