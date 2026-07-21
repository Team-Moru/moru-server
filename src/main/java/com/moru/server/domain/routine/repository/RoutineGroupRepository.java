package com.moru.server.domain.routine.repository;

import com.moru.server.domain.routine.entity.RoutineGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoutineGroupRepository extends JpaRepository<RoutineGroup, Long> {

    @Query("""
        select distinct rg from RoutineGroup rg
        left join fetch rg.routines
        where rg.member.id = :memberId
        order by rg.createdAt desc
    """)
    List<RoutineGroup> findAllWithRoutinesByMemberId(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rg FROM RoutineGroup rg WHERE rg.id = :id")
    Optional<RoutineGroup> findByIdForUpdate(@Param("id") Long id);

    Optional<RoutineGroup> findFirstByMember_IdAndIsActiveTrueOrderByCreatedAtDesc(Long memberId);

    List<RoutineGroup> findAllByMember_Id(Long memberId);
}
