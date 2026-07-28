package com.moru.server.domain.routine.service.command.RoutineTTS;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3 업로드 <b>전용</b> 컴포넌트. 주어진 key 로 바이트를 올리고, 그 key 를 돌려준다.
 * 합성·DB·상태변경을 모른다(단일 책임). AWS S3 SDK 를 감싼다.
 *
 * <p><b>URL 이 아니라 key 를 저장·반환하는 이유:</b> 이 버킷은 비공개이고
 * 재생 URL 은 조회 시점에 S3Presigner 로 만료시간 붙여 즉석 발급한다
 * (S3Config 의 s3Presigner, presign-minutes). 미리 만든 presigned URL 을
 * DB 에 넣으면 60분 뒤 죽는다. 그래서 안정적인 key 만 보관한다.
 */
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public String upload(String key, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType) // 브라우저가 오디오로 인식하게 한다
                        .build(),
                RequestBody.fromBytes(content));

        return key;
    }
}
