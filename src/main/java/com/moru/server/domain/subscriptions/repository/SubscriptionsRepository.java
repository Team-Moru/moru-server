package com.moru.server.domain.subscriptions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moru.server.domain.subscriptions.entity.Subscriptions;

import java.util.Optional;

public interface SubscriptionsRepository extends JpaRepository<Subscriptions, Long> {

    void deleteAllByMember_Id(Long memberId);

    Optional<Subscriptions> findByMember_Id(Long memberId);
}
