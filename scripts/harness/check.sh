#!/usr/bin/env bash
# FOOTY_BE 하네스 검증: 빌드·테스트, 브랜치 이름, 커밋 메시지 형식.
# 사용법: ./scripts/harness/check.sh [--skip-build] [기준_ref]
# 기준_ref 기본값은 origin/main. 이 ref 이후의 커밋만 검사한다.
set -uo pipefail

cd "$(dirname "$0")/../.."

skip_build=false
base_ref="origin/main"
for arg in "$@"; do
  case "$arg" in
    --skip-build) skip_build=true ;;
    *) base_ref="$arg" ;;
  esac
done

failed=0
pass() { printf '  [통과] %s\n' "$1"; }
fail() { printf '  [실패] %s\n' "$1"; failed=1; }
warn() { printf '  [주의] %s\n' "$1"; }

echo "1. 빌드·테스트"
if $skip_build; then
  warn "--skip-build 지정: 빌드를 건너뜀"
else
  # clean이 build/를 지우므로 로그는 임시 파일에 남긴다.
  build_log="$(mktemp -t footy-harness-build)"
  if ./gradlew clean build --console=plain > "$build_log" 2>&1; then
    pass "./gradlew clean build"
  else
    fail "./gradlew clean build (전체 로그: $build_log, 마지막 30줄)"
    tail -n 30 "$build_log" | sed 's/^/      /'
  fi
fi

echo "2. 브랜치 이름"
branch="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$branch" == "main" || "$branch" == "develop" ]]; then
  warn "현재 브랜치가 '$branch'. 기능 작업은 feature/{이슈번호}_{할일} 브랜치에서 한다"
elif [[ "$branch" =~ ^(feature|hotfix)/[0-9]+_[^[:space:]]+$ ]]; then
  pass "$branch"
else
  fail "'$branch' 는 feature/{이슈번호}_{할일} 또는 hotfix/{이슈번호}_{할일} 형식이 아님"
fi

echo "3. 커밋 메시지 ($base_ref 이후)"
if ! git rev-parse --verify --quiet "$base_ref" > /dev/null; then
  warn "'$base_ref' 를 찾을 수 없어 커밋 검사를 건너뜀 (git fetch 필요)"
else
  subjects="$(git log --format='%h%x09%s' "$base_ref..HEAD")"
  if [[ -z "$subjects" ]]; then
    warn "검사할 새 커밋이 없음"
  else
    # 형식: {타입이모지} :: [(#번호)][[범위]] 제목  — 범위 표기와 일반 커밋의 이슈 번호는 decisions.md C-04로 미결정이라 허용만 한다.
    result="$(printf '%s\n' "$subjects" | python3 -c '
import re, sys
types = ["📄", "⚡", "⚰", "🐛", "♻", "📝", "⚙", "🧪", "🚀", "🔀"]
pat = re.compile(r"^(" + "|".join(map(re.escape, types)) + r")️? :: (\(#\d+\) ?)?(\[[^\]]+\] ?)?(?P<subject>\S.*)$")
bad = 0
for line in sys.stdin:
    sha, _, msg = line.rstrip("\n").partition("\t")
    m = pat.match(msg)
    if not m:
        print(f"FAIL {sha} 형식 위반: {msg}"); bad = 1
    elif len(m.group("subject")) > 50:
        print(f"FAIL {sha} 제목 50자 초과: {msg}"); bad = 1
    else:
        print(f"OK {sha} {msg}")
sys.exit(bad)
')"
    status=$?
    while IFS= read -r line; do
      case "$line" in
        OK*) pass "${line#OK }" ;;
        FAIL*) fail "${line#FAIL }" ;;
      esac
    done <<< "$result"
    [[ $status -ne 0 ]] && failed=1
  fi
fi

echo
if [[ $failed -eq 0 ]]; then
  echo "결과: 통과"
else
  echo "결과: 실패"
fi
exit $failed
