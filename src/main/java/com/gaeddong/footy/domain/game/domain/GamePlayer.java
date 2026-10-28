package com.gaeddong.footy.domain.game.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "game_player",
        uniqueConstraints = @UniqueConstraint(name = "uk_game_player_member", columnNames = {"game_id", "member_id"})
)
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_player_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_game_player_game"))
    private Game game;

    // 비회원 선수는 NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_game_player_member"))
    private Member member;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    // 파티장 평가 전에는 NULL
    @Column(name = "skill_level")
    private Integer skillLevel;

    @Column(name = "is_substitute", nullable = false)
    private boolean substitute = false;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    // 참여 시각은 호출하는 쪽이 Clock으로 만들어 넘긴다(D-08)
    public GamePlayer(Game game, Member member, String name, LocalDateTime joinedAt) {
        this.game = game;
        this.member = member;
        this.name = name;
        this.joinedAt = joinedAt;
    }

    private GamePlayer(Game game, String name, Integer skillLevel, LocalDateTime joinedAt) {
        this.game = game;
        this.name = name;
        this.skillLevel = skillLevel;
        this.joinedAt = joinedAt;
    }

    /**
     * 파티장이 비회원 선수를 등록한다(member_id NULL). 실력은 생략할 수 있다.
     */
    public static GamePlayer registerGuest(Game game, String name, Integer skillLevel, LocalDateTime joinedAt) {
        return new GamePlayer(game, name, skillLevel, joinedAt);
    }

    public void changeSkillLevel(int skillLevel) {
        this.skillLevel = skillLevel;
    }
}
