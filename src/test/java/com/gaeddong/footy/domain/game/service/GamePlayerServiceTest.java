package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.global.error.FootyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GamePlayerServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    private GamePlayerService gamePlayerService;

    private Game game;

    @BeforeEach
    void setUp() {
        gamePlayerService = new GamePlayerService(gameRepository, gamePlayerRepository);

        Member host = Member.builder()
                .email("host@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();
        ReflectionTestUtils.setField(host, "id", 1L);
        game = Game.create(host, "일요일 풋살", LocalDateTime.of(2026, 11, 1, 10, 0), "풋살장", 12, 2,
                LocalDateTime.now());
        ReflectionTestUtils.setField(game, "id", 10L);
    }

    @Test
    void 파티장이_여러_선수의_실력을_매긴다() {
        GamePlayer first = player(100L);
        GamePlayer second = player(101L);
        Map<Long, Integer> skillLevels = Map.of(100L, 7, 101L, 3);
        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findAllByGameIdAndIdIn(10L, skillLevels.keySet()))
                .thenReturn(List.of(first, second));

        gamePlayerService.updateSkillLevels(1L, 10L, skillLevels);

        assertThat(first.getSkillLevel()).isEqualTo(7);
        assertThat(second.getSkillLevel()).isEqualTo(3);
    }

    @Test
    void 파티장이_아니면_403이다() {
        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gamePlayerService.updateSkillLevels(2L, 10L, Map.of(100L, 7)))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode")
                .isEqualTo(GameErrorCode.NOT_GAME_HOST);
        verify(gamePlayerRepository, never()).findAllByGameIdAndIdIn(anyLong(), any());
    }

    @Test
    void 삭제된_경기는_404다() {
        ReflectionTestUtils.setField(game, "deleted", true);
        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gamePlayerService.updateSkillLevels(1L, 10L, Map.of(100L, 7)))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode")
                .isEqualTo(GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 이_경기의_선수가_아닌_ID가_있으면_아무것도_바꾸지_않는다() {
        GamePlayer first = player(100L);
        Map<Long, Integer> skillLevels = Map.of(100L, 7, 999L, 3);
        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findAllByGameIdAndIdIn(10L, skillLevels.keySet())).thenReturn(List.of(first));

        assertThatThrownBy(() -> gamePlayerService.updateSkillLevels(1L, 10L, skillLevels))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode")
                .isEqualTo(GameErrorCode.GAME_PLAYER_NOT_FOUND);
        assertThat(first.getSkillLevel()).isNull();
    }

    private GamePlayer player(Long id) {
        GamePlayer player = new GamePlayer(game, null, "선수" + id, LocalDateTime.now());
        ReflectionTestUtils.setField(player, "id", id);
        return player;
    }
}
