package com.gaeddong.footy.domain.member.service;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberPositionRepository;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MemberServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberService memberService = new MemberService(memberRepository, mock(MemberPositionRepository.class),
            Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));

    private static Member newMember() {
        return Member.builder()
                .email("player@gmail.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub-1")
                .build();
    }

    @Test
    void 온보딩하면_공백을_제거한_닉네임이_저장된다() {
        Member member = newMember();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.completeOnboarding(1L, "  태균  ");

        assertThat(member.getNickname()).isEqualTo("태균");
        assertThat(member.isOnboarded()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"가나", "가나다라마바사아자차"})
    void 닉네임은_2자_이상_10자_이하다(String nickname) {
        Member member = newMember();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.completeOnboarding(1L, nickname);

        assertThat(member.getNickname()).isEqualTo(nickname);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "가", " 가 ", "가나다라마바사아자차카"})
    void 닉네임_길이가_맞지_않으면_400(String nickname) {
        assertThatThrownBy(() -> memberService.completeOnboarding(1L, nickname))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.INVALID_NICKNAME);
        verify(memberRepository, never()).findById(any());
    }

    @Test
    void 없는_회원이면_404() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.completeOnboarding(1L, "태균"))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 탈퇴한_회원이면_404() {
        Member member = newMember();
        member.withdraw(NOW);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.completeOnboarding(1L, "태균"))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 이미_온보딩한_회원이면_409() {
        Member member = newMember();
        member.completeOnboarding("태균");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.completeOnboarding(1L, "다른닉네임"))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.ALREADY_ONBOARDED);
        assertThat(member.getNickname()).isEqualTo("태균");
    }

    @Test
    void 탈퇴하면_소프트_삭제된다() {
        Member member = newMember();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.withdraw(1L);

        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void 이미_탈퇴한_회원이_탈퇴하면_404() {
        Member member = newMember();
        member.withdraw(NOW.minusDays(1));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.withdraw(1L))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        assertThat(member.getDeletedAt()).isEqualTo(NOW.minusDays(1));
    }

    @Test
    void 없는_회원이_탈퇴하면_404() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.withdraw(1L))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }
}
