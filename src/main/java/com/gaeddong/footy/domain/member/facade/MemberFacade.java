package com.gaeddong.footy.domain.member.facade;

import com.gaeddong.footy.domain.auth.service.AuthService;
import com.gaeddong.footy.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원 도메인과 인증 도메인을 함께 쓰는 흐름을 묶는다.
 */
@Component
@RequiredArgsConstructor
public class MemberFacade {

    private final MemberService memberService;
    private final AuthService authService;

    /**
     * 회원을 탈퇴 처리한 뒤 로그아웃과 같은 방식으로 토큰을 무효화한다(D-05b).
     */
    public void withdraw(Long memberId, String accessToken) {
        memberService.withdraw(memberId);
        authService.logout(memberId, accessToken);
    }
}
