package com.moru.server.domain.routine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.moru.server.domain.routine.entity.Routine;


@Repository
public interface RoutineRepository extends JpaRepository<Routine, Long> {

    @Query("SELECT MAX(r.orderIndex) FROM Routine r WHERE r.routineGroup.id = :routineGroupId")
    Optional<Integer> findMaxOrderIndexByRoutineGroupId(@Param("routineGroupId") Long routineGroupId);
}
