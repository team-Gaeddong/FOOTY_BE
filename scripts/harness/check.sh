#!/usr/bin/env bash
# 종료 코드: 0=명시한 모드 통과, 1=실패, 2=일부 검사만 완료.
set -uo pipefail

usage() {
  cat <<'USAGE'
사용법: ./scripts/harness/check.sh [--mode change|baseline] [--skip-build]
                                 [--base REF] [--head REF] [--branch NAME]
       ./scripts/harness/check.sh [--skip-build] [기준_ref]

change (기본): 브랜치 + 기준과 HEAD의 공통 조상 이후 커밋 + clean build.
  --base REF     기본 origin/main. 기존 위치 인자와 동시에 지정할 수 없음.
  --head REF     기본 HEAD. 현재 checkout의 HEAD와 같은 커밋이어야 함.
  --branch NAME  detached HEAD CI에서 실제 PR 브랜치 이름을 전달.
                일반 checkout에서는 현재 브랜치 이름과 같아야 함.
baseline: 현재 checkout의 clean build만 검사. 변경 완료 증거로 사용할 수 없음.
          --base, --head, --branch는 지정할 수 없음.
--skip-build: 빌드 생략. 다른 오류가 없어도 일부 검사 결과(종료 코드 2).

종료 코드: 0=모드별 통과, 1=실패, 2=일부 검사(빌드 생략/변경 커밋 없음).
USAGE
}
argument_error() { printf '[실패] 인자 오류: %s\n' "$1" >&2; usage >&2; exit 1; }

mode=change
base_ref=origin/main
head_ref=HEAD
branch_override=''
skip_build=false
mode_set=false
base_set=false
head_set=false
branch_set=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h) [[ $# -eq 1 ]] || argument_error '--help는 단독으로 사용'; usage; exit 0 ;;
    --skip-build)
      $skip_build && argument_error '--skip-build 중복'
      skip_build=true; shift ;;
    --mode|--base|--head|--branch)
      option="$1"
      [[ $# -ge 2 && -n "$2" && "$2" != -* ]] || argument_error "$option 값 누락 또는 잘못된 값"
      case "$option" in
        --mode) $mode_set && argument_error '--mode 중복'; mode="$2"; mode_set=true ;;
        --base) $base_set && argument_error '기준 ref 중복'; base_ref="$2"; base_set=true ;;
        --head) $head_set && argument_error '--head 중복'; head_ref="$2"; head_set=true ;;
        --branch) $branch_set && argument_error '--branch 중복'; branch_override="$2"; branch_set=true ;;
      esac
      shift 2 ;;
    -*) argument_error "알 수 없는 옵션: $1" ;;
    *)
      $base_set && argument_error '기준 ref 중복'
      [[ -n "$1" ]] || argument_error '빈 기준 ref'
      base_ref="$1"; base_set=true; shift ;;
  esac
done
[[ "$mode" == change || "$mode" == baseline ]] || argument_error "지원하지 않는 모드: $mode"
if [[ "$mode" == baseline ]] && { $base_set || $head_set || $branch_set; }; then
  argument_error 'baseline에는 --base/--head/--branch/위치 기준 ref를 사용할 수 없음'
fi

if ! cd "$(dirname "$0")/../.."; then
  printf '[실패] 저장소 디렉터리에 진입할 수 없음\n' >&2
  exit 1
fi
failed=0
partial=0
pass() { printf '  [통과] %s\n' "$1"; }
fail() { printf '  [실패] %s\n' "$1"; failed=1; }
warn() { printf '  [일부 검사] %s\n' "$1"; partial=1; }

if [[ "$mode" == change ]]; then
  echo '1. Git 기준 및 브랜치'
  history_ready=true
  if ! shallow="$(git rev-parse --is-shallow-repository)"; then
    fail 'Git 저장소 정보를 읽을 수 없음'; history_ready=false
  elif [[ "$shallow" != false ]]; then
    fail '얕은 이력으로는 커밋 범위를 검증할 수 없음. 전체 이력을 가져온 뒤 재실행'; history_ready=false
  fi
  if ! base_oid="$(git rev-parse --verify --end-of-options "${base_ref}^{commit}")"; then
    fail "기준 ref '$base_ref'가 없거나 커밋이 아님"; history_ready=false
  fi
  if ! head_oid="$(git rev-parse --verify --end-of-options "${head_ref}^{commit}")"; then
    fail "대상 ref '$head_ref'가 없거나 커밋이 아님"; history_ready=false
  fi
  if ! checkout_oid="$(git rev-parse --verify --end-of-options 'HEAD^{commit}')"; then
    fail '현재 HEAD 커밋을 읽을 수 없음'; history_ready=false
  elif [[ -n "${head_oid:-}" && "$head_oid" != "$checkout_oid" ]]; then
    fail '--head는 현재 checkout의 HEAD와 같은 커밋이어야 함'; history_ready=false
  fi

  branch=''
  if actual_branch="$(git symbolic-ref --quiet --short HEAD)"; then
    branch="$actual_branch"
    if $branch_set && [[ "$branch_override" != "$actual_branch" ]]; then
      fail '--branch가 현재 checkout 브랜치와 다름'
    fi
  else
    branch_status=$?
    if [[ $branch_status -ne 1 ]]; then
      fail '현재 브랜치를 읽을 수 없음'
    elif ! $branch_set; then
      fail 'detached HEAD에서는 --branch로 실제 PR 브랜치 이름을 지정해야 함'
    else
      branch="$branch_override"
    fi
  fi
  if [[ -n "$branch" ]]; then
    if [[ "$branch" =~ ^(feature|hotfix)/[0-9]+[-_][^[:space:]]+$ ]] && git check-ref-format --branch "$branch" > /dev/null 2>&1; then
      pass "$branch"
    else
      fail "'$branch'는 feature|hotfix/{이슈번호}_{할일} 또는 feature|hotfix/{이슈번호}-{할일} 형식이 아님 (main/develop 변경 검사 금지)"
    fi
  fi

  echo '2. 변경 커밋'
  if $history_ready; then
    if ! merge_base="$(git merge-base "$base_oid" "$head_oid")" || [[ -z "$merge_base" ]]; then
      fail '기준과 대상의 공통 조상을 찾을 수 없음'
    elif ! commit_count="$(git rev-list --count "$merge_base..$head_oid")"; then
      fail '커밋 범위를 읽을 수 없음'
    elif [[ ! "$commit_count" =~ ^[0-9]+$ ]]; then
      fail '커밋 개수 결과가 잘못됨'
    elif [[ "$commit_count" == 0 ]]; then
      warn '검사할 새 커밋이 없음. 변경 완료 검증으로 사용할 수 없음'
    else
      printf '  범위: %s..%s (%s개)\n' "$merge_base" "$head_oid" "$commit_count"
      if ! git log --format='%h%x09%s' "$merge_base..$head_oid" | python3 scripts/harness/check_commits.py; then
        fail '커밋 조회 또는 메시지 검증 실패'
      fi
    fi
  else
    fail '유효한 전체 이력이 없어 커밋 검사를 수행할 수 없음'
  fi
else
  echo 'baseline: 현재 checkout의 빌드·테스트만 검사 (변경 완료 검증 아님)'
fi

echo '3. 빌드·테스트'
if $skip_build; then
  warn '--skip-build 지정: 빌드·테스트를 검증하지 않음'
else
  # BSD/macOS와 GNU mktemp 모두 지원. clean이 삭제하는 build/ 밖에 기록.
  if ! build_log="$(mktemp "${TMPDIR:-/tmp}/footy-harness-build.XXXXXX")"; then
    fail '빌드 로그 임시 파일을 만들 수 없음'
  elif ./gradlew clean build --console=plain > "$build_log" 2>&1; then
    pass './gradlew clean build'
    if ! rm -f "$build_log"; then
      fail "성공한 빌드 로그를 정리할 수 없음: $build_log"
    fi
  else
    fail "./gradlew clean build (전체 로그: $build_log, 마지막 30줄)"
    tail -n 30 "$build_log" | sed 's/^/      /'
  fi
fi

echo
if [[ $failed -ne 0 ]]; then
  echo "결과: 실패 ($mode 모드)"
  exit 1
elif [[ $partial -ne 0 ]]; then
  echo "결과: 일부 검사 ($mode 모드, 변경 완료 증거로 사용할 수 없음)"
  exit 2
elif [[ "$mode" == baseline ]]; then
  echo '결과: 기준 빌드 통과 (baseline 모드, 변경 완료 증거로 사용할 수 없음)'
else
  echo '결과: 변경 검사 통과 (change 모드: 브랜치·커밋·빌드)'
fi
