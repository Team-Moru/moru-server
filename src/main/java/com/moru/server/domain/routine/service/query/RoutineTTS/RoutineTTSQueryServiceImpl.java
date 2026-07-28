package com.moru.server.domain.routine.service.query.RoutineTTS;

import com.moru.server.domain.routine.dto.RoutineTTSResponseDTO;
import com.moru.server.domain.routine.entity.Routine;
import com.moru.server.domain.routine.entity.RoutineGroup;
import com.moru.server.domain.routine.entity.RoutineTTS;
import com.moru.server.domain.routine.repository.RoutineGroupRepository;
import com.moru.server.domain.routine.repository.RoutineTTSRepository;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RoutineTTSQueryServiceImpl implements RoutineTTSQueryService {

    private final RoutineGroupRepository routineGroupRepository;
    private final RoutineTTSRepository routineTTSRepository;


    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;


    @Value("${aws.s3.presign-minutes}")
    private long presignMinutes;


    /**
     * 쿼리 <b>2회 고정</b>이다. 루틴이 몇 개든 늘지 않는다.
     * <ol>
     *   <li>그룹 + 루틴 (fetch join)</li>
     *   <li>그 루틴들의 스텝 전부 (IN 절)</li>
     * </ol>
     * 두 컬렉션을 한 쿼리로 fetch join 하면 {@code MultipleBagFetchException} 이 나므로 쪼갠 것이다.
     */
    @Override
    public List<RoutineTTSResponseDTO.RoutineTTSRes> getRoutineTTS(Long routineGroupId, Long memberId) {

        // isOwnedBy 는 member 프록시의 식별자만 읽으므로 추가 쿼리가 없다.
        RoutineGroup routineGroup = routineGroupRepository.findWithRoutinesById(routineGroupId)
                .filter(rg -> rg.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

        List<Routine> routines = routineGroup.getRoutines();
        if (routines.isEmpty()) {
            // 빈 IN 절은 SQL 문법 오류다. 2번 쿼리를 아예 보내지 않는다.
            return List.of();
        }

        Map<Long, List<RoutineTTS>> stepsByRoutineId = groupStepsByRoutineId(routines);

        return routines.stream()
                .map(routine -> RoutineTTSResponseDTO.RoutineTTSRes.builder()
                        .routineId(routine.getId())
                        .title(routine.getTitle())
                        .type(routine.getType())
                        .steps(stepsByRoutineId.getOrDefault(routine.getId(), List.of()).stream()
                                .map(this::toStep)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 스텝 전부를 한 쿼리로 가져와 routineId 로 묶는다.
     *
     * <p>쿼리가 이미 (routine_id, order_index) 순으로 정렬해 주고
     * {@code groupingBy} 의 기본 다운스트림인 {@code toList()} 가 그 순서를 그대로 유지하므로,
     * 여기서 다시 정렬할 필요가 없다.
     *
     * <p>{@code tts.getRoutine()} 은 LAZY 프록시지만 식별자만 읽으므로 초기화되지 않는다.
     */
    private Map<Long, List<RoutineTTS>> groupStepsByRoutineId(List<Routine> routines) {
        List<Long> routineIds = routines.stream()
                .map(Routine::getId)
                .collect(Collectors.toList());

        return routineTTSRepository.findAllByRoutineIdsOrdered(routineIds).stream()
                .collect(Collectors.groupingBy(tts -> tts.getRoutine().getId()));
    }

    private RoutineTTSResponseDTO.RoutineTTSRes.RoutineTTSStep toStep(RoutineTTS tts) {
        return RoutineTTSResponseDTO.RoutineTTSRes.RoutineTTSStep.builder()
                .stepId(tts.getId())
                .content(tts.getContent())
                .ttsIntro(tts.getTtsIntro())
                .ttsStatus(tts.getTtsStatus().name())
                .s3Url(generatePresignedUrl(tts.getS3Url()))
                .build();
    }

    private String generatePresignedUrl(String s3Key) {

        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }


        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();


        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .getObjectRequest(getObjectRequest)
                .build();


        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }


}
