package com.gaeddong.footy.domain.auth.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.auth.domain.repository.LoginCodeRepository;
import com.gaeddong.footy.domain.auth.domain.repository.RefreshTokenRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.security.jwt.JwtProperties;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 보안 필터·예외 처리기·컨트롤러를 함께 확인한다. Redis 저장소는 목으로 대신한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private LoginCodeRepository loginCodeRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member saveMember() {
        return memberRepository.save(Member.builder()
                .email("player@gmail.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub-1")
                .build());
    }

    @Test
    void 로그인_코드를_토큰으로_교환한다() throws Exception {
        Member member = saveMember();
        given(loginCodeRepository.consume("code")).willReturn(Optional.of(member.getId()));

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.onboarded").value(false));
    }

    @Test
    void 잘못된_로그인_코드는_401_에러_형식으로_응답한다() throws Exception {
        given(loginCodeRepository.consume(anyString())).willReturn(Optional.empty());

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_1"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void 토큰_없이_보호된_API를_호출하면_401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 로그아웃하면_204() throws Exception {
        String accessToken = jwtProvider.createAccessToken(1L);

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        verify(refreshTokenRepository).deleteByMemberId(1L);
        verify(blacklistRepository).save(eq(accessToken), any());
    }

    @Test
    void 로그아웃한_access_토큰은_거부한다() throws Exception {
        String accessToken = jwtProvider.createAccessToken(1L);
        given(blacklistRepository.contains(accessToken)).willReturn(true);

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_5"));
    }

    @Test
    void Google_로그인_시작은_Google로_리다이렉트한다() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        startsWith("https://accounts.google.com/o/oauth2/v2/auth")));
    }

    @Test
    void 만료된_access가_헤더에_있어도_재발급_API는_막지_않는다() throws Exception {
        String expiredAccessToken = new JwtProvider(jwtProperties,
                Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC))
                .createAccessToken(1L);

        // 필터가 막았다면 AUTH_401_3(만료)이 나온다. 서비스까지 가서 refresh 검증에 실패해야 한다.
        mockMvc.perform(post("/auth/reissue")
                        .header("Authorization", "Bearer " + expiredAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"not-a-jwt\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_2"));
    }

    @Test
    void 없는_경로는_404() throws Exception {
        mockMvc.perform(get("/no-such-path")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GLOBAL_404_1"));
    }

    @Test
    void 지원하지_않는_메서드는_405() throws Exception {
        mockMvc.perform(get("/auth/logout")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("GLOBAL_405_1"));
    }
}
