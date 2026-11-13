package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GamePlayerService {

    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 실력 점수는 파티장 한 명이 매기며 경기 상태와 관계없이 바꿀 수 있다(#19).
     * 한 명이라도 이 경기의 선수가 아니면 아무것도 반영하지 않는다.
     *
     * @param skillLevels 경기 참가자 ID(game_player_id)별 실력 점수
     */
    @Transactional
    public void updateSkillLevels(Long requesterId, Long gameId, Map<Long, Integer> skillLevels) {
        gameReader.getGameAsHost(gameId, requesterId);

        List<GamePlayer> players = gamePlayerRepository.findAllByGameIdAndIdIn(gameId, skillLevels.keySet());
        if (players.size() != skillLevels.size()) {
            throw new FootyException(GameErrorCode.GAME_PLAYER_NOT_FOUND);
        }
        players.forEach(player -> player.changeSkillLevel(skillLevels.get(player.getId())));
    }
}
