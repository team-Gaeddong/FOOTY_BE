# FOOTY_BE — AI 코딩 에이전트 지침

FOOTY는 축구 경기를 만들고, 참가 선수의 포지션·실력을 바탕으로 팀을 나누고, 경기 후 밸런스·MVP를 평가하는 서비스다. 이 저장소는 그 백엔드다.

이 문서는 모든 작업에 항상 적용되는 규칙만 담는다. 작업별 절차는 아래 Skills를 읽고 따른다.

| 작업 | 먼저 읽을 파일 |
|---|---|
| API 구현 | `.claude/skills/footy-api-implement/SKILL.md` |
| 코드 리뷰 | `.claude/skills/footy-code-review/SKILL.md` |
| 이슈·브랜치·커밋·PR | `.claude/skills/footy-git-workflow/SKILL.md` |

## 1. 기준 문서와 우선순위

| 영역 | 기준(Source of truth) | 저장소 안 사본 |
|---|---|---|
| 기능 목록·담당자·스프린트 | Notion `FOOTY > 기능`, `FOOTY > 스프린트 일정` | `.claude/skills/footy-api-implement/references/feature-map.md` |
| DB 스키마 | ERD Cloud `footy` 다이어그램 | `docs/erd/footy_erd.sql` |
| 이슈·브랜치·커밋·PR·버전 | Notion `FOOTY > Github Commit 명세` | `.claude/skills/footy-git-workflow/SKILL.md` |
| 빌드·런타임 | 이 저장소의 `build.gradle`, `src/` | — |
| 미결정 사항·문서 충돌 | 팀 결정 대기 | `docs/harness/decisions.md` |

- 저장소 안 사본은 작성 시점의 스냅샷이다. 사본과 원본(Notion, ERD Cloud)이 다르면 원본이 우선이고, 차이를 사람에게 보고한다.
- **문서끼리, 또는 문서와 코드가 충돌하면 에이전트가 임의로 하나를 고르지 않는다.** 작업을 멈추고 `docs/harness/decisions.md`의 형식으로 차이점을 보고한 뒤 사람의 결정을 기다린다.
- `docs/harness/decisions.md`에서 상태가 `미결정`인 항목이 작업에 필요하면, 그 부분은 구현하지 않고 질문한다. 문서에 적힌 "제안"은 채택된 규칙이 아니다.

## 2. 현재 확정된 기술 사실 (코드 기준)

- Java 21 (Gradle toolchain), Spring Boot 4.1.1, Gradle 9.7.1 wrapper
- 기본 패키지: `com.example.footy_be` (변경 여부는 `decisions.md` D-02)
- 의존성은 `spring-boot-starter`, `spring-boot-starter-test`뿐이다. Web, JPA, Validation, Security, DB 드라이버는 아직 없다.
- 설정 파일: `src/main/resources/application.properties` (`spring.application.name`만 있음)
- 원격 저장소: `team-Gaeddong/FOOTY_BE` (private), 기본 브랜치 `main`

의존성 추가·DB 선택·패키지 구조는 팀 결정 사항이다. 결정 전에는 추가하지 않는다.

## 3. 도메인과 스키마 규칙

- 테이블 15개: `member`, `member_position`, `game`, `game_player`, `player_position`, `lineup`, `team`, `team_member`, `balance_review`, `mvp_vote`, `notice`, `poll`, `poll_option`, `poll_answer`, `report`
- 엔티티·컬럼·제약은 `docs/erd/footy_erd.sql`과 일치해야 한다. 스키마 변경이 필요하면 코드를 먼저 바꾸지 말고 변경안을 보고한다(ERD Cloud가 먼저 바뀌어야 한다).
- ERD에 이미 반영된 도메인 규칙
  - 경기 생성자(`game.host_id`)가 파티장이다.
  - 비회원 선수 허용: `game_player.member_id`는 NULL 가능.
  - 포지션 값은 `GK`, `DF`, `MF`, `FW`이며 복수 선택과 우선순위(`priority`)를 가진다.
  - 다시 짜기는 `lineup.version` 증가, 팀 확정은 `lineup.is_confirmed`.
  - 회원 탈퇴와 경기 삭제는 `is_deleted` 플래그다(소프트 삭제).
  - 한 경기에서 회원당 밸런스 평가 1건, MVP 투표 1건(UNIQUE 제약).

## 4. 항상 지킬 규칙

- 작업은 Notion 기능 1개 = GitHub 이슈 1개 단위로 한다. 이슈 번호 없이 기능 브랜치를 만들지 않는다.
- 브랜치: `feature/{이슈번호}_{할일}`, 긴급 수정은 `hotfix/{이슈번호}_{할일}`
- 커밋 메시지: `{CommitType} :: {Subject}` (형식 세부와 미결정 부분은 git-workflow 스킬 참고)
- Subject는 한국어, 명령형, 50자 이내, "어떻게"보다 "무엇을/왜"
- `main`에 직접 커밋·푸시하지 않는다. 병합은 PR로만 하고, 사람 리뷰어 최소 1명의 승인이 필요하다. 에이전트 리뷰는 승인으로 치지 않는다.
- 비밀번호·토큰·DB 접속 정보를 코드나 커밋에 넣지 않는다. 비밀번호는 해시로만 저장한다(`member.password`).
- 파티장 전용 동작(공지 작성, 팀 확정, 경기 삭제 등)은 서버에서 권한을 확인한다. 권한 정책이 기능 문서에 없으면 질문한다.
- 요구사항을 정리할 때는 입력, 출력, 예외 상황, 레퍼런스를 함께 적는다(1차 스크럼 회고록의 프롬프트 설계 규칙).

## 5. 검증 명령

```bash
./gradlew clean build          # 컴파일 + 테스트 + 패키징. 모든 작업의 최소 통과 조건
./gradlew test                 # 테스트만
./scripts/harness/check.sh     # 빌드 + 브랜치 이름 + 커밋 메시지 형식 검사
```

- 2026-10-14 기준 `./gradlew test`는 통과한다(`FootyBeApplicationTests.contextLoads` 1개).
- CI는 아직 없다(`.github/workflows` 없음). Notion은 "CI build success"를 병합 조건으로 두므로, CI가 생기기 전까지는 위 명령의 로컬 결과를 PR 본문에 붙인다.

## 6. 완료 조건 (모든 작업 공통)

1. `./scripts/harness/check.sh`가 통과한다(실패하면 실패 출력 그대로 보고).
2. 변경이 연결된 이슈 범위를 벗어나지 않는다.
3. 스키마·의존성·패키지 구조를 바꾸지 않았거나, 바꿨다면 사람이 승인한 결정(`decisions.md`의 `확정` 항목)을 근거로 적었다.
4. 작업 중 발견한 문서 충돌·미결정 사항을 `decisions.md` 형식으로 보고했다.
5. PR 본문에 이슈 번호, 변경 요약, 검증 명령 출력, 남은 질문을 적었다.
