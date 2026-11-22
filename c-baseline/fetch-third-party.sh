#!/usr/bin/env bash
# Download the third-party cipher implementations used by the C baseline, pinned to the exact
# commits the baseline was built and validated with (see README.md for provenance).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
mkdir -p third_party
cd third_party

fetch() {  # fetch <dir> <git url> <commit>
  local dir=$1 url=$2 commit=$3
  if [[ -d $dir/.git ]] && [[ $(git -C "$dir" rev-parse HEAD) == "$commit" ]]; then
    echo "$dir: already at ${commit:0:7}"; return
  fi
  rm -rf "$dir"
  git init -q "$dir"
  git -C "$dir" remote add origin "$url"
  git -C "$dir" fetch -q --depth 1 origin "$commit"
  git -C "$dir" checkout -q FETCH_HEAD
  echo "$dir: ${commit:0:7} from $url"
}

fetch ascon-c       https://github.com/ascon/ascon-c.git           446347f21b209f3921c65ece70027c366cbe1693
fetch XKCP          https://github.com/XKCP/XKCP.git               d789b57c26dd7d5f628f7527c2bd006bc33b6a93
fetch gift          https://github.com/aadomn/gift.git             962e0e46fa3dcd24743a293f934c86a5a68d29e1
fetch lwc-finalists https://github.com/rweather/lwc-finalists.git  77b23e672543df736bdcc8577590e8957fe0c58c
