package com.gaeddong.footy.domain.member.presentation;

import com.gaeddong.footy.domain.member.facade.MemberFacade;
import com.gaeddong.footy.domain.member.presentation.dto.MyProfileResponse;
import com.gaeddong.footy.domain.member.presentation.dto.request.OnboardingRequest;
import com.gaeddong.footy.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberFacade memberFacade;

    @PatchMapping("/me/onboarding")
    public ResponseEntity<Void> completeOnboarding(Authentication authentication,
                                                   @RequestBody OnboardingRequest request) {
        memberService.completeOnboarding((Long) authentication.getPrincipal(), request.nickname());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(Authentication authentication) {
        memberFacade.withdraw((Long) authentication.getPrincipal(), (String) authentication.getCredentials());
        return ResponseEntity.noContent().build();
    }

    // JwtAuthenticationFilter가 회원 ID를 principal로 둔다
    @GetMapping("/me")
    public MyProfileResponse getMyProfile(@AuthenticationPrincipal Long memberId) {
        return memberService.getMyProfile(memberId);
    }
}
