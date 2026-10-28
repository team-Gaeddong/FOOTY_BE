package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.service.PlayerJoinService.JoinedPlayer;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
class PlayerJoinServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long GAME_ID = 10L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 12, 0);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @Mock
    private PlayerPositionRepository playerPositionRepository;

    @Mock
    private MemberRepository memberRepository;

    private PlayerJoinService playerJoinService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        playerJoinService = new PlayerJoinService(gameRepository, gamePlayerRepository, playerPositionRepository,
                memberRepository, clock);
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

    private void givenMemberAndGame(Member member, Game game) {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
    }

    @Test
    void 회원이_닉네임과_포지션으로_경기에_참여한다() {
        Member member = member(MEMBER_ID, "홍길동");
        Game game = game();
        givenMemberAndGame(member, game);
        when(gamePlayerRepository.saveAndFlush(any(GamePlayer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerPositionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JoinedPlayer joined = playerJoinService.join(MEMBER_ID, GAME_ID, List.of(Position.FW, Position.MF));

        GamePlayer player = joined.player();
        assertThat(player.getGame()).isEqualTo(game);
        assertThat(player.getMember()).isEqualTo(member);
        assertThat(player.getName()).isEqualTo("홍길동");
        assertThat(player.getSkillLevel()).isNull();
        assertThat(player.isSubstitute()).isFalse();
        assertThat(player.getJoinedAt()).isEqualTo(NOW);
        assertThat(joined.positions())
                .extracting(PlayerPosition::getGamePlayer, PlayerPosition::getPosition, PlayerPosition::getPriority)
                .containsExactly(tuple(player, Position.FW, 1), tuple(player, Position.MF, 2));
    }

    @Test
    void 포지션_목록이_잘못되면_400이고_조회하지_않는다() {
        assertThatThrownBy(() -> playerJoinService.join(MEMBER_ID, GAME_ID, List.of(Position.FW, Position.FW)))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.INVALID_POSITIONS);
        assertThatThrownBy(() -> playerJoinService.join(MEMBER_ID, GAME_ID, null))
                .extracting("errorCode").isEqualTo(MemberErrorCode.INVALID_POSITIONS);
        verifyNoInteractions(memberRepository, gameRepository, gamePlayerRepository, playerPositionRepository);
    }

    @Test
    void 없거나_탈퇴한_회원이면_404() {
        Member withdrawn = member(MEMBER_ID, "홍길동");
        withdrawn.withdraw(NOW);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(withdrawn));

        assertThatThrownBy(() -> playerJoinService.join(MEMBER_ID, GAME_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> playerJoinService.join(MEMBER_ID, GAME_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 없거나_삭제된_경기면_404() {
        Game deleted = game();
        ReflectionTestUtils.setField(deleted, "deleted", true);
        givenMemberAndGame(member(MEMBER_ID, "홍길동"), deleted);

        assertThatThrownBy(() -> playerJoinService.join(MEMBER_ID, GAME_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);

        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> playerJoinService.join(MEMBER_ID, GAME_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 모집_중이_아니면_409이고_저장하지_않는다() {
        Game confirmed = game();
        ReflectionTestUtils.setField(confirmed, "status", GameStatus.CONFIRMED);
        givenMemberAndGame(member(MEMBER_ID, "홍길동"), confirmed);

        assertThatThrownBy(() -> playerJoinService.join(MEMBER_ID, GAME_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.GAME_NOT_RECRUITING);
        verify(gamePlayerRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이미_참여했으면_409이고_저장하지_않는다() {
        givenMemberAndGame(member(MEMBER_ID, "홍길동"), game());
        when(gamePlayerRepository.existsByGameIdAndMemberId(GAME_ID, MEMBER_ID)).thenReturn(true);

        assertThatThrownBy(() -> playerJoinService.join(MEMBER_ID, GAME_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.ALREADY_JOINED);
        verify(gamePlayerRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시에_참여해_UNIQUE_제약에_걸리면_409() {
        givenMemberAndGame(member(MEMBER_ID, "홍길동"), game());
        when(gamePlayerRepository.saveAndFlush(any(GamePlayer.class)))
                .thenThrow(new DataIntegrityViolationException("uk_game_player_member"));

        assertThatThrownBy(() -> playerJoinService.join(MEMBER_ID, GAME_ID, List.of(Position.FW)))
                .extracting("errorCode").isEqualTo(GameErrorCode.ALREADY_JOINED);
        verify(playerPositionRepository, never()).saveAll(any());
    }
}
