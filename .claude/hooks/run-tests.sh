#!/usr/bin/env bash
# Stop 훅: Claude가 작업을 끝내려 할 때 테스트를 돌린다.
# - 서버(src, Gradle 파일)에 변경이 있으면 ./gradlew test
# - 앱(app/)에 변경이 있으면 flutter test
# 실패하면 exit 2로 결과를 돌려줘 스스로 고치게 한다 (같은 세션에서 최대 3회).
# 코드 변경이 없거나, 이미 통과한 변경 상태라면 건너뛴다 (서버·앱 따로 판단).
set -uo pipefail

input=$(cat)
session_id=$(echo "$input" | jq -r '.session_id // "unknown"')

cd "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}" || exit 0
source .claude/hooks/lib.sh

state_dir="${TMPDIR:-/tmp}/claude-traffic-gym-hooks"
mkdir -p "$state_dir"
attempts_file="$state_dir/attempts-$session_id"

# 주어진 경로들의 커밋 안 된 변경 지문. 변경이 없으면 빈 문자열
fingerprint_of() {
  if [ -z "$(git status --porcelain -- "$@")" ]; then
    echo ""
    return
  fi
  {
    git diff HEAD -- "$@"
    git ls-files --others --exclude-standard -- "$@" | while read -r f; do
      echo "$f"; cat "$f"
    done
  } | shasum | cut -d' ' -f1
}

# 실패 메시지를 모은다
failures=""

# ---- 서버 ----
server_fp=$(fingerprint_of src build.gradle.kts settings.gradle.kts)
server_passed="$state_dir/last-passed"
if [ -n "$server_fp" ] && [ "$(cat "$server_passed" 2>/dev/null)" != "$server_fp" ]; then
  output=$(./gradlew test --console=plain 2>&1)
  if [ $? -eq 0 ]; then
    echo "$server_fp" >"$server_passed"
  else
    record_caught "test" "-"
    failures+="[서버] ./gradlew test 실패"$'\n'
    failures+="$(echo "$output" | grep -vE '^> Task' | grep -E 'FAILED|tests completed|^e: ' | head -20)"$'\n'
    # 실패한 테스트의 실제 메시지는 결과 XML에 있다
    failures+="$(grep -h -A4 '<failure' build/test-results/test/*.xml 2>/dev/null | grep -vE '^\s+at ' | head -30)"$'\n'
    failures+="상세 리포트: build/reports/tests/test/index.html"$'\n'
  fi
fi

# ---- 앱 ----
app_fp=$(fingerprint_of app)
app_passed="$state_dir/last-passed-app"
if [ -n "$app_fp" ] && command -v flutter >/dev/null 2>&1 &&
  [ "$(cat "$app_passed" 2>/dev/null)" != "$app_fp" ]; then
  output=$(cd app && TZ=Asia/Seoul flutter test --no-pub 2>&1)
  if [ $? -eq 0 ]; then
    echo "$app_fp" >"$app_passed"
  else
    record_caught "test-app" "-"
    failures+="[앱] flutter test 실패"$'\n'
    # 실패한 테스트 이름과 기대값·실제값 부분만 추린다
    failures+="$(echo "$output" | grep -E '\[E\]|Expected:|Actual:|Some tests failed|Error:' | head -30)"$'\n'
  fi
fi

if [ -z "$failures" ]; then
  rm -f "$attempts_file"
  exit 0
fi

attempts=$(($(cat "$attempts_file" 2>/dev/null || echo 0) + 1))
echo "$attempts" >"$attempts_file"

# 무한 반복 방지: 3번 고쳐도 실패하면 멈추고 사람에게 넘긴다
if [ "$attempts" -gt 3 ]; then
  rm -f "$attempts_file"
  echo "테스트가 3번 연속 실패해 자동 수정을 멈췄습니다. 사용자에게 실패 내용을 보고하세요." >&2
  exit 0
fi

{
  echo "테스트 실패 (훅: run-tests, 시도 $attempts/3). 원인을 찾아 고친 뒤 다시 마무리하라."
  echo "$failures"
} >&2
exit 2
