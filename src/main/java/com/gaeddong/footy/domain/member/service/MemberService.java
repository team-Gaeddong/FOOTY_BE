package com.gaeddong.footy.domain.member.service;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.MemberPosition;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.PositionPriorities;
import com.gaeddong.footy.domain.member.domain.repository.MemberPositionRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.domain.member.presentation.dto.MyProfileResponse;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 10;

    private final MemberReader memberReader;
    private final MemberPositionRepository memberPositionRepository;
    private final Clock clock;

    /**
     * 회원가입은 첫 Google 로그인 뒤의 온보딩이다(D-05c). Sprint 1은 닉네임만 받는다(D-05e).
     * 닉네임은 앞뒤 공백을 제거한 뒤 2~10자이며 중복을 허용한다.
     */
    @Transactional
    public void completeOnboarding(Long memberId, String nickname) {
        String trimmedNickname = validateNickname(nickname);
        Member member = memberReader.getActiveMember(memberId);
        if (member.isOnboarded()) {
            throw new FootyException(MemberErrorCode.ALREADY_ONBOARDED);
        }
        member.completeOnboarding(trimmedNickname);
    }

    // 탈퇴한 회원은 없는 회원으로 본다
    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long memberId) {
        Member member = memberReader.getActiveMember(memberId);
        return MyProfileResponse.of(member, memberPositionRepository.findAllByMemberIdOrderByPriorityAsc(memberId));
    }

    /**
     * 선호 포지션을 전부 교체한다. 순서가 곧 우선순위다(D-13).
     */
    @Transactional
    public void updatePositions(Long memberId, List<Position> positions) {
        PositionPriorities priorities = new PositionPriorities(positions);
        Member member = memberReader.getActiveMember(memberId);

        memberPositionRepository.deleteAllByMemberId(memberId);
        memberPositionRepository.saveAll(MemberPosition.allOf(member, priorities));
    }

    /**
     * 탈퇴는 소프트 삭제다. 같은 Google 계정으로 다시 로그인하면 복구된다(D-05d).
     */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberReader.getActiveMember(memberId);
        member.withdraw(LocalDateTime.now(clock));
    }

    // 길이는 앞뒤 공백을 뺀 뒤 코드 포인트 수로 센다(char 수가 아니다)
    private String validateNickname(String nickname) {
        if (nickname == null) {
            throw new FootyException(MemberErrorCode.INVALID_NICKNAME);
        }
        String trimmed = nickname.strip();
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length < NICKNAME_MIN_LENGTH || length > NICKNAME_MAX_LENGTH) {
            throw new FootyException(MemberErrorCode.INVALID_NICKNAME);
        }
        return trimmed;
    }
}
