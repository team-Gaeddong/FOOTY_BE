# FOOTY_BE — AI 코딩 에이전트 지침

FOOTY는 축구 경기를 만들고, 참가 선수의 포지션·실력을 바탕으로 팀을 나누고, 경기 후 밸런스·MVP를 평가하는 서비스다. 이 저장소는 그 백엔드다.

이 문서는 모든 작업에 항상 적용되는 규칙만 담는다. 작업별 절차는 아래 Skills를 읽고 따른다. Claude는 `.claude/skills`의 원본을, Codex는 같은 원본을 가리키는 `.agents/skills` 링크를 사용한다.

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
| UNIQUE 제약 | `docs/erd/constraints.md` (ERD Cloud에 표기 불가) | — |
| 이슈·브랜치·커밋·PR·버전 | Notion `FOOTY > Github Commit 명세` | `.claude/skills/footy-git-workflow/SKILL.md` |
| 빌드·런타임 | 이 저장소의 `build.gradle`, `src/` | — |
| 미결정 사항·문서 충돌 | 팀 결정 대기 | `docs/harness/decisions.md` |

- 저장소 안 사본은 작성 시점의 스냅샷이다. 사본과 원본(Notion, ERD Cloud)이 다르면 원본이 우선이고, 차이를 사람에게 보고한다.
- **문서끼리, 또는 문서와 코드가 충돌하면 에이전트가 임의로 하나를 고르지 않는다.** `docs/harness/decisions.md`의 형식으로 차이점을 보고하고, 그 결정에 의존하는 부분만 보류한다. 독립적인 검증·유지보수와 사용자가 이미 승인한 작업은 계속한다.
- `docs/harness/decisions.md`에서 상태가 `미결정`인 항목이 작업에 필요하면, 그 부분은 구현하지 않고 질문한다. 문서에 적힌 "제안"은 채택된 규칙이 아니다.

원본 URL·확인 범위·접근 불가 시 절차는 `docs/harness/sources.md`를 따른다. 사본의 작성 날짜를 원본 재검증 날짜로 해석하지 않는다.

## 2. 현재 확정된 기술 사실 (코드 기준)

- Java 21 (Gradle toolchain), Spring Boot 4.1.1, Gradle 9.7.1 wrapper
- 기본 패키지: `com.gaeddong.footy` (D-02). 도메인별로 `domain.{도메인}.{domain,domain.repository,exception,facade,presentation,service}`, 공통 코드는 `global` (D-01, 해석은 C-16)
- 선언된 의존성: `spring-boot-starter`, `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`, `spring-boot-starter-security`, `spring-boot-starter-security-oauth2-client`, `spring-boot-starter-data-redis`, `jjwt` 0.13.0(D-05·D-05b·D-05d), `lombok`(compileOnly·annotationProcessor), 런타임 `postgresql`, 테스트 `spring-boot-starter-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`, `h2`, `junit-platform-launcher`. Validation은 아직 없다.
- 설정 파일: `src/main/resources/application.properties` (비밀·접속 정보는 환경변수: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `JWT_SECRET`(32바이트 이상), `OAUTH_REDIRECT_URI`. `ddl-auto=none`), 테스트는 `src/test/resources/application.properties` (H2 PostgreSQL 모드, `create-drop`)
- 원격 저장소: `team-Gaeddong/FOOTY_BE` (2026-10-14 조회: public), 기본 브랜치 `main`

의존성 추가·DB 선택·패키지 구조는 팀 결정 사항이다. 결정 전에는 추가하지 않는다.

## 3. 도메인과 스키마 규칙

- 테이블 15개: `member`, `member_position`, `game`, `game_player`, `player_position`, `lineup`, `team`, `team_member`, `balance_review`, `mvp_vote`, `notice`, `poll`, `poll_option`, `poll_answer`, `report`
- 엔티티·컬럼·제약은 `docs/erd/footy_erd.sql`과, UNIQUE 제약은 `docs/erd/constraints.md`와 일치해야 한다. 스키마 변경이 필요하면 코드를 먼저 바꾸지 말고 변경안을 보고한다(ERD Cloud가 먼저 바뀌어야 한다. UNIQUE는 `constraints.md`가 먼저).
- ERD에 이미 반영된 도메인 규칙
  - 로그인은 Google OAuth만 쓴다. 회원은 `member.provider`+`provider_id`로 식별하고, 첫 로그인 때 생성한 뒤 온보딩을 마치면 `is_onboarded=1`이 된다(온보딩 전 `nickname`은 NULL).
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
- 토큰·JWT 서명 키·OAuth 클라이언트 시크릿·DB·Redis 접속 정보를 코드나 커밋에 넣지 않는다. 자체 비밀번호는 저장하지 않는다(Google OAuth 단독 로그인, `decisions.md` D-05c·C-15).
- 파티장 전용 동작(공지 작성, 팀 확정, 경기 삭제 등)은 서버에서 권한을 확인한다. 권한 정책이 기능 문서에 없으면 질문한다.
- 요구사항을 정리할 때는 입력, 출력, 예외 상황, 레퍼런스를 함께 적는다(1차 스크럼 회고록의 프롬프트 설계 규칙).

## 5. 검증 명령

```bash
./scripts/harness/check.sh                     # 변경 검사: 브랜치 + 커밋 + clean build
./scripts/harness/check.sh --mode baseline     # 현재 코드 빌드 점검; 변경 완료 증거 아님
python3 -m unittest discover -s scripts/harness/tests -v  # 하네스 변경 시 회귀 검사
```

- 2026-10-28 기준 `./gradlew test`는 통과한다(155개: `AuthApiTest`, `AuthServiceTest`, `PlayerPositionRepositoryTest`, `GameControllerTest`, `GameCreateApiTest`, `GameListApiTest`, `PlayerPositionApiTest`, `PlayerRegisterApiTest`, `RematchGameApiTest`, `SkillLevelApiTest`, `GamePlayerServiceTest`, `GameServiceTest`, `PlayerPositionServiceTest`, `PlayerRegisterServiceTest`, `MemberTest`, `PositionPrioritiesTest`, `MemberPositionRepositoryTest`, `MemberRepositoryTest`, `MemberFacadeTest`, `MemberApiTest`, `MemberProfileServiceTest`, `MemberServiceTest`, `FootyBeApplicationTests`, `JwtProviderTest`, `OAuth2LoginSuccessHandlerTest`). 테스트는 실제 Redis 없이 돈다(Redis 저장소는 목).
- `.github/workflows/harness.yml`은 PR 변경 검사와 기본 브랜치 빌드를 수행한다. 원격에 반영된 뒤에만 CI가 실행되며, 브랜치 보호의 필수 검사 `harness`와 사람 승인 1명 설정은 별도로 적용해야 한다(`README.md`).
- 종료 코드 0은 명시한 모드의 통과, 1은 실패, 2는 일부 검사다. `--skip-build` 또는 변경 커밋 0개는 완료 증거가 아니다. 기준 ref 누락·얕은 이력은 실패다.
- 기존 테스트 실행은 필수다. 새 정상·예외 테스트는 권장하며, 팀 전체 필수화는 D-09 확정 후 적용한다. 사용자·이슈가 요구한 테스트는 필수로 수행한다. 누락한 검증 범위는 보고한다.

## 6. 완료 조건 (모든 작업 공통)

1. 변경을 커밋·PR로 제출할 때 `./scripts/harness/check.sh`의 change 모드가 통과한다. 커밋 전 로컬 수정·감사는 baseline 결과와 미검증 브랜치·커밋 항목을 구분해 보고하며 제출 완료로 표현하지 않는다. 하네스 변경이면 회귀 검사도 통과해야 한다.
2. 변경이 연결된 이슈 범위를 벗어나지 않는다.
3. 스키마·의존성·패키지 구조를 바꾸지 않았거나, 바꿨다면 사람이 승인한 결정(`decisions.md`의 `확정` 항목)을 근거로 적었다.
4. 작업 중 발견한 문서 충돌·미결정 사항을 `decisions.md` 형식으로 보고했다.
5. PR 본문에 이슈 번호, 변경 요약, 검증 명령 출력, 남은 질문을 적었다.
