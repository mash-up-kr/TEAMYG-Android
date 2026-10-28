# text-topping-chinese-character-coverage

번들 Line Seed TW·JP 폰트의 한자 커버리지 테이블(`TextToppingChineseCharacterCoverageData.kt`)을 만든다.

어느 디렉터리에서 실행해도 된다. 가상환경은 저장소 밖(`${TMPDIR:-/tmp}`)에 만든다.

```bash
python3 -m venv "${TMPDIR:-/tmp}/chinese-character-venv" && "${TMPDIR:-/tmp}/chinese-character-venv/bin/pip" install fonttools
"${TMPDIR:-/tmp}/chinese-character-venv/bin/python" tools/text-topping-chinese-character-coverage/generate.py
```

폰트를 바꾸면 다시 돌린다. 재생성을 잊으면 `TextToppingChineseCharacterCoverageTest`가 실패한다.
