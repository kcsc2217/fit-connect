#!/usr/bin/env bash
# UserPromptSubmit·Stop 훅: 턴 시작·끝 시각을 남긴다. docs/ai-log.md의 "Claude 작업 시간"을 계산할 때 쓴다.
# 형식: 시각<TAB>start|stop<TAB>session_id
# 사용자가 늦게 확인한 시간이 섞이지 않도록, start부터 다음 start 전 마지막 stop까지만 작업 시간으로 센다.
set -uo pipefail

event="${1:-}"
input=$(cat)
session_id=$(echo "$input" | jq -r '.session_id // "unknown"' 2>/dev/null || echo unknown)

cd "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}" || exit 0
printf '%s\t%s\t%s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$event" "$session_id" >>.claude/hooks/timing.log
exit 0
