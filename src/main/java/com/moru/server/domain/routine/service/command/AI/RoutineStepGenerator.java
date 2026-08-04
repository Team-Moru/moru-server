package com.moru.server.domain.routine.service.command.AI;

import java.util.List;

public interface RoutineStepGenerator {

    List<List<String>> generateForTimer(List<String> timerRoutineTitles);

}
