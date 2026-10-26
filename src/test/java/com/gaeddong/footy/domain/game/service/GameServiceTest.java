package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MemberRepository memberRepository;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, memberRepository);
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
}
