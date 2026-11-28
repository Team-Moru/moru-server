package com.moru.server.domain.tts.service.command;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import com.moru.server.domain.tts.entity.TTS;
import com.moru.server.domain.tts.repository.TTSRepository;
import com.moru.server.global.config.V2UsageProperties;
import com.moru.server.global.exception.QuotaExceededException;
import com.moru.server.global.redis.UsageQuotaService;
import com.moru.server.global.redis.UsageQuotaService.Limits;
import com.moru.server.global.response.code.status.ErrorStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SingleTtsServiceTest {
    @Test
    void existingAudioCountsOnlyTowardTheRequestLimit() throws Exception {
        var voices = mock(TTSRepository.class);
        var quota = mock(UsageQuotaService.class);
        var storage = mock(S3Client.class);
        var signer = mock(S3Presigner.class);
        var service = new SingleTtsService(voices, null, null, provider(storage), provider(signer),
                null, quota, new V2UsageProperties());
        ReflectionTestUtils.setField(service, "bucket", "private-assets");
        ReflectionTestUtils.setField(service, "presignMinutes", 60L);
        ReflectionTestUtils.setField(service, "languageCode", "ko-KR");
        when(voices.findById(1L)).thenReturn(Optional.of(
                TTS.builder().googleVoiceName("ko-KR-Chirp3-HD-Leda").build()));
        when(storage.listObjectsV2(org.mockito.ArgumentMatchers.<Consumer<ListObjectsV2Request.Builder>>any()))
                .thenAnswer(invocation -> {
                    Consumer<ListObjectsV2Request.Builder> configure = invocation.getArgument(0);
                    var builder = ListObjectsV2Request.builder();
                    configure.accept(builder);
                    return ListObjectsV2Response.builder()
                            .contents(S3Object.builder().key(builder.build().prefix()).build()).build();
                });
        var signed = mock(PresignedGetObjectRequest.class);
        when(signed.url()).thenReturn(URI.create("https://assets.example.com/audio.mp3").toURL());
        when(signer.presignGetObject(org.mockito.ArgumentMatchers.<Consumer<GetObjectPresignRequest.Builder>>any()))
                .thenReturn(signed);

        UUID installationId = UUID.randomUUID();
        assertThat(service.synthesize(installationId, 1L, "물 마시기"))
                .isEqualTo("https://assets.example.com/audio.mp3");
        verify(quota).consume("tts-request", installationId.toString(), new Limits(60, null),
                new Limits(180, null), ErrorStatus.TTS_REQUEST_RATE_LIMITED);
        verifyNoMoreInteractions(quota);
    }

    @Test
    void stopsBeforeCallingS3WhenRequestLimitIsExceeded() {
        var voices = mock(TTSRepository.class);
        var quota = mock(UsageQuotaService.class);
        var storage = mock(S3Client.class);
        var signer = mock(S3Presigner.class);
        var service = new SingleTtsService(voices, null, null, provider(storage), provider(signer),
                null, quota, new V2UsageProperties());
        ReflectionTestUtils.setField(service, "bucket", "private-assets");
        ReflectionTestUtils.setField(service, "languageCode", "ko-KR");
        when(voices.findById(1L)).thenReturn(Optional.of(
                TTS.builder().googleVoiceName("ko-KR-Chirp3-HD-Leda").build()));
        UUID installationId = UUID.randomUUID();
        var rateLimit = ErrorStatus.TTS_REQUEST_RATE_LIMITED;
        doThrow(new QuotaExceededException(rateLimit, 15)).when(quota).consume(
                "tts-request", installationId.toString(), new Limits(60, null), new Limits(180, null), rateLimit);

        assertThatThrownBy(() -> service.synthesize(installationId, 1L, "물 마시기"))
                .isInstanceOfSatisfying(QuotaExceededException.class,
                        e -> assertThat(e.getError()).isEqualTo(rateLimit));
        verifyNoInteractions(storage, signer);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T instance) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(instance);
        return provider;
    }
}
