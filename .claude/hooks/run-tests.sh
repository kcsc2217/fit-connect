#!/usr/bin/env bash
# Stop 훅: Claude가 작업을 끝내려 할 때 테스트를 돌린다.
# 실패하면 exit 2로 결과를 돌려줘 스스로 고치게 한다 (같은 세션에서 최대 3회).
# 코드 변경이 없거나, 이미 통과한 변경 상태라면 건너뛴다.
set -uo pipefail

input=$(cat)
session_id=$(echo "$input" | jq -r '.session_id // "unknown"')

cd "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}" || exit 0
source .claude/hooks/lib.sh

watched=(src build.gradle.kts settings.gradle.kts)

# 커밋되지 않은 코드 변경이 없으면 건너뛴다
if [ -z "$(git status --porcelain -- "${watched[@]}")" ]; then
  exit 0
fi

# 현재 변경 상태의 지문. 마지막으로 통과한 상태와 같으면 다시 돌리지 않는다
fingerprint=$(
  {
    git diff HEAD -- "${watched[@]}"
    git ls-files --others --exclude-standard -- "${watched[@]}" | while read -r f; do
      echo "$f"; cat "$f"
    done
  } | shasum | cut -d' ' -f1
)
state_dir="${TMPDIR:-/tmp}/claude-traffic-gym-hooks"
mkdir -p "$state_dir"
passed_file="$state_dir/last-passed"
attempts_file="$state_dir/attempts-$session_id"

if [ -f "$passed_file" ] && [ "$(cat "$passed_file")" = "$fingerprint" ]; then
  exit 0
fi

output=$(./gradlew test --console=plain 2>&1)
if [ $? -eq 0 ]; then
  echo "$fingerprint" >"$passed_file"
  rm -f "$attempts_file"
  exit 0
fi

attempts=$(($(cat "$attempts_file" 2>/dev/null || echo 0) + 1))
echo "$attempts" >"$attempts_file"
record_caught "test" "-"

# 무한 반복 방지: 3번 고쳐도 실패하면 멈추고 사람에게 넘긴다
if [ "$attempts" -gt 3 ]; then
  rm -f "$attempts_file"
  echo "테스트가 3번 연속 실패해 자동 수정을 멈췄습니다. 사용자에게 실패 내용을 보고하세요." >&2
  exit 0
fi

{
  echo "테스트 실패 (훅: run-tests, 시도 $attempts/3). 원인을 찾아 고친 뒤 다시 마무리하라."
  echo "$output" | grep -vE '^> Task' | grep -E 'FAILED|tests completed|^e: ' | head -20
  # 실패한 테스트의 실제 메시지는 결과 XML에 있다
  grep -h -A4 '<failure' build/test-results/test/*.xml 2>/dev/null | grep -vE '^\s+at ' | head -30
  echo "상세 리포트: build/reports/tests/test/index.html"
} >&2
exit 2
