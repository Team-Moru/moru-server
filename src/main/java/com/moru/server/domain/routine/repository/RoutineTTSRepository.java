package com.moru.server.domain.routine.repository;

import com.moru.server.domain.routine.entity.RoutineTTS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoutineTTSRepository extends JpaRepository<RoutineTTS, Long> {

    /**
     * 여러 루틴의 스텝을 <b>한 번에</b> 가져온다. 루틴마다 ttsList 를 LAZY 로 건드리면
     * 루틴 수만큼 쿼리가 나가므로(N+1) 이 쿼리로 대체한다.
     *
     * <p><b>{@code order by} 가 필수다.</b> Routine.ttsList 의
     * {@code @OrderBy("orderIndex ASC")} 는 <b>컬렉션을 초기화할 때만</b> 적용된다.
     * 이렇게 별도 쿼리로 가져와 서비스에서 직접 그룹핑하면 그 어노테이션이 관여하지 않으므로,
     * 여기서 정렬하지 않으면 스텝 순서가 보장되지 않는다.
     *
     * <p>{@code t.routine.id} 는 routine_tts 의 FK 컬럼을 그대로 읽으므로 조인이 생기지 않는다.
     *
     * @param routineIds 비어 있으면 안 된다(빈 IN 절은 SQL 문법 오류). 호출부에서 먼저 거른다.
     */
    @Query("""
        select t from RoutineTTS t
        where t.routine.id in :routineIds
        order by t.routine.id asc, t.orderIndex asc
    """)
    List<RoutineTTS> findAllByRoutineIdsOrdered(@Param("routineIds") List<Long> routineIds);

    @Query("""
        select t from RoutineTTS t
        join t.routine r
        join r.routineGroup g
        where g.member.id = :memberId
        order by t.id asc
    """)
    List<RoutineTTS> findAllByMemberId(@Param("memberId") Long memberId);

    @Query("""
        select g.member.voiceSelectionVersion from RoutineTTS t
        join t.routine r
        join r.routineGroup g
        where t.id = :routineTtsId
    """)
    Optional<Long> findCurrentVoiceVersion(@Param("routineTtsId") Long routineTtsId);
}
