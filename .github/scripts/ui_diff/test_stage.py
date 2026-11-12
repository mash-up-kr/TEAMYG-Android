import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import report  # noqa: E402
import stage  # noqa: E402

HEAD = "1" * 40
BASE = "a" * 40
OUT = "build/outputs/roborazzi"
RES = "build/test-results/roborazzi/debug/results"



def plan(task: str, modules=(":core:ui", ":feature:groups:list:impl")) -> str:
    lines = [":core:model:compileKotlin SKIPPED"]
    for m in modules:
        lines += [f"{m}:testDebugUnitTest SKIPPED", f"{m}:finalizeTestRoborazziDebug SKIPPED", f"{m}:{task}RoborazziDebug SKIPPED"]
    return "\n".join(lines) + "\n"


def touch(path: Path, content: str = "x") -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)


class StageTest(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.tmp = Path(self._tmp.name)
        self.repo = self.tmp / "repo"
        self.repo.mkdir()

    def tearDown(self):
        self._tmp.cleanup()

    def write(self, name: str, content: str) -> str:
        path = self.tmp / name
        path.write_text(content)
        return str(path)

    def test_stash_and_restore_keep_module_paths(self):
        touch(self.repo / "core/ui" / OUT / "A.png")
        touch(self.repo / "feature/groups/list/impl" / OUT / "B.png")
        touch(self.repo / "feature/groups/list/impl" / OUT / "notes.txt")
        touch(self.repo / "build" / "outputs/roborazzi/root.png")
        touch(self.repo / ".gradle/x" / OUT / "C.png")
        dest, listing = self.tmp / "base", self.tmp / "base-list.txt"
        stage.main(["stash", "--root", str(self.repo), "--dest", str(dest), "--list", str(listing)])

        expected = [f"core/ui/{OUT}/A.png", f"feature/groups/list/impl/{OUT}/B.png"]
        self.assertEqual(listing.read_text().splitlines(), expected)
        self.assertEqual(report.read_base_list(listing), set(expected))

        fresh = self.tmp / "fresh"
        fresh.mkdir()
        stage.main(["restore", "--src", str(dest), "--root", str(fresh)])
        for rel in expected:
            self.assertTrue((fresh / rel).is_file(), rel)

    def test_stage_results_mirror_repo_layout(self):
        touch(self.repo / "core/ui" / OUT / "A_compare.png")
        touch(self.repo / "core/ui" / RES / "r.json", "{}")
        touch(self.repo / "core/ui/build/reports/x.html")
        dest = self.tmp / "results"
        stage.main(["stage-results", "--root", str(self.repo), "--dest", str(dest)])
        staged = sorted(p.relative_to(dest).as_posix() for p in dest.rglob("*") if p.is_file())
        self.assertEqual(staged, [f"core/ui/{OUT}/A_compare.png", f"core/ui/{RES}/r.json"])

    def meta(self, *extra) -> dict:
        out = self.tmp / "meta" / "meta.json"
        code = stage.main(["meta", "--out", str(out), "--pr", "7", "--head-sha", HEAD, "--base-sha", BASE, *extra])
        self.assertEqual(code, 0)
        raw = json.loads(out.read_text())
        report.validate_meta(raw)
        return raw

    def compare_args(self, base_log: str, head_log: str, with_results: bool = True, base_modules=None, head_modules=None) -> list:
        results = self.tmp / "results"
        if with_results:
            touch(results / "core/ui" / RES / "r.json", "{}")
        base_plan = plan("record") if base_modules is None else plan("record", base_modules)
        head_plan = plan("compare") if head_modules is None else plan("compare", head_modules)
        return [
            "--base-plan", self.write("base-plan.txt", base_plan),
            "--head-plan", self.write("head-plan.txt", head_plan),
            "--base-log", self.write("base.log", base_log),
            "--head-log", self.write("head.log", head_log),
            "--results-root", str(results),
        ]

    def test_preset_status_skips_comparison(self):
        raw = self.meta("--status", "merge-conflict")
        self.assertEqual((raw["status"], raw["failed_modules"]), ("merge-conflict", []))

    def test_all_modules_pass(self):
        log = "> Task :core:ui:testDebugUnitTest\n> Task :feature:groups:list:impl:testDebugUnitTest FROM-CACHE\n"
        raw = self.meta(*self.compare_args(log, log))
        self.assertEqual((raw["status"], raw["failed_modules"]), ("ok", []))

    def test_failed_and_never_run_modules_are_reported(self):
        base_log = "> Task :core:ui:testDebugUnitTest\n> Task :feature:groups:list:impl:testDebugUnitTest FAILED\n"
        # head 에서 core:ui 는 컴파일 실패로 테스트 태스크가 아예 돌지 않았다
        head_log = "> Task :core:ui:compileDebugKotlin FAILED\n> Task :feature:groups:list:impl:testDebugUnitTest\n"
        raw = self.meta(*self.compare_args(base_log, head_log))
        self.assertEqual(raw["status"], "failed")
        self.assertEqual(raw["failed_modules"], [":core:ui", ":feature:groups:list:impl"])

    def test_head_only_module_that_never_ran_is_failed(self):
        # base 에 없던 모듈이 head 에서 컴파일 실패로 테스트를 못 돌았다
        base_log = "> Task :core:ui:testDebugUnitTest\n"
        head_log = "> Task :core:ui:testDebugUnitTest\n> Task :feature:new:impl:compileDebugKotlin FAILED\n"
        raw = self.meta(*self.compare_args(base_log, head_log, base_modules=(":core:ui",), head_modules=(":core:ui", ":feature:new:impl")))
        self.assertEqual((raw["status"], raw["failed_modules"]), ("ok", [":feature:new:impl"]))

    def test_every_head_module_failing_means_failed(self):
        base_log = "> Task :core:ui:testDebugUnitTest\n"
        head_log = "> Task :core:ui:compileDebugKotlin FAILED\n"
        raw = self.meta(*self.compare_args(base_log, head_log, base_modules=(), head_modules=(":core:ui",)))
        self.assertEqual((raw["status"], raw["failed_modules"]), ("failed", [":core:ui"]))

    def test_partial_failure_stays_ok(self):
        log = "> Task :core:ui:testDebugUnitTest\n> Task :feature:groups:list:impl:testDebugUnitTest FAILED\n"
        raw = self.meta(*self.compare_args(log, log))
        self.assertEqual((raw["status"], raw["failed_modules"]), ("ok", [":feature:groups:list:impl"]))

    def test_no_results_means_failed(self):
        log = "> Task :core:ui:testDebugUnitTest\n> Task :feature:groups:list:impl:testDebugUnitTest\n"
        raw = self.meta(*self.compare_args(log, log, with_results=False))
        self.assertEqual(raw["status"], "failed")

    def test_invalid_inputs_are_rejected(self):
        out = str(self.tmp / "m.json")
        self.assertEqual(stage.main(["meta", "--out", out, "--pr", "x", "--head-sha", HEAD, "--base-sha", BASE]), 2)
        self.assertEqual(stage.main(["meta", "--out", out, "--pr", "7", "--head-sha", "abc", "--base-sha", BASE]), 2)



class ContractTest(unittest.TestCase):
    def test_shared_constants_match_report(self):
        self.assertEqual(stage.OUTPUTS, report.OUTPUTS)
        self.assertEqual(stage.RESULTS, report.RESULTS)
        self.assertEqual(stage.STATUSES, report.STATUSES)


if __name__ == "__main__":
    unittest.main()
