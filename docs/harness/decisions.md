# 문서 충돌과 미결정 사항

에이전트는 이 문서의 `미결정`·`보고됨` 항목을 스스로 결정하지 않는다. 팀이 결정하면 상태를 `확정`으로 바꾸고 결정 내용과 날짜를 적는다. 그 뒤에만 에이전트가 그 결정을 따른다.

조사 기준(2026-10-14): Notion `FOOTY` 하위 페이지(기능, 스프린트 일정, 스크럼 회고록, Github Commit 명세), ERD Cloud `footy`, 저장소 `team-Gaeddong/FOOTY_BE`의 `main`(커밋 `116ed8c`).

## 새 항목 보고 형식

```
### C-xx 제목
- 상태: 보고됨
- 출처 A: (문서·위치) — 내용
- 출처 B: (문서·코드·위치) — 내용
- 영향: 어떤 작업이 막히는지
- 결정할 사람/시점:
```

## C. 문서·코드 충돌

### C-01 GitFlow와 실제 브랜치
- 상태: 보고됨
- Notion Github Commit 명세: "기본적으로 GitFlow를 따릅니다", feature/hotfix 브랜치 정의
- 저장소: `main` 브랜치 하나뿐, `develop` 없음
- 영향: feature 브랜치의 분기·PR 대상(base)을 정할 수 없음 → D-10

### C-02 CI 병합 조건과 활성화 상태
- 상태: 보고됨
- Notion: PR 병합 조건 "CI로 인한 build success"
- 2026-10-14 감사: GitHub API에서 public, main 보호 없음, 원격 workflow 0개 확인. 과거 private 플랜의 403 기록으로 현재 설정 불가를 단정하지 않는다.
- 로컬 수정: `.github/workflows/harness.yml` 추가. PR 검사와 baseline 빌드를 분리하고 최종 검사 이름은 `harness`다.
- 남은 작업: 원격 반영·첫 CI 실행 후 필수 검사와 사람 승인 1명을 브랜치 보호에 연결한다. 로컬 파일 추가만으로 활성화 완료라 표현하지 않는다. README의 적용 절차 참고.

### C-03 이슈 라벨
- 상태: 보고됨
- Notion: `BUG`, `feature`, `DOCS`, `Chore`, `Refactor`, `프로젝트 설정`, `테스트 추가` (색상 코드 포함)
- 저장소: GitHub 기본 라벨(`bug`, `documentation`, `enhancement` 등)만 있음
- 영향: 이슈 규칙의 라벨을 붙일 수 없음

### C-04 커밋 메시지의 이슈 번호와 범위 표기
- 상태: 보고됨
- Notion 형식 정의: `CommitType :: (#issue number) Subject`, 이슈 번호는 "merge commit이나 PR을 날릴 때만 사용"
- Notion 예시: `🐛 :: (#19)[FeedServiceImpl] 피드 버그 로직 수정`, `🐛 :: [MainPage] 헤더가 안보이는 버그 수정` — 일반 커밋에 이슈 번호, 정의에 없는 `[범위]` 표기 사용
- 영향: 커밋 검사 규칙. 검사기는 일반 커밋의 두 표기를 모두 허용한다. 스킬의 임의 기본값을 제거했다. 일반 커밋의 정책은 계속 미결정이며 PR·merge의 이슈 번호는 요구한다
- 기타: 예시 문서의 라이선스 문구가 다른 학교 팀(SEMICOLON; & XQUARE) 문서로 되어 있어, 템플릿을 가져온 것으로 보임

### C-05 버전 시작값
- 상태: 보고됨
- Notion: "모든 버전은 01.00.00에서 시작", Major는 "1로 시작"
- `build.gradle`: `version = '0.0.1-SNAPSHOT'`
- 영향: 릴리즈 커밋(`🚀 ::`)과 태그 버전

### C-06 경기 저장의 "경기 결과"
- 상태: 보고됨
- Notion 기능 "경기 저장": "해당 날짜의 팀 구성과 경기 결과 저장"
- ERD: 경기 결과(스코어, 승패)를 담는 컬럼·테이블 없음
- 영향: 경기 저장 API 구현 불가. ERD에 결과 저장 구조를 먼저 추가해야 함

### C-07 기능 이름 불일치
- 상태: 보고됨
- 기능 페이지 ↔ 스프린트 일정: "팀 확정 기능" ↔ "팀 확정", "ai 혹은 알고리즘으로 팀 짜기" ↔ "AI/알고리즘 팀 짜기", "밸런스 추합해서 점수 계산?" ↔ "밸런스 취합 및 점수 계산", "파티장 공지 작성"·"파티장 공지 조회"(2개) ↔ "파티장 공지 작성 / 조회"(1개), "로그인, 로그아웃" ↔ "로그인 / 로그아웃"
- 영향: 이슈 제목과 "기능 1개 = 이슈 1개" 단위. 공지 작성·조회를 이슈 1개로 할지 2개로 할지 결정 필요

### C-08 "파티장"과 "파티"의 정의
- 상태: 보고됨
- Notion: "파티장 공지 작성/조회"라는 기능만 있고 파티(모임)의 정의 없음
- ERD: 별도 `party` 테이블 없이 경기 생성자(`game.host_id`)를 파티장으로 보고 공지·투표를 경기에 연결함(ERD 설계 시 가정)
- 영향: 경기와 무관한 상시 모임 공지가 필요하면 스키마 변경 필요

### C-09 ERD의 DB 방언과 프로젝트 설정
- 상태: 보고됨 — D-06에서 PostgreSQL 확정(2026-10-21). ERD Cloud Export 방언(MySQL)과 다름
- 매핑 기준(제안): `TINYINT(1)` → `boolean`, `DATETIME` → `timestamp`, `AUTO_INCREMENT` → identity. ERD Cloud의 DB 설정을 PostgreSQL로 바꿀지, 사본만 변환할지 결정 필요
- ERD: MySQL 문법 DDL(`AUTO_INCREMENT`, `TINYINT(1)`, `COMMENT`)
- 저장소: DB 드라이버·JPA·datasource 설정 없음, 사용할 DB 문서 없음
- 영향: D-06

### C-10 후보 선수 기준 인원
- 상태: 보고됨
- Notion 기능 "후보 선수 지정": "18명이 넘을 경우…"
- ERD: 경기마다 `game.max_players`, `game.team_count`가 따로 있음
- 영향: 기준이 고정 18명인지, 경기 설정값에서 계산하는지 결정 필요. 후보를 "공평하게" 정하는 기준도 문서에 없음

### C-11 기존 커밋 형식
- 상태: 참고
- 저장소 커밋 `3da8795 first commit`, `116ed8c init`은 커밋 규칙 형식이 아님. 규칙 도입 전 커밋으로 보고 검사 대상에서 제외(검사기는 지정된 base와 HEAD의 공통 조상 이후 커밋만 검사). `5920d19 feat :: 백엔드 harness 구축`도 형식 위반이었다. 이력을 재작성하거나 해당 커밋만 예외 처리하지 않는다. 현재 main을 base로 한 새 변경 범위부터 검사한다

### C-12 ERD 원본에 AUTO_INCREMENT·UNIQUE 표현 없음
- 상태: 확정(UNIQUE 2026-10-19, AUTO_INCREMENT 2026-10-21, 김태균)
- 결정: UNIQUE 제약과 모든 PK의 AUTO_INCREMENT는 `docs/erd/constraints.md`에 명시한다. ERD Cloud에는 표기하지 않는다
- 아래는 보고 당시 기록
- 출처 A: `docs/erd/footy_erd.sql` — 모든 PK `AUTO_INCREMENT`, UNIQUE 6개(`uk_member_email`, `uk_member_provider`, `uk_game_player_member`, `uk_balance_review`, `uk_mvp_vote`, `uk_poll_answer`)
- 출처 B: ERD Cloud `footy` Export (2026-10-19 Chrome에서 직접 확인) — 컬럼·NULL·DEFAULT·코멘트는 출처 A와 같음. FK 24개도 같음(앞선 "FK 없음"은 Export 옵션 `ADD FK CONSTRAINT`가 꺼져 있던 탓). `AUTO_INCREMENT`, UNIQUE는 없음. 컬럼 상세 창(Logical/Physical Name, Domain, Type, Allow null, Default, Comment)에 해당 항목이 없어 ERD Cloud에서 입력할 방법을 찾지 못함
- 영향: 이메일·Google 계정 중복 방지, AGENTS.md 3절의 "밸런스 평가 1건·MVP 투표 1건(UNIQUE 제약)", 엔티티 PK 생성 전략
- 확인 필요: 사본의 AUTO_INCREMENT·UNIQUE를 팀 규칙으로 인정할지(원본에는 코멘트나 메모로 표기), ERD Cloud 표현 한계로 보고 사본만 기준으로 둘지
- 결정할 사람/시점: 팀 / 회원 엔티티 작성 전

### C-13 구현·리뷰 테스트 기준 불일치
- 상태: 지침 불일치 수정 / 팀 정책 D-09는 미결정
- 출처 A: API 구현 스킬 — 모든 예외 테스트를 완료 조건으로 요구했음
- 출처 B: 리뷰·Git 스킬 — 새 테스트 작성은 선택이라고 했음
- 조치: 기존 테스트 실행은 필수, 새 정상·예외 테스트는 권장으로 통일. 사용자·이슈가 명시한 테스트는 필수이며, 미검증 경로를 보고한다. 팀 전체 필수화는 채택하지 않았다.
- 결정할 사람/시점: 팀 / D-09를 확정할 때

### C-14 미결정 커밋 형식의 임의 기본값
- 상태: 지침 불일치 수정 / C-04는 미결정
- 출처 A: AGENTS.md — 문서 충돌을 임의로 결정하지 않음
- 출처 B: Git 스킬 — 일반 커밋의 이슈 번호와 범위 표기에 임시 기본값을 선택했음
- 조치: 임시 기본값을 제거하고 검사기의 허용 범위와 확정된 팀 정책을 구분했다. 관련 선택에만 결정을 요청하며 독립 작업은 계속한다.
- 결정할 사람/시점: 팀 / 일반 커밋 형식 확정 시

### C-15 Google 단독 로그인과 member 스키마·규칙
- 상태: 확정(2026-10-19, 김태균) — ERD Cloud·사본·AGENTS.md 반영 완료
- 채택안: `password` 삭제, `nickname` NULL 허용(온보딩 전 NULL), `provider VARCHAR(20) NOT NULL`, `provider_id VARCHAR(255) NOT NULL`, `is_onboarded TINYINT(1) NOT NULL DEFAULT 0` 추가. 온보딩 미완료 표현은 (나) member 먼저 생성. UNIQUE(`provider`, `provider_id`)는 C-12에 따라 원본 미표기
- 아래는 보고 당시 기록
- 출처 A: D-05c 확정(2026-10-19) — Google OAuth로만 로그인, 회원가입은 온보딩
- 출처 B: ERD `member` — `password VARCHAR(255) NOT NULL`, Google 계정 식별자(`sub`)·가입 경로 컬럼 없음, 온보딩 완료 여부 컬럼 없음
- 출처 C: AGENTS.md 4절 — "비밀번호는 해시로만 저장한다(`member.password`)"
- 영향: 회원 엔티티 작성 불가(넣을 비밀번호가 없음). 이메일만으로 Google 계정을 식별하면 이메일 변경 시 다른 회원이 됨
- 변경안(ERD Cloud 먼저 반영 필요, 채택 아님):
  - `password` 삭제 또는 NULL 허용
  - `provider VARCHAR(20) NOT NULL`, `provider_id VARCHAR(255) NOT NULL` + UNIQUE(`provider`, `provider_id`) 추가
  - 온보딩 미완료 표현: (가) 온보딩 완료 시점에만 `member` 생성, 그 전 Google 정보는 Redis에 임시 저장 → 컬럼 추가 없음 / (나) `member`를 먼저 만들고 `nickname` NULL 허용 또는 상태 컬럼 추가
- 함께 정리할 것: AGENTS.md 4절 비밀번호 규칙, C-12의 `email` UNIQUE 여부
- 결정할 사람/시점: 팀 / 회원 엔티티 작성 전

### C-16 D-01 패키지 구조 해석과 기존 이슈 범위
- 상태: 일부 결정(2026-10-21, 김태균) — Member 엔티티는 이슈 #3에서 만든다(game_player 등은 #1 범위). 패키지 해석은 팀 확인 필요
- 출처 A: D-01 확정(2026-10-21) — `domain, exception, facade, presentation, service`
- 해석(에이전트 적용안): `com.gaeddong.footy.domain.{도메인}.{domain,domain.repository,exception,facade,presentation,service}`, 공통 코드는 `com.gaeddong.footy.global`
- 출처 B: GitHub 이슈 #1 "내 프로필 조회 api"(곽효오) — 본문에 "member, game_player 엔티티를 정의" 포함
- 영향: Member 엔티티를 두 이슈에서 따로 만들면 충돌. 담당 조율 필요
- 결정할 사람/시점: 김태균·곽효오 / Member 엔티티 작성 전

### C-17 브랜치 구분자와 CI 검사
- 상태: 보고됨(2026-10-28)
- 출처 A: `AGENTS.md`·Git 스킬 — `feature/{이슈번호}_{할일}`을 기본 브랜치 형식으로 명시
- 출처 B: 사용자 지정 이슈 #30 브랜치·PR #31 — `feature/30-재경기-생성-api-구현` 사용
- 영향: 기존 하네스가 이름만으로 PR 검증을 실패 처리함. 검사기는 이슈 번호 뒤 `_`와 `-`를 모두 허용하되 기본 브랜치 형식의 팀 정책은 바꾸지 않음
- 결정할 사람/시점: 팀 / 브랜치 명명 규칙을 다시 정할 때

원본 링크·대조 범위는 `sources.md`에서 관리한다. 지침 정리는 팀의 아키텍처·DB·인증 결정을 대신하지 않는다.

## D. 미결정 사항 (Notion·ERD·코드 어디에도 없음)

"제안"은 결정을 돕기 위한 예시이며 채택된 규칙이 아니다.

| ID | 항목 | 상태 | 막히는 작업 | 제안 |
|---|---|---|---|---|
| D-01 | 아키텍처·계층 구조 | 확정(2026-10-21, 김태균) — 세부 해석은 C-16 | 모든 API | 도메인별로 `domain`(엔티티·리포지토리), `exception`, `facade`, `presentation`(컨트롤러·DTO), `service` 패키지를 둔다 |
| D-02 | 기본 패키지명 | 확정(2026-10-21, 김태균) | 모든 코드 | **`com.gaeddong.footy`** (`group = 'com.gaeddong'`) |
| D-03 | API 성공 응답 형식 | 확정(2026-10-21, 김태균) | 모든 API | **공통 래퍼 없이 리소스(응답 DTO) 그대로 반환**. 성공 여부는 상태 코드로 표현 |
| D-04 | 에러 응답·예외 처리 | 확정(2026-10-21, 김태균) | 모든 API | **전역 예외 처리기(`global/error`의 `@RestControllerAdvice`) + ErrorCode(HTTP 상태·코드·메시지)**. 응답 본문은 `{"code": "AUTH_401_1", "message": "..."}`이고 HTTP 상태는 상태 코드로만 표현. 코드는 `{도메인}_{HTTP 상태}_{순번}`. 도메인별 에러 코드는 각 도메인의 `exception` 패키지 |
| D-05 | 인증 방식 | 확정(2026-10-19, 김태균) | 로그인 이후 모든 API | **JWT**. 세부 사항은 아래 D-05a~e |
| D-05a | JWT 토큰 구성 | 확정(2026-10-19·21, 김태균) | 로그인, 로그아웃 | **access + refresh 토큰**. 만료: access 30분, refresh 14일. 서명 키는 환경변수 |
| D-05b | refresh 토큰 저장·로그아웃 처리 | 확정(2026-10-19·21, 김태균) | 로그아웃 | **Redis 도입**(의존성·접속 설정 추가). 로그아웃 시 refresh 토큰 삭제 + access 토큰을 남은 만료 시간만큼 Redis 블랙리스트에 등록 |
| D-05c | 로그인 수단 | 확정(2026-10-19, 김태균) | 로그인, 회원가입 | **Google OAuth로만 로그인**. 자체 이메일·비밀번호 로그인 없음. 회원가입은 첫 Google 로그인 뒤 온보딩으로 진행. ERD와 충돌 → C-15 |
| D-05d | Google OAuth 연동 흐름 | 확정(2026-10-21, 김태균) | 로그인 | **서버 리다이렉트**(Spring `oauth2-client`). 콜백 성공 시 프론트 URI에 **1회용 코드**(Redis, 1분)를 붙여 리다이렉트하고, 프론트가 `POST /auth/token`으로 토큰과 교환한다. JWT 라이브러리는 **jjwt**. refresh는 재발급할 때마다 **회전**. 탈퇴한 회원이 같은 Google 계정으로 로그인하면 **계정 복구** |
| D-05e | 온보딩 입력 항목·미완료 회원 처리 | 확정(2026-10-21, 김태균) | 회원가입(온보딩) | **Sprint 1은 닉네임만** 입력. 닉네임은 앞뒤 공백 제거 후 2~10자, 중복 허용. API는 `PATCH /members/me/onboarding`(본문 `{"nickname"}`), 성공 시 **204**. 탈퇴 API는 `DELETE /members/me`, 성공 시 **204**, 소프트 삭제 후 로그아웃과 같이 토큰 무효화. 선호 포지션은 Sprint 2 "포지션 복수 선택"에서 추가. `is_onboarded=0` 회원은 온보딩·로그아웃·탈퇴·토큰 재발급 외 API 접근 차단 |
| D-06 | DB·영속성 | 확정(2026-10-21, 김태균) — 스키마 관리 방식 미결정 | 엔티티 작성 | **PostgreSQL + Spring Data JPA**, 테스트는 **H2**. ERD DDL은 MySQL 문법 → C-09. 스키마 생성 방식(ddl-auto / 마이그레이션 도구)은 미결정 |
| D-07 | URL·HTTP 규칙 | 접두사만 확정(2026-10-21, 김태균) | 모든 API | **버전·`/api` 접두사 없음**(예: `/auth/token`). 복수형 명사·하위 리소스 중첩(제안: `/games/{id}/players`)은 미결정 |
| D-08 | 코드 스타일·DTO 규칙 | 엔티티 규칙 확정(2026-10-21), 코드 일관성 규칙 확정(2026-10-26, 김태균) — DTO·포매터 미결정 | 모든 코드 | **Lombok 사용.** 엔티티는 `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PRIVATE)`, `@Builder`, `@Getter`, `@EntityListeners(AuditingEntityListener.class)`(생성일은 `@CreatedDate`). 기본값 컬럼은 `@Builder.Default`. `@Setter`·`@Data`는 쓰지 않고 상태 변경은 메서드로. DTO 규칙(제안: `record`, `XxxRequest`/`XxxResponse`)은 미결정. **코드 일관성(2026-10-26, #13)**: 현재 회원 ID는 컨트롤러에서 `@AuthenticationPrincipal Long memberId`로 꺼낸다(access 토큰이 필요한 로그아웃·탈퇴만 `Authentication`을 함께 받음, `CurrentMemberIdProvider`는 쓰지 않음). 시간은 주입받은 `Clock`(Asia/Seoul 고정)으로 서비스에서 만들어 엔티티에 넘기고, 엔티티는 `now()`를 직접 부르지 않는다(`@CreatedDate`는 유지). 의존성 주입은 `@RequiredArgsConstructor`로 통일 |
| D-09 | 테스트 필수 여부 | 미결정 | 팀 전체 새 테스트 의무 정책 | Notion은 "선택". 서비스 단위 테스트 필수화 여부 결정 |
| D-10 | PR base 브랜치 | 미결정 | 모든 PR | C-01과 함께 결정: `develop` 생성 / `main` 직행 |
| D-12 | 회원 기능 구현 순서 | 확정(2026-10-21, 김태균) | 온보딩 API | 로그인(Security·JWT·Google OAuth·Redis)을 먼저 구현하고 온보딩은 그 뒤. 로그인 병합(PR #6) 뒤 온보딩 시작(2026-10-21 지시) |
| D-11 | API 문서화 도구 | 미결정 | 프론트 연동 | springdoc(Swagger UI) 도입 여부 |
| D-13 | 포지션 선택 규칙 | 확정(2026-10-26, 김태균) | 포지션 복수 선택, 포지션 설정 | 포지션은 **순서 있는 목록**으로 받고 **순서가 곧 우선순위**(첫 번째가 priority 1). **1~4개, 중복 불가**, 저장할 때마다 전체 교체. 회원 선호 포지션은 **`PUT /members/me/positions`**(성공 204, 온보딩과 별도). 경기별 선수 포지션(`player_position`)은 **파티장만** 설정. **매치 참여 때 회원 선호 포지션을 경기별 포지션으로 복사**(복사는 매치 참여 이슈에서 호출). 경기별 선수 포지션은 **`PUT /games/{gameId}/players/{gamePlayerId}/positions`**(성공 204), **경기가 `RECRUITING`일 때만** 변경 가능(2026-10-26) |
| D-14 | 경기 생성 정책 | 확정(2026-10-26, 김태균) | 경기 생성 | **`maxPlayers`는 2~30**, **`teamCount`는 2 이상이고 `maxPlayers` 이하**(생략 시 2), **과거 `playDate`는 거부**. 위반 시 `GAME_400_2`(최대 인원)·`GAME_400_4`(팀 수)·`GAME_400_3`(과거 일시). `GAME_400_1`은 실력 매기기 중복 선수(#19)가 먼저 develop에 들어가 번호를 조정(#34). 경기를 만들면 **파티장을 참가 선수(`game_player`)로 자동 추가**(이름은 닉네임)하고 D-13에 따라 선호 포지션을 복사. 생성 응답에서 `isDeleted` 제거 |
| D-15 | CORS | 미결정(2026-10-26 논의, 배포 구조 미정) | 프론트 연동·배포 | 프론트와 백엔드가 다른 origin이면 `CORS_ALLOWED_ORIGINS` 환경변수로 허용 origin 지정. 같은 origin(프록시)이면 불필요 |
| D-16 | 회원 없음·탈퇴 회원 에러 | 확정(2026-10-26, 김태균) | 회원 ID로 회원을 찾는 모든 API | 회원이 없거나 탈퇴(`is_deleted`)한 경우 도메인에 상관없이 **404 `MEMBER_404_1`**로 통일(#12) |
| D-17 | 성능·키 최적화 보류 항목 | 보류(2026-10-26, 김태균) | — | `OnboardingFilter`가 요청마다 회원 조회 1회, 블랙리스트 Redis 키가 access 토큰 전체 문자열(`jti` 사용 시 단축 가능). 현재 규모에선 문제 아님으로 두고 필요해지면 다시 검토(#13) |
| D-18 | 재경기 생성 정책 | 이슈 #30 구현안 확정(2026-10-28, 담당자 위임) | 재경기 생성 API | 원본 파티장만 삭제되지 않은 `FINISHED` 경기를 재생성한다. `POST /games/{gameId}/rematch`에 새 `playDate`를 받으며, 제목·장소·최대 인원·팀 수를 복사해 새 `RECRUITING` 경기를 만든다. 원본 경기는 유지하고 참가자 목록은 복사하지 않으며 원본 연결 컬럼은 추가하지 않는다. 응답은 기존 경기 생성 DTO와 201을 사용한다. |
| D-19 | 선수 등록 방식 | 확정(2026-10-28, 전재준) | 선수 등록(#32·#36) | 파티장의 비회원 등록이 아니라 **회원 본인이 경기에 참여**한다. `POST /games/{gameId}/players`(성공 201), 본문 `positions`는 **필수**(D-13 규칙), 이름은 **회원 닉네임**, 실력은 본인이 입력하지 않고 파티장이 매긴다(#19). 같은 경기 중복 참여는 409. 모집 중(`RECRUITING`)인 경기만 참여 가능한 것은 #32 구현안을 유지한 것으로 팀 확인 필요. `max_players` 초과 여부는 C-10 결정 전까지 막지 않는다. Notion "매치 참여 기능"과 범위가 겹치는지 확인 필요 |
| D-20 | 경기 선수 목록 조회 | 확정(2026-10-29, 전재준) | 선수 목록 조회(#38) | `GET /games/{gameId}/players`(성공 200)는 **로그인한 회원 누구나** 조회한다. 응답 `{"players": [...]}`는 참여 순(`joinedAt`, `id`), 포지션은 우선순위 순. **실력(`skillLevel`)은 파티장에게만** 값을 주고 그 외에는 `null`. 페이지네이션 없음(최대 30명, D-14) |
| D-21 | 팀 짜기(알고리즘) 정책 | 확정(2026-10-29, 이슈 #40 담당자) — 일부 미결정 | AI/알고리즘 팀 짜기(#40) | `POST /games/{gameId}/lineups`(성공 201)는 **파티장만** 호출한다. 실력 내림차순(같으면 참여 순)으로 정렬해 **A팀·B팀에 한 명씩 번갈아** 배정하고 **팀은 항상 2개**다. `lineup`은 `version` 1·`method` `ALGORITHM`·미확정으로 만들고 `team.total_skill`에 팀 실력 합계를 넣는다. 배정 포지션은 **1순위 희망 포지션, 없으면 `FW`**. **실력 미입력 선수가 있으면 409 `LINEUP_409_2`**, **이미 라인업이 있으면 409 `LINEUP_409_1`**(재배정은 "다시 짜기" 범위), 선수 2명 미만 409 `LINEUP_409_3`. 미결정: 후보 선수(`is_substitute`) 포함 여부(현재 포함), `team_count`가 2가 아닌 경기 처리(현재 무시하고 2팀), 경기 상태 제한(현재 없음), 동시 요청 시 라인업 중복 생성 방지(현재 UNIQUE 없음) |
