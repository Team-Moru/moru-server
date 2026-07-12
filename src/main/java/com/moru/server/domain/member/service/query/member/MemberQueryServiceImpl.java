package com.moru.server.domain.member.service.query.member;

import com.moru.server.domain.member.converter.MemberConverter;
import com.moru.server.domain.member.dto.MemberResponseDTO;
import com.moru.server.domain.member.entity.Member;
import com.moru.server.domain.member.repository.MemberRepository;
import com.moru.server.domain.routine.repository.RoutineExecutionRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MemberRepository memberRepository;
    private final RoutineExecutionRepository routineExecutionRepository;

    @Override
    public MemberResponseDTO.ProfileResponse getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.MEMBER_NOT_FOUND));
        return MemberConverter.toProfileResponse(member);
    }

    @Override
    public MemberResponseDTO.StreakResponse getStreak(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorStatus.MEMBER_NOT_FOUND);
        }

        Set<LocalDate> completedDates = new HashSet<>(
                routineExecutionRepository.findCompletedDatesByMemberId(memberId)
        );

        LocalDate today = LocalDate.now(SERVICE_ZONE);
        Long currentStreak = calculateCurrentStreak(completedDates, today);
        Long maxStreak = calculateMaxStreak(completedDates);
        List<Boolean> weeklyStatus = calculateWeeklyStatus(completedDates, today);

        return MemberConverter.toStreakResponse(currentStreak, maxStreak, weeklyStatus);
    }

    private Long calculateCurrentStreak(Set<LocalDate> completedDates, LocalDate today) {
        LocalDate cursor = completedDates.contains(today) ? today : today.minusDays(1);

        long streak = 0;
        while (completedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private Long calculateMaxStreak(Set<LocalDate> completedDates) {
        long maxStreak = 0;
        long currentRun = 0;

        List<LocalDate> sortedDates = new ArrayList<>(completedDates);
        sortedDates.sort(LocalDate::compareTo);

        LocalDate previousDate = null;
        for (LocalDate date : sortedDates) {
            if (previousDate != null && date.equals(previousDate.plusDays(1))) {
                currentRun++;
            } else {
                currentRun = 1;
            }
            maxStreak = Math.max(maxStreak, currentRun);
            previousDate = date;
        }
        return maxStreak;
    }

    private List<Boolean> calculateWeeklyStatus(Set<LocalDate> completedDates, LocalDate today) {
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        List<Boolean> weeklyStatus = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            if (date.isAfter(today)) {
                weeklyStatus.add(false);
            } else {
                weeklyStatus.add(completedDates.contains(date));
            }
        }
        return weeklyStatus;
    }
}
