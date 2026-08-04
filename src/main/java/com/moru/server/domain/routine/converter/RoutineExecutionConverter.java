package com.moru.server.domain.routine.converter;

import com.moru.server.domain.routine.dto.RoutineExecutionRequestDTO;
import com.moru.server.domain.routine.dto.RoutineExecutionResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineExecution;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.YearMonth;
import java.util.stream.Collectors;

public class RoutineExecutionConverter {

    private static final int MINUTES_PER_DAY = 24 * 60;

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
            Map<LocalDate, List<RoutineExecution>> executionsByDate,
            LocalDate today
    ){
        List<RoutineExecutionResponseDTO.DailyExecution> result = new ArrayList<>();

        LocalDate lastDay = yearMonth.atEndOfMonth().isAfter(today) ? today : yearMonth.atEndOfMonth();
        if (lastDay.isBefore(yearMonth.atDay(1))) {
            return result;
        }

        for (LocalDate date = yearMonth.atDay(1); !date.isAfter(lastDay); date = date.plusDays(1)) {
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

        int completionRate = averageRate(toExecutedDailyRates(thisWeekExecutions, monday, daysPassed, false));
        int lastWeekCompletionRate = averageRate(toExecutedDailyRates(lastWeekExecutions, lastMonday, 7, true));
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

    public static RoutineExecutionResponseDTO.DailyResponse toDailyResponse(
            LocalDate executedDate, Long currentStreak,
            List<RoutineExecution> list) {

        Integer totalDurationSecond = list.stream()
                .mapToInt(re -> re.getDurationSecond() != null ? re.getDurationSecond() : 0)
                .sum();

        LocalTime actualWakeTime = list.stream()
                .map(RoutineExecution::getActualWakeTime)
                .filter(Objects::nonNull)
                .min(LocalTime::compareTo)
                .orElse(null);

        List<RoutineExecutionResponseDTO.RoutineResult> routineResults = list.stream()
                .map(re -> new RoutineExecutionResponseDTO.RoutineResult(
                        re.getRoutine().getId(),
                        re.getRoutine().getTitle(),
                        re.getRoutine().getType(),
                        re.getDurationSecond(),
                        re.getIsCompleted(),
                        re.getMemberInput()
                ))
                .toList();

        return RoutineExecutionResponseDTO.DailyResponse.builder()
                .executedDate(executedDate)
                .completionRate(calculateRate(list))
                .totalDurationSecond(totalDurationSecond)
                .actualWakeTime(actualWakeTime)
                .currentStreak(currentStreak)
                .routines(routineResults)
                .build();
    }

    private static List<Integer> toExecutedDailyRates(
            List<RoutineExecution> executions, LocalDate startDate, int dayCount, boolean includeMissingAsZero
    ) {
        Map<LocalDate, List<RoutineExecution>> executionsByDate = executions.stream()
                .collect(Collectors.groupingBy(RoutineExecution::getExecutedDate));

        List<Integer> rates = new ArrayList<>();
        for (int i = 0; i < dayCount; i++) {
            LocalDate date = startDate.plusDays(i);
            List<RoutineExecution> dayExecutions = executionsByDate.get(date);
            if (dayExecutions != null && !dayExecutions.isEmpty()) {
                rates.add(calculateRate(dayExecutions));
            } else if (includeMissingAsZero) {
                rates.add(0);
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

    public static RoutineExecutionResponseDTO.WakePatternResponse toWakePatternResponse(
            List<RoutineExecution> thisWeekExecutions,
            List<RoutineExecution> lastWeekExecutions
    ){
        List<Integer> thisWeekWakeMinutes = toDailyWakeMinutes(thisWeekExecutions);
        List<Integer> lastWeekWakeMinutes = toDailyWakeMinutes(lastWeekExecutions);

        Double thisWeekAvg = circularMeanMinutes(thisWeekWakeMinutes);
        Double lastWeekAvg = circularMeanMinutes(lastWeekWakeMinutes);

        String avgWakeTime = thisWeekAvg != null ? toTimeString(thisWeekAvg) : null;

        Integer wakeTimeDiffMin = (thisWeekAvg != null && lastWeekAvg != null)
                ? circularDiffMinutes(lastWeekAvg, thisWeekAvg)
                : null;

        Integer regularityScore = null;
        Integer stdDevMin = null;
        if (thisWeekWakeMinutes.size() >= 3) {
            double stdDev = circularStdDevMinutes(thisWeekWakeMinutes);
            stdDevMin = (int) Math.round(Math.min(stdDev, MINUTES_PER_DAY / 2.0));
            regularityScore = Math.max(0, (int) Math.round(100 * (1 - stdDev / 120)));
        }

        return RoutineExecutionResponseDTO.WakePatternResponse.builder()
                .avgWakeTime(avgWakeTime)
                .wakeTimeDiffMin(wakeTimeDiffMin)
                .regularityScore(regularityScore)
                .stdDevMin(stdDevMin)
                .regularityLabel(toRegularityLabel(regularityScore))
                .build();
    }

    private static List<Integer> toDailyWakeMinutes(List<RoutineExecution> executions) {
        Map<LocalDate, RoutineExecution> latestByDate = executions.stream()
                .collect(Collectors.toMap(
                        RoutineExecution::getExecutedDate,
                        execution -> execution,
                        (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b
                ));

        return latestByDate.values().stream()
                .map(execution -> toMinutesOfDay(execution.getActualWakeTime()))
                .toList();
    }

    private static int toMinutesOfDay(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    /**
     * 자정을 넘나드는 기상 시각(예: 23:55, 00:05)을 하나의 원형 분포로 보고
     * 단위원 위 각도의 평균(벡터 평균)으로 대표 시각을 구한다. 단순 산술 평균을 쓰면
     * 23:55/00:05가 정오 근처(1435분, 5분의 평균인 720분)로 계산되는 오류가 생긴다.
     */
    private static Double circularMeanMinutes(List<Integer> minutes) {
        if (minutes.isEmpty()) {
            return null;
        }

        double sumSin = 0;
        double sumCos = 0;
        for (int minute : minutes) {
            double angle = 2 * Math.PI * minute / MINUTES_PER_DAY;
            sumSin += Math.sin(angle);
            sumCos += Math.cos(angle);
        }

        double meanAngle = Math.atan2(sumSin / minutes.size(), sumCos / minutes.size());
        double meanMinutes = meanAngle / (2 * Math.PI) * MINUTES_PER_DAY;
        return meanMinutes < 0 ? meanMinutes + MINUTES_PER_DAY : meanMinutes;
    }

    /** 원형 표준편차(Mardia). 자정 경계에서도 흩어진 정도가 연속적으로 계산된다. */
    private static double circularStdDevMinutes(List<Integer> minutes) {
        double sumSin = 0;
        double sumCos = 0;
        for (int minute : minutes) {
            double angle = 2 * Math.PI * minute / MINUTES_PER_DAY;
            sumSin += Math.sin(angle);
            sumCos += Math.cos(angle);
        }

        double meanSin = sumSin / minutes.size();
        double meanCos = sumCos / minutes.size();
        double resultantLength = Math.min(1.0, Math.max(1e-6, Math.sqrt(meanSin * meanSin + meanCos * meanCos)));

        double stdDevRadians = Math.sqrt(-2 * Math.log(resultantLength));
        return stdDevRadians / (2 * Math.PI) * MINUTES_PER_DAY;
    }

    /** to - from을 자정 넘어가는 최단 경로 기준으로 구한다. 예: 23:50 -> 00:10 = +20분 */
    private static int circularDiffMinutes(double from, double to) {
        double diff = ((to - from) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY;
        if (diff > MINUTES_PER_DAY / 2.0) {
            diff -= MINUTES_PER_DAY;
        }
        return (int) Math.round(diff);
    }

    private static String toTimeString(double avgMinutes) {
        int totalMinutes = (int) Math.round(avgMinutes) % MINUTES_PER_DAY;
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    private static String toRegularityLabel(Integer score) {
        if (score == null) {
            return "측정 중";
        }
        if (score >= 90) {
            return "매우 규칙적이에요";
        }
        if (score >= 70) {
            return "꽤 규칙적이에요";
        }
        if (score >= 50) {
            return "보통이에요";
        }
        return "불규칙해요";
    }

    private static int calculateRate(List<RoutineExecution> executions) {
        int totalCount = executions.size();
        int completedCount = (int) executions.stream()
                .filter(RoutineExecution::getIsCompleted)
                .count();
        return totalCount == 0 ? 0 : Math.round(completedCount * 100f / totalCount);
    }
}
