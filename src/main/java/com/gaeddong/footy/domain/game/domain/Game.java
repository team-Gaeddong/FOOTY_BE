package com.gaeddong.footy.domain.game.domain;

import com.gaeddong.footy.domain.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** docs/erd/footy_erd.sql의 game 테이블과 일치하는 경기 엔티티다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private Member host;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "play_date", nullable = false)
    private LocalDateTime playDate;

    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "team_count", nullable = false)
    private int teamCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameStatus status;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Game(Member host, String title, LocalDateTime playDate, String location, int maxPlayers,
                 int teamCount, LocalDateTime createdAt) {
        this.host = host;
        this.title = title;
        this.playDate = playDate;
        this.location = location;
        this.maxPlayers = maxPlayers;
        this.teamCount = teamCount;
        this.status = GameStatus.RECRUITING;
        this.deleted = false;
        this.createdAt = createdAt;
    }

    public static Game create(Member host, String title, LocalDateTime playDate, String location,
                              int maxPlayers, int teamCount, LocalDateTime createdAt) {
        return new Game(host, title, playDate, location, maxPlayers, teamCount, createdAt);
    }
}
