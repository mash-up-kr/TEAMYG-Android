# text-topping-han-coverage

번들 Line Seed TW·JP 폰트의 한자 커버리지 테이블(`TextToppingHanCoverageData.kt`)을 만든다.

저장소 루트에서 실행한다.

```bash
python3 -m venv .venv && .venv/bin/pip install fonttools
.venv/bin/python tools/text-topping-han-coverage/generate.py
```

폰트를 바꾸면 다시 돌린다. 해시 테스트(`TextToppingHanCoverageTest`)가 놓친 경우를 잡는다.
