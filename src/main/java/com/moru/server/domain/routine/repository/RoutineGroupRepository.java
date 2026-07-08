package com.moru.server.domain.routine.repository;

import com.moru.server.domain.routine.entity.RoutineGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoutineGroupRepository extends JpaRepository<RoutineGroup, Long> {

    @Query("""
        select distinct rg from RoutineGroup rg
        join fetch rg.routines
        where rg.member.id = :memberId
        order by rg.createdAt desc
    """)
    List<RoutineGroup> findAllWithRoutinesByMemberId(@Param("memberId") Long memberId);
}