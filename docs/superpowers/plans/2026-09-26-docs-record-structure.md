# docs 기록 구조 개선 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `docs/index.md`·`docs/doc-baseline.md`에 날짜순으로 쌓인 서술을 현재 상태(`docs/status.md`)·시간순 기록(`docs/log.md`)·기준점(`doc-baseline`)으로 나누고, 닫힌 lint 보고서를 없앤다.

**Architecture:** 모든 삭제에 앞서 원본 사본과 추출표를 만들어 미결·절차 노하우가 갈 곳을 먼저 정한다(Task 1). 기계적 이관(로그·기준선·규칙)을 먼저 끝내 체크포인트를 두고, 코드 대조가 필요한 `status.md` 작성과 index 축소를 B단계로 둔다. 일회성 추출·변환 스크립트는 스크래치패드에 두고 저장소에 넣지 않는다.

**Tech Stack:** Markdown, Python 3 stdlib(일회성 스크립트), `git grep`, 기존 `docs/script/check_links.py`.

**Spec:** [`docs/superpowers/specs/2026-09-26-docs-record-structure-design.md`](../specs/2026-09-26-docs-record-structure-design.md)

## Global Constraints

- 작업 저장소 `/Users/jeonheehoon/Documents/work_station/mashup/github/TJYG-Android`, 브랜치 `feature/#525-ai-docs-set-up`.
- 문서 본문은 **한국어**, 코드·식별자·파일명·커밋 메시지는 **영어**(루트 `CLAUDE.md`).
- 코드 주석·KDoc 규약: [`docs/code-conventions.md`](../../code-conventions.md). 서브에이전트 브리프에 이 링크를 넣는다.
- 기록 기준은 스펙 §2 표가 정본이다. `status.md` 규칙은 스펙 §3.1, `log.md` 형식은 §3.2를 그대로 따른다.
- Kotlin·Gradle·CI 파일을 건드리지 않는다. `wiki/`를 건드리지 않는다.
- 스펙 §6 제외 목록(`open-questions` 해소 항목 정리, `server-baseline` 이력 표, 아카이브·ADR·architecture 본문, 과거 서술 파일, `search.py`)은 손대지 않는다. 예외: Task 5의 open-questions 신설·보강.
- **커밋은 사용자가 요청할 때만 한다.** 각 태스크의 Commit 스텝은 메시지를 제안하고 승인을 받은 뒤에 실행한다. 커밋 메시지에 `Co-Authored-By` 트레일러를 붙이지 않는다.
- 스크래치패드: `/private/tmp/claude-501/-Users-jeonheehoon-Documents-work-station-mashup-github-TJYG-Android/7e8e093c-a087-495a-9593-41b0e9d070aa/scratchpad` (아래 `$S`). 셸에서 `export S=<그 경로>`를 먼저 한다. 세션이 바뀌면 그 세션의 스크래치패드를 쓰되, `$S/orig/`는 Task 1이 뜬 사본이므로 새 경로로 복사해 간다.
- 모든 명령은 저장소 루트에서 실행한다. 셸은 macOS zsh다 — `grep -E`·`git grep -E`에 `\b`를 쓰지 않는다.

## Review Focus

1. **이력 표의 섞인 정렬** — 이력 표는 앞 약 25행만 최신이 위이고, 그 뒤는 대체로 오래된 것이 위이며 일부 행이 제자리를 벗어나 있다. `to_log.py`는 내림차순 구간만 뒤집고 날짜로 안정 정렬한다. Task 2 Step 4에서 날짜 단조 증가를 확인한다.
2. **수치 추출 오인** — "신규 유닛 0건"·인용문 속 "유닛 6건" 같은 부분 수치를 집으면 다음 회차 기준이 틀린다. `to_log.py`가 유닛·계측 값이 이전 행보다 줄어든 행을 `DECREASE`로 출력하고, Task 2 Step 3에서 손으로 보정한다.
3. **흔한 단어 앵커** — `Canvas`처럼 어디에나 있는 단어는 `git grep`을 무의미하게 통과한다. Task 5 Step 6에서 대문자 시작 앵커의 선언 위치를 확인한다.
4. **status가 닫힌 OQ를 가리킴** — ⚠️ 줄의 OQ가 `해소됨`(볼드 포함)이면 거짓 경고가 된다. Task 5 Step 7에서 항목 범위 전체(다음 `### `까지)를 보고 검사한다.
5. **doc-baseline을 가리키는 해소 메모** — `open-questions`의 두 해소 메모가 「현재 기준선」 절을 가리킨다. Task 3 Step 6에서 절 제목과 참조를 확인한다.

---

### Task 0: 스펙·계획 커밋

`log.md`·`status.md`·doc-baseline이 스펙을 링크한다. 스펙이 커밋되지 않으면 커밋된 트리에서 그 링크가 깨진다(`check_links`는 워킹 트리를 보므로 못 잡는다).

**Files:**
- Add: `docs/superpowers/specs/2026-09-26-docs-record-structure-design.md`, `docs/superpowers/plans/2026-09-26-docs-record-structure.md`

- [ ] **Step 1: Commit (사용자 승인 후)**

```bash
git add docs/superpowers/specs/2026-09-26-docs-record-structure-design.md docs/superpowers/plans/2026-09-26-docs-record-structure.md
git commit -m "docs(superpowers): add docs record-structure spec and plan"
```

---

## A단계 — 기계적 이관

### Task 1: 원본 사본 + 추출표

스펙 §4. 이후 모든 삭제의 전제다. 저장소 파일을 바꾸지 않는다.

**Files:**
- Create: `$S/extract.py`, `$S/join.py` (일회성)
- Create: `$S/orig/{index.md,doc-baseline.md,lint-2026-07-06-parfait.md,lint-2026-07-22-parfait.md}` (원본 사본 — 이후 모든 `src` 참조의 기준)
- Create: `$S/extract.tsv` (산출)

**Interfaces:**
- Produces: `$S/extract.tsv` — 탭 구분, 헤더 `id	src	marker	text	class	target	note`.
  - `id`: `E001`부터.
  - `src`: `orig/<파일>:<줄>` — 사본 기준. 줄은 해당 문장이 시작하는 줄.
  - `class`: `1`~`5`(스펙 §4 ①~⑤).
  - `target`: class 1·2 → 영역 번호(스펙 §3.1의 1~15). class 3 → 기존 OQ에 붙이면 `OQ-P-NNN`, 「실기기 미확인」으로 가면 `device`. class 4 → `procedure`. class 5 → `drop`.
  - `note`: class 5는 버린 사유 한 줄. class 1은 OQ ID. class 3 `device`는 OQ ID(없으면 빈칸).
- Produces: `$S/orig/` 사본 — Task 2·3·5가 읽는다.

- [ ] **Step 1: 추출 스크립트 작성** — `$S/extract.py`

```python
# $S/extract.py — 사용법: python3 $S/extract.py $S
# 원본을 $S/orig/ 에 떠 두고, src 는 그 사본 기준 줄 번호로 적는다.
import csv, re, pathlib, shutil, sys

S = pathlib.Path(sys.argv[1])
ORIG = S / "orig"
ORIG.mkdir(exist_ok=True)
FILES = {
    "index": "docs/index.md",
    "baseline": "docs/doc-baseline.md",
    "lint0706": "docs/synthesis/lint-2026-07-06-parfait.md",
    "lint0722": "docs/synthesis/lint-2026-07-22-parfait.md",
}
for key, rel in FILES.items():
    dst = ORIG / pathlib.Path(rel).name
    if not dst.exists():
        shutil.copyfile(rel, dst)

MARK = re.compile(r"⚠️|❌|📌|미결|미조치|실기기|실서버|직전 회차가 확인한 것|규율|판정 기준|다음 회차")
SENT_END = re.compile(r"(?<=[다요])\.\s+")

def lines_of(name):
    return (ORIG / name).read_text(encoding="utf-8").splitlines()

def section(lines, title):
    """title 로 시작하는 H2 다음 줄부터 다음 H2 직전까지 (1-based 포함 범위)."""
    s = next(i for i, l in enumerate(lines, 1) if l.startswith(title))
    e = next((i - 1 for i, l in enumerate(lines, 1) if i > s and l.startswith("## ")), len(lines))
    return s + 1, e

def prose_units(lines, a, b):
    """빈 줄로 나눈 문단을 문장으로 자르고, 각 문장의 시작 줄 번호를 돌려준다."""
    para, offs = [], []  # 문단 텍스트 조각, (오프셋, 줄 번호)
    def flush():
        text = " ".join(para)
        pos = 0
        for m in list(SENT_END.finditer(text)) + [None]:
            end = m.start() + 1 if m else len(text)
            sent = text[pos:end].strip()
            if sent:
                line = max(no for off, no in offs if off <= pos)
                yield line, sent
            if m:
                pos = m.end()
    for no in range(a, b + 1):
        line = lines[no - 1]
        if line.strip() == "" or line.startswith("| "):
            if para:
                yield from flush()
            para, offs = [], []
            if line.startswith("| "):
                yield no, line  # 표 행은 줄 단위
            continue
        offset = len(" ".join(para)) + (1 if para else 0)
        offs.append((offset, no))
        para.append(line.strip())
    if para:
        yield from flush()

def row_units(lines, a, b):
    """표 행·목록 줄을 줄 단위로. 긴 줄은 문장으로 자르되 줄 번호는 그대로."""
    for no in range(a, b + 1):
        line = lines[no - 1]
        if not line.strip():
            continue
        for sent in SENT_END.split(line):
            yield no, sent

units = []
idx = lines_of("index.md")
a, b = section(idx, "## 지금 상태")
units += [("index.md", n, t) for n, t in prose_units(idx, a, b)]
units += [("index.md", n, t) for n, t in row_units(idx, b + 1, len(idx))]
base = lines_of("doc-baseline.md")
a, b = section(base, "## 현재 기준선")
units += [("doc-baseline.md", n, t) for n, t in prose_units(base, a, b)]
a, b = section(base, "## 기준선 이력")
units += [("doc-baseline.md", n, t) for n, t in row_units(base, a, b) if base[n - 1].startswith("| 20")]
for name in ("lint-2026-07-06-parfait.md", "lint-2026-07-22-parfait.md"):
    ls = lines_of(name)
    units += [(name, n, t) for n, t in row_units(ls, 1, len(ls))]

rows = []
for name, no, text in units:
    m = MARK.findall(text)
    if m:
        rows.append((f"orig/{name}:{no}", "".join(sorted(set(m))), text))

with (S / "extract.tsv").open("w", encoding="utf-8", newline="") as f:
    w = csv.writer(f, delimiter="\t")
    w.writerow(["id", "src", "marker", "text"])
    for i, (src, mk, text) in enumerate(rows, 1):
        w.writerow([f"E{i:03d}", src, mk, text])

by_src = {}
for src, _, _ in rows:
    by_src[src.split(":")[0]] = by_src.get(src.split(":")[0], 0) + 1
print(len(rows), "rows", by_src)
```

- [ ] **Step 2: 실행**

Run: `python3 $S/extract.py $S`
Expected: `386 rows {'orig/index.md': 34, 'orig/doc-baseline.md': 337, 'orig/lint-2026-07-06-parfait.md': 5, 'orig/lint-2026-07-22-parfait.md': 10}` 근처(원본이 바뀌지 않았다면 정확히 이 값). 네 소스 모두 0보다 커야 한다. `StopIteration`이면 절 제목(`## 지금 상태`, `## 현재 기준선`, `## 기준선 이력`)을 사본에서 확인한다.

- [ ] **Step 3: 분류 — 서브에이전트 4개 병렬**

`extract.tsv`의 행을 id 순서로 4등분한다(E001–, …). 각 서브에이전트에게 준다:
- 스펙 §3.1(영역 번호 1~15)과 §4(분류 ①~⑤) 원문
- 자기 몫의 id 범위, `$S/extract.tsv` 경로, `$S/orig/` 경로(원문 문맥 확인용)
- 판정 규칙: OQ ID의 항목을 `docs/synthesis/open-questions.md`에서 찾아 `- **상태**:` 줄을 본다. 값이 `해소됨` 또는 `**해소됨**`으로 시작하면 해소, 아니면 열림(파일 머리 "읽는 법 — 부정 매칭"). 해소됐어도 해소 메모에 잔존 조건이 있으면 열림.
- 코드 확인은 하지 않는다. class 2는 "코드 관련인데 OQ 없음" 표시만 한다(판정은 Task 5).
- 중복 처리는 하지 않는다(코디네이터가 Step 4에서 한다).
- 산출: `$S/extract.part<N>.tsv` 파일. 탭 구분 4열 `id	class	target	note`, 헤더 한 줄. 채팅으로 TSV를 돌려주지 않는다.

- [ ] **Step 4: 병합과 전역 중복 처리** — `$S/join.py`

```python
# $S/join.py — 사용법: python3 $S/join.py $S
import csv, glob, pathlib, sys
S = pathlib.Path(sys.argv[1])
rows = list(csv.DictReader((S / "extract.tsv").open(encoding="utf-8"), delimiter="\t"))
parts = {}
for p in sorted(glob.glob(str(S / "extract.part*.tsv"))):
    for r in csv.DictReader(open(p, encoding="utf-8"), delimiter="\t"):
        parts[r["id"]] = r
seen = {}
for r in rows:
    p = parts.get(r["id"], {})
    r["class"], r["target"], r["note"] = p.get("class", ""), p.get("target", ""), p.get("note", "")
    oq = r["note"] if r["class"] in {"1", "3"} and r["note"].startswith("OQ-P-") else None
    if oq:
        if oq in seen:
            r["class"], r["target"], r["note"] = "5", "drop", f"중복 {seen[oq]}"
        else:
            seen[oq] = r["id"]
with (S / "extract.tsv").open("w", encoding="utf-8", newline="") as f:
    w = csv.DictWriter(f, fieldnames=["id", "src", "marker", "text", "class", "target", "note"], delimiter="\t")
    w.writeheader(); w.writerows(rows)
missing = [r["id"] for r in rows if r["id"] not in parts]
print("rows", len(rows), "missing", len(missing), missing[:10])
```

Run: `python3 $S/join.py $S`
Expected: `missing 0 []`.

- [ ] **Step 5: 검증**

```bash
python3 - "$S" <<'EOF'
import csv, collections, sys
rows = list(csv.DictReader(open(sys.argv[1] + "/extract.tsv", encoding="utf-8"), delimiter="\t"))
bad = [r["id"] for r in rows if r["class"] not in {"1","2","3","4","5"} or not r["target"]]
drop_no_note = [r["id"] for r in rows if r["class"] == "5" and not r["note"]]
print("rows", len(rows), collections.Counter(r["class"] for r in rows))
print("unclassified", len(bad), bad[:20])
print("drop without reason", len(drop_no_note), drop_no_note[:20])
EOF
```
Expected: `unclassified 0 []`, `drop without reason 0 []`.

- [ ] **Step 6: 사용자 확인**

class별 건수, class 4(절차 노하우) 전 항목, lint 두 파일에서 나온 행의 분류를 사용자에게 보인다. 커밋 없음.

---

### Task 2: `log.md` 신설 + lint 보고서 삭제

스펙 §3.2. doc-baseline 이력 표는 이 태스크에서 **읽기만** 한다(삭제는 Task 3).

**Files:**
- Create: `$S/to_log.py` (일회성), `$S/audit.md` (중간 산출)
- Create: `docs/log.md`
- Delete: `docs/synthesis/lint-2026-07-06-parfait.md`, `docs/synthesis/lint-2026-07-22-parfait.md`
- Modify: `docs/index.md` — 「문서 지도」의 lint 보고서 두 줄. 지우지 않으면 `check_links`가 깨진다.

**Interfaces:**
- Consumes: `$S/orig/doc-baseline.md` 「기준선 이력」 표, Task 1의 `extract.tsv`(lint 행 분류가 끝나 있어야 한다).
- Produces: `docs/log.md` — 머리줄 `## [YYYY-MM-DD] (audit|lint|restructure) | <제목>`, 본문 최대 한 줄. audit 본문 필드는 `<파일 수>파일 +<삽입>/-<삭제> · 유닛 <N> · 계측 <M> · OQ 신설 <ID…|없음> · 해소 <ID…|없음>`.

- [ ] **Step 1: 변환 스크립트 작성** — `$S/to_log.py`

```python
# $S/to_log.py — 사용법: python3 $S/to_log.py $S
# 입력은 Task 1 이 떠 둔 $S/orig/doc-baseline.md. 출력 $S/audit.md, 단조성 경고는 stdout.
import re, pathlib, sys

S = pathlib.Path(sys.argv[1])
src = (S / "orig" / "doc-baseline.md").read_text(encoding="utf-8").splitlines()
start = next(i for i, l in enumerate(src) if l.startswith("## 기준선 이력"))
rows = [l for l in src[start:] if l.startswith("| 20")]

def count(label, t):
    """'유닛 A → B' 가 있으면 마지막 화살표의 B, 없으면 '유닛 N건' 중 앞에 수식어가 없는 첫 값."""
    arrows = re.findall(label + r"\s*\d+(?:건)?\s*→\s*(\d+)", t)
    if arrows:
        return arrows[-1]
    plain = re.findall(r"(?:^|[\s(,·])" + label + r"\s*(\d+)\s*건", t)
    return plain[0] if plain else "?"

def oq_ids(chunk):
    out = []
    for m in re.finditer(r"OQ-P-(\d+)(?:\s*~\s*(\d+))?", chunk):
        a, b = int(m.group(1)), int(m.group(2) or m.group(1))
        out += [f"OQ-P-{n:03d}" for n in range(a, b + 1)]
    return out

def fields(text):
    t = text.replace("**", "")
    files = re.search(r"(\d+)\s*파일[^0-9/|]{0,12}?(\d+)\s*/\s*(\d+)", t)
    f = f"{files.group(1)}파일 +{files.group(2)}/-{files.group(3)}" if files else "?파일 +?/-?"
    new = []
    for m in re.finditer(r"신설[^.;|]{0,40}", t):
        new += oq_ids(m.group(0))
    for m in re.finditer(r"(OQ-P-\d+(?:\s*~\s*\d+)?)[^.;|]{0,12}신설", t):
        new += oq_ids(m.group(1))
    solved = []
    for m in re.finditer(r"(OQ-P-\d+)([^.;|]{0,12})해소", t):
        if "부분" in m.group(2) or "잔존" in t[m.end():m.end() + 12] or "①" in m.group(2):
            continue
        solved.append(m.group(1))
    uniq = lambda xs: ", ".join(dict.fromkeys(xs)) or "없음"
    return f"{f} · 유닛 {count('유닛', t)} · 계측 {count('계측', t)} · OQ 신설 {uniq(new)} · 해소 {uniq(solved)}"

def title(summary):
    prs = list(dict.fromkeys(re.findall(r"#\d+", summary)))
    m = re.search(r"\(([^)]*)\)", summary)
    phrase = (m.group(1) if m else summary).replace("`", "")
    phrase = re.sub(r"#\d+\s*", "", phrase).strip()
    if len(phrase) > 40:
        phrase = phrase[:40].rsplit(" ", 1)[0] + "…"
    return f"{' · '.join(prs)} {phrase}".strip()

dates = [r.split("|")[1].strip() for r in rows]
k = next((i for i in range(1, len(dates)) if dates[i] > dates[i - 1]), len(rows))
ordered = sorted(list(reversed(rows[:k])) + rows[k:], key=lambda r: r.split("|")[1].strip())

out, prev = [], {"유닛": 0, "계측": 0}
for r in ordered:
    cells = [c.strip() for c in r.strip().strip("|").split("|")]
    date, commit, summary, notes = cells[0], cells[1].strip("`"), cells[2], "|".join(cells[3:])
    body = fields(summary + " " + notes)
    out += [f"## [{date}] audit | {commit} — {title(summary)}", body, ""]
    for label in ("유닛", "계측"):
        v = re.search(label + r" (\S+)", body).group(1)
        if v != "?":
            if int(v) < prev[label]:
                print(f"DECREASE {label} {commit}: {prev[label]} -> {v}")
            prev[label] = int(v)
(S / "audit.md").write_text("\n".join(out), encoding="utf-8")
print(len(rows), "rows,", sum(1 for l in out if "?" in l), "lines with ?")
```

- [ ] **Step 2: 실행**

Run: `python3 $S/to_log.py $S`
Expected: 마지막 줄 `98 rows, N lines with ?`(작성 시점 N=68). 그 앞에 `DECREASE` 줄이 0개 이상 나온다(작성 시점 `DECREASE 유닛 2285d09d: 1096 -> 6` 1건).

- [ ] **Step 3: 수작업 보정**

- `DECREASE` 행: `$S/orig/doc-baseline.md`의 해당 행을 읽고 실제 전체 유닛·계측 수로 고친다. 고친 뒤 Step 2를 다시 돌리지 않는다(스크립트가 덮어쓴다) — `$S/audit.md`를 직접 편집한다.
- `?`가 남은 줄: 원본 행에 값이 보이면 손으로 채운다. 원본에 값이 없으면(초기 회차) `?`로 둔다.
- 머리줄 제목 구절이 `…`로 잘려 뜻이 안 통하면 한 구절로 다듬는다. PR 번호는 유지한다.

확인: `grep -A1 'f37a76540' $S/audit.md` → 두 번째 줄에 `유닛 1298 · 계측 46`.

- [ ] **Step 4: `docs/log.md` 조립**

파일 구성(머리말은 H1 아래 인용문만. `## `로 시작하는 줄은 항목 머리줄만):

```markdown
# docs 작업 기록

> append-only. 오래된 것이 위, 새 것이 아래. 최근 기록: `grep "^## \[" docs/log.md | tail -5`
>
> 머리줄: `## [YYYY-MM-DD] <유형> | <제목>`. 유형은 `audit`(doc-baseline 점검 회차) · `lint` · `restructure`.
> 본문은 최대 한 줄. `audit` 제목은 `<develop 커밋> — <PR 번호와 한 구절>`, 본문은
> `<파일 수>파일 +<삽입>/-<삭제> · 유닛 <N> · 계측 <M> · OQ 신설 <ID…|없음> · 해소 <ID…|없음>`
> (값을 알 수 없으면 `?`).

<$S/audit.md 내용. lint 2건을 날짜 순 자리에 넣되, 같은 날짜 audit가 있으면 그 뒤에 둔다>

## [2026-09-26] restructure | index·doc-baseline을 status·log로 분리, lint 보고서 폐지
기록 기준을 루트 CLAUDE.md에 도입하고 doc-baseline 이력 98행을 이 파일로 옮겼다.
```

lint 항목 두 개(원문 `$S/orig/lint-*.md`의 요약 표와 조치 절 기준):

```markdown
## [2026-07-06] lint | parfait 문서 vs 코드 정합 — 7건 발견, 4건 수정
module-structure `feature/app/setting` 누락 · ADR-0002 `:api` navigation 번들 · ADR-0007 토큰 심볼명·단복수 수정. 나머지 3건은 정보성 미조치.

## [2026-07-22] lint | parfait 문서 내부 정합 — wikilink 3건 수정
<원문 조치 절을 읽고 수정·미조치 건수를 한 줄로>
```

미조치 항목은 Task 1에서 이미 추출·분류돼 있어야 한다. `grep 'orig/lint' $S/extract.tsv | cut -f1,5,6`으로 분류가 채워졌는지 확인한다.

Run: `grep '^## \[' docs/log.md | head -3; grep '^## \[' docs/log.md | tail -2; grep -o '^## \[[0-9-]*\]' docs/log.md | sort -c && echo monotonic`
Expected: 첫 줄 `2026-07-06` lint, 다음 `2026-07-15` audit. 마지막 두 줄은 `143cda87b` audit과 restructure. `monotonic`.

- [ ] **Step 5: lint 보고서 삭제 + index 두 줄 삭제**

```bash
git rm docs/synthesis/lint-2026-07-06-parfait.md docs/synthesis/lint-2026-07-22-parfait.md
```
`docs/index.md` 「문서 지도」에서 `synthesis/lint-2026-07-22-parfait.md`·`synthesis/lint-2026-07-06-parfait.md` 항목 두 줄을 Edit로 지운다.

- [ ] **Step 6: 검증**

```bash
python3 docs/script/check_links.py docs
grep -nE '^## ' docs/log.md | grep -vE '^[0-9]+:## \[[0-9]{4}-[0-9]{2}-[0-9]{2}\] (audit|lint|restructure) \| '
grep -c '^## \[.*\] audit' docs/log.md
grep -rn 'lint-2026-0' docs --include='*.md' | grep -v '/archive/\|superpowers/specs/2026-09-26-\|superpowers/plans/2026-09-26-\|docs/log.md\|synthesis/open-questions.md\|plans/2026-08-05-orchestration\|plans/2026-08-28-login-debug\|specs/README.md'
```
Expected: exit 0 / 출력 0줄 / `98` / 출력 0줄.

- [ ] **Step 7: Commit (사용자 승인 후)**

```bash
git add docs/log.md docs/index.md
git commit -m "docs: add append-only log.md and drop closed lint reports"
```

---

### Task 3: doc-baseline 축소 + 점검 절차 + 경로 안내

스펙 §3.3. 이 태스크에서 `docs/status.md` 뼈대와 「실기기 미확인」 절을 만든다 — doc-baseline이 그 절로 링크하기 때문이다.

**Files:**
- Modify: `docs/doc-baseline.md` (전면 재작성)
- Create: `docs/status.md` (머리말 + 「실기기 미확인」 절만)
- Create: `$S/pending-oq.tsv`
- Modify: `docs/api/README.md` — "Android가 바뀌었을 때 → 스킬 `sync-tjyg-develop-baseline`" 줄
- Modify: `docs/api/server-baseline.md` — `wiki/personal-private/project-paths.md` 경로 안내 줄

**Interfaces:**
- Consumes: `$S/extract.tsv` class 3(`target=device`)·class 4 행. 「현재 기준선」 값(커밋 `143cda87b`, 83회차, 유닛 1298 · 계측 46, 미머지 `feature/debug-mode` OQ-P-311 계보).
- Produces: `docs/status.md`의 H2 `## 실기기 미확인`(Task 5가 그 앞에 영역 절을 넣는다). `$S/pending-oq.tsv` — 탭 구분 3열 `id	src	text`, OQ 없이 「실기기 미확인」으로 가야 하는 항목(Task 5 Step 4가 OQ를 신설해 채운다).

- [ ] **Step 1: `docs/status.md` 뼈대**

```markdown
# 기능별 현재 상태

> 현재형으로 쓰고, 바뀌면 덮어쓴다. 이력은 [`log.md`](log.md), 결정 이유는 `adr/`, 설계는 스펙.
> 영역 형식과 규칙: [기록 구조 설계](superpowers/specs/2026-09-26-docs-record-structure-design.md) §3.1.
> 코드 대조 기준: 작업 브랜치 HEAD `<git rev-parse --short HEAD 값>`.

## 실기기 미확인

코드로 판정할 수 없고 실기기·실서버에서 한 번도 확인되지 않은 항목.

- OQ-P-NNN — <한 구절>
```

목록은 추출표 class 3 중 `target=device`이고 `note`에 OQ ID가 있는 행으로 채운다. `note`가 빈 행은 `$S/pending-oq.tsv`에 모은다.

- [ ] **Step 2: doc-baseline 재작성**

전체를 아래 구조로 다시 쓴다. 머리말 인용문 두 개(역할, "기준선이 두 개")는 `$S/orig/doc-baseline.md` 원문 그대로 둔다.

```markdown
# 문서-코드 검증 기준선 (Doc Baseline)

<머리말 인용문 두 개 — 원문 유지>

## 현재 기준선
- **repo**: `TJYG-Android` (`mash-up-kr/TEAMYG-Android`) `develop`
- **커밋**: `143cda87b` (`Merge pull request #511 from mash-up-kr/feature/ai/llm-wiki-document`)
- **검증일**: 2026-09-21 (83회차)
- **테스트 수**: 유닛 1298 · 계측 46
- **미머지 추적 항목**: 하나(`feature/debug-mode`, OQ-P-311 계보)
- **실기기 미확인 이월**: [status.md 「실기기 미확인」](status.md#실기기-미확인)

## 점검 절차 (다음 요청 시)

1. **최신화**: `git fetch origin develop`
2. **신규 머지 나열**: `git log --oneline --merges <기준선>..origin/develop`
   - 각 머지가 건드린 컴포넌트·모듈: `git show --stat <merge-hash>`
3. **문서 대조**: 변경된 심볼이 문서와 어긋나는지 검사한다.
   - 관련 spec/plan `status`·`related_code`, `architecture/*` 인벤토리, `synthesis/open-questions.md` 미머지 항목.
   - 드리프트 발견 → 해당 문서 수정 + [`status.md`](status.md) 해당 영역 **덮어쓰기**. `index.md`에는 쓰지 않는다.
   - 구현 완료분: spec → `implemented`·`specs/archive/`, plan → `done`·`plans/archive/`.
4. **기록**: 위 「현재 기준선」 필드를 교체하고 [`log.md`](log.md)에 `audit` 항목 하나를 추가한다(형식은 그 파일 머리말 인용문).
5. **미머지 재확인**: `git ls-tree -r --name-only origin/develop | grep <심볼>`.

### 판정 규칙
<추출표 class 4 행을 중복 없이 규칙 문장으로. 각 한두 줄. 예:>
- 회차 번호는 직전 회차 번호 + 1이다. 같은 날 여러 회차가 돌 수 있어 날짜로 구분하지 않는다.
- 스펙이 부분 머지된 라운드는 아카이브하지 않는다 — ① 스펙 `status`를 `in-progress`로 ② 본문에서 이번에 고쳐진 자리만 표시 ③ 계획의 해당 체크박스를 닫는다. 남은 조건부 서술은 근거로 둔다.

> 드리프트는 대개 문서 검증일 이후 머지된 PR에서 생긴다. merge 날짜와 문서 `verified` 날짜를 비교하면 후보를 빨리 좁힐 수 있다.
```

- [ ] **Step 3: `docs/api/README.md` 수정**

"Android가 바뀌었을 때 → 스킬 `sync-tjyg-develop-baseline`(…)" 줄을 아래로 바꾼다.

```markdown
- **Android가 바뀌었을 때** → [doc-baseline](../doc-baseline.md) 「점검 절차」를 따르고, 계약에 닿는 변경은 각 문서의 `android_status`·「Android 매핑」 절을 갱신한다.
```

- [ ] **Step 4: `docs/api/server-baseline.md` 수정**

`로컬 경로는 개인정보라 \`wiki/personal-private/project-paths.md\` 참고(아래 \`<S>\`).` 줄을 아래로 바꾼다.

```markdown
`<S>`는 서버 저장소(`mash-up-kr/TEAMYG-SERVER`)의 로컬 클론 경로다. 이 저장소에는 경로를 적지 않는다.
```

- [ ] **Step 5: 줄 수·옛 참조 검사**

```bash
wc -l docs/doc-baseline.md
grep -rn 'personal-private\|sync-tjyg-develop-baseline' docs --include='*.md' | grep -v '/archive/\|superpowers/specs/2026-09-26-\|superpowers/plans/2026-09-26-\|docs/log.md\|synthesis/open-questions.md\|plans/2026-08-05-orchestration\|plans/2026-08-28-login-debug\|specs/README.md'
python3 docs/script/check_links.py docs
```
Expected: ≤ 80 / 출력 0줄 / exit 0.

- [ ] **Step 6: 절 제목·참조 확인 (Review Focus 5)**

```bash
grep -n '^## 현재 기준선$' docs/doc-baseline.md
grep -n '^## 실기기 미확인$' docs/status.md
grep -n 'doc-baseline.*현재 기준선' docs/synthesis/open-questions.md
```
Expected: 두 제목이 각각 1줄. 세 번째 명령이 해소 메모 두 곳을 보여 주고, 둘 다 여전히 존재하는 「현재 기준선」 절을 가리킨다.

- [ ] **Step 7: Commit (사용자 승인 후)**

```bash
git add docs/doc-baseline.md docs/status.md docs/api/README.md docs/api/server-baseline.md
git commit -m "docs: slim doc-baseline to current baseline and procedure"
```

---

### Task 4: 루트 `CLAUDE.md` 기록 기준

**Files:**
- Modify: `CLAUDE.md` 「문서 (`docs/`)」 절

- [ ] **Step 1: 경로 표 수정**

- `| \`docs/synthesis/\` | 미결 항목·린트 로그 |` → `| \`docs/synthesis/\` | 미결 항목 |`
- `docs/index.md` 행 아래에 두 행 추가:

```markdown
| `docs/status.md` | 기능별 현재 상태. 덮어쓰기만 한다 |
| `docs/log.md` | 점검·lint·구조 변경의 시간순 기록. append-only |
```

- [ ] **Step 2: 기록 기준 추가**

경로 표 바로 아래에 `### 무엇을 적나` 소제목을 두고, 아래 순서로 넣는다.
1. 한 줄: "코드로 복원할 수 없는 것만 적는다. 수정 작업에서 에이전트는 문서를 읽고도 코드를 직접 열기 때문에, 코드에서 바로 나오는 사실은 중복이다."
2. 스펙 §2의 표를 **문구 그대로** 복사한다.
3. 한 줄: "미결은 `docs/synthesis/open-questions.md` 한 곳에서만 추적한다. lint·점검 결과는 별도 보고서를 만들지 않는다 — 고친 것은 `log.md` 한 줄, 못 고친 것은 open-questions 항목이다."

- [ ] **Step 3: 검증**

```bash
python3 docs/script/check_links.py docs
grep -c 'docs/status.md' CLAUDE.md
grep -c 'docs/log.md' CLAUDE.md
grep -c '^### 무엇을 적나' CLAUDE.md
```
Expected: exit 0 / 세 값 모두 1 이상.

- [ ] **Step 4: Commit (사용자 승인 후)**

```bash
git add CLAUDE.md
git commit -m "docs: add recording criteria to root CLAUDE.md"
```

### 체크포인트 A

```bash
python3 docs/script/check_links.py docs
python3 -m unittest discover -s docs/script -p 'test_*.py'
grep -nE '^## ' docs/log.md | grep -vE '^[0-9]+:## \[[0-9]{4}-[0-9]{2}-[0-9]{2}\] (audit|lint|restructure) \| '
grep -rn 'lint-2026-0\|personal-private\|sync-tjyg-develop-baseline' docs --include='*.md' | grep -v '/archive/\|superpowers/specs/2026-09-26-\|superpowers/plans/2026-09-26-\|docs/log.md\|synthesis/open-questions.md\|plans/2026-08-05-orchestration\|plans/2026-08-28-login-debug\|specs/README.md'
```
Expected: exit 0 / `OK` / 출력 0줄 / 출력 0줄. 사용자에게 A단계 결과(로그 건수, doc-baseline 줄 수, 판정 규칙 목록)를 보이고 B단계 진행 확인을 받는다.

---

## B단계 — 판정이 필요한 작업

### Task 5: `status.md` 영역 작성 + open-questions 신설·보강

스펙 §3.1·§5.

**Files:**
- Modify: `docs/status.md` — 「실기기 미확인」 절 **앞에** 영역 절 15개 추가, pending 항목 채움
- Modify: `docs/synthesis/open-questions.md` — 신설 항목 추가, 기존 항목 보강, `<!-- oq-next: N -->` 갱신

**Interfaces:**
- Consumes: `$S/extract.tsv` class 1·2·3 행, `$S/pending-oq.tsv`(Task 3), `$S/orig/index.md` 「지금 상태」 원문, `$S/orig/doc-baseline.md`.
- Produces: `docs/status.md` 영역 절. 각 절은 스펙 §3.1 형식의 네 칸.

- [ ] **Step 1: 영역 묶음 나누기**

서브에이전트 5개가 각각 세 영역을 맡는다(스펙 §5가 허용하는 묶음).
- A: 1 앱 진입·인증 / 4 그룹 설정 / 5 앱 설정
- B: 2 그룹 목록 / 3 그룹 생성·참여 / 13 튜토리얼
- C: 6 캔버스 메인 / 9 캔버스 편집 / 11 캔버스 이미지 저장
- D: 7 토핑 생성·배치 / 8 누끼 추출 / 10 달력·지난 캔버스
- E: 12 푸시·딥링크 / 14 공통 기반 / 15 서버 계약

- [ ] **Step 2: 서브에이전트 브리프 (5개 병렬, 읽기 전용)**

각 브리프에 넣을 것:
- 스펙 §2·§3.1 원문과 [`docs/code-conventions.md`](../../code-conventions.md) 링크
- 담당 영역 번호·이름
- 입력:
  - `$S/extract.tsv`에서 `target`이 담당 영역인 class 1·2 행
  - `$S/orig/index.md` 「지금 상태」와 `$S/orig/doc-baseline.md`에서 담당 영역 서술(영역 이름·화면 ID로 찾는다)
  - 관련 스펙(`docs/superpowers/specs/README.md` 표와 `archive/`에서 화면 ID로 찾는다)
  - 열린 OQ 목록: `docs/synthesis/open-questions.md`에서 담당 영역 화면 ID·모듈명이 나오는 항목 중 상태가 `해소됨`/`**해소됨**`으로 시작하지 않는 것
- 할 일:
  1. 작업 브랜치 HEAD 워킹 트리의 코드(`feature/`·`core/`·`data/`·`domain/`·`app/`)를 읽고 영역의 현재 상태를 한두 문장으로 쓴다. 문서끼리 모순되면 코드가 정답이다.
  2. 앵커 3~6개. 백틱 하나에 단일 식별자 또는 저장소 상대 경로 하나. 한정 이름(`.`), 인자·제네릭 표기(`(` `<`), OQ ID, 값 금지. 흔한 단어(`Canvas`, `Route`) 대신 그 영역에만 있는 이름.
  3. class 1 행 → 코드로 아직 유효하면 ⚠️ 줄(OQ ID 포함), 아니면 버림 + 사유.
  4. class 2 행 → 유효하면 "OQ 신설 필요"로 표시하고 제목·출처·항목·해소 조건 초안을 붙인다. 아니면 버림 + 사유.
  5. 설계 줄: 관련 스펙·ADR 링크(`docs/status.md` 기준 상대 경로). **링크 텍스트는 날짜 없는 스펙 id나 `ADR-NNNN`**으로 쓴다(금지 형식 검사가 링크 텍스트의 날짜를 잡는다).
- 금지: 파일 수정. 링크 대상 경로 밖의 PR 번호·날짜·취소선·"→ ✅".
- 산출: 영역별 마크다운 초안 + OQ 신설 초안 목록 + 처리한 추출표 id별 결과(`id → ⚠️ OQ-P-NNN | 신설 | 버림: 사유`).

- [ ] **Step 3: 기존 OQ 보강 (class 3 `target=OQ-P-NNN`)**

추출표 class 3 중 `target`이 OQ ID인 행마다 `docs/synthesis/open-questions.md`의 그 항목을 읽는다.
- 항목에 같은 사실이 이미 있으면 추출표 행을 class 5 `note=OQ에 이미 있음`으로 바꾼다.
- 없으면 항목의 `- **해소 메모**:` 줄 바로 앞에 한 줄을 넣는다:

```markdown
- 📌 **2026-09-26 이관 메모**: <요지> (원문 `orig/<파일>:<줄>`, docs 기록 구조 개선에서 옮김)
```

- [ ] **Step 4: OQ 신설**

서브에이전트의 신설 초안과 `$S/pending-oq.tsv`를 합쳐 `docs/synthesis/open-questions.md`의 `<!-- oq-next: N -->` 주석 바로 위에 항목을 추가한다. 번호는 현재 `oq-next`(작성 시점 407)부터 차례로 매기고, 주석 값을 마지막 번호 + 1로 고친다. 형식은 기존 최근 항목과 같다:

```markdown
### [2026-09-26] <제목>

- **ID**: OQ-P-407
- **출처**: docs 기록 구조 개선 추출 — 원문 `orig/<파일>:<줄>`(`<원본 경로>`의 이관 전 판)
- **항목**: <내용>
- **상태**: 미해결
- **해소 메모**: <해소 조건>
```

신설한 ID를 초안의 ⚠️ 줄과 `status.md` 「실기기 미확인」(pending 항목)에 채운다.

- [ ] **Step 5: `status.md` 조립**

영역 절 15개를 스펙 §3.1 순서로 「실기기 미확인」 앞에 넣는다. 머리말의 기준 커밋 자리에 `git rev-parse --short HEAD` 값을 적는다.

- [ ] **Step 6: 앵커 검사 (Review Focus 3 포함)**

```bash
grep '^- 앵커:' docs/status.md | grep -o '`[^`]*`' | tr -d '`' | while read -r s; do
  case "$s" in
    */*) git ls-files --error-unmatch -- "$s" >/dev/null 2>&1 || [ -d "$s" ] || echo "MISSING path: $s" ;;
    *.*|*\(*|*\<*|OQ-*) echo "FORBIDDEN form: $s" ;;
    *)   git grep -qwF -- "$s" -- ':!docs' ':!wiki' || echo "MISSING symbol: $s" ;;
  esac
done
grep '^- 앵커:' docs/status.md | grep -o '`[A-Z][A-Za-z0-9]*`' | tr -d '`' | sort -u | while read -r s; do
  git grep -qE "(class|object|interface|fun|val|typealias)[[:space:]]+$s([^A-Za-z0-9_]|\$)" -- '*.kt' || echo "NO DECL: $s"
done
```
Expected: 출력 0줄. `/`가 든 앵커는 경로로 먼저 분기하므로 파일 경로의 `.`은 금지 형식에 걸리지 않는다. `NO DECL`이 나오면 선언이 없는 흔한 단어인지 확인하고 영역 고유 이름으로 바꾼다(enum 항목처럼 선언 키워드가 앞에 없는 정당한 앵커면 사용자에게 보이고 둔다).

- [ ] **Step 7: OQ 상태 검사 (Review Focus 4)**

```bash
grep '^- ⚠️' docs/status.md | grep -v 'OQ-P-[0-9]'
grep -o 'OQ-P-[0-9]\+' docs/status.md | sort -u | while read -r id; do
  awk -v id="$id" '
    $0 ~ "^- \\*\\*ID\\*\\*: " id "$" {on=1; found=1; next}
    on && /^### / {exit}
    on && /^- \*\*상태\*\*: (\*\*)?해소됨/ {print "CLOSED " id; exit}
    END {if (!found) print "NO ENTRY " id}
  ' docs/synthesis/open-questions.md
done
```
Expected: 첫 명령 출력 0줄(OQ 없는 ⚠️ 없음). `NO ENTRY` 0줄. `CLOSED`가 나오면 해소 메모에 잔존 조건이 있는지 읽고, 없으면 ⚠️ 줄을 지운다.

- [ ] **Step 8: 금지 형식·링크 검사**

```bash
sed -E 's/\]\([^)]*\)/]/g' docs/status.md | grep -nE '#[0-9]+|~~|→ ✅|20[0-9]{2}-[0-9]{2}-[0-9]{2}'
python3 docs/script/check_links.py docs
```
Expected: 출력 0줄 / exit 0.

- [ ] **Step 9: 손실 방지 검사**

추출표의 class 1·2·3·4 행마다 도착이 확인되는지 대조한다.
- class 1·2: 서브에이전트의 id별 결과에 있고, ⚠️ 줄이면 그 OQ ID가 `status.md`에 있다.
- class 3: `target=device`면 OQ ID가 `status.md` 「실기기 미확인」에 있다. `target=OQ-P-NNN`이면 Step 3의 📌 줄이 그 항목에 있거나 class 5로 바뀌었다.
- class 4: Task 3의 「판정 규칙」에 해당 규칙이 있다.

도착이 확인되지 않은 행 id 목록이 비어야 한다. 사용자에게 버린 주장 목록을 요약해 보인다.

- [ ] **Step 10: Commit (사용자 승인 후)**

```bash
git add docs/status.md docs/synthesis/open-questions.md
git commit -m "docs: add feature-axis status.md verified against the code"
```

---

### Task 6: index 축소

스펙 §3.4. Task 1이 index 전체를 추출해 두었으므로 이 태스크의 삭제로 잃는 미결은 없다.

**Files:**
- Modify: `docs/index.md`

- [ ] **Step 1: 「지금 상태」 교체**

절 제목 `## 지금 상태 (1줄)`을 `## 지금 상태`로 바꾸고 본문을 아래로 교체한다(기존 284줄 삭제).

```markdown
## 지금 상태
Android 단일 플랫폼, Jetpack Compose + Navigation3. 다중 모듈(core/data/domain/feature)·컨벤션
플러그인·Hilt·자체 MVI 기반이고, 서버 연동은 `api/` 계약을 따른다.
기능별 현재 상태와 알려진 결함은 [`status.md`](status.md)에 있다.
```

- [ ] **Step 2: 「무엇을 찾는가」 정리**

- 셀 안의 이력 서술을 지운다. 대상: 날짜, PR 번호, "철회 → 되살림" 같은 경과, OQ 해소 경과, ⚠️ 경고(Task 1에서 추출됐고 Task 5가 `status.md`로 옮겼다). 예: Crashlytics 행은 `| Crashlytics·Analytics·Firebase 설정·푸시(FCM) | [ADR-0013](adr/0013-firebase-fcm-crashlytics.md) + [ADR-0031](adr/0031-analytics-central-screen-mapping.md) |`.
- 표 맨 위에 두 행을 추가한다.

```markdown
| 기능별 현재 상태·알려진 결함 | [status.md](status.md) |
| 언제 무엇을 점검했나 | [log.md](log.md) |
```

- [ ] **Step 3: 「문서 지도」 정리**

- `synthesis/` 설명 "분석·점검 산출물(open-questions·lint)" → "미결 추적(open-questions)".
- doc-baseline 항목을 한 줄로 줄이고, `status.md`·`log.md` 항목 두 개를 추가한다:

```markdown
- **[`doc-baseline.md`](doc-baseline.md)** — 문서를 마지막으로 검증한 `develop` 커밋과 점검 절차.
- **[`status.md`](status.md)** — 기능별 현재 상태. 덮어쓰기만 한다.
- **[`log.md`](log.md)** — 점검·lint·구조 변경의 시간순 기록. append-only.
```

- [ ] **Step 4: 검증**

```bash
wc -l docs/index.md
python3 docs/script/check_links.py docs
grep -nE '#[0-9]{3}|~~|⚠️' docs/index.md
```
Expected: ≤ 100 / exit 0 / 출력 0줄(규율 절 문구가 걸리면 사용자에게 보인다).

- [ ] **Step 5: Commit (사용자 승인 후)**

```bash
git add docs/index.md
git commit -m "docs: slim index.md to routing"
```

---

### Task 7: README 등록 + 마무리 검증

**Files:**
- Modify: `docs/superpowers/specs/README.md`, `docs/superpowers/plans/README.md` — 이번 스펙·계획 행 추가(스펙 §3 표)

- [ ] **Step 1: README 등록**

두 README의 기존 표 형식을 따라 행을 하나씩 추가한다. 상태는 `in-progress` — 아카이브 이동은 머지 뒤 doc-baseline 점검이 한다.

- [ ] **Step 2: 스펙 §7 전체 실행**

스펙 §7 코드블록의 명령과 이 계획의 Task 5 Step 6·7 검사를 전부 실행한다.
Expected: 링크 exit 0, 테스트 OK, 줄 수 index ≤ 100·doc-baseline ≤ 80, 앵커·금지 형식·로그 형식·옛 참조·OQ 상태 출력 0줄.

- [ ] **Step 3: 사용자 보고**

변경 파일 목록, 줄 수 전후(index 355 → N, doc-baseline 2636 → N), 신설·보강 OQ 목록, 버린 주장 요약을 보고한다.

- [ ] **Step 4: Commit (사용자 승인 후)**

```bash
git add docs/superpowers/specs/README.md docs/superpowers/plans/README.md
git commit -m "docs: register record-structure spec and plan"
```
