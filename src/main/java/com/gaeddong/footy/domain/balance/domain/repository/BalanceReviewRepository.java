package com.gaeddong.footy.domain.balance.domain.repository;

import com.gaeddong.footy.domain.balance.domain.BalanceReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceReviewRepository extends JpaRepository<BalanceReview, Long> {

    boolean existsByGameIdAndWriterId(Long gameId, Long writerId);
}
