package com.gaeddong.footy.domain.lineup.domain;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.member.domain.Position;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 팀 배정과 전술 배치. 스키마는 docs/erd/footy_erd.sql의 team_member를 따른다.
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Table(name = "team_member")
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, foreignKey = @ForeignKey(name = "fk_team_member_team"))
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_player_id", nullable = false, foreignKey = @ForeignKey(name = "fk_team_member_player"))
    private GamePlayer gamePlayer;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 10)
    private Position position;

    // 전술 배치도에서 정하기 전에는 NULL
    @Column(name = "pos_x", precision = 5, scale = 2)
    private BigDecimal posX;

    @Column(name = "pos_y", precision = 5, scale = 2)
    private BigDecimal posY;

    // 좌표는 경기장 크기 대비 퍼센트(0~100)다
    public void place(BigDecimal posX, BigDecimal posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public void unplace() {
        this.posX = null;
        this.posY = null;
    }
}
