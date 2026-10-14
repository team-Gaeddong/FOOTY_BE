#!/usr/bin/env python3
"""Validate Git subject records from check.sh; semantic review remains manual."""
import re
import sys
import unicodedata

TYPES = ["📄", "⚡", "⚰", "🐛", "♻", "📝", "⚙", "🧪", "🚀", "🔀"]
PATTERN = re.compile(
    r"^(?P<type>" + "|".join(map(re.escape, TYPES))
    + r")️? :: (?P<issue>\(#\d+\) ?)?(?:\[[^\]]+\] ?)?(?P<subject>\S.*)$"
)
KOREAN = re.compile(r"[가-힣ㄱ-ㅎㅏ-ㅣᄀ-ᇿꥠ-꥿ힰ-퟿]")


def main():
    failed = False
    count = 0
    for record in sys.stdin:
        count += 1
        sha, separator, message = record.rstrip("\n").partition("\t")
        match = PATTERN.fullmatch(message) if separator else None
        reason = None
        if match is None:
            reason = "형식 위반"
        elif match["type"] == "🔀" and match["issue"] is None:
            reason = "PR·merge 타입(🔀)에는 (#이슈번호)가 필요"
        else:
            # NFC avoids counting a decomposed Korean syllable as several letters.
            subject = unicodedata.normalize("NFC", match["subject"])
            if len(subject) > 50:
                reason = "제목 50자 초과"
            elif KOREAN.search(subject) is None:
                reason = "제목에 한국어가 없음"
        if reason:
            print(f"  [실패] {sha} {reason}: {message}")
            failed = True
        else:
            print(f"  [통과] {sha} {message}")
    if count == 0:
        print("  [실패] 커밋 조회 결과가 비어 있음")
        failed = True
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
