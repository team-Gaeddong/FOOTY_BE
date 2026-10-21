package com.gaeddong.footy.domain.member.service;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.MemberPosition;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberPositionRepository;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.domain.member.presentation.dto.MyProfileResponse;
import com.gaeddong.footy.domain.member.presentation.dto.MyProfileResponse.PositionResponse;
import com.gaeddong.footy.global.config.JpaAuditingConfig;
import com.gaeddong.footy.global.error.FootyException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({JpaAuditingConfig.class, MemberService.class})
class MemberProfileServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberPositionRepository memberPositionRepository;

    private Member saveMember(String email, String providerId) {
        return memberRepository.saveAndFlush(Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build());
    }

    private void savePosition(Member member, Position position, int priority) {
        memberPositionRepository.saveAndFlush(MemberPosition.builder()
                .member(member)
                .position(position)
                .priority(priority)
                .build());
    }

    @Test
    void 선호_포지션을_우선순위_순으로_반환한다() {
        Member member = saveMember("player@gmail.com", "google-sub-1");
        savePosition(member, Position.MF, 2);
        savePosition(member, Position.FW, 1);

        MyProfileResponse profile = memberService.getMyProfile(member.getId());

        assertThat(profile.memberId()).isEqualTo(member.getId());
        assertThat(profile.email()).isEqualTo("player@gmail.com");
        assertThat(profile.nickname()).isNull();
        assertThat(profile.onboarded()).isFalse();
        assertThat(profile.positions()).containsExactly(
                new PositionResponse(Position.FW, 1),
                new PositionResponse(Position.MF, 2)
        );
    }

    @Test
    void 없는_회원이면_예외가_발생한다() {
        assertThatThrownBy(() -> memberService.getMyProfile(-1L))
                .isInstanceOfSatisfying(FootyException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 탈퇴한_회원이면_예외가_발생한다() {
        Member member = saveMember("left@gmail.com", "google-sub-2");
        member.withdraw(LocalDateTime.now());
        memberRepository.saveAndFlush(member);

        assertThatThrownBy(() -> memberService.getMyProfile(member.getId()))
                .isInstanceOfSatisfying(FootyException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
