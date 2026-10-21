package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
