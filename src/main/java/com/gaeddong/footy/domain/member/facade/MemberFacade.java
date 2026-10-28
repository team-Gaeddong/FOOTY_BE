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
     * 로그아웃과 같은 방식으로 토큰을 먼저 무효화한 뒤 탈퇴 처리한다(D-05b).
     * 토큰 무효화(Redis)가 실패하면 탈퇴하지 않고, 탈퇴(DB)가 실패하면 "로그아웃됐지만 탈퇴 안 됨"으로 남아 다시 시도할 수 있다.
     */
    public void withdraw(Long memberId, String accessToken) {
        authService.logout(memberId, accessToken);
        memberService.withdraw(memberId);
    }
}
