package com.gaeddong.footy.domain.poll.domain.repository;

import com.gaeddong.footy.domain.poll.domain.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    List<Poll> findAllByGameIdOrderByCreatedAtDescIdDesc(Long gameId);

    Optional<Poll> findByIdAndGameId(Long id, Long gameId);
}
