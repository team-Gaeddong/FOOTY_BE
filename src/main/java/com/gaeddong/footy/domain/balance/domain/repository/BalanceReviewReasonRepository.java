package com.gaeddong.footy.domain.balance.domain.repository;

import com.gaeddong.footy.domain.balance.domain.BalanceReviewReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BalanceReviewReasonRepository extends JpaRepository<BalanceReviewReason, Long> {

    List<BalanceReviewReason> findAllByBalanceReviewIdIn(Collection<Long> balanceReviewIds);
}
