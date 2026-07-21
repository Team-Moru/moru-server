package com.moru.server.domain.subscriptions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moru.server.domain.subscriptions.entity.Subscriptions;

public interface SubscriptionsRepository extends JpaRepository<Subscriptions, Long> {

    void deleteAllByMember_Id(Long memberId);
}
