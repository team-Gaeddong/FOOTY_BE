package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.MemberPosition;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberPositionRepository;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @Mock
    private MemberPositionRepository memberPositionRepository;

    @Mock
    private PlayerPositionService playerPositionService;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, gamePlayerRepository, memberRepository, memberPositionRepository,
                playerPositionService, CLOCK);
    }

    @Test
    void 로그인한_회원이_모집중_경기를_생성한다() {
        Member host = Member.builder()
                .email("host@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();
        ReflectionTestUtils.setField(host, "id", 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(host));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            ReflectionTestUtils.setField(game, "id", 10L);
            return game;
        });

        LocalDateTime playDate = LocalDateTime.of(2026, 11, 1, 10, 0);
        Game saved = gameService.create(1L, "일요일 풋살", playDate, "대구 풋살장", 12, 2);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getHost()).isSameAs(host);
        assertThat(saved.getTitle()).isEqualTo("일요일 풋살");
        assertThat(saved.getPlayDate()).isEqualTo(playDate);
        assertThat(saved.getLocation()).isEqualTo("대구 풋살장");
        assertThat(saved.getMaxPlayers()).isEqualTo(12);
        assertThat(saved.getTeamCount()).isEqualTo(2);
        assertThat(saved.getStatus()).isEqualTo(GameStatus.RECRUITING);
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void 없는_회원은_경기를_만들_수_없다() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.create(1L, "일요일 풋살", LocalDateTime.of(2026, 11, 1, 10, 0),
                "대구 풋살장", 12, 2))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        verify(gameRepository, never()).save(any());
    }

    @Test
    void 탈퇴한_회원은_경기를_만들_수_없다() {
        Member withdrawn = Member.builder()
                .email("host@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();
        withdrawn.withdraw(LocalDateTime.of(2026, 10, 20, 12, 0));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(withdrawn));

        assertThatThrownBy(() -> gameService.create(1L, "일요일 풋살", LocalDateTime.of(2026, 11, 1, 10, 0),
                "대구 풋살장", 12, 2))
                .extracting("errorCode").isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        verify(gameRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"1,2", "31,2"})
    void 최대_인원은_2명에서_30명이다(int maxPlayers, int teamCount) {
        assertThatThrownBy(() -> gameService.create(1L, "풋살", NOW.plusDays(1), "풋살장", maxPlayers, teamCount))
                .extracting("errorCode").isEqualTo(GameErrorCode.INVALID_MAX_PLAYERS);
        verify(memberRepository, never()).findById(any());
    }

    @ParameterizedTest
    @CsvSource({"10,1", "10,0", "10,-1", "10,11"})
    void 팀_수는_2_이상이고_최대_인원_이하다(int maxPlayers, int teamCount) {
        assertThatThrownBy(() -> gameService.create(1L, "풋살", NOW.plusDays(1), "풋살장", maxPlayers, teamCount))
                .extracting("errorCode").isEqualTo(GameErrorCode.INVALID_TEAM_COUNT);
    }

    @Test
    void 지금이거나_지난_일시로는_만들_수_없다() {
        assertThatThrownBy(() -> gameService.create(1L, "풋살", NOW, "풋살장", 10, 2))
                .extracting("errorCode").isEqualTo(GameErrorCode.PAST_PLAY_DATE);
        assertThatThrownBy(() -> gameService.create(1L, "풋살", NOW.minusDays(1), "풋살장", 10, 2))
                .extracting("errorCode").isEqualTo(GameErrorCode.PAST_PLAY_DATE);
    }

    @Test
    void 파티장을_닉네임으로_참가시키고_선호_포지션을_복사한다() {
        Member host = Member.builder()
                .email("host@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();
        host.completeOnboarding("파티장");
        ReflectionTestUtils.setField(host, "id", 1L);
        List<MemberPosition> hostPositions = List.of(
                MemberPosition.builder().member(host).position(Position.FW).priority(1).build());
        when(memberRepository.findById(1L)).thenReturn(Optional.of(host));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gamePlayerRepository.save(any(GamePlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberPositionRepository.findAllByMemberIdOrderByPriorityAsc(1L)).thenReturn(hostPositions);

        Game game = gameService.create(1L, "풋살", NOW.plusDays(1), "풋살장", 10, 2);

        ArgumentCaptor<GamePlayer> captor = ArgumentCaptor.forClass(GamePlayer.class);
        verify(gamePlayerRepository).save(captor.capture());
        GamePlayer hostPlayer = captor.getValue();
        assertThat(hostPlayer.getGame()).isSameAs(game);
        assertThat(hostPlayer.getMember()).isSameAs(host);
        assertThat(hostPlayer.getName()).isEqualTo("파티장");
        verify(playerPositionService).copyMemberPositions(hostPlayer, hostPositions);
    }
}
