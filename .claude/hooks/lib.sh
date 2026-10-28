#!/usr/bin/env bash
# 훅 공통 함수

# 훅이 잡은 오류를 기록한다. docs/ai-log.md의 "훅이 잡은 오류" 칸을 채울 때 쓴다.
# 형식: 시각<TAB>훅 종류<TAB>파일
record_caught() {
  local kind="$1" file="$2"
  printf '%s\t%s\t%s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$kind" "$file" >>.claude/hooks/caught.log
}
