package com.gaeddong.footy.domain.member.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.auth.domain.repository.LoginCodeRepository;
import com.gaeddong.footy.domain.auth.domain.repository.RefreshTokenRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 온보딩(회원가입)·탈퇴 API와 온보딩 전 접근 차단을 보안 필터·예외 처리기와 함께 확인한다. Redis 저장소는 목으로 대신한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

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

    private String bearer(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }

    @Test
    void 온보딩하면_204() throws Exception {
        Member member = saveMember();

        mockMvc.perform(patch("/members/me/onboarding")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\" 태균 \"}"))
                .andExpect(status().isNoContent());

        Member found = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(found.getNickname()).isEqualTo("태균");
        assertThat(found.isOnboarded()).isTrue();
    }

    @Test
    void 토큰_없이_온보딩하면_401() throws Exception {
        mockMvc.perform(patch("/members/me/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"태균\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 닉네임이_없으면_400() throws Exception {
        Member member = saveMember();

        mockMvc.perform(patch("/members/me/onboarding")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_400_1"));
    }

    @Test
    void 본문이_JSON이_아니면_400() throws Exception {
        Member member = saveMember();

        mockMvc.perform(patch("/members/me/onboarding")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    @Test
    void 이미_온보딩했으면_409() throws Exception {
        Member member = saveMember();
        member.completeOnboarding("태균");

        mockMvc.perform(patch("/members/me/onboarding")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"다른닉네임\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_409_1"));
    }

    @Test
    void 온보딩_전_회원은_허용된_API_외에는_403() throws Exception {
        Member member = saveMember();

        // 컨트롤러까지 갔다면 404가 나온다. 필터에서 먼저 막혀야 한다.
        mockMvc.perform(get("/no-such-path").header("Authorization", bearer(member)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_403_1"));
    }

    @Test
    void 온보딩_전에도_로그아웃할_수_있다() throws Exception {
        Member member = saveMember();

        mockMvc.perform(post("/auth/logout").header("Authorization", bearer(member)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 온보딩한_회원은_막지_않는다() throws Exception {
        Member member = saveMember();
        member.completeOnboarding("태균");

        mockMvc.perform(get("/no-such-path").header("Authorization", bearer(member)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GLOBAL_404_1"));
    }

    @Test
    void 탈퇴하면_204이고_토큰을_무효화한다() throws Exception {
        // 온보딩 전 회원도 탈퇴할 수 있다(D-05e).
        Member member = saveMember();
        String accessToken = jwtProvider.createAccessToken(member.getId());

        mockMvc.perform(delete("/members/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        Member found = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getDeletedAt()).isNotNull();
        verify(refreshTokenRepository).deleteByMemberId(member.getId());
        verify(blacklistRepository).save(eq(accessToken), any());
    }

    @Test
    void 토큰_없이_탈퇴하면_401() throws Exception {
        mockMvc.perform(delete("/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }
}
