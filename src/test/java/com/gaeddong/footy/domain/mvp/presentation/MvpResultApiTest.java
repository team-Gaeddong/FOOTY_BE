package com.gaeddong.footy.domain.mvp.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.mvp.domain.MvpVote;
import com.gaeddong.footy.domain.mvp.domain.repository.MvpVoteRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MvpResultApiTest {

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
    private MvpVoteRepository mvpVoteRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member host;
    private Member a;
    private Member b;
    private Game game;
    private GamePlayer hostPlayer;
    private GamePlayer aPlayer;
    private GamePlayer bPlayer;

    @BeforeEach
    void setUp() {
        host = saveMember("host");
        a = saveMember("a");
        b = saveMember("b");
        game = saveGame(host);
        hostPlayer = join(host);
        aPlayer = join(a);
        bPlayer = join(b);
    }

    @Test
    void 득표순으로_선수별_득표수와_최다_득표자를_반환한다() throws Exception {
        vote(host, bPlayer);
        vote(a, bPlayer);
        vote(b, aPlayer);

        perform(a)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.requiredCount").value(3))
                .andExpect(jsonPath("$.votedCount").value(3))
                .andExpect(jsonPath("$.mvpGamePlayerIds", contains(bPlayer.getId().intValue())))
                .andExpect(jsonPath("$.candidates[*].gamePlayerId", contains(
                        bPlayer.getId().intValue(), aPlayer.getId().intValue(), hostPlayer.getId().intValue())))
                .andExpect(jsonPath("$.candidates[0].name").value("b"))
                .andExpect(jsonPath("$.candidates[0].voteCount").value(2))
                .andExpect(jsonPath("$.candidates[1].voteCount").value(1))
                .andExpect(jsonPath("$.candidates[2].voteCount").value(0))
                .andExpect(jsonPath("$.candidates[0]", not(hasKey("voters"))));
    }

    @Test
    void 투표_중에도_현재_집계를_반환하고_동점이면_모두_최다_득표자다() throws Exception {
        vote(host, aPlayer);
        vote(a, bPlayer);

        perform(host)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredCount").value(3))
                .andExpect(jsonPath("$.votedCount").value(2))
                .andExpect(jsonPath("$.mvpGamePlayerIds", containsInAnyOrder(
                        aPlayer.getId().intValue(), bPlayer.getId().intValue())))
                // 득표가 같으면 참여 순
                .andExpect(jsonPath("$.candidates[*].gamePlayerId", contains(
                        aPlayer.getId().intValue(), bPlayer.getId().intValue(), hostPlayer.getId().intValue())));
    }

    @Test
    void 표가_없으면_최다_득표자는_빈_배열이고_비회원_선수도_후보에_포함한다() throws Exception {
        GamePlayer guest = gamePlayerRepository.saveAndFlush(new GamePlayer(game, null, "게스트", LocalDateTime.now()));
        b.withdraw(LocalDateTime.now());
        memberRepository.flush();

        perform(host)
                .andExpect(status().isOk())
                // 비회원·탈퇴 회원은 투표 대상 인원에서 뺀다
                .andExpect(jsonPath("$.requiredCount").value(2))
                .andExpect(jsonPath("$.votedCount").value(0))
                .andExpect(jsonPath("$.mvpGamePlayerIds").isEmpty())
                .andExpect(jsonPath("$.candidates[*].gamePlayerId", contains(
                        hostPlayer.getId().intValue(), aPlayer.getId().intValue(),
                        bPlayer.getId().intValue(), guest.getId().intValue())));
    }

    @Test
    void 파티장과_참가자가_아니면_403을_반환한다() throws Exception {
        Member other = saveMember("other");

        perform(other)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MVP_403_2"));
    }

    @Test
    void 로그인하지_않으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/games/{gameId}/mvp-votes/result", game.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 존재하지_않거나_삭제된_경기는_404를_반환한다() throws Exception {
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.flush();

        perform(host)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
        mockMvc.perform(get("/games/{gameId}/mvp-votes/result", Long.MAX_VALUE)
                        .header("Authorization", token(host)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    private ResultActions perform(Member member) throws Exception {
        return mockMvc.perform(get("/games/{gameId}/mvp-votes/result", game.getId())
                .header("Authorization", token(member)));
    }

    private void vote(Member voter, GamePlayer target) {
        mvpVoteRepository.saveAndFlush(MvpVote.builder()
                .game(game)
                .voter(voter)
                .gamePlayer(target)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Member saveMember(String nickname) {
        String key = UUID.randomUUID().toString();
        Member member = Member.builder()
                .email(key + "@example.com")
                .provider(Provider.GOOGLE)
                .providerId(key)
                .build();
        member.completeOnboarding(nickname);
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame(Member host) {
        Game game = Game.create(host, "주말 풋살", LocalDateTime.now().plusDays(2),
                "풋살장", 10, 2, LocalDateTime.now());
        ReflectionTestUtils.setField(game, "status", GameStatus.FINISHED);
        return gameRepository.saveAndFlush(game);
    }

    private GamePlayer join(Member member) {
        return gamePlayerRepository.saveAndFlush(
                new GamePlayer(game, member, member.getNickname(), LocalDateTime.now()));
    }

    private String token(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }
}
