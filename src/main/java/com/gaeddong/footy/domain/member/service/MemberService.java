package com.gaeddong.footy.domain.member.service;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.repository.MemberPositionRepository;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.domain.member.presentation.dto.MyProfileResponse;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 10;

    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;

    /**
     * 회원가입은 첫 Google 로그인 뒤의 온보딩이다(D-05c). Sprint 1은 닉네임만 받는다(D-05e).
     * 닉네임은 앞뒤 공백을 제거한 뒤 2~10자이며 중복을 허용한다.
     */
    @Transactional
    public void completeOnboarding(Long memberId, String nickname) {
        String trimmed = validateNickname(nickname);
        Member member = memberRepository.findById(memberId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(MemberErrorCode.MEMBER_NOT_FOUND));
        if (member.isOnboarded()) {
            throw new FootyException(MemberErrorCode.ALREADY_ONBOARDED);
        }
        member.completeOnboarding(trimmed);
    }

    // 탈퇴한 회원은 없는 회원으로 본다
    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long memberId) {
        Member member = memberRepository.findByIdAndDeletedFalse(memberId)
                .orElseThrow(() -> new FootyException(MemberErrorCode.MEMBER_NOT_FOUND));
        return MyProfileResponse.of(member, memberPositionRepository.findAllByMemberIdOrderByPriorityAsc(memberId));
    }

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
