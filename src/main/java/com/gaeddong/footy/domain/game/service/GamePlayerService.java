package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class GamePlayerService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;

    public GamePlayerService(GameRepository gameRepository, GamePlayerRepository gamePlayerRepository) {
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
    }

    /**
     * 실력 점수는 파티장 한 명이 매기며 경기 상태와 관계없이 바꿀 수 있다(#19).
     * 한 명이라도 이 경기의 선수가 아니면 아무것도 반영하지 않는다.
     */
    @Transactional
    public void updateSkillLevels(Long memberId, Long gameId, Map<Long, Integer> skillLevels) {
        Game game = gameRepository.findById(gameId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));
        if (!game.getHost().getId().equals(memberId)) {
            throw new FootyException(GameErrorCode.NOT_GAME_HOST);
        }

        List<GamePlayer> players = gamePlayerRepository.findAllByGameIdAndIdIn(gameId, skillLevels.keySet());
        if (players.size() != skillLevels.size()) {
            throw new FootyException(GameErrorCode.GAME_PLAYER_NOT_FOUND);
        }
        players.forEach(player -> player.changeSkillLevel(skillLevels.get(player.getId())));
    }
}
