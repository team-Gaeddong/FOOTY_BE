package com.gaeddong.footy.domain.mvp.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.mvp.domain.repository.MvpVoteRepository;
import com.gaeddong.footy.domain.mvp.domain.repository.MvpVoteRepository.VoteCount;
import com.gaeddong.footy.domain.mvp.exception.MvpErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MVP 투표 집계(D-31). 투표 중에도 현재 득표를 보여 주고, 누가 누구에게 투표했는지는 내보내지 않는다.
 */
@Service
@RequiredArgsConstructor
public class MvpResultService {

    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final MvpVoteRepository mvpVoteRepository;

    @Transactional(readOnly = true)
    public Result getResult(Long requesterId, Long gameId) {
        validateReadable(requesterId, gameId);

        Map<Long, Long> voteCountByPlayerId = mvpVoteRepository.countVotesByGameId(gameId).stream()
                .collect(Collectors.toMap(VoteCount::getGamePlayerId, VoteCount::getVoteCount));
        // 참여 순으로 가져와 안정 정렬하므로 득표가 같으면 참여 순으로 남는다
        List<Candidate> candidates = gamePlayerRepository.findAllByGameIdOrderByJoinedAtAscIdAsc(gameId).stream()
                .map(player -> Candidate.of(player, voteCountByPlayerId.getOrDefault(player.getId(), 0L)))
                .sorted(Comparator.comparingLong(Candidate::voteCount).reversed())
                .toList();

        long topVoteCount = candidates.stream().mapToLong(Candidate::voteCount).max().orElse(0);
        List<Long> mvpGamePlayerIds = topVoteCount == 0 ? List.of() : candidates.stream()
                .filter(candidate -> candidate.voteCount() == topVoteCount)
                .map(Candidate::gamePlayerId)
                .toList();

        return new Result(
                gamePlayerRepository.countByGameIdAndMemberDeletedFalse(gameId),
                mvpVoteRepository.countByGameId(gameId),
                mvpGamePlayerIds,
                candidates);
    }

    private void validateReadable(Long requesterId, Long gameId) {
        Game game = gameReader.getGame(gameId);
        boolean readable = game.isHostedBy(requesterId)
                || gamePlayerRepository.existsByGameIdAndMemberId(gameId, requesterId);
        if (!readable) {
            throw new FootyException(MvpErrorCode.NOT_GAME_PARTICIPANT);
        }
    }

    public record Result(long requiredCount, long votedCount, List<Long> mvpGamePlayerIds, List<Candidate> candidates) {
    }

    public record Candidate(Long gamePlayerId, String name, long voteCount) {

        static Candidate of(GamePlayer player, long voteCount) {
            return new Candidate(player.getId(), player.getName(), voteCount);
        }
    }
}
