package com.gaeddong.footy.domain.member.presentation;

import com.gaeddong.footy.domain.member.presentation.dto.MyProfileResponse;
import com.gaeddong.footy.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // JwtAuthenticationFilter가 회원 ID를 principal로 둔다
    @GetMapping("/me")
    public MyProfileResponse getMyProfile(@AuthenticationPrincipal Long memberId) {
        return memberService.getMyProfile(memberId);
    }
}
