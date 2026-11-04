import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path, PurePosixPath

sys.path.insert(0, str(Path(__file__).resolve().parent))

import report  # noqa: E402
from report import CaptureResult, Meta, classify, is_safe_path, render_comment, validate_meta  # noqa: E402

SCRIPT = Path(__file__).resolve().parent / "report.py"
HEAD = "1" * 40
BASE = "a" * 40
IMAGE_SHA = "b" * 40
OUT = "build/outputs/roborazzi"


def meta(**overrides):
    raw = {"pr": 7, "head_sha": HEAD, "base_sha": BASE, "status": "ok", "failed_modules": []}
    raw.update(overrides)
    return raw


def key(module_dir, name):
    return f"{module_dir}/{OUT}/{name}.png"


def result(module_dir, name, type_, image=None):
    if image is None and type_ in ("changed", "added"):
        suffix = "_compare" if type_ == "changed" else "_actual"
        image = f"{module_dir}/{OUT}/{name}{suffix}.png"
    return CaptureResult(module_dir=module_dir, name=name, type=type_, image=image)


class MetaTest(unittest.TestCase):
    def test_meta_validation(self):
        self.assertIsInstance(validate_meta(meta()), Meta)
        for bad in (
            meta(pr="12; rm"),
            meta(head_sha="1" * 39),
            meta(status="unknown"),
            meta(failed_modules=[":a\n:b"]),
        ):
            with self.assertRaises(ValueError):
                validate_meta(bad)


class SafePathTest(unittest.TestCase):
    def test_unsafe_paths_are_dropped(self):
        self.assertTrue(is_safe_path(PurePosixPath(f"feature/x/{OUT}/a.png")))
        for rel in (
            f"../x/{OUT}/a.png",
            "feature/x/build/outputs/other/a.png",
            f"feature/x/{OUT}/a\nb.png",
            f"feature/x/{OUT}/:(glob)*.png",
        ):
            self.assertFalse(is_safe_path(PurePosixPath(rel)), rel)

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / "secret.png"
            target.write_bytes(b"x")
            link_dir = root / "feature/x" / OUT
            link_dir.mkdir(parents=True)
            (link_dir / "a.png").symlink_to(target)
            (link_dir / "b.png").write_bytes(b"x")
            self.assertFalse(is_safe_path(PurePosixPath(f"feature/x/{OUT}/a.png"), root=root))
            self.assertTrue(is_safe_path(PurePosixPath(f"feature/x/{OUT}/b.png"), root=root))


class ClassifyTest(unittest.TestCase):
    def test_classify_changed_added_removed(self):
        m = "feature/x"
        results = [result(m, "A", "changed"), result(m, "B", "added"), result(m, "C", "unchanged")]
        rep = classify(results, {key(m, "A"), key(m, "C"), key(m, "D")}, set())
        self.assertEqual([r.name for r in rep.changed], ["A"])
        self.assertEqual([r.name for r in rep.added], ["B"])
        self.assertEqual([r.name for r in rep.removed], ["D"])

    def test_failed_module_rows_excluded(self):
        results = [
            result("feature/x", "B", "added"),
            result("feature/y", "E", "added"),
        ]
        base = {key("feature/x", "D"), key("feature/y", "F")}
        rep = classify(results, base, {":feature:x"})
        self.assertEqual([r.module for r in rep.added], [":feature:y"])
        self.assertEqual([r.module for r in rep.removed], [":feature:y"])
        self.assertEqual(rep.failed_modules, [":feature:x"])

    def test_rows_sorted(self):
        results = [
            result("feature/b", "Z", "changed"),
            result("feature/a", "Y", "changed"),
            result("feature/a", "X", "added"),
            result("feature/a", "W", "changed"),
        ]
        first = render_comment(classify(results, set(), set()), validate_meta(meta()), "o/r", IMAGE_SHA)
        second = render_comment(
            classify(list(reversed(results)), set(), set()), validate_meta(meta()), "o/r", IMAGE_SHA
        )
        self.assertEqual(first, second)
        rep = classify(results, set(), set())
        self.assertEqual([(r.module, r.name) for r in rep.changed], [(":feature:a", "W"), (":feature:a", "Y"), (":feature:b", "Z")])


class RenderTest(unittest.TestCase):
    def test_render_uses_image_sha_urls(self):
        m = "feature/한글"
        rep = classify([result(m, "A", "changed")], set(), set())
        body = render_comment(rep, validate_meta(meta()), "o/r", IMAGE_SHA)
        encoded = "feature/%ED%95%9C%EA%B8%80/build/outputs/roborazzi/A_compare.png"
        self.assertIn(f"https://github.com/o/r/blob/{IMAGE_SHA}/{encoded}?raw=true", body)
        self.assertNotIn("한글/build", body)
        self.assertNotIn("ui-snapshots", body)

    def test_render_header_has_marker_and_shas(self):
        rep = classify([result("feature/x", "A", "changed")], set(), set())
        body = render_comment(rep, validate_meta(meta()), "o/r", IMAGE_SHA)
        self.assertEqual(body.splitlines()[0], "<!-- parfait-ui-diff-report -->")
        self.assertIn(BASE[:7], body)
        self.assertIn(HEAD[:7], body)
        self.assertIn("base", body)
        self.assertIn("head", body)

    def test_render_row_cap(self):
        results = [result("feature/x", f"P{i:02d}", "changed") for i in range(45)]
        body = render_comment(classify(results, set(), set()), validate_meta(meta()), "o/r", IMAGE_SHA, max_rows=40)
        rows = [line for line in body.splitlines() if line.startswith("| ") and "_compare.png" in line]
        self.assertEqual(len(rows), 40)
        self.assertIn("45개 중 40개", body)
        self.assertIn("https://github.com/o/r/tree/ui-snapshots/pr-7", body)

    def test_render_zero(self):
        body = render_comment(classify([], set(), set()), validate_meta(meta()), "o/r", IMAGE_SHA)
        self.assertIn("바뀐 화면 없음", body)
        with tempfile.TemporaryDirectory() as tmp:
            out = run_collect(Path(tmp), meta(), [])
            self.assertEqual(out["github_output"], "total=0\n")
            self.assertEqual(out["files"], b"")

    def test_render_status_messages(self):
        expected = {
            "no-base-setup": "base에 스크린샷 구성이 없어 비교하지 못했습니다",
            "merge-conflict": "충돌로 merge ref가 없어 비교하지 못했습니다",
            "failed": "렌더가 실패해 비교하지 못했습니다",
        }
        for status, message in expected.items():
            body = render_comment(classify([], set(), set()), validate_meta(meta(status=status)), "o/r", IMAGE_SHA)
            self.assertIn(message, body)
            self.assertEqual(body.splitlines()[0], "<!-- parfait-ui-diff-report -->")


class CliTest(unittest.TestCase):
    def test_stale_head_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            meta_file = Path(tmp) / "meta.json"
            meta_file.write_text(json.dumps(meta()))
            base_cmd = [sys.executable, str(SCRIPT), "check-head", "--meta", str(meta_file), "--current-head"]
            self.assertEqual(subprocess.run(base_cmd + ["2" * 40]).returncode, 3)
            self.assertEqual(subprocess.run(base_cmd + [HEAD]).returncode, 0)

    def test_meta_outputs_validated_fields(self):
        with tempfile.TemporaryDirectory() as tmp:
            meta_file = Path(tmp) / "meta.json"
            gh = Path(tmp) / "gh"
            meta_file.write_text(json.dumps(meta(status="failed", failed_modules=[":a", ":b"])))
            args = ["meta", "--meta", str(meta_file), "--github-output", str(gh), "--expected-head"]
            self.assertEqual(report.main(args + [HEAD]), 0)
            self.assertEqual(gh.read_text(), f"pr=7\nhead_sha={HEAD}\nstatus=failed\nfailed_modules=2\n")

    def test_meta_rejects_head_not_from_workflow_run(self):
        with tempfile.TemporaryDirectory() as tmp:
            meta_file = Path(tmp) / "meta.json"
            gh = Path(tmp) / "gh"
            meta_file.write_text(json.dumps(meta()))
            args = ["meta", "--meta", str(meta_file), "--github-output", str(gh), "--expected-head", "2" * 40]
            self.assertEqual(report.main(args), 2)
            self.assertFalse(gh.exists())

    def test_meta_rejects_invalid_meta(self):
        with tempfile.TemporaryDirectory() as tmp:
            meta_file = Path(tmp) / "meta.json"
            gh = Path(tmp) / "gh"
            meta_file.write_text(json.dumps(meta(pr="7\nstatus=ok")))
            args = ["meta", "--meta", str(meta_file), "--github-output", str(gh), "--expected-head", HEAD]
            self.assertEqual(report.main(args), 2)
            self.assertFalse(gh.exists())

    def test_collect_reads_artifact(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "results"
            module = "feature/groups/list/impl"
            out_dir = root / module / OUT
            res_dir = root / module / "build/test-results/roborazzi/debug/results"
            out_dir.mkdir(parents=True)
            res_dir.mkdir(parents=True)
            runner = "/home/runner/work/x/x/" + module + "/" + OUT
            for name, type_ in (("A", "changed"), ("B", "added"), ("C", "unchanged")):
                entry = {"type": type_, "golden_file_path": f"{runner}/{name}.png", "timestamp": 1}
                if type_ != "unchanged":
                    entry["compare_file_path"] = f"{runner}/{name}_compare.png"
                    entry["actual_file_path"] = f"{runner}/{name}_actual.png"
                (res_dir / f"1_{name}.json").write_text(json.dumps(entry))
            (res_dir / "2_broken.json").write_text("{not json")
            (out_dir / "A_compare.png").write_bytes(b"x")
            (out_dir / "B_actual.png").write_bytes(b"x")
            base_lines = [key(module, "A"), key(module, "C"), key(module, "D")]
            out = run_collect(root, meta(), base_lines)
            entries = json.loads(out["entries"])
            self.assertEqual([e["name"] for e in entries["changed"]], ["A"])
            self.assertEqual([e["name"] for e in entries["added"]], ["B"])
            self.assertEqual([e["name"] for e in entries["removed"]], ["D"])
            self.assertEqual(out["github_output"], "total=3\n")
            self.assertEqual(
                out["files"].split(b"\0")[:-1],
                [f"{module}/{OUT}/A_compare.png".encode(), f"{module}/{OUT}/B_actual.png".encode()],
            )


def run_collect(root, raw_meta, base_lines):
    work = Path(tempfile.mkdtemp())
    root.mkdir(parents=True, exist_ok=True)
    (work / "meta.json").write_text(json.dumps(raw_meta))
    (work / "base.txt").write_text("".join(line + "\n" for line in base_lines))
    code = report.main([
        "collect",
        "--results-root", str(root),
        "--base-list", str(work / "base.txt"),
        "--meta", str(work / "meta.json"),
        "--out-entries", str(work / "entries.json"),
        "--out-files", str(work / "files"),
        "--github-output", str(work / "gh"),
    ])
    assert code == 0, code
    return {
        "entries": (work / "entries.json").read_text(),
        "files": (work / "files").read_bytes(),
        "github_output": (work / "gh").read_text(),
    }


if __name__ == "__main__":
    unittest.main()
