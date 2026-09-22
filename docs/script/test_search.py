import tempfile
import unittest
from pathlib import Path

import search


def _mk(path, **fm):
    path.parent.mkdir(parents=True, exist_ok=True)
    body = "\n".join(f"{k}: {v}" for k, v in fm.items())
    path.write_text(f"---\n{body}\n---\n# 본문\n", encoding="utf-8")


class ScoreTest(unittest.TestCase):
    def test_title_weight_beats_tags(self):
        by_title = {"id": "x", "title": "토핑 border 렌더링", "tags": "", "code": "", "headings": ""}
        by_tags = {"id": "y", "title": "무관", "tags": "border", "code": "", "headings": ""}
        self.assertGreater(search.score("border", by_title), search.score("border", by_tags))

    def test_no_match_zero(self):
        doc = {"id": "a", "title": "b", "tags": "", "code": "", "headings": ""}
        self.assertEqual(search.score("xyz", doc), 0.0)

    def test_related_code_is_searchable(self):
        doc = {"id": "a", "title": "b", "tags": "", "code": "LoginViewModel", "headings": ""}
        self.assertGreater(search.score("LoginViewModel", doc), 0.0)


class CollectTest(unittest.TestCase):
    def test_reads_frontmatter_id_and_title(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "2026-08-28-login-debug-mode.md"
            _mk(p, id="login-debug-mode", title="로그인 디버그 모드", tags="[plan, login]")
            doc = search.parse_doc(p, archived=False)
            self.assertEqual(doc["id"], "login-debug-mode")
            self.assertEqual(doc["title"], "로그인 디버그 모드")

    def test_falls_back_to_filename_without_frontmatter(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "README.md"
            p.write_text("# 안내\n본문\n", encoding="utf-8")
            doc = search.parse_doc(p, archived=False)
            self.assertEqual(doc["id"], "README")
            self.assertEqual(doc["title"], "")

    def test_marks_archived(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "archive" / "old.md"
            _mk(p, id="old", title="옛 스펙")
            self.assertTrue(search.parse_doc(p, archived=True)["archived"])


class SearchTest(unittest.TestCase):
    def test_ranks_relevant_first(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t)
            _mk(root / "specs" / "a.md", id="topping-border", title="토핑 border 렌더링")
            _mk(root / "specs" / "b.md", id="login-debug", title="로그인 디버그 모드")
            search.DOC_ROOTS = [root / "specs"]
            res = search.search("토핑 border", top=5)
            self.assertEqual(res[0][1]["id"], "topping-border")

    def test_includes_archive(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t)
            _mk(root / "specs" / "archive" / "old.md", id="old-spec", title="옛 토핑 스펙")
            search.DOC_ROOTS = [root / "specs"]
            res = search.search("토핑", top=5)
            self.assertEqual(len(res), 1)
            self.assertTrue(res[0][1]["archived"])


if __name__ == "__main__":
    unittest.main()
