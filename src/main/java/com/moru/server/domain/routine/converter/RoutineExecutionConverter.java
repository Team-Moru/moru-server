package com.moru.server.domain.routine.converter;

import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineExecution;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoutineExecutionConverter {

    public static RoutineExecution toEntity(
            RoutineExecutionRequestDTO.RoutineExecutionResultReq req,
            Routine routine
    ){
        return RoutineExecution.builder()
                .executedDate(req.executedDate())
                .routine(routine)
                .durationSecond(req.durationSecond())
                .isCompleted(req.isCompleted())
                .memberInput(req.memberInput())
                .aiResponse(req.aiResponse())
                .actualWakeTime(req.actualWakeTime())
                .build();
    }

    public static RoutineExecutionResponseDTO.RoutineExecutionResultRes toResponse(RoutineExecution execution){
        return RoutineExecutionResponseDTO.RoutineExecutionResultRes.builder()
                .executionId(execution.getId())
                .routineId(execution.getRoutine().getId())
                .executedDate(execution.getExecutedDate())
                .isCompleted(execution.getIsCompleted())
                .durationSecond(execution.getDurationSecond())
                .build();
    }

    public static List<RoutineExecutionResponseDTO.DailyExecution> toMonthlyResponse(
            YearMonth yearMonth,
            Map<LocalDate, List<RoutineExecution>> executionsByDate
    ){
        List<RoutineExecutionResponseDTO.DailyExecution> result = new ArrayList<>();

        int daysInMonth = yearMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            List<RoutineExecution> executions = executionsByDate.getOrDefault(date, List.of());

            int totalCount = executions.size();
            int completedCount = (int) executions.stream()
                    .filter(RoutineExecution::getIsCompleted)
                    .count();
            int completionRate = totalCount == 0 ? 0 : Math.round(completedCount * 100f / totalCount);

            result.add(RoutineExecutionResponseDTO.DailyExecution.builder()
                    .executedDate(date)
                    .completionRate(completionRate)
                    .build());
        }

        return result;
    }

    public static RoutineExecution toEntity(
            RoutineExecutionRequestDTO.AiResponseReq req,
            Routine routine,
            String aiResponse) {
        return RoutineExecution.builder()
                .executedDate(req.executedDate())
                .routine(routine)
                .durationSecond(req.durationSecond())
                .isCompleted(true)
                .memberInput(req.memberInput())
                .aiResponse(aiResponse)
                .actualWakeTime(req.actualWakeTime())
                .build();
    }

    public static RoutineExecutionResponseDTO.WeeklyReportResponse toWeeklyReportResponse(
            List<RoutineExecution> thisWeekExecutions,
            List<RoutineExecution> lastWeekExecutions,
            LocalDate monday,
            LocalDate today
    ){
        int daysPassed = (int) ChronoUnit.DAYS.between(monday, today) + 1;
        LocalDate lastMonday = monday.minusWeeks(1);

        int completionRate = averageRate(toExecutedDailyRates(thisWeekExecutions, monday, daysPassed));
        int lastWeekCompletionRate = averageRate(toExecutedDailyRates(lastWeekExecutions, lastMonday, 7));
        int completionRateDiff = completionRate - lastWeekCompletionRate;

        int totalDurationSecond = thisWeekExecutions.stream()
                .filter(execution -> !execution.getExecutedDate().isAfter(today))
                .mapToInt(execution -> execution.getDurationSecond() != null ? execution.getDurationSecond() : 0)
                .sum();

        return RoutineExecutionResponseDTO.WeeklyReportResponse.builder()
                .completionRate(completionRate)
                .completionRateDiff(completionRateDiff)
                .totalDurationSecond(totalDurationSecond)
                .weeklyCompletionRate(toDailyCompletionRates(thisWeekExecutions, monday, today))
                .routineStats(toRoutineStats(thisWeekExecutions, today))
                .build();
    }

    private static List<Integer> toExecutedDailyRates(List<RoutineExecution> executions, LocalDate startDate, int dayCount) {
        Map<LocalDate, List<RoutineExecution>> executionsByDate = executions.stream()
                .collect(Collectors.groupingBy(RoutineExecution::getExecutedDate));

        List<Integer> rates = new ArrayList<>();
        for (int i = 0; i < dayCount; i++) {
            LocalDate date = startDate.plusDays(i);
            List<RoutineExecution> dayExecutions = executionsByDate.get(date);
            if (dayExecutions != null && !dayExecutions.isEmpty()) {
                rates.add(calculateRate(dayExecutions));
            }
        }
        return rates;
    }

    private static int averageRate(List<Integer> rates) {
        return rates.isEmpty() ? 0 : Math.round((float) rates.stream().mapToInt(Integer::intValue).sum() / rates.size());
    }

    private static List<RoutineExecutionResponseDTO.DailyCompletionRate> toDailyCompletionRates(
            List<RoutineExecution> thisWeekExecutions, LocalDate monday, LocalDate today
    ){
        Map<LocalDate, List<RoutineExecution>> executionsByDate = thisWeekExecutions.stream()
                .collect(Collectors.groupingBy(RoutineExecution::getExecutedDate));

        List<RoutineExecutionResponseDTO.DailyCompletionRate> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            String day = date.getDayOfWeek().name().substring(0, 3);
            List<RoutineExecution> dayExecutions = executionsByDate.get(date);

            Integer completionRate = (date.isAfter(today) || dayExecutions == null || dayExecutions.isEmpty())
                    ? null
                    : calculateRate(dayExecutions);

            result.add(RoutineExecutionResponseDTO.DailyCompletionRate.builder()
                    .day(day)
                    .completionRate(completionRate)
                    .build());
        }
        return result;
    }

    private static List<RoutineExecutionResponseDTO.RoutineStat> toRoutineStats(
            List<RoutineExecution> thisWeekExecutions, LocalDate today
    ){
        Map<Long, List<RoutineExecution>> executionsByRoutineId = thisWeekExecutions.stream()
                .filter(execution -> !execution.getExecutedDate().isAfter(today))
                .collect(Collectors.groupingBy(execution -> execution.getRoutine().getId()));

        return executionsByRoutineId.entrySet().stream()
                .map(entry -> {
                    List<RoutineExecution> executions = entry.getValue();
                    Routine routine = executions.get(0).getRoutine();
                    long executedDaysCount = executions.stream()
                            .map(RoutineExecution::getExecutedDate)
                            .distinct()
                            .count();
                    int completedCount = (int) executions.stream()
                            .filter(RoutineExecution::getIsCompleted)
                            .count();
                    int completionRate = executedDaysCount == 0 ? 0 : Math.round(completedCount * 100f / executedDaysCount);

                    return RoutineExecutionResponseDTO.RoutineStat.builder()
                            .routineId(entry.getKey())
                            .title(routine.getTitle())
                            .completionRate(completionRate)
                            .build();
                })
                .sorted(Comparator.comparing(RoutineExecutionResponseDTO.RoutineStat::routineId))
                .toList();
    }

    private static int calculateRate(List<RoutineExecution> executions) {
        int totalCount = executions.size();
        int completedCount = (int) executions.stream()
                .filter(RoutineExecution::getIsCompleted)
                .count();
        return totalCount == 0 ? 0 : Math.round(completedCount * 100f / totalCount);
    }
}
