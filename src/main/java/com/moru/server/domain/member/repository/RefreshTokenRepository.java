package com.moru.server.domain.member.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moru.server.domain.member.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT refreshToken
            FROM RefreshToken refreshToken
            WHERE refreshToken.member.id = :memberId
              AND refreshToken.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByMemberIdAndTokenHashForUpdate(
            @Param("memberId") Long memberId,
            @Param("tokenHash") String tokenHash
    );

    List<RefreshToken> findAllByMember_IdAndRevokedAtIsNull(Long memberId);

    void deleteAllByMember_Id(Long memberId);
}
