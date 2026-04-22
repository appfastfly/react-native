package com.appfastfly.deeplink

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Native HTTP client for Appfastfly API calls.
 * All methods are safe — they catch all exceptions and return null on error.
 * Networking runs on a dedicated background thread.
 */
class AppfastflyApiClient(
  private val serviceUrl: String,
  private val apiKey: String
) {

  private val executor = Executors.newSingleThreadExecutor()

  companion object {
    private const val TIMEOUT_MS = 15_000
  }

  interface PostCallback {
    fun onResult(result: JSONObject?)
  }

  interface DeleteCallback {
    fun onResult(error: Exception?)
  }

  /**
   * POST JSON to path. Callback called on background thread.
   * result is null on any error — never throws.
   */
  fun post(path: String, body: JSONObject, callback: PostCallback) {
    executor.execute {
      try {
        val result = doPost(path, body)
        callback.onResult(result)
      } catch (_: Exception) {
        callback.onResult(null)
      }
    }
  }

  /**
   * DELETE JSON to path. Callback called on background thread.
   * error is null on success — never throws.
   */
  fun delete(path: String, body: JSONObject, callback: DeleteCallback) {
    executor.execute {
      try {
        doRequest("DELETE", path, body)
        callback.onResult(null)
      } catch (e: Exception) {
        callback.onResult(e)
      }
    }
  }

  private fun doPost(path: String, body: JSONObject): JSONObject? {
    val responseBody = doRequest("POST", path, body) ?: return null
    return try {
      JSONObject(responseBody)
    } catch (_: Exception) {
      null
    }
  }

  private fun doRequest(method: String, path: String, body: JSONObject): String? {
    var conn: HttpURLConnection? = null
    try {
      val url = URL("$serviceUrl$path")
      conn = url.openConnection() as HttpURLConnection
      conn.requestMethod = method
      conn.connectTimeout = TIMEOUT_MS
      conn.readTimeout = TIMEOUT_MS
      conn.setRequestProperty("Content-Type", "application/json")
      if (apiKey.isNotEmpty()) {
        conn.setRequestProperty("X-API-Key", apiKey)
      }
      conn.doOutput = true

      OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
        writer.write(body.toString())
        writer.flush()
      }

      val responseCode = conn.responseCode
      if (responseCode !in 200..299) {
        return null
      }

      val inputStream = conn.inputStream ?: return null
      val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
      val result = reader.readText()
      reader.close()
      return result

    } catch (_: Exception) {
      return null
    } finally {
      try {
        conn?.disconnect()
      } catch (_: Exception) {
        // Ignore disconnect errors
      }
    }
  }
}
