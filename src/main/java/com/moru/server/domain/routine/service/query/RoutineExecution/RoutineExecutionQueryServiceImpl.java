package com.moru.server.domain.routine.service.query.RoutineExecution;

import com.moru.server.domain.routine.converter.RoutineExecutionConverter;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.entity.RoutineExecution;
import com.moru.server.domain.routine.repository.RoutineExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineExecutionQueryServiceImpl implements RoutineExecutionQueryService {

    private final RoutineExecutionRepository routineExecutionRepository;

    @Override
    public List<RoutineExecutionResponseDTO.DailyExecution> getMonthlyExecutions(Long memberId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<RoutineExecution> executions = routineExecutionRepository
                .findAllByMemberIdAndExecutedDateBetween(memberId, startDate, endDate);

        Map<LocalDate, List<RoutineExecution>> executionsByDate = executions.stream()
                .collect(Collectors.groupingBy(RoutineExecution::getExecutedDate));

        return RoutineExecutionConverter.toMonthlyResponse(yearMonth, executionsByDate);
    }
}
