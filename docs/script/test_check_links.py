#!/usr/bin/env python3
"""check_links 단위 테스트.

용법:
    python3 -m unittest discover -s parfait/script -p 'test_*.py'
"""
import tempfile
import unittest
from pathlib import Path

import check_links


class BlankOutTest(unittest.TestCase):
    def test_fence_keeps_line_count(self):
        text = "a\n```\nlink](x.md)\n```\nb"
        self.assertEqual(len(check_links.blank_out(text).splitlines()), len(text.splitlines()))

    def test_inline_code_removed(self):
        self.assertEqual(check_links.blank_out("see `](x.md)` here"), "see  here")


class IsCheckableTest(unittest.TestCase):
    def test_skips_external_and_anchors(self):
        for target in ("https://a.com", "mailto:a@b.c", "#절", "/abs/path", "", "<name>.md"):
            self.assertFalse(check_links.is_checkable(target), target)

    def test_skips_wikilink_target(self):
        self.assertFalse(check_links.is_checkable("[[토핑]]"))

    def test_accepts_relative(self):
        for target in ("x.md", "../adr/0001.md", "./y.md"):
            self.assertTrue(check_links.is_checkable(target), target)


class BrokenLinksTest(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name)
        self.addCleanup(self._tmp.cleanup)

    def write(self, rel: str, body: str) -> Path:
        path = self.root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body, encoding="utf-8")
        return path

    def test_resolves_existing_relative_link(self):
        self.write("adr/0001.md", "# adr")
        page = self.write("specs/a.md", "[결정](../adr/0001.md)")
        self.assertEqual(check_links.broken_links(page), [])

    def test_reports_wrong_depth(self):
        self.write("adr/0001.md", "# adr")
        page = self.write("specs/archive/a.md", "[결정](../adr/0001.md)")
        found = check_links.broken_links(page)
        self.assertEqual(len(found), 1)
        self.assertEqual(found[0][1], "../adr/0001.md")

    def test_strips_anchor_and_title(self):
        self.write("adr/0001.md", "# adr")
        page = self.write("specs/a.md", '[결정](../adr/0001.md#결정 "제목")')
        self.assertEqual(check_links.broken_links(page), [])

    def test_ignores_link_inside_fence(self):
        page = self.write("specs/a.md", "```\n[x](../없는/파일.md)\n```")
        self.assertEqual(check_links.broken_links(page), [])

    def test_line_number_survives_fence(self):
        page = self.write("specs/a.md", "머리말\n```\n코드\n```\n[x](없다.md)")
        found = check_links.broken_links(page)
        self.assertEqual(found[0][0], 5)

    def test_ignores_paren_after_wikilink(self):
        page = self.write("specs/a.md", "위키 [[정책-v0.2]](별×2·음표×2)와 일치")
        self.assertEqual(check_links.broken_links(page), [])

    def test_directory_link_counts_as_resolved(self):
        self.write("adr/0001.md", "# adr")
        page = self.write("specs/a.md", "[디렉토리](../adr/)")
        self.assertEqual(check_links.broken_links(page), [])


if __name__ == "__main__":
    unittest.main()
