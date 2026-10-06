# docs 작업 기록

> append-only. 오래된 것이 위, 새 것이 아래. 최근 기록: `grep "^## \[" docs/log.md | tail -5`
>
> 머리줄: `## [YYYY-MM-DD] <유형> | <제목>`. 유형은 `audit`(doc-baseline 점검 회차) · `lint` · `restructure`.
> 본문은 최대 한 줄. `audit` 제목은 `<develop 커밋> — <PR 번호와 한 구절>`, 본문은
> `<파일 수>파일 +<삽입>/-<삭제> · 유닛 <N> · 계측 <M> · OQ 신설 <ID…|없음> · 해소 <ID…|없음>`
> (값을 알 수 없으면 `?`).

## [2026-07-06] lint | parfait 문서 vs 코드 정합 — 7건 발견, 4건 수정
module-structure `feature/app/setting` 누락 · ADR-0002 `:api` navigation 번들 · ADR-0007 토큰 심볼명·단복수 수정. 나머지 3건은 정보성 미조치.

## [2026-07-15] audit | 9085bc7 — #143 · #94 clickable
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 없음 · 해소 없음

## [2026-07-16] audit | bd844a5 — #141 ygchipbutton
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 없음

## [2026-07-18] audit | 8f63945 — #157 YGButton-fix
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 없음 · 해소 없음

## [2026-07-18] audit | 8cdf942 — #151 · #135 modal
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-07-19] audit | ce4e9b8 — #158 design-system-preview
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 없음 · 해소 ?

## [2026-07-20] audit | 7b954a8 — #160 · #85 app-side-menu
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 없음 · 해소 없음

## [2026-07-22] audit | 23ef432 — #162 yg-screen
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 없음

## [2026-07-22] audit | 526f4c9 — #159 sync-design-system-260719
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-07-22] lint | parfait 문서 내부 정합 — wikilink 3건 수정
archive 문서 hex 3건(보존)과 wiki/index ADR 개수 stale(위키 소관)은 미조치.

## [2026-07-23] audit | 01ba72e — #156 invite-code
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 없음

## [2026-07-23] audit | 4266c58 — #149 design-system-component-text
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-07-26] audit | 0adeed6 — #166 string-resource
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 없음

## [2026-07-27] audit | 2225bb3 — #175 bump-claude-code-review
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 없음 · 해소 없음

## [2026-07-29] audit | dd29dda — #181 design-system-text-component
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-07-31] audit | d538d0e — #165 design-system-component-colorchip
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-01] audit | 1eff238 — #186 grouptag-topping-component
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-01] audit | cb6fc1f — #180 · #169 group-list-background
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 없음

## [2026-08-01] audit | 39d0846 — #174 network-set-up
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-04] audit | 63ed024 — #188 sync-component
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-04] audit | ada2774 — #192 · #86 account-info
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 없음

## [2026-08-04] audit | 23e9357 — #191 · #171 gallery-screen
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-04] audit | 90c651e — #190 set-up-backend-api
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-06] audit | ebad098 — #197 sync-api-service
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-07] audit | 6892503 — #194 · #169 group-list-item-reapply
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-09] audit | 25f7c17 — #219 · #215 test-environment
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-10] audit | 0a1f688 — #220 login-term-agree-navigation
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-11] audit | 7f38c90 — #227 build-action-cache
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-11] audit | 963ffed — #218 login-a002
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 없음

## [2026-08-12] audit | 4af64ba — #199 · #193 canvas-default-screen
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-12] audit | cadab433 — #230 sync-backend-api-260810
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-12] audit | 45de9413 — #224 group-create-enter-flow
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-13] audit | 0521e0bc — #225 setting pop-up
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-15] audit | 4daa6419 — #237 invite-code-clipboard-paste
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-15] audit | 80895eb1 — #241 mvi-error-infra-a002-login
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 OQ-P-157 · 해소 없음

## [2026-08-15] audit | ca9e4581 — #248 · #245 group-list-api
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 OQ-P-165, OQ-P-166, OQ-P-167, OQ-P-168, OQ-P-169, OQ-P-170, OQ-P-171 · 해소 OQ-P-019, OQ-P-104, OQ-P-112, OQ-P-134, OQ-P-147

## [2026-08-15] audit | bf72292a — #231 background-edit-screen
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 OQ-P-173, OQ-P-174, OQ-P-175, OQ-P-176, OQ-P-177, OQ-P-178 · 해소 없음

## [2026-08-15] audit | 60df07a4 — #250 canvas-topping-member-api
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 OQ-P-181, OQ-P-182 · 해소 OQ-P-132, OQ-P-158

## [2026-08-16] audit | 2d0f6a5d — #259 · #207 canvas-calendar
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 OQ-P-183, OQ-P-184, OQ-P-185, OQ-P-186, OQ-P-187, OQ-P-188 · 해소 ?

## [2026-08-16] audit | 1873a8f0 — #266 · #265 sync-backend-api-260816
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 OQ-P-193, OQ-P-194 · 해소 ?

## [2026-08-16] audit | 2143c229 — #263 user-info-ssot
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 OQ-P-195, OQ-P-196, OQ-P-197, OQ-P-001, OQ-P-198 · 해소 없음

## [2026-08-16] audit | 0e2643cf — #261 group-join-modal-after-nickname
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 없음 · 해소 OQ-P-166

## [2026-08-16] audit | 46ed38fb — #264 canvas-topping-screen
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-17] audit | 955c4636 — #267 common-error-loading-scaffold
?파일 +?/-? · 유닛 417 · 계측 12 · OQ 신설 OQ-P-204, OQ-P-205, OQ-P-206 · 해소 없음

## [2026-08-17] audit | 977f44f2 — #268 canvas-today-parfait-detail
?파일 +?/-? · 유닛 434 · 계측 ? · OQ 신설 ? · 해소 OQ-P-184

## [2026-08-17] audit | fa7d79d6 — #279 canvas-calendar-api
?파일 +?/-? · 유닛 436 · 계측 ? · OQ 신설 OQ-P-211, OQ-P-212 · 해소 OQ-P-183

## [2026-08-17] audit | ede719f0 — #292 · #284 clickable-to-clickable-yg
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-17] audit | 2d15cd9f — #287 · #277 group-leave-report-api
?파일 +?/-? · 유닛 456 · 계측 ? · OQ 신설 OQ-P-216, OQ-P-217, OQ-P-218, OQ-P-403, OQ-P-404 · 해소 OQ-P-138

## [2026-08-18] audit | 8730ffa3 — #297 · #288 group-list-refresh
?파일 +?/-? · 유닛 467 · 계측 ? · OQ 신설 OQ-P-221 · 해소 OQ-P-169

## [2026-08-18] audit | 86f0f6b0 — #305 · #254 group-list-refresh-lottie
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 OQ-P-229, OQ-P-230, OQ-P-231, OQ-P-232, OQ-P-233 · 해소 ?

## [2026-08-19] audit | f12870a8 — #290 feature/topping-add-screen
?파일 +?/-? · 유닛 484 · 계측 ? · OQ 신설 OQ-P-238, OQ-P-239, OQ-P-240, OQ-P-241 · 해소 OQ-P-200

## [2026-08-20] audit | c36cad49 — #306 · #236 withdraw-api
?파일 +?/-? · 유닛 ? · 계측 ? · OQ 신설 OQ-P-242 · 해소 OQ-P-141

## [2026-08-20] audit | 750cc2dd — #310 · #300 sync-backend-api-250819
?파일 +?/-? · 유닛 538 · 계측 ? · OQ 신설 OQ-P-243 · 해소 OQ-P-165, OQ-P-168, OQ-P-216, OQ-P-222, OQ-P-224

## [2026-08-20] audit | cf357937 — #309 refactor/segmentation-develop
?파일 +?/-? · 유닛 560 · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-20] audit | 36719e8e — #315 · #318 term-agree scaffold v2
?파일 +?/-? · 유닛 561 · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-21] audit | da03c9b0 — #320 · #319 · #312 · #298 · #322 overlay reset
?파일 +?/-? · 유닛 602 · 계측 ? · OQ 신설 OQ-P-250, OQ-P-251, OQ-P-252, OQ-P-253 · 해소 OQ-P-197

## [2026-08-22] audit | ef55a58c — #311 · #325 segmentation 공통 로딩·토스트
?파일 +?/-? · 유닛 602 · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-22] audit | 19cb5299 — #334 C-106 결선 스택 PR3~PR6
77파일 +3602/-433 · 유닛 694 · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-22] audit | a0d584ef — #326 nav screen transition
5파일 +156/-1 · 유닛 696 · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-22] audit | 8eb2af7d — #329 canvas bg edit api + test
44파일 +1677/-218 · 유닛 737 · 계측 ? · OQ 신설 OQ-P-261, OQ-P-409, OQ-P-262, OQ-P-263 · 해소 OQ-P-173, OQ-P-199

## [2026-08-22] audit | 96dc215c — #339 portrait lock + version/dependency bump
6파일 +42/-17 · 유닛 737 · 계측 ? · OQ 신설 OQ-P-264, OQ-P-265 · 해소 ?

## [2026-08-23] audit | f31b8c30 — #324 · #335 gallery-store
12파일 +416/-26 · 유닛 745 · 계측 ? · OQ 신설 OQ-P-270, OQ-P-271, OQ-P-272, OQ-P-273, OQ-P-274 · 해소 OQ-P-211

## [2026-08-23] audit | d634efd3 — #336 topping-edit-c305 api
6파일 +318/-32 · 유닛 751 · 계측 ? · OQ 신설 OQ-P-275, OQ-P-276 · 해소 OQ-P-199

## [2026-08-24] audit | 34bf1939 — #342 c103-multi-subject-ui
17파일 +1074/-211 · 유닛 775 · 계측 ? · OQ 신설 없음 · 해소 OQ-P-268

## [2026-08-25] audit | a5e8a760 — #349 segmentation-preprocessing
9파일 +177/-6 · 유닛 ? · 계측 ? · OQ 신설 ? · 해소 ?

## [2026-08-25] audit | 37ea970b — #350 · #345 permission-screen-inset
3파일 +59/-48 · 유닛 ? · 계측 ? · OQ 신설 OQ-P-301 · 해소 ?

## [2026-08-26] audit | df3f4cbe — #368 ignore-release-keystore
17파일 +383/-153 · 유닛 785 · 계측 14 · OQ 신설 ? · 해소 ?

## [2026-08-26] audit | cbb48cd8 — #366 · #365 font
5파일 +97/-0 · 유닛 785 · 계측 14 · OQ 신설 OQ-P-306, OQ-P-307 · 해소 ?

## [2026-08-26] audit | c55e10bc — #372 · #374 · #376 release build
13파일 +144/-54 · 유닛 789 · 계측 14 · OQ 신설 OQ-P-308, OQ-P-309, OQ-P-310, OQ-P-311 · 해소 OQ-P-250

## [2026-08-26] audit | bf06c830 — #371 toast position fix
4파일 +37/-3 · 유닛 789 · 계측 14 · OQ 신설 OQ-P-312 · 해소 ?

## [2026-08-27] audit | 5dc9fbec — #389 canvas-main-alpha-hit
?파일 +?/-? · 유닛 819 · 계측 ? · OQ 신설 OQ-P-313, OQ-P-314, OQ-P-315, OQ-P-316, OQ-P-317 · 해소 OQ-P-254

## [2026-08-27] audit | 4da18230 — #363 segmentation-alpha-refinement
20파일 +3685/-223 · 유닛 926 · 계측 14 · OQ 신설 OQ-P-318, OQ-P-319 · 해소 ?

## [2026-08-28] audit | 84a89728 — #369 · #400 · #398 토핑 편집 재구현
18파일 +441/-31 · 유닛 931 · 계측 14 · OQ 신설 OQ-P-326, OQ-P-324, OQ-P-327, OQ-P-325 · 해소 ?

## [2026-08-28] audit | 627e1867 — #393 · #394 · #396 · #395 · #397 그룹 생성·참여 수정
28파일 +706/-418 · 유닛 942 · 계측 14 · OQ 신설 OQ-P-328 · 해소 ?

## [2026-08-30] audit | 27e85d0d — #405 · #406 · #407 · #409 스포트라이트 토스트 교체
14파일 +383/-59 · 유닛 949 · 계측 17 · OQ 신설 OQ-P-329, OQ-P-331, OQ-P-330 · 해소 ?

## [2026-08-31] audit | afde8c4c — #404 · #408 캔버스 폴링 스택 3단
51파일 +3045/-871 · 유닛 996 · 계측 17 · OQ 신설 OQ-P-332 · 해소 OQ-P-258

## [2026-09-01] audit | 6a1da1b0 — #428 · #425 API 현행화 260831
37파일 +997/-437 · 유닛 1012 · 계측 17 · OQ 신설 ? · 해소 ?

## [2026-09-01] audit | fa46e5cf — #430 그룹 추가 팝업 배경색
1파일 +1/-1 · 유닛 1012 · 계측 17 · OQ 신설 ? · 해소 ?

## [2026-09-01] audit | 0173e454 — #412 · #413 · #414 · #411 · #434 핸들 모서리
25파일 +449/-85 · 유닛 1015 · 계측 17 · OQ 신설 OQ-P-339, OQ-P-340 · 해소 OQ-P-135

## [2026-09-03] audit | 40e1fca6 — #439 · #437 · #438 · #442 README 소개·스크린샷
52파일 +940/-101 · 유닛 1029 · 계측 17 · OQ 신설 OQ-P-347 · 해소 ?

## [2026-09-03] audit | c74f40eb — #444 스플래시 로띠 교체
1파일 +0/-0 · 유닛 1029 · 계측 17 · OQ 신설 OQ-P-350 · 해소 없음

## [2026-09-04] audit | 2b1dce3a — #451 API 현행화 260903 — http/ 요청 모음
5파일 +550/-3 · 유닛 1029 · 계측 17 · OQ 신설 OQ-P-354 · 해소 없음

## [2026-09-04] audit | e6ce42b1 — #440 원격 이미지 로딩 표현 — 캔버스 일괄 드러내기 · G-001 순차 등장
41파일 +1697/-224 · 유닛 1047 · 계측 35 · OQ 신설 ? · 해소 ?

## [2026-09-05] audit | 29c2f050 — #446 · #447 푸시 알림 딥링크
17파일 +462/-0 · 유닛 1060 · 계측 35 · OQ 신설 ? · 해소 ?

## [2026-09-05] audit | bc216632 — #445 · #453 · #449 캔버스 저장 미리보기
42파일 +1535/-98 · 유닛 1072 · 계측 35 · OQ 신설 ? · 해소 없음

## [2026-09-05] audit | 489b14cc — #450 알림 권한 안내 · 기기 토큰 등록 · 이벤트 버스 재배치
42파일 +944/-77 · 유닛 1091 · 계측 35 · OQ 신설 ? · 해소 ?

## [2026-09-06] audit | 5907e286 — #455 · #456 · #457 · #458 크림 하한 3칸
24파일 +399/-130 · 유닛 1096 · 계측 35 · OQ 신설 없음 · 해소 없음

## [2026-09-07] audit | 2285d09d — #461 · #463 그룹 참여 닉네임 기본값
14파일 +302/-28 · 유닛 1106 · 계측 35 · OQ 신설 OQ-P-377 · 해소 없음

## [2026-09-08] audit | 23675cc1 — #464 · #465 · #466 토핑 테두리 거리장 통일
39파일 +2102/-782 · 유닛 1137 · 계측 37 · OQ 신설 없음 · 해소 ?

## [2026-09-08] audit | b7674e88 — #469 · #470 캔버스 응답 groupName 수신
12파일 +57/-6 · 유닛 1139 · 계측 37 · OQ 신설 없음 · 해소 ?

## [2026-09-09] audit | acbc4b45 — #472 · #473 토핑 편집 빈 알맹이 차단
21파일 +941/-68 · 유닛 1164 · 계측 37 · OQ 신설 OQ-P-387, OQ-P-388, OQ-P-389, OQ-P-390, OQ-P-391 · 해소 ?

## [2026-09-10] audit | efa77150 — #477 · #478 · #479 · #480 · #482 지난 캔버스 알럿
78파일 +2573/-305 · 유닛 1229 · 계측 39 · OQ 신설 OQ-P-392, OQ-P-393, OQ-P-394, OQ-P-395, OQ-P-396, OQ-P-397 · 해소 OQ-P-389

## [2026-09-10] audit | 69bbbe68 — #483 버전 1.1.2 코드 9
1파일 +2/-2 · 유닛 1229 · 계측 39 · OQ 신설 없음 · 해소 없음

## [2026-09-10] audit | 95b7fc4d5 — #487 세그멘테이션 재시도 회복 + 실패 화면 「직접 편집」
29파일 +2109/-458 · 유닛 1282 · 계측 39 · OQ 신설 OQ-P-399, OQ-P-400, OQ-P-401 · 해소 ?

## [2026-09-11] audit | c37dc2b4c — #488 · #489 · #490 버전 1.1.3 코드 10
8파일 +133/-3 · 유닛 1288 · 계측 39 · OQ 신설 없음 · 해소 ?

## [2026-09-11] audit | 1b21725ba — #496 그룹 목록 응답 테두리 세 필드 수용 + 테두리 변환 공용 매퍼화
14파일 +151/-28 · 유닛 1292 · 계측 39 · OQ 신설 없음 · 해소 ?

## [2026-09-16] audit | f37a76540 — #497 · #499 G-001 목록 토핑 테두리 렌더 + 띠 판 캐시 선반화
16파일 +1649/-29 · 유닛 1298 · 계측 46 · OQ 신설 OQ-P-402 · 해소 ?

## [2026-09-17] audit | 924cb5802 — #504 · #502 이슈 코드 정리 1차 — ktx Bitmap 확장 ·…
56파일 +179/-208 · 유닛 1298 · 계측 46 · OQ 신설 OQ-P-403 · 해소 ?

## [2026-09-21] audit | e10ead2ca — #514 · #502 · #513 이슈 코드 정리 2차 — 도달 불가 캔버스 화면 셋 삭제 · 사용처 0…
40파일 +124/-695 · 유닛 1298 · 계측 46 · OQ 신설 OQ-P-404 · 해소 OQ-P-053, OQ-P-239

## [2026-09-21] audit | 143cda87b — #511 LLM 위키 체계를 TJYG-Android 저장소로 이식 — wiki/…
158파일 +27237/-0 · 유닛 1298 · 계측 46 · OQ 신설 OQ-P-405 · 해소 ?

## [2026-09-26] restructure | index·doc-baseline을 status·log로 분리, lint 보고서 폐지
기록 기준을 루트 CLAUDE.md에 도입하고 doc-baseline 이력 98행을 이 파일로 옮겼다.

## [2026-09-27] audit | 594f8047e — #534 data 레이어 패키지·이름 정리 · #527 · #535 문구 · #528 docs 이관
497파일 +137403/-4010 · 유닛 1294 · 계측 46 · OQ 신설 없음 · 해소 OQ-P-367(①②, 부분)

## [2026-09-28] lint | 토핑 핀치 제스처 반영 — 삭제된 핸들 심볼 앵커 정리, 7건 수정
status.md C-106·C-301 조작 서술 · OQ-P-202 문구(핀치 기준, 미결 유지) · design-system·module-structure `dragBy` 앵커 · 스펙 `status: implemented` · specs·plans README 상태.

## [2026-09-30] restructure | C-101-Loading 반영 — 세그멘테이션 분석 흐름 문서 갱신, 스펙·계획 archive
status·navigation-flow·data-layer·design-system·ADR-0012 갱신 · 해소 OQ-P-399, OQ-P-401, OQ-P-400(①~⑤) · OQ 신설 OQ-P-411

## [2026-10-02] restructure | 배치 화면 테두리 패널(추가 플로우) 반영 — status 「토핑 생성·배치」 덮어쓰기, ADR-0034 신설
ADR-0026 본문·인덱스에서 초안의 테두리 서술 정리 · OQ-P-081(②)·OQ-P-203(③) 사용처 고쳐 씀 · OQ 신설 OQ-P-412(실기기 미확인) · 유닛 1239 · 계측 30(`feature:groups:canvas:impl`, SM-A356N) · 피그마·실기기 대조는 하지 않았다

## [2026-10-02] lint | 배치 화면 테두리 패널 최종 리뷰 반영 — 낮은 화면에서 캔버스가 확정 버튼 밑으로 넘치던 배치 수정, 문서 6건 정정
OQ-P-256 호출 경로·잠금 테스트 서술을 현재 코드에 맞춤(미결 유지) · OQ-P-412 출처에서 브랜치명 제거, 낮은 화면 실기기 미확인 추가(미결 유지) · status 「튜토리얼」 원본 없는 누끼 확인 진입 · ADR-0025 `related_adr`에 ADR-0034 · 유닛 `feature:groups:canvas:impl` 202·`domain` 130 · 계측 33(`feature:groups:canvas:impl`, 에뮬레이터 Pixel_7_API_36) · 낮은 화면 실기기 확인은 하지 않았다

## [2026-10-02] restructure | 배치 수정 화면(`CanvasToppingArrange`) 반영 — status 「토핑 생성·배치·배치 수정」·「캔버스 배경 편집」 덮어쓰기, 스펙·계획 둘 archive
navigation-flow·design-system·data-layer·api/parfait-image Android 매핑·ADR-0034 갱신 · 해소 OQ-P-201, OQ-P-276, OQ-P-324, OQ-P-337, OQ-P-338, OQ-P-379(이미 닫혀 있던 OQ-P-254 항목 삭제) · 고쳐 씀 OQ-P-081, OQ-P-175, OQ-P-202, OQ-P-203, OQ-P-326, OQ-P-391 · OQ 신설 OQ-P-413, OQ-P-414, OQ-P-415, OQ-P-416, OQ-P-417, OQ-P-418, OQ-P-419 · 유닛 1256 · 계측 44(`feature:groups:canvas:impl`, SM-A356N) · 피그마·실기기·TalkBack 대조는 하지 않았다

## [2026-10-02] lint | 배치 수정 화면 최종 리뷰 반영 — 삭제 뒤 재조회 동안 로딩 유지, 이탈을 `popUpTo<NavKeyCanvasMain>()`으로, 그만두기 확인이 팝업을 닫음, 패널이 닫히는 동안 포커스된 토핑을 위에 유지, 포커스가 사라지면 삭제 모달도 닫음
status 「토핑 생성·배치·배치 수정」·navigation-flow 이탈 경로 정정 · 고쳐 씀 OQ-P-270, OQ-P-414(본문 문구 추가), OQ-P-416(안 쓰는 기하 추가), OQ-P-418(재조회 예외 토스트만 남김) · OQ 신설 OQ-P-420, OQ-P-421, OQ-P-422, OQ-P-423 · 유닛 `feature:groups:canvas:impl` 227·`feature:segmentation:impl` 77·`app` 35 · 계측 44(`feature:groups:canvas:impl`, SM-A356N)

## [2026-10-03] lint | 배치 수정 화면의 토스트를 헤더 아래 캔버스 윗변으로 내림 — 피그마 `5479:13704` 대조, 스캐폴드 자리는 헤더를 덮었다
status 「토핑 생성·배치·배치 수정」에 토스트 자리(`ToppingArrangeLayout`의 `toast` 슬롯) 추가 · 고쳐 씀 OQ-P-419(토스트 모양 대조 항목 걷음) · 추가 플로우 배치 화면의 토스트는 스캐폴드 자리 그대로다

## [2026-10-03] lint | PR 리뷰 반영 — 배치 수정 화면의 확정·삭제 뒤 재조회 예외를 실패로 알리지 않음, 삭제가 다른 토핑의 미저장 변경을 버리는 동작을 테스트로 고정
status 「토핑 생성·배치·배치 수정」 재조회 실패 서술 정정 · 해소 OQ-P-418 · 고쳐 씀 OQ-P-270(고정하는 테스트 이름)

## [2026-10-03] lint | 누끼 편집 진입 흐름 단축을 현재 상태 문서에 반영 — 확인 화면(`SegmentationConfirm`)·편집 결과 왕복 삭제
status 「토핑 생성·배치·배치 수정」·「누끼 추출」·「튜토리얼」 · navigation-flow 토핑 생성 플로우 · state-management 저장 → 초안 기록 → 이동 순서 계약 · design-system 튜토리얼 소비처 · 고쳐 씀 OQ-P-105·OQ-P-269·OQ-P-347·OQ-P-368·OQ-P-369·OQ-P-400 · 해소 OQ-P-277·OQ-P-380 · ADR-0025·0026·0034에 현재 코드와 다른 점 표기

## [2026-10-03] lint | 누끼 편집 화면 개편(C-104)을 현재 상태 문서에 반영 — 스펙·계획 2건을 archive로
status 「누끼 추출」 · design-system 헤더 아래 토스트 호스트·`YGFloatingBar` 배치 · 추가 OQ-P-425 · 고쳐 씀 OQ-P-081(`Edit` 소비처) · 스펙·계획 링크를 archive 경로로

## [2026-10-03] lint | 배경 변경 화면(C-301)을 피그마 `5461:9337`에 맞춤 — 상단 제목 바 + 하단 「저장하기」, 닫기 팝업은 남김
status 「캔버스 배경 편집」 닫기·저장 서술 고쳐 씀 · 고쳐 씀 OQ-P-081(`YGFloatingBarEdit` 프로덕션 사용처 없음) · 고쳐 씀 OQ-P-174(좌우 여백 44dp, ③ 걷음)

## [2026-10-04] audit | 4c40ddfee — #580 G-001 Empty 안내 애니메이션 · #584 · #576 · #572 · #573 · #568 · #564 · #560 · #553 핀치 제스처 · #550 · #555 · #539 · #544
247파일 +14457/-7959 · 유닛 1314 · 계측 91 · OQ 신설 없음 · 해소 없음

## [2026-10-04] lint | 튜토리얼 제거(#586)를 현재 상태 문서에 반영 — 캔버스·갤러리 오버레이와 `ygtutorial`·사용자 설정 저장소가 코드에서 빠짐
status 「튜토리얼」 절 삭제 · design-system 튜토리얼 컴포넌트 · data-layer `UserConfig*`·평문 DataStore 소비처 · state-management `launchWhileSubscribed` 주석 · 해소 OQ-P-363, OQ-P-366, OQ-P-369, OQ-P-417 · 고쳐 씀 OQ-P-175, OQ-P-367, OQ-P-368, OQ-P-370

## [2026-10-05] restructure | 그만두기 팝업(`YGModalQuit.kt`)을 `core:designsystem`에서 `core:ui` `component/modal/`로 옮김 — `core:ui` → `core:designsystem` `implementation` 간선 신설
module-structure `core:ui` 행·enum 변환 공용화 항목 · design-system 컴포넌트 표 · navigation-flow 그만두기 팝업 · status 배치 수정 화면 · 고쳐 씀 OQ-P-027, OQ-P-414

## [2026-10-05] restructure | 전역 Coil 로더 팩토리 `newParfaitImageLoader`를 `core:designsystem`에서 `core:ui` `image/`로 옮김 — 디자인시스템에 남은 `rememberReloadableImageRequest`는 파일명이 `ReloadableImageRequest.kt`가 됨
module-structure `core:ui` 행 · design-system 디렉터리 표·이미지 로딩·이미지 로더 항목 · `ParfaitImageLoaderTest`가 `core:ui` 계측 소스셋으로 가며 CI 컴파일 대상에서 빠짐(OQ-P-102)

## [2026-10-05] restructure | CI 계측 컴파일 대상을 손으로 적던 목록에서 루트 `assembleAllDebugAndroidTest`로 바꿈 — `parfait.test.android` 적용 모듈이 자동으로 걸려 `core:ui` 계측 소스셋도 컴파일된다
status 「빌드·인프라」 CI 계측 줄 · OQ-P-102 ②(기기에서 실행)는 그대로 미결

## [2026-10-05] restructure | CI 시간 단축 — `org.gradle.parallel` 재도입(힙 4096m·`kotlin.daemon.jvmargs`), 시딩 태스크를 `assembleAllDebugAndroidTest`로 맞춤, `test.yml` Gradle 호출 통합, 문서 전용 PR은 스텝 건너뜀(`detect-code-changes`), `test`·`ktlint` concurrency 취소
병렬 검증: 캐시 없이 전체 재실행한 `test`+`assembleAllDebugAndroidTest`+`ktlintCheck`·`assembleRelease` 통과, `lint`는 병렬과 무관한 기존 오류로 실패 · 고쳐 씀 OQ-P-114(configuration cache만 남김) · 신설 OQ-P-427

## [2026-10-05] lint | 리뷰 지적 반영 — `detect-code-changes`가 rename 이전 경로와 3000개 초과 PR을 코드로 보고, `test.yml`에 `--continue`, `ktlint.yml`에 `permissions` 명시
고쳐 씀 OQ-P-102(② 계측 컴파일 대상·규모를 현재 상태로 덮어쓰고 날짜별 누적 단락 제거), OQ-P-114 출처 · module-structure enum 변환 공용화 항목

## [2026-10-05] lint | PR #591 리뷰 반영 — `core:ui` → `:core:designsystem` 간선이 `implementation`인 이유와 OQ-P-142와의 경계를 module-structure에 적음
module-structure `core:ui` 의존 항목 신설 · enum 변환 공용화 항목의 가시성 미결 서술

## [2026-10-06] audit | 닫기·뒤로가기 팝업 흐름 변경(#589)을 문서에 반영
status 토핑 생성·배치·누끼·배경 편집 항목 · navigation-flow `popUpTo` 소비처와 그만두기 팝업·배경 편집 복귀 항목 · ADR-0034 현재 코드 메모 · OQ-P-411 ③ · OQ-P-414(① 변경 없을 때 팝업 해소, 제목·본문만 남김)

## [2026-10-06] restructure | 배치 수정 화면의 삭제를 즉시 DELETE에서 확정 시점 일괄 반영으로 바꿈 — 삭제 버튼은 화면에서만 빼고, 확정이 PATCH 뒤에 DELETE를 보낸다
status 「토핑 생성·배치·배치 수정」 삭제 서술·주의 줄 · data-layer·api/parfait-image·api/README 삭제 소비처 서술 · 해소 OQ-P-270

## [2026-10-06] lint | 리뷰 지적 반영 — 배치 수정 화면의 삭제 대기(`pendingDeleteToppingIds`)와 툼스톤(`deletedToppingIds`)을 가름, 끝난 삭제는 재확정 때 다시 나가지 않는다
status 「토핑 생성·배치·배치 수정」 삭제 서술 · api/parfait-image 삭제 소비처 서술

## [2026-10-06] lint | G-001-Empty 정책 v0.3 대조 — 등장 도중 그룹 추가 칩을 누르면 `Shown`으로 건너뛰고(`GroupListEmptyIntroPhase.onClickAddGroup`), 버튼이 받은 터치는 종료로 치지 않게 고침
고쳐 씀 status 「그룹 목록」 · g001-empty-animation 스펙(단계·종료·주의) · OQ-P-426(④ 신설, 상태)
