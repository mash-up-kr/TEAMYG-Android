"""PR 스크린샷 비교 결과를 모아 PR 코멘트로 만든다.

아티팩트는 PR 코드가 만든 것이라 파일·경로·메타 값을 전부 신뢰하지 않는다.
"""

import argparse
import html
import json
import os
import re
import stat
import sys
import urllib.parse
from dataclasses import asdict, dataclass, field
from pathlib import Path, PurePosixPath

MARKER = "<!-- parfait-ui-diff-report -->"
# OUTPUTS·RESULTS·STATUSES 는 stage.py 와 같은 계약이다. 한쪽을 바꾸면 다른 쪽도 바꾼다(test_stage 가 확인한다)
OUTPUTS = ("build", "outputs", "roborazzi")
RESULTS = ("build", "test-results", "roborazzi", "debug", "results")
NAME_PREFIX = "com.teamyg.parfait."
NAME_WRAP = 28
MAX_JSON_BYTES = 1 << 20
MAX_IMAGE_BYTES = 5 << 20
# 이미지 브랜치 한 번에 올리는 양의 상한. 넘는 행은 이미지 없이 남긴다
MAX_PUSH_IMAGES = 200
MAX_PUSH_BYTES = 100 << 20
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
STATUSES = ("ok", "no-base-setup", "merge-conflict", "failed")
STATUS_MESSAGES = {
    "no-base-setup": "base에 스크린샷 구성이 없어 비교하지 못했습니다.",
    "merge-conflict": "충돌로 merge ref가 없어 비교하지 못했습니다.",
    "failed": "렌더가 실패해 비교하지 못했습니다.",
}
SHA_RE = re.compile(r"[0-9a-f]{40}")
MODULE_RE = re.compile(r"(:[A-Za-z0-9_\-]+)+")
REPO_RE = re.compile(r"[A-Za-z0-9_.\-]+/[A-Za-z0-9_.\-]+")
# 경로 조각은 git pathspec 매직(`:(glob)`)·glob 문자·제어 문자가 들어갈 수 없게 좁힌다.
PART_RE = re.compile(r"[\w.\-]+")


@dataclass(frozen=True)
class Meta:
    pr: int
    head_sha: str
    base_sha: str
    status: str
    failed_modules: list = field(default_factory=list)


@dataclass(frozen=True)
class CaptureResult:
    module_dir: str
    name: str
    type: str
    image: str | None = None

    @property
    def module(self) -> str:
        return ":" + self.module_dir.replace("/", ":")

    @property
    def key(self) -> str:
        return golden_key(self.module_dir, self.name)


@dataclass
class Report:
    changed: list
    added: list
    removed: list
    failed_modules: list
    # 상한에 걸려 이미지를 빼기 전의 이미지 수. 0 이면 빠진 이미지가 없다
    images_before_cap: int = 0

    @property
    def total(self) -> int:
        return len(self.changed) + len(self.added) + len(self.removed)

    @property
    def images(self) -> list:
        return [r.image for r in self.changed + self.added if r.image is not None]


def golden_key(module_dir: str, name: str) -> str:
    return "/".join((module_dir, *OUTPUTS, name + ".png"))


def validate_meta(raw: dict) -> Meta:
    if not isinstance(raw, dict):
        raise ValueError("meta must be an object")
    pr = raw.get("pr")
    if type(pr) is not int or pr <= 0:
        raise ValueError("invalid pr")
    for name in ("head_sha", "base_sha"):
        if not isinstance(raw.get(name), str) or not SHA_RE.fullmatch(raw[name]):
            raise ValueError(f"invalid {name}")
    if raw.get("status") not in STATUSES:
        raise ValueError("invalid status")
    modules = raw.get("failed_modules", [])
    if not isinstance(modules, list) or not all(isinstance(m, str) and MODULE_RE.fullmatch(m) for m in modules):
        raise ValueError("invalid failed_modules")
    return Meta(pr, raw["head_sha"], raw["base_sha"], raw["status"], sorted(set(modules)))


def is_safe_path(rel: PurePosixPath, root: Path | None = None) -> bool:
    """`root`를 주면 경로의 모든 조각이 심볼릭 링크가 아닌지와 끝이 일반 파일인지도 본다."""
    parts = rel.parts
    if rel.is_absolute() or len(parts) < len(OUTPUTS) + 2 or tuple(parts[-4:-1]) != OUTPUTS:
        return False
    if not parts[-1].endswith(".png"):
        return False
    # `.`으로 시작하는 조각을 막아 `.git`·`.gitattributes` 같은 이름과 빈 이름(`.png`)이 이미지 브랜치에 끼지 않게 한다
    if any(p.startswith((".", "-")) or not PART_RE.fullmatch(p) for p in parts):
        return False
    if root is None:
        return True
    current = root
    for part in parts:
        current = current / part
        try:
            mode = os.lstat(current).st_mode
        except OSError:
            return False
        if stat.S_ISLNK(mode):
            return False
    return stat.S_ISREG(mode)


def is_png_file(path: Path) -> bool:
    """이미지 브랜치에 올라가는 파일이라 크기를 제한하고 PNG 시그니처로 시작하는지 본다."""
    try:
        if os.lstat(path).st_size > MAX_IMAGE_BYTES:
            return False
        with open(path, "rb") as f:
            return f.read(len(PNG_SIGNATURE)) == PNG_SIGNATURE
    except OSError:
        return False


def split_golden(rel: PurePosixPath) -> tuple[str, str] | None:
    if not is_safe_path(rel):
        return None
    return "/".join(rel.parts[:-4]), rel.name[: -len(".png")]


def classify(results: list[CaptureResult], base_names: set[str], failed_modules: set[str]) -> Report:
    head_keys = {r.key for r in results}
    changed, added = [], []
    for r in results:
        if r.module in failed_modules:
            continue
        if r.type == "changed":
            changed.append(r)
        elif r.type == "added":
            added.append(r)
    removed = []
    for name in base_names - head_keys:
        parsed = split_golden(PurePosixPath(name))
        if parsed is None:
            continue
        r = CaptureResult(module_dir=parsed[0], name=parsed[1], type="removed")
        if r.module not in failed_modules:
            removed.append(r)

    def order(r):
        return (r.module, r.name)

    return Report(sorted(changed, key=order), sorted(added, key=order), sorted(removed, key=order), sorted(failed_modules))


def cap_images(rep: Report, root: Path, max_images: int = MAX_PUSH_IMAGES, max_bytes: int = MAX_PUSH_BYTES) -> Report:
    """변경·신규 순서대로 상한까지만 이미지를 남긴다. 넘친 행은 이미지 없이 남는다."""
    count = used = 0
    before = len(rep.images)

    def keep(r):
        nonlocal count, used
        if r.image is None:
            return r
        size = os.lstat(root / r.image).st_size
        if count >= max_images or used + size > max_bytes:
            return CaptureResult(r.module_dir, r.name, r.type)
        count += 1
        used += size
        return r

    changed = [keep(r) for r in rep.changed]
    added = [keep(r) for r in rep.added]
    return Report(changed, added, rep.removed, rep.failed_modules, before if count < before else 0)


def find_result_files(root: Path):
    for dirpath, dirnames, filenames in os.walk(root, followlinks=False):
        rel = PurePosixPath(Path(dirpath).relative_to(root).as_posix())
        if len(rel.parts) > len(RESULTS) and tuple(rel.parts[-len(RESULTS):]) == RESULTS:
            module_dir = "/".join(rel.parts[: -len(RESULTS)])
            for filename in sorted(filenames):
                if filename.endswith(".json"):
                    yield module_dir, Path(dirpath) / filename


def load_capture(root: Path, module_dir: str, path: Path) -> tuple[CaptureResult, int] | None:
    try:
        st = os.lstat(path)
        if not stat.S_ISREG(st.st_mode) or st.st_size > MAX_JSON_BYTES:
            return None
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, ValueError):
        return None
    if not isinstance(data, dict) or data.get("type") not in ("changed", "added", "unchanged"):
        return None

    def outputs_rel(field_name):
        value = data.get(field_name)
        if not isinstance(value, str):
            return None
        return PurePosixPath(module_dir, *OUTPUTS, PurePosixPath(value).name)

    golden = outputs_rel("golden_file_path")
    parsed = split_golden(golden) if golden else None
    if parsed is None:
        return None
    image = None
    image_field = {"changed": "compare_file_path", "added": "actual_file_path"}.get(data["type"])
    if image_field:
        rel = outputs_rel(image_field)
        if rel is not None and is_safe_path(rel, root=root) and is_png_file(root / rel):
            image = rel.as_posix()
    timestamp = data.get("timestamp")
    order = timestamp if type(timestamp) is int else 0
    return CaptureResult(module_dir=parsed[0], name=parsed[1], type=data["type"], image=image), order


def collect_results(root: Path) -> list[CaptureResult]:
    latest = {}
    for module_dir, path in find_result_files(root):
        loaded = load_capture(root, module_dir, path)
        if loaded is None:
            continue
        capture, order = loaded
        # 같은 캡처의 JSON이 여러 실행분 남아 있으면 가장 최근 것만 쓴다.
        if capture.key not in latest or order >= latest[capture.key][1]:
            latest[capture.key] = (capture, order)
    return [capture for capture, _ in latest.values()]


def read_base_list(path: Path) -> set[str]:
    names = set()
    for line in path.read_text(encoding="utf-8").split("\n"):
        line = line.rstrip("\r")
        if line and split_golden(PurePosixPath(line)) is not None:
            names.add(line)
    return names


def report_to_dict(rep: Report) -> dict:
    return {
        "changed": [asdict(r) for r in rep.changed],
        "added": [asdict(r) for r in rep.added],
        "removed": [asdict(r) for r in rep.removed],
        "failed_modules": rep.failed_modules,
        "images_before_cap": rep.images_before_cap,
    }


def report_from_dict(raw: dict) -> Report:
    def rows(name):
        out = []
        for item in raw.get(name, []):
            r = CaptureResult(item["module_dir"], item["name"], item["type"], item.get("image"))
            if split_golden(PurePosixPath(r.key)) is None:
                raise ValueError("invalid entry")
            if r.image is not None and not is_safe_path(PurePosixPath(r.image)):
                raise ValueError("invalid entry image")
            out.append(r)
        return out

    modules = raw.get("failed_modules", [])
    if not all(isinstance(m, str) and MODULE_RE.fullmatch(m) for m in modules):
        raise ValueError("invalid failed_modules")
    before_cap = raw.get("images_before_cap", 0)
    if type(before_cap) is not int or before_cap < 0:
        raise ValueError("invalid images_before_cap")
    return Report(rows("changed"), rows("added"), rows("removed"), list(modules), before_cap)


def cell(text: str) -> str:
    return html.escape(text).replace("_", "\\_")


def preview_cell(name: str) -> str:
    if name.startswith(NAME_PREFIX):
        name = name[len(NAME_PREFIX):]
    chunks = [name[i : i + NAME_WRAP] for i in range(0, len(name), NAME_WRAP)]
    return "<br>".join(cell(chunk) for chunk in chunks)


def image_url(repo: str, image_sha: str, rel: str) -> str:
    return f"https://github.com/{repo}/blob/{image_sha}/{urllib.parse.quote(rel, safe='/')}?raw=true"


def render_comment(report: Report, meta: Meta, repo: str, image_sha: str, max_rows: int = 40) -> str:
    if not REPO_RE.fullmatch(repo):
        raise ValueError("invalid repo")
    lines = [MARKER, "## 스크린샷 비교", "", f"base `{meta.base_sha[:7]}` → head `{meta.head_sha[:7]}`", ""]
    if meta.status != "ok":
        lines.append(STATUS_MESSAGES[meta.status])
        return "\n".join(lines) + "\n"
    if report.failed_modules:
        modules = ", ".join(f"`{m}`" for m in report.failed_modules)
        lines += [f"렌더에 실패해 비교에서 뺀 모듈: {modules}", ""]
    if report.total == 0:
        lines.append("바뀐 화면 없음")
        return "\n".join(lines) + "\n"
    if report.images and not SHA_RE.fullmatch(image_sha):
        raise ValueError("invalid image sha")

    lines += [f"변경 {len(report.changed)} · 신규 {len(report.added)} · 삭제 {len(report.removed)}", ""]
    if report.images_before_cap:
        lines += [f"이미지가 많아 {report.images_before_cap}개 중 {len(report.images)}개만 올렸습니다.", ""]
    budget = max_rows
    for title, header, rows, width in (
        ("변경", "| 모듈 | 프리뷰 | Before / Diff / After |", report.changed, 600),
        ("신규", "| 모듈 | 프리뷰 | After |", report.added, 200),
    ):
        shown = rows[:budget]
        budget -= len(shown)
        if not shown:
            continue
        lines += [f"### {title}", "", header, "|---|---|---|"]
        for r in shown:
            img = "이미지 없음" if r.image is None else f'<img src="{image_url(repo, image_sha, r.image)}" width="{width}">'
            lines.append(f"| {cell(r.module)} | {preview_cell(r.name)} | {img} |")
        lines.append("")
    removed = report.removed[:budget]
    if removed:
        lines += ["### 삭제", ""]
        lines += [f"- {cell(r.module)} {preview_cell(r.name)}" for r in removed]
        lines.append("")
    shown_total = max_rows - budget + len(removed)
    if shown_total < report.total:
        # 이미지 링크와 같은 커밋을 가리켜야 브랜치가 다시 push 돼도 이 코멘트의 이미지와 어긋나지 않는다
        ref = image_sha if report.images and SHA_RE.fullmatch(image_sha) else f"ui-snapshots/pr-{meta.pr}"
        tree = f"https://github.com/{repo}/tree/{ref}"
        lines.append(f"{report.total}개 중 {shown_total}개만 보여 줍니다. 전체 이미지는 [ui-snapshots/pr-{meta.pr}]({tree})에 있습니다.")
    return "\n".join(lines).rstrip("\n") + "\n"


def load_meta(path: str) -> Meta:
    return validate_meta(json.loads(Path(path).read_text(encoding="utf-8")))


def cmd_collect(args) -> int:
    meta = load_meta(args.meta)
    if meta.status == "ok":
        root = Path(args.results_root)
        rep = classify(collect_results(root), read_base_list(Path(args.base_list)), set(meta.failed_modules))
        rep = cap_images(rep, root)
    else:
        rep = Report([], [], [], list(meta.failed_modules))
    Path(args.out_entries).write_text(json.dumps(report_to_dict(rep), ensure_ascii=False, indent=2), encoding="utf-8")
    Path(args.out_files).write_bytes(b"".join(f.encode("utf-8") + b"\0" for f in rep.images))
    with open(args.github_output, "a", encoding="utf-8") as out:
        out.write(f"total={rep.total}\n")
    return 0


def cmd_render(args) -> int:
    meta = load_meta(args.meta)
    rep = report_from_dict(json.loads(Path(args.entries).read_text(encoding="utf-8")))
    Path(args.out).write_text(render_comment(rep, meta, args.repo, args.image_sha), encoding="utf-8")
    return 0


def cmd_meta(args) -> int:
    """검증한 메타 값만 스텝 출력으로 넘긴다. head_sha가 워크플로 런의 head와 다르면 거부한다."""
    meta = load_meta(args.meta)
    if meta.head_sha != args.expected_head:
        raise ValueError("meta head_sha does not match the workflow run")
    with open(args.github_output, "a", encoding="utf-8") as out:
        out.write(f"pr={meta.pr}\nhead_sha={meta.head_sha}\nstatus={meta.status}\nfailed_modules={len(meta.failed_modules)}\n")
    return 0


def cmd_check_head(args) -> int:
    return 0 if load_meta(args.meta).head_sha == args.current_head else 3


def main(argv=None) -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    collect = sub.add_parser("collect")
    for flag in ("--results-root", "--base-list", "--meta", "--out-entries", "--out-files", "--github-output"):
        collect.add_argument(flag, required=True)
    render = sub.add_parser("render")
    for flag in ("--entries", "--meta", "--repo", "--out"):
        render.add_argument(flag, required=True)
    render.add_argument("--image-sha", default="")
    meta = sub.add_parser("meta")
    for flag in ("--meta", "--expected-head", "--github-output"):
        meta.add_argument(flag, required=True)
    check = sub.add_parser("check-head")
    check.add_argument("--meta", required=True)
    check.add_argument("--current-head", required=True)
    args = parser.parse_args(argv)
    handler = {"collect": cmd_collect, "render": cmd_render, "meta": cmd_meta, "check-head": cmd_check_head}[args.command]
    try:
        return handler(args)
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"ui-diff report: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
