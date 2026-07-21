package com.moru.server.domain.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moru.server.domain.member.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByMember_IdAndTokenHash(Long memberId, String tokenHash);

    List<RefreshToken> findAllByMember_IdAndRevokedAtIsNull(Long memberId);
}
