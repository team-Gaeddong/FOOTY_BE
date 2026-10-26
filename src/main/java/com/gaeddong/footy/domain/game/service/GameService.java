package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberPositionRepository;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class GameService {

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 30;
    private static final int MIN_TEAM_COUNT = 2;

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;
    private final PlayerPositionService playerPositionService;
    private final Clock clock;

    public GameService(GameRepository gameRepository, GamePlayerRepository gamePlayerRepository,
                       MemberRepository memberRepository, MemberPositionRepository memberPositionRepository,
                       PlayerPositionService playerPositionService, Clock clock) {
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.memberRepository = memberRepository;
        this.memberPositionRepository = memberPositionRepository;
        this.playerPositionService = playerPositionService;
        this.clock = clock;
    }

    @Transactional
    public Game create(Long hostId, String title, LocalDateTime playDate, String location,
                       int maxPlayers, int teamCount) {
        LocalDateTime now = LocalDateTime.now(clock);
        validate(playDate, maxPlayers, teamCount, now);
        // 탈퇴한 회원은 없는 회원으로 본다(D-16)
        Member host = memberRepository.findById(hostId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(MemberErrorCode.MEMBER_NOT_FOUND));

        Game game = gameRepository.save(Game.create(host, title, playDate, location, maxPlayers, teamCount, now));
        joinHost(game, host);
        return game;
    }

    @Transactional(readOnly = true)
    public Page<Game> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new FootyException(GlobalErrorCode.INVALID_INPUT);
        }

        Sort sort = Sort.by(Sort.Direction.ASC, "playDate", "id");
        return gameRepository.findByDeletedFalse(PageRequest.of(page, size, sort));
    }

    /**
     * 파티장을 참가 선수로 추가하고 선호 포지션을 경기별 포지션으로 복사한다(D-13, D-14).
     */
    private void joinHost(Game game, Member host) {
        GamePlayer hostPlayer = gamePlayerRepository.save(new GamePlayer(game, host, host.getNickname()));
        playerPositionService.copyMemberPositions(hostPlayer,
                memberPositionRepository.findAllByMemberIdOrderByPriorityAsc(host.getId()));
    }

    /**
     * 경기 생성 정책(D-14): 최대 인원 2~30, 팀 수 2 이상이고 최대 인원 이하, 과거 일시 불가.
     */
    private void validate(LocalDateTime playDate, int maxPlayers, int teamCount, LocalDateTime now) {
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
