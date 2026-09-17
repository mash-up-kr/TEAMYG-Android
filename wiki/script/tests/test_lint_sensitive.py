from conftest import page

import lint
import wikilib


def _codes_levels(found):
    return {(f.code, f.level) for f in found}


def test_api_key_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/concepts/x.md": page(
            sources="[]", body='api_key: "sk-abcdefghijklmnop1234"'),
    })
    found = lint.check_sensitive(wikilib.load_pages(root))
    assert ("민감/credential", "violation") in _codes_levels(found)


def test_jwt_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/concepts/x.md": page(
            sources="[]", body="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0"),
    })
    found = lint.check_sensitive(wikilib.load_pages(root))
    assert ("민감/jwt", "violation") in _codes_levels(found)


def test_email_is_warning_not_violation(make_repo):
    root = make_repo({
        "wiki/pages/concepts/x.md": page(
            sources="[]", body="연락처는 someone@example.com 이다"),
    })
    found = lint.check_sensitive(wikilib.load_pages(root))
    assert ("민감/email", "warning") in _codes_levels(found)
    assert all(f.level == "warning" for f in found)


def test_phone_and_abs_path_are_warnings(make_repo):
    root = make_repo({
        "wiki/pages/concepts/x.md": page(
            sources="[]", body="010-1234-5678 / /Users/someone/notes"),
    })
    found = lint.check_sensitive(wikilib.load_pages(root))
    assert all(f.level == "warning" for f in found)
    assert {"민감/phone", "민감/abs_path"} <= {f.code for f in found}


def test_clean_page_has_no_findings(make_repo):
    root = make_repo({"wiki/pages/concepts/x.md": page(sources="[]", body="평범한 본문")})
    assert lint.check_sensitive(wikilib.load_pages(root)) == []


def test_action_enum_accepts_allowed_values(make_repo):
    body = "\n".join([
        "### [2026-08-10] 주제",
        "- **상태**: 미해결",
        "- **action**: research",
        "",
        "### [2026-08-09] 다른 주제",
        "- **상태**: 해소됨",
        "- **action**: skip",
    ])
    root = make_repo({"wiki/open-questions.md": page(body=body)})
    assert lint.check_action_enum(root) == []


def test_action_enum_rejects_unknown_value(make_repo):
    body = "### [2026-08-10] 주제\n- **상태**: 미해결\n- **action**: 조사해보기"
    root = make_repo({"wiki/open-questions.md": page(body=body)})
    found = lint.check_action_enum(root)
    assert [f.code for f in found] == ["action"]
    assert "조사해보기" in found[0].message


def test_action_enum_requires_field_per_item(make_repo):
    body = "### [2026-08-10] 주제\n- **상태**: 미해결"
    root = make_repo({"wiki/open-questions.md": page(body=body)})
    found = lint.check_action_enum(root)
    assert [f.code for f in found] == ["action"]
    assert "누락" in found[0].message


def test_action_enum_skips_when_file_absent(make_repo):
    root = make_repo({"wiki/pages/index.md": page()})
    assert lint.check_action_enum(root) == []


def test_credential_in_raw_is_violation(make_repo):
    """raw/는 손대지 않은 원본이 쌓이는 곳 — 붙여넣은 자격증명이 가장 먼저 닿는다.

    wiki/raw는 load_pages에 수집되지 않는다 — 개수를 정확히 1건으로 못박아
    _raw_texts와의 중복 스캔(수집된 페이지로 한 번, raw 훑기로 또 한 번)이
    돌아오지 않았는지 확인한다.
    """
    root = make_repo({
        "wiki/raw/유출.md": 'api_key: "sk-abcdefghijklmnop1234567890"',
    })
    found = lint.check_sensitive(wikilib.load_pages(root), root)
    assert len(found) == 1
    assert found[0].code == "민감/credential"
    assert found[0].level == "violation"
    assert found[0].path == "wiki/raw/유출.md:1"


def test_raw_is_not_scanned_without_repo_root(make_repo):
    """repo_root를 안 넘기면 raw는 전혀 안 보인다.

    wiki/raw가 UNCOLLECTED_DIRS라 load_pages가 만드는 pages에는 애초에
    raw 내용이 없다 — repo_root가 없으면 _raw_texts도 raw를 못 읽으므로
    민감정보가 하나도 안 잡혀야 한다.
    """
    root = make_repo({"wiki/raw/유출.md": 'api_key: "sk-abcdefghijklmnop1234567890"'})
    assert lint.check_sensitive(wikilib.load_pages(root)) == []
