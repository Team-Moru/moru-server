package com.moru.server.domain.tts.service.command;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import com.google.api.gax.rpc.ApiException;

import com.moru.server.domain.routine.service.command.RoutineTTS.GoogleTtsClient;
import com.moru.server.domain.routine.service.command.RoutineTTS.S3Uploader;
import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.domain.tts.repository.TTSRepository;
import com.moru.server.global.config.V2UsageProperties;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.redis.UsageQuotaService;
import com.moru.server.global.redis.UsageQuotaService.Limits;
import com.moru.server.global.response.code.status.ErrorStatus;

@Service
@RequiredArgsConstructor
public class SingleTtsService {
    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final TTSRepository voices;
    private final ObjectProvider<GoogleTtsClient> google;
    private final ObjectProvider<S3Uploader> uploader;
    private final ObjectProvider<S3Client> s3;
    private final ObjectProvider<S3Presigner> presigner;
    private final StringRedisTemplate redis;
    private final UsageQuotaService quota;
    private final V2UsageProperties properties;

    @Value("${aws.s3.bucket:}")
    private String bucket;

    @Value("${aws.s3.presign-minutes:60}")
    private long presignMinutes;

    @Value("${google.tts.language-code:ko-KR}")
    private String languageCode;

    public String synthesize(UUID installationId, long ttsId, String text) {
        if (!StringUtils.hasText(text) || text.codePointCount(0, text.length()) > properties.getTtsMaxCharacters()
                || text.getBytes(StandardCharsets.UTF_8).length > 5000) {
            throw new BusinessException(ErrorStatus.BAD_REQUEST);
        }
        String voice = voices.findById(ttsId)
                .filter(TTS::isV2Enabled)
                .orElseThrow(() -> new BusinessException(ErrorStatus.TTS_NOT_FOUND))
                .getGoogleVoiceName();
        if (!StringUtils.hasText(voice)) {
            throw new BusinessException(ErrorStatus.TTS_VOICE_NAME_NOT_CONFIGURED);
        }
        S3Client storage = s3.getIfAvailable();
        S3Presigner signer = presigner.getIfAvailable();
        if (storage == null || signer == null || !StringUtils.hasText(bucket)) {
            throw new BusinessException(ErrorStatus.TTS_SYNTHESIS_UNAVAILABLE);
        }
        String hash = DigestUtils.sha256Hex("mp3-v1\n" + languageCode + "\n" + voice + "\n" + text);
        String objectKey = "tts/v2/" + hash + ".mp3";
        quota.consume("tts-request", installationId.toString(),
                new Limits(properties.getTtsRequestsPerMinute(), null),
                new Limits(properties.getTtsRequestsGlobalPerMinute(), null),
                ErrorStatus.TTS_REQUEST_RATE_LIMITED);
        try {
            if (!exists(storage, objectKey)) {
                createAudio(storage, objectKey, hash, installationId, text, voice);
            }
            return signer.presignGetObject(request -> request
                    .signatureDuration(Duration.ofMinutes(presignMinutes))
                    .getObjectRequest(object -> object.bucket(bucket).key(objectKey)))
                    .url().toExternalForm();
        } catch (SdkException | ApiException e) {
            throw new BusinessException(ErrorStatus.TTS_SYNTHESIS_UNAVAILABLE);
        }
    }

    private boolean exists(S3Client storage, String key) {
        return storage.listObjectsV2(request -> request.bucket(bucket).prefix(key).maxKeys(1))
                .contents().stream().anyMatch(object -> key.equals(object.key()));
    }

    private void createAudio(S3Client storage, String key, String hash, UUID installationId, String text, String voice) {
        GoogleTtsClient client = google.getIfAvailable();
        S3Uploader target = uploader.getIfAvailable();
        if (client == null || target == null) {
            throw new BusinessException(ErrorStatus.TTS_SYNTHESIS_UNAVAILABLE);
        }
        String lease = "v2:tts:lease:" + hash;
        String owner = UUID.randomUUID().toString();
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(lease, owner, LEASE_DURATION);
            if (acquired == null) {
                throw new BusinessException(ErrorStatus.QUOTA_UNAVAILABLE);
            }
            if (!acquired) {
                throw new BusinessException(ErrorStatus.TTS_SYNTHESIS_IN_PROGRESS);
            }
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorStatus.QUOTA_UNAVAILABLE);
        }
        try {
            if (exists(storage, key)) {
                return;
            }
            quota.consume("tts", installationId.toString(),
                    new Limits(properties.getTtsPerMinute(), properties.getTtsPerDay()),
                    new Limits(properties.getTtsGlobalPerMinute(), properties.getTtsGlobalPerDay()),
                    ErrorStatus.TTS_QUOTA_EXCEEDED);
            byte[] audio = client.synthesizeMp3(text, voice);
            if (audio.length == 0) {
                throw new BusinessException(ErrorStatus.TTS_SYNTHESIS_UNAVAILABLE);
            }
            target.upload(key, audio, "audio/mpeg");
        } finally {
            try {
                redis.execute(RELEASE, List.of(lease), owner);
            } catch (DataAccessException ignored) {
                // Redis expires the lease if release is unavailable.
            }
        }
    }
}
