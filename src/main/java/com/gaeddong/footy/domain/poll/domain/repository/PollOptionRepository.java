package com.gaeddong.footy.domain.poll.domain.repository;

import com.gaeddong.footy.domain.poll.domain.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    // 만든 순서(선택지 입력 순서)
    List<PollOption> findAllByPollIdOrderByIdAsc(Long pollId);
}
