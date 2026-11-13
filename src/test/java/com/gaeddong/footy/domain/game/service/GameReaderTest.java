package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameReaderTest {

    private static final Long HOST_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long GAME_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 12, 0);

    @Mock
    private GameRepository gameRepository;

    private GameReader gameReader;

    @BeforeEach
    void setUp() {
        gameReader = new GameReader(gameRepository);
    }

    private static Game game() {
        Member host = Member.builder()
                .email("host@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-host")
                .build();
        ReflectionTestUtils.setField(host, "id", HOST_ID);
        Game game = Game.create(host, "일요일 풋살", NOW.plusDays(3), "대구 풋살장", 12, 2, NOW);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
        return game;
    }

    @Test
    void 경기를_찾는다() {
        Game game = game();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThat(gameReader.getGame(GAME_ID)).isSameAs(game);
    }

    @Test
    void 없는_경기는_404다() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());
        when(gameRepository.findWithHostById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameReader.getGame(GAME_ID))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
        assertThatThrownBy(() -> gameReader.getGameWithHost(GAME_ID))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 삭제된_경기는_없는_경기로_본다() {
        Game deleted = game();
        ReflectionTestUtils.setField(deleted, "deleted", true);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(deleted));
        when(gameRepository.findWithHostById(GAME_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> gameReader.getGame(GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
        assertThatThrownBy(() -> gameReader.getGameWithHost(GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
        // 파티장 여부보다 삭제 여부를 먼저 본다
        assertThatThrownBy(() -> gameReader.getGameAsHost(GAME_ID, OTHER_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 파티장만_파티장_전용_조회를_통과한다() {
        Game game = game();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThat(gameReader.getGameAsHost(GAME_ID, HOST_ID)).isSameAs(game);
        assertThatThrownBy(() -> gameReader.getGameAsHost(GAME_ID, OTHER_ID))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(GameErrorCode.NOT_GAME_HOST);
    }
}
