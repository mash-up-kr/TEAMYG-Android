"""닫힌 지 오래된 PR 의 스크린샷 이미지 브랜치(`ui-snapshots/pr-<N>`)를 지운다."""

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

PREFIX = "ui-snapshots/"
BRANCH_RE = re.compile(r"ui-snapshots/pr-([1-9][0-9]*)")
REPO_RE = re.compile(r"[A-Za-z0-9_.\-]+/[A-Za-z0-9_.\-]+")


@dataclass(frozen=True)
class PrState:
    state: str  # "open" | "closed"
    closed_at: datetime | None  # timezone-aware UTC


def pr_number(branch: str) -> int | None:
    # `$` 는 끝의 개행을 허용하므로 fullmatch 를 쓴다. `[0-9]` 로 유니코드 숫자도 막는다.
    m = BRANCH_RE.fullmatch(branch)
    return int(m.group(1)) if m else None


def select_expired(
    branches: list[str],
    prs: dict[int, PrState],
    now: datetime,
    keep_days: int = 30,
) -> list[str]:
    """닫힌 지 keep_days 일을 넘긴 PR 의 브랜치만 돌려준다. 조회 실패·열림·날짜 없음은 유지."""
    limit = timedelta(days=keep_days)
    expired = []
    for branch in branches:
        number = pr_number(branch)
        pr = prs.get(number) if number is not None else None
        if pr is None or pr.state != "closed" or pr.closed_at is None:
            continue
        if now - pr.closed_at > limit:
            expired.append(branch)
    return expired


def gh_api(*args: str) -> str:
    return subprocess.run(["gh", "api", *args], check=True, capture_output=True, text=True).stdout


def list_branches(repo: str) -> list[str]:
    # --paginate 는 페이지마다 JSON 배열을 이어 붙여 출력한다.
    out = gh_api("--paginate", "--jq", ".[].ref", f"repos/{repo}/git/matching-refs/heads/{PREFIX}")
    return [line.removeprefix("refs/heads/") for line in out.splitlines() if line]


def fetch_pr(repo: str, number: int) -> PrState | None:
    try:
        data = json.loads(gh_api(f"repos/{repo}/pulls/{number}"))
        state = data["state"]
        raw = data.get("closed_at")
        closed_at = datetime.fromisoformat(raw.replace("Z", "+00:00")) if raw else None
        if closed_at is not None and closed_at.tzinfo is None:
            closed_at = closed_at.replace(tzinfo=timezone.utc)
        return PrState(state, closed_at)
    except (subprocess.CalledProcessError, KeyError, ValueError, TypeError, AttributeError):
        return None


def delete_branch(repo: str, branch: str) -> None:
    gh_api("-X", "DELETE", f"repos/{repo}/git/refs/heads/{branch}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", required=True)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--keep-days", type=int, default=30)
    args = parser.parse_args(argv)
    if not REPO_RE.fullmatch(args.repo):
        print(f"잘못된 저장소: {args.repo}", file=sys.stderr)
        return 2

    branches = list_branches(args.repo)
    candidates = [b for b in branches if pr_number(b) is not None]
    prs = {}
    for branch in candidates:
        number = pr_number(branch)
        pr = fetch_pr(args.repo, number)
        if pr is not None:
            prs[number] = pr
    expired = select_expired(candidates, prs, datetime.now(timezone.utc), args.keep_days)

    failed = 0
    for branch in expired:
        if args.dry_run:
            print(f"[dry-run] 삭제 대상: {branch}")
            continue
        try:
            delete_branch(args.repo, branch)
            print(f"삭제: {branch}")
        except subprocess.CalledProcessError as e:
            failed += 1
            print(f"삭제 실패: {branch}: {e.stderr}", file=sys.stderr)
    print(f"후보 {len(candidates)}개 중 만료 {len(expired)}개")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
