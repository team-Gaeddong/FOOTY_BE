package com.gaeddong.footy.domain.poll.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.service.MemberReader;
import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.domain.PollAnswer;
import com.gaeddong.footy.domain.poll.domain.PollOption;
import com.gaeddong.footy.domain.poll.domain.repository.PollAnswerRepository;
import com.gaeddong.footy.domain.poll.domain.repository.PollOptionRepository;
import com.gaeddong.footy.domain.poll.domain.repository.PollRepository;
import com.gaeddong.footy.domain.poll.exception.PollErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollAnswerServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long GAME_ID = 10L;
    private static final Long POLL_ID = 100L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollOptionRepository pollOptionRepository;

    @Mock
    private PollAnswerRepository pollAnswerRepository;

    private PollAnswerService pollAnswerService;

    private Member member;

    private Game game;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        PollReader pollReader = new PollReader(new GameReader(gameRepository), gamePlayerRepository, pollRepository);
        pollAnswerService = new PollAnswerService(new MemberReader(memberRepository), pollReader,
                pollOptionRepository, pollAnswerRepository, clock);

        Member host = member(HOST_ID, "파티장");
        member = member(MEMBER_ID, "홍길동");
        game = Game.create(host, "일요일 풋살", NOW.plusDays(3), "풋살장", 12, 2, NOW);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
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

    private Poll poll(boolean allowMultiple, LocalDateTime deadline) {
        Poll poll = Poll.builder().game(game).creator(game.getHost()).title("투표")
                .allowMultiple(allowMultiple).deadline(deadline).createdAt(NOW).build();
        ReflectionTestUtils.setField(poll, "id", POLL_ID);
        return poll;
    }

    private List<PollOption> options(Poll poll) {
        PollOption first = PollOption.builder().poll(poll).content("A").build();
        PollOption second = PollOption.builder().poll(poll).content("B").build();
        ReflectionTestUtils.setField(first, "id", 1001L);
        ReflectionTestUtils.setField(second, "id", 1002L);
        return List.of(first, second);
    }

    private List<PollOption> givenParticipantAndPoll(Poll poll) {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.existsByGameIdAndMemberId(GAME_ID, MEMBER_ID)).thenReturn(true);
        when(pollRepository.findByIdAndGameId(POLL_ID, GAME_ID)).thenReturn(Optional.of(poll));
        List<PollOption> options = options(poll);
        when(pollOptionRepository.findAllByPollIdOrderByIdAsc(POLL_ID)).thenReturn(options);
        return options;
    }

    @SuppressWarnings("unchecked")
    private List<PollAnswer> savedAnswers() {
        ArgumentCaptor<List<PollAnswer>> captor = ArgumentCaptor.forClass(List.class);
        verify(pollAnswerRepository).saveAllAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void 참가_회원이_기존_응답을_지우고_새로_투표한다() {
        List<PollOption> options = givenParticipantAndPoll(poll(false, NOW.plusDays(1)));

        pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of(1002L));

        InOrder order = inOrder(pollAnswerRepository);
        order.verify(pollAnswerRepository).deleteAllByPollIdAndMemberId(POLL_ID, MEMBER_ID);
        order.verify(pollAnswerRepository).saveAllAndFlush(any());
        assertThat(savedAnswers())
                .extracting(PollAnswer::getPollOption, PollAnswer::getMember, PollAnswer::getCreatedAt)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(options.get(1), member, NOW));
    }

    @Test
    void 복수_선택_투표면_여러_개를_고를_수_있다() {
        givenParticipantAndPoll(poll(true, null));

        pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of(1001L, 1002L));

        assertThat(savedAnswers()).hasSize(2);
    }

    @Test
    void 빈_목록이면_내_응답을_지워_투표를_취소한다() {
        givenParticipantAndPoll(poll(false, null));

        pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of());

        verify(pollAnswerRepository).deleteAllByPollIdAndMemberId(POLL_ID, MEMBER_ID);
        assertThat(savedAnswers()).isEmpty();
    }

    @Test
    void 하나만_고르는_투표에_여러_개를_보내면_400() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.existsByGameIdAndMemberId(GAME_ID, MEMBER_ID)).thenReturn(true);
        when(pollRepository.findByIdAndGameId(POLL_ID, GAME_ID)).thenReturn(Optional.of(poll(false, null)));

        assertThatThrownBy(() -> pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of(1001L, 1002L)))
                .extracting("errorCode").isEqualTo(PollErrorCode.MULTIPLE_NOT_ALLOWED);
        verify(pollAnswerRepository, never()).deleteAllByPollIdAndMemberId(anyLong(), anyLong());
    }

    @Test
    void 같은_선택지를_중복해서_보내면_400이고_조회하지_않는다() {
        assertThatThrownBy(() -> pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of(1001L, 1001L)))
                .extracting("errorCode").isEqualTo(PollErrorCode.DUPLICATE_OPTION);
        verifyNoInteractions(memberRepository, gameRepository, pollRepository, pollAnswerRepository);
    }

    @Test
    void 마감된_투표면_409() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.existsByGameIdAndMemberId(GAME_ID, MEMBER_ID)).thenReturn(true);
        when(pollRepository.findByIdAndGameId(POLL_ID, GAME_ID)).thenReturn(Optional.of(poll(false, NOW)));

        assertThatThrownBy(() -> pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of(1001L)))
                .extracting("errorCode").isEqualTo(PollErrorCode.POLL_CLOSED);
        verify(pollAnswerRepository, never()).deleteAllByPollIdAndMemberId(anyLong(), anyLong());
    }

    @Test
    void 이_투표의_선택지가_아니면_404이고_아무것도_바꾸지_않는다() {
        givenParticipantAndPoll(poll(true, null));

        assertThatThrownBy(() -> pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of(1001L, 9999L)))
                .extracting("errorCode").isEqualTo(PollErrorCode.OPTION_NOT_FOUND);
        verify(pollAnswerRepository, never()).deleteAllByPollIdAndMemberId(anyLong(), anyLong());
    }

    @Test
    void 파티장도_참가_회원도_아니면_403() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.existsByGameIdAndMemberId(GAME_ID, MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of(1001L)))
                .extracting("errorCode").isEqualTo(PollErrorCode.NOT_GAME_PARTICIPANT);
        verifyNoInteractions(pollRepository, pollAnswerRepository);
    }

    @Test
    void 다른_경기의_투표면_404() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(gamePlayerRepository.existsByGameIdAndMemberId(GAME_ID, MEMBER_ID)).thenReturn(true);
        when(pollRepository.findByIdAndGameId(POLL_ID, GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of(1001L)))
                .extracting("errorCode").isEqualTo(PollErrorCode.POLL_NOT_FOUND);
    }

    @Test
    void 동시에_응답해_UNIQUE_제약에_걸리면_409() {
        givenParticipantAndPoll(poll(false, null));
        when(pollAnswerRepository.saveAllAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_poll_answer"));

        assertThatThrownBy(() -> pollAnswerService.answer(MEMBER_ID, GAME_ID, POLL_ID, List.of(1001L)))
                .extracting("errorCode").isEqualTo(PollErrorCode.CONCURRENT_ANSWER);
    }
}
