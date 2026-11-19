package com.gaeddong.footy.domain.poll.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.domain.PollOption;
import com.gaeddong.footy.domain.poll.domain.repository.PollOptionRepository;
import com.gaeddong.footy.domain.poll.domain.repository.PollRepository;
import com.gaeddong.footy.domain.poll.exception.PollErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long GAME_ID = 10L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollOptionRepository pollOptionRepository;

    private PollService pollService;

    private Game game;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        pollService = new PollService(new GameReader(gameRepository), pollRepository, pollOptionRepository, clock);

        Member host = Member.builder()
                .email("host@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();
        ReflectionTestUtils.setField(host, "id", HOST_ID);
        game = Game.create(host, "일요일 풋살", NOW.plusDays(3), "풋살장", 12, 2, NOW);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 파티장이_선택지와_함께_투표를_만든다() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Poll poll = pollService.create(HOST_ID, GAME_ID, "뒤풀이 장소", List.of("고기", "치킨"), true, NOW.plusDays(1));

        assertThat(poll.getGame()).isEqualTo(game);
        assertThat(poll.getCreator()).isEqualTo(game.getHost());
        assertThat(poll.getTitle()).isEqualTo("뒤풀이 장소");
        assertThat(poll.isAllowMultiple()).isTrue();
        assertThat(poll.getDeadline()).isEqualTo(NOW.plusDays(1));
        assertThat(poll.getCreatedAt()).isEqualTo(NOW);
        ArgumentCaptor<List<PollOption>> captor = ArgumentCaptor.forClass(List.class);
        verify(pollOptionRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(PollOption::getPoll, PollOption::getContent)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(poll, "고기"),
                        org.assertj.core.groups.Tuple.tuple(poll, "치킨"));
    }

    @Test
    void 마감_없이도_만들_수_있다() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(pollRepository.save(any(Poll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Poll poll = pollService.create(HOST_ID, GAME_ID, "투표", List.of("A", "B"), false, null);

        assertThat(poll.getDeadline()).isNull();
        assertThat(poll.isClosedAt(NOW.plusYears(1))).isFalse();
    }

    @Test
    void 선택지가_2개_미만이거나_10개_초과이거나_중복이면_400이고_조회하지_않는다() {
        List<List<String>> invalid = List.of(
                List.of("A"),
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"),
                List.of("A", "A"));

        for (List<String> options : invalid) {
            assertThatThrownBy(() -> pollService.create(HOST_ID, GAME_ID, "투표", options, false, null))
                    .extracting("errorCode").isEqualTo(PollErrorCode.INVALID_OPTIONS);
        }
        verifyNoInteractions(gameRepository, pollRepository, pollOptionRepository);
    }

    @Test
    void 마감_일시가_현재_이전이면_400() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> pollService.create(HOST_ID, GAME_ID, "투표", List.of("A", "B"), false, NOW))
                .extracting("errorCode").isEqualTo(PollErrorCode.PAST_DEADLINE);
        verify(pollRepository, never()).save(any());
    }

    @Test
    void 파티장이_아니면_403() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> pollService.create(2L, GAME_ID, "투표", List.of("A", "B"), false, null))
                .extracting("errorCode").isEqualTo(GameErrorCode.NOT_GAME_HOST);
        verify(pollRepository, never()).save(any());
    }
}
