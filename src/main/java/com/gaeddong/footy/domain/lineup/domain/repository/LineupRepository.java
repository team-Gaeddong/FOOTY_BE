package com.gaeddong.footy.domain.lineup.domain.repository;

import com.gaeddong.footy.domain.lineup.domain.Lineup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LineupRepository extends JpaRepository<Lineup, Long> {

    boolean existsByGameId(Long gameId);

    Optional<Lineup> findFirstByGameIdOrderByVersionDesc(Long gameId);
}
