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

        /** 초대 메시지 템플릿의 초대코드 자리 표시자 */
        private const val CODE_PLACEHOLDER = "%1\$s"

        private const val CODE_PATTERN = "[A-Za-z0-9]{$LENGTH}"

        private val CODE_REGEX = Regex(CODE_PATTERN)

        /**
         * 클립보드처럼 초대코드 외의 문구가 섞인 텍스트에서 초대코드를 추출한다.
         *
         * [messageTemplate] 을 주면 초대 메시지 템플릿의 코드 자리에 있는 값만 정확히 뽑는다.
         * 템플릿과 형태가 다르면 텍스트 전체가 초대코드일 때(코드만 복사한 경우)에만 인정한다.
         *
         * @param messageTemplate `%1$s` 자리에 초대코드가 들어가는 초대 메시지 템플릿
         */
        fun parseOrNull(
            text: String?,
            messageTemplate: String? = null,
        ): InviteCode? {
            if (text.isNullOrBlank() || text.length > MAX_PARSABLE_LENGTH) {
                return null
            }

            val code = messageTemplate?.let { template -> text.findCodeByTemplate(template) }
                ?: text.toCodeOrNull()

            return code?.let(::InviteCode)
        }

        /**
         * 초대 메시지 템플릿을 정규식으로 바꿔 코드 자리의 값만 추출한다.
         * 코드 앞뒤 문구는 그대로 매치해야 하므로 이스케이프해서 사용한다.
         */
        private fun String.findCodeByTemplate(template: String): String? {
            val placeholderIndex = template.indexOf(CODE_PLACEHOLDER)
            if (placeholderIndex < 0) {
                return null
            }

            val prefix = template.take(placeholderIndex)
            val suffix = template.drop(placeholderIndex + CODE_PLACEHOLDER.length)
            val templateRegex = Regex(Regex.escape(prefix) + "($CODE_PATTERN)" + Regex.escape(suffix))

            return templateRegex.find(this)?.groupValues?.get(1)
        }

        /**
         * 코드만 복사한 경우를 위해, 텍스트 전체가 초대코드일 때만 코드로 인정한다.
         *
         * 문장 속에서 6자 토큰을 찾으면 초대와 무관한 텍스트(예: `group_invite_message` 같은 식별자)에서도
         * 코드를 주워오게 되므로 부분 매치는 허용하지 않는다.
         */
        private fun String.toCodeOrNull(): String? = trim().takeIf(CODE_REGEX::matches)
    }
}
