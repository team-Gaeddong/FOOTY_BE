package com.gaeddong.footy.domain.lineup.domain;

import com.gaeddong.footy.domain.game.domain.Game;
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

import java.time.LocalDateTime;

/**
 * 경기의 팀 구성안. 스키마는 docs/erd/footy_erd.sql의 lineup을 따른다.
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Table(name = "lineup")
public class Lineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lineup_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lineup_game"))
    private Game game;

    // 다시 짜기 때 증가한다
    @Column(name = "version", nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private LineupMethod method;

    @Builder.Default
    @Column(name = "is_confirmed", nullable = false)
    private boolean confirmed = false;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // 생성 시각은 호출하는 쪽이 Clock으로 만들어 넘긴다(D-08)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
