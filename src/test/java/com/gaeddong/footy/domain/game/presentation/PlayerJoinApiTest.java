package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
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
 * 선수 본인의 경기 참여 API를 보안 필터·예외 처리기와 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlayerJoinApiTest {

    private static final String BODY = "{\"positions\":[\"FW\",\"MF\"]}";

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

    private Member saveMember(String email, String providerId, String nickname) {
        Member member = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        member.completeOnboarding(nickname);
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame() {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        return gameRepository.save(Game.create(host, "일요일 풋살", LocalDateTime.now().plusDays(3), "풋살장", 12, 2,
                LocalDateTime.now()));
    }

    private ResultActions postJoin(Member member, Long gameId, String body) throws Exception {
        return mockMvc.perform(post("/games/{gameId}/players", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(member.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 회원이_경기에_참여하면_201과_참여한_선수() throws Exception {
        Game game = saveGame();
        Member member = saveMember("player@example.com", "google-player", "홍길동");

        String response = postJoin(member, game.getId(), BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.memberId").value(member.getId()))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.skillLevel").isEmpty())
                .andExpect(jsonPath("$.isSubstitute").value(false))
                .andExpect(jsonPath("$.positions[0]").value("FW"))
                .andExpect(jsonPath("$.positions[1]").value("MF"))
                .andExpect(jsonPath("$.joinedAt").exists())
                .andReturn().getResponse().getContentAsString();

        Long playerId = ((Number) JsonPath.read(response, "$.id")).longValue();
        GamePlayer saved = gamePlayerRepository.findByIdAndGameId(playerId, game.getId()).orElseThrow();
        assertThat(saved.getMember().getId()).isEqualTo(member.getId());
        assertThat(playerPositionRepository.findAllByGamePlayerIdOrderByPriorityAsc(playerId))
                .extracting(PlayerPosition::getPosition, PlayerPosition::getPriority)
                .containsExactly(tuple(Position.FW, 1), tuple(Position.MF, 2));
    }

    @Test
    void 이미_참여한_경기면_409() throws Exception {
        Game game = saveGame();
        Member member = saveMember("player@example.com", "google-player", "홍길동");
        postJoin(member, game.getId(), BODY).andExpect(status().isCreated());

        postJoin(member, game.getId(), BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_409_3"));
    }

    @Test
    void 모집_중이_아니면_409() throws Exception {
        Game game = saveGame();
        ReflectionTestUtils.setField(game, "status", GameStatus.CONFIRMED);
        gameRepository.saveAndFlush(game);
        Member member = saveMember("player@example.com", "google-player", "홍길동");

        postJoin(member, game.getId(), BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_409_1"));
    }

    @Test
    void 없는_경기면_404() throws Exception {
        Member member = saveMember("player@example.com", "google-player", "홍길동");

        postJoin(member, 999_999L, BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 삭제된_경기면_404() throws Exception {
        Game game = saveGame();
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.saveAndFlush(game);
        Member member = saveMember("player@example.com", "google-player", "홍길동");

        postJoin(member, game.getId(), BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 포지션이_없거나_잘못되면_400() throws Exception {
        Game game = saveGame();
        Member member = saveMember("player@example.com", "google-player", "홍길동");

        postJoin(member, game.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_400_2"));
        postJoin(member, game.getId(), "{\"positions\":[\"FW\",\"FW\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_400_2"));
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
