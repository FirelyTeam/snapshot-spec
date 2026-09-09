#!/bin/bash
# Search chat.fhir.org (read-only).
# Usage: zulip-search.sh "search terms" [stream] [num_results]
#   stream: a stream name (default: conformance), or "all" to search every public stream.
# Credentials: ~/.zulip-fhir-key containing email:api_key (never commit, never post with it).
set -euo pipefail

QUERY="${1:?usage: zulip-search.sh \"search terms\" [stream|all] [num]}"
STREAM="${2:-conformance}"
NUM="${3:-30}"
CREDS="$(cat "$HOME/.zulip-fhir-key")"

# Build narrow JSON: stream (or all public streams) + full-text search (covers topic + content)
if [ "$STREAM" = "all" ]; then
  NARROW=$(printf '[{"operator":"streams","operand":"public"},{"operator":"search","operand":"%s"}]' "$QUERY")
else
  NARROW=$(printf '[{"operator":"stream","operand":"%s"},{"operator":"search","operand":"%s"}]' "$STREAM" "$QUERY")
fi

curl -sS -u "$CREDS" -G "https://chat.fhir.org/api/v1/messages" \
  --data-urlencode "anchor=newest" \
  --data-urlencode "num_before=$NUM" \
  --data-urlencode "num_after=0" \
  --data-urlencode "narrow=$NARROW" \
  --data-urlencode "apply_markdown=false"
