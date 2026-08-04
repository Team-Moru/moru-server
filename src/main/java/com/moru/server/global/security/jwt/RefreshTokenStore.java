package com.moru.server.global.security.jwt;

import java.time.Duration;

public interface RefreshTokenStore {

    void save(Long memberId, String tokenHash, Duration ttl);

    boolean rotate(Long memberId, String currentTokenHash, String nextTokenHash, Duration nextTokenTtl);

    boolean deleteIfMatches(Long memberId, String tokenHash);

    void deleteByMemberId(Long memberId);
}
