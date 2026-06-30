/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * File created on 15/11/2016
 */
package org.thunderdog.challegram.unsorted

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.*
import android.os.Message
import android.view.Gravity
import androidx.annotation.AnyThread
import androidx.annotation.IntDef
import androidx.annotation.UiThread
import me.vkryl.core.*
import me.vkryl.core.lambda.CancellableRunnable
import me.vkryl.core.lambda.RunnableBool
import me.vkryl.core.lambda.RunnableData
import me.vkryl.core.reference.ReferenceList
import me.vkryl.core.reference.addReference
import me.vkryl.core.reference.removeReference
import me.vkryl.core.unit.BitUnit
import me.vkryl.core.unit.ByteUnit
import me.vkryl.core.util.Blob
import me.vkryl.core.util.Blob.Companion.readByte
import me.vkryl.core.util.Blob.Companion.readDouble
import me.vkryl.core.util.Blob.Companion.readFloat
import me.vkryl.core.util.Blob.Companion.readString
import me.vkryl.core.util.Blob.Companion.readVarint
import me.vkryl.core.util.Blob.Companion.sizeOf
import me.vkryl.core.util.Blob.Companion.writeDouble
import me.vkryl.core.util.Blob.Companion.writeFloat
import me.vkryl.core.util.BlobEntry
import me.vkryl.leveldb.LevelDB
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.*
import org.drinkmore.Tracer.onDatabaseError
import org.drinkmore.Tracer.onLaunchError
import org.drinkmore.Tracer.onTdlibFatalError
import org.thunderdog.challegram.BuildConfig
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.Log.LogFiles
import org.thunderdog.challegram.R
import org.thunderdog.challegram.U
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.config.Device
import org.thunderdog.challegram.core.Background
import org.thunderdog.challegram.core.BiometricAuthentication
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.core.Lang.SpanCreator
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.emoji.Emoji
import org.thunderdog.challegram.emoji.RecentEmoji
import org.thunderdog.challegram.emoji.RecentInfo
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.navigation.PlaybackSpeedLayout
import org.thunderdog.challegram.player.TGPlayerController
import org.thunderdog.challegram.telegram.*
import org.thunderdog.challegram.telegram.TdlibManager.AccountConfig
import org.thunderdog.challegram.telegram.TdlibUi.ImportedTheme
import org.thunderdog.challegram.theme.*
import org.thunderdog.challegram.theme.ThemeProperties.Companion.getName
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.util.*
import org.thunderdog.challegram.util.AppBuildInfo.Companion.maxBuiltInCommitDate
import org.thunderdog.challegram.util.AppBuildInfo.Companion.restoreFrom
import org.thunderdog.challegram.util.AppBuildInfo.Companion.restoreVersionCode
import org.thunderdog.challegram.util.Crash.AppBuildInfoRestorer
import tgx.td.MessageId
import tgx.td.assertChatSource_12b21238
import tgx.td.assertDeviceToken_de4a4f61
import tgx.td.assertMessageTopic_98b4a9a3
import tgx.td.assertProxyType_bc1a1076
import tgx.td.cacheKey
import tgx.td.equalsTo
import tgx.td.isSecret
import tgx.td.substring
import tgx.td.unsupported
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.ref.Reference
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * All app-related settings.
 * 
 * 
 * SharedPreferences is no longer used at all for the following reasons:
 * 1. Application launch speed;
 * 2. Storage usage;
 * 3. OEM-specific bugs.
 */
@Suppress("deprecation")
class Settings private constructor() {
    private var _settings: Int? = null
    private var _newSettings: Long? = null
    private var _experiments: Long? = null

    private var _nightMode: Int? = null
    private var _nightModeAutoLux: Float? = null
    private var _nightModeSchedule: Long? = null

    var preferredVideoLimit: VideoLimit? = null
        get() {
            if (field == null) {
                field = VideoLimit(pmc.getIntArray(KEY_VIDEO_LIMIT))
            }
            return field
        }
        set(videoLimit) {
            val data = if (videoLimit != null) videoLimit.toIntArray() else null
            field = videoLimit
            if (data != null) {
                pmc.putIntArray(KEY_VIDEO_LIMIT, data)
            } else {
                pmc.remove(KEY_VIDEO_LIMIT)
            }
        }

    private var _stickerMode: Int? = null
    private var _emojiMode: Int? = null

    private var _reactionAvatarsMode: Int? = null

    private var _autoUpdateMode: Int? = null

    private var _incognitoMode: Int? = null

    private var _tutorialFlags: Long? = null

    private var _badgeFlags: Int? = null

    private var _notificationFlags: Int? = null

    inner class TdlibLogSettings(private val settingsKey: String, private val maxSizeKey: String, private val verbosityKey: String) {
        fun disable() {
            setVerbosity(null, 0)
        }

        val isEnabled: Boolean
            get() = getVerbosity(null) > 0

        private var _settings: Int? = null
        private var _modules: MutableMap<String?, IntArray?>? = null

        private val settings: Int
            get() {
                if (_settings == null) {
                    _settings = pmc.getInt(
                        settingsKey,
                        if (UI.isTestLab()) FLAG_TDLIB_OTHER_ENABLE_ANDROID_LOG else 0
                    )
                }
                return _settings!!
            }

        private fun checkLogSetting(flag: Int): Boolean {
            return hasFlag(this.settings, flag)
        }

        private fun setLogSetting(flag: Int, enabled: Boolean): Boolean {
            val flags = this.settings
            val newFlags = setFlag(flags, flag, enabled)
            if (flags != newFlags) {
                _settings = newFlags
                pmc.putInt(settingsKey, newFlags)
                apply(false)
                return true
            }
            return false
        }

        fun needAndroidLog(): Boolean {
            return checkLogSetting(FLAG_TDLIB_OTHER_ENABLE_ANDROID_LOG)
        }

        fun setNeedAndroidLog(needTdlibAndroidLog: Boolean) {
            setLogSetting(FLAG_TDLIB_OTHER_ENABLE_ANDROID_LOG, needTdlibAndroidLog)
        }

        val logMaxFileSize: Long
            get() = getLong(maxSizeKey, DEFAULT_LOG_SIZE)

        fun setMaxFileSize(bytes: Long) {
            if (bytes == DEFAULT_LOG_SIZE) pmc.remove(maxSizeKey)
            else pmc.putLong(maxSizeKey, bytes)
            apply(false)
        }

        val modules: MutableList<String?>
            get() {
                var modules: MutableList<String?>
                try {
                    val logTags = Client.execute<LogTags>(GetLogTags())
                    val tags = logTags.tags
                    modules = java.util.ArrayList<String?>(tags.size + (if (_modules != null) _modules!!.size else 0))
                    Collections.addAll<String?>(modules, *tags)
                } catch (error: Client.ExecutionException) {
                    modules = java.util.ArrayList<String?>(if (_modules != null) _modules!!.size else 0)
                }
                if (_modules != null) {
                    for (key in _modules!!.keys) {
                        if (!modules.contains(key)) {
                            modules.add(key)
                        }
                    }
                }
                return modules
            }

        private fun setLogTagVerbosityLevel(module: String?, verbosityLevel: Int): Boolean {
            try {
                Client.execute<TdApi.Ok?>(SetLogTagVerbosityLevel(module, verbosityLevel))
                return true
            } catch (error: Client.ExecutionException) {
                return false
            }
        }

        private fun setLogVerbosityLevel(globalVerbosityLevel: Int): Boolean {
            try {
                Client.execute<TdApi.Ok?>(SetLogVerbosityLevel(globalVerbosityLevel))
                return true
            } catch (error: Client.ExecutionException) {
                return false
            }
        }

        fun getVerbosity(module: String?): Int {
            if (!isEmpty(module)) {
                val value = if (_modules != null) _modules!!.get(module) else null
                if (value != null) {
                    return value[0]
                } else {
                    val defaultLogVerbosity = queryLogVerbosityLevel(module)
                    _modules!!.put(module, intArrayOf(defaultLogVerbosity, defaultLogVerbosity))
                    return defaultLogVerbosity
                }
            }
            return queryLogVerbosityLevel(module)
        }

        fun getDefaultVerbosity(module: String): Int {
            val value = if (_modules != null) _modules!!.get(module) else null
            if (value == null) {
                val defaultLogVerbosity = queryLogVerbosityLevel(module)
                _modules!!.put(module, intArrayOf(defaultLogVerbosity, defaultLogVerbosity))
                return defaultLogVerbosity
            }
            return value[1]
        }

        fun setVerbosity(module: String?, verbosity: Int) {
            if (isEmpty(module)) {
                if (verbosity != DEFAULT_LOG_GLOBAL_VERBOSITY_LEVEL) {
                    putInt(verbosityKey, verbosity)
                } else {
                    remove(verbosityKey)
                }
                setLogVerbosityLevel(verbosity)
            } else {
                if (_modules == null) _modules = HashMap<String?, IntArray?>()
                var value = _modules!!.get(module)
                val defaultVerbosityLevel = if (value != null) value[1] else queryLogVerbosityLevel(module)
                val currentVerbosityLevel = if (value != null) value[0] else defaultVerbosityLevel
                if (verbosity != currentVerbosityLevel) {
                    try {
                        Client.execute<TdApi.Ok?>(SetLogTagVerbosityLevel(module, verbosity))

                        if (value != null) value[0] = verbosity
                        else _modules!!.put(module, intArrayOf(verbosity, defaultVerbosityLevel).also { value = it })
                        if (value!![0] == value[1]) remove(verbosityKey + "_" + module)
                        else putInt(verbosityKey + "_" + module, verbosity)
                    } catch (ignored: Client.ExecutionException) {
                    }
                }
            }
        }

        fun reset() {
            pmc.edit()
            setVerbosity(null, 0)
            if (_modules != null) {
                for (entry in _modules!!.entries) {
                    setVerbosity(entry.key, entry.value!![1])
                }
            }
            setMaxFileSize(DEFAULT_LOG_SIZE)
            pmc.apply()
        }

        private fun queryLogVerbosityLevel(module: String?): Int {
            try {
                val function = if (isEmpty(module)) GetLogVerbosityLevel() else GetLogTagVerbosityLevel(module)
                val logVerbosityLevel = Client.execute<LogVerbosityLevel>(function)
                return logVerbosityLevel.verbosityLevel
            } catch (error: Client.ExecutionException) {
                return TDLIB_LOG_VERBOSITY_UNKNOWN
            }
        }

        fun apply(async: Boolean) {
            if (UI.isTestLab()) return
            var globalVerbosityLevel: Int = DEFAULT_LOG_GLOBAL_VERBOSITY_LEVEL
            if (_modules == null) _modules = HashMap<String?, IntArray?>()
            for (entry in pmc.find(verbosityKey)) {
                val key = entry.key()
                var verbosityLevel = entry.asInt()
                if (verbosityKey.length == key.length) {
                    globalVerbosityLevel = max(0, verbosityLevel) // Can't be negative
                } else if (key.length > verbosityKey.length + 1) {
                    verbosityLevel = max(1, verbosityLevel) // At least error
                    val module = key.substring(verbosityKey.length + 1)
                    val value = _modules!!.get(module)
                    val defaultVerbosityLevel = if (value != null) value[1] else queryLogVerbosityLevel(module)
                    if (setLogTagVerbosityLevel(module, verbosityLevel)) {
                        if (value != null) {
                            value[0] = verbosityLevel
                        } else {
                            _modules!!.put(module, intArrayOf(verbosityLevel, defaultVerbosityLevel))
                        }
                    }
                }
            }
            setLogVerbosityLevel(globalVerbosityLevel)

            val stream: TdApi.LogStream?
            if (needAndroidLog()) {
                stream = LogStreamDefault()
            } else {
                val logFile = TdlibManager.getLogFile(false)
                if (logFile != null) {
                    stream = LogStreamFile(logFile.getPath(), this.logMaxFileSize, false)
                } else {
                    stream = LogStreamEmpty()
                }
            }
            try {
                Client.execute<TdApi.Ok?>(SetLogStream(stream))
            } catch (error: Client.ExecutionException) {
                val act = Runnable {
                    onTdlibFatalError(null, SetLogStream::class.java, error.error, RuntimeException().getStackTrace())
                }
                if (async) {
                    UI.post(act)
                } else {
                    act.run()
                }
            }
        }
    }

    // TDLib
    val logSettings: TdlibLogSettings = TdlibLogSettings(KEY_TDLIB_OTHER, KEY_TDLIB_LOG_SIZE, KEY_TDLIB_VERBOSITY)

    fun getTdlibLogSettings(): TdlibLogSettings = logSettings

    private var _chatFontSize: Float? = null

    private var _preferredAudioPlaybackMode: Int? = null

    private var _mapProviderType: Int? = null
    private var _mapProviderTypeCloud: Int? = null

    private class ScheduleHandler(private val context: Settings) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            context.handleMessage(msg)
        }
    }

    private val handler = ScheduleHandler(this)

    fun forceRevokeAllFeaturePrompts() {
        pmc
            .putLong(KEY_FEATURES, 0 /*no features were available*/)
            .remove(KEY_FEATURES_ADDED_NOTIFICATIONS)
            .remove(KEY_FEATURES_REMOVED_NOTIFICATIONS)
    }

    private fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_DISPATCH_NIGHT_SCHEDULE_CHECK -> checkNightModeScheduler(false)
        }
    }

    fun scheduleNightScheduleCheck(timeTillNextChange: Long) {
        handler.sendMessageDelayed(Message.obtain(handler, MSG_DISPATCH_NIGHT_SCHEDULE_CHECK), timeTillNextChange)
    }

    fun cancelNightScheduleCheck() {
        handler.removeMessages(MSG_DISPATCH_NIGHT_SCHEDULE_CHECK)
    }

    // Modification listeners
    fun reset() {
        this.tutorialFlags = 0
        pmc.removeByPrefix(KEY_TUTORIAL_PSA)
        resetOther()
    }

    fun edit(): LevelDB {
        return pmc.edit()
    }

    fun remove(key: String) {
        pmc.remove(key)
    }

    fun putLong(key: String, value: Long) {
        pmc.putLong(key, value)
    }

    fun getLong(key: String, defValue: Long): Long {
        return pmc.getLong(key, defValue)
    }

    fun getLongArray(key: String): LongArray? {
        return pmc.getLongArray(key)
    }

    fun putLongArray(key: String, value: LongArray) {
        pmc.putLongArray(key, value)
    }

    fun putInt(key: String, value: Int) {
        pmc.putInt(key, value)
    }

    fun getInt(key: String, defValue: Int): Int {
        return pmc.getInt(key, defValue)
    }

    fun getIntArray(key: String): IntArray? {
        return pmc.getIntArray(key)
    }

    fun putIntArray(key: String, value: IntArray) {
        pmc.putIntArray(key, value)
    }

    fun putFloat(key: String, value: Float) {
        pmc.putFloat(key, value).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        pmc.putBoolean(key, value)
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        return pmc.getBoolean(key, defValue)
    }

    fun putVoid(key: String) {
        pmc.putVoid(key)
    }

    fun containsKey(key: String): Boolean {
        return pmc.contains(key)
    }

    fun putString(key: String, value: String) {
        pmc.putString(key, value)
    }

    fun getString(key: String, defValue: String?): String? {
        return pmc.getString(key, defValue)
    }

    fun removeByPrefix(prefix: String, editor: SharedPreferences.Editor?) {
        pmc.removeByPrefix(prefix) // editor
    }

    fun removeByAnyPrefix(prefixes: Array<String?>, editor: SharedPreferences.Editor?) {
        pmc.removeByAnyPrefix(*prefixes) // , editor
    }

    private fun resetOther() {
        remove(KEY_OTHER)
        this._settings = makeDefaultSettings()
    }

    @Deprecated("")
    private fun upgradeSharedPreferences(prefs: SharedPreferences, editor: SharedPreferences.Editor, version: Int) {
        when (version) {
            LEGACY_VERSION_1 -> {
                // Adding video note autodownload setting if voice autodownload is enabled
                TdlibFilesManager.upgradeSharedPreferences(prefs, editor)
            }

            LEGACY_VERSION_2 -> {}
            LEGACY_VERSION_3, LEGACY_VERSION_6 -> {
                var otherSettings = prefs.getInt(KEY_OTHER, -1)
                if (otherSettings != -1 && (otherSettings and FLAG_OTHER_NO_CHAT_QUICK_SHARE) == 0) {
                    otherSettings = otherSettings or FLAG_OTHER_NO_CHAT_QUICK_SHARE
                    prefs.edit().putInt(KEY_OTHER, otherSettings).apply()
                }
            }

            LEGACY_VERSION_4 -> {
                /*
                       public static final float MAX_NIGHT_LUX_MULTIPLY_DEFAULT = 1.0f;
                       public static final float[] MAX_NIGHT_LUX_MULTIPLIES = {
                         0.5f,
                         MAX_NIGHT_LUX_MULTIPLY_DEFAULT,
                         1.5f,
                         2f,
                         5f,
                         10f,
                         20f
                       };
                       private float maxNightLux;
                       private float maxNightLuxMultiply;
                       * */
                val maxNightLux = prefs.getFloat(KEY_MAX_NIGHT_LUX, MAX_NIGHT_LUX_DEFAULT)
                val maxNightLuxMultiply = max(0.5f, min(20f, prefs.getFloat(KEY_MAX_NIGHT_LUX_MULTIPLY, 1f)))

                val previousNightLux = maxNightLux * maxNightLuxMultiply
                if (previousNightLux != MAX_NIGHT_LUX_DEFAULT) {
                    editor.putFloat(KEY_MAX_NIGHT_LUX, previousNightLux)
                }

                editor.remove(KEY_MAX_NIGHT_LUX_MULTIPLY)

                if (!prefs.contains(KEY_OTHER)) {
                    return
                }

                val otherSettings = prefs.getInt(KEY_OTHER, 0)

                var newOtherSettings = otherSettings

                if ((otherSettings and DISABLED_FLAG_OTHER_USE_AUTO_NIGHT_MODE) != 0) {
                    editor.putInt(KEY_NIGHT_MODE, NIGHT_MODE_AUTO)
                }

                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_NEED_RAISE_TO_SPEAK.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_AUTODOWNLOAD_IN_BACKGROUND.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_DEFAULT_CRASH_MANAGER.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_USE_VOICE_DRAFT.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_NO_CHAT_SWIPES.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_USE_DIFFERENT_MEDIA_PICKER_LAYOUT.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_USE_AUTO_NIGHT_MODE.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_NO_ONLINE.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_ENABLE_RAISE_TO_SPEAK.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_GROUP_MEDIA.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_DISABLE_CUSTOM_TEXT_ACTIONS.inv()
                newOtherSettings = newOtherSettings and DISABLED_FLAG_OTHER_SHOW_FORWARD_OPTIONS.inv()

                if (newOtherSettings != otherSettings) {
                    editor.putInt(KEY_OTHER, newOtherSettings)
                }
            }

            LEGACY_VERSION_5 -> {
                editor.remove("record_id")
            }

            LEGACY_VERSION_7 -> {
                editor.remove("settings_oreo_fix")
            }

            LEGACY_VERSION_8 -> {
                val userId = prefs.getInt(KEY_PUSH_USER_ID, 0)
                val userIdsStr = prefs.getString(KEY_PUSH_USER_IDS, null)
                if (userId != 0) {
                    editor.remove(KEY_PUSH_USER_ID)
                    if (isEmpty(userIdsStr)) {
                        editor.putString(KEY_PUSH_USER_IDS, userId.toString())
                    }
                }
            }

            LEGACY_VERSION_9 -> {
                if (prefs.contains(KEY_OTHER)) {
                    var flags = prefs.getInt(KEY_OTHER, 0)
                    if ((flags and DISABLED_FLAG_OTHER_DISABLE_CALLS_PROXY) != 0) {
                        flags = flags and DISABLED_FLAG_OTHER_DISABLE_CALLS_PROXY.inv()
                        editor.putInt(KEY_OTHER, flags)
                    }
                }
            }
        }
    }

    var _chatDoNotTranslateLanguages: HashMap<String?, Boolean?>? = null

    private fun loadNotTranslatableLanguages() {
        if (_chatDoNotTranslateLanguages != null) return
        _chatDoNotTranslateLanguages = HashMap<String?, Boolean?>()
        val result = pmc.getStringArray(KEY_CHAT_DO_NOT_TRANSLATE_LIST)
        if (result == null) return
        for (lang in result) {
            _chatDoNotTranslateLanguages!!.put(lang, true)
        }
    }

    private fun saveNotTranslatableLanguages() {
        pmc.putStringArray(KEY_CHAT_DO_NOT_TRANSLATE_LIST, this.allNotTranslatableLanguages!!)
    }

    val allNotTranslatableLanguages: Array<String?>?
        get() {
            loadNotTranslatableLanguages()
            val list = StringList(_chatDoNotTranslateLanguages!!.size)
            for (entry in _chatDoNotTranslateLanguages!!.entries) {
                list.append(entry.key)
            }
            return list.get()
        }

    fun isNotTranslatableLanguage(lang: String?): Boolean {
        if (this.chatDoNotTranslateMode == DO_NOT_TRANSLATE_MODE_APP_LANG) {
            return getLanguage()!!.packInfo.pluralCode.equalsOrBothEmpty(lang)
        } else {
            return containsInNotTranslatableLanguageList(lang)
        }
    }

    fun containsInNotTranslatableLanguageList(lang: String?): Boolean {
        loadNotTranslatableLanguages()
        if (lang == null) return false
        return _chatDoNotTranslateLanguages!!.containsKey(lang)
    }

    fun setIsNotTranslatableLanguage(lang: String?, isNotTranslatable: Boolean) {
        if (isNotTranslatable == containsInNotTranslatableLanguageList(lang)) return
        if (isNotTranslatable) {
            _chatDoNotTranslateLanguages!!.put(lang, true)
        } else {
            _chatDoNotTranslateLanguages!!.remove(lang)
        }
        saveNotTranslatableLanguages()
    }


    private var _chatDoNotTranslateMode: Int? = null

    var chatDoNotTranslateMode: Int
        get() {
            if (_chatDoNotTranslateMode == null) {
                _chatDoNotTranslateMode = pmc.getInt(
                    KEY_CHAT_DO_NOT_TRANSLATE_MODE,
                    DO_NOT_TRANSLATE_MODE_APP_LANG
                )
            }
            return _chatDoNotTranslateMode!!
        }
        set(mode) {
            if (_chatDoNotTranslateMode != mode) {
                pmc.putInt(KEY_CHAT_DO_NOT_TRANSLATE_MODE, mode.also { _chatDoNotTranslateMode = it })
            }
        }

    private var _chatTranslateMode: Int? = null

    var chatTranslateMode: Int
        get() {
            if (_chatTranslateMode == null) {
                _chatTranslateMode = pmc.getInt(
                    KEY_CHAT_TRANSLATE_MODE,
                    TRANSLATE_MODE_POPUP
                )
            }
            return _chatTranslateMode!!
        }
        set(mode) {
            if (_chatTranslateMode != mode) {
                pmc.putInt(KEY_CHAT_TRANSLATE_MODE, mode.also { _chatTranslateMode = it })
            }
        }

    fun setTranslateLanguageRecents(recents: Array<String?>) {
        pmc.putStringArray(KEY_CHAT_TRANSLATE_RECENTS, recents)
    }

    fun setTranslateLanguageRecents(recents: MutableList<String?>) {
        val out = arrayOfNulls<String>(recents.size)
        var i = 0
        for (recent in recents) {
            out[i++] = recent
        }
        setTranslateLanguageRecents(out)
    }

    val translateLanguageRecents: ArrayList<String?>
        get() {
            val result = pmc.getStringArray(KEY_CHAT_TRANSLATE_RECENTS)
            if (result != null) {
                return java.util.ArrayList<String?>(Arrays.asList<String?>(*result))
            }
            return java.util.ArrayList<String?>()
        }

    fun clearTranslateLanguageRecents() {
        pmc.remove(KEY_CHAT_TRANSLATE_RECENTS)
    }

    interface ChatListModeChangeListener {
        fun onChatListModeChanged(newChatListMode: Int)
    }

    private var _chatListMode: Int? = null
    private var chatListModeListeners: ReferenceList<ChatListModeChangeListener?>? = null

    var chatListMode: Int
        get() {
            if (_chatListMode == null) {
                val defaultMode: Int = CHAT_MODE_3LINE // TODO determine based on display settings
                _chatListMode = pmc.getInt(KEY_CHAT_LIST_MODE, defaultMode)
            }
            return _chatListMode!!
        }
        set(mode) {
            if (_chatListMode != mode) {
                pmc.putInt(KEY_CHAT_LIST_MODE, mode.also { _chatListMode = it })
                if (chatListModeListeners != null) {
                    for (listener in chatListModeListeners) {
                        listener!!.onChatListModeChanged(mode)
                    }
                }
            }
        }

    fun addChatListModeListener(listener: ChatListModeChangeListener?) {
        if (chatListModeListeners == null) {
            chatListModeListeners = ReferenceList<ChatListModeChangeListener?>()
        }
        chatListModeListeners!!.add(listener)
    }

    fun removeChatListModeListener(listModeChangeListener: ChatListModeChangeListener?) {
        if (chatListModeListeners != null) chatListModeListeners!!.remove(listModeChangeListener)
    }


    interface ChatFolderSettingsListener {
        fun onChatFolderOptionsChanged(@ChatFolderOptions newOptions: Int) {}
        fun onChatFolderStyleChanged(@ChatFolderStyle newStyle: Int) {}
    }

    private val chatFolderSettingsListeners = ReferenceList<ChatFolderSettingsListener?>()

    fun addChatFolderSettingsListener(listener: ChatFolderSettingsListener?) {
        chatFolderSettingsListeners.add(listener)
    }

    fun removeChatFolderSettingsListener(listener: ChatFolderSettingsListener?) {
        chatFolderSettingsListeners.remove(listener)
    }

    private var _chatFolderOptions: Int? = null
    private var _chatFolderStyle: Int? = null

    @get:ChatFolderOptions
    var chatFolderOptions: Int
        get() {
            if (_chatFolderOptions == null) {
                _chatFolderOptions =
                    pmc.getInt(KEY_CHAT_FOLDER_OPTIONS, TdlibSettingsManager.DEFAULT_CHAT_FOLDER_OPTIONS)
            }
            return _chatFolderOptions!!
        }
        set(options) {
            if (_chatFolderOptions != options) {
                if (options == TdlibSettingsManager.DEFAULT_CHAT_FOLDER_OPTIONS) {
                    pmc.remove(KEY_CHAT_FOLDER_OPTIONS)
                } else {
                    pmc.putInt(KEY_CHAT_FOLDER_OPTIONS, options)
                }
                _chatFolderOptions = options
                for (listener in chatFolderSettingsListeners) {
                    listener!!.onChatFolderOptionsChanged(options)
                }
            }
        }

    @get:ChatFolderStyle
    var chatFolderStyle: Int
        get() {
            if (_chatFolderStyle == null) {
                _chatFolderStyle =
                    pmc.getInt(KEY_CHAT_FOLDER_STYLE, TdlibSettingsManager.DEFAULT_CHAT_FOLDER_STYLE)
            }
            return _chatFolderStyle!!
        }
        set(style) {
            if (_chatFolderStyle != style) {
                if (style == TdlibSettingsManager.DEFAULT_CHAT_FOLDER_STYLE) {
                    pmc.remove(KEY_CHAT_FOLDER_STYLE)
                } else {
                    pmc.putInt(KEY_CHAT_FOLDER_STYLE, style)
                }
                _chatFolderStyle = style
                for (listener in chatFolderSettingsListeners) {
                    listener!!.onChatFolderStyleChanged(style)
                }
            }
        }

    private fun makeDefaultNewSettings(): Long {
        val settings: Long = 0

        return settings
    }

    private val newSettings: Long
        get() {
            if (_newSettings == null) _newSettings = pmc.getLong(KEY_OTHER_NEW, makeDefaultNewSettings())
            return _newSettings!!
        }

    fun getNewSetting(key: Long): Boolean {
        return hasFlag(this.newSettings, key)
    }

    private fun setNewSettings(newSettings: Long): Boolean {
        val oldSettings = this.newSettings
        if (oldSettings != newSettings) {
            this._newSettings = newSettings
            pmc.putLong(KEY_OTHER_NEW, newSettings)
            if (newSettingsListeners != null) {
                for (listener in newSettingsListeners) {
                    listener!!.onSettingsChanged(newSettings, oldSettings)
                }
            }
            return true
        }
        return false
    }

    private val experiments: Long
        get() {
            if (_experiments == null) {
                _experiments = pmc.getLong(
                    KEY_EXPERIMENTS,
                    makeDefaultExperiments()
                )
            }
            return _experiments!!
        }

    fun isExperimentEnabled(key: Long): Boolean {
        val experiments = this.experiments
        return hasAllFlags(experiments, EXPERIMENT_FLAG_ALLOW_EXPERIMENTS or key)
    }

    fun setExperimentEnabled(key: Long, enabled: Boolean): Boolean {
        val oldExperiments = this.experiments
        val newExperiments = setFlag(oldExperiments, key, enabled)
        if (oldExperiments != newExperiments) {
            this._experiments = newExperiments
            pmc.putLong(KEY_EXPERIMENTS, newExperiments)
            return true
        }
        return false
    }

    interface SettingsChangeListener {
        fun onSettingsChanged(newSettings: Long, oldSettings: Long)
    }

    private var newSettingsListeners: ReferenceList<SettingsChangeListener?>? = null

    fun addNewSettingsListener(listener: SettingsChangeListener?) {
        if (newSettingsListeners == null) newSettingsListeners = ReferenceList<SettingsChangeListener?>()
        newSettingsListeners!!.add(listener)
    }

    fun removeNewSettingsListener(listener: SettingsChangeListener?) {
        if (newSettingsListeners != null) {
            newSettingsListeners!!.remove(listener)
        }
    }

    fun setNewSetting(key: Long, value: Boolean): Boolean {
        return setNewSettings(setFlag(this.newSettings, key, value))
    }

    fun toggleNewSetting(key: Long): Boolean {
        val enabled = !getNewSetting(key)
        setNewSetting(key, enabled)
        return enabled
    }

    private val settings: Int
        get() {
            if (_settings == null) _settings = pmc.getInt(
                KEY_OTHER,
                makeDefaultSettings()
            )
            return _settings!!
        }

    private fun setSettings(newSettings: Int): Boolean {
        if (this.settings != newSettings) {
            this._settings = newSettings
            pmc.putInt(KEY_OTHER, newSettings)
            return true
        }
        return false
    }

    private fun setSetting(flag: Int, value: Boolean): Boolean {
        return setSettings(setFlag(this.settings, flag, value))
    }

    private fun setNegativeSetting(flag: Int, value: Boolean): Boolean {
        return setSetting(flag, !value)
    }

    private fun checkSetting(flag: Int): Boolean {
        return hasFlag(this.settings, flag)
    }

    private fun checkNegativeSetting(flag: Int): Boolean {
        return !checkSetting(flag)
    }

    /*private void saveOtherSettings () {
    int flags = 0;
    if (autoplayGIFs) {
      flags |= FLAG_OTHER_AUTOPLAY_GIFS;
    }
    if (saveEditedMediaToGallery) {
      flags |= FLAG_OTHER_SAVE_TO_GALLERY;
    }
    if (reduceMotion) {
      flags |= FLAG_OTHER_REDUCE_MOTION;
    }
    if (useSystemEmoji) {
      flags |= FLAG_OTHER_USE_SYSTEM_EMOJI;
    }
    if (dontReadMessages) {
      flags |= FLAG_OTHER_DONT_READ_MESSAGES;
    }
    if (disableChatQuickShare) {
      flags |= FLAG_OTHER_NO_CHAT_QUICK_SHARE;
    }
    if (disableChatQuickReply) {
      flags |= FLAG_OTHER_NO_CHAT_QUICK_REPLY;
    }
    if (sendByEnter) {
      flags |= FLAG_OTHER_SEND_BY_ENTER;
    }
    if (hideChatKeyboard) {
      flags |= FLAG_OTHER_HIDE_CHAT_KEYBOARD;
    }
    if (needForwardOptions) {
      flags |= FLAG_OTHER_SHOW_FORWARD_OPTIONS;
    }
    if (!needPreviewChatsOnHold) {
      flags |= FLAG_OTHER_DISABLE_PREVIEW_CHATS_ON_HOLD;
    }
    if (!useInAppBrowser) {
      flags |= FLAG_OTHER_DISABLE_INAPP_BROWSER;
    }
    if (!useCustomTextActions) {
      flags |= FLAG_OTHER_DISABLE_CUSTOM_TEXT_ACTIONS;
    }
    if (needOutboundCallsPrompt) {
      flags |= FLAG_OTHER_OUTBOUND_CALLS_PROMPT;
    }
    if (!useCustomVibrations) {
      flags |= FLAG_OTHER_DISABLE_CUSTOM_VIBRATIONS;
    }
    if (force169Camera) {
      flags |= FLAG_OTHER_CAMERA_FORCE_16_9;
    }
    if (preferVideoMode) {
      flags |= FLAG_OTHER_PREFER_VIDEO_MODE;
    }
    if (needHqRoundVideos) {
      flags |= FLAG_OTHER_HQ_ROUND_VIDEOS;
    }
    if (startRoundRear) {
      flags |= FLAG_OTHER_START_ROUND_REAR;
    }
    if (disableSecretLinkPreviews) {
      flags |= FLAG_OTHER_DISABLE_SECRET_LINK_PREVIEWS;
    }
    if (accountListOpened) {
      flags |= FLAG_OTHER_ACCOUNT_LIST_OPENED;
    }
    if (forceArabicNumbers) {
      flags |= FLAG_OTHER_FORCE_ARABIC_NUMBERS;
    }
    if (chatFontSizeScaling) {
      flags |= FLAG_OTHER_FONT_SCALING;
    }
    if (rememberAlbumSetting) {
      flags |= FLAG_OTHER_REMEMBER_ALBUM_SETTING;
    }
    if (needGroupMedia) {
      flags |= FLAG_OTHER_NEED_GROUP_MEDIA;
    }
    if (separateMediaTab) {
      flags |= FLAG_OTHER_SEPARATE_MEDIA_TAB;
    }
    if (splitChatNotifications) {
      flags |= FLAG_OTHER_SPLIT_CHAT_NOTIFICATIONS;
    }
    putInt(KEY_OTHER, flags);
  }*/
    private val pmc: LevelDB

    fun pmc(): LevelDB {
        return pmc
    }

    private var ignoreFurtherAccountConfigUpgrades = false

    private fun upgradePmc(pmc: LevelDB, editor: SharedPreferences.Editor, version: Int) {
        when (version) {
            VERSION_10 -> {}
            VERSION_11 -> {
                val prefixes: MutableList<String?> = java.util.ArrayList<String?>()
                val accountNum = TdlibManager.readAccountNum()
                var accountId = 0
                while (accountId < accountNum) {
                    prefixes.add(if (accountId != 0) accountId.toString() + "_" + KEY_SILENT_CHANNEL_PREFIX else KEY_SILENT_CHANNEL_PREFIX)
                    accountId++
                }
                pmc.removeByAnyPrefix(prefixes)
            }

            VERSION_12 -> {
                val mode = pmc.getInt(Passcode.KEY_PASSCODE_MODE, Passcode.MODE_NONE)
                if (mode == Passcode.MODE_BIOMETRICS) {
                    val passcodeHash = pmc.getString(Passcode.KEY_PASSCODE_HASH, null)
                    if (passcodeHash != null) {
                        editor.putString(Passcode.KEY_PASSCODE_BIOMETRICS_HASH, passcodeHash)
                    }
                }
            }

            VERSION_13 -> {
                editor.remove("debug_lang")
            }

            VERSION_14, VERSION_15 -> {
                changeDefaultOtherFlag(pmc, editor, FLAG_OTHER_SAVE_TO_GALLERY, false) // at first, it was true for VERSION_14
            }

            VERSION_16 -> {
                // Removing cloud map provider setting, if it's equal to Google
                try {
                    val type = pmc.tryGetInt(KEY_MAP_PROVIDER_TYPE_CLOUD)
                    if (type == MAP_PROVIDER_GOOGLE) {
                        editor.remove(KEY_MAP_PROVIDER_TYPE_CLOUD)
                    }
                } catch (ignored: FileNotFoundException) {
                }
                // Removing secret chat map provider setting, if it's equal to Google
                try {
                    val type = pmc.tryGetInt(KEY_MAP_PROVIDER_TYPE)
                    if (type == MAP_PROVIDER_GOOGLE) {
                        editor.remove(KEY_MAP_PROVIDER_TYPE)
                    }
                } catch (ignored: FileNotFoundException) {
                }
            }

            VERSION_17 -> {
                for (entry in pmc.find(KEY_THEME_NAME)) {
                    val key = entry.key()
                    val customThemeId = key.substring(KEY_THEME_NAME.length).toInt()
                    if (customThemeId >= 0) {
                        val activeColor: Int
                        try {
                            activeColor = pmc.tryGetInt(themeColorKey(customThemeId, ColorId.headerText))
                        } catch (ignored: Throwable) {
                            continue
                        }
                        val activeKey: String = themeColorKey(customThemeId, ColorId.headerTabActive)
                        val activeTextKey: String = themeColorKey(customThemeId, ColorId.headerTabActiveText)
                        val inactiveTextKey: String = themeColorKey(customThemeId, ColorId.headerTabInactiveText)
                        val barColor = alphaColor(.9f, activeColor)
                        val inactiveColor = alphaColor(.8f, activeColor)
                        if (!pmc.contains(activeKey)) {
                            pmc.putInt(activeKey, barColor)
                        }
                        if (!pmc.contains(activeTextKey)) {
                            pmc.putInt(activeTextKey, activeColor)
                        }
                        if (!pmc.contains(inactiveTextKey)) {
                            pmc.putInt(inactiveTextKey, inactiveColor)
                        }
                    }
                }
            }

            VERSION_18 -> {
                val accountNum = TdlibManager.readAccountNum()
                if (accountNum > 0) {
                    val prefixes = java.util.ArrayList<String?>(accountNum * 6)
                    var accountId = 0
                    while (accountId < accountNum) {
                        pmc.remove(TdlibNotificationManager.key(TdlibNotificationManager._NOTIFICATIONS_STACK_KEY, accountId))

                        // key(_CUSTOM_PRIORITY_KEY + _CUSTOM_USER_SUFFIX + userId)
                        prefixes.add(
                            TdlibNotificationManager.key(
                                TdlibNotificationManager._CUSTOM_PRIORITY_KEY + TdlibNotificationManager._CUSTOM_USER_SUFFIX,  /*+ userId*/
                                accountId
                            )
                        )

                        // key(_CUSTOM_VIBRATE_KEY + _CUSTOM_USER_SUFFIX + userId)
                        prefixes.add(
                            TdlibNotificationManager.key(
                                TdlibNotificationManager._CUSTOM_VIBRATE_KEY + TdlibNotificationManager._CUSTOM_USER_SUFFIX,  /*+ userId*/
                                accountId
                            )
                        )

                        // key(_CUSTOM_VIBRATE_ONLYSILENT_KEY + _CUSTOM_USER_SUFFIX + userId)
                        prefixes.add(
                            TdlibNotificationManager.key(
                                TdlibNotificationManager._CUSTOM_VIBRATE_ONLYSILENT_KEY + TdlibNotificationManager._CUSTOM_USER_SUFFIX,  /*+ userId*/
                                accountId
                            )
                        )

                        // key(_CUSTOM_SOUND_KEY + _CUSTOM_USER_SUFFIX + userId)
                        prefixes.add(
                            TdlibNotificationManager.key(
                                TdlibNotificationManager._CUSTOM_SOUND_KEY + TdlibNotificationManager._CUSTOM_USER_SUFFIX,  /*+ userId*/
                                accountId
                            )
                        )

                        // key(_CUSTOM_SOUND_NAME_KEY + _CUSTOM_USER_SUFFIX + userId)
                        prefixes.add(
                            TdlibNotificationManager.key(
                                TdlibNotificationManager._CUSTOM_SOUND_NAME_KEY + TdlibNotificationManager._CUSTOM_USER_SUFFIX,  /*+ userId*/
                                accountId
                            )
                        )

                        // key(_CUSTOM_PINNED_NOTIFICATIONS_KEY + chatId)
                        prefixes.add(TdlibNotificationManager.key(TdlibNotificationManager.__CUSTOM_PINNED_NOTIFICATIONS_KEY,  /*+ chatId*/accountId))

                        editor.remove(TdlibNotificationManager.key(TdlibNotificationManager.__PINNED_MESSAGE_NOTIFICATION_KEY, accountId))

                        editor.remove(TdlibNotificationManager.key(TdlibNotificationManager.__PRIVATE_MUTE_KEY, accountId))
                        editor.remove(TdlibNotificationManager.key(TdlibNotificationManager.__PRIVATE_PREVIEW_KEY, accountId))
                        editor.remove(TdlibNotificationManager.key(TdlibNotificationManager.__GROUP_MUTE_KEY, accountId))
                        editor.remove(TdlibNotificationManager.key(TdlibNotificationManager.__GROUP_PREVIEW_KEY, accountId))
                        editor.remove(TdlibNotificationManager.key(TdlibNotificationManager.__PINNED_MESSAGE_NOTIFICATION_KEY, accountId))

                        editor.remove(TdlibSettingsManager.key(TdlibSettingsManager.__PEER_TO_PEER_KEY, accountId))
                        accountId++
                    }
                    pmc.removeByAnyPrefix(prefixes)
                }
            }

            VERSION_19 -> {
                try {
                    val badgeMode = pmc.tryGetInt(KEY_BADGE_MODE)
                    var newBadgeMode = 0
                    when (badgeMode) {
                        1 -> newBadgeMode = BADGE_FLAG_MESSAGES
                        2 -> newBadgeMode = BADGE_FLAG_MESSAGES or BADGE_FLAG_MUTED
                    }
                    if (newBadgeMode != 0) editor.putInt(KEY_BADGE_FLAGS, newBadgeMode)
                    editor.remove(KEY_BADGE_MODE)
                } catch (ignored: Throwable) {
                }
                changeDefaultOtherFlag(pmc, editor, FLAG_OTHER_SPLIT_CHAT_NOTIFICATIONS, true)
            }

            VERSION_20 -> {
                try {
                    val file = File(TdlibManager.getLegacyLogFilePath(false))
                    if (file.exists() && !file.delete()) {
                        // nothing?
                    }
                } catch (ignored: Throwable) {
                }
                try {
                    val file = File(TdlibManager.getLegacyLogFilePath(true))
                    if (file.exists() && !file.delete()) {
                        // nothing?
                    }
                } catch (ignored: Throwable) {
                }
            }

            VERSION_21 -> {
                changeDefaultOtherFlag(pmc, editor, FLAG_OTHER_HIDE_SECRET_CHATS, false)
                changeDefaultOtherFlag(pmc, editor, FLAG_OTHER_DISABLE_ADDITIONAL_SYNC, false)
            }

            VERSION_22 -> {
                if (getBoolean("debug_hide_number", false)) pmc.putInt(KEY_UTILITY_FEATURES, UTILITY_FEATURE_HIDE_NUMBER or pmc.getInt(KEY_UTILITY_FEATURES, 0))
                pmc.removeByAnyPrefix(KEY_PREFIX_RTL, "debug_pinned_notification", "debug_hide_number", "debug_encrypted_push")
            }

            VERSION_23 -> {
                pmc.remove(KEY_PUSH_USER_IDS)
            }

            VERSION_24 -> {
                val accountNum = TdlibManager.readAccountNum()
                var accountId = 0
                while (accountId < accountNum) {
                    editor.remove(TdlibSettingsManager.key(TdlibSettingsManager.DEVICE_TOKEN_OR_ENDPOINT_KEY, accountId))
                    editor.remove(TdlibSettingsManager.key(TdlibSettingsManager.DEVICE_UID_KEY, accountId))
                    editor.remove(TdlibSettingsManager.key(TdlibSettingsManager.DEVICE_OTHER_UID_KEY, accountId))
                    accountId++
                }
            }

            VERSION_25 -> {
                changeDefaultOtherFlag(pmc, editor, FLAG_OTHER_USE_SYSTEM_FONTS, false)
            }

            VERSION_26 -> {
                changeDefaultOtherFlag(pmc, editor, FLAG_OTHER_DISABLE_BIG_EMOJI, false)
            }

            VERSION_27 -> {
                val accountNum = TdlibManager.readAccountNum()
                var accountId = 0
                while (accountId < accountNum) {
                    val globalPrefix: String = (if (accountId != 0) KEY_WALLPAPER_PREFIX + "_" + accountId else KEY_WALLPAPER_PREFIX)
                    var usageIdentifier = 0
                    while (usageIdentifier < 2) {
                        val prefix = globalPrefix + getWallpaperIdentifierSuffix(usageIdentifier)
                        if (getBoolean(prefix + KEY_WALLPAPER_EMPTY, false) || getBoolean(prefix + KEY_WALLPAPER_CUSTOM, false)) {
                            editor.remove(prefix)
                            usageIdentifier++
                            continue
                        }
                        val id = getLong(prefix, 0)
                        editor.remove(prefix)
                        if (id != 0L) {
                            val persistentId = getString(prefix + KEY_WALLPAPER_ID, null)
                            editor.remove(prefix + KEY_WALLPAPER_ID)
                            TGBackground.migrateLegacyWallpaper(editor, prefix, splitLongToFirstInt(id), splitLongToSecondInt(id), persistentId)
                        }
                        usageIdentifier++
                    }
                    for (entry in pmc.find(globalPrefix + "_other")) {
                        val prefix = entry.key()
                        if (prefix.matches((globalPrefix + "_other\\d+").toRegex())) {
                            val id = getLong(prefix, 0)
                            editor.remove(prefix)
                            if (id != 0L) {
                                val persistentId = getString(prefix + KEY_WALLPAPER_ID, null)
                                editor.remove(prefix + KEY_WALLPAPER_ID)
                                TGBackground.migrateLegacyWallpaper(editor, prefix, splitLongToFirstInt(id), splitLongToSecondInt(id), persistentId)
                            }
                        }
                    }
                    accountId++
                }
            }

            VERSION_28 -> {}
            VERSION_29 -> {
                try {
                    val markdownMode = pmc.tryGetInt(KEY_MARKDOWN_MODE)
                    if (markdownMode == 1) { // MARKDOWN_MODE_TEXT_ONLY
                        setNewSetting(SETTING_FLAG_EDIT_MARKDOWN, true)
                    }
                    editor.remove(KEY_MARKDOWN_MODE)
                } catch (ignored: FileNotFoundException) {
                }
            }

            VERSION_30 -> {
                editor.remove(KEY_PREFER_LEGACY_API)
                changeDefaultOtherFlag(pmc, editor, DISABLED_FLAG_OTHER_CAMERA_FORCE_16_9, false)
            }

            VERSION_31 -> {
                U.moveUnsafePrivateMedia()
            }

            VERSION_32 -> {
                val zoomTables = File(UI.getAppContext().getFilesDir(), "ZoomTables.data")
                if (zoomTables.exists() && !zoomTables.delete()) {
                }
            }

            VERSION_33 -> {
                if (NIGHT_MODE_DEFAULT == NIGHT_MODE_SYSTEM) {
                    val mode = pmc.getInt(KEY_NIGHT_MODE, NIGHT_MODE_SYSTEM)
                    if (mode == NIGHT_MODE_NONE || mode == NIGHT_MODE_SYSTEM) {
                        val preferredAccountId = TdlibManager.readPreferredAccountId()
                        val globalThemeDaylight = TdlibSettingsManager.getThemeId(this, preferredAccountId, false)
                        val globalThemeNight = TdlibSettingsManager.getThemeId(this, preferredAccountId, true)
                        if (ThemeManager.isCustomTheme(globalThemeNight) || ThemeManager.isCustomTheme(globalThemeDaylight)) {
                            editor.putInt(KEY_NIGHT_MODE, NIGHT_MODE_NONE)
                        } else {
                            editor.remove(KEY_NIGHT_MODE)
                        }
                    }
                }
            }

            VERSION_34 -> {
                val accountNum = TdlibManager.readAccountNum()
                var accountId = 0
                while (accountId < accountNum) {
                    val prefix: String = key(KEY_SCROLL_CHAT_PREFIX, accountId)
                    for (entry in pmc.find(prefix)) {
                        val suffix = entry.key().substring(prefix.length).replace("^\\d+_([^_]+).*$".toRegex(), "$1")
                        if (suffix == KEY_SCROLL_CHAT_RETURN_TO_MESSAGE_ID) {
                            val returnToMessageId = entry.asLong()
                            editor.remove(entry.key())
                            if (returnToMessageId != 0L) {
                                val newKey: String = entry.key().replace(KEY_SCROLL_CHAT_RETURN_TO_MESSAGE_ID, KEY_SCROLL_CHAT_RETURN_TO_MESSAGE_IDS_STACK)
                                pmc.putLongArray(newKey, longArrayOf(returnToMessageId))
                            }
                        }
                    }
                    accountId++
                }
            }

            VERSION_35 -> {
                val accountNum = TdlibManager.readAccountNum()
                var accountId = 0
                while (accountId < accountNum) {
                    val prefix: String = key(TdlibSettingsManager.CONVERSION_PREFIX, accountId)
                    pmc.removeByPrefix(prefix)
                    accountId++
                }
            }

            VERSION_36 -> {
                editor
                    .remove(KEY_TON_LOG_SIZE)
                    .remove(KEY_TON_OTHER)
                    .remove(KEY_TON_VERBOSITY)
            }

            VERSION_37 -> {
                val needLog = Log.checkLogLevel(Log.LEVEL_VERBOSE)

                val whitelist = arrayOf<String?>(
                    "name",
                    "type",
                    "custom",

                    "blurred",
                    "moving",
                    "intensity",

                    "empty",
                    "vector",

                    "color",
                    "colors",
                    "fill"
                )
                // remove: any other key matching "wallpaper_[a-zA-Z0-9]+"
                for (entry in pmc.find("wallpaper_")) {
                    val suffix = entry.key().substring("wallpaper_".length)
                    if (!isNumeric(suffix) &&
                        suffix.matches("^[a-zA-Z0-9]+$".toRegex()) && !suffix.startsWith("other") && !contains<String?>(whitelist, suffix)
                    ) {
                        if (needLog) {
                            Log.v("Removing rudimentary key: %s", entry.key())
                        }
                        editor.remove(entry.key())
                    }
                }
            }

            VERSION_38 -> {
                val accountNum = TdlibManager.readAccountNum()
                var accountId = 0
                while (accountId < accountNum) {
                    val intToLongKeys = arrayOf<String>(
                        accountInfoPrefix(accountId) + KEY_ACCOUNT_INFO_SUFFIX_ID,
                        TdlibSettingsManager.key(TdlibSettingsManager.DEVICE_UID_KEY, accountId)
                    )
                    val intToLongArrayKeys = arrayOf<String>(
                        TdlibSettingsManager.key(TdlibSettingsManager.DEVICE_OTHER_UID_KEY, accountId)
                    )
                    for (key in intToLongKeys) {
                        val int32 = pmc.getIntOrLong(key, 0)
                        if (int32 != 0L) {
                            editor.putLong(key, int32)
                        } else {
                            editor.remove(key)
                        }
                    }
                    for (key in intToLongArrayKeys) {
                        var int32Array: IntArray? = null
                        try {
                            int32Array = pmc.getIntArray(key)
                        } catch (ignored: IllegalStateException) {
                            // Since it's just DEVICE_OTHER_UID_KEY, it's not critical
                        }
                        if (int32Array != null) {
                            val int64Array = LongArray(int32Array.size)
                            var i = 0
                            while (i < int32Array.size) {
                                int64Array[i] = int32Array[i].toLong()
                                i++
                            }
                            pmc.putLongArray(key, int64Array)
                        } else {
                            editor.remove(key)
                        }
                    }
                    accountId++
                }

                upgradeAccountsConfig(TdlibAccount.VERSION_1)
            }

            VERSION_39 -> {
                pmc.removeByPrefix(KEY_TDLIB_CRASH_PREFIX)
            }

            VERSION_40 -> {
                pmc
                    .remove("crash_id_debug")
                    .remove("crash_id_release")
                    .remove("crash_id_reported_debug")
                    .remove("crash_id_reported_release")
            }

            VERSION_41 -> {
                deleteAllLogs(false, null)
            }

            VERSION_42 -> {
                val accountNum = TdlibManager.readAccountNum()
                var accountId = 0
                while (accountId < accountNum) {
                    editor.remove(TdlibSettingsManager.key(TdlibSettingsManager.__DEVICE_TDLIB_VERSION_KEY, accountId))
                    accountId++
                }
            }

            VERSION_43 -> {
                val emojis = pmc.getStringArray(KEY_EMOJI_RECENTS)
                if (emojis != null && emojis.size > 0) {
                    val infos: MutableMap<String?, RecentInfo?> = HashMap<String?, RecentInfo?>()
                    getBinaryMap<RecentInfo>(KEY_EMOJI_COUNTERS, infos, RecentInfo::class.java)

                    var changedCount = 0
                    var changedEmojiCounters = 0
                    var index = 0
                    while (index < emojis.size) {
                        val oldEmoji: String = emojis[index]!!
                        // Save 15*2 bytes per recent custom emoji by simply reducing prefix size
                        if (oldEmoji.startsWith(Emoji.CUSTOM_EMOJI_CACHE_OLD)) {
                            val newEmoji = Emoji.CUSTOM_EMOJI_CACHE + oldEmoji.substring(Emoji.CUSTOM_EMOJI_CACHE_OLD.length)
                            emojis[index] = newEmoji
                            changedCount++

                            val recentInfo = infos.remove(oldEmoji)
                            if (recentInfo != null) {
                                infos.put(newEmoji, recentInfo)
                                changedEmojiCounters++
                            }
                        }
                        index++
                    }
                    if (changedCount > 0) {
                        pmc.putStringArray(KEY_EMOJI_RECENTS, emojis)
                    }
                    if (changedEmojiCounters > 0) {
                        saveBinaryMap(KEY_EMOJI_COUNTERS, infos)
                    }
                }
            }

            VERSION_44 -> {
                upgradeAccountsConfig(TdlibAccount.VERSION_2)
            }

            VERSION_45 -> {
                resetOtherFlag(pmc, editor, FLAG_OTHER_DISABLE_BIG_EMOJI, false)
            }

            VERSION_46 -> {
                var experiments = pmc.getLong(KEY_EXPERIMENTS, makeDefaultExperiments())
                if (hasFlag(experiments, REMOVED_EXPERIMENT_FLAG_ENABLE_FOLDERS)) {
                    experiments = experiments and REMOVED_EXPERIMENT_FLAG_ENABLE_FOLDERS.inv()
                    editor.putLong(KEY_EXPERIMENTS, experiments)
                }
            }

            VERSION_47 -> {
                // No features were officially available before VERSION_46.
                // Reset just once in VERSION_47 right prior to stable release.
                editor.putLong(KEY_FEATURES, 0)
            }

            VERSION_48 -> {
                val passcodeMode = pmc.getInt(Passcode.KEY_PASSCODE_MODE, Passcode.MODE_NONE)
                if (Passcode.isValidMode(passcodeMode) && passcodeMode != Passcode.MODE_NONE) {
                    val extraBiometricsHash = if (passcodeMode != Passcode.MODE_BIOMETRICS) pmc.getString(Passcode.KEY_PASSCODE_BIOMETRICS_HASH, null) else null
                    val usesBiometrics = passcodeMode == Passcode.MODE_BIOMETRICS || extraBiometricsHash != null
                    if (usesBiometrics) {
                        val strongEnrolled = BiometricAuthentication.isStrongAvailable(true)
                        pmc.putInt(Passcode.KEY_PASSCODE_BIOMETRICS_OPTIONS, optional(Passcode.BIOMETRICS_OPTION_ONLY_STRONG, strongEnrolled))
                    }
                }
            }
        }
    }

    private fun resetOtherFlag(pmc: LevelDB, editor: SharedPreferences.Editor, flag: Int, value: Boolean) {
        val defaultSettings: Int = makeDefaultSettings()
        val oldSettings = pmc.getInt(KEY_OTHER, defaultSettings)
        val newSettings = setFlag(oldSettings, flag, value)
        if (oldSettings != newSettings) {
            if (newSettings != defaultSettings) {
                editor.putInt(KEY_OTHER, newSettings)
            } else {
                editor.remove(KEY_OTHER)
            }
        }
    }

    private fun upgradeAccountsConfig(fromConfigVersion: Int) {
        if (ignoreFurtherAccountConfigUpgrades) {
            return
        }
        val oldConfigFile = TdlibManager.getAccountConfigFile()
        val backupFile = File(oldConfigFile.getParentFile(), oldConfigFile.getName() + ".bak." + fromConfigVersion)
        if (oldConfigFile.exists() && !backupFile.exists()) {
            var config: AccountConfig? = null
            try {
                RandomAccessFile(oldConfigFile, TdlibManager.MODE_R).use { r ->
                    config = TdlibManager.readAccountConfig(null, r, fromConfigVersion, false)
                }
            } catch (e: IOException) {
                Log.e(e)
            }
            if (config != null) {
                val newConfigFile = File(oldConfigFile.getParentFile(), oldConfigFile.getName() + ".tmp")
                try {
                    if (newConfigFile.exists() || newConfigFile.createNewFile()) {
                        try {
                            RandomAccessFile(newConfigFile, TdlibManager.MODE_RW).use { r ->
                                TdlibManager.writeAccountConfigFully(r, config)
                                ignoreFurtherAccountConfigUpgrades = true
                            }
                        } catch (e: IOException) {
                            onLaunchError(e)
                            throw DeviceStorageError(e)
                        }
                    }
                    if (!oldConfigFile.renameTo(backupFile)) throw DeviceStorageError("Cannot backup old config")
                    if (!newConfigFile.renameTo(oldConfigFile)) throw DeviceStorageError("Cannot save new config")
                } catch (t: Throwable) {
                    onLaunchError(t)
                    throw DeviceStorageError(t)
                }
            }
        }
    }

    private fun changeDefaultOtherFlag(pmc: LevelDB, editor: SharedPreferences.Editor, flag: Int, value: Boolean) {
        try {
            val settings = pmc.tryGetInt(KEY_OTHER)
            val newSettings = setFlag(settings, flag, value)
            if (settings != newSettings) {
                editor.putInt(KEY_OTHER, newSettings)
            }
        } catch (ignored: FileNotFoundException) {
        }
    }

    private var needProxyLegacyMigrateCheck = false

    fun needProxyLegacyMigrateCheck(): Boolean {
        if (needProxyLegacyMigrateCheck) {
            needProxyLegacyMigrateCheck = false
            return true
        }
        return false
    }

    private val isFreshAppInstallation: Boolean
        get() = !TdlibManager.getAccountConfigFile().exists() && pmc.getLong(
            KEY_APP_INSTALLATION_ID,
            0
        ) == 0L

    private fun migratePrefsToPmc() {
        // Main

        val main = UI.getAppContext().getSharedPreferences(STORAGE_MAIN, Context.MODE_PRIVATE)
        Log.load(main)

        val settingsVersion = main.getInt(KEY_VERSION, 0)
        if (settingsVersion != LEGACY_VERSION) {
            for (i in settingsVersion + 1..LEGACY_VERSION) {
                val editor = main.edit()
                upgradeSharedPreferences(main, editor, i)
                editor.putInt(KEY_VERSION, i)
                editor.apply()
            }
        }

        var editor: SharedPreferences.Editor? = null
        var blob: Blob? = null

        val mainItems = main.getAll()
        var pipX: Int? = null
        var pipY: Int? = null

        if (mainItems != null && !mainItems.isEmpty()) {
            editor = pmc.edit()
            for (entry in mainItems.entries) {
                var key: String = entry.key!!
                val value: Any? = entry.value
                when (key) {
                    KEY_PIP_X -> {
                        if (value is Int) pipX = value
                        continue
                    }

                    KEY_PIP_Y -> {
                        if (value is Int) pipY = value
                        continue
                    }

                    KEY_LAST_LOCATION, KEY_LAST_INLINE_LOCATION -> {
                        if (value is String) {
                            val location: LastLocation? = parseLocation(value, if (KEY_LAST_LOCATION == key) "," else "x")
                            if (location != null) {
                                if (blob == null) {
                                    blob = Blob(8 + 8 + 4)
                                } else {
                                    blob.seekToStart()
                                    blob.ensureCapacity(8 + 8 + 4)
                                }
                                location.saveTo(blob)
                                pmc.putByteArray(key, blob.toByteArray())
                            }
                        }
                        continue
                    }

                    KEY_PUSH_USER_IDS -> {
                        if (value is String) {
                            val array = parseIntArray(value, ",")
                            if (array != null) {
                                pmc.putIntArray(key, array)
                            }
                        }
                        continue
                    }

                    KEY_PUSH_USER_ID -> continue
                    else -> if (key.startsWith(KEY_SCROLL_CHAT_PREFIX)) {
                        val lastIndex = key.lastIndexOf('_')
                        if (lastIndex == -1) break
                        val suffix = key.substring(lastIndex + 1)
                        if (isNumeric(suffix)) {
                            val accountId = parseInt(suffix)
                            key = key(key.substring(0, lastIndex), accountId)
                        }
                        if (key.endsWith(KEY_SCROLL_CHAT_ALIASES)) {
                            if (value is String) {
                                val aliasMessageIds = parseLongArray(value, ",")
                                if (aliasMessageIds != null && aliasMessageIds.size > 0) {
                                    pmc.putLongArray(key, aliasMessageIds)
                                }
                            }
                            continue
                        }
                    } else if (key.startsWith(TdlibNotificationManager._NOTIFICATIONS_STACK_KEY)) {
                        if (value is String) {
                            val items = value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            if (items.size > 0) {
                                val estimatedSize = (8 + 8 + 4) * items.size
                                if (blob == null) {
                                    blob = Blob(estimatedSize)
                                } else {
                                    blob.seekToStart()
                                    blob.ensureCapacity(estimatedSize)
                                }

                                for (item in items) {
                                    val data = item.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                    val flags = if (data.size > 2) data[2].toInt() else 0
                                    val chatId = data[0].toLong()
                                    val messageId = data[1].toLong()
                                    blob.writeLong(chatId)
                                    blob.writeLong(messageId)
                                    blob.writeVarint(flags)
                                }
                                pmc.putByteArray(key, blob.toByteArray())
                            }
                        }
                        continue
                    } else if (key.contains(KEY_SILENT_CHANNEL_PREFIX)) {
                        continue
                    }
                }
                if (value is Int) {
                    editor.putInt(key, value)
                } else if (value is Long) {
                    editor.putLong(key, value)
                } else if (value is Float) {
                    editor.putFloat(key, value)
                } else if (value is Boolean) {
                    editor.putBoolean(key, value)
                } else if (value is String) {
                    editor.putString(key, value)
                } else {
                    Log.e("Unknown preferences value, key:%s, value:%s", key, value)
                    throw UnsupportedOperationException("key = " + key + " value = " + value + " (" + (if (value != null) value.javaClass.getSimpleName() else "null"))
                }
            }
            if (pipX != null && pipY != null) {
                val pip = mergeLong(pipX, pipY)
                editor.putLong(KEY_PIP, pip)
            }
        }

        // Bots. Moving as is
        var bots: SharedPreferences? = null
        val botsPrefs = U.sharedPreferencesFile(STORAGE_BOTS)
        if (botsPrefs != null) {
            bots = UI.getAppContext().getSharedPreferences(STORAGE_BOTS, Context.MODE_PRIVATE)
            editor = movePreferences(bots, pmc, editor, null)
        }

        // Keyboard. Moving with prefix, e.g. "keyboard_size0"
        var keyboard: SharedPreferences? = null
        val keyboardPrefs = U.sharedPreferencesFile(STORAGE_KEYBOARD)
        if (keyboardPrefs != null) {
            keyboard = UI.getAppContext().getSharedPreferences(STORAGE_KEYBOARD, Context.MODE_PRIVATE)
            editor = movePreferences(keyboard, pmc, editor, "keyboard_")
        }

        // Emoji
        val emoji = UI.getAppContext().getSharedPreferences(STORAGE_EMOJI, Context.MODE_PRIVATE)
        val allEmoji = emoji.getAll()
        if (allEmoji != null && !allEmoji.isEmpty()) {
            if (editor == null) {
                editor = pmc.edit()
            }

            var infos: MutableMap<String?, RecentInfo?>? = null
            var recents: MutableList<RecentEmoji>? = null
            val countersRaw: Any? = allEmoji.get(KEY_EMOJI_COUNTERS_OLD)
            if (countersRaw != null && countersRaw is String) {
                infos = HashMap<String?, RecentInfo?>()
                parseEmojiCounters(infos, countersRaw)
            }
            if (infos != null) {
                val recentsRaw: Any? = allEmoji.get(KEY_EMOJI_RECENTS_OLD)
                if (recentsRaw != null && recentsRaw is String) {
                    recents = java.util.ArrayList<RecentEmoji>()
                    parseEmojiRecents(infos, recents, recentsRaw)
                }
            }
            if (infos != null && !infos.isEmpty() && recents != null && !recents.isEmpty()) {
                setEmojiCounters(infos)
                setEmojiRecents(recents)
            }

            val colorsRaw: Any? = allEmoji.get(KEY_EMOJI_COLORS_OLD)
            if (colorsRaw != null && colorsRaw is String) {
                val colors: MutableMap<String?, String?> = HashMap<String?, String?>()
                parseEmojiColors(colors, colorsRaw)
                setEmojiColors(colors, editor)
            }

            val defaultColorRaw: Any? = allEmoji.get(KEY_EMOJI_DEFAULT_COLOR_OLD)
            if (defaultColorRaw != null && defaultColorRaw is String) {
                editor.putString(KEY_EMOJI_DEFAULT_COLOR, defaultColorRaw)
            }
        }

        if (editor != null) {
            editor.apply()
        }

        if (mainItems != null && BuildConfig.DEBUG) {
            for (entry in mainItems.entries) {
                val key: String = entry.key!!
                when (key) {
                    KEY_PIP_X, KEY_PIP_Y, KEY_LAST_LOCATION, KEY_LAST_INLINE_LOCATION, KEY_PUSH_USER_IDS, KEY_PUSH_USER_ID -> continue

                    else -> if (key.startsWith(KEY_SCROLL_CHAT_PREFIX) ||
                        key.startsWith(TdlibNotificationManager._NOTIFICATIONS_STACK_KEY)
                    ) {
                        continue
                    }
                }
                val value: Any? = entry.value
                try {
                    if (value is Int) {
                        val newValue = pmc.tryGetInt(key)
                        Test.assertEquals(value, newValue)
                    } else if (value is Long) {
                        val newValue = pmc.tryGetLong(key)
                        Test.assertEquals(value, newValue)
                    } else if (value is Float) {
                        val newValue = pmc.tryGetFloat(key)
                        Test.assertEquals(value, newValue)
                    } else if (value is Boolean) {
                        val newValue = pmc.tryGetBoolean(key)
                        Test.assertEquals(value, newValue)
                    } else if (value is String) {
                        val newValue = pmc.tryGetString(key)
                        Test.assertEquals(value, newValue)
                    } else {
                        throw UnsupportedOperationException("key = " + key + " value = " + value + " (" + (if (value != null) value.javaClass.getSimpleName() else "null"))
                    }
                } catch (e: FileNotFoundException) {
                    throw RuntimeException(key + " not found")
                } catch (e: AssertionError) {
                    throw RuntimeException(key + " not equals: " + value + " (" + (if (value != null) value.javaClass.getSimpleName() else "null") + ")")
                }
            }
        }

        if (bots != null) {
            bots.edit().clear().apply()
        }
        U.deleteSharedPreferences(STORAGE_BOTS)
        if (keyboard != null) {
            keyboard.edit().clear().apply()
        }
        U.deleteSharedPreferences(STORAGE_KEYBOARD)
        emoji.edit().clear().apply()
        U.deleteSharedPreferences(STORAGE_EMOJI)
        main.edit().clear().apply()
        U.deleteSharedPreferences(STORAGE_MAIN)

        val debugProxyFile: File = proxyConfigFile
        if (debugProxyFile.exists() && !debugProxyFile.delete()) {
            Log.w("Cannot delete debug proxy config file")
        }

        val proxyFile: File = proxyConfigFile
        if (proxyFile.exists()) {
            if (proxyFile.length() > 0) {
                var proxy: TdApi.Proxy? = null
                try {
                    RandomAccessFile(proxyFile, "r").use { r ->
                        proxy = readProxy(r)
                    }
                } catch (e: IOException) {
                    Log.e(e)
                }
                if (proxy != null) {
                    addOrUpdateProxy(proxy, null, true)
                }
            }
            if (!proxyFile.delete()) {
                Log.w("Cannot delete proxy config file")
            }
        } else {
            needProxyLegacyMigrateCheck = true
        }
    }

    fun resetTutorials() {
        this.tutorialFlags = 0
    }

    fun needTutorial(flag: Long): Boolean {
        return !hasFlag(this.tutorialFlags, flag) && !hasFlag(shownTutorials, flag)
    }

    fun needTutorial(source: ChatSource): Boolean {
        when (source.getConstructor()) {
            ChatSourcePublicServiceAnnouncement.CONSTRUCTOR -> {
                val type = (source as ChatSourcePublicServiceAnnouncement).type
                return !pmc.contains(if (isEmpty(type)) KEY_TUTORIAL_PSA else KEY_TUTORIAL_PSA + type)
            }

            TdApi.ChatSourceMtprotoProxy.CONSTRUCTOR -> return needTutorial(TUTORIAL_PROXY_SPONSOR)
            else -> {
                assertChatSource_12b21238()
                throw unsupported(source)
            }
        }
    }

    fun markTutorialAsComplete(source: ChatSource) {
        when (source.getConstructor()) {
            ChatSourcePublicServiceAnnouncement.CONSTRUCTOR -> {
                val type = (source as ChatSourcePublicServiceAnnouncement).type
                pmc.putVoid(if (isEmpty(type)) KEY_TUTORIAL_PSA else KEY_TUTORIAL_PSA + type)
            }

            TdApi.ChatSourceMtprotoProxy.CONSTRUCTOR -> markTutorialAsComplete(TUTORIAL_PROXY_SPONSOR)
        }
    }

    fun inDeveloperMode(): Boolean {
        return BuildConfig.DEBUG || !needTutorial(TUTORIAL_DEVELOPER_MODE)
    }

    private var shownTutorials: Long = 0

    fun markTutorialAsShown(flag: Long) {
        shownTutorials = shownTutorials or flag
    }

    fun markTutorialAsComplete(flag: Long) {
        this.tutorialFlags = this.tutorialFlags or flag
    }

    private var tutorialFlags: Long
        get() {
            if (_tutorialFlags == null) _tutorialFlags = pmc.getLong(KEY_TUTORIAL, 0)
            return _tutorialFlags!!
        }
        private set(flags) {
            this._tutorialFlags = flags
            if (flags == 0L) remove(KEY_TUTORIAL)
            else putLong(KEY_TUTORIAL, flags)
        }

    fun needSecretLinkPreviews(): Boolean {
        return checkNegativeSetting(FLAG_OTHER_DISABLE_SECRET_LINK_PREVIEWS)
    }

    fun setUseSecretLinkPreviews(use: Boolean) {
        if (setNegativeSetting(FLAG_OTHER_DISABLE_SECRET_LINK_PREVIEWS, use)) {
            markTutorialAsComplete(TUTORIAL_SECRET_LINK_PREVIEWS)
        }
    }

    val preferredAudioPlaybackMode: Int
        get() {
            if (_preferredAudioPlaybackMode == null) _preferredAudioPlaybackMode =
                pmc.getInt(KEY_PREFERRED_PLAYBACK_MODE, 0)
            return _preferredAudioPlaybackMode!!
        }

    fun needSaveEditedMediaToGallery(): Boolean {
        return checkSetting(FLAG_OTHER_SAVE_TO_GALLERY)
    }

    fun setSaveEditedMediaToGallery(saveToGallery: Boolean) {
        setSetting(FLAG_OTHER_SAVE_TO_GALLERY, saveToGallery)
    }

    fun rememberAlbumSetting(): Boolean {
        return checkSetting(FLAG_OTHER_REMEMBER_ALBUM_SETTING)
    }

    fun setRememberAlbumSetting(remember: Boolean) {
        setSetting(FLAG_OTHER_REMEMBER_ALBUM_SETTING, remember)
    }

    fun needGroupMedia(): Boolean {
        return checkSetting(FLAG_OTHER_NEED_GROUP_MEDIA)
    }

    fun setNeedGroupMedia(group: Boolean) {
        setSetting(FLAG_OTHER_NEED_GROUP_MEDIA, group)
    }

    fun needSeparateMediaTab(): Boolean {
        return checkSetting(FLAG_OTHER_SEPARATE_MEDIA_TAB)
    }

    fun setNeedSeparateMediaTab(separate: Boolean) {
        setSetting(FLAG_OTHER_SEPARATE_MEDIA_TAB, separate)
    }

    fun needAutoplayGIFs(): Boolean {
        return checkSetting(FLAG_OTHER_AUTOPLAY_GIFS)
    }

    fun setAutoplayGIFs(autoplayGIFs: Boolean) {
        setSetting(FLAG_OTHER_AUTOPLAY_GIFS, autoplayGIFs)
    }

    fun setUseQuickTranslation(useQuickTranslation: Boolean) {
        setSetting(FLAG_OTHER_USE_QUICK_TRANSLATION, useQuickTranslation)
    }

    fun needUseQuickTranslation(): Boolean {
        return checkSetting(FLAG_OTHER_USE_QUICK_TRANSLATION)
    }

    fun forceArabicNumbers(): Boolean {
        return checkSetting(FLAG_OTHER_FORCE_ARABIC_NUMBERS)
    }

    fun setForceArabicNumbers(force: Boolean) {
        setSetting(FLAG_OTHER_FORCE_ARABIC_NUMBERS, force)
    }

    private var needRestrictContent: Boolean? = null

    fun needRestrictContent(): Boolean {
        return (if (needRestrictContent != null) needRestrictContent else (pmc.getBoolean(
            org.thunderdog.challegram.unsorted.Settings.Companion.KEY_RESTRICT_CONTENT,
            true
        )
            .also { needRestrictContent = it }))!!
    }

    fun setRestrictContent(restrict: Boolean) {
        needRestrictContent = restrict
        if (restrict) pmc.remove(KEY_RESTRICT_CONTENT)
        else pmc.putBoolean(KEY_RESTRICT_CONTENT, false)
    }

    fun needReduceMotion(): Boolean {
        return checkSetting(FLAG_OTHER_REDUCE_MOTION)
    }

    fun setReduceMotion(reduceMotion: Boolean) {
        setSetting(FLAG_OTHER_REDUCE_MOTION, reduceMotion)
    }

    fun toggleReduceMotion() {
        setReduceMotion(!needReduceMotion())
    }

    fun useSystemFonts(): Boolean {
        return checkSetting(FLAG_OTHER_USE_SYSTEM_FONTS)
    }

    fun setUseSystemFonts(useSystemFonts: Boolean) {
        setSetting(FLAG_OTHER_USE_SYSTEM_FONTS, useSystemFonts)
    }

    fun useEdgeToEdge(): Boolean {
        if (Config.EDGE_TO_EDGE_AVAILABLE) {
            if (Config.EDGE_TO_EDGE_CUSTOMIZABLE) {
                return !isExperimentEnabled(EXPERIMENT_FLAG_NO_EDGE_TO_EDGE)
            }
            return true
        }
        return false
    }

    fun useBigEmoji(): Boolean {
        return checkNegativeSetting(FLAG_OTHER_DISABLE_BIG_EMOJI)
    }

    fun setUseBigEmoji(useBigEmoji: Boolean) {
        setNegativeSetting(FLAG_OTHER_DISABLE_BIG_EMOJI, useBigEmoji)
    }

    var instantViewMode: Int
        get() = getInt(
            KEY_INSTANT_VIEW,
            INSTANT_VIEW_MODE_INTERNAL
        )
        set(mode) {
            if (mode == INSTANT_VIEW_MODE_INTERNAL) {
                remove(KEY_INSTANT_VIEW)
            } else {
                putInt(KEY_INSTANT_VIEW, mode)
            }
        }

    var stickerMode: Int
        get() {
            if (_stickerMode == null) _stickerMode = pmc.getInt(
                KEY_STICKER_MODE,
                STICKER_MODE_ALL
            )
            return _stickerMode!!
        }
        set(mode) {
            this._stickerMode = mode
            if (mode == STICKER_MODE_ALL) {
                remove(KEY_STICKER_MODE)
            } else {
                putInt(KEY_STICKER_MODE, mode)
            }
        }

    var emojiMode: Int
        get() {
            if (_emojiMode == null) _emojiMode = pmc.getInt(
                KEY_EMOJI_MODE,
                STICKER_MODE_ALL
            )
            return _emojiMode!!
        }
        set(mode) {
            this._emojiMode = mode
            if (mode == STICKER_MODE_ALL) {
                remove(KEY_EMOJI_MODE)
            } else {
                putInt(KEY_EMOJI_MODE, mode)
            }
        }

    var reactionAvatarsMode: Int
        get() {
            if (_reactionAvatarsMode == null) _reactionAvatarsMode = pmc.getInt(
                KEY_REACTION_AVATARS_MODE,
                REACTION_AVATARS_MODE_SMART_FILTER
            )
            return _reactionAvatarsMode!!
        }
        set(mode) {
            this._reactionAvatarsMode = mode
            putInt(KEY_REACTION_AVATARS_MODE, mode)
        }

    var autoUpdateMode: Int
        get() {
            if (_autoUpdateMode == null) _autoUpdateMode = pmc.getInt(
                KEY_AUTO_UPDATE_MODE,
                AUTO_UPDATE_MODE_PROMPT
            )
            return _autoUpdateMode!!
        }
        set(mode) {
            this._autoUpdateMode = mode
            if (mode == AUTO_UPDATE_MODE_PROMPT) {
                remove(KEY_AUTO_UPDATE_MODE)
            } else {
                putInt(KEY_AUTO_UPDATE_MODE, mode)
            }
        }

    val badgeFlags: Int
        get() {
            if (_badgeFlags == null) _badgeFlags = pmc.getInt(KEY_BADGE_FLAGS, 0)
            return _badgeFlags!!
        }

    fun setBadgeFlags(badgeFlags: Int): Boolean {
        if (this.badgeFlags != badgeFlags) {
            this._badgeFlags = badgeFlags
            if (badgeFlags == 0) {
                remove(KEY_BADGE_FLAGS)
            } else {
                putInt(KEY_BADGE_FLAGS, badgeFlags)
            }
            return true
        }
        return false
    }

    fun resetBadge(): Boolean {
        val updated: Boolean
        updated = setBadgeFlags(0)
        return updated
    }

    private val notificationFlags: Int
        get() {
            if (_notificationFlags == null) {
                var flags = pmc.getInt(
                    KEY_NOTIFICATION_FLAGS,
                    NOTIFICATION_FLAGS_DEFAULT
                )
                if (hasFlag(flags, NOTIFICATION_FLAG_ONLY_ACTIVE_ACCOUNT) && hasFlag(
                        flags,
                        NOTIFICATION_FLAG_ONLY_SELECTED_ACCOUNTS
                    )
                ) {
                    flags = setFlag(flags, NOTIFICATION_FLAG_ONLY_ACTIVE_ACCOUNT, false)
                    flags = setFlag(flags, NOTIFICATION_FLAG_ONLY_SELECTED_ACCOUNTS, false)
                }
                _notificationFlags = flags
            }
            return _notificationFlags!!
        }

    fun checkNotificationFlag(flag: Int): Boolean {
        return hasFlag(this.notificationFlags, flag)
    }

    fun setNotificationFlag(flag: Int, enabled: Boolean): Boolean {
        var flags = this.notificationFlags
        if (enabled) {
            if (flag == NOTIFICATION_FLAG_ONLY_ACTIVE_ACCOUNT) {
                flags = setFlag(flags, NOTIFICATION_FLAG_ONLY_SELECTED_ACCOUNTS, false)
            } else if (flag == NOTIFICATION_FLAG_ONLY_SELECTED_ACCOUNTS) {
                flags = setFlag(flags, NOTIFICATION_FLAG_ONLY_ACTIVE_ACCOUNT, false)
            }
        }
        return setNotificationFlags(setFlag(flags, flag, enabled))
    }

    fun resetNotificationFlags(): Boolean {
        return setNotificationFlags(NOTIFICATION_FLAGS_DEFAULT)
    }

    private fun setNotificationFlags(flags: Int): Boolean {
        if (this.notificationFlags != flags) {
            this._notificationFlags = flags
            if (flags == NOTIFICATION_FLAGS_DEFAULT) {
                remove(KEY_NOTIFICATION_FLAGS)
            } else {
                putInt(KEY_NOTIFICATION_FLAGS, flags)
            }
            return true
        }
        return false
    }

    fun needsIncognitoMode(): Boolean {
        return this.incognitoMode == INCOGNITO_CHAT_SECRET // TODO more chat options ?
    }

    fun needsIncognitoMode(chat: Chat?): Boolean {
        return (this.incognitoMode == INCOGNITO_CHAT_SECRET && chat != null && isSecret(chat.id))
    }

    var incognitoMode: Int
        get() {
            if (_incognitoMode == null) _incognitoMode = pmc.getInt(
                KEY_INCOGNITO,
                INCOGNITO_CHAT_SECRET
            )
            return _incognitoMode!!
        }
        set(incognitoMode) {
            this._incognitoMode = incognitoMode
            if (incognitoMode == INCOGNITO_CHAT_SECRET) {
                remove(KEY_INCOGNITO)
            } else {
                putInt(KEY_INCOGNITO, incognitoMode)
            }
        }

    interface VideoModePreferenceListener {
        fun onPreferVideoModeChanged(preferVideoMode: Boolean)

        fun onRecordAudioVideoError(preferVideoMode: Boolean) {}
    }

    private val videoPreferenceChangeListeners: MutableList<Reference<VideoModePreferenceListener?>> =
        java.util.ArrayList<Reference<VideoModePreferenceListener?>>()

    private fun notifyVideoPreferenceListeners(preferVideoMode: Boolean) {
        val size = videoPreferenceChangeListeners.size
        for (i in size - 1 downTo 0) {
            val listener = videoPreferenceChangeListeners.get(i)!!.get()
            if (listener != null) {
                listener.onPreferVideoModeChanged(preferVideoMode)
            } else {
                videoPreferenceChangeListeners.removeAt(i)
            }
        }
    }

    fun notifyRecordAudioVideoError() {
        val preferVideoMode = preferVideoMode()
        val size = videoPreferenceChangeListeners.size
        for (i in size - 1 downTo 0) {
            val listener = videoPreferenceChangeListeners.get(i)!!.get()
            if (listener != null) {
                listener.onRecordAudioVideoError(preferVideoMode)
            } else {
                videoPreferenceChangeListeners.removeAt(i)
            }
        }
    }

    fun addVideoPreferenceChangeListener(listener: VideoModePreferenceListener?) {
        addReference<VideoModePreferenceListener?>(videoPreferenceChangeListeners, listener)
    }

    fun removeVideoPreferenceChangeListener(listener: VideoModePreferenceListener?) {
        removeReference<VideoModePreferenceListener?>(videoPreferenceChangeListeners, listener)
    }

    fun preferVideoMode(): Boolean {
        return Config.ROUND_VIDEOS_RECORD_SUPPORTED && checkSetting(FLAG_OTHER_PREFER_VIDEO_MODE)
    }

    fun setPreferVideoMode(preferVideoMode: Boolean) {
        var preferVideoMode = preferVideoMode
        preferVideoMode = Config.ROUND_VIDEOS_RECORD_SUPPORTED && preferVideoMode
        if (setSetting(FLAG_OTHER_PREFER_VIDEO_MODE, preferVideoMode)) {
            notifyVideoPreferenceListeners(preferVideoMode)
        }
    }

    fun needHqRoundVideos(): Boolean {
        return Device.NEED_HQ_ROUND_VIDEOS || checkSetting(FLAG_OTHER_HQ_ROUND_VIDEOS)
    }

    fun setNeedHqRoundVideos(needRoundVideos: Boolean) {
        setSetting(FLAG_OTHER_HQ_ROUND_VIDEOS, needRoundVideos)
    }

    fun startRoundWithRear(): Boolean {
        return checkSetting(FLAG_OTHER_START_ROUND_REAR)
    }

    fun setStartRoundWithRear(startWithRear: Boolean) {
        setSetting(FLAG_OTHER_START_ROUND_REAR, startWithRear)
    }

    fun needChatQuickShare(): Boolean {
        return checkNegativeSetting(FLAG_OTHER_NO_CHAT_QUICK_SHARE)
    }

    fun needChatQuickReply(): Boolean {
        return checkNegativeSetting(FLAG_OTHER_NO_CHAT_QUICK_REPLY)
    }

    fun setDisableChatQuickActions(disableChatQuickShare: Boolean, disableChatQuickReply: Boolean) {
        var newSettings = this.settings
        newSettings = setFlag(newSettings, FLAG_OTHER_NO_CHAT_QUICK_SHARE, disableChatQuickShare)
        newSettings = setFlag(newSettings, FLAG_OTHER_NO_CHAT_QUICK_REPLY, disableChatQuickReply)
        setSettings(newSettings)
    }

    fun useSystemEmoji(): Boolean {
        return Config.ALLOW_SYSTEM_EMOJI && checkSetting(FLAG_OTHER_USE_SYSTEM_EMOJI)
    }

    fun setUseSystemEmoji(useSystemEmoji: Boolean) {
        setSetting(FLAG_OTHER_USE_SYSTEM_EMOJI, useSystemEmoji)
    }

    fun dontReadMessages(): Boolean {
        return BuildConfig.DEBUG && checkSetting(FLAG_OTHER_DONT_READ_MESSAGES)
    }

    fun setDontReadMessages(dontReadMessages: Boolean) {
        var dontReadMessages = dontReadMessages
        if (dontReadMessages) {
            if (!BuildConfig.DEBUG || !Log.isEnabled(Log.TAG_MESSAGES_LOADER)) dontReadMessages = false
        }
        setSetting(FLAG_OTHER_DONT_READ_MESSAGES, dontReadMessages)
    }

    fun needSendByEnter(): Boolean {
        return checkSetting(FLAG_OTHER_SEND_BY_ENTER)
    }

    fun setNeedSendByEnter(needSendByEnter: Boolean) {
        setSetting(FLAG_OTHER_SEND_BY_ENTER, needSendByEnter)
    }

    fun needHideChatKeyboardOnScroll(): Boolean {
        return checkSetting(FLAG_OTHER_HIDE_CHAT_KEYBOARD)
    }

    fun setNeedHideChatKeyboardOnScroll(needHideChatKeyboardOnScroll: Boolean) {
        setSetting(FLAG_OTHER_HIDE_CHAT_KEYBOARD, needHideChatKeyboardOnScroll)
    }

    fun needPreviewChatOnHold(): Boolean {
        return checkNegativeSetting(FLAG_OTHER_DISABLE_PREVIEW_CHATS_ON_HOLD)
    }

    fun setNeedPreviewChatsOnHold(needPreviewChatsOnHold: Boolean) {
        setNegativeSetting(FLAG_OTHER_DISABLE_PREVIEW_CHATS_ON_HOLD, needPreviewChatsOnHold)
    }

    fun useInAppBrowser(): Boolean {
        return checkNegativeSetting(FLAG_OTHER_DISABLE_INAPP_BROWSER)
    }

    fun setUseInAppBrowser(useInAppBrowser: Boolean) {
        setNegativeSetting(FLAG_OTHER_DISABLE_INAPP_BROWSER, useInAppBrowser)
    }

    val nightMode: Int
        get() {
            if (_nightMode == null) {
                var nightMode = pmc.getInt(
                    KEY_NIGHT_MODE,
                    NIGHT_MODE_DEFAULT
                )
                if (nightMode == NIGHT_MODE_AUTO) {
                    try {
                        val sensorManager =
                            UI.getAppContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager?
                        if (sensorManager != null) {
                            if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
                                Log.e("Disabling night mode, because light sensor is unavailable")
                                nightMode = NIGHT_MODE_DEFAULT
                                remove(KEY_NIGHT_MODE)
                            }
                        }
                    } catch (t: Throwable) {
                        Log.w(t)
                    }
                }
                _nightMode = nightMode
            }
            return _nightMode!!
        }

    private var defaultLimits: MutableList<VideoLimit>? = null

    fun videoLimits(): MutableList<VideoLimit> {
        if (defaultLimits != null) {
            return defaultLimits!!
        }
        val maxTextureSize = U.getMaxTextureSize()
        val limits = arrayOf<VideoLimit>(
            VideoLimit(VideoSize(256, 144)),  // 144p
            VideoLimit(VideoSize(480, 360)),  // 360p
            VideoLimit(VideoSize(854, 480)),  // 480p
            VideoLimit(VideoSize(1024, 640)),  // 640p
            VideoLimit(VideoSize(1280, 720), 60),  // 720p | HD
            VideoLimit(VideoSize(1920, 1080), 60),  // 1080p | FullHD
            VideoLimit(VideoSize(3840, 2160), 60),  // 2160p | 4K
            VideoLimit(VideoSize(7680, 4320), 60) // 4320p | 8K
        )
        if (maxTextureSize <= 0) {
            return limits.toMutableList()
        }
        val result = java.util.ArrayList<VideoLimit>(limits.size)
        for (limit in limits) {
            if (limit.size.majorSize > maxTextureSize) {
                val scale = maxTextureSize.toFloat() / limit.size.majorSize.toFloat()
                var majorSize = (limit.size.majorSize.toFloat() * scale).toInt()
                var minorSize = (limit.size.minorSize.toFloat() * scale).toInt()
                majorSize -= majorSize % 2
                minorSize -= minorSize % 2
                if (result.isEmpty() || result[result.size - 1].size.majorSize < majorSize) {
                    result.add(limit.changeSize(VideoSize(majorSize, minorSize)))
                }
                break
            } else {
                result.add(limit)
            }
        }
        defaultLimits = result
        return result
    }

    class VideoSize(majorSize: Int, minorSize: Int) {
        @JvmField
        val majorSize: Int
        @JvmField
        val minorSize: Int

        init {
            this.majorSize = max(majorSize, minorSize)
            this.minorSize = min(minorSize, majorSize)
        }

        constructor(size: Int) : this(size, size)

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val videoSize = o as VideoSize
            return majorSize == videoSize.majorSize &&
                    minorSize == videoSize.minorSize
        }

        val isDefault: Boolean
            get() = majorSize == minorSize && majorSize == DEFAULT_VIDEO_LIMIT

        val isUnlimited: Boolean
            get() = majorSize == minorSize && majorSize == -1

        override fun hashCode(): Int {
            return hash(majorSize, minorSize)
        }
    }

    class VideoLimit {
        @JvmField
        val size: VideoSize
        @JvmField
        val fps: Int
        @JvmField
        val bitrate: Long

        @JvmOverloads
        constructor(size: VideoSize, fps: Int = DEFAULT_FRAME_RATE, bitrate: Long = BITRATE_UNKNOWN.toLong()) {
            this.size = size
            this.fps = fps
            this.bitrate = bitrate
        }

        fun unlimited(): VideoLimit {
            return VideoLimit(VideoSize(-1), fps, bitrate)
        }

        val isDefault: Boolean
            get() = size.isDefault && fps == DEFAULT_FRAME_RATE && bitrate == BITRATE_UNKNOWN.toLong()

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as VideoLimit
            return fps == that.fps && bitrate == that.bitrate &&
                    size == that.size
        }

        override fun hashCode(): Int {
            return hashCode(size, fps, bitrate)
        }

        constructor(copy: VideoLimit) {
            this.size = copy.size
            this.fps = copy.fps
            this.bitrate = copy.bitrate
        }

        fun changeSize(newSize: VideoSize): VideoLimit {
            return VideoLimit(newSize, this.fps, this.bitrate)
        }

        @JvmOverloads
        constructor(data: IntArray? = null as IntArray?) {
            if (data != null && data.size > 0) {
                this.size = VideoSize(data[0], if (data.size > 1) data[1] else data[0])
                this.fps = if (data.size > 2) data[2] else DEFAULT_FRAME_RATE
                this.bitrate = if (data.size > 3) BitUnit.KBIT.toBits(data[3].toDouble()).toLong() else BITRATE_UNKNOWN.toLong()
            } else {
                this.size = VideoSize(DEFAULT_VIDEO_LIMIT)
                this.fps = DEFAULT_FRAME_RATE
                this.bitrate = BITRATE_UNKNOWN.toLong()
            }
        }

        fun getOutputFrameRate(frameRate: Int): Int {
            return min(
                if (frameRate > 0) frameRate else DEFAULT_FRAME_RATE,
                if (fps > 0) fps else DEFAULT_FRAME_RATE
            )
        }

        fun getOutputBitrate(size: VideoSize, frameRate: Int, inputBitrate: Long): Long {
            return Math.round((size.majorSize * size.minorSize * frameRate) * BITRATE_SCALE)
        }

        fun getOutputSize(width: Int, height: Int): VideoSize? {
            var majorSize = max(width, height)
            var minorSize = min(width, height)
            val ratio = min(
                this.size.majorSize.toFloat() / majorSize.toFloat(),
                this.size.minorSize.toFloat() / minorSize.toFloat()
            )
            if (ratio > 1f) return null
            majorSize = (majorSize.toFloat() * ratio).toInt()
            minorSize = (minorSize.toFloat() * ratio).toInt()
            if (majorSize % 2 == 1) majorSize--
            if (minorSize % 2 == 1) minorSize--
            return VideoSize(majorSize, minorSize)
        }

        fun toIntArray(): IntArray? {
            if (this.isDefault) return null
            val dataSize =  /*bitrate != DefaultVideoStrategy.BITRATE_UNKNOWN ? 4 :*/ // never saving the bitrate as it is calculated automatically
                if (fps != DEFAULT_FRAME_RATE) 3 else if (size.majorSize != size.minorSize) 2 else if (size.majorSize != 0) 1 else 0
            if (dataSize == 0) return null
            val result = IntArray(dataSize)
            result[0] = size.majorSize
            if (result.size > 1) result[1] = size.minorSize
            if (result.size > 2) result[2] = fps
            if (result.size > 3) result[3] = Math.round(BitUnit.BIT.toKbit(bitrate.toDouble())).toInt()
            return result
        }

        companion object {
            @JvmField
            val BITRATE_UNKNOWN: Int = -1

            const val BITRATE_SCALE: Double = 0.109
        }
    }

    fun setAutoNightMode(autoNightMode: Int) {
        val oldNightMode = this.nightMode
        if (oldNightMode != autoNightMode) {
            this._nightMode = autoNightMode
            if (autoNightMode == NIGHT_MODE_DEFAULT) {
                remove(KEY_NIGHT_MODE)
            } else {
                putInt(KEY_NIGHT_MODE, autoNightMode)
            }
            ThemeManager.instance().notifyAutoNightModeChanged(autoNightMode)
            if (autoNightMode == NIGHT_MODE_SCHEDULED || oldNightMode == NIGHT_MODE_SCHEDULED) {
                checkNightModeScheduler(true)
            }
        }
    }

    val maxNightLux: Float
        get() {
            if (_nightModeAutoLux == null) _nightModeAutoLux = pmc.getFloat(
                KEY_MAX_NIGHT_LUX,
                MAX_NIGHT_LUX_DEFAULT
            )
            return _nightModeAutoLux!!
        }

    fun setNightModeMaxLux(lux: Float, save: Boolean): Boolean {
        val changed = this.maxNightLux != lux
        this._nightModeAutoLux = lux
        if (save) {
            putFloat(KEY_MAX_NIGHT_LUX, lux)
        }
        return changed
    }

    val nightModeScheduleOn: Int
        get() = splitLongToFirstInt(this.nightModeSchedule)

    val nightModeScheduleOff: Int
        get() = splitLongToSecondInt(this.nightModeSchedule)

    fun inNightSchedule(): Boolean {
        if (this.nightMode == NIGHT_MODE_SCHEDULED) {
            val nightModeSchedule = this.nightModeSchedule
            val startTime = splitLongToFirstInt(nightModeSchedule)
            val endTime = splitLongToSecondInt(nightModeSchedule)
            if (startTime != endTime) {
                val c = getNowCalendar()
                val hour = c.get(Calendar.HOUR_OF_DAY)
                val minute = c.get(Calendar.MINUTE)
                val second = c.get(Calendar.SECOND)
                val time = mergeTimeToInt(hour, minute, second)
                return belongsToSchedule(time, startTime, endTime)
            }
        }
        return false
    }

    fun checkNightModeScheduler(needCancel: Boolean) {
        if (needCancel) {
            cancelNightScheduleCheck()
        }
        if (this.nightMode != NIGHT_MODE_SCHEDULED) {
            return
        }
        val nightModeSchedule = this.nightModeSchedule
        val startTime = splitLongToFirstInt(nightModeSchedule)
        val endTime = splitLongToSecondInt(nightModeSchedule)
        if (startTime == endTime) {
            ThemeManager.instance().setInNightMode(false, true)
            if (needCancel) {
                cancelNightScheduleCheck()
            }
            return
        }

        val now = System.currentTimeMillis()
        val c = calendarInstance(now)
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)
        val time = mergeTimeToInt(hour, minute, second)

        val inNightSchedule = belongsToSchedule(time, startTime, endTime)
        ThemeManager.instance().setInNightMode(inNightSchedule, true)
        val targetTime = if (inNightSchedule) endTime else startTime

        if (time > targetTime) {
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
        c.set(Calendar.HOUR_OF_DAY, splitIntToHour(targetTime))
        c.set(Calendar.MINUTE, splitIntToMinute(targetTime))
        c.set(Calendar.SECOND, splitIntToSecond(targetTime))

        val future = c.getTimeInMillis()
        if (future < now) {
            throw RuntimeException(
                "Theme schedule failed [time: " + U.timeToString(time) + ", startTime: " + U.timeToString(startTime) + ", endTime: " + U.timeToString(
                    endTime
                ) + "]"
            )
        }

        scheduleNightScheduleCheck(future - now)
    }

    fun setNightModeSchedule(time: Int, isOn: Boolean) {
        val nightModeSchedule = this.nightModeSchedule
        setNightModeSchedule(mergeLong(if (isOn) time else splitLongToFirstInt(nightModeSchedule), if (isOn) splitLongToSecondInt(nightModeSchedule) else time))
    }

    private val nightModeSchedule: Long
        get() {
            if (_nightModeSchedule == null) _nightModeSchedule = pmc.getLong(
                KEY_NIGHT_MODE_SCHEDULED_TIME,
                mergeLong(mergeTimeToInt(22, 0, 0), mergeTimeToInt(7, 0, 0))
            )
            return _nightModeSchedule!!
        }

    fun setNightModeSchedule(newSchedule: Long): Boolean {
        if (this.nightModeSchedule != newSchedule) {
            this._nightModeSchedule = newSchedule
            putLong(KEY_NIGHT_MODE_SCHEDULED_TIME, newSchedule)
            if (this.nightMode == NIGHT_MODE_SCHEDULED) {
                checkNightModeScheduler(true)
            }
            return true
        }
        return false
    }

    fun needOutboundCallsPrompt(): Boolean {
        return checkSetting(FLAG_OTHER_OUTBOUND_CALLS_PROMPT)
    }

    fun setNeedOutboundCallsPrompt(needPrompt: Boolean) {
        setSetting(FLAG_OTHER_OUTBOUND_CALLS_PROMPT, needPrompt)
    }

    fun useCustomVibrations(): Boolean {
        return checkNegativeSetting(FLAG_OTHER_DISABLE_CUSTOM_VIBRATIONS)
    }

    fun setUseCustomVibrations(useCustomVibrations: Boolean) {
        setNegativeSetting(FLAG_OTHER_DISABLE_CUSTOM_VIBRATIONS, useCustomVibrations)
    }

    var cameraAspectRatioMode: Int
        get() = pmc.getInt(
            KEY_CAMERA_ASPECT_RATIO,
            CAMERA_RATIO_16_9
        )
        set(ratioMode) {
            if (ratioMode == CAMERA_RATIO_16_9) {
                pmc.remove(KEY_CAMERA_ASPECT_RATIO)
            } else {
                pmc.putInt(KEY_CAMERA_ASPECT_RATIO, ratioMode)
            }
        }

    val cameraAspectRatio: Float
        get() {
            val mode = this.cameraAspectRatioMode
            when (mode) {
                CAMERA_RATIO_1_1 -> return 1f
                CAMERA_RATIO_4_3 -> return 4f / 3f
                CAMERA_RATIO_FULL_SCREEN -> return 0f
                CAMERA_RATIO_16_9 -> return 16f / 9f
                else -> return 16f / 9f
            }
        }

    var cameraType: Int
        get() {
            if (!Config.CUSTOM_CAMERA_AVAILABLE) {
                return CAMERA_TYPE_SYSTEM
            }
            val type = pmc.getInt(
                KEY_CAMERA_TYPE,
                CAMERA_TYPE_DEFAULT
            )
            return if (type != CAMERA_TYPE_X || Config.CAMERA_X_AVAILABLE) type else CAMERA_TYPE_DEFAULT
        }
        set(type) {
            if (type == CAMERA_TYPE_DEFAULT) {
                pmc.remove(KEY_CAMERA_TYPE)
            } else {
                pmc.putInt(KEY_CAMERA_TYPE, type)
            }
        }

    var cameraVolumeControl: Int
        get() = pmc.getInt(
            KEY_CAMERA_VOLUME_CONTROL,
            CAMERA_VOLUME_CONTROL_SHOOT
        )
        set(type) {
            if (type == CAMERA_VOLUME_CONTROL_SHOOT) {
                pmc.remove(KEY_CAMERA_VOLUME_CONTROL)
            } else {
                pmc.putInt(KEY_CAMERA_VOLUME_CONTROL, type)
            }
        }

    // Font sizes
    interface FontSizeChangeListener {
        fun onFontSizeChanged(newSizeDp: Float)
    }

    private val chatFontSizeChangeListeners: MutableList<Reference<FontSizeChangeListener?>> = java.util.ArrayList<Reference<FontSizeChangeListener?>>()

    val chatFontSize: Float
        get() {
            if (_chatFontSize == null) {
                var chatFontSize = pmc.getFloat(
                    KEY_CHAT_FONT_SIZE,
                    CHAT_FONT_SIZE_DEFAULT
                )
                if (!isGoodChatFontSize(chatFontSize)) {
                    chatFontSize = CHAT_FONT_SIZE_DEFAULT
                }
                _chatFontSize = chatFontSize
            }
            return _chatFontSize!!
        }

    fun canResetChatFontSize(): Boolean {
        return this.chatFontSize != CHAT_FONT_SIZE_DEFAULT || needChatFontSizeScaling()
    }

    fun needChatFontSizeScaling(): Boolean {
        return checkSetting(FLAG_OTHER_FONT_SCALING)
    }

    fun setNeedChatFontSizeScaling(need: Boolean) {
        if (setSetting(FLAG_OTHER_FONT_SCALING, need)) {
            notifyFontSizeListeners(chatFontSizeChangeListeners, this.chatFontSize)
        }
    }

    fun toggleChatFontSizeScaling() {
        setNeedChatFontSizeScaling(!needChatFontSizeScaling())
    }

    fun resetChatFontSize() {
        setNeedChatFontSizeScaling(false)
        setChatFontSize(CHAT_FONT_SIZE_DEFAULT)
    }

    fun setChatFontSize(fontSize: Float): Boolean {
        if (!isGoodChatFontSize(fontSize)) return false
        val oldFontSize = this.chatFontSize
        if (oldFontSize != fontSize) {
            this._chatFontSize = fontSize
            putFloat(KEY_CHAT_FONT_SIZE, fontSize)
            notifyFontSizeListeners(chatFontSizeChangeListeners, fontSize)
            return true
        }
        return false
    }

    fun addChatFontSizeChangeListener(listener: FontSizeChangeListener?) {
        addReference<FontSizeChangeListener?>(chatFontSizeChangeListeners, listener)
    }

    // Logs
    fun hasLogsEnabled(): Boolean {
        return logSettings.isEnabled || Log.getLogLevel() > Log.LEVEL_ASSERT
    }

    fun applyLogSettings(async: Boolean) {
        logSettings.apply(async)
    }

    fun disableAllLogs() {
        logSettings.disable()
        Log.setLogLevel(Log.LEVEL_ASSERT)
    }

    fun deleteAllLogs(withTdlibLogs: Boolean, after: Runnable?) {
        Background.instance().post(Runnable {
            Log.deleteAll(Log.getLogFiles(), RunnableData { futureLogs: LogFiles? ->
                if (withTdlibLogs) {
                    TdlibManager.deleteAllLogFiles()
                }
                if (after != null) after.run()
            }, null)
        })
    }

    fun setAllowSpecialTdlibInstanceMode(accountId: Int, @Tdlib.Mode instanceMode: Int) {
        // Additional protection against corrupted accounts list file
        val allowSpecialInstanceMode =
            instanceMode == Tdlib.Mode.SERVICE || instanceMode == Tdlib.Mode.DEBUG
        val key: String = KEY_TDLIB_DEBUG_PREFIX + accountId
        if (allowSpecialInstanceMode) {
            pmc.putVoid(key)
        } else {
            pmc.remove(key)
        }
    }

    fun allowSpecialTdlibInstanceMode(accountId: Int): Boolean {
        return pmc.contains(KEY_TDLIB_DEBUG_PREFIX + accountId)
    }

    var emojiPosition: Int
        // EmojiLayout
        get() = getInt(KEY_EMOJI_POSITION, 0)
        set(position) {
            putInt(KEY_EMOJI_POSITION, position)
        }

    var emojiMediaSection: Int
        get() = getInt(KEY_EMOJI_MEDIA_SECTION, EmojiMediaType.STICKER)
        set(section) {
            putInt(KEY_EMOJI_MEDIA_SECTION, section)
        }

    var isIntroAttempted: Boolean
        get() = getBoolean(KEY_INTRO_ATTEMPTED, false)
        // Intro
        set(isAttempted) {
            if (isAttempted) {
                putBoolean(KEY_INTRO_ATTEMPTED, true)
            } else {
                remove(KEY_INTRO_ATTEMPTED)
            }
        }

    var mapType: Int
        get() = getInt(
            KEY_MAP_TYPE,
            MAP_TYPE_UNSET
        )
        set(mapType) {
            if (mapType == MAP_TYPE_UNSET) {
                remove(KEY_MAP_TYPE)
            } else {
                putInt(KEY_MAP_TYPE, mapType)
            }
        }

    fun getMapProviderType(cloud: Boolean): Int {
        if (cloud) {
            if (_mapProviderTypeCloud == null) _mapProviderTypeCloud = pmc.getInt(KEY_MAP_PROVIDER_TYPE_CLOUD, MAP_PROVIDER_DEFAULT_CLOUD)
            return _mapProviderTypeCloud!!
        } else {
            if (_mapProviderType == null) _mapProviderType = pmc.getInt(KEY_MAP_PROVIDER_TYPE, MAP_PROVIDER_UNSET)
            return _mapProviderType!!
        }
    }

    fun setMapProviderType(mapProviderType: Int, cloud: Boolean) {
        if (cloud) {
            this._mapProviderTypeCloud = mapProviderType
            putInt(KEY_MAP_PROVIDER_TYPE_CLOUD, mapProviderType)
        } else {
            this._mapProviderType = mapProviderType
            putInt(KEY_MAP_PROVIDER_TYPE, mapProviderType)
        }
    }

    fun deleteWallpapers(tdlib: Tdlib, editor: SharedPreferences.Editor) {
        val accountId = tdlib.id()
        deleteWallpaper(tdlib, editor, 0)
        deleteWallpaper(tdlib, editor, 1)
        var key: String = (if (accountId != 0) KEY_WALLPAPER_PREFIX + "_" + accountId else KEY_WALLPAPER_PREFIX) + "_other"
        pmc.removeByPrefix(key)
        key = (if (accountId != 0) KEY_WALLPAPER_PREFIX + "_" + accountId else KEY_WALLPAPER_PREFIX) + "_chat"
        pmc.removeByPrefix(key)
    }

    fun deleteWallpaper(tdlib: Tdlib, editor: SharedPreferences.Editor, wallpaperIdentifier: Int) {
        deleteWallpaper(tdlib.id(), editor, wallpaperIdentifier)
    }

    fun deleteWallpaper(accountId: Int, editor: SharedPreferences.Editor, wallpaperIdentifier: Int) {
        val key: String =
            (if (accountId != 0) KEY_WALLPAPER_PREFIX + "_" + accountId else KEY_WALLPAPER_PREFIX) + getWallpaperIdentifierSuffix(wallpaperIdentifier)
        editor
            .remove(key)
            .remove(key + KEY_WALLPAPER_EMPTY)
            .remove(key + KEY_WALLPAPER_CUSTOM)
            .remove(key + KEY_WALLPAPER_PATH)
            .remove(key + KEY_WALLPAPER_ID)
    }

    // Last known location TODO: Binary format
    class LastLocation : BlobEntry {
        @JvmField
        var latitude: Double = 0.0
        @JvmField
        var longitude: Double = 0.0
        @JvmField
        var zoomOrAccuracy: Float = 0f

        constructor()

        constructor(latitude: Double, longitude: Double, zoomOrAccuracy: Float) {
            this.latitude = latitude
            this.longitude = longitude
            this.zoomOrAccuracy = zoomOrAccuracy
        }

        override fun estimatedBinarySize(): Int {
            return 8 + 8 + 4
        }

        override fun saveTo(blob: Blob) {
            blob.writeDouble(latitude)
            blob.writeDouble(longitude)
            blob.writeFloat(zoomOrAccuracy)
        }

        override fun restoreFrom(blob: Blob) {
            latitude = blob.readDouble()
            longitude = blob.readDouble()
            zoomOrAccuracy = blob.readFloat()
        }
    }

    val viewedLocation: LastLocation?
        get() = parseLocation(pmc.getByteArray(KEY_LAST_LOCATION))

    fun setViewedLocation(latitude: Double, longitude: Double, zoom: Float) {
        val buffer = ByteArray(8 + 8 + 4)
        writeDouble(buffer, 0, latitude)
        writeDouble(buffer, 8, longitude)
        writeFloat(buffer, 8 + 8, zoom)
        pmc.putByteArray(KEY_LAST_LOCATION, buffer)
    }

    fun saveLastKnownLocation(latitude: Double, longitude: Double, accuracy: Float) {
        val buffer = ByteArray(8 + 8 + 4)
        writeDouble(buffer, 0, latitude)
        writeDouble(buffer, 8, longitude)
        writeFloat(buffer, 8 + 8, accuracy)
        pmc.putByteArray(KEY_LAST_INLINE_LOCATION, buffer)
    }

    val lastKnownLocation: LastLocation?
        get() = parseLocation(pmc.getByteArray(KEY_LAST_INLINE_LOCATION))

    val pipPosition: Long
        // PiP
        get() = getLong(KEY_PIP, mergeLong(if (Lang.rtl()) -1 else 1, -1))

    fun setPipPosition(x: Float, y: Float) {
        putLong(KEY_PIP, mergeLong(sign(x).toInt(), sign(y).toInt()))
    }

    var pipGravity: Int
        get() = getInt(KEY_PIP_GRAVITY, Gravity.TOP or Gravity.RIGHT)
        set(gravity) {
            if (gravity == (Gravity.TOP or Gravity.RIGHT)) {
                remove(KEY_PIP_GRAVITY)
            } else {
                putInt(KEY_PIP_GRAVITY, gravity)
            }
        }

    var paintId: Int
        // Paint
        get() = getInt(KEY_PAINT_ID, 0)
        set(paintId) {
            putInt(KEY_PAINT_ID, paintId)
        }

    var playerFlags: Int
        // TGPlayerController
        get() = getInt(KEY_PLAYER_FLAGS, TGPlayerController.PLAY_FLAGS_DEFAULT)
        set(flags) {
            putInt(KEY_PLAYER_FLAGS, flags)
        }

    // Whether user requested to close a keyboard
    fun shouldKeepKeyboardClosed(accountId: Int, chatId: Long, messageId: Long): Boolean {
        return getLong(key(KEY_HIDE_BOT_KEYBOARD_PREFIX + chatId, accountId), 0) == messageId
    }

    fun onRequestKeyboardClose(accountId: Int, chatId: Long, messageId: Long, close: Boolean) {
        if (close) {
            putLong(key(KEY_HIDE_BOT_KEYBOARD_PREFIX + chatId, accountId), messageId)
        } else {
            remove(key(KEY_HIDE_BOT_KEYBOARD_PREFIX + chatId, accountId))
        }
    }

    // Bots
    fun allowLocationForBot(userId: Long): Boolean {
        return getBoolean("allow_location_" + userId, false)
    }

    fun setAllowLocationForBot(userId: Long) {
        putBoolean("allow_location_" + userId, true)
    }

    // Keyboard
    fun setKeyboardSize(orientation: Int, size: Int) {
        putInt("keyboard_size" + orientation, size)
    }

    fun getKeyboardSize(orientation: Int, defSize: Int): Int {
        var size = getInt("keyboard_size" + orientation, 0)
        if (size <= 0) {
            size = defSize
        }
        return max(size, Screen.dp(75f))
    }

    // Emoji
    private fun saveBinaryMap(storageKey: String, map: MutableMap<String?, out BlobEntry?>) {
        val mapSize = map.size
        var binarySize = sizeOf(mapSize)
        for (entry in map.entries) {
            val key: String = entry.key!!
            val value: BlobEntry = entry.value!!
            binarySize += key.length + value.estimatedBinarySize()
        }
        val blob = Blob(binarySize)
        blob.writeVarint(mapSize)
        for (entry in map.entries) {
            blob.writeString(entry.key!!)
            entry.value!!.saveTo(blob)
        }
        pmc.putByteArray(storageKey, blob.toByteArray())
    }

    private fun <T : BlobEntry> getBinaryMap(storageKey: String, out: MutableMap<String?, T?>, clazz: Class<T>) {
        val data = pmc.getByteArray(storageKey)
        if (data == null || data.size == 0) {
            return
        }
        try {
            val blob = Blob(data)
            val size = blob.readVarint()
            for (i in 0..<size) {
                val key = blob.readString()
                val value = clazz.newInstance()
                value.restoreFrom(blob)
                out.put(key, value)
            }
        } catch (t: Throwable) {
            Log.e("Unable to get binary map", t)
        }
    }

    fun saveBinaryList(storageKey: String, list: MutableList<BlobEntry>) {
        val listSize = list.size
        var binarySize = sizeOf(listSize)
        for (item in list) {
            binarySize += item.estimatedBinarySize()
        }
        val blob = Blob(binarySize)
        blob.writeVarint(binarySize)
        for (entry in list) {
            entry.saveTo(blob)
        }
        pmc.putByteArray(storageKey, blob.toByteArray())
    }

    fun getBinaryList(storageKey: String, clazz: Class<out BlobEntry>): MutableList<BlobEntry?>? {
        val data = pmc.getByteArray(storageKey)
        if (data == null) {
            return null
        }
        try {
            val blob = Blob(data)
            val listSize = blob.readVarint()
            val list: MutableList<BlobEntry?> = java.util.ArrayList<BlobEntry?>(listSize)
            for (i in 0..<listSize) {
                val entry: BlobEntry = clazz.newInstance()
                entry.restoreFrom(blob)
                list.add(entry)
            }
            return list
        } catch (t: Throwable) {
            Log.w("Cannot read binary list, key:%s", storageKey)
        }
        return null
    }

    fun setEmojiCounters(infos: MutableMap<String?, RecentInfo?>) {
        saveBinaryMap(KEY_EMOJI_COUNTERS, infos)
    }

    fun setEmojiRecents(recents: MutableList<RecentEmoji>) {
        val out = arrayOfNulls<String>(recents.size)
        var i = 0
        for (recent in recents) {
            out[i++] = recent.emoji
        }
        pmc.putStringArray(KEY_EMOJI_RECENTS, out)
    }

    fun clearEmojiRecents() {
        pmc.edit().remove(KEY_EMOJI_COUNTERS).remove(KEY_EMOJI_RECENTS).apply()
    }

    fun getEmojiCounters(infos: MutableMap<String?, RecentInfo?>) {
        getBinaryMap<RecentInfo>(KEY_EMOJI_COUNTERS, infos, RecentInfo::class.java)
    }

    fun getEmojiRecents(infos: MutableMap<String?, RecentInfo?>, recents: MutableList<RecentEmoji?>) {
        val emojis = pmc.getStringArray(KEY_EMOJI_RECENTS)
        if (emojis != null && emojis.size > 0) {
            for (emoji in emojis) {
                val info = infos.get(emoji)
                if (info != null) {
                    recents.add(RecentEmoji(emoji, info))
                }
            }
        }
    }

    fun getEmojiOtherColors(otherColors: MutableMap<String?, Array<String?>?>) {
        val array = pmc.getStringArray(KEY_EMOJI_OTHER_COLORS)
        if (array != null && array.size > 0) {
            var key: String? = null
            for (value in array) {
                if (key == null) {
                    key = if (value == null) "" else value
                } else {
                    otherColors.put(key, value!!.split(EMOJI_OTHER_COLORS_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    key = null
                }
            }
        }
    }

    fun setEmojiOtherColors(otherColors: MutableMap<String?, Array<String?>?>, editor: SharedPreferences.Editor?) {
        var editor = editor
        var ownsEditor = false
        if (editor == null) {
            editor = edit()
            ownsEditor = true
        }
        val size = otherColors.size
        if (size > 0) {
            val result = arrayOfNulls<String>(size * 2)
            var i = 0
            for (entry in otherColors.entries) {
                result[i++] = entry.key
                result[i++] = Strings.join(EMOJI_OTHER_COLORS_SEPARATOR, *(entry.value!! as Array<Any?>))
            }
            pmc.putStringArray(KEY_EMOJI_OTHER_COLORS, result)
        } else {
            editor.remove(KEY_EMOJI_OTHER_COLORS)
        }
        if (ownsEditor) {
            editor.apply()
        }
    }

    fun getEmojiColors(colors: MutableMap<String?, String?>) {
        val array = pmc.getStringArray(KEY_EMOJI_COLORS)
        if (array != null && array.size > 0) {
            var key: String? = null
            for (value in array) {
                if (key == null) {
                    key = if (value == null) "" else value
                } else {
                    colors.put(key, value)
                    key = null
                }
            }
        }
    }

    fun setEmojiColors(colors: MutableMap<String?, String?>, editor: SharedPreferences.Editor?) {
        var editor = editor
        var ownsEditor = false
        if (editor == null) {
            editor = edit()
            ownsEditor = true
        }
        val size = colors.size
        if (size > 0) {
            val result = arrayOfNulls<String>(size * 2)
            var i = 0
            for (entry in colors.entries) {
                result[i++] = entry.key
                result[i++] = entry.value
            }
            pmc.putStringArray(KEY_EMOJI_COLORS, result)
        } else {
            editor.remove(KEY_EMOJI_COLORS)
        }
        if (ownsEditor) {
            editor.apply()
        }
    }

    val emojiDefaultTone: String?
        get() = getString(KEY_EMOJI_DEFAULT_COLOR, null)

    fun setEmojiDefaultTone(defaultTone: String, colors: MutableMap<String?, String?>) {
        if (colors.size > 0) {
            val editor: SharedPreferences.Editor = edit()
            if (isEmpty(defaultTone)) {
                editor.remove(KEY_EMOJI_DEFAULT_COLOR)
            } else {
                editor.putString(KEY_EMOJI_DEFAULT_COLOR, defaultTone)
            }
            colors.clear()
            setEmojiColors(colors, editor)
            editor.apply()
        } else {
            if (isEmpty(defaultTone)) {
                remove(KEY_EMOJI_DEFAULT_COLOR)
            } else {
                putString(KEY_EMOJI_DEFAULT_COLOR, defaultTone)
            }
        }
    }

    // Scroll offsets
    class SavedMessageId(@JvmField val id: MessageId, @JvmField val offsetPixels: Int, @JvmField val returnToMessageIds: LongArray?, @JvmField val readFully: Boolean, @JvmField val topEndMessageId: Long) {
        class Builder(internal var messageChatId: Long) {
            internal var messageId: Long = 0
            internal var topEndMessageId: Long = 0
            internal var otherMessageIds: LongArray? = null

            internal var offsetPixels = 0
            internal var returnToMessageIds: LongArray? = null
            internal var readFully = false

            fun build(): SavedMessageId {
                return SavedMessageId(MessageId(messageChatId, messageId, otherMessageIds), offsetPixels, returnToMessageIds, readFully, topEndMessageId)
            }
        }
    }

    fun removeScrollPositions(accountId: Int, editor: SharedPreferences.Editor?) {
        removeByPrefix(key(KEY_SCROLL_CHAT_PREFIX, accountId), editor)
    }

    fun setScrollMessageId(accountId: Int, chatId: Long, topicId: MessageTopic?, savedMessageId: SavedMessageId?) {
        val keyId: String = makeScrollChatKey(KEY_SCROLL_CHAT_MESSAGE_ID, accountId, chatId, topicId)
        val keyChatId: String = makeScrollChatKey(KEY_SCROLL_CHAT_MESSAGE_CHAT_ID, accountId, chatId, topicId)
        val keyReturnToIds: String = makeScrollChatKey(KEY_SCROLL_CHAT_RETURN_TO_MESSAGE_IDS_STACK, accountId, chatId, topicId)
        val keyAliases: String = makeScrollChatKey(KEY_SCROLL_CHAT_ALIASES, accountId, chatId, topicId)
        val keyOffset: String = makeScrollChatKey(KEY_SCROLL_CHAT_OFFSET, accountId, chatId, topicId)
        val keyReadFully: String = makeScrollChatKey(KEY_SCROLL_CHAT_READ_FULLY, accountId, chatId, topicId)
        val keyTopEnd: String = makeScrollChatKey(KEY_SCROLL_CHAT_TOP_END, accountId, chatId, topicId)
        val editor: SharedPreferences.Editor = edit()
        if (savedMessageId == null) {
            editor
                .remove(keyId)
                .remove(keyChatId)
                .remove(keyReturnToIds)
                .remove(keyOffset)
                .remove(keyAliases)
                .remove(keyReadFully)
                .remove(keyTopEnd)
        } else {
            editor.putLong(keyId, savedMessageId.id.messageId)
            if (savedMessageId.id.chatId != chatId) {
                editor.putLong(keyChatId, savedMessageId.id.chatId)
            } else {
                editor.remove(keyChatId)
            }
            if (savedMessageId.id.otherMessageIds != null && savedMessageId.id.otherMessageIds!!.size > 0) {
                (editor as LevelDB).putLongArray(keyAliases, savedMessageId.id.otherMessageIds!!)
            } else {
                editor.remove(keyAliases)
            }

            if (savedMessageId.offsetPixels != 0) {
                editor.putInt(keyOffset, savedMessageId.offsetPixels)
            } else {
                editor.remove(keyOffset)
            }

            if (savedMessageId.returnToMessageIds != null && savedMessageId.returnToMessageIds.size > 0) (editor as LevelDB).putLongArray(
                keyReturnToIds,
                savedMessageId.returnToMessageIds
            )
            else editor.remove(keyReturnToIds)

            if (savedMessageId.readFully) {
                editor.putBoolean(keyReadFully, true)
            } else {
                editor.remove(keyReadFully)
            }

            if (savedMessageId.topEndMessageId != 0L) {
                editor.putLong(keyTopEnd, savedMessageId.topEndMessageId)
            } else {
                editor.remove(keyTopEnd)
            }
        }
        editor.apply()
    }

    fun getScrollMessageId(accountId: Int, chatId: Long, topicId: MessageTopic?): SavedMessageId? {
        val prefix: String = makeScrollChatKey(null, accountId, chatId, null)
        val topicSuffix = if (topicId != null) "_" + topicId.cacheKey() else null
        var b: SavedMessageId.Builder? = null
        for (entry in pmc.find(prefix)) {
            val key = entry.key()
            val mismatch: Boolean
            if (isEmpty(topicSuffix)) {
                if (Config.COMPILE_CHECK) {
                    assertMessageTopic_98b4a9a3()
                }
                mismatch = key.matches("^.+_(?:thread|forum|direct|saved)+\\d+$".toRegex())
            } else {
                mismatch = !key.endsWith(topicSuffix!!)
            }
            if (mismatch) {
                continue
            }
            if (b == null) {
                b = SavedMessageId.Builder(chatId)
            }
            val dataKey = key.substring(prefix.length, key.length - length(topicSuffix))
            when (dataKey) {
                KEY_SCROLL_CHAT_MESSAGE_ID -> b.messageId = entry.asLong()
                KEY_SCROLL_CHAT_MESSAGE_CHAT_ID -> b.messageChatId = entry.asLong()
                KEY_SCROLL_CHAT_ALIASES -> b.otherMessageIds = entry.asLongArray()
                KEY_SCROLL_CHAT_OFFSET -> b.offsetPixels = entry.asInt()
                KEY_SCROLL_CHAT_RETURN_TO_MESSAGE_IDS_STACK -> b.returnToMessageIds = entry.asLongArray()
                KEY_SCROLL_CHAT_READ_FULLY -> b.readFully = entry.asBoolean()
                KEY_SCROLL_CHAT_TOP_END -> b.topEndMessageId = entry.asLong()
            }
        }
        if (b != null && b.messageChatId != chatId) {
            b.messageId = 0 // TODO support on chat level
        }
        return if (b != null) b.build() else null
    }

    fun updateScrollMessageId(accountId: Int, chatId: Long, oldMessageId: Long, newMessageId: Long) {
        val prefix: String = key(KEY_SCROLL_CHAT_PREFIX + chatId, accountId)
        var editor: SharedPreferences.Editor? = null
        for (entry in pmc.find(prefix)) {
            val suffix = entry.key().substring(prefix.length).replace("_thread[\\d]+$".toRegex(), "")
            when (suffix) {
                KEY_SCROLL_CHAT_MESSAGE_ID -> {
                    if (entry.asLong() == oldMessageId) {
                        if (editor == null) {
                            editor = edit()
                        }
                        editor.putLong(entry.key(), newMessageId)
                    }
                }

                KEY_SCROLL_CHAT_ALIASES, KEY_SCROLL_CHAT_RETURN_TO_MESSAGE_IDS_STACK -> {
                    val messageIds = entry.asLongArray()
                    val index = if (messageIds != null) indexOf(messageIds, oldMessageId) else -1
                    if (index >= 0) {
                        messageIds!![index] = newMessageId
                        Arrays.sort(messageIds)
                        if (editor == null) {
                            editor = edit()
                        }
                        (editor as LevelDB).putLongArray(entry.key(), messageIds)
                    }
                }
            }
        }
        if (editor != null) {
            editor.apply()
        }
    }

    var isAccountListOpened: Boolean
        // Other settings
        get() = checkSetting(FLAG_OTHER_ACCOUNT_LIST_OPENED)
        set(isOpened) {
            setSetting(FLAG_OTHER_ACCOUNT_LIST_OPENED, isOpened)
        }

    val effectiveProxyId: Int
        /**
         * @return Identifier of proxy to be applied to TDLib client instances.
         * Returns [.PROXY_ID_NONE] in case [.PROXY_FLAG_ENABLED] is not set.
         */
        get() {
            if (hasFlag(this.proxySettings, PROXY_FLAG_ENABLED)) {
                return this.availableProxyId
            }
            return PROXY_ID_NONE
        }

    val effectiveCallsProxyId: Int
        /**
         * @return Identifier of proxy to be applied to libtgvoip
         */
        get() {
            val settings = this.proxySettings
            if (hasFlag(settings, PROXY_FLAG_ENABLED) && hasFlag(
                    settings,
                    PROXY_FLAG_USE_FOR_CALLS
                )
            ) {
                return this.availableProxyId
            }
            return PROXY_ID_NONE
        }

    val availableProxyId: Int
        /**
         * @return Identifier of proxy to be applied to TDLib client instances.
         * Returns proxy identifier even when [.PROXY_FLAG_ENABLED] is not set.
         */
        get() = pmc.getInt(
            KEY_PROXY_CURRENT,
            PROXY_ID_NONE
        )

    val availableProxyCount: Int
        /**
         * @return Number of available proxy configurations
         */
        get() = pmc.getSizeByPrefix(KEY_PROXY_PREFIX_CONFIG).toInt()

    /**
     * @return true if there is at least one available proxy configuration
     */
    fun hasProxyConfiguration(): Boolean {
        return this.availableProxyCount > 0
    }

    val proxySettings: Int
        /**
         * @return Current proxy flags.
         */
        get() = pmc.getByte(KEY_PROXY_SETTINGS, 0.toByte()).toInt()

    /**
     * @param proxyFlag Proxy flag to check
     * @return Whether proxy flag is turned on
     */
    fun checkProxySetting(proxyFlag: Int): Boolean {
        return (this.proxySettings and proxyFlag) != 0
    }

    /**
     * Toggles proxy setting.
     * 
     * @param setting flag to be toggled
     * @return changed value of the flag.
     */
    fun toggleProxySetting(setting: Int): Boolean {
        val settings = this.proxySettings
        return setProxySettingImpl(settings, setting, (settings and setting) == 0)
    }

    /**
     * Changes proxy setting value.
     * 
     * @param setting flag to be changed
     * @param enabled desired flag state
     */
    fun setProxySetting(setting: Int, enabled: Boolean) {
        setProxySettingImpl(this.proxySettings, setting, enabled)
    }

    /**
     * Disables proxy, if any configuration is in use.
     */
    fun disableProxy() {
        val settings = this.proxySettings
        if ((settings and PROXY_FLAG_ENABLED) != 0) {
            setProxySettingImpl(settings, PROXY_FLAG_ENABLED, false)
        }
    }

    private fun setProxySettingImpl(oldSettings: Int, setting: Int, enabled: Boolean): Boolean {
        val newSettings = setFlag(oldSettings, setting, enabled)
        if (newSettings == oldSettings) {
            return enabled
        }
        if (setting == PROXY_FLAG_ENABLED) {
            val proxyId: Int
            val proxy: Proxy?
            if (enabled) {
                proxyId = this.availableProxyId
                if (proxyId <= PROXY_ID_NONE) {
                    return false
                }
                proxy = getProxyConfig(proxyId)
                if (proxy == null) {
                    return false
                }
            } else {
                proxyId = PROXY_ID_NONE
                proxy = null
            }
            pmc.putByte(KEY_PROXY_SETTINGS, newSettings.toByte())
            if (proxy != null) {
                dispatchProxyConfiguration(proxyId, proxy.proxy, proxy.description, true, false)
            } else {
                dispatchProxyConfiguration(PROXY_ID_NONE, null, null, true, false)
            }
        } else {
            pmc.putByte(KEY_PROXY_SETTINGS, newSettings.toByte())
        }
        return enabled
    }

    /**
     * @param proxyId Proxy identifier
     * @return Proxy configuration, such as server,port,username,password,etc
     */
    fun getProxyConfig(proxyId: Int): Proxy? {
        if (proxyId != PROXY_ID_NONE) {
            val proxy: Proxy? = readProxy(proxyId, pmc.getByteArray(KEY_PROXY_PREFIX_CONFIG + proxyId), null)
            if (proxy == null) {
                Log.e("Configuration unavailable, proxyId:%d", proxyId)
            }
            return proxy
        }
        return null
    }

    /**
     * @param proxyId Proxy identifier
     * @return Proxy name
     */
    fun getProxyName(proxyId: Int): String? {
        val proxy = getProxyConfig(proxyId)
        return if (proxy != null) proxy.name.toString() else null
    }

    /**
     * Get existing proxy identifier
     * 
     * @param proxy Proxy information
     * @return Proxy identifier, or [.PROXY_ID_NONE] if not found
     */
    fun getExistingProxyId(proxy: TdApi.Proxy): Int {
        val data: ByteArray = serializeProxy(proxy)
        if (data != null) {
            val existingKey = pmc.findByValue(KEY_PROXY_PREFIX_CONFIG, data)
            if (existingKey != null) {
                return parseInt(existingKey.substring(KEY_PROXY_PREFIX_CONFIG.length))
            }
        }
        return PROXY_ID_NONE
    }

    fun trackSuccessfulConnection(proxyId: Int, timestampMs: Long, resultMs: Long, isPing: Boolean) {
        require(proxyId > PROXY_ID_UNKNOWN) { proxyId.toString() }
        if (isPing) {
            pmc.putLongArray(KEY_PROXY_PREFIX_LAST_PING + proxyId, longArrayOf(timestampMs, resultMs))
        } else {
            val connectedCount =
                (pmc.getInt(KEY_PROXY_PREFIX_CONNECTED_COUNT + proxyId, 0)
                        + 1)
            pmc.edit()
                .putLongArray(KEY_PROXY_PREFIX_LAST_CONNECTION + proxyId, longArrayOf(timestampMs, resultMs))
                .putInt(KEY_PROXY_PREFIX_CONNECTED_COUNT + proxyId, connectedCount)
                .apply()
        }
    }

    /**
     * Adds proxy configuration or returns identifier of existing one.
     * 
     * @param proxy            Proxy server
     * @param proxyDescription Nullable alias for the proxy.
     * @param setAsCurrent     If set to false, proxy will be saved for later use.
     * @param existingProxyId  Existing proxy identifier to be modified or [.PROXY_ID_NONE]
     * @return proxy identifier
     */
    @JvmOverloads
    fun addOrUpdateProxy(proxy: TdApi.Proxy, proxyDescription: String?, setAsCurrent: Boolean, existingProxyId: Int = PROXY_ID_NONE): Int {
        var proxyDescription = proxyDescription
        val data: ByteArray = serializeProxy(proxy)
        val proxyId: Int
        if (proxyDescription != null) {
            proxyDescription = proxyDescription.trim { it <= ' ' }
        }

        val availableProxyId = this.availableProxyId.toLong()
        var proxySettings = this.proxySettings
        var abort = false

        val editor = pmc.edit()
        var isNewAdd = false

        if (existingProxyId != PROXY_ID_NONE) {
            proxyId = existingProxyId
            editor.putByteArray(KEY_PROXY_PREFIX_CONFIG + proxyId, data)
        } else {
            val existingKey = pmc.findByValue(KEY_PROXY_PREFIX_CONFIG, data)
            if (existingKey != null) {
                proxyId = parseInt(existingKey.substring(KEY_PROXY_PREFIX_CONFIG.length))
                abort = availableProxyId == proxyId.toLong() && (proxySettings and PROXY_FLAG_ENABLED) != 0
            } else {
                proxyId = getInt(KEY_PROXY_LAST_ID, PROXY_ID_NONE) + 1

                editor.putInt(KEY_PROXY_LAST_ID, proxyId) // incrementing
                editor.putByteArray(KEY_PROXY_PREFIX_CONFIG + proxyId, data)
                editor.removeByPrefix(KEY_PROXY_PREFIX_CONNECTION_TIME + proxyId)
                isNewAdd = true
            }
        }

        if (!isEmpty(proxyDescription)) {
            editor.putString(KEY_PROXY_PREFIX_DESCRIPTION + proxyId, proxyDescription)
        } else {
            editor.remove(KEY_PROXY_PREFIX_DESCRIPTION + proxyId)
        }

        if (abort) {
            editor.apply()
            return proxyId
        }

        if (setAsCurrent) {
            if ((proxySettings and PROXY_FLAG_ENABLED) == 0) {
                proxySettings = proxySettings or PROXY_FLAG_ENABLED
                editor.putByte(KEY_PROXY_SETTINGS, proxySettings.toByte())
            }
            if (proxyId.toLong() != availableProxyId) {
                editor.putInt(KEY_PROXY_CURRENT, proxyId)
            }
        } else if (availableProxyId == PROXY_ID_NONE.toLong()) {
            if ((proxySettings and PROXY_FLAG_ENABLED) != 0) {
                proxySettings = proxySettings and PROXY_FLAG_ENABLED.inv()
                editor.putByte(KEY_PROXY_SETTINGS, proxySettings.toByte())
            }
            editor.putInt(KEY_PROXY_CURRENT, proxyId)
        }

        editor.apply()

        if (isNewAdd) {
            dispatchProxyAdded(Proxy(proxyId, proxy, proxyDescription), setAsCurrent)
        }
        dispatchProxyConfiguration(
            proxyId,
            proxy,
            proxyDescription,
            setAsCurrent || (availableProxyId == proxyId.toLong() && (proxySettings and PROXY_FLAG_ENABLED) != 0),
            isNewAdd
        )
        if (availableProxyId == PROXY_ID_NONE.toLong()) {
            dispatchProxyAvailabilityChanged(true)
        }

        return proxyId
    }

    /**
     * Removes proxy configuration. Does nothing, if proxy is currently in use.
     * 
     * @param proxyId proxy identifier
     * @return true if proxy has been successfully deleted
     */
    fun removeProxy(proxyId: Int): Boolean {
        require(proxyId > PROXY_ID_NONE) { proxyId.toString() }

        val availableProxyId = this.availableProxyId
        val proxySettings = this.proxySettings

        if (availableProxyId == proxyId && (proxySettings and PROXY_FLAG_ENABLED) != 0) {
            return false
        }

        pmc.edit()
        pmc.remove(KEY_PROXY_PREFIX_CONFIG + proxyId)
        pmc.removeByPrefix(KEY_PROXY_PREFIX_CONNECTION_TIME + proxyId)
        pmc.apply()

        if (availableProxyId == proxyId) {
            var newProxyId: Int = PROXY_ID_NONE
            val firstConfigKey = pmc.findFirst(KEY_PROXY_PREFIX_CONFIG)
            if (firstConfigKey != null) {
                val i = firstConfigKey.lastIndexOf('_')
                if (i != -1) {
                    newProxyId = parseInt(firstConfigKey.substring(i + 1))
                }
            }
            pmc.putInt(KEY_PROXY_CURRENT, newProxyId)
            if (newProxyId == PROXY_ID_NONE) {
                dispatchProxyAvailabilityChanged(false)
            }
        }

        return true
    }

    /**
     * Trace proxy connection time.
     * Call this method periodically when TDLib connection is established
     * 
     * 
     * See [.PROXY_UPDATE_PERIOD_SECONDS]
     * 
     * @param proxyId Proxy identifier. [.PROXY_ID_NONE] means connection without proxy.
     * @param time    Last successful connection time. Seconds
     */
    @Deprecated("")
    fun traceProxyConnected(proxyId: Int, accountId: Int, time: Int) {
        if (proxyId >= PROXY_ID_NONE) {
            pmc.putInt(KEY_PROXY_PREFIX_CONNECTION_TIME + proxyId + "_" + accountId, time)
        }
    }

    /**
     * @param proxyId Proxy identifier. [.PROXY_ID_NONE] means connection without proxy.
     * @return Last connection establishment time. 0 means never been connected yet.
     */
    @Deprecated("")
    fun getProxyConnectionTime(proxyId: Int, accountId: Int): Int {
        require(proxyId >= PROXY_ID_NONE) { proxyId.toString() }
        return pmc.getInt(KEY_PROXY_PREFIX_CONNECTION_TIME + proxyId + "_" + accountId, 0)
    }

    class Proxy(id: Int, proxy: TdApi.Proxy?, description: String?) : Comparable<Proxy?> {
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(value = [TYPE_SOCKS5, TYPE_MTPROTO, TYPE_HTTP])
        annotation class Type

        @JvmField
        val id: Int

        @JvmField
        var proxy: TdApi.Proxy?

        @JvmField
        var order: Int = ORDER_UNSET
        @JvmField
        var description: String?
        @JvmField
        var successfulConnectionsCount: Int = 0
        @JvmField
        var lastConnectionTime: Long = 0
        var lastConnectionDuration: Long = 0
        var lastPingTime: Long = 0
        var lastPingResult: Long = 0

        @JvmField
        var pingCount: Int = 0
        @JvmField
        var pingMs: Long = PROXY_TIME_UNSET.toLong()
        @JvmField
        var pingError: TdApi.Error? = null
        @JvmField
        var pingErrorCount: Int = 0
        @JvmField
        var winState: Int = 0

        init {
            require(!(id != PROXY_ID_NONE && proxy == null))
            this.id = id
            this.proxy = proxy
            this.description = description
        }

        fun hasPong(): Boolean {
            return pingMs >= 0
        }

        fun canUseForCalls(): Boolean {
            return proxy != null && canUseForCalls(proxy!!.type)
        }

        val name: CharSequence?
            get() {
                if (proxy == null) {
                    return null
                }
                val name = if (isEmpty(description)) proxy!!.server + ":" + proxy!!.port else description
                val stringRes: Int
                when (proxy!!.type.getConstructor()) {
                    ProxyTypeSocks5.CONSTRUCTOR -> {
                        val socks5 = proxy!!.type as ProxyTypeSocks5
                        if (proxy!!.port == 9050 && isEmpty(socks5.username) && isEmpty(socks5.password) && U.isLocalhost(
                                proxy!!.server.lowercase(
                                    Locale.getDefault()
                                )
                            )
                        ) {
                            stringRes = R.string.ProxyTorNetwork
                        } else {
                            stringRes = R.string.ProxySocks5
                        }
                    }

                    ProxyTypeMtproto.CONSTRUCTOR -> stringRes = R.string.ProxyMtproto
                    ProxyTypeHttp.CONSTRUCTOR -> stringRes = R.string.ProxyHttp
                    else -> {
                        assertProxyType_bc1a1076()
                        throw unsupported(proxy!!.type)
                    }
                }
                return Lang.getString(
                    stringRes,
                    SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, argIndex: Int, needFakeBold: Boolean ->
                        CustomTypefaceSpan(
                            null,
                            ColorId.textLight
                        )
                    },
                    name
                )
            }

        override fun compareTo(o: Proxy?): Int {
            if (order != o!!.order) return Integer.compare(order, o.order)
            else return Integer.compare(o.id, id)
        }

        fun defaultOrder(): Int {
            return if (proxy != null) getProxyDefaultOrder(proxy!!.type) else -1
        }

        val isDirect: Boolean
            get() = proxy == null

        override fun toString(): String {
            val name = this.name
            return if (name != null) name.toString() else super.toString()
        }

        companion object {
            internal const val INTERNAL_FLAG_HAS_USERNAME = 1
            internal val INTERNAL_FLAG_HAS_PASSWORD = 1 shl 1

            internal const val TYPE_SOCKS5 = 1
            internal const val TYPE_MTPROTO = 2
            internal const val TYPE_HTTP = 3

            internal val ORDER_UNSET = -1

            @JvmStatic
            fun canUseForCalls(type: TdApi.ProxyType?): Boolean {
                if (type != null) {
                    when (type.getConstructor()) {
                        ProxyTypeSocks5.CONSTRUCTOR -> return true
                    }
                }
                return false
            }

            @JvmStatic
            fun noProxy(loadStats: Boolean): Proxy {
                val proxy = Proxy(PROXY_ID_NONE, null, null)
                if (loadStats) {
                    proxy.successfulConnectionsCount = instance()!!.getInt(
                        KEY_PROXY_PREFIX_CONNECTED_COUNT + PROXY_ID_NONE, 0
                    )
                    val lastPingResult: LongArray? = instance()!!.getLongArray(
                        KEY_PROXY_PREFIX_LAST_PING + PROXY_ID_NONE
                    )
                    if (lastPingResult != null) {
                        proxy.lastPingTime = if (lastPingResult.size > 0) lastPingResult[0] else 0
                        proxy.lastPingResult = if (lastPingResult.size > 1) lastPingResult[1] else 0
                    }
                    val lastConnectionInfo: LongArray? = instance()!!.getLongArray(
                        KEY_PROXY_PREFIX_LAST_CONNECTION + PROXY_ID_NONE
                    )
                    if (lastConnectionInfo != null) {
                        proxy.lastConnectionTime = if (lastConnectionInfo.size > 0) lastConnectionInfo[0] else 0
                        proxy.lastConnectionDuration = if (lastConnectionInfo.size > 0) lastConnectionInfo[1] else 0
                    }
                }
                return proxy
            }
        }
    }

    /**
     * Sets order of proxy list
     * 
     * @param proxyIds Array of proxy identifiers
     */
    fun setProxyOrder(proxyIds: IntArray?) {
        if (proxyIds != null) {
            pmc.putIntArray(KEY_PROXY_ORDER, proxyIds)
        } else {
            pmc.remove(KEY_PROXY_ORDER)
        }
    }

    val availableProxies: MutableList<Proxy>
        /**
         * Get list of all available proxies in the descending order (last added first).
         * 
         * 
         * This operation may take time, if there are too many proxies,
         * maybe it's good idea to invoke this method on background thread.
         * 
         * @return list of available proxy configurations
         */
        get() =// TODO cache
            loadAvailableProxies()

    private fun loadAvailableProxies(): MutableList<Proxy> {
        val proxies: MutableList<Proxy> = java.util.ArrayList<Proxy>()
        var blob: Blob? = null
        val order = pmc.getIntArray(KEY_PROXY_ORDER)
        for (entry in pmc.find(KEY_PROXY_ITEM_PREFIX)) {
            val key = entry.key()
            val i = key.lastIndexOf('_')
            if (i == -1) {
                continue
            }
            val subKey = key.substring(0, i + 1)
            val proxyId = parseInt(key.substring(i + 1))
            if (proxyId < PROXY_ID_NONE) {
                Log.w("Unknown proxy id entry:%d", proxyId)
                continue
            }
            if (proxyId == PROXY_ID_NONE) {
                continue
            }
            if (subKey == KEY_PROXY_PREFIX_CONFIG) {
                // Config itself
                val data = entry.asByteArray()
                if (blob == null) blob = Blob()
                val proxy: Proxy? = readProxy(proxyId, data, blob)
                if (proxy != null) {
                    proxy.order = if (order != null) indexOf(order, proxyId) else Proxy.Companion.ORDER_UNSET
                    proxies.add(proxy)
                } else {
                    Log.w("Removing proxy configuration, because it cannot be read, proxyId:%d", proxyId)
                    removeProxy(proxyId)
                }
            } else if (!proxies.isEmpty()) {
                val lastProxy = proxies.get(proxies.size - 1)
                if (lastProxy.id == proxyId) {
                    try {
                        when (key) {
                            KEY_PROXY_PREFIX_DESCRIPTION -> {
                                lastProxy.description = entry.asString()
                            }

                            KEY_PROXY_PREFIX_CONNECTED_COUNT -> {
                                lastProxy.successfulConnectionsCount = entry.asInt()
                            }

                            KEY_PROXY_PREFIX_LAST_CONNECTION -> {
                                val lastConnectionInfo = entry.asLongArray()
                                lastProxy.lastConnectionTime = if (lastConnectionInfo.size > 0) lastConnectionInfo[0] else 0
                                lastProxy.lastConnectionDuration = if (lastConnectionInfo.size > 1) lastConnectionInfo[1] else 0
                            }

                            KEY_PROXY_PREFIX_LAST_PING -> {
                                val lastPingInfo = entry.asLongArray()
                                lastProxy.lastPingTime = if (lastPingInfo.size > 0) lastPingInfo[0] else 0
                                lastProxy.lastPingResult = if (lastPingInfo.size > 1) lastPingInfo[1] else 0
                            }
                        }
                    } catch (ignored: IllegalArgumentException) {
                    }
                }
            }
        }
        Collections.sort<Proxy?>(proxies)
        return proxies
    }

    interface ProxyChangeListener {
        fun onProxyConfigurationChanged(proxyId: Int, proxy: TdApi.Proxy?, description: String?, isCurrent: Boolean, isNewAdd: Boolean)

        fun onProxyAvailabilityChanged(isAvailable: Boolean)

        fun onProxyAdded(proxy: Proxy?, isCurrent: Boolean)
    }

    private val proxyListeners = ReferenceList<ProxyChangeListener?>()

    fun addProxyListener(listener: ProxyChangeListener?) {
        proxyListeners.add(listener)
    }

    fun removeProxyListener(listener: ProxyChangeListener?) {
        proxyListeners.remove(listener)
    }

    /**
     * Notifies that all TDLib instances must change proxy configuration.
     * 
     * @param id        Proxy identifier
     * @param proxy     Proxy details
     * @param isCurrent True when this proxy is applied to TDLib instances
     * @param isNewAdd
     */
    private fun dispatchProxyConfiguration(id: Int, proxy: TdApi.Proxy?, description: String?, isCurrent: Boolean, isNewAdd: Boolean) {
        for (listener in proxyListeners) {
            listener!!.onProxyConfigurationChanged(id, proxy, description, isCurrent, isNewAdd)
        }
    }

    /**
     * Notifies that at least one proxy configuration became available.
     * 
     * @param isAvailable true if at least one proxy configuration is available.
     */
    private fun dispatchProxyAvailabilityChanged(isAvailable: Boolean) {
        for (listener in proxyListeners) {
            listener!!.onProxyAvailabilityChanged(isAvailable)
        }
    }

    /**
     * Notifies that new proxy configuration has been added
     * 
     * @param proxy Proxy information
     */
    private fun dispatchProxyAdded(proxy: Proxy?, isCurrent: Boolean) {
        for (listener in proxyListeners) {
            listener!!.onProxyAdded(proxy, isCurrent)
        }
    }

    // Earpiece mode
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [EARPIECE_MODE_NEVER, EARPIECE_MODE_PROXIMITY, EARPIECE_MODE_ALWAYS])
    annotation class EarpieceMode

    interface RaiseToSpeakListener {
        fun onEarpieceModeChanged(isVideo: Boolean, @EarpieceMode newMode: Int)
    }

    private val raiseToSpeakListeners = ReferenceList<RaiseToSpeakListener?>()

    fun addRaiseToSpeakListener(listener: RaiseToSpeakListener?) {
        raiseToSpeakListeners.add(listener)
    }

    fun removeRaiseToSpeakListener(listener: RaiseToSpeakListener?) {
        raiseToSpeakListeners.remove(listener)
    }

    @EarpieceMode
    fun getEarpieceMode(isVideo: Boolean): Int {
        @EarpieceMode val earpieceMode: Int
        if (isVideo) {
            earpieceMode = getInt(KEY_EARPIECE_VIDEO_MODE, EARPIECE_MODE_NEVER)
        } else {
            earpieceMode = getInt(KEY_EARPIECE_MODE, EARPIECE_MODE_PROXIMITY)
        }
        when (earpieceMode) {
            EARPIECE_MODE_ALWAYS -> return if (isVideo) EARPIECE_MODE_NEVER else earpieceMode
            EARPIECE_MODE_NEVER, EARPIECE_MODE_PROXIMITY -> return earpieceMode
        }
        return EARPIECE_MODE_NEVER
    }

    fun setEarpieceMode(isVideo: Boolean, @EarpieceMode earpieceMode: Int) {
        val mode = getEarpieceMode(isVideo)
        if (mode != earpieceMode) {
            putInt(if (isVideo) KEY_EARPIECE_VIDEO_MODE else KEY_EARPIECE_MODE, earpieceMode)
            for (listener in raiseToSpeakListeners) {
                listener!!.onEarpieceModeChanged(isVideo, earpieceMode)
            }
        }
    }

    private fun checkPendingPasscodeLocks() {
        for (entry in pmc.find(KEY_BRUT_FORCE_BLOCK_SECONDS)) {
            startPasscodeBlockTimer( /*entry.asInt(),*/entry.key().substring(KEY_BRUT_FORCE_BLOCK_SECONDS.length))
        }
    }

    fun forgetPasscodeErrors(mode: Int, suffix: String?) {
        val key = if (suffix != null) KEY_BRUT_FORCE_ERROR_PREFIX + suffix else "global_" + KEY_BRUT_FORCE_ERROR_PREFIX
        pmc.removeByPrefix(key)
    }

    @AnyThread
    fun isPasscodeBlocked(mode: Int, suffix: String?): Boolean {
        return pmc.contains(if (suffix != null) KEY_BRUT_FORCE_BLOCK_SECONDS + suffix else KEY_BRUT_FORCE_BLOCK_SECONDS)
    }

    fun getPasscodeBlockSeconds(suffix: String?): Int {
        val key: String = if (suffix != null) KEY_BRUT_FORCE_BLOCK_SECONDS + suffix else KEY_BRUT_FORCE_BLOCK_SECONDS
        return pmc.getInt(key, 0)
    }

    private fun blockPasscode(mode: Int, level: Int, suffix: String?) {
        val seconds: Int
        if (level <= 1) {
            seconds = 30
        } else {
            seconds = min(TimeUnit.MINUTES.toSeconds(5).toInt(), 30 + 15 * (level - 1))
        }
        val key: String = if (suffix != null) KEY_BRUT_FORCE_BLOCK_SECONDS + suffix else KEY_BRUT_FORCE_BLOCK_SECONDS
        pmc.putInt(key, seconds)
        startPasscodeBlockTimer(if (suffix != null) suffix else "")
    }

    private fun tickPasscode(suffix: String?): Boolean {
        val key: String = if (suffix != null) KEY_BRUT_FORCE_BLOCK_SECONDS + suffix else KEY_BRUT_FORCE_BLOCK_SECONDS
        var seconds = pmc.getInt(key, 0)
        if (--seconds > 0) {
            pmc.putInt(key, seconds)
            return true
        } else {
            pmc.remove(key)
            return false
        }
    }

    interface PasscodeTickListener {
        fun onPasscodeTick(suffix: String?)
    }

    private var passcodeTickListeners: ReferenceList<PasscodeTickListener?>? = null

    fun addPasscodeTickLister(listener: PasscodeTickListener?) {
        if (passcodeTickListeners == null) {
            passcodeTickListeners = ReferenceList<PasscodeTickListener?>()
        }
        passcodeTickListeners!!.add(listener)
    }

    fun removePasscodeTickListener(listener: PasscodeTickListener?) {
        if (passcodeTickListeners != null) {
            passcodeTickListeners!!.remove(listener)
        }
    }

    @UiThread
    private fun onPasscodeTick(suffix: String?) {
        if (passcodeTickListeners != null) {
            for (listener in passcodeTickListeners) {
                listener!!.onPasscodeTick(suffix)
            }
        }
    }

    private var passcodeTimers: HashMap<String?, CancellableRunnable?>? = null

    private fun startPasscodeBlockTimer(suffix: String) {
        if (passcodeTimers == null) {
            passcodeTimers = HashMap<String?, CancellableRunnable?>()
        } else if (passcodeTimers!!.containsKey(suffix)) {
            return
        }
        val actor: CancellableRunnable = object : CancellableRunnable() {
            public override fun act() {
                if (tickPasscode(suffix)) {
                    Background.instance().post(this, 1000)
                } else {
                    passcodeTimers!!.remove(suffix)
                }
                UI.post(Runnable { onPasscodeTick(suffix) })
            }
        }
        actor.removeOnCancel(UI.getAppHandler())
        passcodeTimers!!.put(suffix, actor)
        Background.instance().post(actor, 1000)
        UI.post(Runnable { onPasscodeTick(suffix) })
    }

    // BackgroundThread
    fun tracePasscodeError(mode: Int, error: String?, suffix: String?) {
        val maximumErrorCount: Int
        when (mode) {
            Passcode.MODE_PASSWORD -> maximumErrorCount = 7
            Passcode.MODE_PINCODE -> maximumErrorCount = 4
            Passcode.MODE_PATTERN, Passcode.MODE_GESTURE -> maximumErrorCount = 5
            else -> maximumErrorCount = 5
        }
        val key = if (suffix != null) KEY_BRUT_FORCE_ERROR_PREFIX + suffix else "global_" + KEY_BRUT_FORCE_ERROR_PREFIX
        var num = 0
        val errorHash = if (error != null) Passcode.getPasscodeHash(error) else null
        for (entry in pmc.find(key)) {
            if (errorHash != null && errorHash.equalsOrBothEmpty(entry.asString())) {
                entry.release()
                return
            }
            num++
        }
        pmc.putString(key + "_" + num, if (error != null) Passcode.getPasscodeHash(error) else "")
        num++
        if ((num % maximumErrorCount) == 0) {
            blockPasscode(mode, num / maximumErrorCount, suffix)
        }
    }

    // Language pack
    class Language internal constructor(@JvmField val packInfo: LanguagePackInfo) {
        val pluralCode: Int
        @JvmField
        val locale: Locale

        init {
            this.pluralCode = Lang.makeLanguageCode(packInfo.pluralCode)
            this.locale = Locale(Lang.normalizeLanguageCode(if (!isEmpty(packInfo.baseLanguagePackId)) packInfo.pluralCode else packInfo.id))
        }
    }

    private var language: Language? = null

    private fun setLanguageImpl(languagePack: LanguagePackInfo) {
        this.language = Settings.Language(languagePack)
    }

    val languagePluralCode: Int
        get() = getLanguage()!!.pluralCode

    val languagePackInfo: LanguagePackInfo
        get() = getLanguage()!!.packInfo

    fun getLanguage(): Language? {
        if (language == null) setLanguageImpl(readLanguage(pmc, KEY_LANGUAGE_CURRENT, true)!!)
        return language
    }

    fun setLanguage(languagePackInfo: LanguagePackInfo) {
        saveLanguage(pmc, KEY_LANGUAGE_CURRENT, languagePackInfo)
        setLanguageImpl(languagePackInfo)
    }

    val lastRecommendedLanguagePackId: String?
        get() = getString(KEY_SUGGESTED_LANGUAGE_CODE, null)

    fun setRecommendedLanguagePackId(languagePackId: String) {
        if (isEmpty(languagePackId)) pmc.remove(KEY_SUGGESTED_LANGUAGE_CODE)
        else pmc.putString(KEY_SUGGESTED_LANGUAGE_CODE, languagePackId)
    }

    fun suggestedLanguagePackInfo(suggestedLanguagePackId: String, tdlib: Tdlib): LanguagePackInfo? {
        if (!isEmpty(suggestedLanguagePackId) && (suggestedLanguagePackId != this.languagePackInfo.id) && (suggestedLanguagePackId != this.lastRecommendedLanguagePackId)) {
            return tdlib.suggestedLanguagePackInfo()
        }
        return null
    }

    fun needRtl(languageCode: String?, defaultValue: Boolean): Boolean {
        return getBoolean(KEY_PREFIX_RTL + Lang.cleanLanguageCode(languageCode), defaultValue)
    }

    fun setNeedRtl(languageCode: String?, value: Boolean) {
        putBoolean(KEY_PREFIX_RTL + Lang.cleanLanguageCode(languageCode), value)
        Lang.checkLanguageSettings()
    }

    private fun newThemeId(): Int {
        return getInt(KEY_THEMES_CREATED_COUNT, 0) + 1
    }

    fun addNewTheme(name: String, @ThemeId parentThemeId: Int, inheritFromCustomThemeId: Int, theme: ImportedTheme?): Int {
        val newThemeId = newThemeId()
        var installationId: String? = null

        if (theme != null) {
            val b = StringBuilder("theme_installation_")
            theme.theme = ThemeCustom(ThemeManager.serializeCustomThemeId(newThemeId))
            if (!isEmpty(theme.wallpaper)) {
                theme.theme!!.setWallpaper(theme.wallpaper)
                b.append('w').append(theme.wallpaper)
            }
            if (!theme.colorsList.isEmpty()) b.append('c')
            for (value in theme.colorsList) {
                theme.theme!!.setColor(value.id, value.intValue)
                b.append(',')
                b.append(value.name).append(":").append(value.intValue)
            }
            if (!theme.propertiesList.isEmpty()) b.append('p')
            for (value in theme.propertiesList) {
                theme.theme!!.setProperty(value.id, value.floatValue)
                b.append(',')
                b.append(value.name).append(":").append(U.formatFloat(value.floatValue, true))
            }
            installationId = b.toString()
            val existingCustomThemeId = pmc.getInt(installationId, 0)
            if (hasCustomTheme(existingCustomThemeId)) {
                theme.theme!!.setId(ThemeManager.serializeCustomThemeId(existingCustomThemeId))

                // pmc.putString(KEY_THEME_NAME + existingCustomThemeId, name); // Replace old theme name?
                return existingCustomThemeId
            } else if (existingCustomThemeId > 0) {
                pmc.remove(installationId)
            }
        }

        pmc.edit()
        putInt(KEY_THEMES_CREATED_COUNT, newThemeId)
        putString(KEY_THEME_NAME + newThemeId, name)
        var hasParentTheme = false
        if (inheritFromCustomThemeId > 0) {
            val prefix: String = KEY_THEME_FULL + inheritFromCustomThemeId + "_"
            for (entry in pmc.find(prefix)) {
                try {
                    val key = entry.key()
                    val newKey: String = KEY_THEME_FULL + newThemeId + key.substring(prefix.length - 1)
                    val type = key.get(prefix.length)
                    when (type) {
                        'p' -> pmc.putFloat(newKey, entry.asFloat())
                        'c' -> pmc.putInt(newKey, entry.asInt())
                        else -> Log.e("Unknown theme key: %s", key)
                    }
                } catch (t: Throwable) {
                    Log.e("Error while copying", t)
                }
            }
            var flags = getCustomThemeFlags(inheritFromCustomThemeId)
            if ((flags and THEME_FLAG_INSTALLED) != 0) {
                flags = flags or THEME_FLAG_COPY
                pmc.putByte(KEY_THEME_FLAGS + newThemeId, flags.toByte())
                val author = pmc.getString(KEY_THEME_AUTHOR + inheritFromCustomThemeId, null)
                if (!isEmpty(author)) {
                    pmc.putString(KEY_THEME_AUTHOR + newThemeId, author)
                }
                val wallpaper = pmc.getString(KEY_THEME_WALLPAPER + inheritFromCustomThemeId, null)
                if (!isEmpty(wallpaper)) {
                    pmc.putString(KEY_THEME_WALLPAPER + newThemeId, wallpaper)
                }
            }
        } else if (theme != null) {
            for (value in theme.colorsList) {
                putInt(themeColorKey(newThemeId, value.name), value.intValue)
            }
            for (value in theme.propertiesList) {
                putFloat(themePropertyKey(newThemeId, value.name), value.floatValue)
                if (value.id == PropertyId.PARENT_THEME) hasParentTheme = true
            }
            if (!isEmpty(theme.author)) {
                pmc.putString(KEY_THEME_AUTHOR + newThemeId, theme.author)
            }
            if (!isEmpty(theme.wallpaper)) {
                pmc.putString(KEY_THEME_WALLPAPER + newThemeId, theme.wallpaper)
            }
            pmc.putByte(KEY_THEME_FLAGS + newThemeId, THEME_FLAG_INSTALLED.toByte())
            if (!isEmpty(installationId)) {
                pmc.putInt(installationId!!, newThemeId)
            }
        }
        if (!hasParentTheme) {
            putFloat(themePropertyKey(newThemeId, PropertyId.PARENT_THEME), parentThemeId.toFloat())
        }
        pmc.apply()
        return newThemeId
    }

    fun installTheme(theme: ImportedTheme): Int {
        try {
            return addNewTheme(theme.name!!, theme.parentThemeId, 0, theme)
        } catch (t: Throwable) {
            Log.e("Cannot install theme", t)
            return 0
        }
    }

    fun removeCustomTheme(customThemeId: Int) {
        var installationIds: MutableList<String>? = null
        if ((getCustomThemeFlags(customThemeId) and THEME_FLAG_INSTALLED) != 0) {
            for (entry in pmc.find("theme_installation_")) {
                if (entry.asInt() == customThemeId) {
                    if (installationIds == null) installationIds = java.util.ArrayList<String>()
                    installationIds.add(entry.key())
                }
            }
        }
        pmc.edit()
        pmc.remove(KEY_THEME_NAME + customThemeId)
        pmc.remove(KEY_THEME_AUTHOR + customThemeId)
        pmc.remove(KEY_THEME_WALLPAPER + customThemeId)
        pmc.remove(KEY_THEME_FLAGS + customThemeId)
        pmc.removeByAnyPrefix(
            KEY_THEME_FULL + customThemeId + "_",
            KEY_THEME_HISTORY + customThemeId + "_"
        )
        if (installationIds != null) {
            for (installationId in installationIds) {
                pmc.remove(installationId)
            }
        }
        pmc.apply()
    }

    fun getThemeProperty(customThemeId: Int, @PropertyId propertyId: Int, defValue: Float): Float {
        return pmc.getFloat(themePropertyKey(customThemeId, propertyId), defValue)
    }

    private fun processThemeEntry(entry: LevelDB.Entry, theme: ThemeInfo?): ThemeInfo? {
        var theme = theme
        val key = entry.key()
        val customThemeId = key.substring(KEY_THEME_NAME.length).toInt()
        if (customThemeId <= 0) {
            return theme
        }
        val themeId = ThemeManager.serializeCustomThemeId(customThemeId)
        if (theme == null || theme.getId() != themeId) {
            val parentThemeId = getThemeProperty(customThemeId, PropertyId.PARENT_THEME, ThemeId.BLUE.toFloat()).toInt()
            theme = ThemeInfo(themeId, entry.asString(), getCustomThemeWallpaper(customThemeId), parentThemeId, getCustomThemeFlags(customThemeId))
        }
        return theme
    }

    fun hasCustomTheme(customThemeId: Int): Boolean {
        return customThemeId > 0 && pmc.contains(themePropertyKey(customThemeId, PropertyId.PARENT_THEME))
    }

    class ThemeExportInfo(@JvmField val name: String?, @JvmField val wallpaper: String?) {
        @JvmField
        val colors: MutableMap<Int?, MutableList<String?>?> = HashMap<Int?, MutableList<String?>?>()
        @JvmField
        val properties: MutableMap<Float?, MutableList<String?>?> = HashMap<Float?, MutableList<String?>?>()
        var parentThemeId: Int = ThemeId.NONE

        fun addColor(name: String?, valueRaw: Int) {
            // String value = Strings.getHexColor(valueRaw, true);
            val value = valueRaw
            var list = colors.get(value)
            if (list == null) {
                list = java.util.ArrayList<String?>()
                colors.put(value, list)
            }
            list.add(name)
        }

        fun addProperty(name: String?, valueRaw: Float) {
            // String value = U.formatFloat(valueRaw, true);
            val value = valueRaw
            var list = properties.get(value)
            if (list == null) {
                list = java.util.ArrayList<String?>()
                properties.put(value, list)
            }
            list.add(name)
        }
    }

    fun loadCustomTheme(customThemeId: Int): ThemeCustom? {
        val prefix: String = KEY_THEME_FULL + customThemeId + "_"
        val themeId = ThemeManager.serializeCustomThemeId(customThemeId)
        val theme = ThemeCustom(themeId)
        val startIndex = prefix.length
        var entryCount = 0
        val colorsMap: Map<String, Int> = ThemeColors.getMap()
        val propsMap: Map<String, Int> = ThemeProperties.getMap()
        for (entry in pmc.find(prefix)) {
            try {
                processThemeEntry(entry, startIndex, theme, colorsMap, propsMap)
                entryCount++
            } catch (t: Throwable) {
                Log.e("Cannot parse theme entry, key: %s", t, entry.key())
            }
        }
        if (entryCount > 0) {
            theme.setWallpaper(getCustomThemeWallpaper(customThemeId))
            return theme
        }
        return null
    }

    fun exportTheme(themeId: Int, needDefault: Boolean): ThemeExportInfo {
        val _customThemeId = ThemeManager.resolveCustomThemeId(themeId)
        val theme: ThemeExportInfo

        val colors: Map<String, Int>?
        val properties: Map<String, Int>?
        if (needDefault || _customThemeId == ThemeId.NONE) {
            colors = ThemeColors.getMap()
            properties = ThemeProperties.getMap()
        } else {
            properties = null
            colors = properties
        }

        if (_customThemeId != ThemeId.NONE) {
            val prefix: String = KEY_THEME_FULL + _customThemeId + "_"
            theme = ThemeExportInfo(getCustomThemeName(_customThemeId), getCustomThemeWallpaper(_customThemeId))
            val startIndex = prefix.length
            for (entry in pmc.find(prefix)) {
                try {
                    processThemeEntry(entry, startIndex, theme, colors, properties)
                } catch (t: Throwable) {
                    Log.e("Cannot parse theme entry, key: %s", t, entry.key())
                }
            }
        } else {
            theme = ThemeExportInfo(Lang.getString(ThemeManager.getBuiltinThemeName(themeId)), null)
            val currentTheme = ThemeSet.getBuiltinTheme(themeId)
            theme.parentThemeId = currentTheme.getProperty(PropertyId.PARENT_THEME).toInt()
            if (theme.parentThemeId != ThemeId.NONE) {
                val parentTheme = ThemeSet.getBuiltinTheme(theme.parentThemeId)
                for (entry in colors!!.entries) {
                    val colorId = entry.value
                    if (parentTheme.getColor(colorId) != currentTheme.getColor(colorId)) {
                        theme.addColor(entry.key, currentTheme.getColor(colorId))
                    }
                }

                for (entry in properties!!.entries) {
                    val propertyId = entry.value
                    if (entry.value == PropertyId.WALLPAPER_ID && currentTheme.getProperty(propertyId) == TGBackground.getDefaultWallpaperId(themeId)
                            .toFloat()
                    ) continue
                    if (parentTheme.getProperty(propertyId) != currentTheme.getProperty(propertyId)) {
                        theme.addProperty(entry.key, currentTheme.getProperty(propertyId))
                    }
                }
            }
        }
        val parentThemeId =
            if (theme.parentThemeId != ThemeId.NONE) theme.parentThemeId else if (!ThemeManager.isCustomTheme(themeId)) themeId else ThemeId.NONE
        if (needDefault && parentThemeId != ThemeId.NONE) {
            for (entry in colors!!.entries) {
                theme.addColor(entry.key, Theme.getColor(entry.value, parentThemeId))
            }
            for (entry in properties!!.entries) {
                val value = Theme.getProperty(entry.value, parentThemeId)
                if (!ThemeManager.isCustomTheme(themeId) && entry.value == PropertyId.WALLPAPER_ID && value == TGBackground.getDefaultWallpaperId(themeId)
                        .toFloat()
                ) {
                    continue
                }
                theme.addProperty(entry.key, value)
            }
        }
        return theme
    }

    val customThemes: MutableList<ThemeInfo?>
        get() {
            val themes: MutableList<ThemeInfo?> = java.util.ArrayList<ThemeInfo?>()
            var theme: ThemeInfo? = null
            for (entry in pmc.find(KEY_THEME_NAME)) {
                try {
                    val currentTheme = processThemeEntry(entry, theme)
                    if (theme !== currentTheme) {
                        themes.add(currentTheme.also { theme = it })
                    }
                } catch (t: Throwable) {
                    Log.e("Cannot parse theme entry, key: %s", t, entry.key())
                }
            }
            return themes
        }

    fun setCustomThemeColor(customThemeId: Int, @ColorId colorId: Int, newColor: Int?) {
        if (newColor == null) pmc.remove(themeColorKey(customThemeId, colorId))
        else pmc.putInt(themeColorKey(customThemeId, colorId), newColor)
    }

    fun setCustomThemeProperty(customThemeId: Int, @PropertyId propertyId: Int, newValue: Float?) {
        if (newValue == null) pmc.remove(themePropertyKey(customThemeId, propertyId))
        else pmc.putFloat(themePropertyKey(customThemeId, propertyId), newValue)
    }

    fun getCustomThemeColor(customThemeId: Int, @ColorId colorId: Int): Int {
        try {
            return pmc.tryGetInt(themeColorKey(customThemeId, colorId))
        } catch (e: FileNotFoundException) {
            return ThemeSet.getColor(getCustomThemeProperty(customThemeId, PropertyId.PARENT_THEME).toInt(), colorId)
        }
    }

    fun getCustomThemeProperty(customThemeId: Int, @PropertyId propertyId: Int): Float {
        try {
            return pmc.tryGetFloat(themePropertyKey(customThemeId, propertyId))
        } catch (e: FileNotFoundException) {
            if (propertyId == PropertyId.PARENT_THEME) return ThemeId.BLUE.toFloat()
            return ThemeSet.getProperty(getCustomThemeProperty(customThemeId, PropertyId.PARENT_THEME).toInt(), propertyId)
        }
    }

    fun setCustomThemeName(customThemeId: Int, name: String) {
        pmc.putString(KEY_THEME_NAME + customThemeId, name)
    }

    fun setCustomThemeWallpaper(customThemeId: Int, name: String) {
        if (isEmpty(name)) {
            pmc.remove(KEY_THEME_WALLPAPER + customThemeId)
        } else {
            pmc.putString(KEY_THEME_WALLPAPER + customThemeId, name)
        }
    }

    private var colorFormat = -1

    fun getColorFormat(): Int {
        if (colorFormat == -1) {
            colorFormat = pmc.getByte(KEY_COLOR_FORMAT, COLOR_FORMAT_HEX.toByte()).toInt()
            if (colorFormat < COLOR_FORMAT_HEX || colorFormat > COLOR_FORMAT_HSL) colorFormat = COLOR_FORMAT_HEX
        }
        return colorFormat
    }

    fun setColorFormat(colorFormat: Int): Boolean {
        if (getColorFormat() != colorFormat) {
            if (colorFormat == COLOR_FORMAT_HEX) pmc.remove(KEY_COLOR_FORMAT)
            else pmc.putByte(KEY_COLOR_FORMAT, colorFormat.toByte())
            this.colorFormat = colorFormat
            return true
        }
        return false
    }

    fun getColorHistory(customThemeId: Int, colorId: Int): IntArray? {
        return pmc.getIntArray(themeColorHistoryKey(customThemeId, colorId))
    }

    fun hasColorHistory(customThemeId: Int, colorId: Int): Boolean {
        return pmc.contains(themeColorHistoryKey(customThemeId, colorId))
    }

    fun setColorHistory(customThemeId: Int, colorId: Int, newHistory: IntArray?) {
        val key: String = themeColorHistoryKey(customThemeId, colorId)
        if (newHistory == null || newHistory.size == 0) {
            pmc.remove(key)
        } else {
            pmc.putIntArray(key, newHistory)
        }
    }

    fun getCustomThemeFlags(customThemeId: Int): Int {
        return pmc.getByte(KEY_THEME_FLAGS + customThemeId, 0.toByte()).toInt()
    }

    fun hasThemeOwnership(customThemeId: Int): Boolean {
        val flags = getCustomThemeFlags(customThemeId)
        return (flags and THEME_FLAG_INSTALLED) == 0 || (flags and THEME_FLAG_COPY) != 0
    }

    fun getThemeAuthor(customThemeId: Int): String? {
        return pmc.getString(KEY_THEME_AUTHOR + customThemeId, null)
    }

    fun getCustomThemeName(customThemeId: Int): String? {
        return pmc.getString(KEY_THEME_NAME + customThemeId, null)
    }

    fun getCustomThemeWallpaper(customThemeId: Int): String? {
        return pmc.getString(KEY_THEME_WALLPAPER + customThemeId, null)
    }

    fun canEditAuthor(customThemeId: Int): Boolean {
        return (getCustomThemeFlags(customThemeId) and THEME_FLAG_INSTALLED) == 0
    }

    var minimizedThemeLocation: Int
        get() = getInt(KEY_THEME_POSITION, Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        set(gravity) {
            val defaultValue = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            if (gravity == defaultValue) {
                remove(KEY_THEME_POSITION)
            } else {
                putInt(KEY_THEME_POSITION, gravity)
            }
        }

    fun needSplitNotificationCategories(): Boolean {
        return checkSetting(FLAG_OTHER_SPLIT_CHAT_NOTIFICATIONS)
    }

    fun setNeedSplitNotificationCategories(split: Boolean): Boolean {
        return setSetting(FLAG_OTHER_SPLIT_CHAT_NOTIFICATIONS, split)
    }

    fun needHideSecretChats(): Boolean {
        return checkSetting(FLAG_OTHER_HIDE_SECRET_CHATS)
    }

    fun setNeedHideSecretChats(need: Boolean): Boolean {
        return setSetting(FLAG_OTHER_HIDE_SECRET_CHATS, need)
    }

    fun needAdditionalSync(): Boolean {
        return checkNegativeSetting(FLAG_OTHER_DISABLE_ADDITIONAL_SYNC)
    }

    var processedPushCount: Long
        // Notification
        get() = getLong("notifications_count", 0)
        set(count) {
            putLong("notifications_count", count)
        }

    private var _lastPushId: Long? = null

    @Synchronized
    fun newPushId(): Long {
        if (_lastPushId == null) {
            _lastPushId = getLong("notifications_count", 0)
        }
        _lastPushId = _lastPushId!! + 1
        val pushId = _lastPushId!!
        putLong("notifications_count", pushId)
        return pushId
    }

    fun putNotificationReceiverId(receiverId: Long, accountId: Int) {
        putInt("receiver_" + receiverId, accountId)
    }

    fun findAccountByReceiverId(receiverId: Long): Int {
        return getInt("receiver_" + receiverId, TdlibAccount.NO_ID)
    }

    fun needNotificationAppVersionUpdate(accountId: Int): Boolean {
        val key = accountId.toString() + "_notifications_version"
        if (getInt(key, 0) != BuildConfig.VERSION_CODE) {
            putInt(key, BuildConfig.VERSION_CODE)
            return true
        }
        return false
    }

    private var utilityFeatures: Int
        get() = pmc.getInt(KEY_UTILITY_FEATURES, 0)
        private set(features) {
            if (features == 0) pmc.remove(KEY_UTILITY_FEATURES)
            else pmc.putInt(KEY_UTILITY_FEATURES, features)
        }

    private fun toggleUtilityFeature(feature: Int, enabled: Boolean) {
        val features = this.utilityFeatures
        val newFeatures = setFlag(features, feature, enabled)
        if (features != newFeatures) {
            this.utilityFeatures = newFeatures
        }
    }

    fun checkUtilityFeature(feature: Int): Boolean {
        return hasFlag(this.utilityFeatures, feature)
    }

    fun forceDisableNetwork(): Boolean {
        return checkUtilityFeature(UTILITY_FEATURE_NO_NETWORK)
    }

    fun setDisableNetwork(disable: Boolean) {
        toggleUtilityFeature(UTILITY_FEATURE_NO_NETWORK, disable)
    }

    fun needHidePhoneNumber(): Boolean {
        return checkUtilityFeature(UTILITY_FEATURE_HIDE_NUMBER)
    }

    fun setHidePhoneNumber(enabled: Boolean) {
        toggleUtilityFeature(UTILITY_FEATURE_HIDE_NUMBER, enabled)
    }

    fun needDisableQrProcessing(): Boolean {
        return checkUtilityFeature(UTILITY_FEATURE_NO_QR_PROCESS)
    }

    fun setDisableQrProcessing(enabled: Boolean) {
        toggleUtilityFeature(UTILITY_FEATURE_NO_QR_PROCESS, enabled)
    }

    fun needShowQrRegions(): Boolean {
        return checkUtilityFeature(UTILITY_FEATURE_QR_REGION_DEBUG)
    }

    fun setShowQrRegions(enabled: Boolean) {
        toggleUtilityFeature(UTILITY_FEATURE_QR_REGION_DEBUG, enabled)
    }

    fun needForceZxingQrProcessing(): Boolean {
        return checkUtilityFeature(UTILITY_FEATURE_QR_ZXING)
    }

    fun setForceZxingQrProcessing(enabled: Boolean) {
        toggleUtilityFeature(UTILITY_FEATURE_QR_ZXING, enabled)
    }

    fun setForceTcpInCalls(enabled: Boolean) {
        toggleUtilityFeature(UTILITY_FEATURE_FORCE_TCP_IN_CALLS, enabled)
    }

    fun forceTcpInCalls(): Boolean {
        return checkUtilityFeature(UTILITY_FEATURE_FORCE_TCP_IN_CALLS)
    }

    fun setForceTdlibRestart(enabled: Boolean) {
        toggleUtilityFeature(UTILITY_FEATURE_INSTANT_TDLIB_RESTART, enabled)
    }

    fun forceTdlibRestart(): Boolean {
        return checkUtilityFeature(UTILITY_FEATURE_INSTANT_TDLIB_RESTART)
    }

    val isEmulator: Boolean
        /*public boolean needEncryptedPush () {
             return checkUtilityFeature(UTILITY_FEATURE_ENCRYPTED_PUSHES);
           }
         
           public void setNeedEncryptedPush (boolean enabled) {
             toggleUtilityFeature(UTILITY_FEATURE_ENCRYPTED_PUSHES, enabled);
           }*/
        get() = pmc.getBoolean(KEY_IS_EMULATOR, false)

    class EmulatorDetectionResult(@JvmField val time: Long, @JvmField val installationId: Long, val elapsed: Long, val result: Long) {
        val isEmulatorDetected: Boolean
            get() = result != 0L

        fun mayBeFalsePositive(): Boolean {
            val testId = splitLongToSecondInt(result)
            when (BuildConfig.FLAVOR) {
                "x86", "x64" -> return false
                "arm64", "arm32" -> return testId == 2
                else -> return testId == 2
            }
        }

        fun toHumanReadableFormat(): String {
            return "0x" + result.toString(16)
        }

        fun toLongArray(): LongArray {
            return longArrayOf(
                time,
                installationId,
                elapsed,
                result
            )
        }

        companion object {
            fun restore(array: LongArray?): EmulatorDetectionResult? {
                if (array == null || array.size != 4) {
                    return null
                }
                return EmulatorDetectionResult(
                    array[0],
                    array[1],
                    array[2],
                    array[3]
                )
            }
        }
    }

    val lastEmulatorDetectionResult: EmulatorDetectionResult?
        get() {
            val emulatorDetectionResult =
                pmc.getLongArray(KEY_EMULATOR_DETECTION_RESULT)
            if (emulatorDetectionResult == null) {
                return null
            }
            return EmulatorDetectionResult.restore(emulatorDetectionResult)
        }

    fun trackEmulatorDetectionResult(installationId: Long, elapsed: Long, emulatorCheckResult: Long): EmulatorDetectionResult {
        val result = EmulatorDetectionResult(
            System.currentTimeMillis(),
            installationId,
            elapsed,
            emulatorCheckResult
        )
        val data = result.toLongArray()
        pmc.putLongArray(KEY_EMULATOR_DETECTION_RESULT, data)
        val wasEmulator = this.isEmulator
        if (wasEmulator != result.isEmulatorDetected) {
            if (result.isEmulatorDetected) {
                putBoolean(KEY_IS_EMULATOR, true)
            } else {
                pmc.remove(KEY_IS_EMULATOR)
            }
        }
        return result
    }

    private var authenticationTokens: MutableList<String?>? = null

    fun trackAuthenticationToken(token: String?) {
        val tokens = this.authenticationTokensList
        if (!tokens.contains(token)) {
            tokens.add(token)
            while (tokens.size > 20) {
                tokens.removeAt(0)
            }
            pmc.putStringArray(KEY_TDLIB_AUTHENTICATION_TOKENS, tokens.toTypedArray<String?>())
        }
    }

    val authenticationTokensList: MutableList<String?>
        get() {
            if (authenticationTokens == null) {
                authenticationTokens = java.util.ArrayList<String?>()
                val tokens =
                    pmc.getStringArray(KEY_TDLIB_AUTHENTICATION_TOKENS)
                if (tokens != null) {
                    Collections.addAll<String?>(authenticationTokens, *tokens)
                }
            }
            return authenticationTokens!!
        }

    fun getAuthenticationTokens(): Array<String?> {
        return this.authenticationTokensList.toTypedArray<String?>()
    }

    private val lastCrashId: Long
        // Tdlib crash
        get() = pmc.getLong(KEY_TDLIB_CRASH_PREFIX, 0) - 1

    fun findRecoveryCrash(): Crash? {
        val lastCrashId = this.lastCrashId
        return getCrash(lastCrashId, true)
    }

    fun storeTestCrash(builder: Crash.Builder) {
        AppState.resetUptime()
        storeCrash(builder)
    }

    fun storeCrash(crashBuilder: Crash.Builder) {
        val crashId = pmc.getLong(KEY_TDLIB_CRASH_PREFIX, 0)
        val crash = crashBuilder
            .id(crashId)
            .uptime(AppState.uptime())
            .appBuildInfo(instance()!!.getCurrentBuildInformation())
            .build()

        val keyPrefix: String = makeCrashPrefix(crashId)

        pmc.edit()
        // increment crashId
        pmc.putLong(KEY_TDLIB_CRASH_PREFIX, crashId + 1)
        // save crash
        crash.saveTo(pmc, keyPrefix)
        // apply & flush
        pmc.apply()
        pmc.flush()
    }

    fun setCrashFlag(info: Crash, flag: Int, enabled: Boolean): Boolean {
        if (info.setFlag(flag, enabled)) {
            info.saveFlags(pmc, makeCrashPrefix(info.id))
            return true
        }
        return false
    }

    fun markCrashAsResolved(info: Crash) {
        if (setCrashFlag(info, Crash.Flags.RESOLVED, true)) {
            AppState.resetUptime()
        }
    }

    fun markCrashAsSaved(info: Crash) {
        setCrashFlag(info, Crash.Flags.APPLICATION_LOG_EVENT_SAVED, true)
    }

    val crashesToSave: MutableList<Crash?>?
        get() {
            val lastCrashId = this.lastCrashId
            if (lastCrashId < 0) return null

            val currentInstallationId = getCurrentBuildInformation().installationId
            var result: MutableList<Crash?>? = null

            var crashedBuildInfo: AppBuildInfo? = null

            for (crashId in lastCrashId downTo 0) {
                val keyPrefix: String = makeCrashPrefix(crashId)
                @Crash.Flags val flags = Crash.restoreFlags(pmc, keyPrefix)
                val crashedInstallationId = Crash.restoreInstallationId(pmc, keyPrefix)
                if (crashedInstallationId != currentInstallationId) {
                    if (crashedBuildInfo == null || crashedInstallationId != crashedBuildInfo.installationId) {
                        crashedBuildInfo = getBuildInformation(crashedInstallationId)
                    }
                    // Forget about crashes from previously installed versions, except if it's the same TDLib commit
                    val crashedTdlibCommit = if (crashedBuildInfo != null) crashedBuildInfo.tdlibCommitFull else null
                    if (isEmpty(crashedTdlibCommit) || !crashedTdlibCommit.equals(getCurrentBuildInformation().tdlibCommitFull, ignoreCase = true) || !hasFlag(
                            flags,
                            Crash.Flags.SOURCE_TDLIB or Crash.Flags.SOURCE_TDLIB_PARAMETERS
                        )
                    ) {
                        break
                    }
                }
                if (!hasFlag(flags, Crash.Flags.SAVE_APPLICATION_LOG_EVENT)) {
                    continue
                }
                if (hasFlag(flags, Crash.Flags.APPLICATION_LOG_EVENT_SAVED)) {
                    break
                }
                val crash = getCrash(crashId, false)
                if (crash != null) {
                    if (result == null) {
                        result = java.util.ArrayList<Crash?>()
                    }
                    result.add(crash)
                }
            }

            if (result != null) {
                // Sort crashes from older to newer
                Collections.reverse(result)
            }

            return result
        }

    fun getCrash(crashId: Long, forApplicationStart: Boolean): Crash? {
        if (crashId < 0) return null
        val keyPrefix: String = makeCrashPrefix(crashId)
        if (forApplicationStart) {
            @Crash.Flags val flags = Crash.restoreFlags(pmc, keyPrefix)
            if (hasFlag(flags, Crash.Flags.RESOLVED)) {
                // Do not attempt to read any other fields, if user pressed "Launch App"
                return null
            }
            val crashedInstallationId = Crash.restoreInstallationId(pmc, keyPrefix)
            val currentBuildInformation = getCurrentBuildInformation()
            if (crashedInstallationId != 0L && currentBuildInformation.installationId > crashedInstallationId) {
                // User has installed a newer version. Ignore crashes from previous APKs
                return null
            }
        }
        val builder = Crash.Builder().id(crashId)
        var nonEmpty = false
        for (entry in pmc.find(keyPrefix)) {
            if (builder.restoreField(entry, keyPrefix, AppBuildInfoRestorer { installationId: Long -> this.getBuildInformation(installationId) })) {
                nonEmpty = true
            }
        }
        if (nonEmpty) {
            val crash = builder.build()
            if (crash.getType() != Crash.Type.UNKNOWN && (!forApplicationStart || crash.shouldShowAtApplicationStart())) {
                return crash
            }
        }
        return null
    }

    val periodicSyncFrequencySeconds: Long
        // Sync
        get() =// TODO
            TimeUnit.MINUTES.toSeconds(30)

    var deviceToken: DeviceToken?
        get() {
            @DeviceTokenType val tokenType =
                pmc.getInt(KEY_PUSH_DEVICE_TOKEN_TYPE, DeviceTokenType.FIREBASE_CLOUD_MESSAGING)
            val tokenOrEndpoint = pmc.getString(KEY_PUSH_DEVICE_TOKEN_OR_ENDPOINT, null)
            return newDeviceToken(tokenType, tokenOrEndpoint)
        }
        set(token) {
            if (token == null) {
                pmc.edit()
                    .remove(KEY_PUSH_DEVICE_TOKEN_TYPE)
                    .remove(KEY_PUSH_DEVICE_TOKEN_OR_ENDPOINT)
                    .apply()
            } else if (!token.equalsTo(this.deviceToken)) {
                resetTokenPushMessageCount()
                val editor: SharedPreferences.Editor = pmc.edit()
                storeDeviceToken(
                    token, editor,
                    KEY_PUSH_DEVICE_TOKEN_TYPE,
                    KEY_PUSH_DEVICE_TOKEN_OR_ENDPOINT
                )
                editor.apply()
            }
        }

    // Device ID used to anonymously identify crashes from the same client
    private var crashDeviceId: String? = null

    fun crashDeviceId(): String? {
        if (crashDeviceId == null) {
            crashDeviceId = pmc.getString(KEY_CRASH_DEVICE_ID, null)
        }
        if (isEmpty(crashDeviceId)) {
            crashDeviceId = U.sha256(
                U.getUsefulMetadata(null) + "\n" +
                        random("abcdefABCDEF0123456789", 16) + "\n" + (Long.MAX_VALUE.toDouble() * Math.random()).toLong()
            )
            pmc.putString(KEY_CRASH_DEVICE_ID, crashDeviceId)
        }
        return crashDeviceId
    }

    // Interface
    abstract class CloudSetting : Comparable<CloudSetting?> {
        @JvmField
        val identifier: String
        var version: Int = 0
        var date: Int = 0

        @JvmField
        var displayName: String? = null
        var position: Int = -1
        @JvmField
        var file: TdApi.File? = null

        @JvmField
        var previewFile: ImageFile? = null

        constructor(identifier: String, version: Int, displayName: String?, position: Int) {
            this.identifier = identifier
            this.version = version
            this.displayName = displayName
            this.position = position
        }

        constructor(identifier: String?) {
            this.identifier = U.getSecureFileName(identifier)
        }

        constructor(message: TdApi.Message, requiredHashtag: String?, builtInStringRes: Int) {
            val document = message.content as MessageDocument
            this.identifier = U.getSecureFileName(document.document.fileName)
            this.date = max(message.date, message.editDate)
            var isValidSetting = false
            var hideName = false
            if (document.caption != null && document.caption.entities != null && document.caption.entities.size > 0) {
                for (entity in document.caption.entities) {
                    when (entity.type.getConstructor()) {
                        TdApi.TextEntityTypeHashtag.CONSTRUCTOR -> {
                            val hashtag = document.caption.text.substring(entity)
                            if (hashtag == requiredHashtag) {
                                isValidSetting = true
                            } else if (hashtag == "#hide") {
                                val isSystem = BuildConfig.DEBUG || (Device.MANUFACTURER == Device.SAMSUNG && identifier == "samsung")
                                require(isSystem)
                            } else if (hashtag == "#noname") {
                                hideName = true
                            } else if (hashtag!!.startsWith("#v")) {
                                version = parseInt(hashtag.substring(2), -1)
                            } else if (hashtag.startsWith("#p")) {
                                position = parseInt(hashtag.substring(2), -1)
                            }
                        }

                        TdApi.TextEntityTypeCode.CONSTRUCTOR, TdApi.TextEntityTypePre.CONSTRUCTOR, TdApi.TextEntityTypePreCode.CONSTRUCTOR -> {
                            displayName = document.caption.text.substring(entity)
                        }
                    }
                }
            }
            require(!(!isValidSetting || version != BuildConfig.EMOJI_VERSION || displayName == null))
            this.file = document.document.document
            if (this.isBuiltIn()) {
                if (hideName) this.displayName = Lang.getString(builtInStringRes)
                this.file = null
            }
        }

        fun setPreviewFile(tdlib: TdlibProvider?, file: TdApi.File?) {
            if (file != null) {
                val desiredSize = Screen.dp(64f)
                this.previewFile = ImageFile(tdlib, file)
                this.previewFile!!.setScaleType(ImageFile.FIT_CENTER)
                this.previewFile!!.setSize(desiredSize) // FIXME improve
            } else {
                this.previewFile = null
            }
        }

        override fun equals(obj: Any?): Boolean {
            return obj is CloudSetting && obj.identifier == this.identifier
        }

        override fun hashCode(): Int {
            return this.identifier.hashCode()
        }

        override fun compareTo(o: CloudSetting?): Int {
            if (identifier.equalsOrBothEmpty(o!!.identifier)) return 0
            val h1 = position != -1
            val h2 = o.position != -1
            if (h1 != h2) return java.lang.Boolean.compare(h2, h1)
            if (h1 && position != o.position) return Integer.compare(position, o.position)
            if (!displayName.equalsOrBothEmpty(o.displayName)) return displayName!!.compareTo(o.displayName!!)
            return identifier.compareTo(o.identifier)
        }

        abstract fun install(callback: RunnableBool)

        abstract fun isBuiltIn(): Boolean

        abstract fun getInstallState(fast: Boolean): Int

        val isInstalled: Boolean
            get() = this.isBuiltIn() || getInstallState(false) == STATE_INSTALLED

        companion object {
            const val STATE_NOT_INSTALLED: Int = 0
            const val STATE_INSTALLED: Int = 1
            const val STATE_UPDATE_NEEDED: Int = 2
        }
    }

    // Emoji pack
    private var emojiPack: EmojiPack? = null
    var outdatedEmojiPack: EmojiPack? = null
        private set

    fun revokeOutdatedEmojiPack() {
        setEmojiPack(emojiPack!!)
    }

    fun getEmojiPack(): EmojiPack {
        if (emojiPack == null) {
            var pack: EmojiPack? = null
            for (entry in pmc.find(KEY_EMOJI_PACK)) {
                if (entry.key().length == KEY_EMOJI_PACK.length) {
                    pack = EmojiPack(entry.asString())
                } else {
                    if (pack == null) continue
                    when (entry.key().substring(KEY_EMOJI_PACK.length)) {
                        "_version" -> pack.version = entry.asInt()
                        "_name" -> pack.displayName = entry.asString()
                        "_date" -> pack.date = entry.asInt()
                    }
                }
            }
            if (pack != null) {
                if (pack.version != BuildConfig.EMOJI_VERSION) {
                    outdatedEmojiPack = pack
                    emojiPack = EmojiPack()
                } else {
                    emojiPack = pack
                }
            } else {
                emojiPack = EmojiPack()
            }
        }
        return emojiPack!!
    }

    val emojiPackIdentifier: String
        get() = getEmojiPack().identifier

    fun setEmojiPack(emojiPack: EmojiPack) {
        require(emojiPack.version == BuildConfig.EMOJI_VERSION) { "emojiPack.version == " + emojiPack.version }
        this.emojiPack = emojiPack
        this.outdatedEmojiPack = null
        if (emojiPack.identifier == BuildConfig.EMOJI_BUILTIN_ID) {
            pmc.removeByPrefix(KEY_EMOJI_PACK)
        } else {
            pmc.edit()
                .putString(KEY_EMOJI_PACK, emojiPack.identifier)
                .putString(KEY_EMOJI_PACK + "_name", emojiPack.displayName)
                .putInt(KEY_EMOJI_PACK + "_date", emojiPack.date)
                .putInt(KEY_EMOJI_PACK + "_version", emojiPack.version)
                .apply()
        }
    }

    fun getEmojiPackInstallState(setting: CloudSetting, fast: Boolean): Int {
        if (BuildConfig.EMOJI_BUILTIN_ID == setting.identifier) return CloudSetting.STATE_INSTALLED
        val installedVersion = pmc.getInt(KEY_EMOJI_INSTALLED_PREFIX + setting.identifier, 0)
        val hasFile = installedVersion > 0 && (fast || File(Emoji.getEmojiPackDirectory(), setting.identifier).exists())
        return if (hasFile) (if (installedVersion == setting.date) CloudSetting.STATE_INSTALLED else CloudSetting.STATE_UPDATE_NEEDED) else CloudSetting.STATE_NOT_INSTALLED
    }

    private var quickReactions: Array<String?>? = null

    fun setQuickReactions(reactions: Array<String?>?) {
        pmc.putStringArray(KEY_QUICK_REACTIONS, reactions!!)
        quickReactions = reactions
    }

    fun getQuickReactions(tdlib: Tdlib): Array<String?> {
        if (quickReactions == null) {
            quickReactions = pmc.getStringArray(KEY_QUICK_REACTIONS)
            if (quickReactions == null) {
                quickReactions = arrayOf<String?>(
                    tdlib.defaultEmojiReaction()
                )
            }
        }
        return quickReactions!!
    }

    var bigReactionsInChannels: Boolean
        get() = getBoolean(KEY_BIG_REACTIONS_IN_CHANNELS, true)
        set(inChannels) {
            pmc.putBoolean(KEY_BIG_REACTIONS_IN_CHANNELS, inChannels)
        }

    var bigReactionsInChats: Boolean
        get() = getBoolean(KEY_BIG_REACTIONS_IN_CHATS, true)
        set(inChats) {
            pmc.putBoolean(KEY_BIG_REACTIONS_IN_CHATS, inChats)
        }

    fun markEmojiPackInstalled(emojiPack: EmojiPack) {
        pmc.putInt(KEY_EMOJI_INSTALLED_PREFIX + emojiPack.identifier, emojiPack.date)
    }

    fun uninstallEmojiPacks(exceptIdentifier: String?) {
        var editor: SharedPreferences.Editor? = null
        for (entry in pmc.find(KEY_EMOJI_INSTALLED_PREFIX)) {
            if (exceptIdentifier != null && entry.key().substring(KEY_EMOJI_INSTALLED_PREFIX.length) == exceptIdentifier) continue
            if (editor == null) editor = edit()
            editor.remove(entry.key())
        }
        if (editor != null) editor.apply()
    }

    class EmojiPack : CloudSetting {
        constructor() : super(BuildConfig.EMOJI_BUILTIN_ID, BuildConfig.EMOJI_VERSION, Lang.getString(R.string.EmojiBuiltIn), 0)

        constructor(identifier: String?) : super(identifier)

        constructor(message: TdApi.Message) : super(message, "#emoji", R.string.EmojiBuiltIn)

        override fun isBuiltIn(): Boolean {
            return identifier == BuildConfig.EMOJI_BUILTIN_ID
        }

        override fun getInstallState(fast: Boolean): Int {
            /*if (fast) {
        return instance().isEmojiPackInstalledFast(this.identifier);
      } else {
        return Settings.instance().isEmojiPackInstalled(this);
      }*/
            return instance()!!.getEmojiPackInstallState(this, fast)
        }

        override fun install(callback: RunnableBool) {
            Emoji.instance().install(this, callback)
        }

        override fun equals(obj: Any?): Boolean {
            return obj is EmojiPack && super.equals(obj)
        }
    }

    // Icon Packs
    class IconPack : CloudSetting {
        constructor() : super(Config.ICONS_BUILTIN_ID, 1, Lang.getString(R.string.IconsBuiltIn), 0)

        constructor(identifier: String?) : super(identifier)

        constructor(message: TdApi.Message) : super(message, "#icons", R.string.IconsBuiltIn)

        override fun isBuiltIn(): Boolean {
            return Config.ICONS_BUILTIN_ID == this.identifier
        }

        override fun getInstallState(fast: Boolean): Int {
            return STATE_NOT_INSTALLED
        }

        override fun install(callback: RunnableBool) {
            // TODO

            callback.runWithBool(false)
        }

        override fun equals(obj: Any?): Boolean {
            return obj is IconPack && super.equals(obj)
        }
    }

    var iconPack: IconPack? = null
        get() {
            if (field == null) {
                field = IconPack()
            }
            return field
        }
        private set

    @Throws(FileNotFoundException::class)
    fun getKnownSize(path: String?, length: Long, lastModified: Long): Long {
        val data = pmc.getLongArray(KEY_KNOWN_SIZE + path)
        if (data == null || data.size < 3 || data[0] != length || data[1] != lastModified) {
            throw FileNotFoundException()
        }
        return data[2]
    }

    fun forgetKnownSize(path: String?) {
        pmc.remove(KEY_KNOWN_SIZE + path)
    }

    fun putKnownSize(path: String?, length: Long, lastModified: Long, width: Int, height: Int) {
        pmc.putLongArray(KEY_KNOWN_SIZE + path, longArrayOf(length, lastModified, mergeLong(width, height)))
    }

    private fun nextInstallationId(): Long {
        return pmc.getLong(KEY_APP_INSTALLATION_ID, 0) + 1
    }

    private var currentBuildInformation: AppBuildInfo? = null

    fun installationId(): Long {
        if (BuildConfig.DEBUG) {
            return 0
        }
        if (currentBuildInformation != null) {
            return currentBuildInformation!!.installationId
        }
        return pmc.getLong(KEY_APP_INSTALLATION_ID, 0)
    }

    private fun trackInstalledApkVersion() {
        val knownCommitDate = pmc.getLong(KEY_APP_COMMIT_DATE, 0)
        if (maxBuiltInCommitDate() <= knownCommitDate) {
            // Track only updates with more recent commits.
            return
        }
        val installationId = nextInstallationId()
        val buildInfo = AppBuildInfo(installationId)
        pmc.edit()
            .putLong(KEY_APP_INSTALLATION_ID, installationId)
            .putLong(KEY_APP_COMMIT_DATE, buildInfo.maxCommitDate())
        buildInfo.saveTo(pmc, KEY_APP_INSTALLATION_PREFIX + installationId)
        pmc.apply()
        this.currentBuildInformation = buildInfo
        resetAppVersionPushMessageCount()
    }

    private fun trackChangesInAvailableFeatures() {
        val currentlyAvailableFeatures = FeatureAvailability.currentlyAvailableFeatures()
        var previouslyAvailableFeatures: Long
        var saveFeatures = false
        try {
            previouslyAvailableFeatures = pmc.tryGetLong(KEY_FEATURES)
        } catch (e: FileNotFoundException) {
            val previousInstallationId = if (currentBuildInformation != null) currentBuildInformation!!.installationId - 1 else -1
            val previouslyInstalledVersionCode =
                if (previousInstallationId != -1L) restoreVersionCode(pmc, KEY_APP_INSTALLATION_PREFIX + previousInstallationId) else 0
            if (previouslyInstalledVersionCode != 0) {
                previouslyAvailableFeatures = FeatureAvailability.recoverAvailableFeaturesForAppVersionCode(previouslyInstalledVersionCode)
            } else {
                // Do not bombard with a dozen of pop-ups on clean app installations
                previouslyAvailableFeatures = currentlyAvailableFeatures
            }
            saveFeatures = true
        }
        if (currentlyAvailableFeatures != previouslyAvailableFeatures) {
            val recentlyAddedFeatures = currentlyAvailableFeatures and (previouslyAvailableFeatures.inv()).toLong()
            val recentlyRemovedFeatures = previouslyAvailableFeatures and (currentlyAvailableFeatures.inv()).toLong()

            var addedFeaturesNotifications = pmc.getLong(KEY_FEATURES_ADDED_NOTIFICATIONS, 0)
            var removedFeaturesNotifications = pmc.getLong(KEY_FEATURES_REMOVED_NOTIFICATIONS, 0)

            addedFeaturesNotifications = addedFeaturesNotifications and recentlyRemovedFeatures.inv()
            addedFeaturesNotifications = addedFeaturesNotifications or recentlyAddedFeatures

            removedFeaturesNotifications = removedFeaturesNotifications and recentlyAddedFeatures.inv()
            removedFeaturesNotifications = removedFeaturesNotifications or recentlyRemovedFeatures

            pmc.edit()
                .putLong(KEY_FEATURES_ADDED_NOTIFICATIONS, addedFeaturesNotifications)
                .putLong(KEY_FEATURES_REMOVED_NOTIFICATIONS, removedFeaturesNotifications)
                .apply()

            this._addedFeaturesNotifications = addedFeaturesNotifications
            this._removedFeaturesNotifications = removedFeaturesNotifications

            saveFeatures = true
        }
        if (saveFeatures) {
            pmc.putLong(KEY_FEATURES, currentlyAvailableFeatures)
        }
    }

    interface FeatureAvailabilityNotificationDismissListener {
        fun onDismissFeatureAvailabilityNotification(@FeatureAvailability.Feature feature: Long, wasAdded: Boolean, wasRemoved: Boolean)
    }

    private var featureNotificationDismissListeners: ReferenceList<FeatureAvailabilityNotificationDismissListener?>? = null

    fun addFeatureAvailabilityNotificationDismissListener(listener: FeatureAvailabilityNotificationDismissListener?) {
        if (this.featureNotificationDismissListeners == null) {
            this.featureNotificationDismissListeners = ReferenceList<FeatureAvailabilityNotificationDismissListener?>()
        }
        this.featureNotificationDismissListeners!!.add(listener)
    }

    fun removeFeatureAvailabilityNotificationDismissListener(listener: FeatureAvailabilityNotificationDismissListener?) {
        if (this.featureNotificationDismissListeners != null) {
            this.featureNotificationDismissListeners!!.remove(listener)
        }
    }

    private var _addedFeaturesNotifications: Long? = null
    private var _removedFeaturesNotifications: Long? = null

    val addedFeaturesNotifications: Long
        get() {
            if (_addedFeaturesNotifications == null) {
                _addedFeaturesNotifications = pmc.getLong(KEY_FEATURES_ADDED_NOTIFICATIONS, 0)
            }
            return _addedFeaturesNotifications!!
        }

    val removedFeaturesNotifications: Long
        get() {
            if (_removedFeaturesNotifications == null) {
                _removedFeaturesNotifications = pmc.getLong(KEY_FEATURES_REMOVED_NOTIFICATIONS, 0)
            }
            return _removedFeaturesNotifications!!
        }

    fun revokeFeatureNotifications(@FeatureAvailability.Feature feature: Long) {
        var addedFeaturesNotifications = this.addedFeaturesNotifications
        var removedFeaturesNotifications = this.removedFeaturesNotifications
        val wasAdded = hasFlag(addedFeaturesNotifications, feature)
        val wasRemoved = hasFlag(removedFeaturesNotifications, feature)
        if (wasAdded || wasRemoved) {
            addedFeaturesNotifications = addedFeaturesNotifications and feature.inv()
            removedFeaturesNotifications = removedFeaturesNotifications and feature.inv()
            pmc.edit()
                .putLong(KEY_FEATURES_ADDED_NOTIFICATIONS, addedFeaturesNotifications)
                .putLong(KEY_FEATURES_REMOVED_NOTIFICATIONS, removedFeaturesNotifications)
                .apply()
            this._addedFeaturesNotifications = addedFeaturesNotifications
            this._removedFeaturesNotifications = removedFeaturesNotifications
            if (featureNotificationDismissListeners != null) {
                for (listener in featureNotificationDismissListeners) {
                    listener!!.onDismissFeatureAvailabilityNotification(feature, wasAdded, wasRemoved)
                }
            }
        }
    }

    fun hasPendingFeatureAddedNotification(@FeatureAvailability.Feature feature: Long): Boolean {
        return hasAllFlags(this.addedFeaturesNotifications, feature)
    }

    val firstBuildInformation: AppBuildInfo?
        get() {
            val appBuildInfo = getBuildInformation(1)
            return if (appBuildInfo != null) appBuildInfo else getCurrentBuildInformation()
        }

    fun getCurrentBuildInformation(): AppBuildInfo {
        if (currentBuildInformation == null) {
            val installationId = pmc.getLong(KEY_APP_INSTALLATION_ID, 0)
            this.currentBuildInformation = restoreFrom(pmc, installationId, KEY_APP_INSTALLATION_PREFIX + installationId)
        }
        return this.currentBuildInformation!!
    }

    fun getBuildInformation(installationId: Long): AppBuildInfo? {
        return if (installationId > 0) restoreFrom(pmc, installationId, KEY_APP_INSTALLATION_PREFIX + installationId) else null
    }

    val previousBuildInformation: AppBuildInfo?
        get() {
            val currentBuild = getCurrentBuildInformation()
            val previousInstallationId = (currentBuild.installationId - 1)
            return getBuildInformation(previousInstallationId)
        }

    val pushMessageStats: String
        get() = "total: " + this.receivedPushMessageCountTotal + " " +
                "by_token: " + this.receivedPushMessageCountByToken + " " +
                "by_app_version: " + this.receivedPushMessageCountByAppVersion + " "

    val receivedPushMessageCountTotal: Long
        get() = pmc.getLong(KEY_PUSH_STATS_TOTAL_COUNT, 0)

    val receivedPushMessageCountByAppVersion: Long
        get() = pmc.getLong(KEY_PUSH_STATS_CURRENT_APP_VERSION_COUNT, 0)

    val receivedPushMessageCountByToken: Long
        get() = pmc.getLong(KEY_PUSH_STATS_CURRENT_TOKEN_COUNT, 0)

    val lastReceivedPushMessageSentTime: Long
        get() = pmc.getLong(KEY_PUSH_LAST_SENT_TIME, 0)

    val lastReceivedPushMessageReceivedTime: Long
        get() = pmc.getLong(KEY_PUSH_LAST_RECEIVED_TIME, 0)

    val lastReceivedPushMessageTtl: Int
        get() = pmc.getInt(KEY_PUSH_LAST_TTL, 0)

    interface PushStatsListener {
        fun onNewPushReceived()
    }

    private val pushStatsListeners = ReferenceList<PushStatsListener?>(true)

    fun addPushStatsListener(listener: PushStatsListener?) {
        pushStatsListeners.add(listener)
    }

    fun removePushStatsListener(listener: PushStatsListener?) {
        pushStatsListeners.remove(listener)
    }

    fun trackPushMessageReceived(sentTime: Long, receivedTime: Long, ttl: Int) {
        val totalReceivedCount = this.receivedPushMessageCountTotal + 1
        val currentVersionReceivedCount = this.receivedPushMessageCountByAppVersion + 1
        val currentTokenReceivedCount = this.receivedPushMessageCountByToken + 1
        pmc.edit()
            .putLong(KEY_PUSH_STATS_TOTAL_COUNT, totalReceivedCount)
            .putLong(KEY_PUSH_STATS_CURRENT_APP_VERSION_COUNT, currentVersionReceivedCount)
            .putLong(KEY_PUSH_STATS_CURRENT_TOKEN_COUNT, currentTokenReceivedCount)
            .putLong(KEY_PUSH_LAST_SENT_TIME, sentTime)
            .putLong(KEY_PUSH_LAST_RECEIVED_TIME, receivedTime)
            .putInt(KEY_PUSH_LAST_TTL, ttl)
            .apply()
        for (listener in pushStatsListeners) {
            listener!!.onNewPushReceived()
        }
    }

    fun resetAppVersionPushMessageCount() {
        pmc.remove(KEY_PUSH_STATS_CURRENT_APP_VERSION_COUNT)
    }

    fun resetTokenPushMessageCount() {
        pmc.remove(KEY_PUSH_STATS_CURRENT_TOKEN_COUNT)
    }

    var reportedPushServiceError: String?
        get() = pmc.getString(KEY_PUSH_REPORTED_ERROR, null)
        set(error) {
            if (!isEmpty(error)) {
                pmc.edit()
                    .putString(KEY_PUSH_REPORTED_ERROR, error)
                    .putLong(KEY_PUSH_REPORTED_ERROR_DATE, System.currentTimeMillis())
                    .apply()
            } else {
                pmc.edit()
                    .remove(KEY_PUSH_REPORTED_ERROR)
                    .remove(KEY_PUSH_REPORTED_ERROR_DATE)
                    .apply()
            }
        }

    val reportedPushServiceErrorDate: Long
        get() = pmc.getLong(KEY_PUSH_REPORTED_ERROR_DATE, 0)

    var defaultLanguageForTranslateDraft: String
        get() = pmc.getString(KEY_DEFAULT_LANGUAGE_FOR_TRANSLATE_DRAFT, "en")!!
        set(language) {
            pmc.putString(KEY_DEFAULT_LANGUAGE_FOR_TRANSLATE_DRAFT, language)
        }

    fun chatFoldersEnabled(): Boolean {
        return FeatureAvailability.Released.CHAT_FOLDERS
    }

    fun showPeerIds(): Boolean {
        return isExperimentEnabled(EXPERIMENT_FLAG_SHOW_PEER_IDS)
    }

    private var _playbackSpeed: Int? = null

    init {
        val pmcDir = File(UI.getAppContext().getFilesDir(), "pmc")
        var fatalError: Boolean
        try {
            fatalError = !createDirectory(pmcDir)
        } catch (e: SecurityException) {
            e.printStackTrace()
            fatalError = true
        }
        if (fatalError) {
            throw DeviceStorageError("Unable to create working directory")
        }
        val ms = SystemClock.uptimeMillis()
        pmc = LevelDB(File(pmcDir, "db").getPath(), true, object : LevelDB.ErrorHandler {
            override fun onFatalError(levelDB: LevelDB?, error: Throwable): Boolean {
                onDatabaseError(error)
                return true
            }

            override fun onError(levelDB: LevelDB?, message: String?, error: Throwable?) {
                // Cannot use custom Log, since settings are not yet loaded
                android.util.Log.e(Log.LOG_TAG, message, error)
            }
        })
        Log.load(pmc)
        var pmcVersion = 0
        try {
            pmcVersion = max(0, pmc.tryGetInt(KEY_VERSION))
        } catch (e: FileNotFoundException) {
            if (this.isFreshAppInstallation) {
                pmc.putInt(KEY_VERSION, pmcVersion)
                pmcVersion = VERSION
            } else {
                migratePrefsToPmc()
            }
        }
        if (pmcVersion > VERSION) {
            Log.e("Downgrading database version: %d -> %d", pmcVersion, VERSION)
            pmc.putInt(KEY_VERSION, VERSION)
        }
        for (version in pmcVersion + 1..VERSION) {
            val editor: SharedPreferences.Editor = pmc.edit()
            upgradePmc(pmc, editor, version)
            editor.putInt(KEY_VERSION, version)
            editor.apply()
        }
        /*if (BuildConfig.DEBUG) {
      int accountNum = TdlibManager.readAccountNum();
      pmc.edit();
      for (int accountId = 0; accountId < accountNum; accountId++) {
        String key = key(TdlibSettingsManager.DEVICE_TDLIB_VERSION_KEY, accountId);
        pmc.remove(key);
      }
      pmc.apply();
    }*/
        if (BuildConfig.DEBUG) {
            pmc.remove(KEY_TUTORIAL)
            pmc.removeByPrefix(KEY_TUTORIAL_PSA)
        }
        if (Config.TEST_NEW_FEATURES_PROMPTS) {
            forceRevokeAllFeaturePrompts()
        }
        trackInstalledApkVersion()
        trackChangesInAvailableFeatures()
        Log.i("Opened database in %dms", SystemClock.uptimeMillis() - ms)
        checkPendingPasscodeLocks()
        applyLogSettings(true)
    }

    var playbackSpeed: Int
        get() {
            if (_playbackSpeed == null) {
                _playbackSpeed = PlaybackSpeedLayout.normalizeSpeed(pmc.getInt(KEY_PLAYBACK_SPEED, 100))
            }
            return _playbackSpeed!!
        }
        set(speed) {
            require(speed > 0) { speed.toString() }
            pmc.putInt(KEY_PLAYBACK_SPEED, speed.also { _playbackSpeed = it })
        }

    companion object {
        private const val LEGACY_VERSION_1 = 1 // Added video notes
        private const val LEGACY_VERSION_2 = 2 // Turn on albums, if disabled
        private const val LEGACY_VERSION_3 = 3 // Turn off quick share
        private const val LEGACY_VERSION_4 = 4 // Migrate night mode setting
        private const val LEGACY_VERSION_5 = 5 // Remove "record_id"
        private const val LEGACY_VERSION_6 = 6 // Turn off quick share (again)
        private const val LEGACY_VERSION_7 = 7 // Remove "settings_oreo_fix"
        private const val LEGACY_VERSION_8 = 8 // Move push_user_id to push_user_ids
        private const val LEGACY_VERSION_9 = 9 // Remove FLAG_OTHER_DISABLE_CALLS_PROXY
        private val LEGACY_VERSION: Int = LEGACY_VERSION_9 // Do not change this value ever again. Use PMC_VERSION

        private const val VERSION_10 = 10 // Turn on Reduce Motion by default
        private const val VERSION_11 = 11 // Reset silent broadcast settings
        private const val VERSION_12 = 12 // Move passcodeHash for MODE_FINGERPRINT to fingerprintHash
        private const val VERSION_13 = 13 // Remove language unlock
        private const val VERSION_14 = 14 // Auto-save edited photos to the gallery
        private const val VERSION_15 = 15 // Auto-save edited photos to the gallery off
        private const val VERSION_16 = 16 // Auto-save edited photos to the gallery off
        private const val VERSION_17 = 17 // header
        private const val VERSION_18 = 18 // Remove notifications stack
        private const val VERSION_19 = 19 // New badge settings
        private const val VERSION_20 = 20 // Moved TDLib log path
        private const val VERSION_21 = 21 // Added FLAG_OTHER_HIDE_SECRET_CHATS
        private const val VERSION_22 = 22 // Cleared KEY_RTL
        private const val VERSION_23 = 23 // Removed KEY_PUSH_USER_IDS
        private const val VERSION_24 = 24 // Dropped push registrations to re-register again all users
        private const val VERSION_25 = 25 // added use system fonts setting
        private const val VERSION_26 = 26 // added disable big emoji setting
        private const val VERSION_27 = 27 // wallpapers -> backgrounds api
        private const val VERSION_28 = 28 // wallpapers -> backgrounds api
        private const val VERSION_29 = 29 // delete markdownMode
        private const val VERSION_30 = 30 // delete prefer_legacy_api
        private const val VERSION_31 = 31 // move photos & videos taken in secret chats from unsecure location
        private const val VERSION_32 = 32 // Delete ZoomTables.data
        private const val VERSION_33 = 33 // Add NIGHT_MODE_SYSTEM
        private const val VERSION_34 = 34 // scrollToMessageId stack
        private const val VERSION_35 = 35 // clear known conversions
        private const val VERSION_36 = 36 // removed TON
        private const val VERSION_37 = 37 // removed weird "wallpaper_" + file.remote.id unused legacy cache
        private const val VERSION_38 = 38 // int32 -> int64
        private const val VERSION_39 = 39 // drop all previously stored crashes
        private const val VERSION_40 = 40 // drop legacy crash management ids
        private const val VERSION_41 = 41 // clear all application log files
        private const val VERSION_42 = 42 // drop __
        private const val VERSION_43 = 43 // optimize recent custom emoji
        private const val VERSION_44 = 44 // 8-bit -> 32-bit account flags
        private const val VERSION_45 = 45 // Reset "Big emoji" setting to default
        private const val VERSION_46 = 46 // Remove folders experimental setting
        private const val VERSION_47 = 47 // Force reset released features list
        private const val VERSION_48 = 48 // Force strong sensor, if user has it.
        private val VERSION: Int = VERSION_48

        private val hasInstance = AtomicBoolean(false)

        @Volatile
        private var instance: Settings? = null

        @JvmStatic
        fun instance(): Settings {
            if (instance == null) {
                synchronized(Settings::class.java) {
                    if (instance == null) {
                        if (hasInstance.getAndSet(true)) throw AssertionError()
                        instance = Settings()
                    }
                }
            }
            return instance!!
        }

        private const val KEY_VERSION = "version"
        private const val KEY_FEATURES = "features"
        private const val KEY_FEATURES_ADDED_NOTIFICATIONS = "features_new"
        private const val KEY_FEATURES_REMOVED_NOTIFICATIONS = "features_gone"
        private const val KEY_OTHER = "settings_other"
        private const val KEY_OTHER_NEW = "settings_other2"
        private const val KEY_EXPERIMENTS = "settings_experiments"

        @Deprecated("")
        private const val KEY_MARKDOWN_MODE = "settings_markdown"
        private const val KEY_MAP_PROVIDER_TYPE = "settings_map_provider"
        private const val KEY_MAP_PROVIDER_TYPE_CLOUD = "settings_map_provider_cloud"
        private const val KEY_STICKER_MODE = "settings_sticker"
        private const val KEY_EMOJI_MODE = "settings_emoji"
        private const val KEY_REACTION_AVATARS_MODE = "settings_reaction_avatars"
        private const val KEY_AUTO_UPDATE_MODE = "settings_auto_update"
        private const val KEY_INCOGNITO = "settings_incognito"
        private const val KEY_NIGHT_MODE = "settings_night_mode"
        private const val KEY_VIDEO_LIMIT = "settings_video_limit"
        private const val KEY_EARPIECE_MODE = "settings_earpiece_mode"
        private const val KEY_EMOJI_PACK = "settings_emoji_pack"
        private const val KEY_EMOJI_INSTALLED_PREFIX = "settings_emoji_installed_"
        private const val KEY_EARPIECE_VIDEO_MODE = "settings_earpiece_video_mode"
        private const val KEY_MAX_NIGHT_LUX = "night_lux_max"
        private const val KEY_MAX_NIGHT_LUX_MULTIPLY = "night_lux_max_multiply"
        private const val KEY_NIGHT_MODE_SCHEDULED_TIME = "settings_night_mode_schedule"
        private const val KEY_BADGE_FLAGS = "settings_badge_flags"
        private const val KEY_NOTIFICATION_FLAGS = "settings_notification_flags"

        @Deprecated("")
        private const val KEY_BADGE_MODE = "settings_badge_mode"
        private const val KEY_THEME_POSITION = "settings_theme_position"
        private const val KEY_EMOJI_POSITION = "emoji_vp_position"
        private const val KEY_EMOJI_MEDIA_SECTION = "emoji_vp_mediasection"
        private const val KEY_TUTORIAL = "settings_tutorial"
        private const val KEY_TUTORIAL_PSA = "settings_tutorial_psa"
        private const val KEY_CHAT_FONT_SIZE = "settings_font_size"
        private const val KEY_CHAT_LIST_MODE = "settings_chat_list_mode"
        private const val KEY_CHAT_TRANSLATE_MODE = "settings_chat_translate_mode"
        private const val KEY_CHAT_DO_NOT_TRANSLATE_MODE = "settings_chat_do_not_translate_mode"
        private const val KEY_CHAT_DO_NOT_TRANSLATE_LIST = "settings_chat_do_not_translate_list"
        private const val KEY_CHAT_TRANSLATE_RECENTS = "language_recents"
        private const val KEY_DEFAULT_LANGUAGE_FOR_TRANSLATE_DRAFT = "language_draft_translate"
        private const val KEY_INSTANT_VIEW = "settings_iv_mode"
        private const val KEY_RESTRICT_CONTENT = "settings_restrict_content"
        private const val KEY_CAMERA_ASPECT_RATIO = "settings_camera_ratio"
        private const val KEY_CAMERA_TYPE = "settings_camera_type"
        private const val KEY_CAMERA_VOLUME_CONTROL = "settings_camera_control"
        private const val KEY_CHAT_FOLDER_STYLE = "settings_folders_style"
        private const val KEY_CHAT_FOLDER_OPTIONS = "settings_folders_options"

        private const val KEY_TDLIB_VERBOSITY = "settings_tdlib_verbosity"
        private const val KEY_TDLIB_DEBUG_PREFIX = "settings_tdlib_allow_debug"
        private const val KEY_TDLIB_OTHER = "settings_tdlib_other"
        private const val KEY_TDLIB_LOG_SIZE = "settings_tdlib_log_size"

        @Deprecated("")
        private const val KEY_TON_VERBOSITY = "settings_ton_verbosity"

        @Deprecated("")
        private const val KEY_TON_OTHER = "settings_ton_other"

        @Deprecated("")
        private const val KEY_TON_LOG_SIZE = "settings_ton_log_size"

        private const val KEY_ACCOUNT_INFO = "account"
        const val KEY_ACCOUNT_INFO_SUFFIX_ID: String = "" // user_id
        const val KEY_ACCOUNT_INFO_SUFFIX_FLAGS: String = "flags" // premium, verified, etc
        const val KEY_ACCOUNT_INFO_SUFFIX_NAME1: String = "name1" // first_name
        const val KEY_ACCOUNT_INFO_SUFFIX_NAME2: String = "name2" // last_name
        const val KEY_ACCOUNT_INFO_SUFFIX_ACCENT_COLOR_ID: String = "accent_id" // accent_color_id
        const val KEY_ACCOUNT_INFO_SUFFIX_ACCENT_BUILT_IN_ACCENT_COLOR_ID: String = "accent_builtin" // accent_color_id
        const val KEY_ACCOUNT_INFO_SUFFIX_LIGHT_THEME_COLORS: String = "accent_light" // accent_light
        const val KEY_ACCOUNT_INFO_SUFFIX_DARK_THEME_COLORS: String = "accent_dark" // accent_dark
        const val KEY_ACCOUNT_INFO_SUFFIX_MIN_CHAT_BOOST_LEVEL: String = "min_boost_level" // min_chat_boost_level
        const val KEY_ACCOUNT_INFO_SUFFIX_USERNAME: String = "username" // username
        const val KEY_ACCOUNT_INFO_SUFFIX_USERNAMES_ACTIVE: String = "usernames_active" // username
        const val KEY_ACCOUNT_INFO_SUFFIX_USERNAMES_DISABLED: String = "usernames_disabled" // last_name
        const val KEY_ACCOUNT_INFO_SUFFIX_PHONE: String = "phone" // phone
        const val KEY_ACCOUNT_INFO_SUFFIX_PHOTO: String = "photo" // path, if loaded
        const val KEY_ACCOUNT_INFO_SUFFIX_PHOTO_FULL: String = "photo_full" // path, if loaded
        const val KEY_ACCOUNT_INFO_SUFFIX_COUNTER: String = "counter_" // counter

        const val KEY_ACCOUNT_INFO_SUFFIX_EMOJI_STATUS_PREFIX: String = "emoji_" // emoji status
        const val KEY_EMOJI_STATUS_SUFFIX_ID: String = "id"
        const val KEY_EMOJI_STATUS_SUFFIX_METADATA: String = "data"
        const val KEY_EMOJI_STATUS_SUFFIX_THUMBNAIL: String = "thumb"
        const val KEY_EMOJI_STATUS_SUFFIX_STICKER: String = "sticker"

        @JvmStatic
        fun accountInfoPrefix(accountId: Int): String {
            return KEY_ACCOUNT_INFO + accountId + "_"
        }

        private const val KEY_TDLIB_AUTHENTICATION_TOKENS = "settings_authentication_token"
        private const val KEY_TDLIB_CRASH_PREFIX = "settings_tdlib_crash"

        private const val KEY_APP_COMMIT_DATE = "app_commit_date"
        private const val KEY_APP_INSTALLATION_ID = "app_install_id"
        private const val KEY_APP_INSTALLATION_PREFIX = "installation"

        private const val KEY_KNOWN_SIZE = "known_size_for_"
        private const val KEY_LANGUAGE_CURRENT = "settings_language_code"
        private const val KEY_LANGUAGE_CODE_SUFFIX_BASE = "base"
        private const val KEY_LANGUAGE_CODE_SUFFIX_PLURAL = "plural"
        private const val KEY_LANGUAGE_CODE_SUFFIX_RTL = "rtl"
        private const val KEY_SUGGESTED_LANGUAGE_CODE = "settings_language_code_suggested"
        private const val KEY_PREFIX_RTL = "settings_rtl"
        private const val KEY_UTILITY_FEATURES = "debug_features"
        private const val KEY_COLOR_FORMAT = "settings_color_format"
        private const val KEY_PREFERRED_PLAYBACK_MODE = "preferred_audio_mode"
        const val KEY_LOG_SETTINGS: String = "log_settings"
        const val KEY_LOG_LEVEL: String = "log_level"
        const val KEY_LOG_TAGS: String = "log_tags"

        @Deprecated("")
        private const val KEY_PREFER_LEGACY_API = "camera_legacy"
        private const val KEY_INTRO_ATTEMPTED = "intro_attempt"
        private const val KEY_MAP_TYPE = "map_type"
        private const val KEY_LAST_LOCATION = "last_view_location"
        private const val KEY_LAST_INLINE_LOCATION = "last_inline_location"

        @Deprecated("")
        private const val KEY_PIP_X = "pip_x"

        @Deprecated("")
        private const val KEY_PIP_Y = "pip_y"

        @Deprecated("")
        private const val KEY_SILENT_CHANNEL_PREFIX = "channel_silent"
        private const val KEY_PIP = "pip"
        private const val KEY_PAINT_ID = "paint_id"
        private const val KEY_PIP_GRAVITY = "pip_gravity"
        private const val KEY_PLAYER_FLAGS = "player_flags"
        private const val KEY_HIDE_BOT_KEYBOARD_PREFIX = "hide_bot_keyboard_"
        private const val KEY_SCROLL_CHAT_PREFIX = "scroll_chat"
        private const val KEY_SCROLL_CHAT_ALIASES = "_aliases"
        private const val KEY_SCROLL_CHAT_MESSAGE_ID = "_message"
        private const val KEY_SCROLL_CHAT_MESSAGE_CHAT_ID = "_chat"

        @Deprecated("")
        private const val KEY_SCROLL_CHAT_RETURN_TO_MESSAGE_ID = "_return"
        private const val KEY_SCROLL_CHAT_RETURN_TO_MESSAGE_IDS_STACK = "_stack"
        private const val KEY_SCROLL_CHAT_OFFSET = "_offset"
        private const val KEY_SCROLL_CHAT_READ_FULLY = "_read"
        private const val KEY_SCROLL_CHAT_TOP_END = "_top"

        @Deprecated("")
        private const val KEY_PUSH_USER_IDS = "push_user_ids"

        @Deprecated("")
        private const val KEY_PUSH_USER_ID = "push_user_id"
        private const val KEY_PUSH_DEVICE_TOKEN_TYPE = "push_device_token_type"
        private const val KEY_PUSH_DEVICE_TOKEN_OR_ENDPOINT = "push_device_token"
        private const val KEY_PUSH_STATS_TOTAL_COUNT = "push_stats_total"
        private const val KEY_PUSH_STATS_CURRENT_APP_VERSION_COUNT = "push_stats_app"
        private const val KEY_PUSH_STATS_CURRENT_TOKEN_COUNT = "push_stats_token"
        private const val KEY_PUSH_LAST_RECEIVED_TIME = "push_last_received_time"
        private const val KEY_PUSH_LAST_SENT_TIME = "push_last_sent_time"
        private const val KEY_PUSH_LAST_TTL = "push_last_ttl"
        private const val KEY_PUSH_REPORTED_ERROR = "push_reported_error"
        private const val KEY_PUSH_REPORTED_ERROR_DATE = "push_reported_error_date"
        private const val KEY_CRASH_DEVICE_ID = "crash_device_id"
        private const val KEY_IS_EMULATOR = "is_emulator"
        private const val KEY_EMULATOR_DETECTION_RESULT = "emulator"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"

        @Deprecated("")
        private const val KEY_EMOJI_COUNTERS_OLD = "counters_v2"

        @Deprecated("")
        private const val KEY_EMOJI_RECENTS_OLD = "recents_v2"

        @Deprecated("")
        private const val KEY_EMOJI_COLORS_OLD = "colors_v2"

        @Deprecated("")
        private const val KEY_EMOJI_DEFAULT_COLOR_OLD = "default_v2"

        private const val KEY_EMOJI_COUNTERS = "emoji_counters"
        private const val KEY_EMOJI_RECENTS = "emoji_recents"
        private const val KEY_EMOJI_COLORS = "emoji_colors"
        private const val KEY_EMOJI_OTHER_COLORS = "emoji_other_colors"
        private const val KEY_EMOJI_DEFAULT_COLOR = "emoji_default"

        private const val KEY_QUICK_REACTION = "quick_reaction"
        private const val KEY_QUICK_REACTIONS = "quick_reactions"
        private const val KEY_BIG_REACTIONS_IN_CHANNELS = "big_reactions_in_channels"
        private const val KEY_BIG_REACTIONS_IN_CHATS = "big_reactions_in_chats"

        private const val KEY_WALLPAPER_PREFIX = "wallpaper"
        private const val KEY_WALLPAPER_CUSTOM = "_custom"
        private const val KEY_WALLPAPER_EMPTY = "_empty"
        private const val KEY_WALLPAPER_PATH = "_path"
        private const val KEY_WALLPAPER_ID = "_id"

        private fun key(key: String, accountId: Int): String {
            return if (accountId != 0) accountId.toString() + "_" + key else key
        }

        @Deprecated("")
        const val STORAGE_MAIN: String = "main"

        @Deprecated("")
        const val STORAGE_EMOJI: String = "emoji"

        @Deprecated("")
        const val STORAGE_BOTS: String = "bots"

        @Deprecated("")
        const val STORAGE_KEYBOARD: String = "keyboard"

        private const val FLAG_OTHER_AUTOPLAY_GIFS = 1
        private val FLAG_OTHER_SAVE_TO_GALLERY = 1 shl 1
        private val FLAG_OTHER_ACCOUNT_LIST_OPENED = 1 shl 2
        private val FLAG_OTHER_HIDE_SECRET_CHATS = 1 shl 3
        private val FLAG_OTHER_REDUCE_MOTION = 1 shl 4
        private val FLAG_OTHER_FORCE_ARABIC_NUMBERS = 1 shl 5
        private val FLAG_OTHER_FONT_SCALING = 1 shl 6
        private val FLAG_OTHER_REMEMBER_ALBUM_SETTING = 1 shl 7
        private val FLAG_OTHER_USE_SYSTEM_EMOJI = 1 shl 8
        private val FLAG_OTHER_DONT_READ_MESSAGES = 1 shl 9
        private val FLAG_OTHER_NO_CHAT_QUICK_SHARE = 1 shl 10
        private val FLAG_OTHER_NO_CHAT_QUICK_REPLY = 1 shl 11
        private val FLAG_OTHER_SEND_BY_ENTER = 1 shl 12
        private val FLAG_OTHER_HIDE_CHAT_KEYBOARD = 1 shl 13
        private val FLAG_OTHER_USE_QUICK_TRANSLATION = 1 shl 14
        private val FLAG_OTHER_DISABLE_PREVIEW_CHATS_ON_HOLD = 1 shl 15
        private val FLAG_OTHER_NEED_GROUP_MEDIA = 1 shl 16
        private val FLAG_OTHER_DISABLE_INAPP_BROWSER = 1 shl 17
        private val FLAG_OTHER_SEPARATE_MEDIA_TAB = 1 shl 18
        private val FLAG_OTHER_SPLIT_CHAT_NOTIFICATIONS = 1 shl 20
        private val FLAG_OTHER_OUTBOUND_CALLS_PROMPT = 1 shl 21
        private val FLAG_OTHER_DISABLE_CUSTOM_VIBRATIONS = 1 shl 22
        private val FLAG_OTHER_DISABLE_ADDITIONAL_SYNC = 1 shl 23
        private val FLAG_OTHER_PREFER_VIDEO_MODE = 1 shl 25
        private val FLAG_OTHER_HQ_ROUND_VIDEOS = 1 shl 26
        private val FLAG_OTHER_USE_SYSTEM_FONTS = 1 shl 27
        private val FLAG_OTHER_START_ROUND_REAR = 1 shl 28
        private val FLAG_OTHER_DISABLE_BIG_EMOJI = 1 shl 29
        private val FLAG_OTHER_DISABLE_SECRET_LINK_PREVIEWS = 1 shl 30

        @JvmField
        val SETTING_FLAG_BATMAN_POLL_TRANSITIONS: Long = (1 shl 1).toLong()
        @JvmField
        val SETTING_FLAG_EDIT_MARKDOWN: Long = (1 shl 2).toLong()
        @JvmField
        val SETTING_FLAG_NO_ANIMATED_STICKERS_LOOP: Long = (1 shl 3).toLong()
        @JvmField
        val SETTING_FLAG_NO_ANIMATED_EMOJI: Long = (1 shl 4).toLong()
        @JvmField
        val SETTING_FLAG_EXPLICIT_DICE: Long = (1 shl 5).toLong()
        @JvmField
        val SETTING_FLAG_USE_METRIC_FILE_SIZE_UNITS: Long = (1 shl 6).toLong()
        @JvmField
        val SETTING_FLAG_FORCE_EXO_PLAYER_EXTENSIONS: Long = (1 shl 7).toLong()
        @JvmField
        val SETTING_FLAG_NO_AUDIO_COMPRESSION: Long = (1 shl 8).toLong()
        @JvmField
        val SETTING_FLAG_DOWNLOAD_BETAS: Long = (1 shl 9).toLong()
        val SETTING_FLAG_NO_ANIMATED_EMOJI_LOOP: Long = (1 shl 10).toLong()

        @JvmField
        val SETTING_FLAG_CAMERA_NO_FLIP: Long = (1 shl 10).toLong()
        @JvmField
        val SETTING_FLAG_CAMERA_KEEP_DISCARDED_MEDIA: Long = (1 shl 11).toLong()
        @JvmField
        val SETTING_FLAG_CAMERA_SHOW_GRID: Long = (1 shl 12).toLong()

        @JvmField
        val SETTING_FLAG_NO_EMBEDS: Long = (1 shl 13).toLong()
        @JvmField
        val SETTING_FLAG_LIMIT_STICKERS_FPS: Long = (1 shl 14).toLong()
        @JvmField
        val SETTING_FLAG_EXPAND_RECENT_STICKERS: Long = (1 shl 15).toLong()
        @JvmField
        val SETTING_FLAG_FOREGROUND_SERVICE_ENABLED: Long = (1 shl 16).toLong()
        @JvmField
        val SETTING_FLAG_DYNAMIC_ORDER_STICKER_PACKS: Long = (1 shl 17).toLong()
        @JvmField
        val SETTING_FLAG_DYNAMIC_ORDER_EMOJI_PACKS: Long = (1 shl 18).toLong()
        @JvmField
        val SETTING_FLAG_FORCE_DEFAULT_ANIMATION_FOR_RIGHT_SWIPE_EDGE: Long = (1 shl 19).toLong()
        @JvmField
        val SETTING_FLAG_FORCE_DISABLE_HLS_VIDEO: Long = (1 shl 20).toLong()

        const val EXPERIMENT_FLAG_ALLOW_EXPERIMENTS: Long = 1
        @JvmField
        val EXPERIMENT_FLAG_SHOW_PEER_IDS: Long = (1 shl 2).toLong()
        @JvmField
        val EXPERIMENT_FLAG_NO_EDGE_TO_EDGE: Long = (1 shl 3).toLong()
        @JvmField
        val EXPERIMENT_FLAG_FORCE_ALTERNATIVE_PUSH_SERVICE: Long = (1 shl 4).toLong()

        val REMOVED_EXPERIMENT_FLAG_ENABLE_FOLDERS: Long = (1 shl 1).toLong()

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_NEED_RAISE_TO_SPEAK = 1 shl 2

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_AUTODOWNLOAD_IN_BACKGROUND = 1 shl 3

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_DEFAULT_CRASH_MANAGER = 1 shl 5

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_USE_VOICE_DRAFT = 1 shl 6

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_NO_CHAT_SWIPES = 1 shl 7

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_SHOW_FORWARD_OPTIONS = 1 shl 14

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_USE_DIFFERENT_MEDIA_PICKER_LAYOUT = 1 shl 16

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_USE_AUTO_NIGHT_MODE = 1 shl 18

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_NO_ONLINE = 1 shl 23

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_CAMERA_FORCE_16_9 = 1 shl 24

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_ENABLE_RAISE_TO_SPEAK = 1 shl 27

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_GROUP_MEDIA = 1 shl 29

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_DISABLE_CALLS_PROXY = 1 shl 20

        @Deprecated("")
        private val DISABLED_FLAG_OTHER_DISABLE_CUSTOM_TEXT_ACTIONS = 1 shl 19

        const val NIGHT_MODE_NONE: Int = 0
        const val NIGHT_MODE_AUTO: Int = 1
        const val NIGHT_MODE_SCHEDULED: Int = 2
        const val NIGHT_MODE_SYSTEM: Int = 3
        @JvmField
        val NIGHT_MODE_DEFAULT: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) NIGHT_MODE_SYSTEM else NIGHT_MODE_NONE

        const val STICKER_MODE_ALL: Int = 0
        const val STICKER_MODE_ONLY_INSTALLED: Int = 1
        const val STICKER_MODE_NONE: Int = 2

        const val REACTION_AVATARS_MODE_NEVER: Int = 0
        const val REACTION_AVATARS_MODE_SMART_FILTER: Int = 1
        const val REACTION_AVATARS_MODE_ALWAYS: Int = 2

        const val AUTO_UPDATE_MODE_PROMPT: Int = 0
        const val AUTO_UPDATE_MODE_NEVER: Int = 1
        const val AUTO_UPDATE_MODE_WIFI_ONLY: Int = 2
        const val AUTO_UPDATE_MODE_ALWAYS: Int = 3

        const val INCOGNITO_CHAT_SECRET: Int = 1

        const val TUTORIAL_INLINE_SEARCH_SECRECY: Long = 1
        @JvmField
        val TUTORIAL_SECRET_LINK_PREVIEWS: Long = (1 shl 1).toLong()
        val TUTORIAL_YOUTUBE_ROTATION: Long = (1 shl 2).toLong()
        @JvmField
        val TUTORIAL_CHAT_DEMO_TRANSLATION: Long = (1 shl 3).toLong()
        val TUTORIAL_DEVELOPER_MODE: Long = (1 shl 4).toLong()
        val TUTORIAL_TESTER_MODE: Long = (1 shl 5).toLong()
        @JvmField
        val TUTORIAL_EMOJI_TONE_ALL: Long = (1 shl 6).toLong()
        @JvmField
        val TUTORIAL_SYNC_SETTINGS: Long = (1 shl 7).toLong()
        @JvmField
        val TUTORIAL_EMOJI_PACKS: Long = (1 shl 8).toLong()
        val TUTORIAL_SCHEDULE: Long = (1 shl 9).toLong()
        val TUTORIAL_SET_REMINDER: Long = (1 shl 10).toLong()
        val TUTORIAL_SEND_WITHOUT_MARKDOWN: Long = (1 shl 11).toLong()
        val TUTORIAL_SEND_AS_FILE: Long = (1 shl 12).toLong()
        @JvmField
        val TUTORIAL_FORWARD_SCHEDULE: Long = (1 shl 13).toLong()
        @JvmField
        val TUTORIAL_FORWARD_COPY: Long = (1 shl 14).toLong()
        @JvmField
        val TUTORIAL_HOLD_VIDEO: Long = (1 shl 15).toLong()
        val TUTORIAL_PROXY_SPONSOR: Long = (1 shl 16).toLong()
        val TUTORIAL_BRUSH_COLOR_TONE: Long = (1 shl 17).toLong()
        val TUTORIAL_QR_SCAN: Long = (1 shl 18).toLong()
        val TUTORIAL_SELECT_LANGUAGE_INLINE_MODE: Long = (1 shl 19).toLong()
        val TUTORIAL_MULTIPLE_LINK_PREVIEWS: Long = (1 shl 20).toLong()
        @JvmField
        val TUTORIAL_PLAYBACK_SPEED_HOLD: Long = (1 shl 21).toLong()
        @JvmField
        val TUTORIAL_PLAYBACK_SPEED_SWIPE: Long = (1 shl 22).toLong()

        const val BADGE_FLAG_MESSAGES: Int = 1
        const val BADGE_FLAG_MUTED = 1 shl 1
        const val BADGE_FLAG_ARCHIVED = 1 shl 2

        const val NOTIFICATION_FLAG_INCLUDE_PRIVATE: Int = 1
        @JvmField
        val NOTIFICATION_FLAG_INCLUDE_GROUPS: Int = 1 shl 1
        @JvmField
        val NOTIFICATION_FLAG_INCLUDE_CHANNELS: Int = 1 shl 2
        @JvmField
        val NOTIFICATION_FLAG_ONLY_ACTIVE_ACCOUNT: Int = 1 shl 3
        @JvmField
        val NOTIFICATION_FLAG_ONLY_SELECTED_ACCOUNTS: Int = 1 shl 4
        val NOTIFICATION_FLAGS_DEFAULT: Int = NOTIFICATION_FLAG_INCLUDE_PRIVATE

        private val DEFAULT_LOG_SIZE = ByteUnit.MIB.toBytes(50.0)
        private const val DEFAULT_LOG_GLOBAL_VERBOSITY_LEVEL = 0

        // private static final int FLAG_TDLIB_OTHER_USE_DEBUG_DC = 1;
        private val FLAG_TDLIB_OTHER_ENABLE_ANDROID_LOG = 1 shl 1
        const val TDLIB_LOG_VERBOSITY_UNKNOWN = -1

        val CHAT_FONT_SIZES: FloatArray = floatArrayOf(12f, 13f, 14f, 15f, 16f, 18f, 20f, 22f, 24f, 26f)
        const val CHAT_FONT_SIZE_DEFAULT: Float = 15f
        private val CHAT_FONT_SIZE_MIN: Float = CHAT_FONT_SIZES[0]
        private val CHAT_FONT_SIZE_MAX: Float = CHAT_FONT_SIZES[CHAT_FONT_SIZES.size - 1]

        const val INSTANT_VIEW_MODE_NONE: Int = 0
        const val INSTANT_VIEW_MODE_INTERNAL: Int = 1
        const val INSTANT_VIEW_MODE_ALL: Int = 2

        const val MAX_NIGHT_LUX_DEFAULT: Float = 1.5f

        const val MAP_PROVIDER_UNSET = -1
        const val MAP_PROVIDER_NONE: Int = 0
        const val MAP_PROVIDER_TELEGRAM: Int = 1
        const val MAP_PROVIDER_GOOGLE: Int = 2

        @JvmField
        val MAP_PROVIDER_DEFAULT_CLOUD: Int = MAP_PROVIDER_TELEGRAM

        // Schedule
        private const val MSG_DISPATCH_NIGHT_SCHEDULE_CHECK = 0

        const val DO_NOT_TRANSLATE_MODE_APP_LANG: Int = 1
        const val DO_NOT_TRANSLATE_MODE_SELECTED: Int = 2
        const val TRANSLATE_MODE_NONE: Int = 1
        const val TRANSLATE_MODE_POPUP: Int = 2
        const val TRANSLATE_MODE_INLINE: Int = 3
        const val CHAT_MODE_2LINE: Int = 1
        const val CHAT_MODE_3LINE: Int = 2
        const val CHAT_MODE_3LINE_BIG: Int = 3

        private fun makeDefaultExperiments(): Long {
            // TODO: this flag allows implementing later a global toggle that enables/disables all experiments
            // while preserving specific experiments toggle values.
            return EXPERIMENT_FLAG_ALLOW_EXPERIMENTS
        }

        private fun makeDefaultSettings(): Int {
            var defaultSettings = 0

            /*autoplayGIFs = true;
    autoplayGIFs = (settings & FLAG_OTHER_AUTOPLAY_GIFS) != 0;*/
            defaultSettings = defaultSettings or FLAG_OTHER_AUTOPLAY_GIFS

            /*saveEditedMediaToGallery = false;
    saveEditedMediaToGallery = (settings & FLAG_OTHER_SAVE_TO_GALLERY) != 0;*/

            /*forceArabicNumbers = false;
    forceArabicNumbers = (settings & FLAG_OTHER_FORCE_ARABIC_NUMBERS) != 0;*/

            /*reduceMotion = false;
    reduceMotion = (settings & FLAG_OTHER_REDUCE_MOTION) != 0;*/

            /*useSystemEmoji = false;
    useSystemEmoji = Config.ALLOW_SYSTEM_EMOJI && (settings & FLAG_OTHER_USE_SYSTEM_EMOJI) != 0;*/

            /*dontReadMessages = false;
    dontReadMessages = BuildConfig.DEBUG && (settings & FLAG_OTHER_DONT_READ_MESSAGES) != 0;*/

            /*disableChatQuickShare = true;
    disableChatQuickShare = (settings & FLAG_OTHER_NO_CHAT_QUICK_SHARE) != 0;*/
            defaultSettings = defaultSettings or FLAG_OTHER_NO_CHAT_QUICK_SHARE

            /*disableChatQuickReply = false;
    disableChatQuickReply = (settings & FLAG_OTHER_NO_CHAT_QUICK_REPLY) != 0;*/

            /*sendByEnter = false;
    sendByEnter = (settings & FLAG_OTHER_SEND_BY_ENTER) != 0;*/

            /*hideChatKeyboard = false;
    hideChatKeyboard = (settings & FLAG_OTHER_HIDE_CHAT_KEYBOARD) != 0;*/

            /*needForwardOptions = false;
    needForwardOptions = (settings & FLAG_OTHER_SHOW_FORWARD_OPTIONS) != 0;*/

            /*needPreviewChatsOnHold = true;
    needPreviewChatsOnHold = (settings & FLAG_OTHER_DISABLE_PREVIEW_CHATS_ON_HOLD) == 0;*/

            /*useInAppBrowser = true;
    useInAppBrowser = (settings & FLAG_OTHER_DISABLE_INAPP_BROWSER) == 0;*/

            /*useCustomTextActions = true;
    useCustomTextActions = (settings & FLAG_OTHER_DISABLE_CUSTOM_TEXT_ACTIONS) == 0;*/

            /*needOutboundCallsPrompt = false;
    needOutboundCallsPrompt = (settings & FLAG_OTHER_OUTBOUND_CALLS_PROMPT) != 0;*/

            /*useCustomVibrations = true;
    useCustomVibrations = (settings & FLAG_OTHER_DISABLE_CUSTOM_VIBRATIONS) == 0;*/

            /*force169Camera = false;
    force169Camera = (settings & FLAG_OTHER_CAMERA_FORCE_16_9) != 0;*/

            /*preferVideoMode = Config.ROUND_VIDEOS_RECORD_SUPPORTED;
    preferVideoMode = Config.ROUND_VIDEOS_RECORD_SUPPORTED && (settings & FLAG_OTHER_PREFER_VIDEO_MODE) != 0;*/
            if (Config.ROUND_VIDEOS_RECORD_SUPPORTED) {
                defaultSettings = defaultSettings or FLAG_OTHER_PREFER_VIDEO_MODE
            }

            /*needHqRoundVideos = false;
    needHqRoundVideos = (settings & FLAG_OTHER_HQ_ROUND_VIDEOS) != 0;*/

            /*startRoundRear = false;
    startRoundRear = (settings & FLAG_OTHER_START_ROUND_REAR) != 0;*/

            /*disableSecretLinkPreviews = false;
    disableSecretLinkPreviews = (settings & FLAG_OTHER_DISABLE_SECRET_LINK_PREVIEWS) != 0;*/

            /*accountListOpened = false;
    accountListOpened = (settings & FLAG_OTHER_ACCOUNT_LIST_OPENED) != 0;*/

            /*chatFontSizeScaling = false;
    chatFontSizeScaling = (settings & FLAG_OTHER_FONT_SCALING) != 0;*/

            /*rememberAlbumSetting = false;
    rememberAlbumSetting = (settings & FLAG_OTHER_REMEMBER_ALBUM_SETTING) != 0;*/

            /*needGroupMedia = true;
    needGroupMedia = (settings & FLAG_OTHER_NEED_GROUP_MEDIA) != 0;*/
            defaultSettings = defaultSettings or FLAG_OTHER_NEED_GROUP_MEDIA

            /*separateMediaTab = false;
    separateMediaTab = (settings & FLAG_OTHER_SEPARATE_MEDIA_TAB) != 0;*/

            /*splitChatNotifications = true;
    splitChatNotifications = (settings & FLAG_OTHER_SPLIT_CHAT_NOTIFICATIONS) != 0;*/
            defaultSettings = defaultSettings or FLAG_OTHER_SPLIT_CHAT_NOTIFICATIONS

            return defaultSettings

            /*if (needDefault) {
      autoplayGIFs = true;
      saveEditedMediaToGallery = false;
      reduceMotion = false;
      disableChatQuickShare = true;
      disableChatQuickReply = false;
      useSystemEmoji = false;
      sendByEnter = false;
      hideChatKeyboard = false;
      needForwardOptions = false;
      needPreviewChatsOnHold = true;
      useInAppBrowser = true;
      useCustomTextActions = true;
      needOutboundCallsPrompt = false;
      useCustomVibrations = true;
      force169Camera = false;
      preferVideoMode = Config.ROUND_VIDEOS_RECORD_SUPPORTED;
      needHqRoundVideos = false;
      startRoundRear = false;
      disableSecretLinkPreviews = false;
      accountListOpened = false;
      forceArabicNumbers = false;
      chatFontSizeScaling = false;
      rememberAlbumSetting = false;
      needGroupMedia = true;
      separateMediaTab = false;
      splitChatNotifications = true;
    } else {
      autoplayGIFs = (settings & FLAG_OTHER_AUTOPLAY_GIFS) != 0;
      saveEditedMediaToGallery = (settings & FLAG_OTHER_SAVE_TO_GALLERY) != 0;
      forceArabicNumbers = (settings & FLAG_OTHER_FORCE_ARABIC_NUMBERS) != 0;
      reduceMotion = (settings & FLAG_OTHER_REDUCE_MOTION) != 0;
      useSystemEmoji = Config.ALLOW_SYSTEM_EMOJI && (settings & FLAG_OTHER_USE_SYSTEM_EMOJI) != 0;
      dontReadMessages = BuildConfig.DEBUG && (settings & FLAG_OTHER_DONT_READ_MESSAGES) != 0;
      disableChatQuickShare = (settings & FLAG_OTHER_NO_CHAT_QUICK_SHARE) != 0;
      disableChatQuickReply = (settings & FLAG_OTHER_NO_CHAT_QUICK_REPLY) != 0;
      sendByEnter = (settings & FLAG_OTHER_SEND_BY_ENTER) != 0;
      hideChatKeyboard = (settings & FLAG_OTHER_HIDE_CHAT_KEYBOARD) != 0;
      needForwardOptions = (settings & FLAG_OTHER_SHOW_FORWARD_OPTIONS) != 0;
      needPreviewChatsOnHold = (settings & FLAG_OTHER_DISABLE_PREVIEW_CHATS_ON_HOLD) == 0;
      useInAppBrowser = (settings & FLAG_OTHER_DISABLE_INAPP_BROWSER) == 0;
      useCustomTextActions = (settings & FLAG_OTHER_DISABLE_CUSTOM_TEXT_ACTIONS) == 0;
      needOutboundCallsPrompt = (settings & FLAG_OTHER_OUTBOUND_CALLS_PROMPT) != 0;
      useCustomVibrations = (settings & FLAG_OTHER_DISABLE_CUSTOM_VIBRATIONS) == 0;
      force169Camera = (settings & FLAG_OTHER_CAMERA_FORCE_16_9) != 0;
      preferVideoMode = Config.ROUND_VIDEOS_RECORD_SUPPORTED && (settings & FLAG_OTHER_PREFER_VIDEO_MODE) != 0;
      needHqRoundVideos = (settings & FLAG_OTHER_HQ_ROUND_VIDEOS) != 0;
      startRoundRear = (settings & FLAG_OTHER_START_ROUND_REAR) != 0;
      disableSecretLinkPreviews = (settings & FLAG_OTHER_DISABLE_SECRET_LINK_PREVIEWS) != 0;
      accountListOpened = (settings & FLAG_OTHER_ACCOUNT_LIST_OPENED) != 0;
      chatFontSizeScaling = (settings & FLAG_OTHER_FONT_SCALING) != 0;
      rememberAlbumSetting = (settings & FLAG_OTHER_REMEMBER_ALBUM_SETTING) != 0;
      needGroupMedia = (settings & FLAG_OTHER_NEED_GROUP_MEDIA) != 0;
      separateMediaTab = (settings & FLAG_OTHER_SEPARATE_MEDIA_TAB) != 0;
      splitChatNotifications = (settings & FLAG_OTHER_SPLIT_CHAT_NOTIFICATIONS) != 0;
    }*/
        }

        @get:Deprecated("")
        val proxyConfigFile: File
            get() = File(UI.getAppContext().getFilesDir(),  /*debug ? "tdlib_proxy_debug.bin" :*/"tdlib_proxy.bin")

        @Deprecated("")
        @Throws(IOException::class)
        private fun readProxy(file: RandomAccessFile): TdApi.Proxy? {
            when (readVarint(file)) {
                1456461592 -> {
                    val server = readString(file)
                    val port = readVarint(file)
                    val flags = readByte(file)
                    val username = if ((flags.toInt() and 1) != 0) readString(file) else ""
                    val password = if ((flags.toInt() and 2) != 0) readString(file) else ""
                    return TdApi.Proxy(
                        server,
                        port,
                        ProxyTypeSocks5(username, password)
                    )
                }

                else -> return null
            }
        }

        private fun movePreferences(
            from: SharedPreferences?,
            to: SharedPreferences,
            editor: SharedPreferences.Editor?,
            keyPrefix: String?
        ): SharedPreferences.Editor? {
            var editor = editor
            if (from == null) {
                return editor
            }
            val all = from.getAll()
            if (all == null || all.isEmpty()) {
                return editor
            }
            if (editor == null) {
                editor = to.edit()
            }
            for (entry in all.entries) {
                var key = entry.key
                if (keyPrefix != null) {
                    key = keyPrefix + key
                }
                val value: Any? = entry.value
                if (value is Boolean) {
                    editor.putBoolean(key, value)
                } else if (value is Int) {
                    editor.putInt(key, value)
                } else if (value is Long) {
                    editor.putLong(key, value)
                } else if (value is String) {
                    editor.putString(key, value)
                } else if (value is Float) {
                    editor.putFloat(key, value)
                } else {
                    Log.e("Unknown value type, key:%s value:%b", key, value)
                }
            }
            return editor
        }

        const val DEFAULT_VIDEO_LIMIT: Int = 854
        const val DEFAULT_FRAME_RATE: Int = 29 // DefaultVideoStrategy.DEFAULT_FRAME_RATE;

        // Camera
        const val CAMERA_RATIO_16_9: Int = 0
        const val CAMERA_RATIO_4_3: Int = 1
        const val CAMERA_RATIO_1_1: Int = 2
        const val CAMERA_RATIO_FULL_SCREEN: Int = 3

        const val CAMERA_TYPE_LEGACY: Int = 0
        const val CAMERA_TYPE_X: Int = 1
        const val CAMERA_TYPE_SYSTEM: Int = 2

        @JvmField
        val CAMERA_TYPE_DEFAULT: Int =  /*Config.CAMERA_X_AVAILABLE ? CAMERA_TYPE_X : */CAMERA_TYPE_LEGACY

        const val CAMERA_VOLUME_CONTROL_SHOOT: Int = 0
        const val CAMERA_VOLUME_CONTROL_ZOOM: Int = 1
        const val CAMERA_VOLUME_CONTROL_NONE: Int = 2

        fun isGoodChatFontSize(dp: Float): Boolean {
            return dp >= CHAT_FONT_SIZE_MIN && dp <= CHAT_FONT_SIZE_MAX
        }

        private fun notifyFontSizeListeners(list: MutableList<Reference<FontSizeChangeListener?>>, newSizeDp: Float) {
            val size = list.size
            for (i in size - 1 downTo 0) {
                val listener = list.get(i)!!.get()
                if (listener != null) {
                    listener.onFontSizeChanged(newSizeDp)
                } else {
                    list.removeAt(i)
                }
            }
        }

        // Map
        @JvmField
        val MAP_TYPE_UNSET: Int = -1
        const val MAP_TYPE_DEFAULT: Int = 0
        const val MAP_TYPE_DARK: Int = 1
        const val MAP_TYPE_SATELLITE: Int = 2
        const val MAP_TYPE_TERRAIN: Int = 3
        const val MAP_TYPE_HYBRID: Int = 4

        // Wallpaper
        @JvmStatic
        fun getWallpaperIdentifierSuffix(identifier: Int): String {
            when (identifier) {
                0 -> return ""
                1 -> return "_dark"
            }
            return "_other" + identifier
        }

        @Deprecated("")
        private fun parseLocation(value: String, delimiter: String): LastLocation? {
            if (isEmpty(value)) {
                return null
            }
            val data = value.split(delimiter.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                val latitude = data[0].toDouble()
                val longitude = data[1].toDouble()
                val floatValue = if (data.size > 2) data[2].toFloat() else 0f
                return LastLocation(latitude, longitude, floatValue)
            } catch (t: Throwable) {
                Log.e("Cannot read location", t)
            }
            return null
        }

        private fun parseLocation(data: ByteArray?): LastLocation? {
            if (data != null && data.size == 8 + 8 + 4) {
                return LastLocation(readDouble(data, 0), readDouble(data, 8), readFloat(data, 8 + 8))
            }
            return null
        }

        @Deprecated("")
        private fun parseEmojiCounters(infos: MutableMap<String?, RecentInfo?>, cachedInfos: String?) {
            if (cachedInfos != null) {
                val items = cachedInfos.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (cachedInfo in items) {
                    val info = cachedInfo.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (info.size == 3 && isNumeric(info[1]) && isNumeric(info[2])) {
                        val recentInfo = RecentInfo(parseInt(info[1]), parseInt(info[2]))
                        infos.put(info[0], recentInfo)
                    }
                }
            }
        }

        @Deprecated("")
        private fun parseEmojiRecents(infos: MutableMap<String?, RecentInfo?>, recents: MutableList<RecentEmoji>, cachedRecents: String?) {
            if (cachedRecents != null) {
                val items = cachedRecents.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (emoji in items) {
                    val info = infos.get(emoji)
                    if (info != null) {
                        recents.add(RecentEmoji(emoji, info))
                    }
                }
            }
        }

        @Deprecated("")
        private fun parseEmojiColors(colors: MutableMap<String?, String?>, cachedColors: String?) {
            if (cachedColors != null) {
                val items = cachedColors.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (data in items) {
                    val info = data.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (info.size == 2) {
                        colors.put(info[0], info[1])
                    } else if (info.size == 1) {
                        colors.put(info[0], "")
                    }
                }
            }
        }

        private const val EMOJI_OTHER_COLORS_SEPARATOR = ","

        private fun makeScrollChatKey(key: String?, accountId: Int, chatId: Long, topicId: MessageTopic?): String {
            val b = StringBuilder(KEY_SCROLL_CHAT_PREFIX)
                .append(chatId)
            if (key != null) {
                b.append(key)
            }
            if (topicId != null) {
                b.append("_").append(topicId.cacheKey())
            }
            return key(b.toString(), accountId)
        }

        // Proxy
        // type:int   description: Proxy id
        private const val KEY_PROXY_LAST_ID = "proxy_id" // autoincrement

        // type:int description: Current proxy
        private const val KEY_PROXY_CURRENT = "proxy_current"

        // type:byte description: Global proxy settings
        private const val KEY_PROXY_SETTINGS = "proxy_settings"

        // type:int[] description: Proxy order
        private const val KEY_PROXY_ORDER = "proxy_order"

        private const val KEY_PROXY_ITEM_PREFIX = "proxy_item_"

        // type:byte[] description: Proxy configuration (constructor, server, port, ?username, ?password)
        private val KEY_PROXY_PREFIX_CONFIG: String = KEY_PROXY_ITEM_PREFIX + "config_" // + proxy_id

        // type:long   description: Last connection time (in milliseconds)
        @Deprecated("")
        private val KEY_PROXY_PREFIX_CONNECTION_TIME: String = KEY_PROXY_ITEM_PREFIX + "time_" // + proxy_id + "_" + accountId

        // type:string description: Proxy description
        private val KEY_PROXY_PREFIX_DESCRIPTION: String = KEY_PROXY_ITEM_PREFIX + "desc_"

        // type:int description: Amount of times proxy was connected
        private val KEY_PROXY_PREFIX_CONNECTED_COUNT: String = KEY_PROXY_ITEM_PREFIX + "success_"

        // type:long[] description: {time proxy connected, time elapsed since connection attempted}
        private val KEY_PROXY_PREFIX_LAST_CONNECTION: String = KEY_PROXY_ITEM_PREFIX + "connect_"

        // type:long[] description: {time pong received, ping value}
        private val KEY_PROXY_PREFIX_LAST_PING: String = KEY_PROXY_ITEM_PREFIX + "ping_"

        const val PROXY_FLAG_ENABLED: Int = 1
        @JvmField
        val PROXY_FLAG_USE_FOR_CALLS: Int = 1 shl 1
        @JvmField
        val PROXY_FLAG_SHOW_ERRORS: Int = 1 shl 2
        @JvmField
        val PROXY_FLAG_SWITCH_AUTOMATICALLY: Int = 1 shl 3
        @JvmField
        val PROXY_FLAG_SWITCH_ALLOW_DIRECT: Int = 1 shl 4

        @JvmField
        val PROXY_ID_UNKNOWN: Int = -1
        const val PROXY_ID_NONE: Int = 0

        @JvmField
        val PROXY_TIME_UNSET: Int = -1
        @JvmField
        val PROXY_TIME_LOADING: Int = -2
        @JvmField
        val PROXY_TIME_EMPTY: Int = -3

        private fun readProxy(proxyId: Int, data: ByteArray?, blob: Blob?): Proxy? {
            var blob = blob
            if (data == null || data.size == 0) {
                return null
            }
            try {
                if (blob == null) {
                    blob = Blob(data)
                } else {
                    blob.reset(data)
                }
                val server = blob.readString()
                val port = blob.readInt()
                @Proxy.Type val typeId = blob.readByte().toInt()
                val type: TdApi.ProxyType

                when (typeId) {
                    Proxy.Companion.TYPE_SOCKS5 -> {
                        val socks5 = ProxyTypeSocks5("", "")
                        val flags = blob.readByte().toInt()
                        if ((flags and Proxy.Companion.INTERNAL_FLAG_HAS_USERNAME) != 0) socks5.username = blob.readString()
                        if ((flags and Proxy.Companion.INTERNAL_FLAG_HAS_PASSWORD) != 0) socks5.password = blob.readString()
                        type = socks5
                    }

                    Proxy.Companion.TYPE_MTPROTO -> {
                        type = ProxyTypeMtproto(blob.readString())
                    }

                    Proxy.Companion.TYPE_HTTP -> {
                        val http = ProxyTypeHttp("", "", false)
                        val flags = blob.readByte().toInt()
                        if ((flags and Proxy.Companion.INTERNAL_FLAG_HAS_USERNAME) != 0) http.username = blob.readString()
                        if ((flags and Proxy.Companion.INTERNAL_FLAG_HAS_PASSWORD) != 0) http.password = blob.readString()
                        http.httpOnly = blob.readByte() == 1.toByte()
                        type = http
                    }

                    else -> throw UnsupportedOperationException(typeId.toString())
                }

                return Proxy(proxyId, TdApi.Proxy(server, port, type), null)
            } catch (t: Throwable) {
                Log.w("Unable to read proxy configuration", t)
            }
            return null
        }

        @JvmStatic
        fun getProxyUsername(type: TdApi.ProxyType): String? {
            when (type.getConstructor()) {
                ProxyTypeSocks5.CONSTRUCTOR -> return (type as ProxyTypeSocks5).username
                ProxyTypeHttp.CONSTRUCTOR -> return (type as ProxyTypeHttp).username
                ProxyTypeMtproto.CONSTRUCTOR -> return null
                else -> {
                    assertProxyType_bc1a1076()
                    throw unsupported(type)
                }
            }
        }

        @JvmStatic
        fun getProxyPassword(type: TdApi.ProxyType): String? {
            when (type.getConstructor()) {
                ProxyTypeSocks5.CONSTRUCTOR -> return (type as ProxyTypeSocks5).password
                ProxyTypeHttp.CONSTRUCTOR -> return (type as ProxyTypeHttp).password
                ProxyTypeMtproto.CONSTRUCTOR -> return null
                else -> {
                    assertProxyType_bc1a1076()
                    throw unsupported(type)
                }
            }
        }

        @JvmStatic
        fun getProxyDefaultOrder(type: TdApi.ProxyType): Int {
            when (type.getConstructor()) {
                ProxyTypeMtproto.CONSTRUCTOR -> return 1
                ProxyTypeSocks5.CONSTRUCTOR -> return 2
                ProxyTypeHttp.CONSTRUCTOR -> return 3
                else -> {
                    assertProxyType_bc1a1076()
                    throw unsupported(type)
                }
            }
        }

        @Proxy.Type
        private fun getProxyType(type: TdApi.ProxyType): Int {
            when (type.getConstructor()) {
                ProxyTypeSocks5.CONSTRUCTOR -> return Proxy.Companion.TYPE_SOCKS5
                ProxyTypeMtproto.CONSTRUCTOR -> return Proxy.Companion.TYPE_MTPROTO
                ProxyTypeHttp.CONSTRUCTOR -> return Proxy.Companion.TYPE_HTTP
                else -> {
                    assertProxyType_bc1a1076()
                    throw unsupported(type)
                }
            }
        }

        private fun serializeProxy(proxy: TdApi.Proxy): ByteArray {
            @Proxy.Type val typeId: Int = getProxyType(proxy.type)

            val blob: Blob
            var size = 0

            size += sizeOf(proxy.server, false)
            size += 4 /*port*/ + 1 /*typeId*/

            when (typeId) {
                Proxy.Companion.TYPE_SOCKS5 -> {
                    val socks5 = proxy.type as ProxyTypeSocks5

                    size += calculateProxyCredentialsSize(socks5.username, socks5.password)
                }

                Proxy.Companion.TYPE_MTPROTO -> {
                    val mtproto = proxy.type as ProxyTypeMtproto

                    size += sizeOf(if (mtproto.secret != null) mtproto.secret else "", true)
                }

                Proxy.Companion.TYPE_HTTP -> {
                    val http = proxy.type as ProxyTypeHttp

                    size += calculateProxyCredentialsSize(http.username, http.password)
                    size += 1
                }
            }

            blob = Blob(size)
            blob.writeString(proxy.server)
            blob.writeInt(proxy.port)
            blob.writeByte(typeId.toByte())

            when (typeId) {
                Proxy.Companion.TYPE_SOCKS5 -> {
                    val socks5 = proxy.type as ProxyTypeSocks5
                    writeProxyCredentials(blob, socks5.username, socks5.password)
                }

                Proxy.Companion.TYPE_MTPROTO -> {
                    val mtproto = proxy.type as ProxyTypeMtproto

                    blob.writeString(if (mtproto.secret != null) mtproto.secret else "")
                }

                Proxy.Companion.TYPE_HTTP -> {
                    val http = proxy.type as ProxyTypeHttp

                    writeProxyCredentials(blob, http.username, http.password)
                    blob.writeByte((if (http.httpOnly) 1 else 0).toByte())
                }
            }

            return blob.toByteArray()
        }

        private fun calculateProxyCredentialsSize(username: String?, password: String?): Int {
            return 1 + sizeOf(username, false) + sizeOf(password, false)
        }

        private fun writeProxyCredentials(blob: Blob, username: String, password: String) {
            var flags = 0
            if (!isEmpty(username)) flags = flags or Proxy.Companion.INTERNAL_FLAG_HAS_USERNAME
            if (!isEmpty(password)) flags = flags or Proxy.Companion.INTERNAL_FLAG_HAS_PASSWORD
            blob.writeByte(flags.toByte())
            if ((flags and Proxy.Companion.INTERNAL_FLAG_HAS_USERNAME) != 0) blob.writeString(username)
            if ((flags and Proxy.Companion.INTERNAL_FLAG_HAS_PASSWORD) != 0) blob.writeString(password)
        }

        const val PROXY_UPDATE_AWAIT_SECONDS: Double = 1.5
        const val PROXY_UPDATE_PERIOD_SECONDS: Double = 60.0

        const val EARPIECE_MODE_NEVER: Int = 0
        const val EARPIECE_MODE_PROXIMITY: Int = 1
        const val EARPIECE_MODE_ALWAYS: Int = 2

        // Passcode brute force
        private const val KEY_BRUT_FORCE_BLOCK_SECONDS = "brut_force_seconds"
        private const val KEY_BRUT_FORCE_ERROR_PREFIX = "brut_force_errors"

        fun readLanguage(pmc: LevelDB, saveKey: String, needDefault: Boolean): LanguagePackInfo? {
            var languagePackId: String? = null
            var pluralCode: String? = null
            var baseLanguagePackId: String? = null
            var isRtl = false
            for (entry in pmc.find(saveKey)) {
                val key = entry.key()
                if (key.length == saveKey.length) {
                    languagePackId = entry.asString()
                } else {
                    when (key.substring(saveKey.length)) {
                        KEY_LANGUAGE_CODE_SUFFIX_BASE -> baseLanguagePackId = entry.asString()
                        KEY_LANGUAGE_CODE_SUFFIX_PLURAL -> pluralCode = entry.asString()
                        KEY_LANGUAGE_CODE_SUFFIX_RTL -> isRtl = entry.asBoolean()
                    }
                }
            }
            if (!isEmpty(languagePackId)) {
                if (isEmpty(baseLanguagePackId) && TD.isLocalLanguagePackId(languagePackId)) {
                    baseLanguagePackId = Lang.getBuiltinLanguagePackId()
                }
                if (isEmpty(pluralCode)) {
                    pluralCode = Lang.normalizeLanguageCode(if (baseLanguagePackId != null) baseLanguagePackId else languagePackId)
                }
                return Lang.newLanguagePackInfo(languagePackId, baseLanguagePackId, pluralCode, isRtl)
            }
            return if (needDefault) Lang.getBuiltinLanguage() else null
        }

        fun saveLanguage(pmc: LevelDB, saveKey: String?, languagePackInfo: LanguagePackInfo?) {
            if (languagePackInfo == null || Lang.isBuiltinLanguage(languagePackInfo.id)) {
                val editor: SharedPreferences.Editor = pmc.edit()
                editor
                    .remove(saveKey)
                    .remove(saveKey + KEY_LANGUAGE_CODE_SUFFIX_BASE)
                    .remove(saveKey + KEY_LANGUAGE_CODE_SUFFIX_PLURAL)
                    .remove(saveKey + KEY_LANGUAGE_CODE_SUFFIX_RTL)
                    .apply()
            } else {
                val editor: SharedPreferences.Editor = pmc.edit()
                editor.putString(saveKey, languagePackInfo.id)

                if (!isEmpty(languagePackInfo.baseLanguagePackId) && !TD.isLocalLanguagePackId(languagePackInfo.id)) editor.putString(
                    saveKey + KEY_LANGUAGE_CODE_SUFFIX_BASE,
                    languagePackInfo.baseLanguagePackId
                )
                else editor.remove(saveKey + KEY_LANGUAGE_CODE_SUFFIX_BASE)

                if (!isEmpty(languagePackInfo.pluralCode) && languagePackInfo.pluralCode != languagePackInfo.id) editor.putString(
                    saveKey + KEY_LANGUAGE_CODE_SUFFIX_PLURAL,
                    languagePackInfo.pluralCode
                )
                else editor.remove(saveKey + KEY_LANGUAGE_CODE_SUFFIX_PLURAL)

                if (languagePackInfo.isRtl) editor.putBoolean(saveKey + KEY_LANGUAGE_CODE_SUFFIX_RTL, true)
                else editor.remove(saveKey + KEY_LANGUAGE_CODE_SUFFIX_RTL)

                editor.apply()
            }
        }

        // Themes
        private const val KEY_THEMES_CREATED_COUNT = "settings_theme_count"

        private const val KEY_THEME_FULL = "theme"
        private const val KEY_THEME_NAME = "theme_name"
        private const val KEY_THEME_AUTHOR = "theme_author"
        private const val KEY_THEME_WALLPAPER = "theme_wallpaper"
        private const val KEY_THEME_FLAGS = "theme_flags"
        private const val KEY_THEME_HISTORY = "theme_history"

        private fun themePropertyKey(customThemeId: Int, @PropertyId propertyId: Int): String {
            return themePropertyKey(customThemeId, Theme.getPropertyName(propertyId))
        }

        private fun themePropertyKey(customThemeId: Int, colorName: String?): String {
            return KEY_THEME_FULL + customThemeId + "_p_" + colorName
        }

        private fun themeColorKey(customThemeId: Int, @ColorId colorId: Int): String {
            return themeColorKey(customThemeId, Theme.getColorName(colorId))
        }

        private fun themeColorKey(customThemeId: Int, colorName: String?): String {
            return KEY_THEME_FULL + customThemeId + "_c_" + colorName
        }

        private fun themeColorHistoryKey(customThemeId: Int, @ColorId colorId: Int): String {
            return KEY_THEME_HISTORY + customThemeId + "_" + Theme.getColorName(colorId)
        }

        private fun processThemeEntry(
            entry: LevelDB.Entry,
            startIndex: Int,
            theme: ThemeCustom,
            colorsMap: Map<String, Int>,
            propsMap: Map<String, Int>
        ) {
            val key = entry.key()
            val type = key.get(startIndex)
            val name = key.substring(startIndex + 2)
            when (type) {
                'c' -> {
                    val id = colorsMap.get(name)
                    if (id != null) {
                        theme.setColor(id, entry.asInt())
                    } else {
                        Log.w("Unknown theme color: %s", name)
                    }
                }

                'p' -> {
                    val id = propsMap.get(name)
                    if (id != null) {
                        theme.setProperty(id, entry.asFloat())
                    } else {
                        Log.w("Unknown theme property: %s", name)
                    }
                }

                else -> Log.w("Unknown theme key: %s", key)
            }
        }

        private fun processThemeEntry(
            entry: LevelDB.Entry,
            startIndex: Int,
            theme: ThemeExportInfo,
            colors: Map<String, Int>?,
            properties: Map<String, Int>?
        ) {
            val key = entry.key()
            val type = key.get(startIndex)
            val name = key.substring(startIndex + 2)
            when (type) {
                'c' -> {
                    theme.addColor(name, entry.asInt())
                    if (colors != null) (colors as? MutableMap<String, Int>)?.remove(name)
                }

                'p' -> {
                    val value = entry.asFloat()
                    theme.addProperty(name, value)
                    if (theme.parentThemeId == ThemeId.NONE && getName(PropertyId.PARENT_THEME) == name) {
                        theme.parentThemeId = value.toInt()
                    }
                    if (properties != null) (properties as? MutableMap<String, Int>)?.remove(name)
                }

                else -> Log.w("Unknown theme key: %s", key)
            }
        }

        const val COLOR_FORMAT_HEX: Int = 0
        const val COLOR_FORMAT_RGB: Int = 1
        const val COLOR_FORMAT_HSL: Int = 2

        const val THEME_FLAG_INSTALLED: Int = 1
        @JvmField
        val THEME_FLAG_COPY: Int = 1 shl 1

        // Testing Utilities
        private const val UTILITY_FEATURE_HIDE_NUMBER = 1

        // private static final int UTILITY_FEATURE_ENCRYPTED_PUSHES = 1 << 1;
        private val UTILITY_FEATURE_FORCE_TCP_IN_CALLS = 1 shl 2
        private val UTILITY_FEATURE_INSTANT_TDLIB_RESTART = 1 shl 3
        private val UTILITY_FEATURE_NO_NETWORK = 1 shl 4
        private val UTILITY_FEATURE_TABS = 1 shl 5
        private val UTILITY_FEATURE_NO_QR_PROCESS = 1 shl 6
        private val UTILITY_FEATURE_QR_ZXING = 1 shl 7
        private val UTILITY_FEATURE_QR_REGION_DEBUG = 1 shl 8

        private fun makeCrashPrefix(crashId: Long): String {
            return KEY_TDLIB_CRASH_PREFIX + crashId + "_"
        }

        // Push token
        @JvmStatic
        fun storeDeviceToken(deviceToken: DeviceToken, editor: SharedPreferences.Editor, keyTokenType: String?, keyTokenOrEndpoint: String?) {
            @DeviceTokenType val tokenType = TdlibNotificationUtils.getDeviceTokenType(deviceToken)
            val tokenOrEndpoint: String?
            when (tokenType) {
                DeviceTokenType.FIREBASE_CLOUD_MESSAGING -> tokenOrEndpoint = (deviceToken as DeviceTokenFirebaseCloudMessaging).token
                DeviceTokenType.HUAWEI_PUSH_SERVICE -> tokenOrEndpoint = (deviceToken as DeviceTokenHuaweiPush).token
                DeviceTokenType.SIMPLE_PUSH_SERVICE -> tokenOrEndpoint = (deviceToken as DeviceTokenSimplePush).endpoint
                else -> {
                    assertDeviceToken_de4a4f61()
                    throw unsupported(deviceToken)
                }
            }
            editor
                .putInt(keyTokenType, tokenType)
                .putString(keyTokenOrEndpoint, tokenOrEndpoint)
        }

        @JvmStatic
        fun newDeviceToken(@DeviceTokenType tokenType: Int, tokenOrEndpoint: String?): DeviceToken? {
            if (isEmpty(tokenOrEndpoint)) {
                return null
            }
            when (tokenType) {
                DeviceTokenType.FIREBASE_CLOUD_MESSAGING -> return DeviceTokenFirebaseCloudMessaging(tokenOrEndpoint, true)
                DeviceTokenType.SIMPLE_PUSH_SERVICE -> return DeviceTokenSimplePush(tokenOrEndpoint)
                DeviceTokenType.HUAWEI_PUSH_SERVICE -> return DeviceTokenHuaweiPush(tokenOrEndpoint, true)
            }
            return null
        }
    }
}
