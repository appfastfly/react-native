package com.appfastfly.deeplink

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import android.webkit.WebSettings
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import java.util.Locale
import java.util.TimeZone

class AppfastflyFingerprint(private val context: Context) {

  fun collect(): ReadableMap {
    val map = Arguments.createMap()
    val metrics = DisplayMetrics()

    @Suppress("DEPRECATION")
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    @Suppress("DEPRECATION")
    wm.defaultDisplay.getRealMetrics(metrics)

    val locale = Locale.getDefault()

    map.putString("deviceId", getAndroidId())
    map.putString("brand", Build.BRAND)
    map.putString("model", Build.MODEL)
    map.putString("os", "Android")
    map.putString("osVersion", Build.VERSION.RELEASE)
    map.putString("osBuild", Build.DISPLAY)
    map.putInt("screenWidth", (metrics.widthPixels / metrics.density).toInt())
    map.putInt("screenHeight", (metrics.heightPixels / metrics.density).toInt())
    map.putDouble("screenDpi", metrics.densityDpi.toDouble())
    map.putString("locale", locale.toString())
    map.putString("language", locale.language)
    map.putString("country", locale.country)
    map.putString("timezone", TimeZone.getDefault().id)
    map.putString("userAgent", getDefaultUserAgent())
    map.putBoolean("isEmulator", isEmulator())

    return map
  }

  private fun getAndroidId(): String {
    return Settings.Secure.getString(
      context.contentResolver,
      Settings.Secure.ANDROID_ID
    ) ?: ""
  }

  private fun getDefaultUserAgent(): String {
    return try {
      WebSettings.getDefaultUserAgent(context)
    } catch (_: Exception) {
      "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
  }

  private fun isEmulator(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
      Build.FINGERPRINT.startsWith("unknown") ||
      Build.MODEL.contains("Emulator") ||
      Build.MODEL.contains("Android SDK built for x86") ||
      Build.MANUFACTURER.contains("Genymotion") ||
      Build.PRODUCT.contains("sdk_gphone") ||
      Build.PRODUCT.contains("emulator") ||
      Build.HARDWARE.contains("goldfish") ||
      Build.HARDWARE.contains("ranchu")
  }
}
