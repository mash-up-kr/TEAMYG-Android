package com.teamyg.parfait.domain.model.member

/**
 * 계정 하나당 하나인 전역 닉네임. 그룹 안에서 쓰는 이름은 GroupNickname 으로 별개다.
 *
 * 서버 유효성 규칙은 GroupNickname 과 문자 그대로 같지만(1~15자, 한글·영문·숫자,
 * 단어 사이 한 칸 공백) 타입을 합치지 않는다. 합치면 전역 닉네임을 그룹 API 에
 * 그대로 넘기는 실수가 컴파일을 통과한다. 검증은 서버가 하며 이 타입은 감싸기만 한다.
 */
@JvmInline
value class GlobalNickname(val value: String)
