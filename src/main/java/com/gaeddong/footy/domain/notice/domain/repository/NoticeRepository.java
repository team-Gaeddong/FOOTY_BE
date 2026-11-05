package com.gaeddong.footy.domain.notice.domain.repository;

import com.gaeddong.footy.domain.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
