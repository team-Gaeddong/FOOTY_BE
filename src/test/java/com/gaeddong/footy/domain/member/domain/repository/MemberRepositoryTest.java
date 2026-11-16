package com.gaeddong.footy.domain.member.domain.repository;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    private static Member member(String email, String providerId) {
        return Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
    }

    @Test
    void 가입한_회원을_제공자_식별자로_찾는다() {
        memberRepository.saveAndFlush(member("player@gmail.com", "google-sub-1"));

        Member found = memberRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-sub-1").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getEmail()).isEqualTo("player@gmail.com");
        assertThat(found.getNickname()).isNull();
        assertThat(found.getRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(found.isOnboarded()).isFalse();
        assertThat(found.isDeleted()).isFalse();
        assertThat(memberRepository.existsByEmail("player@gmail.com")).isTrue();
    }

    @Test
    void 같은_구글_계정으로_두_번_가입할_수_없다() {
        memberRepository.saveAndFlush(member("a@gmail.com", "google-sub-1"));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(
                member("b@gmail.com", "google-sub-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_이메일로_두_번_가입할_수_없다() {
        memberRepository.saveAndFlush(member("a@gmail.com", "google-sub-1"));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(
                member("a@gmail.com", "google-sub-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_닉네임으로_두_명이_온보딩할_수_없다() {
        Member first = member("a@gmail.com", "google-sub-1");
        first.completeOnboarding("태균");
        memberRepository.saveAndFlush(first);
        Member second = member("b@gmail.com", "google-sub-2");
        second.completeOnboarding("태균");

        assertThatThrownBy(() -> memberRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 온보딩_전_회원은_닉네임이_없어도_여러_명_저장된다() {
        memberRepository.saveAndFlush(member("a@gmail.com", "google-sub-1"));
        memberRepository.saveAndFlush(member("b@gmail.com", "google-sub-2"));

        assertThat(memberRepository.count()).isEqualTo(2);
    }

    @Test
    void 탈퇴한_회원의_닉네임도_사용_중으로_본다() {
        Member withdrawn = member("a@gmail.com", "google-sub-1");
        withdrawn.completeOnboarding("태균");
        withdrawn.withdraw(LocalDateTime.of(2026, 11, 16, 12, 0));
        memberRepository.saveAndFlush(withdrawn);

        assertThat(memberRepository.existsByNickname("태균")).isTrue();
        assertThat(memberRepository.existsByNickname("효오")).isFalse();
    }
}
