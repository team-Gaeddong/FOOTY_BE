package com.gaeddong.footy.domain.member.presentation.dto;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.MemberPosition;
import com.gaeddong.footy.domain.member.domain.Position;

import java.math.BigDecimal;
import java.util.List;

public record MyProfileResponse(
        Long memberId,
        String email,
        String nickname,
        BigDecimal rating,
        boolean onboarded,
        List<PositionResponse> positions
) {

    public static MyProfileResponse of(Member member, List<MemberPosition> positions) {
        return new MyProfileResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRating(),
                member.isOnboarded(),
                positions.stream().map(PositionResponse::from).toList()
        );
    }

    public record PositionResponse(Position position, int priority) {

        public static PositionResponse from(MemberPosition memberPosition) {
            return new PositionResponse(memberPosition.getPosition(), memberPosition.getPriority());
        }
    }
}
