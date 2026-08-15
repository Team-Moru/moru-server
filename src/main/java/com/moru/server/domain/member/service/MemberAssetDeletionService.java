package com.moru.server.domain.member.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

@Service
public class MemberAssetDeletionService {

    private static final int DELETE_BATCH_SIZE = 1_000;
    private static final List<String> ALLOWED_PREFIXES = List.of("tts/", "profiles/");

    private final ObjectProvider<S3Client> s3ClientProvider;
    private final String bucket;
    private final boolean s3Enabled;

    public MemberAssetDeletionService(
            ObjectProvider<S3Client> s3ClientProvider,
            @Value("${aws.s3.bucket:}") String bucket,
            @Value("${aws.s3.enabled:false}") boolean s3Enabled
    ) {
        this.s3ClientProvider = s3ClientProvider;
        this.bucket = bucket;
        this.s3Enabled = s3Enabled;
    }

    public void deleteMemberAssets(Long memberId, String profileImageKey, Collection<String> ttsKeys) {
        if (!s3Enabled) {
            if (StringUtils.hasText(profileImageKey) || (ttsKeys != null && !ttsKeys.isEmpty())) {
                throw new BusinessException(ErrorStatus.MEMBER_ASSET_DELETE_FAILED);
            }
            return;
        }

        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null || !StringUtils.hasText(bucket)) {
            throw new BusinessException(ErrorStatus.MEMBER_ASSET_DELETE_FAILED);
        }

        try {
            Set<String> keys = new LinkedHashSet<>();
            addAllowedKey(keys, profileImageKey);
            if (ttsKeys != null) {
                ttsKeys.forEach(key -> addAllowedKey(keys, key));
            }
            keys.addAll(listKeys(s3Client, memberPrefix("tts", memberId)));
            keys.addAll(listKeys(s3Client, memberPrefix("profiles", memberId)));
            deleteKeys(s3Client, keys);
        } catch (BusinessException exception) {
            throw exception;
        } catch (SdkException exception) {
            throw new BusinessException(ErrorStatus.MEMBER_ASSET_DELETE_FAILED);
        }
    }

    private List<String> listKeys(S3Client s3Client, String prefix) {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .continuationToken(continuationToken)
                    .build());
            response.contents().forEach(object -> keys.add(object.key()));
            continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
        } while (continuationToken != null);

        return keys;
    }

    private void deleteKeys(S3Client s3Client, Collection<String> keys) {
        List<String> keyList = new ArrayList<>(keys);

        for (int start = 0; start < keyList.size(); start += DELETE_BATCH_SIZE) {
            int end = Math.min(start + DELETE_BATCH_SIZE, keyList.size());
            List<ObjectIdentifier> objects = keyList.subList(start, end).stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();

            DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(objects).quiet(true).build())
                    .build());
            if (response != null && response.hasErrors() && !response.errors().isEmpty()) {
                throw new BusinessException(ErrorStatus.MEMBER_ASSET_DELETE_FAILED);
            }
        }
    }

    private void addAllowedKey(Set<String> keys, String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        if (ALLOWED_PREFIXES.stream().noneMatch(key::startsWith)) {
            throw new BusinessException(ErrorStatus.MEMBER_ASSET_DELETE_FAILED);
        }
        keys.add(key);
    }

    private String memberPrefix(String assetType, Long memberId) {
        return assetType + "/members/" + memberId + "/";
    }
}
