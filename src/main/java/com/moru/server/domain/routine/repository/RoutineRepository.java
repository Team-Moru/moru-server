package com.moru.server.domain.routine.repository;

import com.moru.server.domain.routine.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineRepository extends JpaRepository<Routine, Long> {
}