package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    // 참가 선수가 없는 경기는 결과에 나오지 않는다
    @Query("""
            select p.game.id as gameId, count(p) as playerCount
            from GamePlayer p
            where p.game.id in :gameIds
            group by p.game.id
            """)
    List<PlayerCount> countPlayersByGameIds(Collection<Long> gameIds);

    // 회원별로 종료된 경기에서 받은 경기 후 평점의 평균과 그 경기 수. 평점을 받은 경기가 없는 회원은 결과에 나오지 않는다
    @Query("""
            select p.member.id as memberId, avg(p.rating) as average, count(p) as ratedGameCount
            from GamePlayer p
            where p.member.id in :memberIds
              and p.rating is not null
              and p.game.status = com.gaeddong.footy.domain.game.domain.GameStatus.FINISHED
              and p.game.deleted = false
            group by p.member.id
            """)
    List<RatingSummary> summarizeRatings(Collection<Long> memberIds);

    interface RatingSummary {

        Long getMemberId();

        Double getAverage();

        long getRatedGameCount();
    }

    interface PlayerCount {

        Long getGameId();

        long getPlayerCount();
    }
}
