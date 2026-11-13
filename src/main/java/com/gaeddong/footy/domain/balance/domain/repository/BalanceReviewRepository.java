package com.gaeddong.footy.domain.balance.domain.repository;

import com.gaeddong.footy.domain.balance.domain.BalanceReview;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceReviewRepository extends JpaRepository<BalanceReview, Long> {

    boolean existsByGameIdAndWriterId(Long gameId, Long writerId);

    long countByGameId(Long gameId);

    @EntityGraph(attributePaths = "writer")
    List<BalanceReview> findAllByGameIdOrderByIdAsc(Long gameId);
}
