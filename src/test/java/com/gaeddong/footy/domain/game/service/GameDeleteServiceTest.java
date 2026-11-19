package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameDeleteServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long GAME_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    @Mock
    private GameRepository gameRepository;

    private GameDeleteService gameDeleteService;

    private Game game;

    @BeforeEach
    void setUp() {
        gameDeleteService = new GameDeleteService(new GameReader(gameRepository));

        Member host = Member.builder()
                .email("host@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();
        ReflectionTestUtils.setField(host, "id", HOST_ID);
        game = Game.create(host, "일요일 풋살", NOW.plusDays(3), "풋살장", 12, 2, NOW);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
    }

    @ParameterizedTest
    @EnumSource(value = GameStatus.class, names = {"RECRUITING", "CONFIRMED"})
    void 파티장은_종료_전_경기를_소프트_삭제한다(GameStatus status) {
        ReflectionTestUtils.setField(game, "status", status);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        gameDeleteService.delete(HOST_ID, GAME_ID);

        assertThat(game.isDeleted()).isTrue();
    }

    @Test
    void 종료된_경기는_409이고_삭제하지_않는다() {
        ReflectionTestUtils.setField(game, "status", GameStatus.FINISHED);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameDeleteService.delete(HOST_ID, GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.FINISHED_GAME_NOT_DELETABLE);
        assertThat(game.isDeleted()).isFalse();
    }

    @Test
    void 파티장이_아니면_403이고_삭제하지_않는다() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameDeleteService.delete(2L, GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.NOT_GAME_HOST);
        assertThat(game.isDeleted()).isFalse();
    }

    @Test
    void 없거나_이미_삭제된_경기면_404() {
        game.delete();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameDeleteService.delete(HOST_ID, GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> gameDeleteService.delete(HOST_ID, GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
    }
}
