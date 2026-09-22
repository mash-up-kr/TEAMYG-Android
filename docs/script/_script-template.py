#!/usr/bin/env python3
"""<한 줄: 이 스크립트가 하는 일>.

용법:
    python3 parfait/script/<name>.py <args>

규약:
- stdlib 전용(pip 의존성 0).
- repo 루트 = Path(__file__).resolve().parents[2] 기준 상대 경로.
- 스킬이 호출하면 SKILL.md에서 `python3 parfait/script/<name>.py`로 참조.
"""
import argparse
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SKILLS_DIR = REPO_ROOT / ".claude" / "skills"


def main():
    ap = argparse.ArgumentParser(description="<설명>")
    # ap.add_argument(...)
    ap.parse_args()
    # ...


if __name__ == "__main__":
    main()
