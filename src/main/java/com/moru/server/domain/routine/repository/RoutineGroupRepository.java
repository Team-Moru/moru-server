package com.moru.server.domain.routine.repository;

import com.moru.server.domain.routine.entity.RoutineGroup;
import com.moru.server.domain.routine.entity.enums.RoutineGoalType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoutineGroupRepository extends JpaRepository<RoutineGroup, Long> {

    @Query("""
        select distinct rg from RoutineGroup rg
        left join fetch rg.routines
        where rg.member.id = :memberId
        order by rg.createdAt desc
    """)
    List<RoutineGroup> findAllWithRoutinesByMemberId(@Param("memberId") Long memberId);

    /**
     * 그룹 + 루틴까지 한 쿼리로. 루틴 정렬은 RoutineGroup.routines 의
     * {@code @OrderBy("orderIndex ASC")} 가 fetch join SQL 에 붙어 처리한다
     * (위 findAllWithRoutinesByMemberId 와 동일한 방식).
     *
     * <p>여기서 ttsList 까지 같이 fetch join 하면 List 컬렉션 두 개를 동시에 가져오게 돼
     * {@code MultipleBagFetchException} 이 난다. 그래서 스텝은
     * {@link com.moru.server.domain.routine.repository.RoutineTTSRepository} 의
     * 별도 IN 쿼리로 한 번에 가져온다.
     */
    @Query("""
        select distinct rg from RoutineGroup rg
        left join fetch rg.routines
        where rg.id = :routineGroupId
    """)
    Optional<RoutineGroup> findWithRoutinesById(@Param("routineGroupId") Long routineGroupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rg FROM RoutineGroup rg WHERE rg.id = :id")
    Optional<RoutineGroup> findByIdForUpdate(@Param("id") Long id);

    Optional<RoutineGroup> findFirstByMember_IdAndIsActiveTrueOrderByCreatedAtDesc(Long memberId);

    List<RoutineGroup> findAllByMember_Id(Long memberId);

    @Query("""
    select distinct rg from RoutineGroup rg
    left join fetch rg.routines
    where rg.goalType = :goalType and rg.isTemplate = true
    order by rg.createdAt asc
""")
    List<RoutineGroup> findTemplatesWithRoutinesByGoalType(@Param("goalType") RoutineGoalType goalType);

    List<RoutineGroup> findByMember_IdAndIsActiveTrue(Long memberId);
}
