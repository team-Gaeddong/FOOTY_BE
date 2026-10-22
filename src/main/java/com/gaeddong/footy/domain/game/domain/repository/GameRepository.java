package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {

    @EntityGraph(attributePaths = "host")
    Page<Game> findByDeletedFalse(Pageable pageable);
}
