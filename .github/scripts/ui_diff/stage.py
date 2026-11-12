"""비교 워크플로(`ui-diff.yml`)에서 기준 PNG를 옮기고 결과 아티팩트와 메타를 만든다.

이 스크립트는 PR 코드를 그대로 돌리는 권한 없는 잡에서만 쓴다. 산출물을 믿을지는 받는 쪽(`report.py`)이 정한다.
"""

import argparse
import json
import os
import re
import shutil
import stat
import sys
from pathlib import Path

# OUTPUTS·RESULTS·STATUSES 는 report.py 와 같은 계약이다. 한쪽을 바꾸면 다른 쪽도 바꾼다(test_stage 가 확인한다)
OUTPUTS = ("build", "outputs", "roborazzi")
RESULTS = ("build", "test-results", "roborazzi", "debug", "results")
SKIP_DIRS = {".git", ".gradle", ".kotlin", ".idea", "node_modules"}
STATUSES = ("ok", "no-base-setup", "merge-conflict", "failed")
SHA_RE = re.compile(r"[0-9a-f]{40}")
TEST_TASK = "testDebugUnitTest"
SNAPSHOT_TASK_RE = re.compile(r"^(:[A-Za-z0-9_\-:]+):(?:record|compare)RoborazziDebug SKIPPED\s*$")
TASK_LINE_RE = re.compile(r"^> Task (:[A-Za-z0-9_\-:]+):" + TEST_TASK + r"(?: (\S+))?\s*$")


def module_dirs_with(root: Path, tail: tuple):
    """`<module>/build/...` 꼴의 `tail` 디렉터리를 찾는다. `build` 아래로는 더 내려가지 않는다."""
    for dirpath, dirnames, _ in os.walk(root, followlinks=False):
        here = Path(dirpath)
        # 루트 프로젝트의 build/ 는 모듈이 아니다
        if "build" in dirnames and here != root:
            target = here.joinpath(*tail)
            if target.is_dir() and not target.is_symlink():
                yield here.relative_to(root).as_posix(), target
        dirnames[:] = sorted(d for d in dirnames if d not in SKIP_DIRS and d != "build")


def regular_files(directory: Path, suffix: str):
    for entry in sorted(directory.iterdir()):
        if entry.name.endswith(suffix) and stat.S_ISREG(os.lstat(entry).st_mode):
            yield entry


def copy_into(dest_root: Path, rel: str, source: Path) -> None:
    target = dest_root / rel
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)


def cmd_stash(args) -> int:
    root, dest = Path(args.root), Path(args.dest)
    names = []
    for module_dir, outputs in module_dirs_with(root, OUTPUTS):
        for png in regular_files(outputs, ".png"):
            rel = "/".join((module_dir, *OUTPUTS, png.name))
            copy_into(dest, rel, png)
            names.append(rel)
    Path(args.list).write_text("".join(n + "\n" for n in names), encoding="utf-8")
    print(f"stashed {len(names)} base images")
    return 0


def cmd_restore(args) -> int:
    src, root = Path(args.src), Path(args.root)
    count = 0
    if src.is_dir():
        for dirpath, _, filenames in os.walk(src, followlinks=False):
            for name in filenames:
                source = Path(dirpath) / name
                copy_into(root, source.relative_to(src).as_posix(), source)
                count += 1
    print(f"restored {count} base images")
    return 0


def cmd_stage_results(args) -> int:
    root, dest = Path(args.root), Path(args.dest)
    images = results = 0
    for module_dir, outputs in module_dirs_with(root, OUTPUTS):
        for png in regular_files(outputs, ".png"):
            copy_into(dest, "/".join((module_dir, *OUTPUTS, png.name)), png)
            images += 1
    for module_dir, directory in module_dirs_with(root, RESULTS):
        for result in regular_files(directory, ".json"):
            copy_into(dest, "/".join((module_dir, *RESULTS, result.name)), result)
            results += 1
    print(f"staged {images} images, {results} results")
    return 0


def read_lines(path: str | None) -> list[str]:
    if not path or not Path(path).is_file():
        return []
    return Path(path).read_text(encoding="utf-8", errors="replace").splitlines()


def snapshot_modules(plan_lines: list[str]) -> set[str]:
    """`recordRoborazziDebug`·`compareRoborazziDebug`의 `--dry-run` 출력에서 스냅숏 모듈의 Gradle 경로를 뽑는다."""
    return {m.group(1) for line in plan_lines if (m := SNAPSHOT_TASK_RE.match(line))}


def failed_modules(modules: set[str], log_lines: list[str]) -> set[str]:
    """테스트 태스크가 실패했거나, 앞선 태스크 실패로 아예 돌지 못한 모듈."""
    passed, failed = set(), set()
    for line in log_lines:
        m = TASK_LINE_RE.match(line)
        if not m:
            continue
        (failed if m.group(2) == "FAILED" else passed).add(m.group(1))
    return failed | (modules - passed)


def count_results(results_root: Path) -> int:
    return sum(1 for _, directory in module_dirs_with(results_root, RESULTS) for _ in regular_files(directory, ".json"))


def build_meta(args) -> dict:
    meta = {"pr": int(args.pr), "head_sha": args.head_sha, "base_sha": args.base_sha, "status": args.status or "ok", "failed_modules": []}
    if args.status:
        return meta
    base_modules = snapshot_modules(read_lines(args.base_plan))
    head_modules = snapshot_modules(read_lines(args.head_plan))
    failed = failed_modules(base_modules, read_lines(args.base_log)) | failed_modules(head_modules, read_lines(args.head_log))
    meta["failed_modules"] = sorted(failed)
    every_module_failed = bool(base_modules | head_modules) and (base_modules | head_modules) <= failed
    if every_module_failed or count_results(Path(args.results_root)) == 0:
        meta["status"] = "failed"
    return meta


def cmd_meta(args) -> int:
    if not args.pr.isdigit() or int(args.pr) <= 0:
        raise ValueError("invalid pr")
    for sha in (args.head_sha, args.base_sha):
        if not SHA_RE.fullmatch(sha):
            raise ValueError("invalid sha")
    if args.status and args.status not in STATUSES:
        raise ValueError("invalid status")
    meta = build_meta(args)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
        f.write("\n")
    print(json.dumps(meta))
    return 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    stash = sub.add_parser("stash")
    for flag in ("--root", "--dest", "--list"):
        stash.add_argument(flag, required=True)
    restore = sub.add_parser("restore")
    for flag in ("--src", "--root"):
        restore.add_argument(flag, required=True)
    stage = sub.add_parser("stage-results")
    for flag in ("--root", "--dest"):
        stage.add_argument(flag, required=True)
    meta = sub.add_parser("meta")
    for flag in ("--out", "--pr", "--head-sha", "--base-sha"):
        meta.add_argument(flag, required=True)
    # --status 를 주면 비교 없이 그 상태로 끝낸다(merge-conflict, no-base-setup)
    for flag in ("--status", "--base-plan", "--base-log", "--head-plan", "--head-log", "--results-root"):
        meta.add_argument(flag, default="")
    args = parser.parse_args(argv)
    handler = {"stash": cmd_stash, "restore": cmd_restore, "stage-results": cmd_stage_results, "meta": cmd_meta}[args.command]
    try:
        return handler(args)
    except (OSError, ValueError) as error:
        print(f"ui-diff stage: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
