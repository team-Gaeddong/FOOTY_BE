package com.gaeddong.footy.domain.lineup.domain.repository;

import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @EntityGraph(attributePaths = "gamePlayer")
    List<TeamMember> findAllByTeamIdOrderByIdAsc(Long teamId);

    @EntityGraph(attributePaths = "gamePlayer")
    List<TeamMember> findAllByTeamIdInOrderByIdAsc(Collection<Long> teamIds);
}
