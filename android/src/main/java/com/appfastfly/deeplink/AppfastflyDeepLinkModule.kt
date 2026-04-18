package com.appfastfly.deeplink

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONObject

@ReactModule(name = AppfastflyDeepLinkModule.NAME)
class AppfastflyDeepLinkModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext),
  ActivityEventListener {

  companion object {
    const val NAME = "AppfastflyDeepLink"

    // Static pending URL for cold start (before module init)
    @JvmStatic
    var pendingUrl: String? = null

    /**
     * Call from your MainActivity.onCreate() or onNewIntent() to forward
     * App Link / URI scheme intents to the SDK.
     *
     * Example:
     *   override fun onCreate(savedInstanceState: Bundle?) {
     *     super.onCreate(savedInstanceState)
     *     AppfastflyDeepLinkModule.handleIntent(intent)
     *   }
     *   override fun onNewIntent(intent: Intent) {
     *     super.onNewIntent(intent)
     *     AppfastflyDeepLinkModule.handleIntent(intent)
     *   }
     */
    @JvmStatic
    fun handleIntent(intent: Intent?) {
      val url = intent?.data?.toString() ?: return
      if (instance != null) {
        instance!!.emitDeepLinkUrl(url)
      } else {
        pendingUrl = url
      }
    }

    private var instance: AppfastflyDeepLinkModule? = null
  }

  private var hasListeners = false
  private var cachedEvent: WritableMap? = null

  private val serviceUrl: String by lazy {
    getMetaData("com.appfastfly.SERVICE_URL") ?: "https://api.appfastfly.io.vn"
  }

  private val apiKey: String by lazy {
    getMetaData("com.appfastfly.API_KEY") ?: ""
  }

  override fun getName(): String = NAME

  override fun initialize() {
    super.initialize()
    instance = this
    reactApplicationContext.addActivityEventListener(this)

    if (serviceUrl.isEmpty() || apiKey.isEmpty()) {
      android.util.Log.w("Appfastfly", "Missing com.appfastfly.SERVICE_URL or com.appfastfly.API_KEY in AndroidManifest.xml")
    }

    // Emit any URL that arrived before module was ready
    pendingUrl?.let {
      emitDeepLinkUrl(it)
      pendingUrl = null
    }

    // Check current activity intent for cold start
    currentActivity?.intent?.data?.toString()?.let { url ->
      emitDeepLinkUrl(url)
    }
  }

  override fun invalidate() {
    reactApplicationContext.removeActivityEventListener(this)
    instance = null
    super.invalidate()
  }

  // ActivityEventListener — handles warm start (app already running)
  override fun onNewIntent(intent: Intent?) {
    val url = intent?.data?.toString() ?: return
    emitDeepLinkUrl(url)
  }

  override fun onActivityResult(
    activity: Activity?,
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    // Not used
  }

  private fun emitDeepLinkUrl(url: String) {
    val event = Arguments.createMap().apply {
      putString("url", url)
    }
    sendEvent("onDeepLink", event)
  }

  @ReactMethod
  fun getConfig(promise: Promise) {
    val map = Arguments.createMap()
    map.putString("serviceUrl", serviceUrl)
    map.putString("apiKey", apiKey)
    promise.resolve(map)
  }

  @ReactMethod
  fun getDeviceFingerprint(promise: Promise) {
    try {
      val fingerprint = AppfastflyFingerprint(reactApplicationContext)
      val result = fingerprint.collect()
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("fingerprint_error", e.message, e)
    }
  }

  @ReactMethod
  fun getClipboardToken(prefix: String, promise: Promise) {
    try {
      val clipboard = AppfastflyClipboard(reactApplicationContext)
      val token = clipboard.getToken(prefix)
      promise.resolve(token)
    } catch (e: Exception) {
      promise.reject("clipboard_error", e.message, e)
    }
  }

  @ReactMethod
  fun clearClipboard() {
    val clipboard = AppfastflyClipboard(reactApplicationContext)
    clipboard.clear()
  }

  @ReactMethod
  fun getInstallReferrer(promise: Promise) {
    try {
      val referrer = AppfastflyInstallReferrer(reactApplicationContext)
      referrer.get(promise)
    } catch (e: Exception) {
      promise.reject("referrer_error", e.message, e)
    }
  }

  @ReactMethod
  fun isFirstLaunch(promise: Promise) {
    val prefs = reactApplicationContext.getSharedPreferences("appfastfly", 0)
    val initialized = prefs.getBoolean("initialized", false)
    promise.resolve(!initialized)
  }

  @ReactMethod
  fun markInitialized() {
    val prefs = reactApplicationContext.getSharedPreferences("appfastfly", 0)
    prefs.edit().putBoolean("initialized", true).apply()
  }

  @ReactMethod
  fun getCachedParams(promise: Promise) {
    val prefs = reactApplicationContext.getSharedPreferences("appfastfly", 0)
    val json = prefs.getString("latest_params", null)
    if (json != null) {
      try {
        val map = jsonToWritableMap(JSONObject(json))
        promise.resolve(map)
      } catch (e: Exception) {
        promise.resolve(null)
      }
    } else {
      promise.resolve(null)
    }
  }

  @ReactMethod
  fun setCachedParams(params: ReadableMap) {
    val prefs = reactApplicationContext.getSharedPreferences("appfastfly", 0)
    val jsonObj = JSONObject(params.toHashMap())
    prefs.edit().putString("latest_params", jsonObj.toString()).apply()
  }

  @ReactMethod
  fun addListener(eventName: String) {
    hasListeners = true
    // If cached event exists, emit it
    cachedEvent?.let {
      sendEvent(eventName, it)
      cachedEvent = null
    }
  }

  @ReactMethod
  fun removeListeners(count: Int) {
    hasListeners = false
  }

  private fun sendEvent(eventName: String, params: WritableMap) {
    if (hasListeners) {
      reactApplicationContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(eventName, params)
    } else {
      cachedEvent = params
    }
  }

  private fun getMetaData(key: String): String? {
    return try {
      val ai = reactApplicationContext.packageManager.getApplicationInfo(
        reactApplicationContext.packageName,
        PackageManager.GET_META_DATA
      )
      ai.metaData?.getString(key)
    } catch (e: Exception) {
      null
    }
  }

  private fun jsonToWritableMap(json: JSONObject): WritableMap {
    val map = Arguments.createMap()
    val keys = json.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      when (val value = json.get(key)) {
        is String -> map.putString(key, value)
        is Int -> map.putInt(key, value)
        is Double -> map.putDouble(key, value)
        is Boolean -> map.putBoolean(key, value)
        is JSONObject -> map.putMap(key, jsonToWritableMap(value))
        else -> map.putString(key, value.toString())
      }
    }
    return map
  }
}
