package org.thunderdog.challegram.voip

import org.json.JSONException
import org.json.JSONObject
import org.thunderdog.challegram.Log

/**
 * Created by grishka on 01.03.17.
 */
class VoIPServerConfig {
  companion object {
    private var config: JSONObject? = null

    @JvmStatic
    fun setConfig (json: String) {
      try {
        config = JSONObject(json)
        nativeSetConfig(json)
      } catch (x: JSONException) {
        Log.e(Log.TAG_VOIP, "Error parsing VoIP config", x)
      }
    }

    @JvmStatic
    fun getInt (key: String, fallback: Int): Int {
      return config!!.optInt(key, fallback)
    }

    @JvmStatic
    fun getDouble (key: String, fallback: Double): Double {
      return if (config != null) config!!.optDouble(key, fallback) else fallback
    }

    @JvmStatic
    fun getString (key: String, fallback: String): String {
      return if (config != null) config!!.optString(key, fallback) else fallback
    }

    @JvmStatic
    fun getBoolean (key: String, fallback: Boolean): Boolean {
      return if (config != null) config!!.optBoolean(key, fallback) else fallback
    }

    @JvmStatic
    private external fun nativeSetConfig (json: String)
  }
}
