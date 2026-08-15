package com.moru.server.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moru.server.domain.member.entity.AppleOAuthCredential;

public interface AppleOAuthCredentialRepository extends JpaRepository<AppleOAuthCredential, Long> {

    Optional<AppleOAuthCredential> findByMember_Id(Long memberId);

    void deleteAllByMember_Id(Long memberId);
}
