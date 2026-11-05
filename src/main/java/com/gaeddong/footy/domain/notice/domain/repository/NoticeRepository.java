package com.gaeddong.footy.domain.notice.domain.repository;

import com.gaeddong.footy.domain.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByGameIdOrderByCreatedAtDescIdDesc(Long gameId);

    Optional<Notice> findByIdAndGameId(Long id, Long gameId);
}
