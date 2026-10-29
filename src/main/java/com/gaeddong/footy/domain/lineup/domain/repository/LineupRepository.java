package com.gaeddong.footy.domain.lineup.domain.repository;

import com.gaeddong.footy.domain.lineup.domain.Lineup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LineupRepository extends JpaRepository<Lineup, Long> {

    boolean existsByGameId(Long gameId);
}
