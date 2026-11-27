package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerListService {

    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPositionRepository playerPositionRepository;

    /**
     * 경기 참가 선수를 참여 순으로 조회한다. 로그인한 회원 누구나 볼 수 있고, 실력은 파티장에게만 보인다(D-20).
     * 회원 평점은 파티장과 그 경기 참가자에게만 보인다(D-42).
     */
    @Transactional(readOnly = true)
    public List<ListedPlayer> list(Long requesterId, Long gameId) {
        Game game = gameReader.getGame(gameId);
        boolean skillVisible = game.isHostedBy(requesterId);

        List<GamePlayer> players = gamePlayerRepository.findWithMemberByGameIdOrderByJoinedAtAscIdAsc(gameId);
        if (players.isEmpty()) {
            return List.of();
        }
        boolean ratingVisible = skillVisible || isParticipant(players, requesterId);
        Map<Long, List<Position>> positionsByPlayerId = findPositionsByPlayerId(players);

        return players.stream()
                .map(player -> new ListedPlayer(
                        player,
                        positionsByPlayerId.getOrDefault(player.getId(), List.of()),
                        skillVisible ? player.getSkillLevel() : null,
                        ratingVisible ? ratingOf(player) : null))
                .toList();
    }

    private boolean isParticipant(List<GamePlayer> players, Long requesterId) {
        return players.stream()
                .map(GamePlayer::getMember)
                .anyMatch(member -> member != null && member.getId().equals(requesterId));
    }

    // 비회원 선수, 탈퇴한 회원, 아직 평점이 없는 회원은 null이다
    private BigDecimal ratingOf(GamePlayer player) {
        Member member = player.getMember();
        if (member == null || member.isDeleted() || !member.hasRating()) {
            return null;
        }
        return member.getRating();
    }

    // 선수마다 따로 조회하지 않도록 포지션을 한 번에 가져온다. 선수별 목록은 우선순위 순이다
    private Map<Long, List<Position>> findPositionsByPlayerId(List<GamePlayer> players) {
        return playerPositionRepository
                .findAllByGamePlayerIdInOrderByPriorityAsc(players.stream().map(GamePlayer::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(position -> position.getGamePlayer().getId(),
                        Collectors.mapping(PlayerPosition::getPosition, Collectors.toList())));
    }

    /**
     * skillLevel·rating은 요청자에게 보여줄 값이다. skillLevel은 파티장이 아니면 NULL,
     * rating(회원 평점)은 파티장도 참가자도 아니거나 평점이 없으면 NULL.
     */
    public record ListedPlayer(GamePlayer player, List<Position> positions, Integer skillLevel, BigDecimal rating) {
    }
}
