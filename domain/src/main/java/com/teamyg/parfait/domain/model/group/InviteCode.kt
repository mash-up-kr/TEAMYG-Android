package com.teamyg.parfait.domain.model.group

@JvmInline
value class InviteCode(val value: String) {
    companion object {
        const val LENGTH = 6

        /**
         * 초대코드가 담길 수 있는 텍스트의 최대 길이.
         * 이보다 긴 텍스트는 초대 메시지가 아니라고 보고 파싱하지 않는다.
         */
        private const val MAX_PARSABLE_LENGTH = 200

        private const val CODE_CHAR_PATTERN = "[A-Za-z0-9]"

        private const val CODE_PATTERN = "$CODE_CHAR_PATTERN{$LENGTH}"

        private val CODE_REGEX = Regex(CODE_PATTERN)

        private val CODE_CHAR_REGEX = Regex(CODE_CHAR_PATTERN)

        /** `Char.isLetterOrDigit()` 은 한글도 참이라 쓸 수 없어 [CODE_CHAR_PATTERN] 으로 판정한다. */
        fun isCodeChar(char: Char): Boolean = CODE_CHAR_REGEX.matches(char.toString())

        /**
         * 클립보드의 초대 메시지에서 맨 뒤 [LENGTH]자를 코드로 읽는다. iOS 가 읽는 방식과 같아서, 메시지는
         * 코드로 끝나야 하고 코드 앞 문구는 보지 않는다.
         */
        fun parseFromMessageOrNull(text: String?): InviteCode? {
            if (text.isNullOrBlank() || text.length > MAX_PARSABLE_LENGTH) {
                return null
            }

            return text
                .trim()
                .takeLast(LENGTH)
                .takeIf { it.length == LENGTH && it.all(::isCodeChar) }
                ?.let(::InviteCode)
        }

        /** 전체가 코드인 텍스트만 인정한다 — 링크 쿼리 값처럼 코드 하나만 와야 하는 입력용이다 */
        fun parseOrNull(text: String?): InviteCode? {
            if (text.isNullOrBlank() || text.length > MAX_PARSABLE_LENGTH) {
                return null
            }

            return text.trim().takeIf(CODE_REGEX::matches)?.let(::InviteCode)
        }
    }
}
