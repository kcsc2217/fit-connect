#!/usr/bin/env bash
# PostToolUse(Edit|Write) 훅: 파일을 고친 직후 포맷 + 검사.
# - Kotlin(.kt, .kts): ktlint 포맷(설치 시) + 컴파일
# - Dart(app/**/*.dart): dart format + flutter analyze
# 실패하면 exit 2로 에러를 Claude에게 돌려줘 바로 고치게 한다.
set -uo pipefail

input=$(cat)
file=$(echo "$input" | jq -r '.tool_input.file_path // empty')

cd "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}" || exit 0
source .claude/hooks/lib.sh

case "$file" in
  *.kt | *.kts) ;;
  */app/*.dart)
    command -v flutter >/dev/null 2>&1 || exit 0
    [ -f "$file" ] && dart format "$file" >/dev/null 2>&1
    output=$(cd app && flutter analyze --no-pub 2>&1)
    if [ $? -ne 0 ]; then
      record_caught "analyze" "$file"
      {
        echo "flutter analyze 실패 (훅: format-and-compile). 아래 문제를 고쳐라."
        echo "$output" | grep -E '(error|warning|info) •' | head -30
      } >&2
      exit 2
    fi
    exit 0
    ;;
  *) exit 0 ;;
esac

# 포맷터: ktlint가 설치돼 있을 때만 (brew install ktlint)
if command -v ktlint >/dev/null 2>&1 && [ -f "$file" ]; then
  ktlint --format "$file" >/dev/null 2>&1 || true
fi

output=$(./gradlew compileKotlin compileTestKotlin -q --console=plain 2>&1)
if [ $? -ne 0 ]; then
  record_caught "compile" "$file"
  {
    echo "컴파일 실패 (훅: format-and-compile). 아래 에러를 고쳐라."
    # Kotlin 컴파일 에러(e: ...)를 우선 보여주고, 없으면 마지막 출력을 보여준다
    errors=$(echo "$output" | grep -E '^e: ' | head -30)
    if [ -n "$errors" ]; then echo "$errors"; else echo "$output" | tail -30; fi
  } >&2
  exit 2
fi

exit 0
