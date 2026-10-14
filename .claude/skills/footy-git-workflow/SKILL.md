---
name: footy-git-workflow
description: FOOTY_BE의 이슈 생성, 브랜치 이름, 커밋 메시지, PR, 버전 규칙. 이슈를 만들거나 브랜치를 따거나 커밋·PR을 작성할 때 사용한다.
---

# FOOTY 이슈·브랜치·커밋·PR 규칙

출처: Notion `FOOTY > Github Commit 명세` (2026-10-14 확인). 원본과 다르면 원본이 우선이다.

## 개발 프로세스

1. 이슈 생성 — 기능 1개당 이슈 1개, 성격에 맞는 라벨 지정
2. 이슈 기반 브랜치 생성
3. 이슈 범위의 기능 개발 완료
4. PR 또는 merge commit도 커밋 메시지 규칙을 따른다
5. 병합 조건: 리뷰어 최소 1명 approve, CI build success, 테스트 작성(선택)
6. 병합 후 feature 브랜치 삭제
7. 이슈 close

## 라벨 (Notion 정의)

| Label | 의미 |
|---|---|
| `BUG` | 예기치 않은 문제, 의도하지 않은 동작 |
| `feature` | 새 기능 요청 |
| `DOCS` | 문서 추가·개선 |
| `Chore` | 자잘한 수정 |
| `Refactor` | 리팩토링 필요 |
| `프로젝트 설정` | 프로젝트 설정 필요 |
| `테스트 추가` | 새 테스트 요청 |

현재 GitHub 저장소에는 이 라벨이 없고 GitHub 기본 라벨만 있다(`decisions.md` C-03). 라벨을 직접 만들지 말고 사람에게 알린다.

## 브랜치

```
feature/{issue number}_{todo}
hotfix/{issue number}_{todo}
```

예: `feature/12_member-signup`. Notion은 "기본적으로 GitFlow"라고 하지만 `develop` 브랜치가 없다(`decisions.md` C-01). PR의 base 브랜치가 정해지기 전에는 사람에게 확인한다.

## 커밋 메시지

```
CommitType :: (#issue number) Subject
```

| CommitType | 의미 |
|---|---|
| `📄 ::` | 파일 생성 및 구조 변경 |
| `⚡ ::` | 기능 업데이트 |
| `⚰️ ::` | 기능 삭제 |
| `🐛 ::` | 버그 수정 |
| `♻️ ::` | 코드 리팩토링 |
| `📝 ::` | 문서 작성 및 수정 |
| `⚙️ ::` | 프로젝트 세팅 |
| `🧪 ::` | 테스트 코드 추가 |
| `🚀 ::` | 새 버전 릴리즈 |
| `🔀 ::` | Merge or PR |

- `(#issue number)`: Notion 설명은 "merge commit이나 PR을 날릴 때만 사용"이지만, 같은 문서의 예시에는 일반 커밋에도 붙어 있다(`decisions.md` C-04). 결정 전에는 PR·merge 커밋에만 붙인다.
- Subject: 50자 이내, 명령형, 한국어, "~수정했다" 대신 "~수정", "Add~" 대신 "~추가"
- Notion 예시에는 `[MainPage]`처럼 대괄호 범위 표기가 있으나 형식 정의에는 없다(C-04). 쓰지 않는 것을 기본으로 한다.

예:
```
🐛 :: 헤더가 안보이는 버그 수정
🔀 :: (#12) 회원가입 API 추가
📝 :: README 파일 수정
```

## PR 본문 템플릿

```markdown
## 연결 이슈
close #이슈번호

## 변경 사항
- 

## 검증
./scripts/harness/check.sh 출력 붙이기

## 확인이 필요한 점
- 문서 충돌·미결정 사항 (decisions.md 항목 번호)
```

## 버전 규칙

`X.Y.Z` = Major(전체 변경, API 깨짐) · Minor(기능 추가·수정) · Patch(버그·내부 보완). 표기 예: `01.01.09` → `1.1.9`. 현재 `build.gradle`의 `0.0.1-SNAPSHOT`과 Notion의 시작 버전이 다르다(`decisions.md` C-05). 버전은 사람이 올린다.

## 검증

```bash
./scripts/harness/check.sh
```

브랜치 이름과 `origin/main` 이후 커밋 메시지 형식을 함께 검사한다.
