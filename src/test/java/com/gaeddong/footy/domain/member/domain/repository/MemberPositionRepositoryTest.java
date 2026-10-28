package com.gaeddong.footy.domain.member.domain.repository;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.MemberPosition;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class MemberPositionRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberPositionRepository memberPositionRepository;

    private Member saveMember(String email, String providerId) {
        return memberRepository.save(Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
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
    void 회원의_선호_포지션만_지운다() {
        Member me = saveMember("a@gmail.com", "google-sub-1");
        Member other = saveMember("b@gmail.com", "google-sub-2");
        savePosition(me, Position.FW, 1);
        savePosition(me, Position.MF, 2);
        savePosition(other, Position.GK, 1);

        memberPositionRepository.deleteAllByMemberId(me.getId());

        assertThat(memberPositionRepository.findAllByMemberIdOrderByPriorityAsc(me.getId())).isEmpty();
        assertThat(memberPositionRepository.findAllByMemberIdOrderByPriorityAsc(other.getId()))
                .extracting(MemberPosition::getPosition)
                .containsExactly(Position.GK);
    }
}
