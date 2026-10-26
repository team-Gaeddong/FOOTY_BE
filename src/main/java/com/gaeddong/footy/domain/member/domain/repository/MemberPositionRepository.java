package com.gaeddong.footy.domain.member.domain.repository;

import com.gaeddong.footy.domain.member.domain.MemberPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberPositionRepository extends JpaRepository<MemberPosition, Long> {

    List<MemberPosition> findAllByMemberIdOrderByPriorityAsc(Long memberId);

    // 전체 교체 전에 바로 지운다. 파생 delete는 새 행 insert 뒤에 실행될 수 있다.
    @Modifying(flushAutomatically = true)
    @Query("delete from MemberPosition mp where mp.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
