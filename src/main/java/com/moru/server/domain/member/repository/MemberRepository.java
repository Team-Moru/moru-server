package com.moru.server.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moru.server.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByOauthId(String oauthId);

    @Modifying
    @Query(value = "DELETE FROM refresh_tokens WHERE member_id = :memberId", nativeQuery = true)
    void deleteRefreshTokensByMemberId(@Param("memberId") Long memberId);
}
