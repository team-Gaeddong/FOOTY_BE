package com.gaeddong.footy.domain.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 회원. 스키마는 docs/erd/footy_erd.sql의 member, UNIQUE는 docs/erd/constraints.md를 따른다.
 * 첫 Google 로그인 때 생성되고, 온보딩을 마치면 nickname이 채워진다(C-15).
 */
@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_member_provider", columnNames = {"provider", "provider_id"})
        }
)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Column(name = "rating", nullable = false, precision = 5, scale = 2)
    private BigDecimal rating;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "is_onboarded", nullable = false)
    private boolean onboarded;

    protected Member() {
    }

    private Member(String email, Provider provider, String providerId, LocalDateTime createdAt) {
        this.email = Objects.requireNonNull(email);
        this.provider = Objects.requireNonNull(provider);
        this.providerId = Objects.requireNonNull(providerId);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.rating = BigDecimal.ZERO;
        this.deleted = false;
        this.onboarded = false;
    }

    /**
     * 첫 소셜 로그인 때 온보딩 전 회원을 만든다.
     */
    public static Member signUp(String email, Provider provider, String providerId, LocalDateTime now) {
        return new Member(email, provider, providerId, now);
    }

    public void completeOnboarding(String nickname) {
        if (onboarded) {
            throw new IllegalStateException("이미 온보딩을 마친 회원입니다.");
        }
        this.nickname = Objects.requireNonNull(nickname);
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
        this.deletedAt = Objects.requireNonNull(now);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Provider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public boolean isOnboarded() {
        return onboarded;
    }
}
