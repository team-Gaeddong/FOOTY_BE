package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.MemberPosition;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberPositionRepository;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 경기 생성 정책(D-14)을 보안 필터·예외 처리기·DB와 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameCreateApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberPositionRepository memberPositionRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private PlayerPositionRepository playerPositionRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member saveHost() {
        Member host = Member.builder()
                .email("host@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-host")
                .build();
        host.completeOnboarding("파티장");
        return memberRepository.saveAndFlush(host);
    }

    private String body(LocalDateTime playDate, int maxPlayers, Integer teamCount) {
        return "{\"title\":\"일요일 풋살\",\"playDate\":\"" + playDate.withNano(0) + "\",\"location\":\"풋살장\","
                + "\"maxPlayers\":" + maxPlayers + (teamCount == null ? "" : ",\"teamCount\":" + teamCount) + "}";
    }

    private MvcResult create(Member host, String body) throws Exception {
        return mockMvc.perform(post("/games")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(host.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Test
    void 경기를_만들면_파티장이_참가하고_선호_포지션이_복사된다() throws Exception {
        Member host = saveHost();
        memberPositionRepository.save(MemberPosition.builder().member(host).position(Position.MF).priority(1).build());
        memberPositionRepository.save(MemberPosition.builder().member(host).position(Position.DF).priority(2).build());

        MvcResult result = create(host, body(LocalDateTime.now().plusDays(3), 12, null));

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        Long gameId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).has("isDeleted")).isFalse();

        List<GamePlayer> players = gamePlayerRepository.findAll().stream()
                .filter(player -> player.getGame().getId().equals(gameId))
                .toList();
        assertThat(players).hasSize(1);
        assertThat(players.get(0).getMember().getId()).isEqualTo(host.getId());
        assertThat(players.get(0).getName()).isEqualTo("파티장");
        assertThat(playerPositionRepository.findAllByGamePlayerIdOrderByPriorityAsc(players.get(0).getId()))
                .extracting(PlayerPosition::getPosition, PlayerPosition::getPriority)
                .containsExactly(tuple(Position.MF, 1), tuple(Position.DF, 2));
    }

    @Test
    void 정책을_어기면_GAME_400() throws Exception {
        Member host = saveHost();
        LocalDateTime future = LocalDateTime.now().plusDays(3);

        mockMvc.perform(post("/games")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(host.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(future, 31, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GAME_400_1"));
        mockMvc.perform(post("/games")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(host.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(future, 10, 11)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GAME_400_2"));
        mockMvc.perform(post("/games")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(host.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalDateTime.now().minusDays(1), 10, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GAME_400_3"));
        assertThat(gamePlayerRepository.count()).isZero();
    }
}
