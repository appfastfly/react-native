package com.appfastfly.deeplink

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONObject

@ReactModule(name = NativeAppfastflyDeepLinkSpec.NAME)
class AppfastflyDeepLinkModule(reactContext: ReactApplicationContext) :
  NativeAppfastflyDeepLinkSpec(reactContext),
  ActivityEventListener {

  companion object {
    const val NAME = NativeAppfastflyDeepLinkSpec.NAME

    @JvmStatic
    var pendingUrl: String? = null

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
  private var apiClient: AppfastflyApiClient? = null

  private val serviceUrl: String by lazy {
    getMetaData("com.appfastfly.SERVICE_URL") ?: "https://api.appfastfly.io.vn"
  }

  private val apiKey: String by lazy {
    getMetaData("com.appfastfly.API_KEY") ?: ""
  }

  override fun initialize() {
    super.initialize()
    instance = this
    reactApplicationContext.addActivityEventListener(this)

    try {
      apiClient = AppfastflyApiClient(serviceUrl, apiKey)
    } catch (_: Exception) {}

    if (serviceUrl.isEmpty() || apiKey.isEmpty()) {
      android.util.Log.w("Appfastfly", "Missing SERVICE_URL or API_KEY in AndroidManifest.xml")
    }

    pendingUrl?.let {
      emitDeepLinkUrl(it)
      pendingUrl = null
    }

    try {
      reactApplicationContext.currentActivity?.intent?.data?.toString()?.let { url ->
        emitDeepLinkUrl(url)
      }
    } catch (_: Exception) {}
  }

  override fun invalidate() {
    try { reactApplicationContext.removeActivityEventListener(this) } catch (_: Exception) {}
    instance = null
    super.invalidate()
  }

  override fun onNewIntent(intent: Intent) {
    try {
      val url = intent.data?.toString() ?: return
      emitDeepLinkUrl(url)
    } catch (_: Exception) {}
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {}

  private fun emitDeepLinkUrl(url: String) {
    try {
      val event = Arguments.createMap().apply { putString("url", url) }
      sendEvent("onDeepLink", event)
    } catch (_: Exception) {}
  }

  // --- Config & Fingerprint ---

  override fun getConfig(promise: Promise) {
    try {
      val map = Arguments.createMap()
      map.putString("serviceUrl", serviceUrl)
      map.putString("apiKey", apiKey)
      promise.resolve(map)
    } catch (_: Exception) {
      promise.resolve(Arguments.createMap().apply {
        putString("serviceUrl", "")
        putString("apiKey", "")
      })
    }
  }

  override fun getDeviceFingerprint(promise: Promise) {
    try {
      val fingerprint = AppfastflyFingerprint(reactApplicationContext)
      promise.resolve(fingerprint.collect())
    } catch (_: Exception) {
      promise.resolve(Arguments.createMap())
    }
  }

  override fun getClipboardToken(prefix: String, promise: Promise) {
    try {
      val clipboard = AppfastflyClipboard(reactApplicationContext)
      promise.resolve(clipboard.getToken(prefix))
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  override fun clearClipboard() {
    try { AppfastflyClipboard(reactApplicationContext).clear() } catch (_: Exception) {}
  }

  override fun getInstallReferrer(promise: Promise) {
    try {
      AppfastflyInstallReferrer(reactApplicationContext).get(promise)
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  // --- Launch state ---

  override fun isFirstLaunch(promise: Promise) {
    try {
      val prefs = reactApplicationContext.getSharedPreferences("appfastfly", 0)
      promise.resolve(!prefs.getBoolean("initialized", false))
    } catch (_: Exception) {
      promise.resolve(false)
    }
  }

  override fun markInitialized() {
    try {
      reactApplicationContext.getSharedPreferences("appfastfly", 0)
        .edit().putBoolean("initialized", true).apply()
    } catch (_: Exception) {}
  }

  // --- Cache ---

  override fun getCachedParams(promise: Promise) {
    try {
      val json = reactApplicationContext.getSharedPreferences("appfastfly", 0)
        .getString("latest_params", null)
      if (json != null) {
        promise.resolve(jsonToWritableMap(JSONObject(json)))
      } else {
        promise.resolve(null)
      }
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  override fun setCachedParams(params: ReadableMap) {
    try {
      val jsonObj = JSONObject(params.toHashMap())
      reactApplicationContext.getSharedPreferences("appfastfly", 0)
        .edit().putString("latest_params", jsonObj.toString()).apply()
    } catch (_: Exception) {}
  }

  // --- Networking ---

  override fun initSession(promise: Promise) {
    try {
      val client = apiClient ?: run { promise.resolve(null); return }

      val fingerprint = AppfastflyFingerprint(reactApplicationContext)
      val fpMap = fingerprint.collect()
      val body = JSONObject(fpMap.toHashMap())
      body.put("platform", "android")

      // Collect clipboard token
      try {
        val clipboard = AppfastflyClipboard(reactApplicationContext)
        val token = clipboard.getToken("aff:")
        if (token != null) {
          body.put("clipboardToken", token)
          clipboard.clear()
        }
      } catch (_: Exception) {}

      // Collect install referrer then resolve
      fetchInstallReferrer { referrer ->
        if (!referrer.isNullOrEmpty()) {
          body.put("installReferrer", referrer)
        }
        doResolveCall(client, body, promise)
      }
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  override fun resolveLink(shortCode: String, promise: Promise) {
    try {
      val client = apiClient ?: run { promise.resolve(null); return }
      if (shortCode.isEmpty()) { promise.resolve(null); return }

      val body = JSONObject()
      body.put("shortCode", shortCode)
      body.put("platform", "android")

      client.post("/api/v1/resolve", body, object : AppfastflyApiClient.PostCallback {
        override fun onResult(result: JSONObject?) {
          try {
            promise.resolve(result?.let { jsonToWritableMap(it) })
          } catch (_: Exception) {
            promise.resolve(null)
          }
        }
      })
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  override fun setUserIdentity(userId: String, promise: Promise) {
    try {
      val client = apiClient ?: run { promise.resolve(null); return }
      if (userId.isEmpty()) { promise.resolve(null); return }

      val deviceId = try {
        AppfastflyFingerprint(reactApplicationContext).collect().toHashMap()["deviceId"]?.toString() ?: ""
      } catch (_: Exception) { "" }

      val body = JSONObject().apply {
        put("deviceId", deviceId)
        put("platform", "android")
        put("userId", userId)
      }

      client.post("/api/v1/identity", body, object : AppfastflyApiClient.PostCallback {
        override fun onResult(result: JSONObject?) { promise.resolve(null) }
      })
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  override fun clearUserIdentity(promise: Promise) {
    try {
      val client = apiClient ?: run { promise.resolve(null); return }

      val deviceId = try {
        AppfastflyFingerprint(reactApplicationContext).collect().toHashMap()["deviceId"]?.toString() ?: ""
      } catch (_: Exception) { "" }

      val body = JSONObject().apply {
        put("deviceId", deviceId)
        put("platform", "android")
      }

      client.delete("/api/v1/identity", body, object : AppfastflyApiClient.DeleteCallback {
        override fun onResult(error: Exception?) { promise.resolve(null) }
      })
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  // --- Internal helpers ---

  private fun doResolveCall(client: AppfastflyApiClient, body: JSONObject, promise: Promise) {
    client.post("/api/v1/resolve", body, object : AppfastflyApiClient.PostCallback {
      override fun onResult(result: JSONObject?) {
        try {
          promise.resolve(result?.let { jsonToWritableMap(it) })
        } catch (_: Exception) {
          promise.resolve(null)
        }
      }
    })
  }

  /**
   * Fetch install referrer asynchronously, then invoke callback with the result.
   * On any error, callback receives null.
   */
  private fun fetchInstallReferrer(callback: (String?) -> Unit) {
    try {
      val referrer = AppfastflyInstallReferrer(reactApplicationContext)
      referrer.get(AppfastflyInstallReferrer.ReferrerCallback { result -> callback(result) })
    } catch (_: Exception) {
      callback(null)
    }
  }

  // --- Events ---

  override fun addListener(eventName: String) {
    hasListeners = true
    cachedEvent?.let {
      sendEvent(eventName, it)
      cachedEvent = null
    }
  }

  override fun removeListeners(count: Double) {
    hasListeners = false
  }

  private fun sendEvent(eventName: String, params: WritableMap) {
    try {
      if (hasListeners) {
        reactApplicationContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
          .emit(eventName, params)
      } else {
        cachedEvent = params
      }
    } catch (_: Exception) {}
  }

  private fun getMetaData(key: String): String? {
    return try {
      val ai = reactApplicationContext.packageManager.getApplicationInfo(
        reactApplicationContext.packageName,
        PackageManager.GET_META_DATA
      )
      ai.metaData?.getString(key)
    } catch (_: Exception) {
      null
    }
  }

  private fun jsonToWritableMap(json: JSONObject): WritableMap {
    val map = Arguments.createMap()
    try {
      val keys = json.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        try {
          when (val value = json.get(key)) {
            is String -> map.putString(key, value)
            is Int -> map.putInt(key, value)
            is Long -> map.putDouble(key, value.toDouble())
            is Double -> map.putDouble(key, value)
            is Boolean -> map.putBoolean(key, value)
            is JSONObject -> map.putMap(key, jsonToWritableMap(value))
            JSONObject.NULL -> map.putNull(key)
            else -> map.putString(key, value.toString())
          }
        } catch (_: Exception) {}
      }
    } catch (_: Exception) {}
    return map
  }
}
