# FOOTY_BE

축구 경기·선수·팀 편성 서비스의 Spring Boot 백엔드. 현재 코드는 애플리케이션 시작 클래스와 컨텍스트 테스트 1개이며 API·DB 계층은 아직 없다.

## 로컬 검증

Java 21, Git, Bash, Python 3가 필요하다. Gradle은 저장소 Wrapper를 사용한다. 초기 실행에는 Gradle·Maven 의존성 다운로드가 필요하다.

```bash
./scripts/harness/check.sh --mode baseline
python3 -m unittest discover -s scripts/harness/tests -v
```

baseline은 현재 소스의 빌드·기존 테스트만 확인한다. main이나 커밋 전 수정의 점검에 사용할 수 있지만 변경 제출 완료 증거는 아니다.

이슈가 연결된 기능 브랜치에서 커밋한 뒤, 실제 PR base를 기준으로 다음을 실행한다. `origin/main`은 예시이며 PR base 정책 D-10을 대신하지 않는다.

```bash
git fetch origin
./scripts/harness/check.sh --base origin/main
```

검사기는 main/develop의 변경, 기준 ref 누락, 얕은 이력, 잘못된 커밋, 빌드 실패를 차단한다. 종료 코드 0은 명시한 모드의 통과, 1은 실패, 2는 일부 검사다. `--skip-build`와 새 커밋 0개는 일부 검사이며 전체 완료로 처리하지 않는다. 실패한 빌드의 전체 로그 경로는 출력에 표시된다. 명령형 문장·기능 범위·권한·테스트 충분성은 사람/에이전트 리뷰로 확인한다.

## 에이전트 지침

- 공통 규칙: [AGENTS.md](AGENTS.md). Claude는 `CLAUDE.md`에서 이를 import한다.
- 스킬 원본: `.claude/skills`. Codex는 `.agents/skills`의 상대 심볼릭 링크로 같은 파일을 읽는다. 링크를 보존하는 checkout을 사용하고, 링크가 파일로 체크아웃된 환경에서는 링크 지원을 설정하거나 AGENTS의 원본 경로를 직접 읽는다.
- 팀 결정: [decisions.md](docs/harness/decisions.md). D-01~D-11의 제안은 채택된 정책이 아니다.
- 원본 문서: [sources.md](docs/harness/sources.md). Notion·ERD URL은 확인 전까지 미확인으로 남긴다.

## CI와 병합 보호 활성화

`.github/workflows/harness.yml`은 Linux/macOS에서 하네스 회귀 테스트와 Java 빌드를 실행한다. PR은 실제 head 커밋을 검사·빌드하고, main/develop push 및 수동 실행은 baseline 빌드를 수행한다. 두 플랫폼의 성공을 집계하는 검사 이름은 `harness`다. PR의 합성 merge 커밋을 빌드하는 설정은 아니다.

로컬 파일 추가만으로 GitHub CI와 브랜치 보호가 활성화되지는 않는다. 저장소에 변경을 반영해 워크플로가 한 번 실행된 후 관리자가 main 보호에 다음을 적용한다.

1. PR을 통한 병합과 사람 승인 최소 1명을 요구한다.
2. 필수 상태 검사로 `harness`를 등록하고 병합 전 최신 base 반영을 요구한다.
3. 관리자도 보호 규칙을 따르게 하고, 강제 push 및 브랜치 삭제를 허용하지 않는다.
4. 실제 테스트 PR에서 실패한 검사와 승인 없는 병합이 차단되는지 확인한다.

이 수정은 로컬 변경이며 원격 CI 실행·보호 설정 적용 여부는 별도로 확인해야 한다. 공개 여부·보호 설정은 변경될 수 있으므로 과거 private 플랜 오류로 판단하지 않는다.
