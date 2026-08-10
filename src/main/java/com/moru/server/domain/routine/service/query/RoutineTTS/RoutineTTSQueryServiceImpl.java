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
import org.springframework.beans.factory.ObjectProvider;
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
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoutineTTSQueryServiceImpl implements RoutineTTSQueryService {

    private final RoutineGroupRepository routineGroupRepository;
    private final RoutineTTSRepository routineTTSRepository;


    private final ObjectProvider<S3Presigner> s3PresignerProvider;

    @Value("${aws.s3.bucket:}")
    private String bucket;


    @Value("${aws.s3.presign-minutes:60}")
    private long presignMinutes;


    @Override
    public List<RoutineTTSResponseDTO.RoutineTTSRes> getRoutineTTS(Long routineGroupId, Long memberId) {

        RoutineGroup routineGroup = routineGroupRepository.findWithRoutinesById(routineGroupId)
                .filter(rg -> rg.isOwnedBy(memberId))
                .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_GROUP_NOT_FOUND));

        List<Routine> routines = routineGroup.getRoutines();
        if (routines.isEmpty()) {
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
                .selectionVersion(tts.getVoiceVersion())
                .build();
    }

    private String generatePresignedUrl(String s3Key) {

        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }

        S3Presigner s3Presigner = s3PresignerProvider.getIfAvailable();
        if (s3Presigner == null) {
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
