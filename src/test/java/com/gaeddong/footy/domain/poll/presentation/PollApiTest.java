package com.gaeddong.footy.domain.poll.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.domain.PollOption;
import com.gaeddong.footy.domain.poll.domain.repository.PollOptionRepository;
import com.gaeddong.footy.domain.poll.domain.repository.PollRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 의견 투표 생성·조회·응답 API를 보안 필터·예외 처리기와 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PollApiTest {

    private static final String CREATE_BODY = """
            {"title":"  뒤풀이 장소  ","options":["고기"," 치킨 ","피자"],"allowMultiple":false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollOptionRepository pollOptionRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member host;
    private Member player;
    private Member outsider;
    private Game game;

    @BeforeEach
    void setUp() {
        host = saveMember("host@example.com", "google-host", "파티장");
        player = saveMember("player@example.com", "google-player", "홍길동");
        outsider = saveMember("outsider@example.com", "google-outsider", "구경꾼");
        game = gameRepository.save(Game.create(host, "일요일 풋살", LocalDateTime.now().plusDays(3), "풋살장", 12, 2,
                LocalDateTime.now()));
        gamePlayerRepository.save(new GamePlayer(game, host, host.getNickname(), LocalDateTime.now()));
        gamePlayerRepository.save(new GamePlayer(game, player, player.getNickname(), LocalDateTime.now()));
    }

    private Member saveMember(String email, String providerId, String nickname) {
        Member member = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        member.completeOnboarding(nickname);
        return memberRepository.saveAndFlush(member);
    }

    private String bearer(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }

    private ResultActions createPoll(Member requester, String body) throws Exception {
        return mockMvc.perform(post("/games/{gameId}/polls", game.getId())
                .header("Authorization", bearer(requester))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions answer(Member requester, Long pollId, String optionIds) throws Exception {
        return mockMvc.perform(put("/games/{gameId}/polls/{pollId}/answers", game.getId(), pollId)
                .header("Authorization", bearer(requester))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"optionIds\":" + optionIds + "}"));
    }

    private Long createdPollId() throws Exception {
        String response = createPoll(host, CREATE_BODY).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private List<Long> optionIds(Long pollId) {
        return pollOptionRepository.findAllByPollIdOrderByIdAsc(pollId).stream().map(PollOption::getId).toList();
    }

    @Test
    void 파티장이_투표를_만들면_201과_빈_결과() throws Exception {
        createPoll(host, CREATE_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.title").value("뒤풀이 장소"))
                .andExpect(jsonPath("$.allowMultiple").value(false))
                .andExpect(jsonPath("$.deadline").isEmpty())
                .andExpect(jsonPath("$.isClosed").value(false))
                .andExpect(jsonPath("$.voterCount").value(0))
                .andExpect(jsonPath("$.options", hasSize(3)))
                .andExpect(jsonPath("$.options[1].content").value("치킨"))
                .andExpect(jsonPath("$.options[1].voteCount").value(0))
                .andExpect(jsonPath("$.options[1].isSelected").value(false));
    }

    @Test
    void 파티장이_아니면_만들_수_없다() throws Exception {
        createPoll(player, CREATE_BODY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));
    }

    @Test
    void 선택지가_잘못되면_400() throws Exception {
        createPoll(host, "{\"title\":\"투표\",\"options\":[\"A\",\" A \"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POLL_400_1"));
        createPoll(host, "{\"title\":\"투표\",\"options\":[\"A\",\"  \"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        createPoll(host, "{\"title\":\"투표\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 지난_마감_일시면_400() throws Exception {
        createPoll(host, "{\"title\":\"투표\",\"options\":[\"A\",\"B\"],\"deadline\":\"2020-01-01T00:00:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POLL_400_2"));
    }

    @Test
    void 참가_회원이_투표하면_득표_수와_투표자_닉네임이_보인다() throws Exception {
        Long pollId = createdPollId();
        List<Long> optionIds = optionIds(pollId);

        answer(player, pollId, "[" + optionIds.get(1) + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.options[1].voteCount").value(1))
                .andExpect(jsonPath("$.options[1].voters[0].memberId").value(player.getId()))
                .andExpect(jsonPath("$.options[1].voters[0].nickname").value("홍길동"))
                .andExpect(jsonPath("$.options[1].isSelected").value(true))
                .andExpect(jsonPath("$.options[0].isSelected").value(false));

        // 파티장이 볼 때는 내 선택이 아니다
        mockMvc.perform(get("/games/{gameId}/polls/{pollId}", game.getId(), pollId)
                        .header("Authorization", bearer(host)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[1].voteCount").value(1))
                .andExpect(jsonPath("$.options[1].isSelected").value(false));
    }

    @Test
    void 마감_전에는_응답을_바꾸거나_취소할_수_있다() throws Exception {
        Long pollId = createdPollId();
        List<Long> optionIds = optionIds(pollId);
        answer(player, pollId, "[" + optionIds.get(0) + "]").andExpect(status().isOk());

        answer(player, pollId, "[" + optionIds.get(2) + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].voteCount").value(0))
                .andExpect(jsonPath("$.options[2].voteCount").value(1));

        answer(player, pollId, "[]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(0))
                .andExpect(jsonPath("$.options[2].voteCount").value(0));
    }

    @Test
    void 하나만_고르는_투표에_여러_개면_400() throws Exception {
        Long pollId = createdPollId();
        List<Long> optionIds = optionIds(pollId);

        answer(player, pollId, "[" + optionIds.get(0) + "," + optionIds.get(1) + "]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POLL_400_3"));
    }

    @Test
    void 복수_선택_투표는_여러_개를_고를_수_있다() throws Exception {
        String response = createPoll(host, "{\"title\":\"가능한 날\",\"options\":[\"토\",\"일\"],\"allowMultiple\":true}")
                .andReturn().getResponse().getContentAsString();
        Long pollId = ((Number) JsonPath.read(response, "$.id")).longValue();
        List<Long> optionIds = optionIds(pollId);

        answer(player, pollId, "[" + optionIds.get(0) + "," + optionIds.get(1) + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voterCount").value(1))
                .andExpect(jsonPath("$.options[0].voteCount").value(1))
                .andExpect(jsonPath("$.options[1].voteCount").value(1));
    }

    @Test
    void 마감된_투표에_응답하면_409() throws Exception {
        Poll closed = pollRepository.save(Poll.builder().game(game).creator(host).title("마감")
                .deadline(LocalDateTime.now().minusMinutes(1)).createdAt(LocalDateTime.now().minusDays(1)).build());
        PollOption option = pollOptionRepository.save(PollOption.builder().poll(closed).content("A").build());
        pollOptionRepository.save(PollOption.builder().poll(closed).content("B").build());

        answer(player, closed.getId(), "[" + option.getId() + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLL_409_1"));
    }

    @Test
    void 이_투표의_선택지가_아니면_404() throws Exception {
        Long pollId = createdPollId();

        answer(player, pollId, "[999999]")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POLL_404_2"));
    }

    @Test
    void 참가하지_않은_회원은_조회도_투표도_403() throws Exception {
        Long pollId = createdPollId();

        mockMvc.perform(get("/games/{gameId}/polls", game.getId()).header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POLL_403_1"));
        mockMvc.perform(get("/games/{gameId}/polls/{pollId}", game.getId(), pollId)
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POLL_403_1"));
        answer(outsider, pollId, "[" + optionIds(pollId).get(0) + "]")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POLL_403_1"));
    }

    @Test
    void 목록은_최신순이고_투표_인원과_내_투표_여부가_보인다() throws Exception {
        Long first = createdPollId();
        Long second = createdPollId();
        answer(player, first, "[" + optionIds(first).get(0) + "]").andExpect(status().isOk());

        mockMvc.perform(get("/games/{gameId}/polls", game.getId()).header("Authorization", bearer(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.polls", hasSize(2)))
                .andExpect(jsonPath("$.polls[0].id").value(second))
                .andExpect(jsonPath("$.polls[0].voterCount").value(0))
                .andExpect(jsonPath("$.polls[0].hasVoted").value(false))
                .andExpect(jsonPath("$.polls[1].id").value(first))
                .andExpect(jsonPath("$.polls[1].voterCount").value(1))
                .andExpect(jsonPath("$.polls[1].hasVoted").value(true))
                .andExpect(jsonPath("$.polls[1].isClosed").value(false));
    }

    @Test
    void 다른_경기의_투표면_404() throws Exception {
        Long pollId = createdPollId();
        Game other = gameRepository.save(Game.create(host, "다른 경기", LocalDateTime.now().plusDays(5), "풋살장", 12, 2,
                LocalDateTime.now()));

        mockMvc.perform(get("/games/{gameId}/polls/{pollId}", other.getId(), pollId)
                        .header("Authorization", bearer(host)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POLL_404_1"));
    }

    @Test
    void 토큰_없이_호출하면_401() throws Exception {
        mockMvc.perform(get("/games/{gameId}/polls", game.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }
}
