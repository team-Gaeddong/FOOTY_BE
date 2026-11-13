package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.PositionPriorities;
import com.gaeddong.footy.domain.member.service.MemberReader;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerJoinService {

    private final GameReader gameReader;
    private final MemberReader memberReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final Clock clock;

    /**
     * 회원 본인이 모집 중인 경기에 참여한다. 이름은 닉네임, 실력은 파티장이 나중에 매긴다(D-19).
     */
    @Transactional
    public JoinedPlayer join(Long memberId, Long gameId, List<Position> positions) {
        PositionPriorities priorities = new PositionPriorities(positions);
        Member member = memberReader.getActiveMember(memberId);
        Game game = gameReader.getGame(gameId);
        if (!game.isRecruiting()) {
            throw new FootyException(GameErrorCode.GAME_NOT_RECRUITING);
        }
        if (gamePlayerRepository.existsByGameIdAndMemberId(gameId, memberId)) {
            throw new FootyException(GameErrorCode.ALREADY_JOINED);
        }

        GamePlayer player = saveNewPlayer(new GamePlayer(game, member, member.getNickname(), LocalDateTime.now(clock)));
        List<PlayerPosition> savedPositions = playerPositionRepository.saveAll(PlayerPosition.allOf(player, priorities));
        return new JoinedPlayer(player, savedPositions);
    }

    // 동시에 참여하면 사전 확인을 통과해도 uk_game_player_member에 걸린다
    private GamePlayer saveNewPlayer(GamePlayer player) {
        try {
            return gamePlayerRepository.saveAndFlush(player);
        } catch (DataIntegrityViolationException e) {
            throw new FootyException(GameErrorCode.ALREADY_JOINED);
        }
    }

    public record JoinedPlayer(GamePlayer player, List<PlayerPosition> positions) {
    }
}
