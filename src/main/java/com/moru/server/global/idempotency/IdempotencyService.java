package com.moru.server.global.idempotency;

import java.util.function.Supplier;

public interface IdempotencyService {

    /**
     * Idempotency-Key가 있으면 dedup+응답캐싱을 적용해 operation을 실행하고,
     * 없으면(하위 호환) operation을 그냥 실행한다.
     *
     * @param action           API 구분자 (예: "create-routine-group")
     * @param memberId         요청자 ID (다른 유저의 키와 충돌 방지)
     * @param idempotencyKey   클라이언트가 보낸 Idempotency-Key 헤더값 (nullable)
     * @param responseType     캐싱된 응답을 역직렬화할 타입
     * @param operation        실제 처리 로직
     */
    <T> T execute(
            String action,
            Long memberId,
            String idempotencyKey,
            Class<T> responseType,
            Supplier<T> operation
    );
}