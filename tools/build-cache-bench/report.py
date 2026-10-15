#!/usr/bin/env python3
"""측정 결과 CSV 를 사람이 읽는 report.html 한 장으로 조립한다.

사용법: report.py <run 디렉토리>

외부 라이브러리를 쓰지 않는다. 차트는 인라인 SVG 라 파일 하나만 열면 된다.
"""
import csv
import html
import os
import sys
from collections import OrderedDict

SCENARIO_ORDER = ["S0", "S1", "S2", "S3", "S4"]

SCENARIO_LABEL = {
    "S0": ("S0", "무변경 재실행", "하한선"),
    "S1": ("S1", "출력만 삭제", "로컬 캐시 최상 조건"),
    "S2": ("S2", "출력 + 캐시 비움", "캐시 축출 직후"),
    "S3": ("S3", "캐시에 A만", "T_local — 리모트 캐시 없음"),
    "S4": ("S4", "캐시에 A와 B", "T_remote — CI 가 구운 항목을 받음"),
}

# 스택 순서 = 팔레트 슬롯 1·3·2. 인접 쌍(blue↔aqua, aqua↔orange)으로 검증했다.
OUTCOME_ORDER = ["from_cache", "up_to_date", "executed"]
OUTCOME_LABEL = {
    "from_cache": "캐시 적중",
    "up_to_date": "up-to-date",
    "executed": "실행",
}


def read_csv(path):
    if not os.path.exists(path):
        return []
    with open(path, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def median(values):
    if not values:
        return None
    s = sorted(values)
    return s[(len(s) - 1) // 2]


def shorten_path(text, run_dir):
    """사유 문자열의 긴 절대경로를 읽을 수 있게 줄인다. 원문은 펼침에 남는다."""
    tree = os.path.join(run_dir, "tree") + os.sep
    text = text.replace(tree, "")
    home = os.path.expanduser("~")
    return text.replace(home + os.sep, "~/")


def collect(run_dir):
    builds = read_csv(os.path.join(run_dir, "builds.csv"))
    sizes = read_csv(os.path.join(run_dir, "cache-size.csv"))

    targets = list(OrderedDict.fromkeys(r["target"] for r in builds))
    pair = builds[0]["pair"] if builds else "none"
    iterations = len({r["iteration"] for r in builds}) or 1

    wall = {}
    for r in builds:
        wall.setdefault((r["scenario"], r["target"]), []).append(int(r["wall_ms"]))
    wall = {k: median(v) for k, v in wall.items()}

    # 태스크 CSV 는 빌드마다 한 파일이다. 시나리오·그래프별로 전 회차를 합친다.
    tasks_dir = os.path.join(run_dir, "tasks")
    outcomes, misses = {}, {}
    if os.path.isdir(tasks_dir):
        for name in sorted(os.listdir(tasks_dir)):
            if not name.endswith(".csv"):
                continue
            scenario = name.split("-", 1)[0]
            slug = name[len(scenario) + 1: name.rfind("-")]
            target = next((t for t in targets if t.replace(":", "_") == slug), slug)
            for row in read_csv(os.path.join(tasks_dir, name)):
                path, outcome = row["task_path"], row["outcome"]
                if path.startswith(":build-logic:"):
                    continue
                if outcome in ("SKIPPED", "NO-SOURCE"):
                    continue
                key = (scenario, target)
                bucket = outcomes.setdefault(key, {k: 0 for k in OUTCOME_ORDER})
                if outcome == "FROM_CACHE":
                    bucket["from_cache"] += 1
                elif outcome == "UP_TO_DATE":
                    bucket["up_to_date"] += 1
                else:
                    bucket["executed"] += 1
                    if scenario == "S3":
                        agg = misses.setdefault(target, {})
                        prev = agg.get(path, [0, ""])
                        prev[0] += int(row["duration_ms"])
                        prev[1] = row.get("execution_reasons", "") or prev[1]
                        agg[path] = prev

    size = {}
    for r in sizes:
        size.setdefault((r["scenario"], r["target"]), []).append(r)
    return {
        "targets": targets, "pair": pair, "iterations": iterations,
        "wall": wall, "outcomes": outcomes, "misses": misses, "size": size,
    }


CSS = """
:root {
  color-scheme: light;
  --surface-0: #f4f4f2; --surface-1: #fcfcfb; --border: #dcdbd5;
  --text-primary: #0b0b0b; --text-secondary: #52514e; --text-muted: #77756e;
  --from-cache: #2a78d6; --up-to-date: #1baf7a; --executed: #eb6834;
  --bar: #6b6a64; --bar-key: #2a78d6;
}
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    color-scheme: dark;
    --surface-0: #111110; --surface-1: #1a1a19; --border: #35342f;
    --text-primary: #ffffff; --text-secondary: #c3c2b7; --text-muted: #94938a;
    --from-cache: #3987e5; --up-to-date: #199e70; --executed: #d95926;
    --bar: #8a897f; --bar-key: #3987e5;
  }
}
* { box-sizing: border-box; }
body {
  margin: 0; padding: 32px 20px 64px;
  background: var(--surface-0); color: var(--text-primary);
  font: 14px/1.6 -apple-system, BlinkMacSystemFont, "Helvetica Neue", "Apple SD Gothic Neo", sans-serif;
}
main { max-width: 980px; margin: 0 auto; }
h1 { font-size: 22px; margin: 0 0 4px; letter-spacing: -0.01em; }
h2 { font-size: 15px; margin: 40px 0 12px; letter-spacing: -0.005em; }
h3 { font-size: 13px; margin: 24px 0 8px; color: var(--text-secondary); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-weight: 600; }
.meta { color: var(--text-muted); font-size: 12.5px; margin: 0 0 8px; }
.meta code { font-size: 12px; word-break: break-all; }
.note { color: var(--text-secondary); font-size: 13px; margin: 0 0 16px; }
.card {
  background: var(--surface-1); border: 1px solid var(--border);
  border-radius: 10px; padding: 18px 20px; margin-bottom: 12px;
}
.headline { display: flex; flex-wrap: wrap; align-items: baseline; gap: 10px 18px; }
.headline .target { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 13px; color: var(--text-secondary); flex-basis: 100%; }
.big { font-size: 34px; font-weight: 650; letter-spacing: -0.02em; font-variant-numeric: tabular-nums; }
.pct { font-size: 15px; color: var(--text-secondary); font-variant-numeric: tabular-nums; }
.flow { font-size: 13px; color: var(--text-secondary); font-variant-numeric: tabular-nums; }
table { border-collapse: collapse; width: 100%; font-size: 13px; }
th, td { text-align: left; padding: 7px 10px; border-bottom: 1px solid var(--border); }
th { color: var(--text-muted); font-weight: 500; font-size: 12px; }
td.num, th.num { text-align: right; font-variant-numeric: tabular-nums; }
td.task { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
.legend { display: flex; gap: 16px; flex-wrap: wrap; margin: 0 0 12px; font-size: 12.5px; color: var(--text-secondary); }
.swatch { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 5px; vertical-align: -1px; }
details { margin-top: 6px; }
summary { cursor: pointer; color: var(--text-muted); font-size: 12px; }
.reason { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11.5px; color: var(--text-secondary); white-space: pre-wrap; word-break: break-all; margin-top: 6px; }
ul.limits { margin: 0; padding-left: 18px; color: var(--text-secondary); font-size: 13px; }
ul.limits li { margin-bottom: 6px; }
svg { display: block; overflow: visible; }
.scroll { overflow-x: auto; }
"""


def esc(s):
    return html.escape(str(s), quote=True)


def bar_chart(rows, key_scenarios):
    """시나리오별 소요 시간 가로 막대. rows = [(라벨, 설명, ms)]"""
    if not rows:
        return ""
    top = max(r[2] for r in rows) or 1
    row_h, gap, label_w, track_w = 30, 8, 190, 560
    height = len(rows) * (row_h + gap)
    out = [f'<svg viewBox="0 0 {label_w + track_w + 90} {height}" role="img">']
    for i, (label, desc, ms) in enumerate(rows):
        y = i * (row_h + gap)
        w = max(3, round(track_w * ms / top))
        fill = "var(--bar-key)" if label in key_scenarios else "var(--bar)"
        out.append(
            f'<text x="0" y="{y + 13}" font-size="12" font-weight="600" fill="var(--text-primary)">{esc(label)}</text>'
            f'<text x="0" y="{y + 27}" font-size="10.5" fill="var(--text-muted)">{esc(desc)}</text>'
            f'<rect x="{label_w}" y="{y + 5}" width="{w}" height="{row_h - 10}" rx="4" fill="{fill}"/>'
            f'<text x="{label_w + w + 8}" y="{y + row_h / 2 + 4}" font-size="12" '
            f'fill="var(--text-secondary)" style="font-variant-numeric:tabular-nums">{ms:,} ms</text>'
        )
    out.append("</svg>")
    return "".join(out)


def stack_chart(rows):
    """적중률 스택 막대. rows = [(시나리오, {outcome: n})]"""
    if not rows:
        return ""
    total = max(sum(c.values()) for _, c in rows) or 1
    row_h, gap, label_w, track_w = 26, 8, 40, 620
    height = len(rows) * (row_h + gap)
    out = [f'<svg viewBox="0 0 {label_w + track_w + 60} {height}" role="img">']
    for i, (scenario, counts) in enumerate(rows):
        y = i * (row_h + gap)
        x = label_w
        out.append(f'<text x="0" y="{y + row_h / 2 + 4}" font-size="12" font-weight="600" '
                   f'fill="var(--text-primary)">{esc(scenario)}</text>')
        for outcome in OUTCOME_ORDER:
            n = counts.get(outcome, 0)
            if not n:
                continue
            w = max(2, round(track_w * n / total))
            out.append(
                f'<rect x="{x}" y="{y}" width="{w}" height="{row_h}" rx="3" '
                f'fill="var(--{outcome.replace("_", "-")})"/>'
            )
            if w >= 34:
                out.append(f'<text x="{x + w / 2}" y="{y + row_h / 2 + 4}" font-size="11" '
                           f'text-anchor="middle" fill="#fff" '
                           f'style="font-variant-numeric:tabular-nums">{n}</text>')
            x += w + 2  # 인접 채움 사이 2px 표면 간격
        out.append(f'<text x="{x + 8}" y="{y + row_h / 2 + 4}" font-size="11.5" '
                   f'fill="var(--text-muted)" style="font-variant-numeric:tabular-nums">'
                   f'{sum(counts.values())}</text>')
    out.append("</svg>")
    return "".join(out)


def render(run_dir, d):
    t = []
    t.append("<!doctype html><html lang=\"ko\"><head><meta charset=\"utf-8\">")
    t.append('<meta name="viewport" content="width=device-width, initial-scale=1">')
    t.append("<title>빌드 캐시 측정 결과</title>")
    t.append(f"<style>{CSS}</style></head><body><main>")

    t.append("<h1>빌드 캐시 측정 결과</h1>")
    t.append(f'<p class="meta">커밋 쌍 <code>{esc(d["pair"])}</code> · 반복 {d["iterations"]}회'
             f'{" (중앙값)" if d["iterations"] > 1 else ""} · <code>{esc(os.path.basename(run_dir.rstrip("/")))}</code></p>')

    # 1. 핵심 값
    t.append("<h2>핵심 값 — 리모트 캐시가 사 오는 몫</h2>")
    t.append('<p class="note">커밋 <code>A</code>까지 빌드해 둔 개발자가 <code>B</code>를 당겨받아 빌드할 때, '
             '캐시에 <code>A</code>만 있는 경우(<code>T_local</code>)와 CI가 구운 <code>B</code>까지 있는 '
             '경우(<code>T_remote</code>)의 차이다.</p>')
    any_key = False
    for target in d["targets"]:
        s3, s4 = d["wall"].get(("S3", target)), d["wall"].get(("S4", target))
        if s3 is None or s4 is None:
            continue
        any_key = True
        diff, pct = s3 - s4, (s3 - s4) / s3 * 100 if s3 else 0
        ex3 = d["outcomes"].get(("S3", target), {}).get("executed", 0)
        ex4 = d["outcomes"].get(("S4", target), {}).get("executed", 0)
        t.append('<div class="card"><div class="headline">'
                 f'<span class="target">{esc(target)}</span>'
                 f'<span class="big">{diff:,} ms</span>'
                 f'<span class="pct">절감 {pct:.0f}%</span>'
                 f'<span class="flow">{s3:,} ms → {s4:,} ms · 실행 태스크 {ex3} → {ex4}</span>'
                 "</div></div>")
    if not any_key:
        t.append('<div class="card"><p class="note" style="margin:0">S3·S4 를 돌리지 않아 핵심 값이 없다. '
                 '<code>--scenarios</code> 에 둘을 넣고 <code>--pair</code> 를 주면 나온다.</p></div>')

    # 2. 시나리오별 소요 시간
    t.append("<h2>시나리오별 소요 시간</h2>")
    t.append('<p class="note">파란 막대가 핵심 값을 이루는 두 시나리오다. '
             '<code>S4</code>가 <code>S1</code> 쪽에, <code>S3</code>가 <code>S2</code> 쪽에 붙을수록 '
             '리모트 캐시가 사 오는 몫이 크다.</p>')
    for target in d["targets"]:
        rows = []
        for s in SCENARIO_ORDER:
            ms = d["wall"].get((s, target))
            if ms is None:
                continue
            _, what, why = SCENARIO_LABEL[s]
            rows.append((f"{s} · {what}", why, ms))
        if not rows:
            continue
        t.append(f"<h3>{esc(target)}</h3>")
        t.append(f'<div class="card scroll">{bar_chart(rows, {"S3 · 캐시에 A만", "S4 · 캐시에 A와 B"})}</div>')

    # 3. 태스크 결과 구성
    t.append("<h2>태스크 결과 구성</h2>")
    t.append('<p class="note">included build(<code>:build-logic:</code>)와 <code>SKIPPED</code>·'
             '<code>NO-SOURCE</code>를 뺀 actionable 태스크다. '
             '<code>S3</code>가 사실상 전부 up-to-date 면 캐시 효과가 없는 것이 아니라 '
             '<strong>커밋 쌍이 무신호</strong>라는 뜻이다.</p>')
    t.append('<p class="legend">'
             + "".join(f'<span><span class="swatch" style="background:var(--{o.replace("_", "-")})"></span>'
                       f'{OUTCOME_LABEL[o]}</span>' for o in OUTCOME_ORDER)
             + "</p>")
    for target in d["targets"]:
        rows = [(s, d["outcomes"][(s, target)]) for s in SCENARIO_ORDER if (s, target) in d["outcomes"]]
        if not rows:
            continue
        t.append(f"<h3>{esc(target)}</h3>")
        t.append(f'<div class="card scroll">{stack_chart(rows)}</div>')
        t.append('<div class="card scroll"><table><thead><tr><th>시나리오</th>'
                 + "".join(f'<th class="num">{OUTCOME_LABEL[o]}</th>' for o in OUTCOME_ORDER)
                 + '<th class="num">actionable</th><th class="num">적중률</th></tr></thead><tbody>')
        for s, c in rows:
            total = sum(c.values())
            rate = c["from_cache"] / total * 100 if total else 0
            t.append(f"<tr><td>{esc(s)}</td>"
                     + "".join(f'<td class="num">{c[o]}</td>' for o in OUTCOME_ORDER)
                     + f'<td class="num">{total}</td><td class="num">{rate:.1f}%</td></tr>')
        t.append("</tbody></table></div>")

    # 4. S3 미스 태스크
    if d["misses"]:
        t.append("<h2>리모트 캐시가 메울 태스크</h2>")
        t.append('<p class="note"><code>S3</code>에서 실행된 태스크다. 이것들이 <code>S4</code>에서 '
                 '사라진 몫이고, 시간은 이 실행에서 쓴 <strong>합</strong>이지 1회 빌드 시간이 아니다. '
                 '그래프당 상위 30개만 싣는다.</p>')
        for target in d["targets"]:
            agg = d["misses"].get(target)
            if not agg:
                continue
            t.append(f"<h3>{esc(target)}</h3>")
            t.append('<div class="card scroll"><table><thead><tr><th class="num">ms</th>'
                     "<th>태스크</th><th>왜 실행됐나</th></tr></thead><tbody>")
            for path, (ms, reason) in sorted(agg.items(), key=lambda kv: -kv[1][0])[:30]:
                short = shorten_path(reason, run_dir)
                head = short.split(";")[0][:110]
                t.append(f'<tr><td class="num">{ms:,}</td><td class="task">{esc(path)}</td><td>'
                         f'<div style="color:var(--text-secondary);font-size:12px">{esc(head)}</div>'
                         f"<details><summary>사유 전문</summary>"
                         f'<div class="reason">{esc(short)}</div></details></td></tr>')
            t.append("</tbody></table></div>")

    # 5. 캐시 규모
    if d["size"]:
        t.append("<h2>전용 캐시 규모</h2>")
        t.append('<p class="note">사전 상태를 다 세운 시점과 측정 빌드를 마친 시점이다. '
                 '리모트 캐시의 저장 비용과 전송량을 가늠할 근거다.</p>')
        t.append('<div class="card scroll"><table><thead><tr><th>시나리오</th><th>그래프</th>'
                 '<th class="num">사전 항목</th><th class="num">사전 KB</th>'
                 '<th class="num">사후 항목</th><th class="num">사후 KB</th></tr></thead><tbody>')
        for target in d["targets"]:
            for s in SCENARIO_ORDER:
                for r in d["size"].get((s, target), []):
                    t.append(f'<tr><td>{esc(s)}</td><td class="task">{esc(target)}</td>'
                             f'<td class="num">{esc(r["pre_entries"])}</td><td class="num">{esc(r["pre_kb"])}</td>'
                             f'<td class="num">{esc(r["post_entries"])}</td><td class="num">{esc(r["post_kb"])}</td></tr>')
        t.append("</tbody></table></div>")

    # 6. 한계
    t.append("<h2>이 수치를 읽을 때</h2>")
    t.append('<div class="card"><ul class="limits">'
             "<li><strong>OS 이식성은 확인되지 않았다.</strong> CI 는 <code>ubuntu-latest</code>, 개발자는 "
             "다른 OS 다. CI 가 만든 항목이 개발자 머신에서 적중하는지는 리모트 캐시를 실제로 세워야 알 수 있다. "
             "이식성 게이트가 확인하는 것은 절대경로까지다.</li>"
             "<li><strong>전송 시간이 빠져 있다.</strong> <code>T_remote</code> 는 CI 가 구운 항목이 즉시 "
             "손에 들어온다고 본다. 실제 이득은 이 값보다 작다 — 핵심 값은 낙관적 상한이다.</li>"
             "<li><strong>Kotlin 데몬은 <code>--stop</code> 으로 죽지 않는다.</strong> 회차마다 교체되는 것은 "
             "Gradle 데몬뿐이라 Kotlin 컴파일 쪽 웜업은 통제되지 않는다.</li>"
             "<li><code>clean</code> 이 지우지 않는 상태(<code>.gradle</code> 의 파일 해시, "
             "<code>transforms-*</code>)가 남는다. <code>S2</code> 는 “캐시 축출 직후”이지 “새 머신”이 아니다.</li>"
             "<li>머신마다 수치가 다르다. 서로 다른 머신의 값을 직접 비교하지 않는다.</li>"
             "</ul></div>")

    t.append("</main></body></html>")
    return "".join(t)


def main():
    if len(sys.argv) != 2:
        print("사용법: report.py <run 디렉토리>", file=sys.stderr)
        return 2
    run_dir = sys.argv[1]
    if not os.path.isdir(run_dir):
        print(f"디렉토리가 없다: {run_dir}", file=sys.stderr)
        return 1
    out = os.path.join(run_dir, "report.html")
    with open(out, "w", encoding="utf-8") as f:
        f.write(render(run_dir, collect(run_dir)))
    print(out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
