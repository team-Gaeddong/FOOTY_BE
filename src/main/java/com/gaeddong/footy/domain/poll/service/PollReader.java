package com.gaeddong.footy.domain.poll.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.domain.repository.PollRepository;
import com.gaeddong.footy.domain.poll.exception.PollErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 투표 조회·응답의 공통 규칙. 파티장과 경기 참가 회원만 투표를 보고 참여한다(D-35).
 * 트랜잭션은 호출하는 서비스의 것을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class PollReader {

    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final PollRepository pollRepository;

    /**
     * 없거나 삭제된 경기는 404 GAME_404_1, 파티장도 참가 회원도 아니면 403 POLL_403_1이다.
     */
    public Game getGameAsParticipant(Long gameId, Long memberId) {
        Game game = gameReader.getGame(gameId);
        boolean participant = game.isHostedBy(memberId)
                || gamePlayerRepository.existsByGameIdAndMemberId(gameId, memberId);
        if (!participant) {
            throw new FootyException(PollErrorCode.NOT_GAME_PARTICIPANT);
        }
        return game;
    }

    /**
     * 다른 경기의 투표도 없는 투표로 본다.
     */
    public Poll getPoll(Long gameId, Long pollId) {
        return pollRepository.findByIdAndGameId(pollId, gameId)
                .orElseThrow(() -> new FootyException(PollErrorCode.POLL_NOT_FOUND));
    }
}
