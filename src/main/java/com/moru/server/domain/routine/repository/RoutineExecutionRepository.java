package com.moru.server.domain.routine.repository;

import com.moru.server.domain.routine.entity.RoutineExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoutineExecutionRepository extends JpaRepository<RoutineExecution, Long> {

    Optional<RoutineExecution> findByRoutine_IdAndExecutedDate(Long routineId, LocalDate executedDate);

    @Query("""
        select distinct re.executedDate from RoutineExecution re
        where re.routine.routineGroup.member.id = :memberId
        and re.isCompleted = true
        order by re.executedDate
    """)
    List<LocalDate> findCompletedDatesByMemberId(@Param("memberId") Long memberId);

    @Query("""
        select count(re) from RoutineExecution re
        where re.routine.routineGroup.id = :routineGroupId
        and re.executedDate = :executedDate
        and re.isCompleted = true
    """)
    int countCompletedByRoutineGroupIdAndExecutedDate(@Param("routineGroupId") Long routineGroupId, @Param("executedDate") LocalDate executedDate);

    @Query("""
        select re from RoutineExecution re
        where re.routine.routineGroup.id = :routineGroupId
        and re.executedDate = :executedDate
    """)
    List<RoutineExecution> findByRoutineGroupIdAndExecutedDate(@Param("routineGroupId") Long routineGroupId, @Param("executedDate") LocalDate executedDate);

    @Query("""
        select re from RoutineExecution re
        where re.routine.routineGroup.member.id = :memberId
        and re.executedDate between :startDate and :endDate
        order by re.executedDate
    """)
    List<RoutineExecution> findAllByMemberIdAndExecutedDateBetween(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        select re from RoutineExecution re
        join fetch re.routine r
        where re.routine.routineGroup.member.id = :memberId
        and re.executedDate between :startDate and :endDate
        order by re.executedDate
    """)
    List<RoutineExecution> findAllWithRoutineByMemberIdAndExecutedDateBetween(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        select count(re) > 0 from RoutineExecution re
        where re.routine.routineGroup.member.id = :memberId
    """)
    boolean existsByMemberId(@Param("memberId") Long memberId);

    @Query("""
        select re from RoutineExecution re
        where re.routine.routineGroup.member.id = :memberId
        and re.executedDate between :startDate and :endDate
        and re.actualWakeTime is not null
        order by re.executedDate
    """)
    List<RoutineExecution> findAllByMemberIdAndExecutedDateBetweenAndActualWakeTimeIsNotNull(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
