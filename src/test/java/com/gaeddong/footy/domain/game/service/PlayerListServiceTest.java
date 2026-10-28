package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.service.PlayerListService.ListedPlayer;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerListServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long GAME_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 12, 0);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @Mock
    private PlayerPositionRepository playerPositionRepository;

    private PlayerListService playerListService;

    @BeforeEach
    void setUp() {
        playerListService = new PlayerListService(gameRepository, gamePlayerRepository, playerPositionRepository);
    }

    private static Member member(Long id, String nickname) {
        Member member = Member.builder()
                .email(nickname + "@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-" + id)
                .build();
        member.completeOnboarding(nickname);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private static Game game() {
        Game game = Game.create(member(HOST_ID, "파티장"), "일요일 풋살", NOW.plusDays(3), "대구 풋살장", 12, 2, NOW);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
        return game;
    }

    private static GamePlayer player(Game game, Long id, Member member, Integer skillLevel) {
        GamePlayer player = new GamePlayer(game, member, member.getNickname(), NOW);
        ReflectionTestUtils.setField(player, "id", id);
        if (skillLevel != null) {
            player.changeSkillLevel(skillLevel);
        }
        return player;
    }

    private static PlayerPosition position(GamePlayer player, Position position, int priority) {
        return PlayerPosition.builder().gamePlayer(player).position(position).priority(priority).build();
    }

    private List<GamePlayer> givenPlayers(Game game) {
        GamePlayer host = player(game, 100L, game.getHost(), 8);
        GamePlayer other = player(game, 101L, member(OTHER_ID, "홍길동"), 5);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.findAllByGameIdOrderByJoinedAtAscIdAsc(GAME_ID)).thenReturn(List.of(host, other));
        when(playerPositionRepository.findAllByGamePlayerIdInOrderByPriorityAsc(List.of(100L, 101L))).thenReturn(List.of(
                position(host, Position.GK, 1),
                position(other, Position.FW, 1),
                position(host, Position.DF, 2)));
        return List.of(host, other);
    }

    @Test
    void 파티장이_조회하면_실력까지_참여_순서대로_보인다() {
        List<GamePlayer> players = givenPlayers(game());

        List<ListedPlayer> listed = playerListService.list(HOST_ID, GAME_ID);

        assertThat(listed)
                .extracting(ListedPlayer::player, ListedPlayer::positions, ListedPlayer::skillLevel)
                .containsExactly(
                        tuple(players.get(0), List.of(Position.GK, Position.DF), 8),
                        tuple(players.get(1), List.of(Position.FW), 5));
    }

    @Test
    void 파티장이_아니면_실력은_NULL이다() {
        givenPlayers(game());

        List<ListedPlayer> listed = playerListService.list(OTHER_ID, GAME_ID);

        assertThat(listed).extracting(ListedPlayer::skillLevel).containsExactly(null, null);
        assertThat(listed).extracting(ListedPlayer::positions)
                .containsExactly(List.of(Position.GK, Position.DF), List.of(Position.FW));
    }

    @Test
    void 참가_선수가_없으면_빈_목록이고_포지션을_조회하지_않는다() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
        when(gamePlayerRepository.findAllByGameIdOrderByJoinedAtAscIdAsc(GAME_ID)).thenReturn(List.of());

        assertThat(playerListService.list(HOST_ID, GAME_ID)).isEmpty();
        verify(playerPositionRepository, never()).findAllByGamePlayerIdInOrderByPriorityAsc(any());
    }

    @Test
    void 없거나_삭제된_경기면_404() {
        Game deleted = game();
        ReflectionTestUtils.setField(deleted, "deleted", true);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> playerListService.list(HOST_ID, GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> playerListService.list(HOST_ID, GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
    }
}
