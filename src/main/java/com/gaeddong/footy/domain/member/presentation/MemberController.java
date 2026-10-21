package com.gaeddong.footy.domain.member.presentation;

import com.gaeddong.footy.domain.member.presentation.dto.request.OnboardingRequest;
import com.gaeddong.footy.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PatchMapping("/me/onboarding")
    public ResponseEntity<Void> completeOnboarding(Authentication authentication,
                                                   @RequestBody OnboardingRequest request) {
        memberService.completeOnboarding((Long) authentication.getPrincipal(), request.nickname());
        return ResponseEntity.noContent().build();
    }
}
