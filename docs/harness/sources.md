# 원본 문서 등록부

2026-10-14 하네스 감사 기준. 기존 사본의 확인 날짜는 당시 작성자의 기록이며, 이번 감사에서 Notion·ERD 원본을 다시 확인했다는 뜻이 아니다.

| 원본 | URL / 식별자 | 로컬 사본 | 검증 상태 |
|---|---|---|---|
| Notion FOOTY > 기능·스프린트 일정 | 미확인 | `.claude/skills/footy-api-implement/references/feature-map.md` | 2026-10-14 작성된 사본만 확인 |
| Notion FOOTY > Github Commit 명세 | 미확인 | `.claude/skills/footy-git-workflow/SKILL.md` | 원본 문구 충돌 C-04 미결정 |
| Notion FOOTY > 1차 스크럼 회고록 | 미확인 | AGENTS.md의 요구사항 정리 규칙 | 원본 미대조 |
| ERD Cloud footy | https://www.erdcloud.com/d/e7RdzNfbJHXhjyKiu | `docs/erd/footy_erd.sql` | 2026-10-19 Chrome에서 Export(PK·FK 옵션 켬)와 대조: 컬럼·NULL·DEFAULT·FK 24개 일치, AUTO_INCREMENT·UNIQUE는 원본에 없음(C-12). member 변경(C-15) 원본·사본 반영. WebFetch로는 JS 렌더링이라 조회 불가 |
| GitHub FOOTY_BE | https://github.com/team-Gaeddong/FOOTY_BE | 로컬 Git 저장소 | 2026-10-14 API 조회: public, 기본 main, 보호 없음, 기존 workflow 0개 |

## 원본을 확인할 때

팀원이 실제 URL/페이지 ID를 제공하면 위 표에 기록한다. 이름만 보고 다른 페이지를 원본으로 선택하거나 URL을 만들어내지 않는다. 접근에 필요한 계정이나 토큰은 이 문서에 저장하지 않는다.

Notion은 해당 기능의 입력·출력·예외·담당 범위를 대조한다. ERD는 Export SQL과 로컬 DDL의 테이블, 컬럼, NULL, 기본값, 키·제약을 대조한다. 확인한 사람, 날짜, 원본 수정 시각/버전(제공되는 경우), 대조 범위를 남긴다. 차이는 `decisions.md` 형식으로 보고한다.

## 접근할 수 없을 때

접근 실패와 사본의 검증 한계를 보고한다. 사본만으로 가능한 코드 점검·빌드·하네스 유지보수는 계속한다. 확인되지 않은 원본에 의존하는 기능·스키마 변경만 보류하고, 필요한 부분의 내보내기나 링크를 요청한다. 사본을 최신 원본으로 인증하지 않는다.
