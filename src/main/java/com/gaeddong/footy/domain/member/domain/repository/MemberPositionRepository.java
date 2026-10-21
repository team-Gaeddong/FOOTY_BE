package com.gaeddong.footy.domain.member.domain.repository;

import com.gaeddong.footy.domain.member.domain.MemberPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberPositionRepository extends JpaRepository<MemberPosition, Long> {

    List<MemberPosition> findAllByMemberIdOrderByPriorityAsc(Long memberId);
}
