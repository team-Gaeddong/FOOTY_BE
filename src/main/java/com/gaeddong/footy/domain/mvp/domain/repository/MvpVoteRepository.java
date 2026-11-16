package com.gaeddong.footy.domain.mvp.domain.repository;

import com.gaeddong.footy.domain.mvp.domain.MvpVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MvpVoteRepository extends JpaRepository<MvpVote, Long> {

    boolean existsByGameIdAndVoterId(Long gameId, Long voterId);

    Optional<MvpVote> findByGameIdAndVoterId(Long gameId, Long voterId);

    long countByGameId(Long gameId);

    @Query("""
            select v.gamePlayer.id as gamePlayerId, count(v) as voteCount
            from MvpVote v
            where v.game.id = :gameId
            group by v.gamePlayer.id
            """)
    List<VoteCount> countVotesByGameId(Long gameId);

    interface VoteCount {

        Long getGamePlayerId();

        long getVoteCount();
    }
}
