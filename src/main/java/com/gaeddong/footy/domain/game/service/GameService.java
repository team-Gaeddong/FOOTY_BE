package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberPositionRepository;
import com.gaeddong.footy.domain.member.service.MemberReader;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 30;
    private static final int MIN_TEAM_COUNT = 2;

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final MemberReader memberReader;
    private final MemberPositionRepository memberPositionRepository;
    private final PlayerPositionService playerPositionService;
    private final Clock clock;

    /**
     * 경기를 만들고 만든 회원(파티장)을 첫 참가 선수로 넣는다(D-14).
     */
    @Transactional
    public Game create(Long hostId, String title, LocalDateTime playDate, String location,
                       int maxPlayers, int teamCount) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateCreatePolicy(playDate, maxPlayers, teamCount, now);
        Member host = memberReader.getActiveMember(hostId);

        Game game = gameRepository.save(Game.create(host, title, playDate, location, maxPlayers, teamCount, now));
        joinHost(game, host, now);
        return game;
    }

    /**
     * 파티장을 참가 선수로 추가하고 선호 포지션을 경기별 포지션으로 복사한다(D-13, D-14).
     */
    private void joinHost(Game game, Member host, LocalDateTime joinedAt) {
        GamePlayer hostPlayer = gamePlayerRepository.save(new GamePlayer(game, host, host.getNickname(), joinedAt));
        playerPositionService.copyMemberPositions(hostPlayer,
                memberPositionRepository.findAllByMemberIdOrderByPriorityAsc(host.getId()));
    }

    /**
     * 경기 생성 정책(D-14): 최대 인원 2~30, 팀 수 2 이상이고 최대 인원 이하, 과거 일시 불가.
     */
    private void validateCreatePolicy(LocalDateTime playDate, int maxPlayers, int teamCount, LocalDateTime now) {
        if (maxPlayers < MIN_PLAYERS || maxPlayers > MAX_PLAYERS) {
            throw new FootyException(GameErrorCode.INVALID_MAX_PLAYERS);
        }
        if (teamCount < MIN_TEAM_COUNT || teamCount > maxPlayers) {
            throw new FootyException(GameErrorCode.INVALID_TEAM_COUNT);
        }
        if (!playDate.isAfter(now)) {
            throw new FootyException(GameErrorCode.PAST_PLAY_DATE);
        }
    }
}
