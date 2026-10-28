#!/usr/bin/env bash
# PostToolUse(Edit|Write) 훅: Kotlin 파일을 고친 직후 포맷 + 컴파일 체크.
# 컴파일이 실패하면 exit 2로 에러를 Claude에게 돌려줘 바로 고치게 한다.
set -uo pipefail

input=$(cat)
file=$(echo "$input" | jq -r '.tool_input.file_path // empty')

case "$file" in
  *.kt | *.kts) ;;
  *) exit 0 ;;
esac

cd "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}" || exit 0
source .claude/hooks/lib.sh

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
