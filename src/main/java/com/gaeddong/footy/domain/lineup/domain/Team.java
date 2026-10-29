package com.gaeddong.footy.domain.lineup.domain;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 라인업에 속한 팀. 스키마는 docs/erd/footy_erd.sql의 team을 따른다.
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lineup_id", nullable = false, foreignKey = @ForeignKey(name = "fk_team_lineup"))
    private Lineup lineup;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "formation", length = 20)
    private String formation;

    @Column(name = "total_skill")
    private Integer totalSkill;

    public void changeFormation(String formation) {
        this.formation = formation;
    }
}
