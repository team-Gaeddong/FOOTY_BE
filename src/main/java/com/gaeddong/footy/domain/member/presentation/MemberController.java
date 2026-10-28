package com.gaeddong.footy.domain.member.presentation;

import com.gaeddong.footy.domain.member.facade.MemberFacade;
import com.gaeddong.footy.domain.member.presentation.dto.MyProfileResponse;
import com.gaeddong.footy.domain.member.presentation.dto.request.OnboardingRequest;
import com.gaeddong.footy.domain.member.presentation.dto.request.UpdatePositionsRequest;
import com.gaeddong.footy.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ResponseEntity<Void> completeOnboarding(@AuthenticationPrincipal Long memberId,
                                                   @RequestBody OnboardingRequest request) {
        memberService.completeOnboarding(memberId, request.nickname());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/positions")
    public ResponseEntity<Void> updatePositions(@AuthenticationPrincipal Long memberId,
                                                @RequestBody UpdatePositionsRequest request) {
        memberService.updatePositions(memberId, request.positions());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    // 탈퇴하면서 access 토큰도 무효화하므로 credentials(토큰)가 필요하다
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long memberId, Authentication authentication) {
        memberFacade.withdraw(memberId, (String) authentication.getCredentials());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MyProfileResponse getMyProfile(@AuthenticationPrincipal Long memberId) {
        return memberService.getMyProfile(memberId);
    }
}
