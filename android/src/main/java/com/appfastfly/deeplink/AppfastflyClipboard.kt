package com.appfastfly.deeplink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class AppfastflyClipboard(private val context: Context) {

  fun getToken(prefix: String): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null

    val text = clip.getItemAt(0).text?.toString() ?: return null
    if (!text.startsWith(prefix)) return null

    return text.removePrefix(prefix)
  }

  fun clear() {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
  }
}
