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
- 상태: 확정(2026-10-29, 이슈 #42 담당자) — D-22
- 결정: 고정 18명이 아니라 경기의 `game.max_players`를 기준으로 한다. 실력 낮은 선수부터 후보가 된다
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
| D-05e | 온보딩 입력 항목·미완료 회원 처리 | 확정(2026-10-21, 김태균) — 닉네임 중복은 2026-11-16 불가로 변경(김태균, 이슈 #68) | 회원가입(온보딩) | **Sprint 1은 닉네임만** 입력. 닉네임은 앞뒤 공백 제거 후 2~10자, **중복 불가**(처음에는 허용이었으나 같은 닉네임의 회원을 신고·투표 화면에서 구분할 수 없어 바꿨다). 이미 쓰는 닉네임이면 409 `MEMBER_409_2`. 비교는 공백 제거 뒤 완전 일치(대소문자 구분)이고, 탈퇴한 회원의 닉네임도 계속 점유한다(재로그인 복구 D-05d). UNIQUE `uk_member_nickname`(`constraints.md`)으로 동시 요청도 막는다. 미결정: 운영 DB의 기존 중복 정리와 제약 적용 시점, 대소문자·유사 문자 구분, 닉네임 변경 API. API는 `PATCH /members/me/onboarding`(본문 `{"nickname"}`), 성공 시 **204**. 탈퇴 API는 `DELETE /members/me`, 성공 시 **204**, 소프트 삭제 후 로그아웃과 같이 토큰 무효화. 선호 포지션은 Sprint 2 "포지션 복수 선택"에서 추가. `is_onboarded=0` 회원은 온보딩·로그아웃·탈퇴·토큰 재발급 외 API 접근 차단 |
| D-06 | DB·영속성 | 확정(2026-10-21, 김태균) — 스키마 관리 방식 미결정 | 엔티티 작성 | **PostgreSQL + Spring Data JPA**, 테스트는 **H2**. ERD DDL은 MySQL 문법 → C-09. 스키마 생성 방식(ddl-auto / 마이그레이션 도구)은 미결정 |
| D-07 | URL·HTTP 규칙 | 접두사만 확정(2026-10-21, 김태균) | 모든 API | **버전·`/api` 접두사 없음**(예: `/auth/token`). 복수형 명사·하위 리소스 중첩(제안: `/games/{id}/players`)은 미결정 |
| D-08 | 코드 스타일·DTO 규칙 | 엔티티 규칙 확정(2026-10-21), 코드 일관성 규칙 확정(2026-10-26, 김태균) — DTO·포매터 미결정 | 모든 코드 | **Lombok 사용.** 엔티티는 `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PRIVATE)`, `@Builder`, `@Getter`, `@EntityListeners(AuditingEntityListener.class)`(생성일은 `@CreatedDate`). 기본값 컬럼은 `@Builder.Default`. `@Setter`·`@Data`는 쓰지 않고 상태 변경은 메서드로. DTO 규칙(제안: `record`, `XxxRequest`/`XxxResponse`)은 미결정. **코드 일관성(2026-10-26, #13)**: 현재 회원 ID는 컨트롤러에서 `@AuthenticationPrincipal Long memberId`로 꺼낸다(access 토큰이 필요한 로그아웃·탈퇴만 `Authentication`을 함께 받음, `CurrentMemberIdProvider`는 쓰지 않음). 시간은 주입받은 `Clock`(Asia/Seoul 고정)으로 서비스에서 만들어 엔티티에 넘기고, 엔티티는 `now()`를 직접 부르지 않는다(`@CreatedDate`는 유지). 의존성 주입은 `@RequiredArgsConstructor`로 통일 |
| D-09 | 테스트 필수 여부 | 미결정 | 팀 전체 새 테스트 의무 정책 | Notion은 "선택". 서비스 단위 테스트 필수화 여부 결정 |
| D-10 | PR base 브랜치 | 미결정 | 모든 PR | C-01과 함께 결정: `develop` 생성 / `main` 직행 |
| D-12 | 회원 기능 구현 순서 | 확정(2026-10-21, 김태균) | 온보딩 API | 로그인(Security·JWT·Google OAuth·Redis)을 먼저 구현하고 온보딩은 그 뒤. 로그인 병합(PR #6) 뒤 온보딩 시작(2026-10-21 지시) |
| D-11 | API 문서화 도구 | 미결정 | 프론트 연동 | springdoc(Swagger UI) 도입 여부 |
| D-13 | 포지션 선택 규칙 | 확정(2026-10-26, 김태균) | 포지션 복수 선택, 포지션 설정 | 포지션은 **순서 있는 목록**으로 받고 **순서가 곧 우선순위**(첫 번째가 priority 1). **1~4개, 중복 불가**, 저장할 때마다 전체 교체. 회원 선호 포지션은 **`PUT /members/me/positions`**(성공 204, 온보딩과 별도). 경기별 선수 포지션(`player_position`)은 **파티장만** 설정. **매치 참여 때 회원 선호 포지션을 경기별 포지션으로 복사**(복사는 매치 참여 이슈에서 호출). 경기별 선수 포지션은 **`PUT /games/{gameId}/players/{gamePlayerId}/positions`**(성공 204), **경기가 `RECRUITING`일 때만** 변경 가능(2026-10-26) |
| D-14 | 경기 생성 정책 | 확정(2026-10-26, 김태균) | 경기 생성 | **`maxPlayers`는 2~30**, **`teamCount`는 2 이상이고 `maxPlayers` 이하**(생략 시 2), **과거 `playDate`는 거부**. 위반 시 `GAME_400_2`(최대 인원)·`GAME_400_4`(팀 수)·`GAME_400_3`(과거 일시). `GAME_400_1`은 실력 매기기 중복 선수(#19)가 먼저 develop에 들어가 번호를 조정(#34). 경기를 만들면 **파티장을 참가 선수(`game_player`)로 자동 추가**(이름은 닉네임)하고 D-13에 따라 선호 포지션을 복사. 생성 응답에서 `isDeleted` 제거 |
| D-15 | CORS | 확정(2026-10-29, 곽효오 — 이슈 #44) | 프론트 연동·배포 | **`CORS_ALLOWED_ORIGINS` 환경변수**(쉼표 구분)로 허용 origin을 지정하고, 비어 있으면 다른 origin을 허용하지 않는다. 허용 메서드 GET·POST·PUT·PATCH·DELETE·OPTIONS, 허용 헤더 `Authorization`·`Content-Type`, **credentials 불허**(토큰은 헤더·본문). 허용되지 않은 origin의 preflight는 403. 같은 origin(프록시) 배포면 비워 둔다 |
| D-16 | 회원 없음·탈퇴 회원 에러 | 확정(2026-10-26, 김태균) | 회원 ID로 회원을 찾는 모든 API | 회원이 없거나 탈퇴(`is_deleted`)한 경우 도메인에 상관없이 **404 `MEMBER_404_1`**로 통일(#12) |
| D-17 | 성능·키 최적화 보류 항목 | 보류(2026-10-26, 김태균) | — | `OnboardingFilter`가 요청마다 회원 조회 1회, 블랙리스트 Redis 키가 access 토큰 전체 문자열(`jti` 사용 시 단축 가능). 현재 규모에선 문제 아님으로 두고 필요해지면 다시 검토(#13) |
| D-18 | 재경기 생성 정책 | 이슈 #30 구현안 확정(2026-10-28, 담당자 위임) | 재경기 생성 API | 원본 파티장만 삭제되지 않은 `FINISHED` 경기를 재생성한다. `POST /games/{gameId}/rematch`에 새 `playDate`를 받으며, 제목·장소·최대 인원·팀 수를 복사해 새 `RECRUITING` 경기를 만든다. 원본 경기는 유지하고 참가자 목록은 복사하지 않으며 원본 연결 컬럼은 추가하지 않는다. 응답은 기존 경기 생성 DTO와 201을 사용한다. |
| D-19 | 선수 등록 방식 | 확정(2026-10-28, 전재준) | 선수 등록(#32·#36) | 파티장의 비회원 등록이 아니라 **회원 본인이 경기에 참여**한다. `POST /games/{gameId}/players`(성공 201), 본문 `positions`는 **필수**(D-13 규칙), 이름은 **회원 닉네임**, 실력은 본인이 입력하지 않고 파티장이 매긴다(#19). 같은 경기 중복 참여는 409. 모집 중(`RECRUITING`)인 경기만 참여 가능한 것은 #32 구현안을 유지한 것으로 팀 확인 필요. `max_players` 초과 여부는 C-10 결정 전까지 막지 않는다. Notion "매치 참여 기능"과 범위가 겹치는지 확인 필요 |
| D-20 | 경기 선수 목록 조회 | 확정(2026-10-29, 전재준) | 선수 목록 조회(#38) | `GET /games/{gameId}/players`(성공 200)는 **로그인한 회원 누구나** 조회한다. 응답 `{"players": [...]}`는 참여 순(`joinedAt`, `id`), 포지션은 우선순위 순. **실력(`skillLevel`)은 파티장에게만** 값을 주고 그 외에는 `null`. 페이지네이션 없음(최대 30명, D-14) |
| D-21 | 팀 짜기(알고리즘) 정책 | 확정(2026-10-29, 이슈 #40 담당자) — 일부 미결정 | AI/알고리즘 팀 짜기(#40) | `POST /games/{gameId}/lineups`(성공 201)는 **파티장만** 호출한다. 실력 내림차순(같으면 참여 순)으로 정렬해 **A팀·B팀에 한 명씩 번갈아** 배정하고 **팀은 항상 2개**다. `lineup`은 `version` 1·`method` `ALGORITHM`·미확정으로 만들고 `team.total_skill`에 팀 실력 합계를 넣는다. 배정 포지션은 **1순위 희망 포지션, 없으면 `FW`**. **실력 미입력 선수가 있으면 409 `LINEUP_409_2`**, **이미 라인업이 있으면 409 `LINEUP_409_1`**(재배정은 "다시 짜기" 범위), 선수 2명 미만 409 `LINEUP_409_3`. 미결정: `team_count`가 2가 아닌 경기 처리(현재 무시하고 2팀), 경기 상태 제한(현재 없음), 동시 요청 시 라인업 중복 생성 방지(현재 UNIQUE 없음) |
| D-22 | 후보 선수 지정 정책 | 확정(2026-10-29, 이슈 #42 담당자) — 일부 미결정 | 후보 선수 지정(#42) | 팀 짜기(`POST /games/{gameId}/lineups`) 때 지정한다. 실력 내림차순(같으면 참여 순)으로 정렬해 **앞의 `game.max_players`명이 선발, 나머지가 후보**(`game_player.is_substitute`=true). 경계에서 실력이 같으면 **늦게 참여한 선수가 후보**. 후보도 **A/B팀에 번갈아 배정**(`team_member`)하되 **`team.total_skill`에서 제외**. 응답 `members[].isSubstitute`로 내려준다. 미결정: 선수 참여 시 `max_players` 초과를 막지 않는 현재 동작(D-19) 유지 여부, 18명 문구(Notion)와의 차이 Notion 반영 |
| D-23 | 전술 배치도 정책 | 확정(2026-10-29, 김태균) — 일부 미결정 | 드래그 앤 드롭 전술 배치도(#45) | **별도 tactic 테이블 없이 ERD 기준**(`team.formation`, `team_member.pos_x`·`pos_y`)으로 구현한다. 이름 붙인 전술 저장·목록·팀 단위 관리는 범위 밖(스키마 변경 필요). `PUT /games/{gameId}/teams/{teamId}/tactic`(성공 200)은 **파티장만**, 팀 확정(`lineup.is_confirmed`) 후에는 409 `LINEUP_409_4`. 본문 `{"formation", "placements": [{"gamePlayerId", "posX", "posY"}]}`은 **전체 교체**라 목록에 없는 팀원은 배치 해제(좌표 NULL). 좌표는 **경기장 대비 퍼센트 0~100, 소수 2자리**(`DECIMAL(5,2)`), 배치 최대 11명, 포메이션 20자 이하. 팀원이 아닌 선수 400 `LINEUP_400_2`, 중복 400 `LINEUP_400_1`, 경기에 속하지 않은 팀 404 `LINEUP_404_1`. `GET` 같은 경로(200)는 **로그인한 회원 누구나**(D-20과 같은 기준). 미결정: 슬롯 이동 시 배정 포지션(`team_member.position`) 변경 여부(현재 바꾸지 않음), 후보 선수 배치 허용 여부(현재 허용) |
| D-24 | 라인업 조회 | 확정(2026-11-03, 김태균) — 일부 미결정 | 경기 라인업 조회(#49) | `GET /games/{gameId}/lineups/latest`(성공 200)는 **가장 높은 `version`의 라인업**을 팀 짜기 응답과 같은 형식으로 반환한다. **로그인한 회원 누구나** 조회하고, 실력(`skillLevel`, `totalSkill`)은 **파티장에게만** 값을 주고 그 외에는 `null`(D-20과 같은 기준). 팀은 `team_id` 순, 팀원은 `team_member_id` 순(배정 순서). 라인업이 없으면 404 `LINEUP_404_2`. 미결정: 파티장이 아닌 회원에게 `totalSkill`을 숨기는지(현재 숨김) |
| D-25 | 경기 상세 조회 | 확정(2026-11-04, 이슈 #54 담당자) | 경기 상세 조회(#54) | Notion 기능 설명이 비어 있어 이슈에서 범위를 정했다. `GET /games/{gameId}`(성공 200)는 **로그인한 회원 누구나** 조회한다. 응답은 경기 정보(`id`, `title`, `playDate`, `location`, `maxPlayers`, `teamCount`, `status`, `createdAt`)와 `host: {id, nickname}`, `playerCount`(후보 포함 참가 선수 수), 요청자 기준 `isHost`·`isJoined`. **선수 목록·라인업은 포함하지 않고** 기존 API(D-20, D-24)를 쓴다. 없거나 삭제된 경기는 404 `GAME_404_1`. Notion 기능 설명 반영은 팀 확인 필요 |
| D-26 | 파티장 공지 생성 정책 | 확정(2026-11-05, 이슈 #56 담당자) | 파티장 공지 작성 API(#56) | `POST /games/{gameId}/notices`는 경기 생성자만 사용할 수 있고, 삭제되지 않은 경기면 상태 제한 없이 허용한다. 본문 `title`·`content`는 필수이며 제목은 최대 100자, 내용은 공백만 입력할 수 없다. 성공은 201과 직접 응답 `{id, title, content, createdAt}`을 반환한다. 공지 조회는 이 이슈 범위에 포함하지 않는다. |
| D-27 | 경기 공지 조회 정책 | 확정(2026-11-05, 이슈 #58 담당자) | 공지 조회 API(#58) | `GET /games/{gameId}/notices`는 파티장과 경기 참가자만 공지 목록을 조회한다. 목록은 `createdAt`·ID 내림차순이며 공지가 없으면 빈 배열을 반환한다. `GET /games/{gameId}/notices/{noticeId}`는 같은 권한으로 공지 상세를 반환한다. 응답 필드는 `{id, title, content, createdAt}`. 없는/삭제된 경기는 `GAME_404_1`, 비참여자는 `NOTICE_403_1`, 없거나 다른 경기에 속한 공지는 `NOTICE_404_1`이다. |
| D-28 | 경기 이후 밸런스 작성 정책 | 확정(2026-11-05, 이슈 #60 담당자) — ERD Cloud 반영 완료(2026-11-05) | 경기 이후 밸런스 작성(#60) | `POST /games/{gameId}/balance-reviews`(성공 201)는 **해당 경기에 참가한 회원**(`game_player.member_id`)만, **경기 `status`가 `FINISHED`일 때만** 작성한다. 본문 `score`(필수, **1~10**), `reasons`(선택, 체크박스 복수 선택, 중복 불가), `comment`(선택, 500자 이하, 공백만이면 NULL 저장). 사유 값은 `OUR_TEAM_TOO_WEAK`(우리 팀이 너무 못한다)·`OPPONENT_TOO_STRONG`(상대 팀이 너무 잘한다)·`SKILL_GAP`(개인 실력 차이가 컸다)·`POSITION_MISMATCH`(포지션 배치가 안 맞았다)·`HEADCOUNT_MISMATCH`(인원이 안 맞았다)·`WELL_BALANCED`(팽팽하고 재밌었다). **스키마 변경**: `balance_review.score` 주석 1~5→1~10, 새 테이블 `balance_review_reason`(UNIQUE `uk_balance_review_reason`). 없거나 삭제된 경기 404 `GAME_404_1`, 참가하지 않은 회원 403 `BALANCE_403_1`, 종료 전 경기 409 `BALANCE_409_1`, 이미 작성 409 `BALANCE_409_2`, 사유 중복 400 `BALANCE_400_1`. 응답 `{id, gameId, score, reasons, comment, createdAt}`. 미결정: 수정·조회 API, 작성 마감 기한 |
| D-29 | 밸런스 취합 및 점수 계산 정책 | 확정(2026-11-13, 이슈 #64 담당자) — 일부 미결정 | 밸런스 취합 및 점수 계산(#64) | 목적은 팀 자동 재배정이 아니라 **파티장이 결과를 보고 팀을 직접 수정**하게 하는 것이다. `GET /games/{gameId}/balance-reviews/summary`(성공 200)는 **파티장과 경기 참가자**만 조회한다(D-27과 같은 기준). **작성 대상**은 탈퇴하지 않은 회원 참가 선수(`game_player.member_id`, 비회원 선수 제외)이고, **대상 전원이 작성해야 계산**한다. 그 전에는 `completed=false`와 `requiredCount`·`reviewedCount`만 주고 `averageScore`는 `null`, `teams`는 빈 배열. 계산은 저장하지 않고 조회할 때마다 한다. 팀 구분은 **가장 높은 `version`의 라인업**(D-24)이고, 작성자가 배정된 팀(`team_member` → `game_player.member_id`)으로 묶는다. 평균은 **소수 둘째 자리 반올림**, 팀에 배정되지 않은 작성자는 전체 평균에만 포함. 응답 `{gameId, completed, requiredCount, reviewedCount, averageScore, teams: [{teamId, name, reviewCount, averageScore, reasonCounts(사유 6개 전부, 0 포함), reviews: [{writerId, nickname, score, reasons, comment}]}]}`. 의견은 **작성자 닉네임과 함께** 공개한다. `member.rating`은 바꾸지 않는다(Notion 기능 표의 `member.rating` 연결은 팀 확인 필요). 없거나 삭제된 경기 404 `GAME_404_1`, 비참가자 403 `BALANCE_403_2`, 전원 작성 후 라인업이 없으면 404 `LINEUP_404_2`. 미결정: 탈퇴 등으로 끝내 작성하지 않는 참가자가 있을 때 마감(기한·파티장 강제 계산) 처리, 경기 상태 제한(현재 없음) |
| D-30 | 악성 유저 신고 정책 | 확정(2026-11-13, 이슈 #66 담당자) — 일부 미결정 | 악성 유저 신고(#66) | Notion 기능 설명이 비어 있어 이슈에서 범위를 정했다. `POST /games/{gameId}/reports`(성공 201)는 **신고자와 피신고자가 모두 그 경기에 참가한 회원**(`game_player.member_id`)일 때만 접수한다. 삭제되지 않은 경기면 상태 제한이 없다. 본문 `reportedMemberId`(필수), `reason`(필수, 하나만 선택), `detail`(선택, 500자 이하, 공백만이면 NULL 저장). 사유 값은 `ABUSE`(욕설·비방)·`VIOLENT_PLAY`(거친 플레이·폭력)·`NO_SHOW`(무단 불참)·`OTHER`(기타). 접수한 신고는 `status` `PENDING`으로 저장한다. **같은 신고자→피신고자→경기 조합에 `PENDING` 신고가 있으면 거부**한다. 스키마는 바꾸지 않고(`report`에 UNIQUE 없음) 서비스에서만 확인하므로 동시 요청은 둘 다 저장될 수 있다. `report.game_id`는 NULL을 허용하지만 경기 단위로만 받아 항상 채운다. 자기 신고 400 `REPORT_400_1`, 참가하지 않은 신고자 403 `REPORT_403_1`, 없거나 삭제된 경기 404 `GAME_404_1`, 없거나 탈퇴한 피신고자 404 `MEMBER_404_1`(D-16), 그 경기에 참가하지 않은 피신고자 404 `GAME_404_2`, 대기 중인 같은 신고 409 `REPORT_409_1`. 응답 `{id, gameId, reportedMemberId, reason, detail, status, createdAt}`. 미결정: 신고 처리(`DONE`)·조회 API와 그 권한(`member`에 관리자 역할 없음), 신고 누적 제재, 경기 상태에 따른 신고 가능 시점 |
| D-31 | 경기 MVP 투표 정책 | 확정(2026-11-16, 이슈 #70 담당자) — 일부 미결정 | 경기 MVP(#70) | `POST /games/{gameId}/mvp-votes`(성공 201)는 **해당 경기에 참가한 회원**(`game_player.member_id`)만, **경기 `status`가 `FINISHED`일 때만** 투표한다. 본문 `gamePlayerId`(필수)로 **같은 경기 참가 선수 한 명**을 고르며 비회원·후보 선수도 고를 수 있고 **본인은 고를 수 없다**. **1인 1표**(`uk_mvp_vote`)이고 수정은 없다. 응답 `{id, gameId, gamePlayerId, createdAt}`. `GET /games/{gameId}/mvp-votes/result`(성공 200)는 **그 경기에 이미 투표한 회원만** 조회한다(파티장도 투표해야 조회). 투표가 끝나지 않았어도 **현재 집계**를 준다. 응답 `{gameId, requiredCount, votedCount, mvpGamePlayerIds, candidates: [{gamePlayerId, name, voteCount}]}`. `requiredCount`는 탈퇴하지 않은 회원 참가 선수 수(D-29와 같은 기준), `candidates`는 참가 선수 전원을 득표 내림차순·참여 순으로, `mvpGamePlayerIds`는 최다 득표자(동점이면 모두). **누가 누구에게 투표했는지는 공개하지 않는다**. `GET /games/{gameId}/mvp-votes/me`(성공 200)는 로그인한 회원 누구나 **본인의** 투표 여부를 조회한다. 응답 `{gameId, voted, gamePlayerId}`(투표 전이면 `gamePlayerId` null). 없거나 삭제된 경기 404 `GAME_404_1`, 참가하지 않은 투표자 403 `MVP_403_1`, 투표하지 않은 회원의 결과 조회 403 `MVP_403_2`, 종료 전 경기 409 `MVP_409_1`, 이미 투표 409 `MVP_409_2`, 그 경기 선수가 아닌 대상 404 `GAME_404_2`, 본인 투표 400 `MVP_400_1`. 미결정: 투표 마감 기한, MVP 결과의 `member` 반영 여부 |
| D-32 | 전술 배치도 이미지 저장 방식 | 확정(2026-11-20, 이슈 #75 담당자) — 일부 미결정 | 전술 배치도 이미지 저장(#75, FE `team-Gaeddong/FOOTY_FE#57`) | **서버에 저장하지 않고 FE에서 PNG를 만들어 사용자 기기에 내려받기만** 한다. 업로드 API·파일 저장소(로컬 디스크·S3 등)·의존성 추가 없음. 이미지는 **라인업의 모든 팀 배치를 합친 1장**이고, 각 팀은 `GET /games/{gameId}/teams/{teamId}/tactic`(D-23)의 **저장된 배치**로 그린다. 저장 버튼은 **파티장에게만** 보인다(FE 노출 조건이며 서버 권한 확인 대상이 아니다. 배치 조회는 D-23대로 로그인 회원 누구나). `lineup.image_url`은 쓰지 않고 NULL로 둔다(스키마 변경 없음). 미결정: 서버 보관·공유 링크가 필요해질 때의 저장소 선택과 `image_url` 사용 방식, 파티장이 아닌 회원에게 내려받기를 열지 여부 |
| D-33 | 경기 삭제 정책 | 확정(2026-11-20, 이슈 #74 담당자 전재준) | 경기 삭제(#74) | `DELETE /games/{gameId}`(성공 204)는 **파티장만** 호출하고(403 `GAME_403_1`), `game.is_deleted`를 바꾸는 **소프트 삭제**다. **종료 전(`RECRUITING`·`CONFIRMED`)까지만** 삭제하고 종료된 경기는 409 `GAME_409_5`. 없거나 이미 삭제된 경기 404 `GAME_404_1`. 참가 선수·라인업 등 연관 데이터는 지우지 않으며, 삭제된 경기는 다른 API에서 없는 경기로 본다(`GameReader`). D-32·`GAME_409_4`는 열린 PR #73(경기 종료)이 쓰므로 건너뛴다. 삭제 취소(복구)는 범위 밖 |
| D-34 | 경기 종료 정책 | 확정(2026-11-16, 이슈 #71 담당자) — 일부 미결정 | 경기 종료(#71) | 지금까지 `game.status`를 `FINISHED`로 바꾸는 경로가 없어 종료 경기 전용 기능(D-18 재경기, D-28 밸런스 작성, D-31 MVP 투표)을 쓸 수 없었다. `PATCH /games/{gameId}/finish`(성공 200)는 **파티장만** 호출하고(403 `GAME_403_1`), **팀이 확정된(`CONFIRMED`) 경기만** 종료한다. 모집 중이거나 이미 종료된 경기는 409 `GAME_409_4`. 응답은 종료 후의 경기 상세(D-25와 같은 형식). 종료는 되돌리지 않는다(팀 확정도 종료된 경기는 거부, `LineupService.confirm`). 없거나 삭제된 경기 404 `GAME_404_1`. 미결정: `play_date`가 지나면 자동 종료할지, `play_date` 전에 종료를 막을지(현재 막지 않음) |
| D-35 | 의견 투표 정책 | 확정(2026-11-20, 이슈 #78 담당자 전재준) — #73 병합 때 D-34 자리에서 사라져 #80에서 D-35로 다시 기록 | 각종 의견 투표 생성(#78) | Notion 기능 설명이 비어 있어 이슈에서 범위를 정했다. `POST /games/{gameId}/polls`(성공 201)는 **파티장만**(403 `GAME_403_1`) 삭제되지 않은 경기면 상태와 관계없이 만든다. 제목 1~100자, 선택지 **2~10개·서로 다름**(400 `POLL_400_1`), `allowMultiple` 생략 시 단일 선택, `deadline`은 선택이며 현재 이후(400 `POLL_400_2`). 조회(`GET /games/{gameId}/polls`, `GET .../polls/{pollId}`)와 응답(`PUT .../polls/{pollId}/answers`)은 **파티장과 경기 참가 회원만**(403 `POLL_403_1`). 응답은 보낸 선택지로 **전부 교체**하고 빈 목록이면 취소하며 **마감 전에는 변경 가능**(마감 뒤 409 `POLL_409_1`). 단일 선택 투표에 2개 이상 400 `POLL_400_3`, 같은 선택지 중복 400 `POLL_400_4`, 이 투표의 선택지가 아니면 404 `POLL_404_2`(아무것도 반영 안 함), 다른 경기의 투표 404 `POLL_404_1`. 결과는 **선택지별 득표 수와 투표자 닉네임을 공개**하고 요청자의 선택(`isSelected`)을 표시한다. 투표 수정·삭제·조기 종료는 범위 밖. |
| D-36 | 직접 팀 편성 정책 | **제안**(2026-11-20, 이슈 #82) — 리뷰에서 팀이 확정해야 하며 그 전에는 채택된 규칙이 아니다 | 직접 팀 편성(#82, FE `team-Gaeddong/FOOTY_FE#68`) | 출처는 제안서 기능 ⑥ "직접 팀 편성"과 ⑦ 중 "자동 편성 결과에서 일부 선수만 직접 변경"이고 **Notion 기능 목록에는 없어 담당·스프린트 확인이 필요하다**. 지금까지 팀 짜기(D-21)가 만든 라인업에서 선수를 다른 팀으로 옮길 방법이 없었다. `PUT /games/{gameId}/lineups/latest/teams`(성공 200)는 **파티장만** 호출하고, 가장 높은 `version` 라인업의 팀 소속을 **전체 교체**한다. 본문 `{"teams": [{"teamId", "gamePlayerIds": [...]}]}`은 라인업의 모든 팀을 한 번씩 담아야 하고, 모든 `gamePlayerIds`의 합집합이 지금 그 라인업의 팀에 배정된 선수와 같아야 하며(중복·누락·다른 경기 선수 불가) **팀마다 1명 이상**이어야 한다. 옮긴 선수는 `team_member.team_id`를 바꾸고 좌표(`pos_x`·`pos_y`)를 NULL로 되돌린다(이전 팀 배치 기준이라서). **배정 포지션(`team_member.position`)·후보 여부(`is_substitute`)·`team.formation`·`lineup.method`는 바꾸지 않고**, `team.total_skill`은 후보를 뺀 선발 실력 합계로 다시 계산한다(D-22). 옮기지 않은 선수의 좌표는 그대로라서 같은 본문을 다시 보내도 결과가 같다(멱등). 응답은 `GET .../lineups/latest`(D-24)와 같은 형식이고 파티장이므로 실력을 포함한다. **팀 확정 전(라인업 미확정)이고 경기가 `RECRUITING`일 때만** 가능하다. 파티장이 아님 403 `GAME_403_1`, 없거나 삭제된 경기 404 `GAME_404_1`, 라인업 없음 404 `LINEUP_404_2`, 팀 확정됨 409 `LINEUP_409_4`(D-23과 같음), 경기가 모집 중이 아님 409 `GAME_409_1`, 선수 중복 400 `LINEUP_400_1`, 라인업에 속하지 않은 선수 400 `LINEUP_400_2`, 팀 목록이 라인업의 팀과 다름(빠짐·모르는 팀·중복) 400 `LINEUP_400_3`(새), 라인업의 선수가 빠짐 400 `LINEUP_400_4`(새), 빈 팀 400 `LINEUP_400_5`(새), 본문 형식 오류 400 `GLOBAL_400_1`. 미결정: 직접 수정한 라인업의 `method`를 `MANUAL`로 바꿀지(현재 유지), 후보 지정(D-22)을 직접 바꾸는 기능, 라인업 없이 처음부터 직접 편성하는 기능, 팀을 옮긴 선수의 배정 포지션 변경(D-23과 같은 미결정), 동시에 수정하는 두 요청의 충돌 처리(현재 나중 요청이 이김, 버전·락 없음), 한 팀 최소 인원이 1명인 것이 맞는지 |
| D-37 | 경기 목록 필터·참가 인원 | 확정(2026-11-23, 이슈 #86 담당자) — 날짜 범위의 페이지·경계 기준은 구현안(리뷰에서 확인 필요), 일부 미결정 | 경기 목록 개선(#86, FE 요청 2·3·4번) | FE 시안에서 `GET /games`로 채울 수 없던 부분을 보완한다. 선택 파라미터 `status`(쉼표 또는 반복으로 여러 개: `RECRUITING`·`CONFIRMED`·`FINISHED`), `role`(`host`·`joined`, 대소문자 무관), `from`·`to`(경기 날짜 범위)를 추가한다. 모두 생략할 수 있고 함께 쓰면 AND 조건이라서 **파라미터 없이 부르면 이전과 같다**. `role=host`는 **요청자가 파티장**인 경기, `role=joined`는 **요청자가 참가 선수**(`game_player.member_id`)인 경기이고, 파티장은 경기 생성 때 자동 참가하므로(D-14) 직접 만든 경기도 `joined`에 포함된다. `from`·`to`는 **날짜(`yyyy-MM-dd`)** 로 받고 **둘 다 그 날을 포함**한다(`from` 0시부터 `to` 다음 날 0시 전까지, `play_date`는 시간대 없는 서버 기준 일시). 한쪽만 줘도 되고, `from`이 `to`보다 늦거나 형식이 틀리면 400 `GLOBAL_400_1`. 홈 화면은 7일치를 `from`·`to`로 한 번에 받는다. **날짜 범위를 줘도 페이지 규칙은 그대로**(`size` 기본 20, 최대 100)이므로 FE는 `size=100`으로 요청하고, `totalElements`가 `size`를 넘으면 다음 쪽을 더 받아야 한다. 정렬(`playDate`·`id` 오름차순)과 삭제된 경기 제외는 그대로다. 응답의 `games[]`에 **`playerCount`**(후보 포함 참가 선수 수, 경기 상세 D-25와 같은 기준)를 추가한다. 알 수 없는 `status`·`role` 값 400 `GLOBAL_400_1`. 미결정: 날짜 범위를 주면 페이지 없이 전부 내려 줄지(지금은 `size` 상한 100), 기본 정렬을 예정 경기 우선으로 바꿀지, 목록 응답에 요청자 기준 `isJoined`를 넣을지 |
| D-38 | `member.rating`(평점) 계산·갱신 | 미결정(2026-11-23 보고, FE 요청 1번) | 프로필의 `rating`, 밸런스 취합 및 점수 계산 | FE와 `member.rating`의 의미를 "평점"으로 보기로 했으나 **계산 기준이 정해지지 않아 구현하지 않았다**. 현재 ERD 주석은 "누적 실력 점수"이고 값을 바꾸는 코드가 없어 항상 0.00이다(D-29). 결정할 것: ① **무엇을 평균 낼지** — 지금 개인에게 매겨지는 점수는 파티장이 주는 실력(`game_player.skill_level`, 1~10)뿐이고 밸런스 평가(D-28)는 경기 단위 점수다. ② **합계인지 평균인지**("평점"이면 10점 만점 평균, 현재 주석은 "누적"). ③ **갱신 시점**(경기 종료 D-34 때 반영할지). ④ **평점이 없는 회원**을 0.00으로 줄지 `null`로 줄지(`rating`은 NOT NULL이라 `null` 응답은 변환 규칙이나 스키마 변경이 필요). ⑤ **공개 범위** — 파티장 실력을 기준으로 삼으면 실력을 파티장에게만 보여 주는 정책(D-20)과 부딪힌다. 주석 변경은 ERD Cloud가 먼저 바뀌어야 한다(AGENTS.md 3절). 응답 필드명 `rating`은 유지한다는 점만 FE와 합의됐다 |
| D-39 | 내 밸런스 평가 여부 조회 | 확정(2026-11-23, 이슈 #87 담당자) | 내 밸런스 평가 조회(#87, FE 요청 5번) | 경기 후 탭의 "미작성 / 작성 완료" 표시를 위해 추가한다. `GET /games/{gameId}/balance-reviews/me`(성공 200)는 로그인한 회원 누구나 **본인의** 작성 여부만 조회한다(MVP의 `mvp-votes/me`, D-31과 같은 기준). 응답 `{gameId, reviewed, review}`이고, 작성했으면 `review`는 작성 응답(D-28)과 같은 `{id, gameId, score, reasons, comment, createdAt}`(사유는 고른 순서), 작성 전이면 `reviewed=false`·`review=null`. **참가하지 않은 회원이나 종료 전 경기도 오류가 아니라 미작성**으로 응답한다(작성 가능 여부는 알려 주지 않는다). 없거나 삭제된 경기 404 `GAME_404_1` |
