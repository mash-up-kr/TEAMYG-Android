---
id: vendor-android-kotlin-skills
title: Android/Kotlin/Compose 스킬 벤더링 + skill-finder + update-injected-skills Implementation Plan
status: done
type: work-order
created: 2026-07-22
updated: 2026-07-22
platforms: android
owner:
related_adr:
related_spec: vendor-android-kotlin-skills
related_code: vendor.py, search.py, update-injected-skills/SKILL.md, skill-finder/SKILL.md, sources.json, baseline.json, manifest.json
archived_reason: 구현 완료 — 73개 스킬 벤더링·skill-finder·update-injected-skills·CLAUDE.md 라우팅 전부 커밋(inline 실행).
tags: [plan, parfait, tooling, skills, vendoring]
---

# Android/Kotlin/Compose 스킬 벤더링 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 4개 외부 repo(android/skills·Kotlin/kotlin-agent-skills·skydoves/compose-performance-skills·chrisbanes/skills)의 73개 스킬을 이 repo `.claude/skills/`에 벤더링하고, 경량 검색 스킬(`skill-finder`)과 baseline+diff 업데이트 스킬(`update-injected-skills`)로 탐색·유지보수를 배선한다.

**Architecture:** Python(stdlib 전용) 스크립트 2개. `vendor.py`가 clone→discover→copy 및 baseline/manifest/catalog 생성 + diff 기반 델타 업데이트를 담당(초기 설치 = full 모드, 이후 = update 모드). `search.py`가 벤더된 SKILL.md frontmatter를 키워드 가중 랭킹. 두 스킬은 이 스크립트를 감싸는 얇은 SKILL.md. CLAUDE.md 워크플로 A에 spec/plan 작성 시 skill-finder 사용 규칙 추가.

**Tech Stack:** Python 3(stdlib: `subprocess`/`json`/`shutil`/`pathlib`/`re`/`argparse`/`unittest`), git CLI.

**Spec:** [specs/2026-07-22-vendor-android-kotlin-skills.md](../../specs/archive/2026-07-22-vendor-android-kotlin-skills.md)

**작업 repo:** team-yg-pesonal-agent(이 repo). 브랜치 `skills/vendor-android-kotlin-compose`(이미 생성됨). 커밋/push/PR은 CLAUDE.md 규율상 사용자 확인 후.

## Global Constraints

- **stdlib 전용** — pip 의존성 0. `python3`로 실행.
- **스킬 원본 불변** — 벤더된 SKILL.md 내용을 로컬 편집하지 않는다(순수 벤더, upstream 보존).
- **프로젝트 스킬 이름 = 디렉토리명** — `.claude/skills/<leaf>/SKILL.md` 1단계 배치.
- **라이선스** — 4 repo 전부 Apache-2.0. 각 repo LICENSE를 `.claude/skills-vendor/licenses/`에 보존.
- **경로 기준**: 스크립트는 `parfait/script/`에 위치, repo 루트(`parents[2]`) 기준으로 `.claude/skills`·`.claude/skills-vendor` 접근 → repo 이동에 무관. (규약: `parfait/script/README.md`)
- **캐시·산출 분리**: clone 캐시·머신용 json은 `.claude/skills-vendor/`(비-스킬 dir). `.cache/`는 `.gitignore`.
- **소스 SHA는 baseline.json이 SoT** — 사람용 baseline.md는 렌더 산출물.

## File Structure

```
parfait/script/                   # 파이썬 툴링 홈(규약: parfait/script/README.md)
  vendor.py, test_vendor.py       # 벤더/업데이트 엔진 + 테스트
  search.py, test_search.py       # 검색 랭킹 + 테스트
.claude/skills-vendor/            # 비-스킬 지원 dir (SKILL.md 없음 → CC 무시)
  sources.json                    # 입력: 4 repo (name, url) — branch는 자동 해석
  baseline.json                   # 머신 SoT: {repo: {sha, branch, url}}
  manifest.json                   # 머신 SoT: {leaf: {repo, path, sha}}
  baseline.md / MANIFEST.md / CATALOG.md   # 사람용 렌더 산출물
  licenses/<repo>.LICENSE
  .cache/<repo>/                  # blobless clone (gitignore)
.claude/skills/
  update-injected-skills/SKILL.md # vendor.py(parfait/script) 호출하는 얇은 스킬
  skill-finder/SKILL.md           # search.py(parfait/script) 호출하는 얇은 스킬
  <73 vendored>/SKILL.md(+부속파일)
```

---

### Task 1: skills-vendor 스캐폴드 + sources.json + gitignore

**Files:**
- Create: `.claude/skills-vendor/sources.json`
- Create: `.claude/skills-vendor/.gitignore`
- Create: `.claude/skills-vendor/licenses/.gitkeep`

- [ ] **Step 1: sources.json 작성**

```json
{
  "repos": [
    { "name": "android-skills", "url": "https://github.com/android/skills.git" },
    { "name": "kotlin-agent-skills", "url": "https://github.com/Kotlin/kotlin-agent-skills.git" },
    { "name": "compose-performance-skills", "url": "https://github.com/skydoves/compose-performance-skills.git" },
    { "name": "chrisbanes-skills", "url": "https://github.com/chrisbanes/skills.git" }
  ]
}
```

- [ ] **Step 2: .gitignore 작성** (캐시 제외)

```
.cache/
```

- [ ] **Step 3: licenses 디렉토리 유지 파일**

`.claude/skills-vendor/licenses/.gitkeep` — 빈 파일.

- [ ] **Step 4: Commit**

```bash
git add .claude/skills-vendor/sources.json .claude/skills-vendor/.gitignore .claude/skills-vendor/licenses/.gitkeep
git commit -m "feat(skills): 벤더링 스캐폴드 — sources.json + gitignore"
```

---

### Task 2: vendor.py — 헬퍼(clone/discover) + leaf_name 단위 테스트

**Files:**
- Create: `parfait/script/vendor.py`
- Test: `parfait/script/test_vendor.py`

**Interfaces:**
- Produces: `leaf_name(path)->str`, `sync_cache(repo)->(Path,str,str)`, `discover(cache,ref)->list[str]`, `copy_skill(cache,md_rel,leaf)`, 상수 `SKILLS_DIR`/`VENDOR_DIR`.

- [ ] **Step 1: 실패 테스트 작성** (`test_vendor.py`)

```python
import unittest
import vendor

class LeafNameTest(unittest.TestCase):
    def test_nested_path(self):
        self.assertEqual(vendor.leaf_name("jetpack-compose/theming/styles/SKILL.md"), "styles")
    def test_skills_wrapper(self):
        self.assertEqual(vendor.leaf_name("skills/compose-state-hoisting/SKILL.md"), "compose-state-hoisting")
    def test_root(self):
        self.assertEqual(vendor.leaf_name("SKILL.md"), "")

class AffectedTest(unittest.TestCase):
    def test_added_and_deleted(self):
        diff = "A\tskills/new-skill/SKILL.md\nD\tskills/old-skill/SKILL.md\nM\tsrc/other.kt"
        res = vendor.affected(diff)
        self.assertEqual(res["new-skill"], "mod")
        self.assertEqual(res["old-skill"], "del")
        self.assertNotIn("other", res)
    def test_dotdir_excluded(self):
        self.assertEqual(vendor.affected("A\t.claude-plugin/x/SKILL.md"), {})

if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 실패 확인**

Run: `cd parfait/script && python3 -m unittest -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'vendor'`

- [ ] **Step 3: vendor.py 헬퍼 구현** (파일 상단 ~ discover까지)

```python
#!/usr/bin/env python3
"""Vendor Android/Kotlin/Compose skills into .claude/skills/ with baseline+diff updates."""
import argparse, json, re, shutil, subprocess
from datetime import date
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]   # parfait/script/vendor.py → repo 루트
SKILLS_DIR = REPO_ROOT / ".claude" / "skills"
VENDOR_DIR = REPO_ROOT / ".claude" / "skills-vendor"
CACHE = VENDOR_DIR / ".cache"
SOURCES = VENDOR_DIR / "sources.json"
BASELINE_JSON = VENDOR_DIR / "baseline.json"
MANIFEST_JSON = VENDOR_DIR / "manifest.json"
LICENSES = VENDOR_DIR / "licenses"


def run(cmd, cwd=None):
    return subprocess.run(cmd, cwd=cwd, check=True, capture_output=True, text=True).stdout


def leaf_name(skill_md_path):
    return Path(skill_md_path).parent.name


def _is_skill_path(p):
    return p.endswith("SKILL.md") and not any(seg.startswith(".") for seg in p.split("/"))


def default_branch(url):
    out = run(["git", "ls-remote", "--symref", url, "HEAD"])
    m = re.search(r"ref:\s+refs/heads/(\S+)\s+HEAD", out)
    return m.group(1) if m else "main"


def sync_cache(repo):
    dest = CACHE / repo["name"]
    if not dest.exists():
        CACHE.mkdir(parents=True, exist_ok=True)
        run(["git", "clone", "--filter=blob:none", repo["url"], str(dest)])
    branch = repo.get("branch") or default_branch(repo["url"])
    run(["git", "fetch", "origin", branch], cwd=dest)
    run(["git", "checkout", branch], cwd=dest)
    run(["git", "reset", "--hard", f"origin/{branch}"], cwd=dest)
    sha = run(["git", "rev-parse", "HEAD"], cwd=dest).strip()
    return dest, branch, sha


def discover(cache, ref):
    out = run(["git", "ls-tree", "-r", "--name-only", ref], cwd=cache)
    return [p for p in out.splitlines() if _is_skill_path(p) and "/" in p]


def copy_skill(cache, skill_md_rel, leaf):
    src = cache / Path(skill_md_rel).parent
    dst = SKILLS_DIR / leaf
    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(src, dst, ignore=shutil.ignore_patterns(".git*"))


def affected(diff_text):
    res = {}
    for line in diff_text.splitlines():
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        status, path = parts[0], parts[-1]
        if not _is_skill_path(path) or "/" not in path:
            continue
        res[leaf_name(path)] = "del" if status.startswith("D") else "mod"
    return res
```

- [ ] **Step 4: 통과 확인**

Run: `cd parfait/script && python3 -m unittest -v`
Expected: PASS (LeafNameTest 3, AffectedTest 2)

- [ ] **Step 5: Commit**

```bash
git add parfait/script/vendor.py parfait/script/test_vendor.py
git commit -m "feat(skills): vendor.py 헬퍼(clone/discover/leaf/affected) + 테스트"
```

---

### Task 3: vendor.py — full 벤더 모드 + baseline/manifest/licenses 기록

**Files:**
- Modify: `parfait/script/vendor.py` (함수 추가)

**Interfaces:**
- Consumes: `sync_cache`, `discover`, `copy_skill`, `leaf_name`.
- Produces: `full_vendor(dry=False)->(baseline, manifest)`.

- [ ] **Step 1: full_vendor 구현** (vendor.py에 추가)

```python
def _copy_license(cache, name):
    for lic in ("LICENSE", "LICENSE.txt", "LICENSE.md"):
        p = cache / lic
        if p.exists():
            LICENSES.mkdir(parents=True, exist_ok=True)
            shutil.copy(p, LICENSES / f"{name}.LICENSE")
            return


def _resolve_leaf(md, repo_name, seen):
    leaf = leaf_name(md)
    if leaf in seen:                       # 충돌 시 repo 접두사
        leaf = f"{repo_name.split('-')[0]}-{leaf}"
    seen[leaf] = repo_name
    return leaf


def full_vendor(dry=False):
    sources = json.loads(SOURCES.read_text())["repos"]
    baseline, manifest, seen = {}, {}, {}
    for repo in sources:
        cache, branch, sha = sync_cache(repo)
        baseline[repo["name"]] = {"sha": sha, "branch": branch, "url": repo["url"]}
        for md in discover(cache, "HEAD"):
            leaf = _resolve_leaf(md, repo["name"], seen)
            manifest[leaf] = {"repo": repo["name"], "path": md, "sha": sha}
            if not dry:
                copy_skill(cache, md, leaf)
        if not dry:
            _copy_license(cache, repo["name"])
    if not dry:
        BASELINE_JSON.write_text(json.dumps(baseline, indent=2, ensure_ascii=False) + "\n")
        MANIFEST_JSON.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n")
        render_docs(baseline, manifest)
    return baseline, manifest
```

- [ ] **Step 2: 임시 render_docs 스텁 추가** (Task 4에서 채움 — import 에러 방지)

```python
def render_docs(baseline, manifest):
    pass  # Task 4에서 구현
```

- [ ] **Step 3: dry-run 스모크 테스트**

Run: `cd parfait/script && python3 -c "import vendor; b,m=vendor.full_vendor(dry=True); print(len(b),'repos', len(m),'skills')"`
Expected: `4 repos 73 skills` (± upstream 변동). 파일 미생성(dry).

- [ ] **Step 4: Commit**

```bash
git add parfait/script/vendor.py
git commit -m "feat(skills): vendor.py full 벤더 모드 + baseline/manifest/license 기록"
```

---

### Task 4: vendor.py — 사람용 문서 렌더(baseline.md/MANIFEST.md/CATALOG.md)

**Files:**
- Modify: `parfait/script/vendor.py` (`render_docs` 구현)

**Interfaces:**
- Consumes: baseline/manifest dict.
- Produces: `.claude/skills-vendor/{baseline.md, MANIFEST.md, CATALOG.md}`.

- [ ] **Step 1: render_docs 구현** (스텁 대체)

```python
def _desc_of(leaf):
    md = SKILLS_DIR / leaf / "SKILL.md"
    if not md.exists():
        return ""
    m = re.search(r"^description:\s*(.+)$", md.read_text(encoding="utf-8", errors="ignore"), re.M)
    return (m.group(1).strip().strip("\"'")[:140]) if m else ""


def _topic(path):
    segs = Path(path).parts
    top = segs[0]
    if top == "skills" and len(segs) >= 2:      # kotlin/chrisbanes flat wrapper
        return "skills"
    return top


def render_docs(baseline, manifest):
    today = date.today().isoformat()
    # baseline.md
    lines = ["# 벤더 스킬 baseline (SoT = baseline.json)", "",
             f"> 갱신: {today}", "", "| repo | branch | SHA |", "|---|---|---|"]
    for name, info in baseline.items():
        lines.append(f"| {name} | {info['branch']} | `{info['sha'][:9]}` |")
    (VENDOR_DIR / "baseline.md").write_text("\n".join(lines) + "\n")
    # MANIFEST.md
    ml = ["# 벤더 스킬 MANIFEST (SoT = manifest.json)", "",
          f"> 갱신: {today} | 총 {len(manifest)}개", "", "| skill | repo | 원본 경로 |", "|---|---|---|"]
    for leaf in sorted(manifest):
        i = manifest[leaf]
        ml.append(f"| {leaf} | {i['repo']} | `{i['path']}` |")
    (VENDOR_DIR / "MANIFEST.md").write_text("\n".join(ml) + "\n")
    # CATALOG.md — repo > topic 그룹핑
    groups = {}
    for leaf, i in manifest.items():
        groups.setdefault((i["repo"], _topic(i["path"])), []).append(leaf)
    cl = ["# 벤더 스킬 CATALOG (주제별)", "",
          f"> 갱신: {today} | spec/plan 작성 시 `skill-finder`로 검색, 목차는 아래.", ""]
    for (repo, topic) in sorted(groups):
        cl.append(f"## {repo} / {topic}")
        for leaf in sorted(groups[(repo, topic)]):
            cl.append(f"- **{leaf}** — {_desc_of(leaf)}")
        cl.append("")
    (VENDOR_DIR / "CATALOG.md").write_text("\n".join(cl) + "\n")
```

- [ ] **Step 2: 렌더 스모크 테스트** (dry 아님이면 실제 파일 생성되나, 여기선 함수만 확인)

Run: `cd parfait/script && python3 -c "import vendor; vendor.render_docs({'r':{'branch':'main','sha':'abc123456'}}, {'x':{'repo':'r','path':'a/b/x/SKILL.md','sha':'abc'}}); print(open(vendor.VENDOR_DIR/'CATALOG.md').read())"`
Expected: CATALOG.md에 `## r / a` + `- **x** — ...` 출력.

- [ ] **Step 3: 렌더 산출물 되돌리기** (스모크로 생성된 임시 파일 제거 — Task 8에서 실데이터로 재생성)

```bash
git checkout -- .claude/skills-vendor/ 2>/dev/null; rm -f .claude/skills-vendor/CATALOG.md .claude/skills-vendor/baseline.md .claude/skills-vendor/MANIFEST.md
```

- [ ] **Step 4: Commit**

```bash
git add parfait/script/vendor.py
git commit -m "feat(skills): vendor.py 사람용 문서 렌더(baseline/MANIFEST/CATALOG)"
```

---

### Task 5: vendor.py — update(diff) 모드 + CLI

**Files:**
- Modify: `parfait/script/vendor.py` (`update`, `main` 추가)

**Interfaces:**
- Consumes: baseline.json/manifest.json, `sync_cache`, `discover`, `copy_skill`.
- Produces: `update(dry=False)->changes(dict)`, `main()` CLI(`--full`/`--update`/`--dry-run`).

- [ ] **Step 1: update + main 구현** (vendor.py에 추가)

```python
def update(dry=False):
    baseline = json.loads(BASELINE_JSON.read_text())
    manifest = json.loads(MANIFEST_JSON.read_text())
    sources = json.loads(SOURCES.read_text())["repos"]
    changes = {"added": [], "modified": [], "deleted": []}
    for repo in sources:
        name = repo["name"]
        cache, branch, head = sync_cache(repo)
        base = baseline.get(name, {}).get("sha")
        if base == head:
            continue
        cur = {leaf_name(md): md for md in discover(cache, "HEAD")}
        prev = {leaf for leaf, i in manifest.items() if i["repo"] == name}
        for leaf in list(prev):                       # 삭제분
            if leaf not in cur:
                changes["deleted"].append(leaf)
                if not dry and (SKILLS_DIR / leaf).exists():
                    shutil.rmtree(SKILLS_DIR / leaf)
                manifest.pop(leaf, None)
        for leaf, md in cur.items():                   # 추가/수정분
            d = str(Path(md).parent)
            diff = run(["git", "diff", "--name-only", f"{base}..HEAD", "--", d], cwd=cache) if base else "x"
            if leaf not in manifest:
                changes["added"].append(leaf)
            elif diff.strip():
                changes["modified"].append(leaf)
            else:
                continue
            if not dry:
                copy_skill(cache, md, leaf)
            manifest[leaf] = {"repo": name, "path": md, "sha": head}
        baseline[name] = {"sha": head, "branch": branch, "url": repo["url"]}
    if not dry:
        BASELINE_JSON.write_text(json.dumps(baseline, indent=2, ensure_ascii=False) + "\n")
        MANIFEST_JSON.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n")
        render_docs(baseline, manifest)
    return changes


def main():
    ap = argparse.ArgumentParser(description="Vendor/update injected skills")
    g = ap.add_mutually_exclusive_group(required=True)
    g.add_argument("--full", action="store_true", help="전량 벤더(초기 설치)")
    g.add_argument("--update", action="store_true", help="baseline 대비 delta 업데이트")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    if a.full:
        b, m = full_vendor(dry=a.dry_run)
        print(f"[full] {len(b)} repos, {len(m)} skills{' (dry)' if a.dry_run else ''}")
    else:
        c = update(dry=a.dry_run)
        print(f"[update] +{len(c['added'])} ~{len(c['modified'])} -{len(c['deleted'])}{' (dry)' if a.dry_run else ''}")
        for k in ("added", "modified", "deleted"):
            if c[k]:
                print(f"  {k}: {', '.join(sorted(c[k]))}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: CLI 파싱 테스트** (update는 baseline.json 필요 → 여기선 --help만)

Run: `cd parfait/script && python3 vendor.py --help`
Expected: `--full`/`--update`/`--dry-run` 표시, exit 0.

- [ ] **Step 3: 기존 단위 테스트 재확인**

Run: `cd parfait/script && python3 -m unittest -v`
Expected: PASS(5).

- [ ] **Step 4: Commit**

```bash
git add parfait/script/vendor.py
git commit -m "feat(skills): vendor.py update(diff) 모드 + CLI"
```

---

### Task 6: update-injected-skills SKILL.md

**Files:**
- Create: `.claude/skills/update-injected-skills/SKILL.md`

- [ ] **Step 1: SKILL.md 작성**

```markdown
---
name: update-injected-skills
description: 벤더링한 android/kotlin/compose 스킬을 upstream과 동기화한다. 사용자가 "스킬 업데이트", "벤더 스킬 갱신", "update-injected-skills", "스킬 최신화"라고 할 때 사용. baseline.json의 마지막 SHA 대비 delta(추가·수정·삭제)만 재벤더한다.
---

# update-injected-skills

`.claude/skills-vendor/`의 4개 소스 repo를 baseline SHA 대비 diff로 업데이트한다.

## 절차
1. **dry-run 먼저**: `python3 parfait/script/vendor.py --update --dry-run`
   - 출력 `+추가 ~수정 -삭제`와 스킬 목록 확인.
2. delta 있으면 실제 적용: `python3 parfait/script/vendor.py --update`
3. 변경 요약을 사용자에게 보고(추가/수정/삭제 스킬명 + baseline SHA 갱신분).
4. `.claude/skills/`·`.claude/skills-vendor/{baseline,manifest,MANIFEST,CATALOG}` 변경을 **사용자 확인 후** 커밋(CLAUDE.md Git 규율: 브랜치→PR→merge).

## 주의
- baseline SoT = `baseline.json`. `baseline.md`는 렌더 산출물.
- 전량 재설치가 필요하면 `--full`(초기 설치 전용).
- 스킬 원본은 편집 금지(순수 벤더).
```

- [ ] **Step 2: Commit**

```bash
git add .claude/skills/update-injected-skills/SKILL.md
git commit -m "feat(skills): update-injected-skills 스킬"
```

---

### Task 7: 실제 전량 벤더 실행 (73 스킬 설치)

**Files:**
- Create: `.claude/skills/<73 vendored>/` (스크립트 산출)
- Create: `.claude/skills-vendor/{baseline.json, manifest.json, baseline.md, MANIFEST.md, CATALOG.md, licenses/*.LICENSE}`

- [ ] **Step 1: full 실행**

Run: `cd "$(git rev-parse --show-toplevel)" && python3 parfait/script/vendor.py --full`
Expected: `[full] 4 repos, 73 skills` (± upstream).

- [ ] **Step 2: 결과 검증**

Run: `ls .claude/skills | wc -l; ls .claude/skills-vendor/licenses; python3 -c "import json;print(len(json.load(open('.claude/skills-vendor/manifest.json'))))"`
Expected: 스킬 dir 73 + 기존 스킬, licenses 4개, manifest 73.

- [ ] **Step 3: 충돌 접두사 발생 여부 확인** (leaf 충돌 로그)

Run: `python3 -c "import json;m=json.load(open('.claude/skills-vendor/manifest.json'));print([k for k in m if k.count('-') and m[k]['repo'].split('-')[0]==k.split('-')[0] and k.split('-',1)[1] in m])"`
Expected: `[]` (사전 검증상 충돌 없음). 비어있지 않으면 접두사된 스킬 — CATALOG·보고에 명시.

- [ ] **Step 4: 벤더 산출 커밋** (대량)

```bash
git add .claude/skills .claude/skills-vendor
git commit -m "feat(skills): 73개 android/kotlin/compose 스킬 전량 벤더링 + baseline/manifest/catalog/licenses"
```

---

### Task 8: search.py — 랭킹 로직 (TDD)

**Files:**
- Create: `parfait/script/search.py`
- Test: `parfait/script/test_search.py`

**Interfaces:**
- Produces: `tokenize(s)->list`, `score(query, skill)->float`, `search(query, top)->list[(score, skill)]`, `parse_skill(md_path)->dict`, 상수 `SKILLS_DIR`.

- [ ] **Step 1: 실패 테스트 작성**

```python
import unittest, tempfile
from pathlib import Path
import search

def _mk(root, name, desc):
    d = root / name
    d.mkdir()
    (d / "SKILL.md").write_text(f"---\nname: {name}\ndescription: {desc}\n---\n# {name}\n", encoding="utf-8")

class ScoreTest(unittest.TestCase):
    def test_name_weight_beats_desc(self):
        s_name = {"name": "recomposition-debug", "desc": "misc", "headings": ""}
        s_desc = {"name": "misc", "desc": "recomposition tuning", "headings": ""}
        self.assertGreater(search.score("recomposition", s_name), search.score("recomposition", s_desc))
    def test_no_match_zero(self):
        self.assertEqual(search.score("xyz", {"name": "a", "desc": "b", "headings": ""}), 0.0)

class SearchTest(unittest.TestCase):
    def test_ranks_relevant_first(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t)
            _mk(root, "stability-diagnostics", "diagnose compose stability")
            _mk(root, "navigation-3", "set up navigation")
            search.SKILLS_DIR = root
            res = search.search("compose stability", top=5)
            self.assertEqual(res[0][1]["name"], "stability-diagnostics")

if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 실패 확인**

Run: `cd parfait/script && python3 -m unittest -v`
Expected: FAIL — `No module named 'search'`

- [ ] **Step 3: search.py 구현**

```python
#!/usr/bin/env python3
"""Rank vendored skills by a natural-language query over SKILL.md frontmatter."""
import argparse, re
from pathlib import Path

SKILLS_DIR = Path(__file__).resolve().parents[2] / ".claude" / "skills"   # parfait/script → repo/.claude/skills


def tokenize(s):
    return re.findall(r"[a-z0-9]+", s.lower())


def parse_skill(md_path):
    text = md_path.read_text(encoding="utf-8", errors="ignore")
    desc = ""
    m = re.search(r"^---\n(.*?)\n---", text, re.S)
    if m:
        dm = re.search(r"^description:\s*(.+)$", m.group(1), re.M)
        if dm:
            desc = dm.group(1).strip().strip("\"'")
    headings = " ".join(re.findall(r"^#{1,3}\s+(.+)$", text, re.M))
    return {"name": md_path.parent.name, "desc": desc, "headings": headings, "path": str(md_path)}


def score(query, skill):
    q = set(tokenize(query))
    if not q:
        return 0.0
    name_t, desc_t, head_t = tokenize(skill["name"]), tokenize(skill["desc"]), tokenize(skill.get("headings", ""))
    total = 0.0
    for t in q:
        total += 4 * name_t.count(t) + 2 * desc_t.count(t) + 1 * head_t.count(t)
        if any(t in n for n in name_t):
            total += 1
    return total


def search(query, top=8):
    skills = []
    for d in sorted(SKILLS_DIR.iterdir()):
        md = d / "SKILL.md"
        if md.is_file():
            skills.append(parse_skill(md))
    ranked = sorted(((score(query, s), s) for s in skills), key=lambda x: -x[0])
    return [(sc, s) for sc, s in ranked if sc > 0][:top]


def main():
    ap = argparse.ArgumentParser(description="Search vendored skills")
    ap.add_argument("query", nargs="+")
    ap.add_argument("--top", type=int, default=8)
    a = ap.parse_args()
    q = " ".join(a.query)
    res = search(q, a.top)
    if not res:
        print(f"매칭 스킬 없음: {q}")
        return
    for sc, s in res:
        print(f"{s['name']}  (score {sc:.0f}) — {s['desc'][:120]}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 4: 통과 확인**

Run: `cd parfait/script && python3 -m unittest -v`
Expected: PASS(3).

- [ ] **Step 5: Commit**

```bash
git add parfait/script/search.py parfait/script/test_search.py
git commit -m "feat(skills): skill-finder search.py 랭킹 + 테스트"
```

---

### Task 9: 실제 벤더 스킬 대상 검색 스모크 + skill-finder SKILL.md

**Files:**
- Create: `.claude/skills/skill-finder/SKILL.md`

- [ ] **Step 1: 실데이터 검색 스모크**

Run: `python3 parfait/script/search.py "recomposition performance" --top 5`
Expected: recomposition/성능 관련 벤더 스킬(예: `debugging-recompositions`·`compose-recomposition-performance`)이 상위.

- [ ] **Step 2: SKILL.md 작성**

```markdown
---
name: skill-finder
description: 벤더링된 android/kotlin/compose 스킬을 자연어로 검색해 적재적소 스킬을 찾는다. spec/plan 작성이나 TJYG-Android 구현 중 "어떤 스킬 써야 하지", "recomposition 관련 스킬 찾아", "이 주제 스킬 검색"이 필요할 때 사용. SKILL.md frontmatter를 키워드 가중 랭킹해 상위 후보를 돌려준다.
---

# skill-finder

`.claude/skills/`의 벤더 스킬을 쿼리로 랭킹한다(이름>description>제목 가중).

## 사용
`python3 parfait/script/search.py "<자연어 쿼리>" [--top N]`

예: `python3 parfait/script/search.py "lazy list scroll jank" --top 5`

## 언제
- **spec/plan 작성 시**(워크플로 A): 다룰 주제(recomposition·stability·navigation·coroutines·testing·gradle 등)를 쿼리해 관련 벤더 스킬을 찾은 뒤, 그 스킬을 네이티브 `Skill`로 호출해 설계·계획에 반영.
- 구현 중 특정 문제(성능·안정성·마이그레이션)에 맞는 스킬을 모를 때.

## 참고
- 전체 목차는 `.claude/skills-vendor/CATALOG.md`.
- 검색은 후보 랭킹만 — 실제 지침은 해당 스킬을 `Skill`로 호출해 로드.
```

- [ ] **Step 3: Commit**

```bash
git add .claude/skills/skill-finder/SKILL.md
git commit -m "feat(skills): skill-finder 스킬"
```

---

### Task 10: CLAUDE.md 워크플로 A 라우팅 — spec/plan 시 skill-finder 사용

**Files:**
- Modify: `CLAUDE.md` (워크플로 A 섹션)

- [ ] **Step 1: 워크플로 A에 규칙 추가**

`### A. TJYG-Android 코드 구현` 블록의 3단계 목록 바로 아래에 다음을 추가:

```markdown
- **스킬 적재적소(필수)**: brainstorming(스펙)·writing-plans(계획) 단계에서, 다룰 주제
  (Compose UI/state·recomposition·stability·side-effects·navigation·coroutines·testing·gradle·마이그레이션 등)에 대해
  **`skill-finder`로 먼저 검색**(`python3 parfait/script/search.py "<주제>"`)하고,
  상위 후보 중 관련 스킬을 네이티브 `Skill`로 로드한 뒤 설계/계획을 확정한다. 전체 목차는
  `.claude/skills-vendor/CATALOG.md`. 벤더 스킬 갱신은 `update-injected-skills`.
```

- [ ] **Step 2: 렌더 확인**

Run: `grep -n "skill-finder" CLAUDE.md`
Expected: 워크플로 A 안에 1건.

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: 워크플로 A에 skill-finder 적재적소 라우팅 추가"
```

---

### Task 11: 최종 검증 + spec/plan 아카이브 등록

**Files:**
- Modify: `parfait/specs/README.md`, `parfait/specs/2026-07-22-vendor-android-kotlin-skills.md`(status), `parfait/plans/README.md`, 본 plan(status)

- [ ] **Step 1: 전체 스모크**

Run:
```bash
python3 parfait/script/vendor.py --update --dry-run
python3 parfait/script/search.py "stability inference" --top 3
cd parfait/script && python3 -m unittest
```
Expected: update dry-run `+0 ~0 -0`(방금 벤더 직후), 검색 상위에 stability 스킬, 테스트 PASS(test_vendor+test_search).

- [ ] **Step 2: spec `implemented`·archive / plan `done`·archive**

- `parfait/specs/2026-07-22-vendor-android-kotlin-skills.md` frontmatter `status: implemented` → `git mv`로 `specs/archive/`. 본 plan `status: done`(+`archived_reason`) → `plans/archive/`.
- 이동 시 상대링크 `../` → `../../` 보정(archive 한 단계 깊음). 상호 spec↔plan 링크도 archive 경로로.
- `parfait/specs/README.md`·`parfait/plans/README.md` active 행 → archive 표 이동.

- [ ] **Step 3: Commit**

```bash
git add parfait/
git commit -m "docs(parfait): 스킬 벤더링 spec/plan implemented·done 아카이브"
```

- [ ] **Step 4: 사용자 확인 후 push→PR→merge** (CLAUDE.md 규율)

---

## Self-Review 메모
- 스펙 커버리지: A(Task 1·3·7)·B 라우팅(Task 10)·C skill-finder(Task 8·9)·D update-injected-skills(Task 2·5·6) 전부 대응. 라이선스(Task 3), CATALOG/MANIFEST(Task 4), baseline SoT(Task 3·5).
- 타입 일관: `leaf_name`·`copy_skill`·`discover`·`full_vendor`/`update`/`render_docs` 시그니처 태스크 간 일치.
- 열린 질문(스펙): 랭킹은 키워드 가중 채택(BM25 미도입, 73 규모 충분). CATALOG 그룹은 repo/topic 자동 — 후속 수동 조정 여지. 충돌 접두사는 Task 7 Step 3에서 검증(현재 0).
