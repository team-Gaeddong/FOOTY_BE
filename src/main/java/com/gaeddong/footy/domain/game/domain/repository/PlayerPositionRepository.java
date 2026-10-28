package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PlayerPositionRepository extends JpaRepository<PlayerPosition, Long> {

    List<PlayerPosition> findAllByGamePlayerIdOrderByPriorityAsc(Long gamePlayerId);

    List<PlayerPosition> findAllByGamePlayerIdInOrderByPriorityAsc(Collection<Long> gamePlayerIds);

    // 전체 교체 전에 바로 지운다. 파생 delete는 새 행 insert 뒤에 실행될 수 있다.
    @Modifying(flushAutomatically = true)
    @Query("delete from PlayerPosition pp where pp.gamePlayer.id = :gamePlayerId")
    void deleteAllByGamePlayerId(@Param("gamePlayerId") Long gamePlayerId);
}
