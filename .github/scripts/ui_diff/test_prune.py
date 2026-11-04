import sys
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from prune import PrState, select_expired  # noqa: E402

NOW = datetime(2026, 11, 4, tzinfo=timezone.utc)


def closed(days, extra=timedelta()):
    return PrState("closed", NOW - timedelta(days=days) - extra)


class SelectExpiredTest(unittest.TestCase):
    def test_select_expired(self):
        branches = [
            "ui-snapshots/pr-1",
            "ui-snapshots/pr-2",
            "ui-snapshots/pr-3",
            "ui-snapshots/pr-4",
            "ui-snapshots/foo",
            "feature/x",
        ]
        prs = {
            1: closed(31),
            2: closed(29),
            3: PrState("open", None),
        }
        self.assertEqual(select_expired(branches, prs, NOW), ["ui-snapshots/pr-1"])

    def test_exactly_keep_days_is_kept(self):
        prs = {1: closed(30), 2: closed(30, timedelta(seconds=1))}
        self.assertEqual(
            select_expired(["ui-snapshots/pr-1", "ui-snapshots/pr-2"], prs, NOW),
            ["ui-snapshots/pr-2"],
        )

    def test_only_exact_pattern_is_candidate(self):
        names = [
            "ui-snapshots/pr-0",
            "ui-snapshots/pr-01",
            "ui-snapshots/pr-1/x",
            "ui-snapshots/pr-1\n",
            "xui-snapshots/pr-1",
            "ui-snapshots/pr-٣",
        ]
        prs = {n: closed(99) for n in range(0, 10)}
        self.assertEqual(select_expired(names, prs, NOW), [])

    def test_closed_without_date_is_kept(self):
        self.assertEqual(select_expired(["ui-snapshots/pr-1"], {1: PrState("closed", None)}, NOW), [])

    def test_custom_keep_days(self):
        self.assertEqual(
            select_expired(["ui-snapshots/pr-1"], {1: closed(8)}, NOW, keep_days=7),
            ["ui-snapshots/pr-1"],
        )


if __name__ == "__main__":
    unittest.main()
