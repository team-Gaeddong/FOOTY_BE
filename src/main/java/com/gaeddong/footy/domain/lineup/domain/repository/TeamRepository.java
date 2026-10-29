package com.gaeddong.footy.domain.lineup.domain.repository;

import com.gaeddong.footy.domain.lineup.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
