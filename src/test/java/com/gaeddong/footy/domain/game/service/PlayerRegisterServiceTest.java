package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.service.PlayerRegisterService.RegisteredPlayer;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerRegisterServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long GAME_ID = 10L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 12, 0);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @Mock
    private PlayerPositionRepository playerPositionRepository;

    private PlayerRegisterService playerRegisterService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        playerRegisterService = new PlayerRegisterService(gameRepository, gamePlayerRepository,
                playerPositionRepository, clock);
    }

    private static Game game() {
        Member host = Member.builder()
                .email("host@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();
        ReflectionTestUtils.setField(host, "id", HOST_ID);
        Game game = Game.create(host, "일요일 풋살", NOW.plusDays(3), "대구 풋살장", 12, 2, NOW);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
        return game;
    }

    private void returnSavedArguments() {
        when(gamePlayerRepository.save(any(GamePlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerPositionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 파티장이_비회원_선수를_이름_포지션_실력과_함께_등록한다() {
        Game game = game();
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        returnSavedArguments();

        RegisteredPlayer registered = playerRegisterService.register(
                HOST_ID, GAME_ID, "홍길동", List.of(Position.FW, Position.MF), 7);

        GamePlayer player = registered.player();
        assertThat(player.getGame()).isEqualTo(game);
        assertThat(player.getMember()).isNull();
        assertThat(player.getName()).isEqualTo("홍길동");
        assertThat(player.getSkillLevel()).isEqualTo(7);
        assertThat(player.isSubstitute()).isFalse();
        assertThat(player.getJoinedAt()).isEqualTo(NOW);
        assertThat(registered.positions())
                .extracting(PlayerPosition::getGamePlayer, PlayerPosition::getPosition, PlayerPosition::getPriority)
                .containsExactly(tuple(player, Position.FW, 1), tuple(player, Position.MF, 2));
    }

    @Test
    void 실력을_생략하면_NULL로_등록한다() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
        returnSavedArguments();

        RegisteredPlayer registered = playerRegisterService.register(
                HOST_ID, GAME_ID, "홍길동", List.of(Position.GK), null);

        assertThat(registered.player().getSkillLevel()).isNull();
    }

    @Test
    void 포지션_목록이_잘못되면_400이고_조회하지_않는다() {
        assertThatThrownBy(() -> playerRegisterService.register(
                HOST_ID, GAME_ID, "홍길동", List.of(Position.FW, Position.FW), 7))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.INVALID_POSITIONS);
        verifyNoInteractions(gameRepository, gamePlayerRepository, playerPositionRepository);
    }

    @Test
    void 없거나_삭제된_경기면_404() {
        Game deleted = game();
        ReflectionTestUtils.setField(deleted, "deleted", true);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> playerRegisterService.register(HOST_ID, GAME_ID, "홍길동", List.of(Position.FW), 7))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> playerRegisterService.register(HOST_ID, GAME_ID, "홍길동", List.of(Position.FW), 7))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 파티장이_아니면_403이고_저장하지_않는다() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));

        assertThatThrownBy(() -> playerRegisterService.register(2L, GAME_ID, "홍길동", List.of(Position.FW), 7))
                .extracting("errorCode").isEqualTo(GameErrorCode.NOT_GAME_HOST);
        verify(gamePlayerRepository, never()).save(any());
    }

    @Test
    void 모집_중이_아니면_409이고_저장하지_않는다() {
        Game confirmed = game();
        ReflectionTestUtils.setField(confirmed, "status", GameStatus.CONFIRMED);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> playerRegisterService.register(HOST_ID, GAME_ID, "홍길동", List.of(Position.FW), 7))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_RECRUITING);
        verify(gamePlayerRepository, never()).save(any());
    }
}
