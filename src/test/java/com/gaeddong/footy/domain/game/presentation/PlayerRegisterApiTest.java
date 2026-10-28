package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 선수 등록 API를 보안 필터·예외 처리기와 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlayerRegisterApiTest {

    private static final String BODY = "{\"name\":\"  홍길동  \",\"positions\":[\"FW\",\"MF\"],\"skillLevel\":7}";

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
    private PlayerPositionRepository playerPositionRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member saveMember(String email, String providerId) {
        Member member = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        member.completeOnboarding("회원");
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame(Member host) {
        return gameRepository.save(Game.create(host, "일요일 풋살", LocalDateTime.now().plusDays(3), "풋살장", 12, 2,
                LocalDateTime.now()));
    }

    private ResultActions postPlayer(Member requester, Long gameId, String body) throws Exception {
        return mockMvc.perform(post("/games/{gameId}/players", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 파티장이_선수를_등록하면_201과_등록된_선수() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Game game = saveGame(host);

        String response = postPlayer(host, game.getId(), BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.skillLevel").value(7))
                .andExpect(jsonPath("$.isSubstitute").value(false))
                .andExpect(jsonPath("$.positions[0]").value("FW"))
                .andExpect(jsonPath("$.positions[1]").value("MF"))
                .andExpect(jsonPath("$.joinedAt").exists())
                .andReturn().getResponse().getContentAsString();

        Long playerId = ((Number) JsonPath.read(response, "$.id")).longValue();
        GamePlayer saved = gamePlayerRepository.findByIdAndGameId(playerId, game.getId()).orElseThrow();
        assertThat(saved.getMember()).isNull();
        assertThat(saved.getName()).isEqualTo("홍길동");
        assertThat(playerPositionRepository.findAllByGamePlayerIdOrderByPriorityAsc(playerId))
                .extracting(PlayerPosition::getPosition, PlayerPosition::getPriority)
                .containsExactly(tuple(Position.FW, 1), tuple(Position.MF, 2));
    }

    @Test
    void 실력을_생략해도_201() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Game game = saveGame(host);

        postPlayer(host, game.getId(), "{\"name\":\"홍길동\",\"positions\":[\"GK\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skillLevel").isEmpty());
    }

    @Test
    void 파티장이_아니면_403() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Member other = saveMember("other@example.com", "google-other");
        Game game = saveGame(host);

        postPlayer(other, game.getId(), BODY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));
    }

    @Test
    void 없는_경기면_404() throws Exception {
        Member host = saveMember("host@example.com", "google-host");

        postPlayer(host, 999_999L, BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 삭제된_경기면_404() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Game game = saveGame(host);
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.saveAndFlush(game);

        postPlayer(host, game.getId(), BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 포지션_목록이_잘못되면_400() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Game game = saveGame(host);

        postPlayer(host, game.getId(), "{\"name\":\"홍길동\",\"positions\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_400_2"));
    }

    @Test
    void 이름이_비었거나_30자를_넘으면_400() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Game game = saveGame(host);

        postPlayer(host, game.getId(), "{\"name\":\"   \",\"positions\":[\"FW\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        postPlayer(host, game.getId(), "{\"name\":\"" + "가".repeat(31) + "\",\"positions\":[\"FW\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 실력이_1부터_10_밖이면_400() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Game game = saveGame(host);

        postPlayer(host, game.getId(), "{\"name\":\"홍길동\",\"positions\":[\"FW\"],\"skillLevel\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        postPlayer(host, game.getId(), "{\"name\":\"홍길동\",\"positions\":[\"FW\"],\"skillLevel\":11}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 토큰_없이_호출하면_401() throws Exception {
        mockMvc.perform(post("/games/1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }
}
