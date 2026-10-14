# 기능 ↔ 스프린트 ↔ 테이블 매핑

스냅샷: Notion `FOOTY > 기능`, `FOOTY > 스프린트 일정` (2026-10-14 확인). 원본이 바뀌었으면 원본을 따른다. 날짜는 기존 사본의 작성 기록이다. 원본 URL·이번 재검증 범위는 `docs/harness/sources.md`를 확인한다.

- 기능 이름과 담당자는 스프린트 일정 기준이다. 기능 페이지의 이름과 다른 항목이 있다(`docs/harness/decisions.md` C-07).
- "Notion 설명"은 기능 페이지 본문 그대로다. 비어 있으면 설명이 없는 것이다.
- 주차는 전체 9주 기준이다(Sprint 1 = 1~2주, Sprint 2 = 3~4주, Sprint 3 = 5~6주, Sprint 4 = 7~8주, 마무리 = 9주).

| 기능 | 스프린트 | 주차 | 담당 | 주요 테이블 | Notion 설명 |
|---|---|---|---|---|---|
| 요구사항 정리 및 공통 구조 설계 | Sprint 1 | 1 | 다함께 | — | |
| 로그인 / 로그아웃 | Sprint 1 | 1 | 김태균 | member | |
| 회원가입 / 탈퇴 | Sprint 1 | 1 | 김태균 | member | |
| 내 프로필 조회 | Sprint 1 | 1 | 곽효오 | member, member_position | |
| 경기 생성 | Sprint 1 | 1~2 | 안세하 | game | |
| 경기 일정 조회 / 목록 | Sprint 1 | 2 | 안세하 | game | |
| 선수 등록 | Sprint 1 | 2 | 전재준 | game_player | 이름/닉네임, 포지션, 실력 등을 등록 |
| 매치 참여 기능 | Sprint 2 | 3 | 김태균 | game_player | |
| 포지션 복수 선택 | Sprint 2 | 3 | 김태균 | member_position, player_position | 우선 순위 정하기 |
| 포지션 설정 | Sprint 2 | 3 | 안세하 | player_position | GK / DF / MF / FW, 복수 선택 가능 |
| 선수 실력 매기기 | Sprint 2 | 3 | 곽효오 | game_player.skill_level | |
| 후보 선수 지정 | Sprint 2 | 3 | 곽효오 | game_player.is_substitute | 18명이 넘을 경우 실력 차이를 고려해서 공평하게 정해짐 |
| AI/알고리즘 팀 짜기 | Sprint 2 | 4 | 곽효오 | lineup, team, team_member | |
| 드래그 앤 드롭 전술 배치도 | Sprint 3 | 5 | 김태균 | team_member.pos_x, pos_y | |
| 팀 확정 | Sprint 3 | 5 | 안세하 | lineup.is_confirmed | |
| 다시 짜기 | Sprint 3 | 5~6 | 안세하 | lineup.version | 마음에 안 들면 조건 유지하고 재배정 |
| 경기 저장 | Sprint 3 | 6 | 안세하 | lineup, (결과 컬럼 없음) | 해당 날짜의 팀 구성과 경기 결과 저장 |
| 전술 배치도 이미지 저장 | Sprint 3 | 6 | 전재준 | lineup.image_url | |
| 경기 상세 조회 | Sprint 3 | 6 | 곽효오 | game 외 다수 | |
| 경기 이후 밸런스 작성 | Sprint 4 | 7 | 곽효오 | balance_review | |
| 경기 MVP | Sprint 4 | 7 | 곽효오 | mvp_vote | 경기 끝나고 오늘의 MVP 선택하게 하기 |
| 밸런스 취합 및 점수 계산 | Sprint 4 | 7 | 곽효오 | balance_review, member.rating | |
| 파티장 공지 작성 / 조회 | Sprint 4 | 7 | 전재준 | notice | |
| 각종 의견 투표 생성 | Sprint 4 | 7 | 전재준 | poll, poll_option, poll_answer | |
| 경기 삭제 | Sprint 4 | 8 | 전재준 | game.is_deleted | |
| 악성 유저 신고 | Sprint 4 | 8 | 김태균 | report | |
| 전체 기능 연동 테스트 | 마무리 | 9 | — | — | |
| 최종 버그 수정 및 배포 준비 | 마무리 | 9 | — | — | |

채택하지 않은 기능: "전술 배치도 자동 생성"(재준) — 사유 "위 팀짜기 기능과 겹침 이슈". 구현하지 않는다.
