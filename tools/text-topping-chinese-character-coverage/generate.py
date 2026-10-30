"""번들 Line Seed TW/JP 폰트의 한자 커버리지 테이블을 Kotlin 소스로 생성한다.

어느 디렉터리에서 실행해도 된다:
    python3 tools/text-topping-chinese-character-coverage/generate.py
"""
import hashlib
import sys
from pathlib import Path

try:
    from fontTools.ttLib import TTFont
except ImportError:
    print(
        "fonttools 가 필요하다:\n"
        '  python3 -m venv "${TMPDIR:-/tmp}/chinese-character-venv" && "${TMPDIR:-/tmp}/chinese-character-venv/bin/pip" install fonttools\n'
        '  "${TMPDIR:-/tmp}/chinese-character-venv/bin/python" tools/text-topping-chinese-character-coverage/generate.py',
        file=sys.stderr,
    )
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[2]
MODULE = ROOT / "feature/groups/canvas/impl/src/main"
FONT_DIR = MODULE / "res/font"
OUTPUT = (
    MODULE / "kotlin/com/teamyg/parfait/feature/groups/canvas/impl/model/"
    "TextToppingChineseCharacterCoverageData.kt"
)
# 반복 부호 々〆〇와 〻 을 한자로 본다. 구간 분할도 이 범위를 생성물의 TEXT_TOPPING_CHINESE_CHARACTER_RANGES 로 받는다
CHINESE_CHARACTER_RANGES = [
    (0x3005, 0x3007), (0x303B, 0x303B), (0x3400, 0x4DBF), (0x4E00, 0x9FFF), (0xF900, 0xFAFF), (0x20000, 0x3FFFF),
]
LINE_WIDTH = 120

FONTS = [("TW", "line_seed_tw_regular.ttf"), ("JP", "line_seed_jp_regular.ttf")]


def is_chinese_character(cp):
    return any(lo <= cp <= hi for lo, hi in CHINESE_CHARACTER_RANGES)


def to_ranges(code_points):
    ranges = []
    for cp in sorted(code_points):
        if ranges and ranges[-1][1] + 1 == cp:
            ranges[-1][1] = cp
        else:
            ranges.append([cp, cp])
    return ranges


def bmp_const(name, ranges):
    escapes = "".join(f"\\u{v:04X}" for pair in ranges for v in pair)
    per_line = (LINE_WIDTH - 16) // 6 // 2 * 2 * 6
    chunks = [escapes[i:i + per_line] for i in range(0, len(escapes), per_line)]
    body = " +\n        ".join(f'"{c}"' for c in chunks)
    return f"private const val {name}_BMP =\n    {body}\n"


def supplementary_array(ranges):
    values = [f"0x{v:X}" for pair in ranges for v in pair]
    lines, line = [], "       "
    for v in values:
        piece = f" {v},"
        if len(line) + len(piece) > LINE_WIDTH:
            lines.append(line)
            line = "       "
        line += piece
    if line.strip():
        lines.append(line)
    return "intArrayOf(\n" + "\n".join(lines) + "\n    )"


def han_ranges_const():
    items = "\n".join(f"    0x{lo:X}..0x{hi:X}," for lo, hi in CHINESE_CHARACTER_RANGES)
    return f"internal val TEXT_TOPPING_CHINESE_CHARACTER_RANGES: List<IntRange> = listOf(\n{items}\n)\n"


def main():
    out = [
        "// 생성물 — 직접 고치지 않는다. python3 tools/text-topping-chinese-character-coverage/generate.py",
        "package com.teamyg.parfait.feature.groups.canvas.impl.model",
        "",
        han_ranges_const(),
    ]
    for tag, file_name in FONTS:
        path = FONT_DIR / file_name
        with open(path, "rb") as f:
            digest = hashlib.sha256(f.read()).hexdigest()
        cmap = TTFont(path).getBestCmap()
        ranges = to_ranges(cp for cp in cmap if is_chinese_character(cp))
        bmp = [r for r in ranges if r[1] <= 0xFFFF]
        sup = [r for r in ranges if r[0] > 0xFFFF]
        print(f"{tag}: 구간 {len(ranges)} (BMP {len(bmp)}, 보조 평면 {len(sup)})")
        out.append(bmp_const(f"LINE_SEED_{tag}", bmp))
        out.append(f'internal const val LINE_SEED_{tag}_SHA256 = "{digest}"\n')
        sup_src = supplementary_array(sup) if sup else "intArrayOf()"
        out.append(
            f"internal val LINE_SEED_{tag}_CHINESE_CHARACTERS = TextToppingChineseCharacterCoverage(\n"
            f"    LINE_SEED_{tag}_BMP,\n    {sup_src},\n)\n"
        )
    with open(OUTPUT, "w", encoding="utf-8") as f:
        f.write("\n".join(out))


if __name__ == "__main__":
    main()
