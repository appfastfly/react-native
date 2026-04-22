package com.appfastfly.deeplink

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.facebook.react.bridge.Promise

class AppfastflyInstallReferrer(private val context: Context) {

  fun interface ReferrerCallback {
    fun onResult(referrer: String?)
  }

  fun get(promise: Promise) {
    get { referrer -> promise.resolve(referrer) }
  }

  fun get(callback: ReferrerCallback) {
    try {
      val client = InstallReferrerClient.newBuilder(context).build()

      client.startConnection(object : InstallReferrerStateListener {
        override fun onInstallReferrerSetupFinished(responseCode: Int) {
          when (responseCode) {
            InstallReferrerClient.InstallReferrerResponse.OK -> {
              try {
                val referrer = client.installReferrer.installReferrer
                callback.onResult(referrer)
              } catch (_: Exception) {
                callback.onResult(null)
              } finally {
                try { client.endConnection() } catch (_: Exception) {}
              }
            }
            else -> {
              callback.onResult(null)
              try { client.endConnection() } catch (_: Exception) {}
            }
          }
        }

        override fun onInstallReferrerServiceDisconnected() {
          callback.onResult(null)
        }
      })
    } catch (_: Exception) {
      callback.onResult(null)
    }
  }
}
