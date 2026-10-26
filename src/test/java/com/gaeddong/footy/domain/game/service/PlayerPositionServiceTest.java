package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.MemberPosition;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerPositionServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long GAME_ID = 10L;
    private static final Long PLAYER_ID = 100L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @Mock
    private PlayerPositionRepository playerPositionRepository;

    private PlayerPositionService playerPositionService;

    @BeforeEach
    void setUp() {
        playerPositionService = new PlayerPositionService(gameRepository, gamePlayerRepository, playerPositionRepository);
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

    private static GamePlayer player(Game game) {
        GamePlayer player = new GamePlayer(game, null, "비회원");
        ReflectionTestUtils.setField(player, "id", PLAYER_ID);
        return player;
    }

    @SuppressWarnings("unchecked")
    private List<PlayerPosition> savedPositions() {
        ArgumentCaptor<List<PlayerPosition>> captor = ArgumentCaptor.forClass(List.class);
        verify(playerPositionRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void 파티장이_선수_포지션을_순서대로_교체한다() {
        Game game = game();
        GamePlayer player = player(game);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findByIdAndGameId(PLAYER_ID, GAME_ID)).thenReturn(Optional.of(player));

        playerPositionService.updatePositions(HOST_ID, GAME_ID, PLAYER_ID, List.of(Position.DF, Position.GK));

        InOrder order = inOrder(playerPositionRepository);
        order.verify(playerPositionRepository).deleteAllByGamePlayerId(PLAYER_ID);
        order.verify(playerPositionRepository).saveAll(any());
        assertThat(savedPositions())
                .extracting(PlayerPosition::getGamePlayer, PlayerPosition::getPosition, PlayerPosition::getPriority)
                .containsExactly(tuple(player, Position.DF, 1), tuple(player, Position.GK, 2));
    }

    @Test
    void 포지션_목록이_잘못되면_400이고_조회하지_않는다() {
        assertThatThrownBy(() -> playerPositionService.updatePositions(HOST_ID, GAME_ID, PLAYER_ID, List.of()))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.INVALID_POSITIONS);
        verifyNoInteractions(gameRepository, gamePlayerRepository, playerPositionRepository);
    }

    @Test
    void 없거나_삭제된_경기면_404() {
        Game deleted = game();
        ReflectionTestUtils.setField(deleted, "deleted", true);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> playerPositionService.updatePositions(HOST_ID, GAME_ID, PLAYER_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> playerPositionService.updatePositions(HOST_ID, GAME_ID, PLAYER_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 파티장이_아니면_403() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));

        assertThatThrownBy(() -> playerPositionService.updatePositions(2L, GAME_ID, PLAYER_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.NOT_GAME_HOST);
        verify(playerPositionRepository, never()).deleteAllByGamePlayerId(anyLong());
    }

    @Test
    void 모집_중이_아니면_409() {
        Game confirmed = game();
        ReflectionTestUtils.setField(confirmed, "status", GameStatus.CONFIRMED);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> playerPositionService.updatePositions(HOST_ID, GAME_ID, PLAYER_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_RECRUITING);
    }

    @Test
    void 그_경기의_참가자가_아니면_404() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
        when(gamePlayerRepository.findByIdAndGameId(PLAYER_ID, GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerPositionService.updatePositions(HOST_ID, GAME_ID, PLAYER_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_PLAYER_NOT_FOUND);
        verify(playerPositionRepository, never()).saveAll(any());
    }

    @Test
    void 선호_포지션을_우선순위_순서대로_복사한다() {
        GamePlayer player = player(game());
        Member member = Member.builder().email("m@example.com").provider(Provider.GOOGLE).providerId("sub-m").build();
        List<MemberPosition> memberPositions = List.of(
                MemberPosition.builder().member(member).position(Position.MF).priority(2).build(),
                MemberPosition.builder().member(member).position(Position.FW).priority(1).build());

        playerPositionService.copyMemberPositions(player, memberPositions);

        assertThat(savedPositions())
                .extracting(PlayerPosition::getPosition, PlayerPosition::getPriority)
                .containsExactly(tuple(Position.FW, 1), tuple(Position.MF, 2));
    }

    @Test
    void 선호_포지션이_없으면_복사하지_않는다() {
        playerPositionService.copyMemberPositions(player(game()), List.of());

        verifyNoInteractions(playerPositionRepository);
    }
}
