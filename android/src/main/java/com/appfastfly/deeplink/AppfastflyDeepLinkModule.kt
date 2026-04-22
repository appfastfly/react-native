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

    // Initialize native HTTP client
    try {
      apiClient = AppfastflyApiClient(serviceUrl, apiKey)
    } catch (_: Exception) {
      android.util.Log.w("Appfastfly", "Failed to initialize API client")
    }

    if (serviceUrl.isEmpty() || apiKey.isEmpty()) {
      android.util.Log.w("Appfastfly", "Missing com.appfastfly.SERVICE_URL or com.appfastfly.API_KEY in AndroidManifest.xml")
    }

    pendingUrl?.let {
      emitDeepLinkUrl(it)
      pendingUrl = null
    }

    try {
      reactApplicationContext.currentActivity?.intent?.data?.toString()?.let { url ->
        emitDeepLinkUrl(url)
      }
    } catch (_: Exception) {
      // Silently ignore
    }
  }

  override fun invalidate() {
    try {
      reactApplicationContext.removeActivityEventListener(this)
    } catch (_: Exception) {}
    instance = null
    super.invalidate()
  }

  override fun onNewIntent(intent: Intent) {
    try {
      val url = intent.data?.toString() ?: return
      emitDeepLinkUrl(url)
    } catch (_: Exception) {
      // Silently ignore
    }
  }

  override fun onActivityResult(
    activity: Activity,
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {}

  private fun emitDeepLinkUrl(url: String) {
    try {
      val event = Arguments.createMap().apply {
        putString("url", url)
      }
      sendEvent("onDeepLink", event)
    } catch (_: Exception) {
      // Silently ignore
    }
  }

  // --- Existing Spec methods ---

  override fun getConfig(promise: Promise) {
    try {
      val map = Arguments.createMap()
      map.putString("serviceUrl", serviceUrl)
      map.putString("apiKey", apiKey)
      promise.resolve(map)
    } catch (_: Exception) {
      val map = Arguments.createMap()
      map.putString("serviceUrl", "")
      map.putString("apiKey", "")
      promise.resolve(map)
    }
  }

  override fun getDeviceFingerprint(promise: Promise) {
    try {
      val fingerprint = AppfastflyFingerprint(reactApplicationContext)
      val result = fingerprint.collect()
      promise.resolve(result)
    } catch (_: Exception) {
      promise.resolve(Arguments.createMap())
    }
  }

  override fun getClipboardToken(prefix: String, promise: Promise) {
    try {
      val clipboard = AppfastflyClipboard(reactApplicationContext)
      val token = clipboard.getToken(prefix)
      promise.resolve(token)
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  override fun clearClipboard() {
    try {
      val clipboard = AppfastflyClipboard(reactApplicationContext)
      clipboard.clear()
    } catch (_: Exception) {
      // Silently ignore
    }
  }

  override fun getInstallReferrer(promise: Promise) {
    try {
      val referrer = AppfastflyInstallReferrer(reactApplicationContext)
      referrer.get(promise)
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  override fun isFirstLaunch(promise: Promise) {
    try {
      val prefs = reactApplicationContext.getSharedPreferences("appfastfly", 0)
      val initialized = prefs.getBoolean("initialized", false)
      promise.resolve(!initialized)
    } catch (_: Exception) {
      promise.resolve(false)
    }
  }

  override fun markInitialized() {
    try {
      val prefs = reactApplicationContext.getSharedPreferences("appfastfly", 0)
      prefs.edit().putBoolean("initialized", true).apply()
    } catch (_: Exception) {
      // Silently ignore
    }
  }

  override fun getCachedParams(promise: Promise) {
    try {
      val prefs = reactApplicationContext.getSharedPreferences("appfastfly", 0)
      val json = prefs.getString("latest_params", null)
      if (json != null) {
        val map = jsonToWritableMap(JSONObject(json))
        promise.resolve(map)
      } else {
        promise.resolve(null)
      }
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  override fun setCachedParams(params: ReadableMap) {
    try {
      val prefs = reactApplicationContext.getSharedPreferences("appfastfly", 0)
      val jsonObj = JSONObject(params.toHashMap())
      prefs.edit().putString("latest_params", jsonObj.toString()).apply()
    } catch (_: Exception) {
      // Silently ignore
    }
  }

  // --- Native networking methods (NEW) ---

  override fun initSession(promise: Promise) {
    try {
      val client = apiClient
      if (client == null) {
        promise.resolve(null)
        return
      }

      val fingerprint = AppfastflyFingerprint(reactApplicationContext)
      val fpMap = fingerprint.collect()
      val body = JSONObject(fpMap.toHashMap())
      body.put("platform", "android")

      // Collect clipboard token
      try {
        val clipboard = AppfastflyClipboard(reactApplicationContext)
        val clipboardToken = clipboard.getToken("aff:")
        if (clipboardToken != null) {
          body.put("clipboardToken", clipboardToken)
          clipboard.clear()
        }
      } catch (_: Exception) {
        // Clipboard access failed — continue without it
      }

      // Collect install referrer
      try {
        val referrer = AppfastflyInstallReferrer(reactApplicationContext)
        referrer.get(object : Promise {
          override fun resolve(value: Any?) {
            if (value is String && value.isNotEmpty()) {
              body.put("installReferrer", value)
            }
            // Now make the API call
            doResolveCall(client, body, promise)
          }
          override fun reject(code: String?, message: String?) { doResolveCall(client, body, promise) }
          override fun reject(code: String?, throwable: Throwable?) { doResolveCall(client, body, promise) }
          override fun reject(code: String?, message: String?, throwable: Throwable?) { doResolveCall(client, body, promise) }
          override fun reject(throwable: Throwable?) { doResolveCall(client, body, promise) }
          @Deprecated("Deprecated")
          override fun reject(throwable: Throwable?, userInfo: WritableMap?) { doResolveCall(client, body, promise) }
          @Deprecated("Deprecated")
          override fun reject(code: String?, userInfo: WritableMap?) { doResolveCall(client, body, promise) }
          @Deprecated("Deprecated")
          override fun reject(code: String?, throwable: Throwable?, userInfo: WritableMap?) { doResolveCall(client, body, promise) }
          @Deprecated("Deprecated")
          override fun reject(code: String?, message: String?, userInfo: WritableMap?) { doResolveCall(client, body, promise) }
          @Deprecated("Deprecated")
          override fun reject(code: String?, message: String?, throwable: Throwable?, userInfo: WritableMap?) { doResolveCall(client, body, promise) }
        })
      } catch (_: Exception) {
        // Install referrer failed — continue without it
        doResolveCall(client, body, promise)
      }
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  private fun doResolveCall(client: AppfastflyApiClient, body: JSONObject, promise: Promise) {
    client.post("/api/v1/resolve", body, object : AppfastflyApiClient.PostCallback {
      override fun onResult(result: JSONObject?) {
        try {
          if (result != null) {
            promise.resolve(jsonToWritableMap(result))
          } else {
            promise.resolve(null)
          }
        } catch (_: Exception) {
          promise.resolve(null)
        }
      }
    })
  }

  override fun resolveLink(shortCode: String, promise: Promise) {
    try {
      val client = apiClient
      if (client == null || shortCode.isEmpty()) {
        promise.resolve(null)
        return
      }

      val body = JSONObject()
      body.put("shortCode", shortCode)
      body.put("platform", "android")

      client.post("/api/v1/resolve", body, object : AppfastflyApiClient.PostCallback {
        override fun onResult(result: JSONObject?) {
          try {
            if (result != null) {
              promise.resolve(jsonToWritableMap(result))
            } else {
              promise.resolve(null)
            }
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
      val client = apiClient
      if (client == null || userId.isEmpty()) {
        promise.resolve(null)
        return
      }

      val fingerprint = AppfastflyFingerprint(reactApplicationContext)
      val fpMap = fingerprint.collect()
      val deviceId = fpMap.toHashMap()["deviceId"]?.toString() ?: ""

      val body = JSONObject()
      body.put("deviceId", deviceId)
      body.put("platform", "android")
      body.put("userId", userId)

      client.post("/api/v1/identity", body, object : AppfastflyApiClient.PostCallback {
        override fun onResult(result: JSONObject?) {
          promise.resolve(null)
        }
      })
    } catch (_: Exception) {
      promise.resolve(null)
    }
  }

  override fun clearUserIdentity(promise: Promise) {
    try {
      val client = apiClient
      if (client == null) {
        promise.resolve(null)
        return
      }

      val fingerprint = AppfastflyFingerprint(reactApplicationContext)
      val fpMap = fingerprint.collect()
      val deviceId = fpMap.toHashMap()["deviceId"]?.toString() ?: ""

      val body = JSONObject()
      body.put("deviceId", deviceId)
      body.put("platform", "android")

      client.delete("/api/v1/identity", body, object : AppfastflyApiClient.DeleteCallback {
        override fun onResult(error: Exception?) {
          promise.resolve(null)
        }
      })
    } catch (_: Exception) {
      promise.resolve(null)
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
    } catch (_: Exception) {
      // Silently ignore if JS module not available
    }
  }

  // --- Helpers ---

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
        } catch (_: Exception) {
          // Skip this key on error
        }
      }
    } catch (_: Exception) {
      // Return partial map on error
    }
    return map
  }
}
