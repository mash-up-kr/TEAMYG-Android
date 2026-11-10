package com.teamyg.parfait.core.ui.share

import android.content.Context
import android.content.Intent
import com.teamyg.parfait.core.ui.R

/** 초대 메시지(`group_invite_message`)를 시스템 공유 시트로 넘긴다. */
fun Context.shareInviteMessage(inviteCode: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, getString(R.string.group_invite_message, inviteCode))
    }
    startActivity(Intent.createChooser(sendIntent, null))
}
