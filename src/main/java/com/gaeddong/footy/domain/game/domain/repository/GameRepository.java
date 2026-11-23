package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    @EntityGraph(attributePaths = "host")
    Page<Game> findByDeletedFalseAndStatusIn(Collection<GameStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "host")
    Page<Game> findByDeletedFalseAndStatusInAndHostId(
            Collection<GameStatus> statuses, Long hostId, Pageable pageable);

    // 회원이 참가 선수로 들어 있는 경기. 파티장은 경기 생성 때 자동 참가하므로 직접 만든 경기도 포함된다
    @EntityGraph(attributePaths = "host")
    @Query("""
            select g
            from Game g
            where g.deleted = false
              and g.status in :statuses
              and exists (select 1 from GamePlayer p where p.game = g and p.member.id = :memberId)
            """)
    Page<Game> findJoinedBy(Long memberId, Collection<GameStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "host")
    Optional<Game> findWithHostById(Long id);
}
