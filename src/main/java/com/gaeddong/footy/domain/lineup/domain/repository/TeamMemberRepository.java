package com.gaeddong.footy.domain.lineup.domain.repository;

import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
}
