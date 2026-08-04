package com.moru.server.domain.routine.service.query.RoutineTTS;

import com.moru.server.domain.routine.dto.RoutineTTSResponseDTO;

import java.util.List;

public interface RoutineTTSQueryService {


    List<RoutineTTSResponseDTO.RoutineTTSRes> getRoutineTTS(Long routineGroupId,Long memberId);

}
