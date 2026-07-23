package com.teamyg.parfait.core.util.jvm.extension

fun Char.isKorean() = this in 'ㄱ'..'ㆎ' || this in 'ㅏ'..'ㅣ' || this in '가'..'힣'
