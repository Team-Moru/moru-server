package com.moru.server.domain.routine.repository;

import com.moru.server.domain.routine.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    @Query("SELECT MAX(r.orderIndex) FROM Routine r WHERE r.routineGroup.id = :routineGroupId")
    Optional<Integer> findMaxOrderIndexByRoutineGroupId(@Param("routineGroupId") Long routineGroupId);
}
