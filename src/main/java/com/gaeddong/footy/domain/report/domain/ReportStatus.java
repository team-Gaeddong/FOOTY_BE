package com.gaeddong.footy.domain.report.domain;

/**
 * 신고 처리 상태. 접수하면 PENDING이고, DONE으로 바꾸는 처리 API는 아직 없다(D-30).
 */
public enum ReportStatus {
    PENDING,
    DONE
}
