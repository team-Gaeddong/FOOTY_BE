package com.gaeddong.footy.domain.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 회원. 스키마는 docs/erd/footy_erd.sql의 member, UNIQUE는 docs/erd/constraints.md를 따른다.
 * 첫 Google 로그인 때 생성되고, 온보딩을 마치면 nickname이 채워진다(C-15).
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_member_provider", columnNames = {"provider", "provider_id"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Builder.Default
    @Column(name = "rating", nullable = false, precision = 5, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Builder.Default
    @Column(name = "is_onboarded", nullable = false)
    private boolean onboarded = false;

    public void completeOnboarding(String nickname) {
        if (onboarded) {
            throw new IllegalStateException("이미 온보딩을 마친 회원입니다.");
        }
        this.nickname = nickname;
        this.onboarded = true;
    }

    /**
     * 탈퇴는 소프트 삭제다(is_deleted, deleted_at).
     */
    public void withdraw(LocalDateTime now) {
        if (deleted) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }
        this.deleted = true;
        this.deletedAt = now;
    }
}
