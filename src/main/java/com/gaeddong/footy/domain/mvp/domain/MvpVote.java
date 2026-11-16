package com.gaeddong.footy.domain.mvp.domain;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 경기 MVP 투표. 스키마는 docs/erd/footy_erd.sql의 mvp_vote를 따른다.
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Table(
        name = "mvp_vote",
        uniqueConstraints = @UniqueConstraint(name = "uk_mvp_vote", columnNames = {"game_id", "voter_id"})
)
public class MvpVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mvp_vote_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mvp_vote_game"))
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mvp_vote_voter"))
    private Member voter;

    // MVP로 뽑힌 참가 선수
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_player_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mvp_vote_player"))
    private GamePlayer gamePlayer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
