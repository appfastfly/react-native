package com.appfastfly.deeplink

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.facebook.react.bridge.Promise

class AppfastflyInstallReferrer(private val context: Context) {

  fun get(promise: Promise) {
    val client = InstallReferrerClient.newBuilder(context).build()

    client.startConnection(object : InstallReferrerStateListener {
      override fun onInstallReferrerSetupFinished(responseCode: Int) {
        when (responseCode) {
          InstallReferrerClient.InstallReferrerResponse.OK -> {
            try {
              val response = client.installReferrer
              val referrer = response.installReferrer
              promise.resolve(referrer)
            } catch (e: Exception) {
              promise.resolve(null)
            } finally {
              client.endConnection()
            }
          }
          else -> {
            promise.resolve(null)
            client.endConnection()
          }
        }
      }

      override fun onInstallReferrerServiceDisconnected() {
        promise.resolve(null)
      }
    })
  }
}
