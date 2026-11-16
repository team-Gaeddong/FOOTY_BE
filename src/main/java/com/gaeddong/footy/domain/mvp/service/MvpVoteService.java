package com.gaeddong.footy.domain.mvp.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.service.MemberReader;
import com.gaeddong.footy.domain.mvp.domain.MvpVote;
import com.gaeddong.footy.domain.mvp.domain.repository.MvpVoteRepository;
import com.gaeddong.footy.domain.mvp.exception.MvpErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MvpVoteService {

    private final MemberReader memberReader;
    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final MvpVoteRepository mvpVoteRepository;
    private final Clock clock;

    /**
     * 경기에 참가한 회원이 종료된 경기에서 본인이 아닌 참가 선수 한 명을 MVP로 뽑는다(D-31).
     */
    @Transactional
    public MvpVote vote(Long voterId, Long gameId, Long gamePlayerId) {
        Member voter = memberReader.getActiveMember(voterId);
        Game game = gameReader.getGame(gameId);
        validateVoter(game, voterId);
        GamePlayer candidate = gamePlayerRepository.findByIdAndGameId(gamePlayerId, gameId)
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_PLAYER_NOT_FOUND));
        if (candidate.getMember() != null && candidate.getMember().getId().equals(voterId)) {
            throw new FootyException(MvpErrorCode.SELF_VOTE);
        }

        return saveNewVote(MvpVote.builder()
                .game(game)
                .voter(voter)
                .gamePlayer(candidate)
                .createdAt(LocalDateTime.now(clock))
                .build());
    }

    private void validateVoter(Game game, Long voterId) {
        if (!gamePlayerRepository.existsByGameIdAndMemberId(game.getId(), voterId)) {
            throw new FootyException(MvpErrorCode.NOT_GAME_PLAYER);
        }
        if (!game.isFinished()) {
            throw new FootyException(MvpErrorCode.GAME_NOT_FINISHED);
        }
        if (mvpVoteRepository.existsByGameIdAndVoterId(game.getId(), voterId)) {
            throw new FootyException(MvpErrorCode.ALREADY_VOTED);
        }
    }

    // 동시에 투표하면 사전 확인을 통과해도 uk_mvp_vote에 걸린다
    private MvpVote saveNewVote(MvpVote vote) {
        try {
            return mvpVoteRepository.saveAndFlush(vote);
        } catch (DataIntegrityViolationException e) {
            throw new FootyException(MvpErrorCode.ALREADY_VOTED);
        }
    }
}
