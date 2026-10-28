package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    List<GamePlayer> findAllByGameIdAndIdIn(Long gameId, Collection<Long> ids);

    Optional<GamePlayer> findByIdAndGameId(Long id, Long gameId);
}
