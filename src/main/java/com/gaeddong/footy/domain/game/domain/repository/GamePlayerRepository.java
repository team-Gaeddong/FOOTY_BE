package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    List<GamePlayer> findAllByGameIdAndIdIn(Long gameId, Collection<Long> ids);

    List<GamePlayer> findAllByGameIdOrderByJoinedAtAscIdAsc(Long gameId);

    Optional<GamePlayer> findByIdAndGameId(Long id, Long gameId);

    boolean existsByGameIdAndMemberId(Long gameId, Long memberId);

    long countByGameId(Long gameId);

    // 밸런스를 작성할 수 있는 선수(탈퇴하지 않은 회원) 수
    long countByGameIdAndMemberDeletedFalse(Long gameId);
}
