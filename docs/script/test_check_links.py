#!/usr/bin/env python3
"""check_links 단위 테스트.

용법:
    python3 -m unittest discover -s docs/script -p 'test_*.py'
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

    def test_skips_superpowers_scratch(self):
        control = self.write("specs/a.md", "ok")
        scratch = self.write(".superpowers/sdd/task-1-report.md", "[x](../없는/파일.md)")
        found = list(check_links.iter_markdown([self.root]))
        self.assertIn(control, found)
        self.assertNotIn(scratch, found)

    def test_stray_unclosed_fence_does_not_hide_following_link(self):
        body = (
            "이 문서는 마크다운 문법을 설명한다. 코드 펜스는 ```으로 연다.\n\n"
            "[진짜 깨진 링크](../없는/파일.md)\n\n"
            "```python\nprint('x')\n```\n"
        )
        page = self.write("specs/a.md", body)
        found = check_links.broken_links(page)
        self.assertEqual(len(found), 1)
        self.assertEqual(found[0][1], "../없는/파일.md")

    def test_closed_fence_still_hides_its_example_link(self):
        page = self.write("specs/a.md", "```\n[x](../없는/파일.md)\n```")
        self.assertEqual(check_links.broken_links(page), [])

    def test_tilde_fence_hides_example_link(self):
        page = self.write("specs/a.md", "~~~\n[x](../없는/파일.md)\n~~~")
        self.assertEqual(check_links.broken_links(page), [])

    def test_four_backtick_fence_contains_three_backtick_block(self):
        body = "````\n```\n[x](../없는/파일.md)\n```\n````"
        page = self.write("specs/a.md", body)
        self.assertEqual(check_links.broken_links(page), [])

    def test_line_number_after_fence_still_correct(self):
        page = self.write("specs/a.md", "머리말\n```\n코드\n```\n[x](없다.md)")
        found = check_links.broken_links(page)
        self.assertEqual(found[0][0], 5)

    def test_broken_html_img_src_is_reported(self):
        page = self.write("specs/a.md", '<img src="../없는/그림.png">')
        found = check_links.broken_links(page)
        self.assertEqual(len(found), 1)
        self.assertEqual(found[0][1], "../없는/그림.png")

    def test_valid_html_img_src_is_not_reported(self):
        self.write("그림.png", "")
        page = self.write("specs/a.md", '<img src="../그림.png">')
        self.assertEqual(check_links.broken_links(page), [])


if __name__ == "__main__":
    unittest.main()
