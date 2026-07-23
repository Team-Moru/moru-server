package com.moru.server.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moru.server.domain.member.entity.MemberTerm;

public interface MemberTermRepository extends JpaRepository<MemberTerm, Long> {

    void deleteAllByMember_Id(Long memberId);
}
