package com.moru.server.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.entity.enums.LoginType;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginTypeAndOauthId(LoginType loginType, String oauthId);
}
