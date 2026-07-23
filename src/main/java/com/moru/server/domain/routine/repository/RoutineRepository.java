package com.moru.server.domain.routine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.moru.server.domain.routine.entity.Routine;


public interface RoutineRepository extends JpaRepository<Routine, Long> {

    @Query("SELECT MAX(r.orderIndex) FROM Routine r WHERE r.routineGroup.id = :routineGroupId")
    Optional<Integer> findMaxOrderIndexByRoutineGroupId(@Param("routineGroupId") Long routineGroupId);

    @Query("SELECT r FROM Routine r JOIN FETCH r.routineGroup WHERE r.id = :id")
    Optional<Routine> findWithGroupById(@Param("id") Long id);
}
