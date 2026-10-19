---
name: footy-code-review
description: FOOTY_BE의 PR이나 로컬 변경을 팀 규칙·ERD·Notion 기능 기준으로 리뷰하는 절차와 보고 형식. 코드 리뷰, PR 점검, 병합 전 확인을 요청받았을 때 사용한다.
---

# FOOTY 코드 리뷰 워크플로

에이전트 리뷰는 참고 의견이다. Notion 규칙상 병합에는 사람 리뷰어 1명 이상의 approve가 필요하고, 에이전트는 PR을 approve·merge하지 않는다. PR에 코멘트를 남기는 것도 사람이 요청했을 때만 한다.

## 1. 대상 파악

```bash
gh pr view <번호> --json number,title,body,headRefName,baseRefName,files
gh pr diff <번호>
# 로컬 변경이면 실제 base를 기준으로 커밋·미커밋 변경 모두 확인
git diff origin/main...HEAD
git diff
git diff --cached
git status --short
```

- 연결 이슈와 Notion 기능을 확인한다(`footy-api-implement/references/feature-map.md`).
- PR 브랜치를 체크아웃해 검증을 직접 돌린다. 리뷰하는 동안 코드를 고치지 않는다.

## 2. 검증 실행

```bash
./scripts/harness/check.sh --base origin/main
```

base는 실제 PR 대상에 맞춘다. 커밋 전 로컬 감사는 `--mode baseline`으로 빌드를 검증하고 브랜치·커밋 검증 미완료를 명시한다. 하네스 변경이면 `python3 -m unittest discover -s scripts/harness/tests -v`도 실행한다. detached HEAD에서는 실제 PR head를 checkout한 뒤 `--head <head SHA> --branch <원본 브랜치>`를 전달한다. 인자를 임의로 바꿔 검사를 우회하지 않는다.

결과(모드, 기준 ref, 종료 코드, 통과/실패/일부 검사와 실패 출력)를 리뷰 맨 앞에 적는다. baseline 통과·종료 코드 2는 변경 완료 증거가 아니다.

## 3. 점검 항목

| # | 항목 | 기준 |
|---|---|---|
| R1 | 범위 | 변경이 연결 이슈의 기능 하나에 머무는가 |
| R2 | 스키마 | 엔티티·컬럼·NULL이 `docs/erd/footy_erd.sql`과, UNIQUE가 `docs/erd/constraints.md`와 같은가. 스키마 밖 컬럼 추가는 없는가 |
| R3 | 미결정 사항 | `decisions.md`에서 `미결정`인 항목을 코드가 임의로 정하지 않았는가 |
| R4 | 확정 규칙 | `확정` 항목(계층·응답·에러 형식 등)을 따르는가 |
| R5 | 정확성 | 요구사항의 입력·출력·예외가 구현과 맞는가. NULL, 빈 목록, 중복, 소프트 삭제된 대상 처리 |
| R6 | 권한 | 파티장 전용 동작을 서버에서 확인하는가. 본인 데이터만 수정 가능한가 |
| R7 | 보안 | 비밀정보(JWT 키, OAuth 시크릿, DB·Redis 접속 정보) 하드코딩 없음, 응답에 토큰·내부 정보 노출 없음, 온보딩 미완료 회원의 접근 제한 |
| R8 | 테스트 | 기존 테스트 실행 여부와 정상·예외 경로 검증. D-09 확정 전 새 테스트 누락은 권고이며 미검증 범위를 보고한다. 사용자·이슈 또는 확정된 D-09가 필수로 요구한 테스트 누락은 완료 불가 |
| R9 | Git 규칙 | 브랜치 이름, 커밋 메시지, PR 본문이 `footy-git-workflow`를 따르는가 |

## 4. 보고 형식

```markdown
## 검증
check.sh: 모드 / 기준 ref / 종료 코드 / 통과·실패·일부 검사 (출력)

## 발견 사항
| 심각도 | 위치 | 항목 | 내용 | 근거 |
|---|---|---|---|---|
| 차단 | src/.../MemberService.java:42 | R6 | 탈퇴한 회원도 로그인됨 | AGENTS.md 3장 소프트 삭제 |

## 문서·코드 충돌
- (decisions.md 형식, 없으면 "없음")

## 결론
병합 가능 / 수정 필요 / 결정 필요
```

심각도
- **차단**: 잘못된 동작, 보안 문제, 스키마 불일치, 빌드·테스트 실패, 명시적으로 요구된 테스트 누락, 미결정 사항 임의 결정
- **수정 권고**: 동작은 맞지만 보완이 필요함(선택 테스트 누락 등). 자동 검사 실패는 변경 제출 전에 해결
- **참고**: 취향이나 개선 제안. 3개 이하로 제한

추측으로 결함을 만들지 않는다. 각 발견 사항은 코드 위치와 재현 조건 또는 문서 근거를 함께 적는다.

## 완료 조건

- [ ] `check.sh`를 실제로 실행하고 결과를 적었다
- [ ] R1~R9를 모두 확인했다
- [ ] 발견 사항마다 위치와 근거가 있다
- [ ] 충돌은 결정하지 않고 보고했다
