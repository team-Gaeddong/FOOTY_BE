package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.service.GameDetailService.GameDetail;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
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
class GameDetailServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long GAME_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 12, 0);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    private GameDetailService gameDetailService;

    @BeforeEach
    void setUp() {
        gameDetailService = new GameDetailService(gameRepository, gamePlayerRepository);
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

    @Test
    void 파티장이_조회하면_파티장이고_참가한_것으로_보인다() {
        Game game = game();
        when(gameRepository.findWithHostById(GAME_ID)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.countByGameId(GAME_ID)).thenReturn(5L);
        when(gamePlayerRepository.existsByGameIdAndMemberId(GAME_ID, HOST_ID)).thenReturn(true);

        GameDetail detail = gameDetailService.get(HOST_ID, GAME_ID);

        assertThat(detail.game()).isSameAs(game);
        assertThat(detail.playerCount()).isEqualTo(5L);
        assertThat(detail.host()).isTrue();
        assertThat(detail.joined()).isTrue();
    }

    @Test
    void 참가하지_않은_회원이_조회하면_파티장도_참가자도_아니다() {
        when(gameRepository.findWithHostById(GAME_ID)).thenReturn(Optional.of(game()));
        when(gamePlayerRepository.countByGameId(GAME_ID)).thenReturn(1L);
        when(gamePlayerRepository.existsByGameIdAndMemberId(GAME_ID, OTHER_ID)).thenReturn(false);

        GameDetail detail = gameDetailService.get(OTHER_ID, GAME_ID);

        assertThat(detail.host()).isFalse();
        assertThat(detail.joined()).isFalse();
    }

    @Test
    void 없거나_삭제된_경기면_404() {
        Game deleted = game();
        ReflectionTestUtils.setField(deleted, "deleted", true);
        when(gameRepository.findWithHostById(GAME_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> gameDetailService.get(HOST_ID, GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);

        when(gameRepository.findWithHostById(GAME_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> gameDetailService.get(HOST_ID, GAME_ID))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
    }
}
