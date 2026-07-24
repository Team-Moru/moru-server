package com.moru.server.domain.routine.service.query.RoutineExecution;

import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.member.service.query.member.MemberQueryService;
import com.moru.server.domain.routine.converter.RoutineExecutionConverter;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.entity.RoutineExecution;
import com.moru.server.domain.routine.repository.RoutineExecutionRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.moru.server.domain.routine.converter.RoutineExecutionConverter.toDailyResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineExecutionQueryServiceImpl implements RoutineExecutionQueryService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final RoutineExecutionRepository routineExecutionRepository;
    private final MemberRepository memberRepository;
    private final MemberQueryService memberQueryService;
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

    @Override
    public RoutineExecutionResponseDTO.WeeklyReportResponse getWeeklyReport(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        LocalDate today = LocalDate.now(SERVICE_ZONE);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        LocalDate lastMonday = monday.minusWeeks(1);
        LocalDate lastSunday = sunday.minusWeeks(1);

        List<RoutineExecution> thisWeekExecutions = routineExecutionRepository
                .findAllWithRoutineByMemberIdAndExecutedDateBetween(memberId, monday, sunday);
        List<RoutineExecution> lastWeekExecutions = routineExecutionRepository
                .findAllByMemberIdAndExecutedDateBetween(memberId, lastMonday, lastSunday);

        return RoutineExecutionConverter.toWeeklyReportResponse(thisWeekExecutions, lastWeekExecutions, monday, today);
    }

    @Override
    public RoutineExecutionResponseDTO.DailyResponse getDailyResponse(Long memberId, LocalDate executedDate) {

        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        List<RoutineExecution> list =routineExecutionRepository.findAllWithRoutineByMemberIdAndExecutedDateBetween(memberId,executedDate,executedDate);

        Long currentStreak = memberQueryService.getStreak(memberId).currentStreak();


        return toDailyResponse(executedDate,currentStreak,list);

    }
}
