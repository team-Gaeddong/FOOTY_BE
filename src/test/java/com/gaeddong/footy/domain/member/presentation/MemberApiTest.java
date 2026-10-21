package com.gaeddong.footy.domain.member.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.auth.domain.repository.LoginCodeRepository;
import com.gaeddong.footy.domain.auth.domain.repository.RefreshTokenRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 보안 필터·예외 처리기·컨트롤러를 함께 확인한다. Redis 저장소는 목으로 대신한다.
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

    @Autowired
    private MemberPositionRepository memberPositionRepository;

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

    private void savePosition(Member member, Position position, int priority) {
        memberPositionRepository.save(MemberPosition.builder()
                .member(member)
                .position(position)
                .priority(priority)
                .build());
    }

    @Test
    void 내_프로필을_조회한다() throws Exception {
        Member member = saveMember();
        member.completeOnboarding("효오");
        savePosition(member, Position.MF, 2);
        savePosition(member, Position.FW, 1);

        mockMvc.perform(get("/members/me")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(member.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(member.getId()))
                .andExpect(jsonPath("$.email").value("player@gmail.com"))
                .andExpect(jsonPath("$.nickname").value("효오"))
                .andExpect(jsonPath("$.onboarded").value(true))
                .andExpect(jsonPath("$.positions[0].position").value("FW"))
                .andExpect(jsonPath("$.positions[1].position").value("MF"))
                .andExpect(jsonPath("$.positions[1].priority").value(2));
    }

    @Test
    void 탈퇴한_회원이면_404() throws Exception {
        Member member = saveMember();
        member.withdraw(LocalDateTime.now());

        mockMvc.perform(get("/members/me")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(member.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_404_1"));
    }

    @Test
    void 토큰_없이_조회하면_401() throws Exception {
        mockMvc.perform(get("/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }
}
