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

    def test_field_cap_stops_repetition_beating_specific_match(self):
        # wide 는 헤딩 하나에서만 토큰이 잔뜩(50회) 반복되는 메타 문서
        # (헤딩 408개짜리 open-questions.md 흉내)고, narrow 는 id·title·tags·
        # code·headings 다섯 필드 전부에서 한 번씩만 정확히 맞는 문서다.
        # 상한이 없으면 wide 의 headings 기여만 1*50=50 으로 narrow 총점
        # (id4+title4+tags2+code2+head1+substring보너스1=14)을 가볍게 이긴다.
        # 캡을 걸면 wide 의 headings 기여가 1*3=3 으로 눌려 narrow 가 이긴다.
        wide = {
            "id": "open-questions",
            "title": "열린 질문",
            "tags": "",
            "code": "",
            "headings": " ".join(["토핑"] * 50),
        }
        narrow = {
            "id": "토핑-border-distance-field",
            "title": "토핑 border 거리 필드",
            "tags": "토핑",
            "code": "토핑모듈",
            "headings": "토핑",
        }
        uncapped_wide = 1 * 50
        uncapped_narrow = 4 + 4 + 2 + 2 + 1 + 1
        assert uncapped_wide > uncapped_narrow, "상한 없이는 wide 가 이겨야 회귀 테스트가 의미 있다"
        self.assertGreater(search.score("토핑", narrow), search.score("토핑", wide))


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

    def test_field_reads_whole_block_list(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "x.md"
            p.write_text(
                "---\nid: x\ntitle: 제목\nrelated_code:\n"
                "  - settings.gradle.kts\n  - TestConfig.kt#setConfigTestUnit\n---\n# 본문\n",
                encoding="utf-8",
            )
            doc = search.parse_doc(p, archived=False)
            self.assertIn("settings.gradle.kts", doc["code"])
            self.assertIn("TestConfig.kt#setConfigTestUnit", doc["code"])

    def test_empty_field_does_not_swallow_the_next_one(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "y.md"
            p.write_text(
                "---\nid: y\ntitle: 제목\nrelated_adr:\nrelated_spec: login-debug-mode\n---\n# 본문\n",
                encoding="utf-8",
            )
            doc = search.parse_doc(p, archived=False)
            self.assertNotIn("related_spec", doc["code"])
            self.assertIn("login-debug-mode", doc["code"])

    def test_marks_archived(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "archive" / "old.md"
            _mk(p, id="old", title="옛 스펙")
            self.assertTrue(search.parse_doc(p, archived=True)["archived"])


class SearchTest(unittest.TestCase):
    def setUp(self):
        # 모듈 전역을 건드리므로 반드시 되돌린다 — 이 클래스가 마지막으로
        # 실행되는 것에 기대면 테스트 파일이 하나 더 생기는 순간 깨진다.
        saved = search.DOC_ROOTS
        self.addCleanup(lambda: setattr(search, "DOC_ROOTS", saved))

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
