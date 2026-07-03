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
 * File created on 18/02/2018
 */
package org.thunderdog.challegram.telegram

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.*
import android.os.Message
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.*
import androidx.collection.SparseArrayCompat
import androidx.core.os.CancellationSignal
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.vkryl.core.*
import me.vkryl.core.collection.IntList
import me.vkryl.core.collection.LongList
import me.vkryl.core.collection.LongSet
import me.vkryl.core.lambda.*
import me.vkryl.core.unit.ByteUnit
import me.vkryl.core.util.ConditionalExecutor
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.*
import org.thunderdog.challegram.*
import org.thunderdog.challegram.R
import org.thunderdog.challegram.component.base.SettingView
import org.thunderdog.challegram.component.chat.MessagesManager
import org.thunderdog.challegram.component.popups.ModernActionedLayout
import org.thunderdog.challegram.component.preview.PreviewLayout
import org.thunderdog.challegram.component.sticker.StickerSetWrap
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.core.Background
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.core.Lang.SpanCreator
import org.thunderdog.challegram.core.getAllKeys
import org.thunderdog.challegram.data.*
import org.thunderdog.challegram.data.TGMessage.MessageIdChangeListener
import org.thunderdog.challegram.mediaview.MediaViewController
import org.thunderdog.challegram.navigation.*
import org.thunderdog.challegram.navigation.SettingsWrapBuilder.CustomSettingProcessor
import org.thunderdog.challegram.navigation.TooltipOverlayView.TooltipBuilder
import org.thunderdog.challegram.navigation.ViewController.*
import org.thunderdog.challegram.telegram.TdlibMessageViewer.Viewport
import org.thunderdog.challegram.theme.*
import org.thunderdog.challegram.tool.Intents
import org.thunderdog.challegram.tool.Screen
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.ui.*
import org.thunderdog.challegram.ui.ChatsController.PickerDelegate
import org.thunderdog.challegram.ui.MessagesController.Referrer
import org.thunderdog.challegram.ui.PasswordController.CustomConfirmDelegate
import org.thunderdog.challegram.ui.camera.CameraController
import org.thunderdog.challegram.ui.camera.CameraController.QrCodeListener
import org.thunderdog.challegram.unsorted.Passcode
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.util.*
import org.thunderdog.challegram.util.text.Text
import org.thunderdog.challegram.voip.VoIPLogs
import org.thunderdog.challegram.widget.*
import org.thunderdog.challegram.widget.ForceTouchView.ForceTouchContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import tgx.td.*
import tgx.td.data.MessageWithProperties
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileReader
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.contracts.ExperimentalContracts
import kotlin.math.floor
import kotlin.math.max

@OptIn(ExperimentalContracts::class)
class TdlibUi /*package*/ internal constructor(private val tdlib: Tdlib) : Handler(Looper.getMainLooper()) {
    fun execute(runnable: Runnable) {
        if (UI.inUiThread()) {
            runnable.run()
        } else {
            post(runnable)
        }
    }

    override fun handleMessage(msg: Message) {
        if (msg.what >= 0) {
            tdlib.handleUiMessage(msg)
        }
    }

    fun unblockMember(c: ViewController<*>, chatId: Long, senderId: MessageSender, currentStatus: ChatMemberStatus) {
        if (senderId.getConstructor() == MessageSenderChat.CONSTRUCTOR) {
            tdlib.setChatMemberStatus(chatId, senderId, ChatMemberStatusLeft(), currentStatus, null)
            return
        }
        if (currentStatus.getConstructor() == ChatMemberStatusRestricted.CONSTRUCTOR) {
            tdlib.setChatMemberStatus(chatId, senderId, ChatMemberStatusMember(), currentStatus, null)
            return
        }

        c.showSettings(
            SettingsWrapBuilder(R.id.btn_unblockSender)
                .addHeaderItem(ListItem(ListItem.TYPE_INFO, 0, 0, Lang.getString(R.string.QUnblockX, tdlib.senderName(senderId)), false))
                .setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                    val addBackToGroup = result!!.get(R.id.btn_inviteBack) != 0
                    tdlib.setChatMemberStatus(chatId, senderId, if (addBackToGroup) ChatMemberStatusMember() else ChatMemberStatusLeft(), currentStatus, null)
                })
                .setRawItems(
                    arrayOf<ListItem>(
                        ListItem(
                            ListItem.TYPE_CHECKBOX_OPTION,
                            R.id.btn_inviteBack,
                            0,
                            if (tdlib.isChannel(chatId)) R.string.InviteBackToChannel else R.string.InviteBackToGroup,
                            false
                        )
                    )
                )
                .setSaveStr(R.string.Unban)
                .setSaveColorId(ColorId.textNegative)
        )
    }

    private fun getBlockString(chatId: Long, senderId: MessageSender, willBeBlocked: Boolean): CharSequence {
        if (tdlib.isChannel(chatId)) {
            return Lang.getStringBold(if (willBeBlocked) R.string.MemberCannotJoinChannel else R.string.MemberCanJoinChannel, tdlib.senderName(senderId))
        } else {
            return Lang.getStringBold(if (willBeBlocked) R.string.MemberCannotJoinGroup else R.string.MemberCanJoinGroup, tdlib.senderName(senderId))
        }
    }

    fun kickMember(c: ViewController<*>, chatId: Long, senderId: MessageSender, currentStatus: ChatMemberStatus) {
        if (senderId.getConstructor() == MessageSenderChat.CONSTRUCTOR) return
        if (getType(chatId) == TdApi.ChatTypeBasicGroup.CONSTRUCTOR) {
            c.showOptions(
                Lang.getStringBold(R.string.MemberCannotJoinRegularGroup, tdlib.senderName(senderId, true)),
                intArrayOf(R.id.btn_blockSender, R.id.btn_cancel),
                arrayOf<String?>(
                    Lang.getString(R.string.RemoveFromGroup), Lang.getString(R.string.Cancel)
                ),
                intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                intArrayOf(R.drawable.baseline_remove_circle_24, R.drawable.baseline_cancel_24),
                OptionDelegate { itemView: View?, id: Int ->
                    if (id == R.id.btn_blockSender) {
                        tdlib.setChatMemberStatus(chatId, senderId, ChatMemberStatusLeft(), currentStatus, null)
                    }
                    true
                })
            return
        }
        val headerItem = ListItem(ListItem.TYPE_INFO, 0, 0, getBlockString(chatId, senderId, true), false)
        c.showSettings(
            SettingsWrapBuilder(R.id.btn_blockSender)
                .addHeaderItem(headerItem)
                .setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                    val blockUser = result!!.get(R.id.btn_restrictMember) != 0
                    if (currentStatus.getConstructor() == ChatMemberStatusRestricted.CONSTRUCTOR && !blockUser) {
                        val now = currentStatus as ChatMemberStatusRestricted
                        tdlib.setChatMemberStatus(
                            chatId,
                            senderId,
                            ChatMemberStatusRestricted(false, now.restrictedUntilDate, now.permissions),
                            currentStatus,
                            null
                        )
                    } else {
                        tdlib.setChatMemberStatus(chatId, senderId, ChatMemberStatusBanned(), currentStatus, null)
                        if (!blockUser) {
                            tdlib.setChatMemberStatus(chatId, senderId, ChatMemberStatusLeft(), currentStatus, null)
                        }
                    }
                })
                .setOnSettingItemClick(object : ViewController.OnSettingItemClick { override fun onSettingItemClick(view: View?, settingsId: Int, item: ListItem?, doneButton: TextView?, settingsAdapter: SettingsAdapter?, window: PopupLayout?) {
                    headerItem.setString(getBlockString(chatId, senderId, settingsAdapter!!.getCheckIntResults().get(R.id.btn_restrictMember) != 0))
                    settingsAdapter.updateValuedSettingByPosition(settingsAdapter.indexOfView(headerItem))
                }})
                .setRawItems(
                    arrayOf<ListItem>(
                        ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_restrictMember, 0, R.string.BanMember, true)
                    )
                ).setSaveStr(R.string.RemoveMember).setSaveColorId(ColorId.textNegative)
        )
    }

    fun showDeleteOptions(context: ViewController<*>, message: TdApi.Message) {
        tdlib.getMessageProperties(message, RunnableData { properties: MessageProperties? ->
            context.runOnUiThreadOptional(Runnable {
                showDeleteOptions(context, MessageWithProperties(message, properties!!))
            })
        })
    }

    fun showDeleteOptions(context: ViewController<*>?, message: MessageWithProperties?) {
        showDeleteOptions(context, arrayOf<MessageWithProperties>(message!!), null)
    }

    fun showDeleteOptions(context: ViewController<*>?, messages: Array<MessageWithProperties>?, after: Runnable?) {
        if (context != null && messages != null && messages.size > 0) {
            if (deleteSuperGroupMessages(context, messages, after)) {
                return
            }
            if (deleteWithRevoke(context, messages, after)) {
                return
            }

            val tdlib = context.tdlib()!!
            val chatId = messages.findUniqueChatId()

            var allScheduled = true
            for (msg in messages) {
                if (!TD.isScheduled(msg.message)) {
                    allScheduled = false
                    break
                }
            }

            var deleteActionMsg = if (messages.size == 1) Lang.getString(
                if (allScheduled) (if (tdlib.isSelfChat(chatId)) R.string.DeleteReminder else R.string.DeleteScheduled) else (if (tdlib.isSelfChat(chatId)) R.string.DeleteMessage else R.string.DeleteForMe)
            ) else Lang.plural(
                if (allScheduled) (if (tdlib.isSelfChat(chatId)) R.string.DeleteXReminders else R.string.DeleteXScheduled) else (if (tdlib.isSelfChat(chatId)) R.string.DeleteXMessages else R.string.DeleteXForMe),
                messages.size.toLong()
            )

            if (!allScheduled) {
                for (msg in messages) {
                    if (!msg.properties.canBeDeletedOnlyForSelf) {
                        if (isUserChat(chatId)) {
                            deleteActionMsg = Lang.getString(R.string.DeleteForMeAndX, tdlib.cache().userFirstName(tdlib.chatUserId(chatId)))
                        } else {
                            deleteActionMsg = Lang.getString(R.string.DeleteForEveryone)
                        }
                        break
                    }
                }
            }

            context.showOptions(
                null,
                intArrayOf(R.id.menu_btn_delete, R.id.btn_cancel),
                arrayOf<String?>(deleteActionMsg, Lang.getString(R.string.Cancel)),
                intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
                OptionDelegate { itemView: View?, id: Int ->
                    if (id == R.id.menu_btn_delete) {
                        val messageIds = messages.toMessageIdsMap()
                        for (i in 0..<messageIds.size()) {
                            tdlib.deleteMessages(messageIds.keyAt(i), messageIds.valueAt(i), false)
                        }
                        if (after != null) {
                            after.run()
                        }
                    }
                    true
                }
            )
        }
    }

    fun getTTLShort(chatId: Long): String? {
        val ttl = tdlib.chatTTL(chatId)
        return if (ttl > 0) getDuration(ttl.toLong(), TimeUnit.SECONDS, false) else null
    }

    fun openSupport(context: ViewController<*>): CancellableRunnable {
        val chat = arrayOfNulls<Chat>(1)
        val error = arrayOfNulls<TdApi.Error>(1)

        val runnable: CancellableRunnable = object : CancellableRunnable() {
            public override fun act() {
                if (chat[0] != null) {
                    openChat(context, chat[0]!!, null)
                } else if (error[0] != null) {
                    UI.showError(error[0])
                }
            }
        }

        tdlib.client().send(GetSupportUser(), tdlib.silentHandler())

        val popupFinal = arrayOfNulls<PopupLayout>(1)
        popupFinal[0] = context.showOptions(
            Strings.buildMarkdown(
                context,
                Lang.getString(R.string.AskAQuestionInfo),
                CustomTypefaceSpan.OnClickListener { view: View?, span: CustomTypefaceSpan?, clickedText: String? ->
                    if (popupFinal[0] != null) {
                        popupFinal[0]!!.hideWindow(true)
                    }
                    false
                }),
            intArrayOf(R.id.btn_openChat, R.id.btn_cancel),
            StringList.asArray(R.string.AskButton, R.string.Cancel),
            intArrayOf(ViewController.OptionColor.BLUE, ViewController.OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_help_24, R.drawable.baseline_cancel_24),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_openChat) {
                    tdlib.client().send(GetSupportUser(), Client.ResultHandler { `object`: TdApi.Object? ->
                        when (`object`!!.getConstructor()) {
                            TdApi.User.CONSTRUCTOR -> {
                                val supportUser = `object` as TdApi.User
                                tdlib.client().send(CreatePrivateChat(supportUser.id, false), Client.ResultHandler { object1: TdApi.Object? ->
                                    when (object1!!.getConstructor()) {
                                        Chat.CONSTRUCTOR -> {
                                            chat[0] = object1 as Chat
                                            tdlib.ui().post(runnable)
                                        }

                                        TdApi.Error.CONSTRUCTOR -> {
                                            error[0] = object1 as TdApi.Error
                                            tdlib.ui().post(runnable)
                                        }
                                    }
                                })
                            }

                            TdApi.Error.CONSTRUCTOR -> {
                                error[0] = `object` as TdApi.Error
                                tdlib.ui().post(runnable)
                            }
                        }
                    })
                }
                true
            })
        return runnable
    }

    fun shouldSendScreenshotHint(chat: Chat?): Boolean {
        val context = UI.getUiContext()
        if (context != null) {
            val popupLayout = context.getCurrentPopupWindow()
            return popupLayout != null && popupLayout.getBoundController() is MediaViewController && (popupLayout.getBoundController() as MediaViewController).getArgumentsStrict().mode == MediaViewController.MODE_SECRET
        }
        return false
    }

    // TTL
    class TTLOption(val ttlTime: Int, val ttlString: String) {
        val isOff: Boolean
            get() = ttlTime == 0

        val isImmediate: Boolean
            get() = ttlTime == IMMEDIATE

        override fun toString(): String {
            return ttlString
        }

        companion object {
            val IMMEDIATE: Int = -1
        }
    }

    fun updateTTLButton(menuId: Int, headerView: HeaderView?, chat: Chat?, force: Boolean): Boolean {
        var isVisible = false
        if (headerView != null) {
            headerView.updateMenuStopwatch(
                menuId,
                R.id.menu_btn_stopwatch,
                getTTLShort(if (chat != null) chat.id else 0),
                tdlib.canChangeMessageAutoDeleteTime(if (chat != null) chat.id else 0).also { isVisible = it },
                force
            )
        }
        return isVisible
    }

    private fun setTTL(chat: Chat?, newTtl: Int) {
        if (chat == null) {
            return
        }
        val oldTtl = tdlib.chatTTL(chat.id)
        if (oldTtl != newTtl) {
            tdlib.setChatMessageAutoDeleteTime(chat.id, newTtl)
        }
    }

    fun showTTLPicker(context: Context?, chat: Chat) {
        val ttl = tdlib.chatTTL(chat.id)
        val selfDestructType: MessageSelfDestructType? = if (ttl != 0) MessageSelfDestructTypeTimer(ttl) else null
        showTTLPicker(context, selfDestructType, !isSecret(chat.id), false, false, 0, RunnableData<TTLOption?> { result: TTLOption? -> setTTL(chat, result!!.ttlTime) })
    }

    fun showTTLPicker(
        context: Context?,
        currentSelfDestructType: MessageSelfDestructType?,
        allowInstant: Boolean,
        useDarkMode: Boolean,
        precise: Boolean,
        @StringRes message: Int,
        callback: RunnableData<TTLOption?>?
    ) {
        val ttlOptions = ArrayList<TTLOption>(21)
        ttlOptions.add(TTLOption(0, Lang.getString(R.string.Off)))
        if (allowInstant) {
            ttlOptions.add(TTLOption(-1, Lang.getString(R.string.TimerInstant)))
        }
        val secondsCount = if (precise) 20 else 15
        for (i in 1..secondsCount) {
            ttlOptions.add(TTLOption(i, Lang.plural(R.string.xSeconds, i.toLong())))
        }

        if (precise) {
            var i = secondsCount + 5
            while (i < 60) {
                ttlOptions.add(TTLOption(i, Lang.plural(R.string.xSeconds, i.toLong())))
                i += 5
            }
            ttlOptions.add(TTLOption(60, Lang.plural(R.string.xMinutes, 1)))
        } else {
            ttlOptions.add(TTLOption(30, Lang.plural(R.string.xSeconds, 30)))
            ttlOptions.add(TTLOption(60, Lang.plural(R.string.xMinutes, 1)))
            ttlOptions.add(TTLOption(3600, Lang.plural(R.string.xHours, 1)))
            ttlOptions.add(TTLOption(86400, Lang.plural(R.string.xDays, 1)))
            ttlOptions.add(TTLOption(604800, Lang.plural(R.string.xWeeks, 1)))
        }

        var i = 0
        var foundIndex = 0
        for (option in ttlOptions) {
            val isValid: Boolean
            if (currentSelfDestructType == null) {
                isValid = option.isOff
            } else {
                when (currentSelfDestructType.getConstructor()) {
                    TdApi.MessageSelfDestructTypeImmediately.CONSTRUCTOR -> isValid = option.isImmediate
                    MessageSelfDestructTypeTimer.CONSTRUCTOR -> isValid =
                        option.ttlTime == (currentSelfDestructType as MessageSelfDestructTypeTimer).selfDestructTime

                    else -> {
                        assertMessageSelfDestructType_58882d8c()
                        throw unsupported(currentSelfDestructType)
                    }
                }
            }
            if (isValid) {
                foundIndex = i
                break
            }
            i++
        }

        val infiniteView = InfiniteRecyclerView<TTLOption>(context, true)
        if (useDarkMode) {
            infiniteView.forceDarkMode()
        }
        infiniteView.initWithItems(ttlOptions, foundIndex)

        val builder = AlertDialog.Builder(context, if (useDarkMode) R.style.DialogThemeDark else Theme.dialogTheme())
        builder.setTitle(Lang.getString(R.string.MessageLifetime))
        if (message != 0) {
            builder.setMessage(Lang.getString(message))
        }
        builder.setPositiveButton(Lang.getString(R.string.Done), DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
            val option = infiniteView.getCurrentItem()
            if (option != null && callback != null) {
                callback.runWithData(option)
            }
        })
        builder.setView(infiniteView)
        UI.getContext(context).showAlert(builder, if (useDarkMode) ThemeSet.getBuiltinTheme(ThemeId.NIGHT_BLACK) else null)
    }

    /*public static void showMuteMore (ViewController<?> c, int buttonIndex) {
    String[] strings = {
      Lang.getString(R.string.Enable),
      Lang.plural(R.string.MuteForXHours, 1),
      Lang.plural(R.string.MuteForXHours, 8),
      Lang.plural(R.string.MuteForXDays, 2),
      Lang.getString(R.string.Disable)
    };
    c.showMore(new int[] {R.id.btn_menu_enable, R.id.btn_menu_1hour, R.id.btn_menu_8hours, R.id.btn_menu_2days, R.id.btn_menu_disable}, strings, buttonIndex);
  }*/
    fun toggleMute(context: ViewController<*>, chatId: Long, allowCustomize: Boolean, after: Runnable?) {
        val chat = tdlib.chat(chatId)
        val scopeSettings = tdlib.scopeNotificationSettings(chatId)
        if (chat == null || scopeSettings == null) return
        if (scopeSettings.muteFor == 0 && !chat.notificationSettings.useDefaultMuteFor && chat.notificationSettings.muteFor > 0 && !allowCustomize) {
            // Unmuting chat while notifications are enabled globally
            tdlib.setMuteFor(chat.id, 0)
            U.run(after)
        } else {
            showMuteOptions(context, chatId, allowCustomize, after)
        }
    }

    fun showMuteOptions(context: ViewController<*>, chatId: Long, allowCustomize: Boolean, after: Runnable?) {
        showMuteOptions(context, null, chatId, allowCustomize, after)
    }

    fun showMuteOptions(context: ViewController<*>, scope: NotificationSettingsScope?, allowCustomize: Boolean) {
        showMuteOptions(context, scope, 0, allowCustomize, null)
    }

    private fun showMuteOptions(context: ViewController<*>, scope: NotificationSettingsScope?, chatId: Long, allowCustomize: Boolean, after: Runnable?) {
        require(!((scope == null && chatId == 0L) || (scope != null && chatId != 0L)))

        val capacity = 5
        val strings = StringList(capacity)
        val icons = IntList(capacity)
        val ids = IntList(capacity)
        var colors: IntList? = null
        var info: String? = null
        val areBlocked: Boolean

        if (scope != null) {
            areBlocked = tdlib.notifications().areNotificationsBlocked(scope)
            if (areBlocked) colors = IntList(capacity)
            val muteFor = tdlib.scopeMuteFor(scope)
            val mutedForever = TD.isMutedForever(muteFor)
            fillMuteOptions(ids, icons, strings, colors, muteFor > 0, !mutedForever, !mutedForever, allowCustomize, false, null, areBlocked)
        } else {
            val chat = tdlib.chat(chatId)
            val scopeSettings = tdlib.scopeNotificationSettings(chatId)
            if (chat == null || scopeSettings == null) {
                Log.e("Can't open chat settings: hasChat:%b hasScope:%b", chat != null, scopeSettings != null)
                return
            }
            areBlocked = tdlib.notifications().areNotificationsBlocked(chatId, true)
            if (areBlocked) colors = IntList(capacity)
            if (chat.notificationSettings.useDefaultMuteFor) {
                if (scopeSettings.muteFor == 0) { // Muting chat while notifications are enabled globally
                    fillMuteOptions(ids, icons, strings, colors, false, true, true, allowCustomize, false, null, areBlocked)
                } else { // Unmuting chat while notifications are disabled globally
                    fillMuteOptions(ids, icons, strings, colors, true, false, false, allowCustomize, false, null, areBlocked)
                    info = Lang.getString(R.string.NotificationsEnableOverride, Lang.lowercase(getValueForSettings(scopeSettings.muteFor)))
                }
            } else if (chat.notificationSettings.muteFor > 0) {
                if (scopeSettings.muteFor == 0) { // Unmuting chat while notifications are enabled globally
                    fillMuteOptions(ids, icons, strings, colors, true, false, true, allowCustomize, false, null, areBlocked)
                } else { // Unmuting chat while notifications are disabled globally
                    fillMuteOptions(ids, icons, strings, colors, true, false, false, allowCustomize, false, null, areBlocked)
                    info = Lang.getString(R.string.NotificationsEnableOverride, Lang.lowercase(getValueForSettings(scopeSettings.muteFor)))
                }
            } else {
                if (scopeSettings.muteFor == 0) { // Muting chat while notifications are enabled globally
                    fillMuteOptions(ids, icons, strings, colors, true, true, true, allowCustomize, true, null, areBlocked)
                    // getValueForSettings(scopeSettings.muteFor).toLowerCase()
                    info = Lang.getString(R.string.NotificationsDefaultInfo)
                } else { // Muting chat while notifications are disabled globally
                    fillMuteOptions(
                        ids,
                        icons,
                        strings,
                        colors,
                        false,
                        !TD.isMutedForever(scopeSettings.muteFor),
                        true,
                        allowCustomize,
                        false,
                        getValueForSettings(scopeSettings.muteFor, true),
                        areBlocked
                    )
                    info = Lang.getString(R.string.NotificationsDefaultInfo)
                }
            }
        }
        context.showOptions(
            info,
            ids.get(),
            strings.get(),
            if (colors != null) colors.get() else null,
            icons.get(),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_menu_customize) {
                    val notificationController = SettingsNotificationController(context.context(), tdlib)
                    if (scope != null) {
                        notificationController.setArguments(SettingsNotificationController.Args(scope))
                    } else {
                        notificationController.setArguments(SettingsNotificationController.Args(chatId))
                    }
                    context.navigateTo(notificationController)
                    U.run(after)
                    return@OptionDelegate true
                }
                if (chatId != 0L) {
                    if (id == R.id.btn_menu_resetToDefault) {
                        val chat = tdlib.chatStrict(chatId)
                        chat.notificationSettings.useDefaultMuteFor = true
                        tdlib.setChatNotificationSettings(chatId, chat.notificationSettings)
                        U.run(after)
                        return@OptionDelegate true
                    }
                }
                val muteFor: Int = getMuteDurationForId(id)
                if (scope != null) {
                    tdlib.setScopeMuteFor(scope, muteFor)
                } else {
                    tdlib.setMuteFor(chatId, muteFor)
                }
                U.run(after)
                true
            })
    }

    fun getIconForSetting(chat: Chat?): Int {
        val muteFor = tdlib.chatMuteFor(chat)
        return if (muteFor >= MUTE_MAX) R.drawable.baseline_notifications_off_24 else if (muteFor > 0) R.drawable.baseline_notifications_paused_24 else R.drawable.baseline_notifications_24
    }

    fun setValueForSetting(view: SettingView, scope: NotificationSettingsScope, allowStyle: Boolean) {
        val areBlocked = tdlib.notifications().areNotificationsBlocked(scope)
        if (areBlocked) {
            view.setDataColorId(ColorId.textNegative)
            view.setData(R.string.NotificationsBlocked)
        } else {
            val settings = tdlib.notifications().getScopeNotificationSettings(scope)
            view.setDataColorId(0)
            if (settings == null) {
                view.setData(Lang.getString(R.string.LoadingInformation))
            } else {
                if (allowStyle && settings.muteFor == 0) {
                    val importance = tdlib.notifications().getDefaultPriorityOrImportance(scope)
                    if (importance < NotificationManager.IMPORTANCE_DEFAULT) {
                        val stringRes: Int = getPriorityOrImportanceString(
                            importance,
                            tdlib.notifications().isDefaultSoundEnabled(scope),
                            tdlib.notifications().isDefaultVibrateModeEnabled(scope)
                        )
                        view.setData(Lang.getString(R.string.NotificationsEnabledHint, Lang.lowercase(Lang.getString(stringRes))))
                        return
                    }
                }
                view.setData(getValueForSettings(settings.muteFor))
            }
        }
    }

    fun setValueForSetting(view: SettingView, chatId: Long) {
        val areBlocked = tdlib.notifications().areNotificationsBlocked(chatId, true)
        if (areBlocked) {
            view.setDataColorId(ColorId.textNegative)
            view.setData(R.string.NotificationsBlocked)
        } else {
            val settings = tdlib.chatSettings(chatId)
            view.setDataColorId(0)
            if (settings != null) {
                view.setData(getValueForSettings(tdlib.chatMuteFor(chatId)))
            } else {
                view.setData(R.string.LoadingInformation)
            }
        }
    }

    fun unmute(chatId: Long) {
        tdlib.setMuteFor(chatId, 0)
    }

    // Profile helper
    fun handleProfileClick(context: ViewController<*>, view: View?, @IdRes id: Int, user: TdApi.User?, allowChangeNumber: Boolean): Boolean {
        if (user == null) {
            return false
        }
        if (id == R.id.btn_username) {
            val ids = IntList(4)
            val icons = IntList(4)
            val strings = StringList(4)

            val canEdit: Boolean

            if (tdlib.isSelfUserId(user.id)) {
                if (!user.hasUsername()) {
                    context.navigationController()!!.navigateTo(EditUsernameController(context.context(), context.tdlib()))
                    return true
                }

                ids.append(R.id.btn_username_edit)
                icons.append(R.drawable.baseline_edit_24)
                strings.append(R.string.EditUsername)

                canEdit = true
            } else {
                canEdit = false
            }

            /*ids.append(R.id.btn_username_copy);
        icons.append(R.drawable.ic_copy_gray);
        strings.append(R.string.Copy);*/
            ids.append(R.id.btn_username_copy_link)
            icons.append(R.drawable.baseline_link_24)
            strings.append(R.string.CopyLink)

            ids.append(R.id.btn_username_share)
            icons.append(R.drawable.baseline_forward_24)
            strings.append(R.string.ShareLink)

            context.showOptions(if (canEdit) null else "@" + user.primaryUsername(), ids.get(), strings.get(), null, icons.get())

            return true
        } else if (id == R.id.btn_phone) {
            if (!isEmpty(user.phoneNumber)) {
                val size = if (allowChangeNumber) 4 else 3
                val ids = IntList(size)
                val icons = IntList(size)
                val strings = StringList(size)

                if (allowChangeNumber) {
                    ids.append(R.id.btn_changePhoneNumber)
                    icons.append(R.drawable.baseline_edit_24)
                    strings.append(R.string.PhoneNumberChange)
                }

                ids.append(R.id.btn_phone_call)
                icons.append(R.drawable.baseline_phone_24)
                strings.append(R.string.Call)

                ids.append(R.id.btn_phone_copy)
                icons.append(R.drawable.baseline_content_copy_24)
                strings.append(R.string.Copy)

                ids.append(R.id.btn_phone_share)
                icons.append(R.drawable.baseline_forward_24)
                strings.append(R.string.ShareContact)

                val phoneNumber = Strings.formatPhone(user.phoneNumber)
                var message: CharSequence?
                if (allowChangeNumber) {
                    message = phoneNumber
                } else {
                    message = getLinkText(user)
                    if (message is SpannableStringBuilder) {
                        message.append("\n\n").append(phoneNumber)
                    } else if (!isEmpty(message)) {
                        message = message.toString() + "\n\n" + phoneNumber
                    } else {
                        message = phoneNumber
                    }
                }
                context.showOptions(message, ids.get(), strings.get(), null, icons.get())
            } else {
                if (!allowChangeNumber) {
                    val text = getLinkText(user)
                    if (text != null) {
                        if (view != null) {
                            context.context().tooltipManager().builder(view).show(context, tdlib, R.drawable.baseline_info_24, text)
                        } else {
                            UI.showToast(text, Toast.LENGTH_SHORT)
                        }
                    }
                }
            }
            return true
        } else if (id == R.id.btn_addToGroup) {
            addToGroup(context, user.id)
            return true
        }
        return false
    }

    private fun getLinkText(user: TdApi.User): CharSequence? {
        if (user.isMutualContact) {
            return Lang.getStringBold(if (TD.hasPhoneNumber(user)) R.string.ContactStateMutual else R.string.ContactStateMutualNoPhone, TD.getUserName(user))
        } else if (user.isContact) {
            return Lang.getStringBold(
                if (TD.hasPhoneNumber(user)) R.string.ContactStateOutgoing else R.string.ContactStateOutgoingNoPhone,
                TD.getUserName(user)
            )
        } else {
            return null
        }
    }

    fun handleProfileOption(context: ViewController<*>, @IdRes id: Int, user: TdApi.User?): Boolean {
        if (user == null) {
            return false
        }
        if (id == R.id.btn_username_edit) {
            context.navigationController()!!.navigateTo(EditUsernameController(context.context(), context.tdlib()))
            return true
        } else if (id == R.id.btn_username_copy) {
            UI.copyText('@'.toString() + user.primaryUsername(), R.string.CopiedUsername)
            return true
        } else if (id == R.id.btn_username_copy_link) {
            UI.copyText(context.tdlib()!!.tMeUrl(user.usernames!!), R.string.CopiedLink)
            return true
        } else if (id == R.id.btn_username_share) {
            shareUsername(context, user)
            return true
        } else if (id == R.id.btn_phone_share) {
            shareUser(context, user)
            return true
        } else if (id == R.id.btn_phone_copy) {
            UI.copyText('+'.toString() + user.phoneNumber, R.string.copied_phone)
            return true
        } else if (id == R.id.btn_phone_call) {
            Intents.openNumber('+'.toString() + user.phoneNumber)
            return true
        } else if (id == R.id.btn_changePhoneNumber) {
            context.navigationController()!!.navigateTo(SettingsPhoneController(context.context(), context.tdlib()))
            return true
        }
        return false
    }

    fun handleProfileMore(context: ViewController<*>, @IdRes id: Int, user: TdApi.User?, userFull: UserFullInfo?): Boolean {
        if (id == R.id.more_btn_edit) {
            if (user != null) {
                val c = EditNameController(context.context(), context.tdlib())
                if (context.tdlib()!!.isSelfUserId(user.id)) {
                    c.setMode(EditNameController.Mode.RENAME_SELF)
                } else {
                    if (TD.canEditBot(user)) {
                        c.setMode(EditNameController.Mode.RENAME_BOT)
                    } else {
                        c.setMode(EditNameController.Mode.RENAME_CONTACT)
                    }
                    c.setUser(user)
                }
                context.navigationController()!!.navigateTo(c)
            }
            return true
        } else if (id == R.id.more_btn_addToContacts) {
            if (user != null) {
                addContact(context, user)
            }
            return true
        } else if (id == R.id.more_btn_logout) {
            logOut(context, true)
            return true
        } else if (id == R.id.more_btn_addToGroup) {
            addToGroup(context, user!!.id)
            return true
        } else if (id == R.id.more_btn_share) {
            shareUser(context, user!!)
            return true
        }
        return false
    }

    fun addContact(context: ViewController<*>, contact: Contact?) {
        if (contact == null) {
            return
        }
        val user = if (contact.userId != 0L) tdlib.cache().user(contact.userId) else null
        if (user != null) {
            addContact(context, user, contact.phoneNumber)
        } else {
            val controller = PhoneController(context.context(), context.tdlib())
            controller.setMode(PhoneController.MODE_ADD_CONTACT)
            controller.setInitialData(contact.phoneNumber, contact.firstName, contact.lastName)
            context.navigationController()!!.navigateTo(controller)
        }
    }

    fun addContact(context: ViewController<*>, user: TdApi.User?) {
        if (user != null) {
            addContact(context, user, if (isEmpty(user.phoneNumber)) null else user.phoneNumber)
        }
    }

    fun addContact(context: ViewController<*>, user: TdApi.User?, knownPhoneNumber: String?) {
        if (user == null) {
            return
        }
        val controller = EditNameController(context.context(), context.tdlib())
        controller.setMode(EditNameController.Mode.ADD_CONTACT)
        controller.setUser(user)
        controller.setKnownPhoneNumber(knownPhoneNumber)
        context.navigationController()!!.navigateTo(controller)

        /*if (!TD.hasPhoneNumber(user)) {

    } else {
      PhoneController controller = new PhoneController(context.context(), context.tdlib());
      controller.setMode(PhoneController.MODE_ADD_CONTACT);
      controller.setInitialData(user.id, user.phoneNumber, user.firstName, user.lastName);
      context.navigationController()!!.navigateTo(controller);
    }*/
    }

    fun addAccount(context: BaseActivity, allowConfirm: Boolean, isDebug: Boolean) {
        val accountId = TdlibManager.instance().newAccount(isDebug)
        if (accountId == TdlibAccount.NO_ID) {
            return
        }
        val c = PhoneController(context, TdlibManager.getTdlib(accountId))
        c.setIsAccountAdd(true)
        context.navigation().navigateTo(c)
    }

    private fun shareUsername(context: ViewController<*>, user: TdApi.User, username: String? = user.primaryUsername()) {
        if (isEmpty(username)) return
        val link = tdlib.tMeUrl(username)
        val text: String?
        val export: String?
        if (tdlib.isSelfUserId(user.id)) {
            text = Lang.getString(R.string.ShareTextMyLink2, link)
            export = Lang.getString(R.string.ShareTextMyLink, link)
        } else {
            val name = tdlib.cache().userName(user.id)
            text = Lang.getString(R.string.ShareTextProfileLink2, name, link)
            export = Lang.getString(R.string.ShareTextProfileLink, name, link)
        }
        val c = ShareController(context.context(), context.tdlib())
        c.setArguments(ShareController.Args(text).setShare(export, Lang.getString(R.string.ShareLink)))
        c.show()
    }

    private fun shareUser(context: ViewController<*>, user: TdApi.User) {
        val username = tdlib.cache().userUsername(user.id)
        val name = tdlib.cache().userName(user.id)
        val url = if (isEmpty(username)) null else tdlib.tMeUrl(username)
        if (TD.isBot(user)) {
            val text = Lang.getString(R.string.ShareTextLink, name, url)
            val c = ShareController(context.context(), context.tdlib())
            c.setArguments(ShareController.Args(text).setShare(Lang.getString(R.string.ShareTextBotLink, url), Lang.getString(R.string.ShareBtnBot)))
            c.show()
        } else {
            val c = ShareController(context.context(), context.tdlib())
            val args = ShareController.Args(user)
            c.setArguments(args)
            c.show()
        }
    }

    @JvmOverloads
    fun shareUrl(
        context: TdlibDelegate,
        url: String?,
        internalShareText: String? = null,
        externalShareText: String = url!!,
        externalShareButton: String? = null
    ) {
        if (!isEmpty(url)) {
            val c = ShareController(context.context(), context.tdlib())
            c.setArguments(
                ShareController.Args(if (!isEmpty(internalShareText)) internalShareText else url)
                    .setShare(externalShareText, externalShareButton)
            )
            c.show()
        }
    }

    fun shareProxyUrl(context: TdlibDelegate, url: String?) {
        shareUrl(
            context, url,
            Lang.getString(R.string.ShareTextProxyLink2, url),
            Lang.getString(R.string.ShareTextProxyLink, url),
            Lang.getString(R.string.ShareBtnProxy)
        )
    }

    fun shareLanguageUrl(context: TdlibDelegate, languagePackInfo: LanguagePackInfo) {
        val url = context.tdlib()!!.tMeLanguageUrl(languagePackInfo.id)
        val text = Lang.getString(R.string.ShareTextLanguageLink, languagePackInfo.name, url)
        val c = ShareController(context.context(), context.tdlib())
        c.setArguments(ShareController.Args(text).setShare(text, Lang.getString(R.string.ShareBtnLanguage)))
        c.show()
    }

    fun shareStickerSetUrl(context: TdlibDelegate, stickerSet: StickerSetInfo) {
        val link = tdlib.tMeStickerSetUrl(stickerSet.name)
        val title = stickerSet.title
        val c = ShareController(context.context(), context.tdlib())
        c.setArguments(
            ShareController.Args(Lang.getString(R.string.ShareTextStickerLink2, title, link))
                .setShare(Lang.getString(R.string.ShareTextStickerLink, title, link), Lang.getString(R.string.ShareBtnStickerSet))
        )
        c.show()
    }

    fun shareText(context: TdlibDelegate, text: String?) {
        val c = ShareController(context.context(), context.tdlib())
        c.setArguments(ShareController.Args(text).setExport(text))
        c.show()
    }

    // Picker users
    private fun addToGroup(context: TdlibDelegate, userId: Long) {
        if (TdlibManager.inBackgroundThread()) {
            tdlib.runOnUiThread(Runnable { addToGroup(context, userId) })
            return
        }
        val c = ChatsController(context.context(), context.tdlib())
        c.setArguments(ChatsController.Arguments(ChatFilter.groupsInviteFilter(tdlib), object : PickerDelegate {
            override fun onChatPicked(chat: Chat, onDone: Runnable?): Boolean {
                if (!tdlib.canInviteUsers(chat)) {
                    UI.showToast(R.string.YouCantInviteMembers, Toast.LENGTH_SHORT)
                    return false
                }
                if (chat.type.getConstructor() == TdApi.ChatTypePrivate.CONSTRUCTOR) {
                    tdlib.send<FailedToAddMembers?>(AddChatMember(chat.id, userId, 0), tdlib.errorHandler<FailedToAddMembers?>())
                } else if (chat.type.getConstructor() == TdApi.ChatTypeSupergroup.CONSTRUCTOR) {
                    tdlib.send<FailedToAddMembers?>(AddChatMembers(chat.id, longArrayOf(userId)), tdlib.errorHandler<FailedToAddMembers?>())
                } else {
                    return false
                }
                return true
            }

            override fun getTitleStringRes(): Int {
                return R.string.BotInvite
            }
        }))
        context.context().navigation().navigateTo(c)
    }

    fun addToGroup(context: TdlibDelegate, start: TGBotStart, isGame: Boolean) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post(Runnable { addToGroup(context, start, isGame) })
            return
        }
        val c = ChatsController(context.context(), context.tdlib())
        c.setArguments(ChatsController.Arguments(if (isGame) ChatFilter.gamesFilter(tdlib) else ChatFilter.groupsInviteFilter(tdlib), object : PickerDelegate {
            override fun onChatPicked(chat: Chat, onDone: Runnable): Boolean {
                if (!tdlib.canSendBasicMessage(chat)) {
                    UI.showToast(R.string.YouCantSendMessages, Toast.LENGTH_SHORT)
                    return false
                }
                if (!isGame && !tdlib.canInviteUsers(chat)) {
                    tdlib.client().send(GetChatMember(chat.id, MessageSenderUser(start.getUserId())), Client.ResultHandler { `object`: TdApi.Object? ->
                        if (`object`!!.getConstructor() == ChatMember.CONSTRUCTOR) {
                            val member = `object` as ChatMember
                            if (TD.isMember(member.status)) {
                                tdlib.ui().post(onDone)
                                return@ResultHandler
                            }
                        }
                        UI.showToast(R.string.YouCantInviteMembers, Toast.LENGTH_SHORT)
                    })
                    return false
                }
                return true
            }

            override fun getTitleStringRes(): Int {
                return if (isGame) R.string.GameInvite else R.string.BotInvite
            }

            override fun getShareItem(): Any {
                return start
            }
        }))
        context.context().navigation().navigateTo(c)
    }

    // Stickers
    private fun newStickerSetHandler(context: TdlibDelegate, openParameters: UrlOpenParameters?): Client.ResultHandler {
        val currentController = context.context().navigation().getCurrentStackItem()
        return Client.ResultHandler { `object`: TdApi.Object? ->
            context.tdlib()!!.ui().post(Runnable {
                if (currentController == null || currentController.isDestroyed()) {
                    return@Runnable
                }
                when (`object`!!.getConstructor()) {
                    TdApi.StickerSet.CONSTRUCTOR -> {
                        val stickerSet = `object` as TdApi.StickerSet
                        StickerSetWrap.showStickerSet(context, stickerSet)
                    }

                    StickerSetInfo.CONSTRUCTOR -> {
                        val stickerSetInfo = `object` as StickerSetInfo
                        StickerSetWrap.showStickerSet(context, stickerSetInfo)
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        showLinkTooltip(context.tdlib()!!, R.drawable.baseline_warning_24, TD.toErrorString(`object`), openParameters)
                    }
                }
            })
        }
    }

    fun showStickerSet(context: TdlibDelegate, name: String?, openParameters: UrlOpenParameters?) {
        // TODO progress
        tdlib.client().send(SearchStickerSet(name, false), newStickerSetHandler(context, openParameters))
    }

    fun showStickerSet(context: TdlibDelegate, setId: Long, openParameters: UrlOpenParameters?) {
        // TODO progress
        tdlib.client().send(GetStickerSet(setId), newStickerSetHandler(context, openParameters))
    }

    fun showStickerSets(context: TdlibDelegate, setIds: LongArray, isEmojiPacks: Boolean, openParameters: UrlOpenParameters?) {
        if (setIds.size == 1) {
            showStickerSet(context, setIds[0], openParameters)
        } else {
            StickerSetWrap.showStickerSets(context, setIds, isEmojiPacks)
        }
    }

    // Confirm phone
    fun confirmPhone(context: TdlibDelegate, info: AuthenticationCodeInfo?, phoneNumber: String?) {
        val c = PasswordController(context.context(), context.tdlib())
        c.setArguments(PasswordController.Args(PasswordController.MODE_CODE_PHONE_CONFIRM, info, Strings.formatPhone(phoneNumber)))
        context.context().navigation().navigateTo(c)
    }

    class ChatOpenParameters {
        var options: Int = 0
        var after: RunnableLong? = null
        var onDone: Runnable? = null
        var shareItem: Any? = null

        var highlightSet: Boolean = false
        var highlightMode: Int = 0
        var highlightMessageId: MessageId? = null

        var searchQuery: String? = null
        var foundMessage: MessageId? = null

        var urlOpenParameters: UrlOpenParameters? = null

        var chatList: ChatList? = null
        var inviteLink: String? = null
        var inviteLinkInfo: ChatInviteLinkInfo? = null
        var threadInfo: ThreadInfo? = null
        var messageTopicId: MessageTopic? = null
        var filter: SearchMessagesFilter? = null
        var videoChatOrLiveStreamInvitation: InternalLinkTypeVideoChat? = null
        var fillDraft: TdApi.FormattedText? = null

        internal fun onDone() {
            if (onDone != null) {
                onDone!!.run()
                onDone = null
            }
        }

        fun saveInstanceState(outState: Bundle, keyPrefix: String?): Boolean {
            if (after != null || onDone != null || shareItem != null) return false
            if (options != 0) outState.putInt(keyPrefix + "cp_options", options)
            if (highlightSet) outState.putBoolean(keyPrefix + "cp_highlightSet", highlightSet)
            if (highlightMode != 0) outState.putInt(keyPrefix + "cp_highlightMode", highlightMode)
            if (highlightMessageId != null) TD.saveMessageId(outState, keyPrefix + "cp_highlightMessageId", highlightMessageId)
            if (chatList != null) outState.putString(keyPrefix + "cp_chatList", TD.makeChatListKey(chatList))
            if (threadInfo != null) threadInfo!!.saveTo(outState, keyPrefix + "cp_messageThread")
            outState.put(keyPrefix + "cp_topicId", messageTopicId)
            outState.put(keyPrefix + "cp_filter", filter)
            return true
        }

        fun noOpen(): ChatOpenParameters {
            this.options = this.options or CHAT_OPTION_NO_OPEN
            return this
        }

        fun urlOpenParameters(urlOpenParameters: UrlOpenParameters?): ChatOpenParameters {
            this.urlOpenParameters = urlOpenParameters
            return this
        }

        fun inviteLink(inviteLink: String?, inviteLinkInfo: ChatInviteLinkInfo?): ChatOpenParameters {
            this.inviteLink = inviteLink
            this.inviteLinkInfo = inviteLinkInfo
            return this
        }

        fun fillDraft(fillDraft: TdApi.FormattedText?): ChatOpenParameters {
            this.fillDraft = fillDraft
            return this
        }

        fun chatList(chatList: ChatList?): ChatOpenParameters {
            this.chatList = chatList
            return this
        }

        fun messageThread(threadInfo: ThreadInfo?): ChatOpenParameters {
            require(!(threadInfo != null && hasFlag(options, CHAT_OPTION_SCHEDULED_MESSAGES)))
            this.threadInfo = threadInfo
            return this
        }

        fun searchFilter(filter: SearchMessagesFilter?): ChatOpenParameters {
            this.filter = filter
            return this
        }

        fun passcodeUnlocked(): ChatOpenParameters {
            this.options = this.options or CHAT_OPTION_PASSCODE_UNLOCKED
            return this
        }

        fun videoChatOrLiveStreamInvitation(videoChatOrLiveStreamInvitation: InternalLinkTypeVideoChat?): ChatOpenParameters {
            this.videoChatOrLiveStreamInvitation = videoChatOrLiveStreamInvitation
            return this
        }

        fun keepStack(): ChatOpenParameters {
            this.options = this.options or CHAT_OPTION_KEEP_STACK
            return this
        }

        fun forceMessagesView(): ChatOpenParameters {
            this.options = this.options or CHAT_OPTION_FORCE_MESSAGES_VIEW
            return this
        }

        fun messageTopicId(messageTopicId: MessageTopic?): ChatOpenParameters {
            this.messageTopicId = messageTopicId
            return this
        }

        fun scheduledOnly(): ChatOpenParameters {
            require(this.threadInfo == null)
            this.options = this.options or CHAT_OPTION_SCHEDULED_MESSAGES
            return this
        }

        fun removeDuplicates(): ChatOpenParameters {
            this.options = this.options or CHAT_OPTION_REMOVE_DUPLICATES
            return this
        }

        fun openProfileInCaseOfPrivateChat(): ChatOpenParameters {
            this.options = this.options or CHAT_OPTION_NEED_PRIVATE_PROFILE
            return this
        }

        fun openProfileInCaseOfDuplicateChat(): ChatOpenParameters {
            this.options = this.options or CHAT_OPTION_OPEN_PROFILE_IF_DUPLICATE
            return this
        }

        fun openDirectMessagesChat(): ChatOpenParameters {
            this.options = this.options or CHAT_OPTION_OPEN_DIRECT_MESSAGES_CHAT
            return this
        }

        fun after(after: RunnableLong?): ChatOpenParameters {
            this.after = after
            return this
        }

        fun onDone(onDone: Runnable?): ChatOpenParameters {
            this.onDone = onDone
            return this
        }

        fun shareItem(shareItem: Any?): ChatOpenParameters {
            this.shareItem = shareItem
            return this
        }

        fun ensureHighlightAvailable(): ChatOpenParameters {
            this.options = this.options or CHAT_OPTION_ENSURE_HIGHLIGHT_AVAILABILITY
            return this
        }

        fun highlightMessage(message: TdApi.Message): ChatOpenParameters {
            return highlightMessage(MessageId(message.chatId, message.id))
        }

        fun foundMessage(query: String?, message: TdApi.Message): ChatOpenParameters {
            this.foundMessage = MessageId(message.chatId, message.id)
            this.searchQuery = query
            return highlightMessage(foundMessage)
        }

        fun highlightMessage(highlightMessageId: MessageId?): ChatOpenParameters {
            this.highlightSet = true
            this.highlightMode = MessagesManager.HIGHLIGHT_MODE_NORMAL
            this.highlightMessageId = highlightMessageId
            return this
        }

        fun highlightMessage(highlightMode: Int, highlightMessageId: MessageId?): ChatOpenParameters {
            this.highlightSet = true
            this.highlightMode = highlightMode
            this.highlightMessageId = highlightMessageId
            return this
        }

        companion object {
            @JvmStatic
            fun restoreInstanceState(tdlib: Tdlib?, `in`: Bundle, keyPrefix: String?): ChatOpenParameters? {
                val params = ChatOpenParameters()
                params.options = `in`.getInt(keyPrefix + "cp_options", 0)
                params.highlightSet = `in`.getBoolean(keyPrefix + "cp_highlightSet", false)
                params.highlightMode = `in`.getInt(keyPrefix + "cp_highlightMode", 0)
                params.highlightMessageId = TD.restoreMessageId(`in`, keyPrefix + "cp_highlightMessageId")
                params.chatList = TD.chatListFromKey(`in`.getString(keyPrefix + "cp_chatList", null))
                val threadInfo = ThreadInfo.restoreFrom(tdlib, `in`, keyPrefix + "cp_messageThread")
                if (threadInfo === ThreadInfo.INVALID) return null
                params.threadInfo = threadInfo
                params.messageTopicId = `in`.restoreMessageTopic(keyPrefix + "cp_topicId")
                params.filter = `in`.restoreSearchMessagesFilter(keyPrefix + "cp_filter")
                return params
            }
        }
    }

    private fun showChatOpenError(createRequest: TdApi.Function<*>, error: TdApi.Error, parameters: ChatOpenParameters?) {
        if (!UI.inUiThread()) {
            tdlib.ui().post(Runnable {
                showChatOpenError(createRequest, error, parameters)
            })
            return
        }
        var message: CharSequence = TD.toErrorString(error)
        if (!isEmpty(message)) {
            when (error.message) {
                "USERNAME_NOT_OCCUPIED" -> {
                    if (createRequest.getConstructor() == SearchPublicChat.CONSTRUCTOR) {
                        message = Lang.getStringBold(R.string.UsernameNotOccupied, (createRequest as SearchPublicChat).username)
                    }
                }

                "INVITE_REQUEST_SENT" -> {
                    if (parameters != null && parameters.inviteLinkInfo != null && parameters.inviteLinkInfo!!.createsJoinRequest) {
                        message = Lang.getStringBold(
                            if (TD.isChannel(parameters.inviteLinkInfo!!.type)) R.string.RequestJoinChannelSent else R.string.RequestJoinGroupSent,
                            parameters.inviteLinkInfo!!.title
                        )
                    }
                }
            }
            showLinkTooltip(tdlib, R.drawable.baseline_error_24, message, if (parameters != null) parameters.urlOpenParameters else null)
        }
    }

    fun openChat(context: TdlibDelegate, senderId: MessageSender, openParameters: ChatOpenParameters?) {
        val chatId = senderId.getSenderId()
        val function: TdApi.Function<Chat?>
        when (senderId.getConstructor()) {
            MessageSenderUser.CONSTRUCTOR -> function = CreatePrivateChat((senderId as MessageSenderUser).userId, false)
            MessageSenderChat.CONSTRUCTOR -> function = GetChat((senderId as MessageSenderChat).chatId)
            else -> {
                assertMessageSender_439d4c9c()
                throw unsupported(senderId)
            }
        }
        openChat(context, chatId, function, openParameters)
    }

    private fun openChat(context: TdlibDelegate, chatId: Long, createRequest: TdApi.Function<*>, params: ChatOpenParameters?) {
        if (chatId != 0L) {
            val chat = tdlib.chat(chatId)
            if (chat != null) {
                openChat(context, chat, params)
                return
            }
        }
        // TODO progress
        tdlib.client().send(createRequest, Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                Chat.CONSTRUCTOR -> openChat(context, tdlib.objectToChat(`object`)!!, params)
                SupergroupFullInfo.CONSTRUCTOR -> openChat(context, (`object` as SupergroupFullInfo).linkedChatId, params)
                TdApi.Error.CONSTRUCTOR -> {
                    showChatOpenError(createRequest, `object` as TdApi.Error, params)
                    if (params != null) tdlib.ui().post(Runnable { params.onDone() })
                }
            }
        })
    }

    fun openChat(context: TdlibDelegate, chatFinal: Chat, params: ChatOpenParameters?) {
        if (params != null && params.highlightMessageId != null) {
            params.highlightMessageId = MessageId(chatFinal.id, params.highlightMessageId!!.messageId, params.highlightMessageId!!.otherMessageIds)
        }
        if (params != null && params.highlightSet && params.highlightMessageId != null && (params.options and CHAT_OPTION_ENSURE_HIGHLIGHT_AVAILABILITY) != 0) {
            // Checking if post is available
            val function: TdApi.Function<*>?
            if (params.highlightMessageId!!.otherMessageIds != null && params.highlightMessageId!!.otherMessageIds!!.size > 0) {
                function = GetMessages(
                    params.highlightMessageId!!.chatId,
                    params.highlightMessageId!!.otherMessageIds!!.addElement(params.highlightMessageId!!.messageId)
                )
            } else {
                function = GetMessage(params.highlightMessageId!!.chatId, params.highlightMessageId!!.messageId)
            }
            tdlib.client().send(function, Client.ResultHandler { `object`: TdApi.Object? ->
                var error = true
                when (`object`!!.getConstructor()) {
                    TdApi.Message.CONSTRUCTOR -> {
                        error = false
                    }

                    TdApi.Messages.CONSTRUCTOR -> {
                        val messages = (`object` as TdApi.Messages).messages
                        for (message in messages) {
                            if (message != null) {
                                error = false
                                break
                            }
                        }
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        Log.i("Message not found: %s", TD.toErrorString(`object`))
                    }
                }
                if (error) {
                    if (!(context is MessagesController && context.compareChat(chatFinal.id, if (params != null) params.threadInfo else null))) {
                        UI.showToast(if (TD.isChannel(chatFinal.type)) R.string.PostNotFound else R.string.MessageNotFound, Toast.LENGTH_SHORT)
                        params.options = params.options and CHAT_OPTION_ENSURE_HIGHLIGHT_AVAILABILITY.inv()
                        params.highlightSet = false
                        openChat(context, chatFinal, params)
                    } else {
                        tdlib.ui().post(Runnable {
                            showLinkTooltip(
                                context.tdlib()!!,
                                R.drawable.baseline_warning_24,
                                Lang.getString(if (TD.isChannel(chatFinal.type)) R.string.PostNotFound else R.string.MessageNotFound),
                                params.urlOpenParameters
                            )
                            params.onDone()
                        })
                    }
                } else {
                    params.options = params.options and CHAT_OPTION_ENSURE_HIGHLIGHT_AVAILABILITY.inv()
                    openChat(context, chatFinal, params)
                }
            })
            return
        }

        if (!UI.inUiThread()) {
            tdlib.runOnUiThread(Runnable { openChat(context, chatFinal, params) })
            return
        }

        val chat = tdlib.syncChat(chatFinal)
        if (chat == null) {
            Log.e("Unable to open chat %d: chat is no longer available in memory", chatFinal.id)
            if (params != null) params.onDone()
            return
        }

        val navigation = context.context().navigation()

        if ((params == null || (params.options and CHAT_OPTION_PASSCODE_UNLOCKED) == 0) && tdlib.hasPasscode(chat)) {
            val c = PasscodeController(context.context(), context.tdlib())
            c.setArguments(PasscodeController.Args(chat, tdlib.chatPasscode(chat), params))
            c.setPasscodeMode(PasscodeController.MODE_UNLOCK)
            if (navigation!!.isEmpty()) {
                navigation.initController(c)
                val main = MainController(context.context(), context.tdlib())
                main.getValue()
                navigation.getStack().insert(main, 0)
            } else {
                navigation.navigateTo(c)
            }
            if (params != null) params.onDone()
            return
        }

        val urlOpenParameters = if (params != null) params.urlOpenParameters else null

        val accessState = tdlib.chatAccessState(chat)
        if (accessState < Tdlib.CHAT_ACCESS_OK) {
            if (params != null && params.inviteLinkInfo != null && params.inviteLinkInfo!!.createsJoinRequest) {
                showLinkTooltip(
                    context.tdlib()!!,
                    R.drawable.baseline_warning_24,
                    Lang.getString(
                        if (TD.isChannel(params.inviteLinkInfo!!.type)) R.string.RequestJoinChannelSent else R.string.RequestJoinGroupSent,
                        params.inviteLinkInfo!!.title
                    ),
                    urlOpenParameters
                )
            } else {
                showAccessError(context, urlOpenParameters, accessState, tdlib.isChannel(chat.id))
            }
            if (params != null) params.onDone()
            return
        }

        val after = if (params != null) params.after else null
        val shareItem = if (params != null) params.shareItem else null
        val chatList = if (params != null) params.chatList else null
        val options = if (params != null) params.options else 0
        val onlyScheduled = (options and CHAT_OPTION_SCHEDULED_MESSAGES) != 0
        val voiceChatInvitation = if (params != null) params.videoChatOrLiveStreamInvitation else null
        val messageThread = if (params != null) params.threadInfo else null
        val messageTopicId = if (params != null) params.messageTopicId else null
        val filter = if (params != null) params.filter else null
        val referrer = if (params != null && !isEmpty(params.inviteLink)) Referrer(params.inviteLink) else null
        val forceDraft = if (params != null && !params.fillDraft.isEmpty()) params.fillDraft else null

        if ((options and CHAT_OPTION_NEED_PRIVATE_PROFILE) != 0 && TD.isPrivateChat(chat.type)) {
            openChatProfile(context, chat, messageThread, urlOpenParameters)
            params!!.onDone()
            return
        }

        if ((options and CHAT_OPTION_OPEN_DIRECT_MESSAGES_CHAT) != 0) {
            openDirectMessagesChat(context, chat, messageThread, urlOpenParameters, Runnable { params!!.onDone() })
            return
        }

        if ((options and CHAT_OPTION_NO_OPEN) != 0) {
            if (after != null) {
                after.runWithLong(chat.id)
            }
            params!!.onDone()
            return
        }

        val highlightMode: Int
        val highlightMessageId: MessageId?
        if (params != null && params.highlightSet) {
            highlightMode = params.highlightMode
            highlightMessageId = params.highlightMessageId
        } else {
            highlightMode = MessagesManager.getAnchorHighlightMode(tdlib.id(), chat, messageThread)
            highlightMessageId = MessagesManager.getAnchorMessageId(tdlib.id(), chat, messageThread, highlightMode)
        }

        val isSelfChat = tdlib.isSelfChat(chat.id)

        var doneOpen = false
        if (context is MessagesController && !context.inPreviewMode() && context.compareChat(chat.id, messageThread, onlyScheduled)) {
            var doneSomething = false
            if (highlightMode == MessagesManager.HIGHLIGHT_MODE_NORMAL && highlightMessageId != null) {
                context.highlightMessage(highlightMessageId, urlOpenParameters)
                doneSomething = true
            }
            if (shareItem != null) {
                context.shareItem(shareItem)
                doneSomething = true
            }
            if (voiceChatInvitation != null) {
                context.openVoiceChatInvitation(voiceChatInvitation)
                doneSomething = true
            }
            if (forceDraft != null) {
                context.fillDraft(forceDraft, true)
                doneSomething = true
            }

            if (hasFlag(options, CHAT_OPTION_OPEN_PROFILE_IF_DUPLICATE)) {
                openChatProfile(context, chat, messageThread, urlOpenParameters)
                doneSomething = true
            }

            if (!doneSomething) {
                // TODO animate header
                UI.forceVibrateError(context.context().getContentView())
            }
            doneOpen = true
        }
        if (!doneOpen && highlightMode == MessagesManager.HIGHLIGHT_MODE_NORMAL && highlightMessageId != null) {
            val c = context.context().navigation().getCurrentStackItem()
            if (c != null && c !== context && c.tdlib() === context.tdlib() && c is MessagesController && !c.inPreviewMode() && c.compareChat(
                    chat.id,
                    messageThread,
                    onlyScheduled
                )
            ) {
                c.highlightMessage(highlightMessageId, urlOpenParameters)
                doneOpen = true
            }
        }
        if (doneOpen) {
            if (after != null) {
                after.runWithLong(chat.id)
            }
            if (params != null) {
                params.onDone()
            }
            return
        }

        // Forum supergroups open the topic list instead of a flat message view, unless a specific
        // topic/message was requested or the caller forced the flat message view ("View as Messages").
        // Note: TDLib's chat.viewAsTopics is false for forum-tabs forums and for chats explicitly set
        // to "view as messages"; we gate on isForum so every forum exposes its topics (the topic list
        // is the only forum UI we provide), and the header offers an escape to the flat view.
        if (tdlib.isForum(chat.id)
            && messageThread == null && messageTopicId == null && highlightMode != MessagesManager.HIGHLIGHT_MODE_NORMAL && shareItem == null && voiceChatInvitation == null && forceDraft == null && filter == null && !onlyScheduled && (options and CHAT_OPTION_FORCE_MESSAGES_VIEW) == 0 && (options and CHAT_OPTION_OPEN_PROFILE_IF_DUPLICATE) == 0
        ) {
            val topicsController = TopicsController(context.context(), context.tdlib()!!)
            topicsController.setArguments(TopicsController.Args(chat.id))
            navigation!!.navigateTo(topicsController)
            if (after != null) {
                after.runWithLong(chat.id)
            }
            if (params != null) {
                params.onDone()
            }
            return
        }

        val controller: MessagesController

        // boolean reused = false;
        val current = context.context().navigation().getCurrentStackItem()

        if ((context.context() !is MainActivity) || context is MessagesController || isSelfChat || current === (context.context() as MainActivity).getMessagesController(
                tdlib,
                false
            )
        ) {
            controller = MessagesController(context.context(), context.tdlib())
        } else {
            var m = (context.context() as MainActivity).getMessagesController(tdlib, true)
            m.finishSelectMode(-2)
            var i = 0
            if (navigation != null) {
                val stack = navigation.getStack()
                for (c in stack.getAll()) {
                    if (c is MessagesController && c === m) {
                        if ((options and CHAT_OPTION_KEEP_STACK) != 0) {
                            m = MessagesController(context.context(), context.tdlib())
                        } else {
                            stack.remove(i)
                            m.destroy()
                        }
                        break
                    }
                    i++
                }
            }
            controller = m
        }

        controller.setShareItem(shareItem)
        if (params != null && (params.options and CHAT_OPTION_PASSCODE_UNLOCKED) != 0) {
            controller.addOneShotFocusListener(Runnable {
                val prevStackItem = controller.stackItemAt(controller.stackSize() - 2)
                if (prevStackItem is PasscodeController && prevStackItem.getChatId() == controller.getChatId()) {
                    controller.destroyStackItemAt(controller.stackSize() - 2)
                }
            })
        }

        val actor = Runnable {
            if (after != null) {
                after.runWithLong(chat.id)
            }
            context.tdlib()!!.context().changePreferredAccountId(context.tdlib()!!.id(), TdlibManager.SWITCH_REASON_CHAT_OPEN, RunnableBool { success: Boolean ->
                if (success) {
                    controller.setResetOnFocus()
                }
            })
        }
        controller.postOnAnimationReady(actor)

        val arguments: MessagesController.Arguments
        if (params != null && !isEmpty(params.searchQuery) && params.foundMessage != null) {
            arguments = MessagesController.Arguments(
                chatList,
                chat,
                messageThread,
                messageTopicId,
                highlightMessageId,
                highlightMode,
                filter,
                params.foundMessage,
                params.searchQuery
            )
        } else if (highlightMessageId != null) {
            arguments = MessagesController.Arguments(chatList, chat, messageThread, messageTopicId, highlightMessageId, highlightMode, filter)
        } else {
            arguments = MessagesController.Arguments(tdlib, chatList, chat, messageThread, messageTopicId, filter)
        }
        controller.setArguments(
            arguments
                .setScheduled(onlyScheduled)
                .referrer(referrer)
                .voiceChatInvitation(voiceChatInvitation)
                .fillDraft(forceDraft)
        )

        val view = controller.getValue()
        if (controller.context().isNavigationBusy()) {
            if (params != null) {
                params.onDone()
            }
            return
        }
        if (view?.parent != null) {
            (view.parent as ViewGroup).removeView(view)
        }

        if (navigation!!.isEmpty()) {
            navigation.initController(controller)
            val c = MainController(context.context(), context.tdlib())
            c.getValue()
            navigation.getStack().insert(c, 0)
        } else if (navigation.getStackSize() > 1 && (options and CHAT_OPTION_KEEP_STACK) == 0) {
            navigation.setControllerAnimated(controller, true, true)
        } else {
            navigation.navigateTo(controller)
        }
        if (params != null) {
            params.onDone()
        }
    }

    fun openDirectMessagesChat(context: TdlibDelegate, chat: Chat, threadInfo: ThreadInfo?, openParameters: UrlOpenParameters?, after: Runnable?) {
        if (TdlibManager.inBackgroundThread()) {
            tdlib.runOnUiThread(Runnable { openDirectMessagesChat(context, chat, threadInfo, openParameters, after) })
            return
        }
        val supergroup = tdlib.chatToSupergroup(chat.id)
        if (supergroup == null || !tdlib.hasDirectMessagesChat(chat.id)) {
            showAccessError(context, openParameters, Tdlib.CHAT_ACCESS_FAIL, false)
            if (after != null) {
                after.run()
            }
            return
        }
        tdlib.cache().supergroupFull(supergroup.id, RunnableData { supergroupFull: SupergroupFullInfo? ->
            if (supergroupFull == null || supergroupFull.directMessagesChatId == 0L) {
                showAccessError(context, openParameters, Tdlib.CHAT_ACCESS_FAIL, false)
            } else {
                openChat(context, supergroupFull.directMessagesChatId, ChatOpenParameters().keepStack().urlOpenParameters(openParameters))
            }
            if (after != null) {
                after.run()
            }
        })
    }

    fun openChatProfile(context: TdlibDelegate, chat: Chat, threadInfo: ThreadInfo?, openParameters: UrlOpenParameters?) {
        if (TdlibManager.inBackgroundThread()) {
            tdlib.runOnUiThread(Runnable { openChatProfile(context, chat, threadInfo, openParameters) })
            return
        }
        val accessState = tdlib.chatAccessState(chat)
        if (accessState < Tdlib.CHAT_ACCESS_OK) {
            showAccessError(context, openParameters, accessState, tdlib.isChannel(chat.id))
            return
        }
        if (context.context().isNavigationBusy()) {
            return
        }
        val navigation = context.context().navigation()
        val profileController = ProfileController(context.context(), context.tdlib())
        try {
            profileController.setArguments(ProfileController.Args(chat, threadInfo, false))
        } catch (t: Throwable) {
            Log.e("Unable to open profile", t)
            return
        }

        if (navigation.isEmpty()) {
            navigation.initController(profileController)
            val c = MainController(context.context(), context.tdlib())
            c.getValue()
            navigation.getStack().insert(c, 0)
        } else {
            val c = navigation.getCurrentStackItem()
            if (c is MessagesController && !tdlib.isSelfChat(chat) && c.headerChatId == chat.id && !c.inPreviewMode()) {
                profileController.setShareCustomHeaderView(true)
            } else if (c is ProfileController && c.isSameProfile(profileController)) {
                profileController.getValue()
                profileController.destroy()
                return
            }
            navigation.navigateTo(profileController)
        }
    }

    private fun openChatProfile(
        context: TdlibDelegate,
        chatId: Long,
        messageThread: ThreadInfo?,
        createRequest: TdApi.Function<*>?,
        openParameters: UrlOpenParameters?
    ) {
        val chat = tdlib.chat(chatId)
        if (chat != null) {
            openChatProfile(context, chat, messageThread, openParameters)
            return
        }
        // TODO progress
        tdlib.client().send(createRequest, Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                Chat.CONSTRUCTOR -> openChatProfile(context, tdlib.objectToChat(`object`)!!, messageThread, openParameters)
                TdApi.User.CONSTRUCTOR -> {
                    val userId = (`object` as TdApi.User).id
                    openChatProfile(context, fromUserId(userId), messageThread, CreatePrivateChat(userId, false), openParameters)
                }

                TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
            }
        })
    }

    fun openChat(context: TdlibDelegate, chatId: Long, params: ChatOpenParameters?) {
        openChat(context, chatId, GetChat(chatId), params)
    }

    fun openChatProfile(context: TdlibDelegate?, chatId: Long, messageThread: ThreadInfo?, openParameters: UrlOpenParameters?) {
        context ?: return
        openChatProfile(context, chatId, messageThread, GetChat(chatId), openParameters)
    }

    fun openPublicChat(context: TdlibDelegate, username: String, openParameters: UrlOpenParameters?) {
        openChat(context, 0, SearchPublicChat(username), ChatOpenParameters().urlOpenParameters(openParameters).keepStack().openProfileInCaseOfPrivateChat())
    }

    fun openDirectMessages(context: TdlibDelegate, username: String, openParameters: UrlOpenParameters?) {
        openChat(context, 0, SearchPublicChat(username), ChatOpenParameters().urlOpenParameters(openParameters).keepStack().openDirectMessagesChat())
    }

    fun openVideoChatOrLiveStream(context: TdlibDelegate, videoChatOrLiveStreamInvitation: InternalLinkTypeVideoChat, openParameters: UrlOpenParameters?) {
        openChat(
            context,
            0,
            SearchPublicChat(videoChatOrLiveStreamInvitation.chatUsername),
            ChatOpenParameters().urlOpenParameters(openParameters).videoChatOrLiveStreamInvitation(videoChatOrLiveStreamInvitation).keepStack()
                .openProfileInCaseOfPrivateChat()
        )
    }

    private fun joinGroupCall(context: TdlibDelegate?, inputGroupCall: InputGroupCall?, openParameters: UrlOpenParameters?) {
        // TODO join group call
        showLinkTooltip(tdlib, R.drawable.baseline_warning_24, Lang.getString(R.string.InternalUrlUnsupported), openParameters)
    }

    private fun startBot(context: TdlibDelegate, botUsername: String?, startArgument: String?, botMode: Int, openParameters: UrlOpenParameters?) {
        // TODO progress
        tdlib.client().send(SearchPublicChat(botUsername), Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                Chat.CONSTRUCTOR -> {
                    val chat = tdlib.objectToChat(`object`)!!
                    if (!tdlib.isBotChat(chat)) {
                        showLinkTooltip(context.tdlib()!!, R.drawable.baseline_warning_24, Lang.getStringBold(R.string.BotNotFound, botUsername), openParameters)
                        return@ResultHandler
                    }
                    val user = tdlib.chatUser(chat)
                    if (user == null || user.type.getConstructor() != TdApi.UserTypeBot.CONSTRUCTOR) {
                        showLinkTooltip(context.tdlib()!!, R.drawable.baseline_warning_24, Lang.getStringBold(R.string.BotNotFound, botUsername), openParameters)
                        return@ResultHandler
                    }
                    /* commented out because it's possible that bot is a member of the selected group
          if (addToGroup && !((TdApi.UserTypeBot) user.type).canJoinGroups) {
            UI.showToast(UI.getString(R.string.BotNotFound, botUsername), Toast.LENGTH_SHORT);
            return;
          }*/
                    when (botMode) {
                        BOT_MODE_START -> {
                            openChat(
                                context,
                                chat,
                                ChatOpenParameters().urlOpenParameters(openParameters).shareItem(
                                    TGBotStart(
                                        user.id,
                                        startArgument,
                                        false,
                                        openParameters != null && openParameters.ignoreExplicitUserInteraction
                                    )
                                ).keepStack()
                            )
                        }

                        BOT_MODE_START_IN_GROUP, BOT_MODE_START_GAME -> {
                            val isGame = botMode == BOT_MODE_START_GAME
                            addToGroup(
                                context,
                                TGBotStart(user.id, startArgument, isGame, openParameters != null && openParameters.ignoreExplicitUserInteraction),
                                isGame
                            )
                        }
                    }
                }

                TdApi.Error.CONSTRUCTOR -> showLinkTooltip(
                    context.tdlib()!!,
                    R.drawable.baseline_warning_24,
                    Lang.getStringBold(R.string.BotNotFound, botUsername),
                    openParameters
                )
            }
        })
    }

    fun openPublicMessage(context: TdlibDelegate, username: String, messageId: MessageId?, urlOpenParameters: UrlOpenParameters?) {
        openChat(
            context,
            0,
            SearchPublicChat(username),
            ChatOpenParameters().keepStack().highlightMessage(messageId).ensureHighlightAvailable().urlOpenParameters(urlOpenParameters)
        )
    }

    fun openSupergroupMessage(context: TdlibDelegate, supergroupId: Long, messageId: MessageId?, urlOpenParameters: UrlOpenParameters?) {
        if (messageId != null) {
            openChat(
                context,
                0,
                CreateSupergroupChat(supergroupId, false),
                ChatOpenParameters().keepStack().highlightMessage(messageId).ensureHighlightAvailable().urlOpenParameters(urlOpenParameters)
            )
        } else {
            openChat(context, 0, CreateSupergroupChat(supergroupId, false), ChatOpenParameters().keepStack().urlOpenParameters(urlOpenParameters))
        }
    }

    fun openMessage(context: TdlibDelegate, message: TdApi.Message, openParameters: UrlOpenParameters?) {
        openMessage(context, message.chatId, MessageId(message.chatId, message.id), openParameters)
    }

    fun openMessage(context: TdlibDelegate?, chatId: Long, messageId: MessageId?, openParameters: UrlOpenParameters?) {
        context ?: return
        openMessage(context, chatId, messageId,  /* messageThread */null, openParameters)
    }

    fun openMessage(context: TdlibDelegate, chatId: Long, messageId: MessageId?, messageThread: ThreadInfo?, openParameters: UrlOpenParameters?) {
        openChat(
            context,
            chatId,
            ChatOpenParameters().keepStack().highlightMessage(messageId).ensureHighlightAvailable().messageThread(messageThread)
                .urlOpenParameters(openParameters)
        )
    }

    fun openMessage(context: TdlibDelegate, messageLink: MessageLinkInfo, openParameters: UrlOpenParameters?) {
        if (messageLink.message != null) {
            // TODO support for album, media timestamp, etc
            val messageId = MessageId(messageLink.message!!.chatId, messageLink.message!!.id)
            if (messageLink.topicId.messageThreadId() != 0L) {
                // FIXME TDLib/Server: need GetMessageThread alternative that accepts (chatId, messageThreadId)
                context.tdlib()!!.send<MessageThreadInfo?>(
                    GetMessageThread(messageId.chatId, messageId.messageId),
                    Tdlib.ResultHandler { messageThreadInfo: MessageThreadInfo?, error: TdApi.Error? ->
                        if (error != null) {
                            openMessage(context, messageLink.chatId, messageId, openParameters)
                        } else {
                            val messageThread = ThreadInfo.openedFromMessage(
                                context.tdlib()!!,
                                messageThreadInfo!!,
                                if (openParameters != null) openParameters.messageId else null
                            )
                            if (Config.SHOW_CHANNEL_POST_REPLY_INFO_IN_COMMENTS) {
                                val message = messageThread.getOldestMessage()
                                if (message != null && message.replyTo == null && message.forwardInfo != null && tdlib.isChannelAutoForward(message)) {
                                    tdlib.send<TdApi.Message?>(
                                        GetRepliedMessage(
                                            message.forwardInfo!!.source!!.chatId,
                                            message.forwardInfo!!.source!!.messageId
                                        ), Tdlib.ResultHandler { repliedMessage: TdApi.Message?, repliedMessageError: TdApi.Error? ->
                                            if (repliedMessage != null) {
                                                message.replyTo = MessageReplyToMessage(
                                                    repliedMessage.chatId,
                                                    repliedMessage.id,
                                                    null,
                                                    0,
                                                    "",
                                                    null,
                                                    repliedMessage.date,
                                                    repliedMessage.content
                                                )
                                            }
                                            openMessage(context, messageThread.getChatId(), messageId, messageThread, openParameters)
                                        })
                                    return@ResultHandler
                                }
                            }
                            openMessage(context, messageThread.getChatId(), messageId, messageThread, openParameters)
                        }
                    })
            } else {
                // TODO: properly handle messageLink.topicId
                openMessage(context, messageLink.chatId, messageId, openParameters)
            }
        } else {
            if (tdlib.chat(messageLink.chatId) != null) {
                UI.showToast(if (tdlib.isChannel(messageLink.chatId)) R.string.PostNotFound else R.string.MessageNotFound, Toast.LENGTH_SHORT)
            }
            openChat(context, messageLink.chatId, ChatOpenParameters().keepStack().urlOpenParameters(openParameters))
        }
    }

    fun openScheduledMessage(context: TdlibDelegate, message: TdApi.Message) {
        openScheduledMessage(context, message.chatId, MessageId(message.chatId, message.id))
    }

    fun openScheduledMessage(context: TdlibDelegate, chatId: Long, messageId: MessageId?) {
        openChat(context, chatId, ChatOpenParameters().keepStack().scheduledOnly().highlightMessage(messageId).ensureHighlightAvailable())
    }

    fun openPrivateChat(context: TdlibDelegate, userId: Long, openParameters: ChatOpenParameters?) {
        openChat(context, fromUserId(userId), CreatePrivateChat(userId, false), openParameters)
    }

    fun openPrivateProfile(context: TdlibDelegate?, userId: Long, openParameters: UrlOpenParameters?) {
        context ?: return
        openChatProfile(context, fromUserId(userId), null, CreatePrivateChat(userId, false), openParameters)
    }

    fun openSenderProfile(context: TdlibDelegate, senderId: MessageSender, openParameters: UrlOpenParameters?) {
        when (senderId.getConstructor()) {
            MessageSenderUser.CONSTRUCTOR -> openPrivateProfile(context, (senderId as MessageSenderUser).userId, openParameters)
            MessageSenderChat.CONSTRUCTOR -> openChatProfile(context, (senderId as MessageSenderChat).chatId, null, openParameters)
            else -> {
                assertMessageSender_439d4c9c()
                throw unsupported(senderId)
            }
        }
    }

    fun startSecretChat(context: TdlibDelegate, userId: Long, allowExisting: Boolean, params: ChatOpenParameters?) {
        // TODO open existing active secret chat if allowExisting == true
        // TODO progress
        tdlib.client().send(CreateNewSecretChat(userId), Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                Chat.CONSTRUCTOR -> openChat(context, tdlib.objectToChat(`object`)!!, params)
                TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
            }
        })
    }

    fun openSupergroupChat(context: TdlibDelegate, supergroupId: Long, params: ChatOpenParameters?) {
        openChat(context, fromSupergroupId(supergroupId), CreateSupergroupChat(supergroupId, false), params)
    }

    fun openLinkedChat(context: TdlibDelegate, supergroupId: Long, directMessages: Boolean, params: ChatOpenParameters?) {
        tdlib.send<SupergroupFullInfo?>(GetSupergroupFullInfo(supergroupId), Tdlib.ResultHandler { supergroupFull: SupergroupFullInfo?, error: TdApi.Error? ->
            if (supergroupFull != null) {
                val chatId = if (directMessages) supergroupFull.directMessagesChatId else supergroupFull.linkedChatId
                if (chatId == 0L) {
                    showLinkTooltip(
                        tdlib,
                        R.drawable.baseline_error_24,
                        Lang.getMarkdownString(context, R.string.LinkedChatNotFound),
                        if (params != null) params.urlOpenParameters else null
                    )
                } else {
                    openChat(context, chatId, params)
                }
            } else {
                showChatOpenError(GetSupergroupFullInfo(supergroupId), error!!, params)
                if (params != null) tdlib.ui().post(Runnable { params.onDone() })
            }
        })
    }

    fun openSupergroupProfile(context: TdlibDelegate, supergroupId: Long, openParameters: UrlOpenParameters?) {
        openChatProfile(context, fromSupergroupId(supergroupId), null, CreateSupergroupChat(supergroupId, false), openParameters)
    }

    fun openBasicGroupChat(context: TdlibDelegate, basicGroupId: Long, params: ChatOpenParameters?) {
        openChat(context, fromSupergroupId(basicGroupId), CreateBasicGroupChat(basicGroupId, false), params)
    }

    fun openBasicGroupProfile(context: TdlibDelegate, supergroupId: Long, openParameters: UrlOpenParameters?) {
        openChatProfile(context, fromSupergroupId(supergroupId), null, CreateSupergroupChat(supergroupId, false), openParameters)
    }

    fun upgradeBasicGroupChat(context: TdlibDelegate, chatId: Long) {
        openChat(context, 0, UpgradeBasicGroupChatToSupergroupChat(chatId), null)
    }

    // Open link
    private fun openJoinDialog(context: TdlibDelegate, inviteLink: String?, inviteLinkInfo: ChatInviteLinkInfo?, openParameters: UrlOpenParameters?) {
        if (TdlibManager.inBackgroundThread()) {
            tdlib.runOnUiThread(Runnable { openJoinDialog(context, inviteLink, inviteLinkInfo, openParameters) })
            return
        }
        val c = context.context().navigation().getCurrentStackItem()
        if (c != null) {
            ModernActionedLayout.showJoinDialog(c, inviteLinkInfo!!, Runnable { joinChatByInviteLink(context, inviteLink, inviteLinkInfo, openParameters) })
        }
        return
    }

    private fun checkChatInviteLink(context: TdlibDelegate, inviteLink: String?, openParameters: UrlOpenParameters?) {
        // TODO progress
        tdlib.client().send(CheckChatInviteLink(inviteLink), Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                ChatInviteLinkInfo.CONSTRUCTOR -> {
                    val inviteLinkInfo = `object` as ChatInviteLinkInfo
                    tdlib.traceInviteLink(inviteLinkInfo)
                    val accessState = tdlib.chatAccessState(inviteLinkInfo.chatId)
                    if (inviteLinkInfo.chatId == 0L || accessState == Tdlib.CHAT_ACCESS_PRIVATE) {
                        openJoinDialog(context, inviteLink, inviteLinkInfo, openParameters)
                    } else {
                        openChat(
                            context,
                            inviteLinkInfo.chatId,
                            ChatOpenParameters().urlOpenParameters(openParameters).inviteLink(inviteLink, inviteLinkInfo).keepStack().removeDuplicates()
                        )
                    }
                }

                TdApi.Error.CONSTRUCTOR -> {
                    showLinkTooltip(tdlib, R.drawable.baseline_error_24, TD.toErrorString(`object`), openParameters)
                }
            }
        })
    }

    private fun installLanguage(context: TdlibDelegate, languagePackId: String, openParameters: UrlOpenParameters?): Boolean {
        if (TD.isLocalLanguagePackId(languagePackId)) {
            Log.e("Attempt to install custom local languagePackId:%s", languagePackId)
            return true
        }
        // TODO progress
        tdlib.send<LanguagePackInfo?>(GetLanguagePackInfo(languagePackId), Tdlib.ResultHandler { info: LanguagePackInfo?, error: TdApi.Error? ->
            if (error != null) {
                showLinkTooltip(context.tdlib()!!, R.drawable.baseline_warning_24, TD.toErrorString(error), openParameters)
            } else {
                tdlib.ui().post(Runnable {
                    if (context.context().getActivityState() != UI.State.DESTROYED) {
                        showLanguageInstallPrompt(context, info!!)
                    }
                })
            }
        })
        return true
    }

    private fun joinChatByInviteLink(context: TdlibDelegate, inviteLink: String?, inviteLinkInfo: ChatInviteLinkInfo?, urlOpenParameters: UrlOpenParameters?) {
        openChat(
            context,
            0,
            JoinChatByInviteLink(inviteLink),
            ChatOpenParameters().urlOpenParameters(urlOpenParameters).inviteLink(inviteLink, inviteLinkInfo).keepStack().removeDuplicates()
        )
    }

    class UrlOpenParameters : MessageIdChangeListener {
        var instantViewMode: Int = INSTANT_VIEW_UNSPECIFIED
        var embedViewMode: Int = EMBED_VIEW_UNSPECIFIED
        var sourceLinkPreview: TdApi.LinkPreview? = null

        var messageId: MessageId? = null
        @JvmField
        var refererUrl: String? = null
        var instantViewFallbackUrl: String? = null
        var originalUrl: String? = null
        @JvmField
        var tooltip: TooltipBuilder? = null
        var requireOpenPrompt: Boolean = false
        var ignoreExplicitUserInteraction: Boolean = false
        var forceDirectMessagesChat: Boolean = false
        var openPromptCancellationCallback: Runnable? = null
        var displayUrl: String? = null

        private var parentController: ViewController<*>? = null
        internal var sourceMessage: TGMessage? = null

        constructor()

        constructor(options: UrlOpenParameters?) {
            if (options != null) {
                this.instantViewMode = options.instantViewMode
                this.embedViewMode = options.embedViewMode
                this.messageId = options.messageId
                this.refererUrl = options.refererUrl
                this.instantViewFallbackUrl = options.instantViewFallbackUrl
                this.tooltip = options.tooltip
                this.requireOpenPrompt = options.requireOpenPrompt
                this.ignoreExplicitUserInteraction = options.ignoreExplicitUserInteraction
                this.displayUrl = options.displayUrl
                this.parentController = options.parentController
                this.originalUrl = options.originalUrl
                this.sourceLinkPreview = options.sourceLinkPreview
                if (options.sourceMessage != null) {
                    sourceMessage(options.sourceMessage)
                }
            }
        }

        fun tooltip(b: TooltipBuilder?): UrlOpenParameters {
            this.tooltip = b
            if (b != null && parentController != null && !b.hasController()) {
                b.controller(parentController)
            }
            return this
        }

        fun controller(controller: ViewController<*>?): UrlOpenParameters {
            this.parentController = controller
            return this
        }

        fun sourceChat(chatId: Long): UrlOpenParameters? {
            if (this.sourceMessage != null) {
                check(this.sourceMessage!!.chatId == chatId)
                return this
            }
            return sourceMessage(MessageId(chatId, 0L))
        }

        fun displayUrl(displayUrl: String?): UrlOpenParameters {
            this.displayUrl = displayUrl
            return this
        }

        fun sourceMessage(messageId: MessageId?): UrlOpenParameters {
            this.messageId = messageId
            return this
        }

        fun sourceMessage(message: TGMessage?): UrlOpenParameters? {
            if (message != null) {
                controller(message.controller())
                if (message.isSponsoredMessage()) {
                    return this
                } else if (message.isSending) {
                    this.sourceMessage = message
                    message.getMessageIdChangeListeners().add(this)
                } else {
                    this.sourceMessage = null
                }
                return sourceMessage(MessageId(message.chatId, message.id, message.getOtherMessageIds(message.id)))
            } else {
                this.sourceMessage = null
                return sourceMessage(null as MessageId?)
            }
        }

        override fun onMessageIdChanged(msg: TGMessage?, oldMessageId: Long, newMessageId: Long, success: Boolean) {
            if (messageId != null && messageId!!.compareTo(msg!!.chatId, oldMessageId)) {
                messageId = messageId!!.replaceMessageId(oldMessageId, newMessageId)
                msg!!.getMessageIdChangeListeners().remove(this)
                this.sourceMessage = null
            }
        }

        @JvmOverloads
        fun requireOpenPrompt(requirePrompt: Boolean = true): UrlOpenParameters {
            this.requireOpenPrompt = requirePrompt
            return this
        }

        fun ignoreExplicitUserInteraction(ignoreExplicitUserInteraction: Boolean): UrlOpenParameters {
            this.ignoreExplicitUserInteraction = ignoreExplicitUserInteraction
            return this
        }

        fun disableOpenPrompt(): UrlOpenParameters {
            this.requireOpenPrompt = false
            return this
        }

        fun instantViewMode(instantViewMode: Int): UrlOpenParameters {
            this.instantViewMode = instantViewMode
            return this
        }

        fun forceInstantView(): UrlOpenParameters {
            return instantViewMode(INSTANT_VIEW_ENABLED)
        }

        fun disableInstantView(): UrlOpenParameters {
            return instantViewMode(INSTANT_VIEW_DISABLED)
        }

        fun embedViewMode(embedViewMode: Int): UrlOpenParameters {
            this.embedViewMode = embedViewMode
            return this
        }

        fun forceEmbedView(): UrlOpenParameters {
            return embedViewMode(EMBED_VIEW_ENABLED)
        }

        fun disableEmbedView(): UrlOpenParameters {
            return embedViewMode(EMBED_VIEW_DISABLED)
        }

        fun sourceLinkPreview(linkPreview: TdApi.LinkPreview?): UrlOpenParameters {
            this.sourceLinkPreview = linkPreview
            return this
        }

        fun referer(refererUrl: String): UrlOpenParameters {
            this.refererUrl = refererUrl
            return this
        }

        fun originalUrl(originalUrl: String): UrlOpenParameters {
            this.originalUrl = originalUrl
            return this
        }

        fun instantViewFallbackUrl(fallbackUrl: String): UrlOpenParameters {
            this.instantViewFallbackUrl = fallbackUrl
            return this
        }
    }

    fun openUrlOptions(context: ViewController<*>, url: String, options: UrlOpenParameters?) {
        context.showOptions(
            url,
            intArrayOf(R.id.btn_open, R.id.btn_copyLink),
            arrayOf<String?>(Lang.getString(R.string.Open), Lang.getString(R.string.CopyLink)),
            null,
            intArrayOf(R.drawable.baseline_open_in_browser_24, R.drawable.baseline_content_copy_24),
            OptionDelegate { v: View?, optionId: Int ->
                if (optionId == R.id.btn_open) {
                    openUrl(context, url, options)
                } else if (optionId == R.id.btn_copyLink) {
                    UI.copyText(url, R.string.CopiedLink)
                }
                true
            })
    }

    private fun openExternalUrl(context: TdlibDelegate, originalUrl: String, options: UrlOpenParameters?, after: RunnableBool?) {
        if (options != null && options.messageId != null && isSecret(options.messageId!!.chatId)) {
            openUrlImpl(context, originalUrl, options, after)
            return
        }
        tdlib.send<LoginUrlInfo?>(GetExternalLinkInfo(originalUrl), Tdlib.ResultHandler { loginUrlInfo: LoginUrlInfo?, error: TdApi.Error? ->
            if (error != null) {
                openUrlImpl(context, originalUrl, options, after)
                return@ResultHandler
            }
            when (loginUrlInfo!!.getConstructor()) {
                LoginUrlInfoOpen.CONSTRUCTOR -> {
                    val open = loginUrlInfo as LoginUrlInfoOpen
                    if (options != null) {
                        if (open.skipConfirmation) {
                            options.disableOpenPrompt()
                        }
                        options.displayUrl(originalUrl)
                    }
                    openUrlImpl(context, open.url, options, after)
                }

                LoginUrlInfoRequestConfirmation.CONSTRUCTOR -> {
                    val confirm = loginUrlInfo as LoginUrlInfoRequestConfirmation
                    val items: MutableList<ListItem?> = ArrayList<ListItem?>()
                    items.add(
                        ListItem(
                            ListItem.TYPE_CHECKBOX_OPTION_MULTILINE,
                            R.id.btn_signIn, 0,
                            Lang.getString(
                                R.string.LogInAsOn,
                                SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, argIndex: Int, needFakeBold: Boolean ->
                                    if (argIndex == 1) CustomTypefaceSpan(
                                        null,
                                        ColorId.textLink
                                    ) else Lang.newBoldSpan(needFakeBold)
                                },
                                context.tdlib()!!.accountName(),
                                confirm.domain
                            ),
                            true
                        )
                    )
                    if (confirm.requestWriteAccess) {
                        items.add(
                            ListItem(
                                ListItem.TYPE_CHECKBOX_OPTION_MULTILINE,
                                R.id.btn_allowWriteAccess,
                                0,
                                Lang.getString(R.string.AllowWriteAccess, Lang.boldCreator(), context.tdlib()!!.cache().userName(confirm.botUserId)),
                                true
                            )
                        )
                    }

                    val controller = if (context is ViewController<*>) context else context.context().navigation().getCurrentStackItem()
                    if (controller != null && !controller.isDestroyed()) {
                        controller.showSettings(
                            SettingsWrapBuilder(R.id.btn_open)
                                .addHeaderItem(
                                    Lang.getString(
                                        R.string.OpenLinkConfirm,
                                        SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, spanIndex: Int, needFakeBold: Boolean ->
                                            CustomTypefaceSpan(
                                                null,
                                                ColorId.textLink
                                            )
                                        },
                                        confirm.url
                                    )
                                )
                                .setRawItems(items)
                                .setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                                    val needSignIn = items.get(0)!!.isSelected()
                                    val needWriteAccess = items.size > 1 && items.get(1)!!.isSelected()
                                    if (needSignIn) {
                                        context.tdlib()!!.send<TdApi.HttpUrl?>(
                                            GetExternalLink(originalUrl, needWriteAccess),
                                            Tdlib.ResultHandler { httpUrl: TdApi.HttpUrl?, error1: TdApi.Error? ->
                                                val destinationUrl = if (error1 != null) originalUrl else httpUrl!!.url
                                                openUrlImpl(context, destinationUrl, if (options != null) options.disableOpenPrompt() else null, after)
                                            })
                                    } else {
                                        openUrlImpl(context, originalUrl, if (options != null) options.disableOpenPrompt() else null, after)
                                    }
                                })
                                .setSettingProcessor(CustomSettingProcessor { item: ListItem?, itemView: SettingView?, isUpdate: Boolean ->
                                    when (item!!.getViewType()) {
                                        ListItem.TYPE_CHECKBOX_OPTION, ListItem.TYPE_CHECKBOX_OPTION_MULTILINE, ListItem.TYPE_CHECKBOX_OPTION_WITH_AVATAR -> (itemView!!.getChildAt(
                                            0
                                        ) as CheckBoxView).setChecked(item.isSelected(), isUpdate)
                                    }
                                })
                                .setOnSettingItemClick(if (confirm.requestWriteAccess) object : ViewController.OnSettingItemClick { override fun onSettingItemClick(itemView: View?, settingsId: Int, item: ListItem?, doneButton: TextView?, settingsAdapter: SettingsAdapter?, window: PopupLayout?) {
                                    val itemId = item!!.getId()
                                    if (itemId == R.id.btn_signIn) {
                                        val needSignIn = settingsAdapter!!.getCheckIntResults().get(R.id.btn_signIn) == R.id.btn_signIn
                                        if (!needSignIn) {
                                            items.get(1)!!.setSelected(false)
                                            settingsAdapter.updateValuedSettingById(R.id.btn_allowWriteAccess)
                                        }
                                    } else if (itemId == R.id.btn_allowWriteAccess) {
                                        val needWriteAccess = settingsAdapter!!.getCheckIntResults().get(R.id.btn_allowWriteAccess) == R.id.btn_allowWriteAccess
                                        if (needWriteAccess) {
                                            items.get(0)!!.setSelected(true)
                                            settingsAdapter.updateValuedSettingById(R.id.btn_signIn)
                                        }
                                    }
                                }} else null)
                                .setSaveStr(R.string.Open)
                                .setRawItems(items)
                        )
                    } else {
                        if (after != null) {
                            after.runWithBool(false)
                        }
                    }
                }

                else -> {
                    assertLoginUrlInfo_7af29c11()
                    throw unsupported(loginUrlInfo)
                }
            }
        })
    }

    private fun openUrlImpl(context: TdlibDelegate, url: String, options: UrlOpenParameters?, after: RunnableBool?) {
        if (!UI.inUiThread()) {
            tdlib.ui().post(Runnable { openUrlImpl(context, url, options, after) })
            return
        }

        val uri = Strings.wrapHttps(url)

        if (options != null && options.requireOpenPrompt && (uri == null || !tdlib.isTrustedHost(url, true))) {
            val c = if (context is ViewController<*>) context else context.context().navigation().getCurrentStackItem()
            if (c != null && !c.isDestroyed()) {
                val b = AlertDialog.Builder(context.context(), Theme.dialogTheme())
                b.setTitle(Lang.getString(R.string.AppName))
                b.setMessage(Lang.getString(R.string.OpenThisLink, if (!isEmpty(options.displayUrl)) options.displayUrl else url))
                b.setPositiveButton(Lang.getString(R.string.Open), DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    tdlib.ui()
                        .openExternalUrl(context, url, options.disableOpenPrompt(), after)
                }
                )
                b.setNegativeButton(Lang.getString(R.string.Cancel), DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    if (options.openPromptCancellationCallback != null) {
                        options.openPromptCancellationCallback!!.run()
                    }
                    dialog!!.dismiss()
                })
                b.setOnCancelListener(DialogInterface.OnCancelListener { dialog: DialogInterface? ->
                    if (options.openPromptCancellationCallback != null) {
                        options.openPromptCancellationCallback!!.run()
                    }
                })
                c.showAlert(b)
            }
            return
        }

        if (uri == null) {
            val result = Intents.openLink(url)
            if (after != null) {
                after.runWithBool(result)
            }
            return
        }

        val isFromSecretChat = options != null && options.sourceMessage != null && isSecret(options.sourceMessage!!.chatId)

        val instantViewMode: Int
        val embedViewMode: Int
        if (isFromSecretChat && !Settings.instance().needSecretLinkPreviews()) {
            instantViewMode = INSTANT_VIEW_DISABLED
            embedViewMode = EMBED_VIEW_DISABLED
        } else {
            if (options == null || options.instantViewMode == INSTANT_VIEW_UNSPECIFIED) {
                var ok = false
                try {
                    val host = uri.getHost()
                    val path = uri.getPath()
                    if (!isEmpty(host) && path != null && path.length > 1) {
                        when (Settings.instance().instantViewMode) {
                            Settings.INSTANT_VIEW_MODE_INTERNAL -> ok = tdlib.isKnownHost(host, true)
                            Settings.INSTANT_VIEW_MODE_ALL -> ok = true
                        }
                    }
                } catch (t: Throwable) {
                    Log.i(t)
                }
                instantViewMode = if (ok) INSTANT_VIEW_ENABLED else INSTANT_VIEW_DISABLED
            } else {
                instantViewMode = options.instantViewMode
            }
            if (options == null || options.embedViewMode == EMBED_VIEW_UNSPECIFIED) {
                embedViewMode = if (Settings.instance().getNewSetting(Settings.SETTING_FLAG_NO_EMBEDS)) EMBED_VIEW_DISABLED else EMBED_VIEW_ENABLED
            } else {
                embedViewMode = options.embedViewMode
            }
        }

        val externalUrl = (if (options == null || isEmpty(options.instantViewFallbackUrl)) url else options.instantViewFallbackUrl)!!
        val uriFinal: Uri? = uri
        if (instantViewMode == INSTANT_VIEW_DISABLED && embedViewMode == EMBED_VIEW_DISABLED) {
            val result = Intents.openLink(url)
            if (after != null) {
                after.runWithBool(result)
            }
            return
        }

        if (embedViewMode == EMBED_VIEW_ENABLED && context is ViewController<*>) {
            val linkPreview = if (options != null) options.sourceLinkPreview else null
            if ((linkPreview != null && PreviewLayout.show(context, linkPreview, isFromSecretChat)) ||
                (linkPreview == null && PreviewLayout.show(context, url, isFromSecretChat))
            ) {
                if (after != null) {
                    after.runWithBool(true)
                }
                return
            }
        }

        val signal = AtomicBoolean()
        val foundWebPage = AtomicReference<TdApi.LinkPreview?>()
        val runnable = arrayOfNulls<CancellableRunnable>(1)

        tdlib.send<TdApi.LinkPreview?>(
            GetLinkPreview(TdApi.FormattedText(url, null), null),
            Tdlib.ResultHandler { linkPreview: TdApi.LinkPreview?, error: TdApi.Error? ->
                if (error != null) {
                    post(runnable[0]!!)
                    return@ResultHandler
                }
                foundWebPage.set(linkPreview)
                if (instantViewMode == INSTANT_VIEW_DISABLED || !TD.hasInstantView(linkPreview!!.instantViewVersion)) {
                    post(runnable[0]!!)
                    return@ResultHandler
                }
                tdlib.fetchInstantView(url, Tdlib.ResultHandler { instantView: WebPageInstantView?, instantViewError: TdApi.Error? ->
                    if (instantViewError != null || tdlib.isBadInstantView(instantView)) {
                        post(runnable[0]!!)
                        return@ResultHandler
                    }
                    post(Runnable {
                        if (!signal.getAndSet(true)) {
                            runnable[0]!!.cancel()

                            val controller = InstantViewController(context.context(), context.tdlib())
                            try {
                                controller.setArguments(InstantViewController.Args(linkPreview, instantView, Uri.parse(url).getEncodedFragment()))
                                controller.show()
                                if (after != null) {
                                    after.runWithBool(true)
                                }
                            } catch (t: Throwable) {
                                Log.e("Unable to open instantView, url:%s", t, url)
                                UI.showToast(R.string.InstantViewUnsupported, Toast.LENGTH_SHORT)
                                UI.openUrl(externalUrl)
                            }
                        }
                    })
                })
            })
        runnable[0] = object : CancellableRunnable() {
            public override fun act() {
                if (!signal.getAndSet(true)) {
                    if (options != null && !isEmpty(options.instantViewFallbackUrl) && (options.instantViewFallbackUrl != url) && (options.instantViewFallbackUrl != options.originalUrl)) {
                        openUrl(context, options.instantViewFallbackUrl!!, UrlOpenParameters(options).instantViewMode(INSTANT_VIEW_UNSPECIFIED), after)
                        return
                    }
                    if (tdlib.isKnownHost(uriFinal!!.getHost(), false)) {
                        val segments = uriFinal.getPathSegments()
                        if (segments != null && segments.size == 1 && "iv" == segments.get(0)) {
                            val originalUrl = uriFinal.getQueryParameter("url")
                            if (Strings.isValidLink(originalUrl)) {
                                openUrl(context, originalUrl!!, UrlOpenParameters(options).disableInstantView(), after)
                                return
                            }
                        }
                    }
                    if (embedViewMode == EMBED_VIEW_ENABLED) {
                        val linkPreview = foundWebPage.get()
                        if (context is ViewController<*> && linkPreview != null && PreviewLayout.show(context, linkPreview, isFromSecretChat)) {
                            if (after != null) {
                                after.runWithBool(true)
                            }
                            return
                        }
                    }
                    if (externalUrl != url && !(options != null && externalUrl == options.originalUrl)) {
                        openUrl(context, externalUrl, UrlOpenParameters(options).instantViewMode(INSTANT_VIEW_UNSPECIFIED), after)
                    } else {
                        val result = Intents.openLink(externalUrl)
                        if (after != null) {
                            after.runWithBool(result)
                        }
                    }
                }
            }
        }
        runnable[0]!!.removeOnCancel(UI.getAppHandler())
        UI.post(runnable[0], 2000)
    }

    @JvmOverloads
    fun openUrl(context: TdlibDelegate, url: String, options: UrlOpenParameters?, after: RunnableBool? = null) {
        openTelegramUrl(context, url, options, RunnableBool { processed: Boolean ->
            if (!processed) {
                openExternalUrl(context, url, options, after)
            } else {
                if (after != null) {
                    after.runWithBool(true)
                }
            }
        })
    }

    private fun parseTgScheme(context: TdlibDelegate?, originalUrl: String?, uri: Uri): String? {
        var command = uri.getHost()
        if (isEmpty(command)) {
            val schemeSpecificPart = uri.getSchemeSpecificPart()
            val i = schemeSpecificPart.indexOf('?')
            command = if (i == -1) schemeSpecificPart else schemeSpecificPart.substring(0, i)
        }

        val tMeUrl = tdlib.tMeUrl()

        when (command) {
            "join" -> {
                val inviteHash = uri.getQueryParameter("invite")
                if (isEmpty(inviteHash)) {
                    return null
                }
                return tMeUrl + "joinchat/" + inviteHash
            }

            "resolve" -> {
                val domain = uri.getQueryParameter("domain")
                if (isEmpty(domain)) {
                    return null
                }
                val postId = parseLong(uri.getQueryParameter("post"))
                if (postId != 0L) {
                    return tMeUrl + domain + "/" + postId
                }
                val start = uri.getQueryParameter("start")
                if (!isEmpty(start)) {
                    return tMeUrl + domain + "?start=" + start
                }
                val startGroup = uri.getQueryParameter("startgroup")
                if (!isEmpty(startGroup)) {
                    return tMeUrl + domain + "?startgroup=" + startGroup
                }
                return tMeUrl + domain
            }

            "addstickers" -> {
                val set = uri.getQueryParameter("set")
                if (!isEmpty(set)) {
                    return tMeUrl + "addstickers/" + set
                }
            }

            "bg" -> {
                // TODO

                /*https://t.me/bg/dedcd9 // solid
          tg://bg?color=dedcd9

          https://t.me/bg/Qe9IiLLfiVIBAAAAn_BDUKSYaCs // still
          tg://bg?slug=Qe9IiLLfiVIBAAAAn_BDUKSYaCs

          https://t.me/bg/fqv01SQemVIBAAAApND8LDRUhRU?intensity=47&bg_color=47e304 // pattern
          tg://bg?slug=fqv01SQemVIBAAAApND8LDRUhRU&bg_color=47e304&intensity=47

          https://t.me/bg/00c2ed-9200ed?rotation=45
          tg://bg?gradient=00c2ed-9200ed

          https://t.me/bg/fqv01SQemVIBAAAApND8LDRUhRU?intensity=99&bg_color=abcdef-000000&mode=motion&rotation=45
          tg://bg?slug=fqv01SQemVIBAAAApND8LDRUhRU&mode=motion&bg_color=abcdef-000000&intensity=99*/
                val slug = uri.getQueryParameter("slug")
                val bgColor = uri.getQueryParameter("bg_color")
                val intensity = uri.getQueryParameter("intensity")
            }

            "privatepost" -> {
                val supergroupId = parseLong(uri.getQueryParameter("channel"))
                val messageId = parseLong(uri.getQueryParameter("msg_id"))
                if (supergroupId != 0L) {
                    if (messageId != 0L) {
                        return tMeUrl + "c/" + supergroupId + "/" + messageId
                    } else {
                        return tMeUrl + "c/" + supergroupId
                    }
                }
            }

            "login" -> {
                val code = uri.getQueryParameter("code")
                if (!isEmpty(code)) {
                    return tMeUrl + "login/" + code
                }
            }

            "setlanguage" -> {
                val lang = uri.getQueryParameter("lang")
                if (!isEmpty(lang)) {
                    return tMeUrl + "setlanguage/" + lang
                }
            }

            "confirmphone" -> {
                val phone = uri.getQueryParameter("phone")
                val hash = uri.getQueryParameter("hash")
                if (!isEmpty(phone) && !isEmpty(hash)) {
                    return tMeUrl + "confirmphone?phone=" + phone + "&hash=" + hash
                }
            }

            "msg_url" -> {
                val url = uri.getQueryParameter("url")
                if (isEmpty(url)) {
                    return null
                }
                val text = uri.getQueryParameter("text")
                val b = StringBuilder(tMeUrl)
                b.append("share/url?url=").append(Uri.encode(url, "UTF-8"))
                if (!isEmpty(text)) {
                    b.append("&text=").append(Uri.encode(text, "UTF-8"))
                }
                return b.toString()
            }

            "socks" -> {
                val server = uri.getQueryParameter("server")
                if (isEmpty(server)) {
                    return null
                }
                val port = parseInt(uri.getQueryParameter("port"))
                val user = uri.getQueryParameter("user")
                val pass = uri.getQueryParameter("pass")
                val b = StringBuilder(tMeUrl)
                b.append("socks?server=").append(Uri.encode(server, "UTF-8"))
                b.append("&port=").append(port)
                if (!isEmpty(user)) {
                    b.append("&user=").append(Uri.encode(user, "UTF-8"))
                }
                if (!isEmpty(pass)) {
                    b.append("&pass=").append(Uri.encode(pass, "UTF-8"))
                }
                return b.toString()
            }

            "proxy" -> {
                val server = uri.getQueryParameter("server")
                if (isEmpty(server)) {
                    return null
                }
                val port = parseInt(uri.getQueryParameter("port"))
                val secret = uri.getQueryParameter("secret")
                val b = StringBuilder(tMeUrl)
                b.append("proxy?server=").append(Uri.encode(server, "UTF-8"))
                b.append("&port=").append(port)
                if (!isEmpty(secret)) {
                    b.append("&secret=").append(Uri.encode(secret, "UTF-8"))
                }
                return b.toString()
            }
        }
        if (context != null && originalUrl != null) {
            tdlib.client().send(GetDeepLinkInfo(originalUrl), Client.ResultHandler { result: TdApi.Object? ->
                when (result!!.getConstructor()) {
                    DeepLinkInfo.CONSTRUCTOR -> {
                        val info = result as DeepLinkInfo
                        tdlib.ui().post(Runnable {
                            val c = context.context().navigation().getCurrentStackItem()
                            if (c != null) {
                                c.processDeepLinkInfo(info)
                            }
                        })
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        UI.showToast(R.string.DeepLinkUnsupported, Toast.LENGTH_SHORT)
                    }
                }
            })
            return TME_LINK_PROCESSED
        }
        return null
    }

    fun getTelegramLinkType(url: String): Int {
        var url = url
        if (isEmpty(url)) {
            return TME_URL_NONE
        }

        url = url.replace("tg://", "tg:")
        if (url.startsWith("tg:") && !url.startsWith("tg://")) {
            url = "tg://" + url.substring("tg:".length)
        }

        var uri = Uri.parse(url)
        val scheme = uri.getScheme()
        if ("tg" == scheme) {
            url = parseTgScheme(null, null, uri)!!
            if (isEmpty(url)) {
                return TME_URL_NONE
            }
            uri = Uri.parse(url)
        } else if (isEmpty(scheme)) {
            url = "https://" + url
            uri = Uri.parse(url)
        }

        if (!tdlib.isKnownHost(url, false)) {
            return TME_URL_NONE
        }

        val segments = uri.getPathSegments()
        if (segments == null || segments.isEmpty()) {
            return TME_URL_NONE
        }
        val command: String? = segments.get(0)
        val pathArg = if (segments.size > 1) segments.get(1) else null

        val postId = parseLong(pathArg)

        if (!Strings.isValidLink(command) && postId != 0L) {
            return TME_URL_MESSAGE
        }

        return TME_URL_NONE
    }

    fun needViewInBrowser(url: String): Boolean {
        var url = url
        if (isEmpty(url)) return false

        if (TdlibManager.instance().inRecoveryMode()) return true

        url = url.replace("tg://", "tg:")
        if (url.startsWith("tg:") && !url.startsWith("tg://")) {
            url = "tg://" + url.substring("tg:".length)
        }

        var uri = Uri.parse(url)

        val scheme = uri.getScheme()
        if ("tg" == scheme) return false

        if (isEmpty(scheme)) uri = Uri.parse("https://" + uri.toString())

        if (!tdlib.isKnownHost(url, false)) {
            return false
        }

        val segments = uri.getPathSegments()
        if (segments != null && !segments.isEmpty()) {
            val command = segments.get(0)
            val pathArg1 = if (segments.size > 1) segments.get(1) else null
            when (command) {
                "s" -> return !isEmpty(pathArg1)
            }
        }

        return false
    }

    private fun preProcessTelegramUrl(url: String): String {
        try {
            val uri = wrapHttps(url)
            if (uri == null) {
                return url
            }
            var host = uri.getHost()
            // convert username.t.me/path?query to t.me/username/path?query
            val firstIndex = host!!.indexOf('.')
            if (firstIndex == -1) {
                return url
            }
            val subdomain = host.substring(0, firstIndex)
            host = host.substring(firstIndex + 1)
            if (!tdlib.isKnownHost(host, false)) {
                return url
            }
            val path = uri.getPath()
            val newPath = "/" + subdomain + (if (!isEmpty(path) && path != "/") path else "")
            val newUri = uri.buildUpon()
                .authority(host)
                .path(newPath)
                .build()
            return newUri.toString()
        } catch (t: Throwable) {
            Log.i("Unable to pre process url: %s", t, url)
        }
        return url
    }

    private fun parseTelegramUrl(url: String?): InternalLinkType? {
        return null
    }

    fun openTelegramUrl(context: TdlibDelegate, rawUrl: String, openParameters: UrlOpenParameters?, after: RunnableBool?) {
        if (isEmpty(rawUrl) || tdlib.context().inRecoveryMode()) {
            if (after != null) after.runWithBool(false)
            return
        }
        val url = AtomicReference<String?>(preProcessTelegramUrl(rawUrl))
        tdlib.send<InternalLinkType?>(GetInternalLinkType(url.get()), object : Tdlib.ResultHandler<InternalLinkType?> {
            override fun onResult(internalLinkType: InternalLinkType?, error: TdApi.Error?) {
                val currentUrl = url.get()
                val linkType: InternalLinkType?
                if (error != null) {
                    linkType = parseTelegramUrl(rawUrl)
                } else if (internalLinkType is InternalLinkTypeUnknownDeepLink) {
                    val parsedType = parseTelegramUrl(rawUrl)
                    linkType = if (parsedType != null) parsedType else internalLinkType
                } else {
                    linkType = internalLinkType
                }
                if ((linkType == null || internalLinkType is InternalLinkTypeUnknownDeepLink) && url.get() != rawUrl) {
                    url.set(rawUrl)
                    tdlib.send<InternalLinkType?>(GetInternalLinkType(url.get()), this)
                    return
                }
                if (linkType == null) {
                    if (after != null) {
                        post(Runnable { after.runWithBool(false) })
                    }
                    return
                }
                post(Runnable { openInternalLinkType(context, currentUrl, linkType, openParameters, after) }
                )
            }
        })
    }

    fun openInternalLinkType(
        context: TdlibDelegate,
        originalUrl: String?,
        linkType: InternalLinkType,
        openParameters: UrlOpenParameters?,
        after: RunnableBool?
    ) {
        if (!UI.inUiThread()) {
            post(Runnable { openInternalLinkType(context, originalUrl, linkType, openParameters, after) }
            )
            return
        }
        if (context.context().navigation().isDestroyed()) {
            if (after != null) {
                after.runWithBool(false)
            }
            return
        }
        val unsupported = Runnable {
            showLinkTooltip(tdlib, R.drawable.baseline_warning_24, Lang.getString(R.string.InternalUrlUnsupported), openParameters)
        }
        var ok = true
        when (linkType.getConstructor()) {
            InternalLinkTypeStickerSet.CONSTRUCTOR -> {
                val stickerSet = linkType as InternalLinkTypeStickerSet
                showStickerSet(context, stickerSet.stickerSetName, openParameters)
            }

            InternalLinkTypeAuthenticationCode.CONSTRUCTOR -> {
                val authCode = linkType as InternalLinkTypeAuthenticationCode
                tdlib.listeners().updateAuthorizationCodeReceived(authCode.code)
            }

            InternalLinkTypeLanguagePack.CONSTRUCTOR -> {
                val languagePack = linkType as InternalLinkTypeLanguagePack
                ok = installLanguage(context, languagePack.languagePackId, openParameters)
            }

            InternalLinkTypeChatInvite.CONSTRUCTOR -> {
                val chatInvite = linkType as InternalLinkTypeChatInvite
                checkChatInviteLink(context, chatInvite.inviteLink, openParameters)
            }

            InternalLinkTypeMessageDraft.CONSTRUCTOR -> {
                val messageDraft = linkType as InternalLinkTypeMessageDraft
                val c = ShareController(context.context(), context.tdlib())
                c.setArguments(ShareController.Args(messageDraft.text))
                c.show()
            }

            InternalLinkTypePhoneNumberConfirmation.CONSTRUCTOR -> {
                val confirmPhone = linkType as InternalLinkTypePhoneNumberConfirmation
                val authenticationSettings = context.tdlib()!!.phoneNumberAuthenticationSettings(context.context())
                // TODO progress?
                val currentController = context.context().navigation().getCurrentStackItem()
                tdlib.send<AuthenticationCodeInfo?>(
                    SendPhoneNumberCode(
                        confirmPhone.phoneNumber,
                        authenticationSettings,
                        PhoneNumberCodeTypeConfirmOwnership(confirmPhone.hash)
                    ), Tdlib.ResultHandler { authernticationCodeInfo: AuthenticationCodeInfo?, error: TdApi.Error? ->
                        if (error != null) {
                            showLinkTooltip(tdlib, R.drawable.baseline_warning_24, TD.toErrorString(error), openParameters)
                        } else {
                            post(Runnable {
                                if (currentController != null && !currentController.isDestroyed()) {
                                    confirmPhone(context, authernticationCodeInfo, confirmPhone.phoneNumber)
                                }
                            })
                        }
                    })
            }

            InternalLinkTypeProxy.CONSTRUCTOR -> {
                val proxy = (linkType as InternalLinkTypeProxy).proxy
                if (proxy != null) {
                    openProxyAlert(context, proxy)
                } else {
                    showLinkTooltip(tdlib, R.drawable.baseline_warning_24, Lang.getString(R.string.ProxyLinkUnsupported), openParameters)
                }
            }

            InternalLinkTypeUserPhoneNumber.CONSTRUCTOR -> {
                val phoneNumber = (linkType as InternalLinkTypeUserPhoneNumber).phoneNumber
                openChatProfile(context, 0, null, SearchUserByPhoneNumber(phoneNumber, false), openParameters)
            }

            InternalLinkTypeUserToken.CONSTRUCTOR -> {
                val token = (linkType as InternalLinkTypeUserToken).token
                openChatProfile(context, 0, null, SearchUserByToken(token), openParameters)
            }

            InternalLinkTypeGroupCall.CONSTRUCTOR -> {
                val groupCall = linkType as InternalLinkTypeGroupCall
                joinGroupCall(context, InputGroupCallLink(groupCall.inviteLink), openParameters)
            }

            InternalLinkTypeVideoChat.CONSTRUCTOR -> {
                val voiceChatInvitation = linkType as InternalLinkTypeVideoChat
                openVideoChatOrLiveStream(context, voiceChatInvitation, openParameters)
            }

            InternalLinkTypeMessage.CONSTRUCTOR -> {
                val messageLink = linkType as InternalLinkTypeMessage
                // TODO show progress?
                tdlib.client().send(GetMessageLinkInfo(messageLink.url), Client.ResultHandler { messageLinkResult: TdApi.Object? ->
                    when (messageLinkResult!!.getConstructor()) {
                        MessageLinkInfo.CONSTRUCTOR -> {
                            val messageLinkInfo = messageLinkResult as MessageLinkInfo
                            post(Runnable {
                                openMessage(context, messageLinkInfo, openParameters)
                                if (after != null) {
                                    after.runWithBool(true)
                                }
                            })
                        }

                        TdApi.Error.CONSTRUCTOR -> {
                            if (after != null) {
                                post(Runnable { after.runWithBool(false) })
                            }
                        }
                    }
                })
                return  // async
            }

            InternalLinkTypeBotStart.CONSTRUCTOR -> {
                val startBot = linkType as InternalLinkTypeBotStart
                startBot(context, startBot.botUsername, startBot.startParameter, BOT_MODE_START, openParameters)
            }

            InternalLinkTypeBotStartInGroup.CONSTRUCTOR -> {
                val startBot = linkType as InternalLinkTypeBotStartInGroup
                startBot(context, startBot.botUsername, startBot.startParameter, BOT_MODE_START_IN_GROUP, openParameters)
            }

            InternalLinkTypeBotAddToChannel.CONSTRUCTOR -> {
                val addToChannel = linkType as InternalLinkTypeBotAddToChannel
                // TODO add to channel flow
                showLinkTooltip(tdlib, R.drawable.baseline_warning_24, Lang.getString(R.string.InternalUrlUnsupported), openParameters)
            }

            InternalLinkTypeGame.CONSTRUCTOR -> {
                val game = linkType as InternalLinkTypeGame
                startBot(context, game.botUsername, game.gameShortName, BOT_MODE_START_GAME, openParameters)
            }

            InternalLinkTypeSettings.CONSTRUCTOR -> {
                val section = (linkType as InternalLinkTypeSettings).section
                var result: ViewController<*>? = null
                if (section != null) {
                    when (section.getConstructor()) {
                        SettingsSectionAppearance.CONSTRUCTOR -> {
                            val appearance = section as SettingsSectionAppearance
                            when (appearance.subsection) {
                                "wallpapers", "wallpapers/edit", "wallpapers/set", "wallpapers/choose-photo" -> {
                                    val c = MessagesController(context.context(), context.tdlib())
                                    c.setArguments(MessagesController.Arguments(MessagesController.PREVIEW_MODE_WALLPAPER, null, null))
                                    result = c
                                }

                                "text-size", "text-size/use-system" -> {
                                    val c = MessagesController(context.context(), context.tdlib())
                                    c.setArguments(MessagesController.Arguments(MessagesController.PREVIEW_MODE_FONT_SIZE, null, null))
                                    result = c
                                }

                                "your-color/profile", "your-color/profile/add-icons", "your-color/profile/use-gift", "your-color/profile/reset", "your-color/name", "your-color/name/add-icons", "your-color/name/use-gift", "app-icon", "tap-for-next-media" -> {
                                    // TODO
                                    unsupported.run()
                                }

                                "stickers-and-emoji", "stickers-and-emoji/edit", "stickers-and-emoji/trending", "stickers-and-emoji/archived", "stickers-and-emoji/archived/edit", "stickers-and-emoji/emoji", "stickers-and-emoji/emoji/edit", "stickers-and-emoji/emoji/archived", "stickers-and-emoji/emoji/archived/edit", "stickers-and-emoji/emoji/suggest", "stickers-and-emoji/emoji/quick-reaction", "stickers-and-emoji/emoji/quick-reaction/choose", "stickers-and-emoji/suggest-by-emoji", "stickers-and-emoji/large-emoji", "stickers-and-emoji/dynamic-order", "stickers-and-emoji/emoji/show-more" -> {
                                    result = SettingsStickersAndEmojiController(context.context(), context.tdlib())
                                }

                                "animations" -> {
                                    val c = SettingsThemeController(context.context(), context.tdlib())
                                    c.setArguments(SettingsThemeController.Args(SettingsThemeController.MODE_INTERFACE_OPTIONS))
                                    result = c
                                }

                                "themes", "themes/edit", "themes/create", "night-mode", "auto-night-mode", "message-corners", "" -> {
                                    val c = SettingsThemeController(context.context(), context.tdlib())
                                    c.setArguments(SettingsThemeController.Args(SettingsThemeController.MODE_THEMES))
                                    result = c
                                }

                                else -> {
                                    val c = SettingsThemeController(context.context(), context.tdlib())
                                    c.setArguments(SettingsThemeController.Args(SettingsThemeController.MODE_THEMES))
                                    result = c
                                }
                            }
                        }

                        TdApi.SettingsSectionChatFolders.CONSTRUCTOR -> {
                            if (Settings.instance().chatFoldersEnabled()) {
                                val chatFolders = SettingsFoldersController(context.context(), context.tdlib())
                                context.context().navigation().navigateTo(chatFolders)
                            } else {
                                showLinkTooltip(tdlib, R.drawable.baseline_warning_24, Lang.getString(R.string.InternalUrlUnsupported), openParameters)
                            }
                        }

                        TdApi.SettingsSectionDevices.CONSTRUCTOR -> {
                            val sessions = SettingsSessionsController(context.context(), context.tdlib())
                            val websites = SettingsWebsitesController(context.context(), context.tdlib())
                            result = SimpleViewPagerController(
                                context.context(), context.tdlib(), arrayOf<ViewController<*>>(sessions, websites), arrayOf<String>(
                                    Lang.getString(R.string.Devices).uppercase(Locale.getDefault()),
                                    Lang.getString(R.string.Websites).uppercase(Locale.getDefault())
                                ), false
                            )
                        }

                        SettingsSectionLanguage.CONSTRUCTOR -> {
                            val language = section as SettingsSectionLanguage
                            when (language.subsection) {
                                "" -> {}
                                "show-button" -> {}
                                "translate-chats" -> {}
                                "do-not-translate" -> {}
                            }
                            result = SettingsLanguageController(context.context(), context.tdlib())
                        }

                        TdApi.SettingsSectionFaq.CONSTRUCTOR -> {
                            openFaq(context)
                        }

                        TdApi.SettingsSectionPrivacyPolicy.CONSTRUCTOR -> {
                            openPrivacyPolicy(context)
                        }

                        TdApi.SettingsSectionAskQuestion.CONSTRUCTOR -> {
                            openSupport(context.context().navigation().getCurrentStackItem()!!)
                        }

                        SettingsSectionPrivacyAndSecurity.CONSTRUCTOR -> {
                            // Subsection of the section; may be one of
                            val privacyAndSecurity = section as SettingsSectionPrivacyAndSecurity
                            when (privacyAndSecurity.subsection) {
                                "login-email" -> {
                                    val navigation = context.context().navigation()
                                    val current = navigation.getCurrentStackItem()
                                    if (current != null) {
                                        editLoginEmail(current)
                                    }
                                }

                                "2sv", "2sv/change", "2sv/disable", "2sv/change-email" -> {
                                    val navigation = context.context().navigation()
                                    val current = navigation.getCurrentStackItem()
                                    if (current != null) {
                                        tdlib.send<PasswordState?>(
                                            GetPasswordState(),
                                            Tdlib.ResultHandler { passwordState: PasswordState?, error: TdApi.Error? ->
                                                current.runOnUiThreadOptional(
                                                    Runnable {
                                                        if (passwordState != null) {
                                                            if (!passwordState.hasPassword) {
                                                                val controller = Settings2FAController(context.context(), context.tdlib())
                                                                controller.setArguments(Settings2FAController.Args(null, null, null))
                                                                navigation.navigateTo(controller)
                                                            } else {
                                                                val controller = PasswordController(context.context(), context.tdlib())
                                                                controller.setArguments(
                                                                    PasswordController.Args(
                                                                        PasswordController.MODE_UNLOCK_EDIT,
                                                                        passwordState
                                                                    )
                                                                )
                                                                navigation.navigateTo(controller)
                                                            }
                                                        }
                                                    })
                                            })
                                    }
                                }

                                "active-websites", "active-websites/edit", "active-websites/disconnect-all" -> {
                                    result = SettingsWebsitesController(context.context(), context.tdlib())
                                }

                                "phone-number", "phone-number/never", "phone-number/always" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingShowPhoneNumber()))
                                    result = c
                                }

                                "last-seen", "last-seen/never", "last-seen/always", "last-seen/hide-read-time" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingShowStatus()))
                                    result = c
                                }

                                "profile-photos", "profile-photos/never", "profile-photos/always", "profile-photos/set-public", "profile-photos/update-public", "profile-photos/remove-public" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingShowProfilePhoto()))
                                    result = c
                                }

                                "bio", "bio/never", "bio/always" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingShowBio()))
                                    result = c
                                }

                                "gifts", "gifts/show-icon", "gifts/never", "gifts/always", "gifts/accepted-types" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingAutosaveGifts()))
                                    result = c
                                }

                                "birthday", "birthday/add", "birthday/never", "birthday/always" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingShowBirthdate()))
                                    result = c
                                }

                                "saved-music", "saved-music/never", "saved-music/always" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingShowProfileAudio()))
                                    result = c
                                }

                                "forwards", "forwards/never", "forwards/always" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingShowLinkInForwardedMessages()))
                                    result = c
                                }

                                "calls", "calls/never", "calls/always" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingAllowCalls()))
                                    result = c
                                }

                                "calls/p2p", "calls/p2p/never", "calls/p2p/always" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingAllowPeerToPeerCalls()))
                                    result = c
                                }

                                "calls/ios-integration" -> {
                                    // TODO different text?
                                    unsupported.run()
                                }

                                "voice", "voice/never", "voice/always" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingAllowPrivateVoiceAndVideoNoteMessages()))
                                    result = c
                                }

                                "invites", "invites/never", "invites/always" -> {
                                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingAllowChatInvites()))
                                    result = c
                                }

                                "passcode", "passcode/disable", "passcode/change", "passcode/auto-lock", "passcode/face-id", "passcode/fingerprint" -> {
                                    openPasscodeSetup(context)
                                }

                                "passkey", "passkey/create", "messages", "messages/set-price", "messages/exceptions", "blocked", "blocked/edit", "blocked/block-user", "blocked/block-user/chats", "blocked/block-user/contacts", "auto-delete", "auto-delete/set-custom", "self-destruct", "data-settings", "data-settings/sync-contacts", "data-settings/delete-synced", "data-settings/suggest-contacts", "data-settings/delete-cloud-drafts", "data-settings/clear-payment-info", "data-settings/link-previews", "data-settings/bot-settings", "data-settings/map-provider", "archive-and-mute" -> {}
                                "" -> {
                                    result = SettingsPrivacyController(context.context(), context.tdlib())
                                }

                                else -> {
                                    result = SettingsPrivacyController(context.context(), context.tdlib())
                                }
                            }
                        }

                        TdApi.SettingsSectionNotifications.CONSTRUCTOR -> {}
                        SettingsSectionEditProfile.CONSTRUCTOR -> {
                            val editProfile = section as SettingsSectionEditProfile
                            when (editProfile.subsection) {
                                "change-number" -> {
                                    result = SettingsPhoneController(context.context(), context.tdlib())
                                }

                                "", "set-photo", "first-name", "last-name", "emoji-status", "bio", "birthday", "username", "your-color", "channel", "add-account", "log-out", "profile-color/profile", "profile-color/profile/add-icons", "profile-color/profile/use-gift", "profile-color/name", "profile-color/name/add-icons", "profile-color/name/use-gift", "profile-photo/use-emoji" -> {
                                    result = SettingsController(context.context(), context.tdlib())
                                }
                            }
                        }

                        TdApi.SettingsSectionDataAndStorage.CONSTRUCTOR -> {
                            result = SettingsDataController(context.context(), context.tdlib())
                        }

                        TdApi.SettingsSectionQrCode.CONSTRUCTOR, TdApi.SettingsSectionSearch.CONSTRUCTOR, TdApi.SettingsSectionMyStars.CONSTRUCTOR, TdApi.SettingsSectionMyToncoins.CONSTRUCTOR, TdApi.SettingsSectionPowerSaving.CONSTRUCTOR, TdApi.SettingsSectionPremium.CONSTRUCTOR, TdApi.SettingsSectionSendGift.CONSTRUCTOR, TdApi.SettingsSectionBusiness.CONSTRUCTOR, TdApi.SettingsSectionInAppBrowser.CONSTRUCTOR, TdApi.SettingsSectionFeatures.CONSTRUCTOR -> {
                            unsupported.run()
                        }

                        else -> {
                            assertSettingsSection_94405f42()
                            throw unsupported(section)
                        }
                    }
                } else {
                    result = SettingsController(context.context(), context.tdlib())
                }
                if (result != null) {
                    context.context().navigation().navigateTo(result)
                }
            }

            InternalLinkTypePublicChat.CONSTRUCTOR -> {
                val publicChat = linkType as InternalLinkTypePublicChat
                if ((IV_PREVIEW_USERNAME == publicChat.chatUsername) and !isEmpty(originalUrl)) {
                    openExternalUrl(context, originalUrl!!, UrlOpenParameters(openParameters).forceInstantView(), after)
                } else {
                    openPublicChat(context, publicChat.chatUsername, openParameters)
                }
            }

            InternalLinkTypeDirectMessagesChat.CONSTRUCTOR -> {
                val directMessagesChat = linkType as InternalLinkTypeDirectMessagesChat
                openDirectMessages(context, directMessagesChat.channelUsername, openParameters)
            }

            InternalLinkTypeInstantView.CONSTRUCTOR -> {
                val instantView = linkType as InternalLinkTypeInstantView
                val instantViewOpenParameters = UrlOpenParameters(openParameters)
                    .forceInstantView()
                    .instantViewFallbackUrl(instantView.fallbackUrl)
                if (!isEmpty(originalUrl)) {
                    instantViewOpenParameters.originalUrl(originalUrl!!)
                }
                openExternalUrl(context, instantView.url, instantViewOpenParameters, after)
            }

            TdApi.InternalLinkTypeStory.CONSTRUCTOR, TdApi.InternalLinkTypeLiveStory.CONSTRUCTOR, TdApi.InternalLinkTypeStoryAlbum.CONSTRUCTOR, TdApi.InternalLinkTypeAttachmentMenuBot.CONSTRUCTOR, TdApi.InternalLinkTypeWebApp.CONSTRUCTOR, TdApi.InternalLinkTypeMainWebApp.CONSTRUCTOR, TdApi.InternalLinkTypeInvoice.CONSTRUCTOR, TdApi.InternalLinkTypeRestorePurchases.CONSTRUCTOR, TdApi.InternalLinkTypeChatBoost.CONSTRUCTOR, TdApi.InternalLinkTypeGiftCollection.CONSTRUCTOR, TdApi.InternalLinkTypeGiftAuction.CONSTRUCTOR, TdApi.InternalLinkTypeChatAffiliateProgram.CONSTRUCTOR, TdApi.InternalLinkTypeUpgradedGift.CONSTRUCTOR, TdApi.InternalLinkTypePassportDataRequest.CONSTRUCTOR, TdApi.InternalLinkTypeCallsPage.CONSTRUCTOR, TdApi.InternalLinkTypeChatSelection.CONSTRUCTOR, TdApi.InternalLinkTypeContactsPage.CONSTRUCTOR, TdApi.InternalLinkTypeMyProfilePage.CONSTRUCTOR, TdApi.InternalLinkTypeNewChannelChat.CONSTRUCTOR, TdApi.InternalLinkTypeNewGroupChat.CONSTRUCTOR, TdApi.InternalLinkTypeNewPrivateChat.CONSTRUCTOR, TdApi.InternalLinkTypeNewStory.CONSTRUCTOR, TdApi.InternalLinkTypeOauth.CONSTRUCTOR, TdApi.InternalLinkTypePremiumFeaturesPage.CONSTRUCTOR, TdApi.InternalLinkTypePremiumGiftPurchase.CONSTRUCTOR, TdApi.InternalLinkTypeRequestManagedBot.CONSTRUCTOR, TdApi.InternalLinkTypeSavedMessages.CONSTRUCTOR, TdApi.InternalLinkTypeSearch.CONSTRUCTOR, TdApi.InternalLinkTypeStarPurchase.CONSTRUCTOR, TdApi.InternalLinkTypeTextCompositionStyle.CONSTRUCTOR -> {
                unsupported.run()
            }

            InternalLinkTypePremiumGiftCode.CONSTRUCTOR -> {
                val code = (linkType as InternalLinkTypePremiumGiftCode).code

                // TODO progress
                tdlib.send<PremiumGiftCodeInfo?>(CheckPremiumGiftCode(code), Tdlib.ResultHandler { info: PremiumGiftCodeInfo?, error: TdApi.Error? ->
                    if (error != null) {
                        if (after != null) {
                            post(Runnable { after.runWithBool(false) })
                        }
                    } else {
                        post(Runnable {
                            val c = context.context().navigation().getCurrentStackItem()
                            if (c != null) {
                                ModernActionedLayout.showGiftCode(c, code, null, info!!)
                            }
                            if (after != null) {
                                after.runWithBool(true)
                            }
                        })
                    }
                })
            }

            TdApi.InternalLinkTypeQrCodeAuthentication.CONSTRUCTOR -> {
                showLinkTooltip(tdlib, R.drawable.baseline_warning_24, Lang.getString(R.string.ScanQRLinkHint), openParameters)
            }

            InternalLinkTypeTheme.CONSTRUCTOR -> {
                val theme = linkType as InternalLinkTypeTheme
                // TODO tdlib
                showLinkTooltip(tdlib, R.drawable.baseline_info_24, Lang.getMarkdownString(context, R.string.NoCloudThemeSupport), openParameters)
            }

            InternalLinkTypeBackground.CONSTRUCTOR -> {
                val background = linkType as InternalLinkTypeBackground
                // TODO show progress?
                tdlib.send<TdApi.Background?>(
                    SearchBackground(background.backgroundName),
                    Tdlib.ResultHandler { wallpaper: TdApi.Background?, error: TdApi.Error? ->
                        if (error != null) {
                            if (after != null) {
                                post(Runnable { after.runWithBool(false) })
                            }
                        } else {
                            post(Runnable {
                                val c = MessagesController(context.context(), context.tdlib())
                                c.setArguments(
                                    MessagesController.Arguments(MessagesController.PREVIEW_MODE_WALLPAPER_OBJECT, null, null).setWallpaperObject(wallpaper)
                                )
                                context.context().navigation().navigateTo(c)
                                if (after != null) {
                                    after.runWithBool(true)
                                }
                            })
                        }
                    })
                return
            }

            InternalLinkTypeChatFolderInvite.CONSTRUCTOR -> {
                if (Settings.instance().chatFoldersEnabled()) {
                    val invite = linkType as InternalLinkTypeChatFolderInvite
                    Companion.checkChatFolderInviteLink(context, originalUrl!!, invite, openParameters)
                } else {
                    showLinkTooltip(tdlib, R.drawable.baseline_warning_24, Lang.getString(R.string.InternalUrlUnsupported), openParameters)
                }
            }

            InternalLinkTypeBusinessChat.CONSTRUCTOR -> {
                val businessChatLink = linkType as InternalLinkTypeBusinessChat
                tdlib.send<BusinessChatLinkInfo?>(
                    GetBusinessChatLinkInfo(businessChatLink.linkName),
                    Tdlib.ResultHandler { businessChatLinkInfo: BusinessChatLinkInfo?, error: TdApi.Error? ->
                        if (error != null) {
                            post(Runnable {
                                showLinkTooltip(tdlib, R.drawable.baseline_warning_24, TD.toErrorString(error), openParameters)
                                if (after != null) {
                                    after.runWithBool(false)
                                }
                            })
                        } else {
                            post(Runnable {
                                openChat(
                                    context, businessChatLinkInfo!!.chatId, ChatOpenParameters()
                                        .keepStack()
                                        .fillDraft(businessChatLinkInfo.text)
                                )
                                if (after != null) {
                                    after.runWithBool(true)
                                }
                            })
                        }
                    })
                return  // async
            }

            InternalLinkTypeUnknownDeepLink.CONSTRUCTOR -> {
                // TODO progress
                val unknownDeepLink = linkType as InternalLinkTypeUnknownDeepLink
                tdlib.send<DeepLinkInfo?>(GetDeepLinkInfo(unknownDeepLink.link), Tdlib.ResultHandler { deepLink: DeepLinkInfo?, error: TdApi.Error? ->
                    if (error != null) {
                        post(Runnable {
                            if (error.code == 404) {
                                showLinkTooltip(tdlib, R.drawable.baseline_warning_24, Lang.getString(R.string.DeepLinkUnsupported), openParameters)
                            } else {
                                showLinkTooltip(tdlib, R.drawable.baseline_warning_24, TD.toErrorString(error), openParameters)
                            }
                            if (after != null) {
                                after.runWithBool(true) // Forcing true to avoid trying to open it again.
                            }
                        })
                    } else {
                        post(Runnable {
                            val c = context.context().navigation().getCurrentStackItem()
                            if (c != null) {
                                c.processDeepLinkInfo(deepLink!!)
                            }
                            if (after != null) {
                                after.runWithBool(true)
                            }
                        })
                    }
                })
                return  // async
            }

            else -> {
                assertInternalLinkType_44babac4()
                throw unsupported(linkType)
            }
        }
        if (after != null) {
            after.runWithBool(ok)
        }
    }

    fun openPasscodeSetup(context: TdlibDelegate) {
        if (Passcode.instance().isEnabled()) {
            val passcode = PasscodeController(context.context(), context.tdlib())
            passcode.setPasscodeMode(PasscodeController.MODE_UNLOCK_SETUP)
            context.context().navigation().navigateTo(passcode)
        } else {
            context.context().navigation().navigateTo(PasscodeSetupController(context.context(), context.tdlib()))
        }
    }

    fun openFaq(context: TdlibDelegate) {
        tdlib.ui().openUrl(context, Lang.getString(R.string.url_faq), UrlOpenParameters().forceInstantView())
    }

    fun openPrivacyPolicy(context: TdlibDelegate) {
        tdlib.ui().openUrl(context, Lang.getStringSecure(R.string.url_privacyPolicy), UrlOpenParameters().forceInstantView())
    }

    fun editLoginEmail(context: ViewController<*>) {
        context.tdlib()!!.send<PasswordState?>(GetPasswordState(), Tdlib.ResultHandler { passwordState: PasswordState?, error: TdApi.Error? ->
            if (passwordState != null) {
                context.runOnUiThreadOptional(Runnable {
                    editLoginEmail(context, passwordState)
                })
            }
        })
    }

    fun editLoginEmail(context: ViewController<*>, passwordState: PasswordState) {
        val act = Runnable {
            val controller = PasswordController(context.context(), context.tdlib())
            controller.setArguments(PasswordController.Args(PasswordController.MODE_LOGIN_EMAIL_CHANGE, passwordState))
            context.navigateTo(controller)
        }
        if (isEmpty(passwordState.loginEmailAddressPattern)) {
            act.run()
        } else {
            val b = ViewController.Options.Builder()
                .info(Lang.getMarkdownString(context, R.string.ChangeEmailPromptText))
                .item(OptionItem.Builder().id(R.id.btn_changeEmail).name(R.string.ChangeEmailPromptButton).icon(R.drawable.baseline_edit_24).build())
                .cancelItem()
            if (!isEmpty(passwordState.loginEmailAddressPattern)) {
                // TODO(spoiler): replace `*` with the spoiler effect
                b.title(passwordState.loginEmailAddressPattern)
            }
            context.showOptions(b.build(), OptionDelegate { optionView: View?, optionId: Int ->
                if (optionId == R.id.btn_changeEmail) {
                    act.run()
                }
                true
            }
            )
        }
    }

    fun showUrlOptions(context: TdlibDelegate, url: String, openParametersFuture: Future<UrlOpenParameters?>?) {
        val c = context.context().navigation().getCurrentStackItem()
        if (c == null) return

        c.showOptions(
            url, intArrayOf(
                R.id.btn_open,
                R.id.btn_copyLink,
                R.id.btn_share
            ), arrayOf<String?>(
                Lang.getString(R.string.Open),
                Lang.getString(R.string.Copy),
                Lang.getString(R.string.Share)
            ), null, intArrayOf(
                R.drawable.baseline_open_in_browser_24,
                R.drawable.baseline_content_copy_24,
                R.drawable.baseline_forward_24
            ), OptionDelegate { optionItemView: View?, id: Int ->
                if (id == R.id.btn_open) {
                    openUrl(context, url, if (openParametersFuture != null) openParametersFuture.getValue() else null)
                } else if (id == R.id.btn_copyLink) {
                    UI.copyText(url, R.string.CopiedLink)
                } else if (id == R.id.btn_share) {
                    shareUrl(context, url)
                }
                true
            })
    }

    fun openProxyAlert(context: TdlibDelegate, proxy: TdApi.Proxy?) {
        val c = context.context().navigation().getCurrentStackItem()
        if (c == null) return
        if (proxy == null) {
            // unsupported proxy
            return
        }
        val proxyDescription = newProxyDescription(proxy.server, proxy.port.toString()).toString()

        val msg = Strings.buildHtml(proxyDescription)
        msg.insert(0, "\n\n")
        val title = Lang.getString(R.string.EnableProxyAlertTitle)
        msg.insert(0, title)
        msg.setSpan(TD.newBoldSpan(title), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        msg.append("\n\n")
        msg.append(Lang.getString(R.string.EnableProxyAlertHint))
        if (proxy.type.getConstructor() == TdApi.ProxyTypeMtproto.CONSTRUCTOR) {
            msg.append("\n\n")
            msg.append(Lang.getString(R.string.EnableProxyAlertHintMtproto))
        }
        val ids = IntList(3)
        val strings = StringList(3)
        val icons = IntList(3)
        val colors = IntList(3)

        ids.append(R.id.btn_addProxy)
        strings.append(R.string.ProxyEnable)
        icons.append(R.drawable.baseline_security_24)
        colors.append(ViewController.OptionColor.BLUE)

        ids.append(R.id.btn_save)
        strings.append(R.string.ProxySaveForLater)
        icons.append(R.drawable.baseline_playlist_add_24)
        colors.append(ViewController.OptionColor.NORMAL)

        ids.append(R.id.btn_cancel)
        strings.append(R.string.Cancel)
        icons.append(R.drawable.baseline_cancel_24)
        colors.append(ViewController.OptionColor.NORMAL)

        c.showOptions(msg, ids.get(), strings.get(), colors.get(), icons.get(), OptionDelegate { itemView: View?, id: Int ->
            if (id == R.id.btn_addProxy) {
                Settings.instance().addOrUpdateProxy(proxy, null, true)
            } else if (id == R.id.btn_save) {
                Settings.instance().addOrUpdateProxy(proxy, null, false)
            }
            true
        })
    }

    // Delete account on server
    fun permanentlyDeleteAccount(context: ViewController<*>, showAlternatives: Boolean) {
        val needShowAlternatives = tdlib.isAuthorized && showAlternatives
        context.showOptions(
            Lang.getMarkdownString(context, if (needShowAlternatives) R.string.DeleteAccountConfirmFirst else R.string.DeleteAccountConfirm),
            intArrayOf(R.id.btn_deleteAccount, R.id.btn_cancel),
            arrayOf<String?>(
                Lang.getString(if (needShowAlternatives) R.string.DeleteAccountConfirmFirstBtn else R.string.DeleteAccountConfirmBtn),
                Lang.getString(R.string.Cancel)
            ),
            intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_delete_alert_24, R.drawable.baseline_cancel_24),
            OptionDelegate { optionItemView: View?, id: Int ->
                if (id == R.id.btn_deleteAccount) {
                    if (needShowAlternatives) {
                        val c = SettingsLogOutController(context.context(), tdlib)
                        c.setArguments(SettingsLogOutController.Type.DELETE_ACCOUNT)
                        context.navigateTo(c)
                        return@OptionDelegate true
                    }

                    tdlib.send<PasswordState?>(GetPasswordState(), Tdlib.ResultHandler { passwordState: PasswordState?, error: TdApi.Error? ->
                        context.runOnUiThreadOptional(
                            Runnable {
                                if (error != null) {
                                    UI.showError(error)
                                    return@Runnable
                                }
                                context.runOnUiThreadOptional(Runnable {
                                    if (!passwordState!!.hasPassword) {
                                        context.navigateTo(EditDeleteAccountReasonController(context.context(), tdlib))
                                        return@Runnable
                                    }
                                    promptPassword(context, passwordState, object : CustomConfirmDelegate {
                                        override fun getName(): CharSequence {
                                            return Lang.getString(R.string.DeleteAccount)
                                        }

                                        override fun needNext(): Boolean {
                                            return true
                                        }

                                        override fun onPasswordConfirmed(c: ViewController<*>, password: String?) {
                                            val target = EditDeleteAccountReasonController(context.context(), tdlib)
                                            target.setArguments(password)
                                            c.navigateTo(target)
                                        }
                                    })
                                })
                            })
                    })
                }
                true
            }
        )
    }

    private fun promptPassword(context: ViewController<*>, passwordState: PasswordState?, confirmDelegate: CustomConfirmDelegate) {
        val controller = PasswordController(context.context(), context.tdlib())
        controller.setArguments(
            PasswordController.Args(PasswordController.MODE_CUSTOM_CONFIRM, passwordState)
                .setConfirmDelegate(confirmDelegate)
        )
        context.navigateTo(controller)
    }

    // Log out
    fun logOut(context: ViewController<*>, showAlternatives: Boolean) {
        if (tdlib.myUserId() == 0L) {
            return
        }
        if (showAlternatives) {
            val c = SettingsLogOutController(context.context(), tdlib)
            context.navigateTo(c)
            return
        }
        removeAccount(context, tdlib.account()!!, true)
        /*context.showOptions(new int[]{R.id.btn_logout, R.id.btn_cancel}, new String[]{Lang.getString(R.string.LogOut), Lang.getString(R.string.Cancel)}, new int[]{ViewController.OPTION_COLOR_RED, ViewController.OPTION_COLOR_NORMAL}, id -> {
      if (id == R.id.btn_logout) {
        tdlib.send(new TdApi.LogOut(), tdlib.typedOkHandler());
        // TD.clearAll(tdlib.id(), true);
        ImageLoader.instance().clear(tdlib.id(), true);
        */
        /* TODO reset prefs

        Prefs.instance().clear();
        ContactManager.instance().reset(false, null);*/
        /*

        // TGDataManager.instance().logOut();
        // proceedToLogin();
      }
      return true;
    });*/
    }

    // Map
    fun openMap(context: TdlibDelegate, args: MapController.Args): Boolean {
        if (!U.isGooglePlayServicesAvailable(context.context())) {
            return Intents.openMap(args.latitude, args.longitude, args.title, args.address)
        }
        val c = MapControllerFactory.newMapController(context.context(), context.tdlib())
        c.setArguments(args)
        context.context().navigation().navigateTo(c)
        return true
    }

    // Proxy
    /**
     * Opens available proxy list.
     * 
     * If no proxies available, opens proxy add screen.
     */
    fun openProxySettings(context: TdlibDelegate, needProxyHint: Boolean) {
        if (Settings.instance().availableProxyCount == 0) {
            addNewProxy(context, needProxyHint)
        } else {
            context.context().navigation().navigateTo(SettingsProxyController(context.context(), context.tdlib()))
        }
    }

    /**
     * Asks which type of proxy should be added,
     * then opens appropriate screen
     */
    fun addNewProxy(context: TdlibDelegate, needProxyHint: Boolean) {
        val c = context.context().navigation().getCurrentStackItem()
        if (c == null) {
            return
        }

        /*boolean needTor = !checkTorAvailability;
    if (checkTorAvailability) {
      TdApi.ProxySocks5 torProxy = new TdApi.ProxySocks5("127.0.0.1", 9050, null, null);
      int proxyId = Settings.instance().getExistingProxyId(torProxy);
      needTor = proxyId == Settings.PROXY_ID_NONE;
    }
    boolean needInstallTor = needTor && !U.isAppInstalled(U.PACKAGE_TOR);*/
        val showShowQr = tdlib.allowQrLoginCamera()

        val ids = IntList(if (showShowQr) 4 else 3)
        val strings = StringList(if (showShowQr) 4 else 3)

        ids.append(R.id.btn_proxyTelegram)
        ids.append(R.id.btn_proxySocks5)
        ids.append(R.id.btn_proxyHttp)

        if (needProxyHint) {
            strings.append(R.string.AddMtprotoProxy)
            strings.append(R.string.AddSocks5Proxy)
            strings.append(R.string.AddHttpProxy)
        } else {
            strings.append(R.string.MtprotoProxy)
            strings.append(R.string.Socks5Proxy)
            strings.append(R.string.HttpProxy)
        }

        if (showShowQr) {
            ids.append(R.id.btn_proxyQr)
            strings.append(R.string.ScanQR)
        }

        val callback = OptionDelegate { itemView: View?, id: Int ->
            if (id == R.id.btn_proxySocks5) {
                val e = EditProxyController(context.context(), context.tdlib())
                e.setArguments(EditProxyController.Args(EditProxyController.MODE_SOCKS5))
                c.navigateTo(e)
            } else if (id == R.id.btn_proxyTelegram) {
                val e = EditProxyController(context.context(), context.tdlib())
                e.setArguments(EditProxyController.Args(EditProxyController.MODE_MTPROTO))
                c.navigateTo(e)
            } else if (id == R.id.btn_proxyHttp) {
                val e = EditProxyController(context.context(), context.tdlib())
                e.setArguments(EditProxyController.Args(EditProxyController.MODE_HTTP))
                c.navigateTo(e)
            } else if (id == R.id.btn_proxyQr) {
                postDelayed(Runnable {
                    c.openInAppCamera(
                        CameraOpenOptions().ignoreAnchor(true).noTrace(true).allowSystem(false).optionalMicrophone(true)
                        .qrModeSubtitle(R.string.ScanQRFullSubtitleProxy).mode(
                        CameraController.MODE_QR
                    ).qrCodeListener(QrCodeListener { qrCode: String? ->
                        context.tdlib()!!.send<InternalLinkType?>(
                            GetInternalLinkType(qrCode),
                            Tdlib.ResultHandler { internalLinkType: InternalLinkType?, error: TdApi.Error? ->
                                if (internalLinkType != null && internalLinkType.getConstructor() == InternalLinkTypeProxy.CONSTRUCTOR) {
                                    val proxy = (internalLinkType as InternalLinkTypeProxy).proxy
                                    post(Runnable { openProxyAlert(context, proxy) }
                                    )
                                }
                            })
                    })
                    )
                }, 250L)
                /*case R.id.btn_proxyTor: {
          if (needInstallTor) {
            UI.showToast(R.string.ProxyTorUnavailable, Toast.LENGTH_SHORT);
            Intents.openGooglePlay(U.PACKAGE_TOR);
            return false;
          } else {
            Settings.instance().addOrUpdateProxy(new TdApi.ProxySocks5("127.0.0.1", 9050, null, null), "Tor network", false);
          }
          break;
        }*/
            }
            true
        }

        if (ids.size() == 1) {
            callback.onOptionItemPressed(null, ids.get(0))
        } else {
            c.showOptions(if (needProxyHint) Lang.getString(R.string.ProxyInfo) else null, ids.get(), strings.get(), callback)
        }
    }

    fun handleTermsOfService(update: UpdateTermsOfService?) {
        // TODO handle updateTermsOfService
        // Log.i("%s", update);
    }

    // Leave/join chat
    fun processLeaveButton(context: ViewController<*>, chatList: ChatList?, chatId: Long, actionId: Int, after: Runnable?): Boolean {
        if (actionId == R.id.btn_returnToChat) {
            leaveJoinChat(context, chatId, true, after)
            return true
        } else if (actionId == R.id.btn_removePsaChatFromList) {
            showHidePsaConfirm(context, chatList, chatId, after)
            return true
        } else if (actionId == R.id.btn_removeChatFromList || actionId == R.id.btn_removeChatFromListAndStop) {
            showDeleteChatConfirm(context, chatId, false, actionId == R.id.btn_removeChatFromListAndStop, after)
            return true
        } else if (actionId == R.id.btn_removeChatFromListOrClearHistory) {
            showDeleteChatConfirm(context, chatId, !tdlib.isChannel(chatId), tdlib.suggestStopBot(chatId), after)
            return true
        } else if (actionId == R.id.btn_clearChatHistory) {
            showClearHistoryConfirm(context, chatId, after)
            return true
        } else if (actionId == R.id.btn_setPasscode) {
            showPasscodeOptions(context, chatId)
            return true
        }
        return false
    }

    fun addDeleteChatOptions(chatId: Long, ids: IntList, strings: StringList, allowClearHistory: Boolean, forceJoin: Boolean) {
        val chat = tdlib.chat(chatId)
        if (chat == null) return
        when (getType(chatId)) {
            TdApi.ChatTypePrivate.CONSTRUCTOR, TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                if (allowClearHistory && tdlib.canClearHistory(chatId)) {
                    ids.append(R.id.btn_clearChatHistory)
                    strings.append(R.string.ClearHistory)
                }

                if (tdlib.suggestStopBot(chat)) {
                    ids.append(R.id.btn_removeChatFromListAndStop)
                    strings.append(R.string.DeleteAndStop)
                } else {
                    ids.append(R.id.btn_removeChatFromList)
                    strings.append(R.string.DeleteChat)
                }
            }

            TdApi.ChatTypeBasicGroup.CONSTRUCTOR -> {
                if (allowClearHistory && tdlib.canClearHistory(chat)) {
                    ids.append(R.id.btn_clearChatHistory)
                    strings.append(R.string.ClearHistory)
                }
                val basicGroup = tdlib.chatToBasicGroup(chatId)
                val status = tdlib.chatStatus(chatId)
                if (tdlib.chatAvailable(chat) && status != null) {
                    if (TD.isMember(status, false) || (basicGroup != null && !basicGroup.isActive)) {
                        ids.append(R.id.btn_removeChatFromList)
                        strings.append(R.string.LeaveMegaMenu)
                    } else {
                        ids.append(R.id.btn_removeChatFromList)
                        strings.append(R.string.DeleteChat)
                    }
                    if (TD.canReturnToChat(status) && (basicGroup == null || basicGroup.isActive)) {
                        ids.append(R.id.btn_returnToChat)
                        strings.append(R.string.returnToGroup)
                    }
                }
            }

            TdApi.ChatTypeSupergroup.CONSTRUCTOR -> {
                if (allowClearHistory && tdlib.canClearHistory(chat)) {
                    ids.append(R.id.btn_clearChatHistory)
                    strings.append(R.string.ClearHistory)
                }
                val status = tdlib.chatStatus(chatId)
                if (tdlib.chatAvailable(chat)) {
                    ids.append(R.id.btn_removeChatFromList)
                    strings.append(if (tdlib.isChannel(chatId)) R.string.LeaveChannel else R.string.LeaveMegaMenu)
                } else if (TD.canReturnToChat(status)) {
                    if (status!!.getConstructor() == ChatMemberStatusLeft.CONSTRUCTOR && (tdlib.isPublicChat(chatId) || tdlib.isTemporaryAccessible(chatId))) {
                        if (forceJoin) {
                            ids.append(R.id.btn_returnToChat)
                            strings.append(if (tdlib.isChannel(chatId)) R.string.JoinChannel else R.string.JoinChat)
                        }
                    } else {
                        ids.append(R.id.btn_returnToChat)
                        strings.append(if (tdlib.isChannel(chatId)) R.string.returnToChannel else R.string.returnToGroup)
                    }
                }
            }
        }
    }

    private fun leaveJoinChat(context: ViewController<*>, chatId: Long, join: Boolean, after: Runnable?) {
        val status = tdlib.chatStatus(chatId)
        if (status == null) {
            return
        }
        if (tdlib.context().watchDog().isOffline()) {
            UI.showNetworkPrompt()
            return
        }
        var confirmButtonRes = 0
        var informationStr: String? = null
        var forceAdd = false
        val hasPublicLink = !isEmpty(tdlib.chatUsername(chatId))
        val isChannel = tdlib.isChannel(chatId)
        val canReturnAfterLeave = isBasicGroup(chatId) || hasPublicLink
        val newStatus: ChatMemberStatus?
        when (status.getConstructor()) {
            ChatMemberStatusCreator.CONSTRUCTOR -> {
                forceAdd = !(status as ChatMemberStatusCreator).isMember
                if (join != forceAdd) {
                    return
                }
                val oldStatus = status
                newStatus = ChatMemberStatusCreator(oldStatus.isAnonymous, forceAdd)
                if (!forceAdd) {
                    informationStr =
                        Lang.getString(if (hasPublicLink) (if (isChannel) R.string.LeaveReturnPublicLinkHintChannel else R.string.LeaveReturnPublicLinkHintGroup) else (if (isChannel) R.string.LeaveCreatorHintChannel else R.string.LeaveCreatorHintGroup))
                    confirmButtonRes = if (isChannel) R.string.LeaveChannel else R.string.LeaveMegaMenu
                }
            }

            ChatMemberStatusAdministrator.CONSTRUCTOR -> {
                if (join) {
                    return
                }
                informationStr =
                    Lang.getString(if (canReturnAfterLeave) R.string.LeaveChatAdminHint else (if (isChannel) R.string.LeaveAdminNoReturnHintChannel else R.string.LeaveAdminNoReturnHintGroup))
                confirmButtonRes = if (isChannel) R.string.LeaveChannel else R.string.LeaveMegaMenu
                newStatus = ChatMemberStatusLeft()
            }

            ChatMemberStatusMember.CONSTRUCTOR -> {
                if (join) {
                    return
                }
                confirmButtonRes = if (isChannel) R.string.LeaveChannel else R.string.LeaveMegaMenu
                if (canReturnAfterLeave) informationStr =
                    if (hasPublicLink) Lang.getString(if (isChannel) R.string.LeaveReturnPublicLinkHintChannel else R.string.LeaveReturnPublicLinkHintGroup) else null
                else informationStr = Lang.getString(if (isChannel) R.string.LeaveNoReturnHintChannel else R.string.LeaveNoReturnHintGroup)
                newStatus = ChatMemberStatusLeft()
            }

            ChatMemberStatusRestricted.CONSTRUCTOR -> {
                val restricted = status as ChatMemberStatusRestricted
                if (join == restricted.isMember) {
                    return
                }
                if (restricted.isMember) {
                    newStatus = ChatMemberStatusLeft()
                    if (canReturnAfterLeave) informationStr =
                        if (hasPublicLink) Lang.getString(if (isChannel) R.string.LeaveReturnPublicLinkHintChannel else R.string.LeaveReturnPublicLinkHintGroup) else null
                    else informationStr = Lang.getString(if (isChannel) R.string.LeaveNoReturnHintGroup else R.string.LeaveNoReturnHintChannel)
                    confirmButtonRes = if (isChannel) R.string.LeaveChannel else R.string.LeaveMegaMenu
                } else {
                    newStatus = ChatMemberStatusMember()
                    forceAdd = true
                }
            }

            ChatMemberStatusLeft.CONSTRUCTOR -> {
                if (!join) return
                newStatus = ChatMemberStatusMember()
            }

            ChatMemberStatusBanned.CONSTRUCTOR -> newStatus = null
            else -> newStatus = null
        }
        if (newStatus == null) {
            if (join) {
                UI.showToast(R.string.NoReturnToChat, Toast.LENGTH_SHORT)
            }
            return
        }
        val needAdd =
            forceAdd || (isBasicGroup(chatId) && status.getConstructor() == ChatMemberStatusLeft.CONSTRUCTOR && newStatus.getConstructor() == ChatMemberStatusMember.CONSTRUCTOR)
        val act = RunnableBool { deleteChat: Boolean ->
            val myUserId = tdlib.myUserId()
            if (myUserId != 0L) {
                if (needAdd) {
                    tdlib.send<FailedToAddMembers?>(AddChatMember(chatId, myUserId, 0), tdlib.errorHandler<FailedToAddMembers?>())
                } else {
                    var handler = tdlib.okHandler()
                    if (deleteChat) {
                        handler = Client.ResultHandler { result: TdApi.Object? ->
                            when (result!!.getConstructor()) {
                                TdApi.Ok.CONSTRUCTOR -> {
                                    if (isBasicGroup(chatId)) {
                                        tdlib.send<TdApi.Ok?>(DeleteChatHistory(chatId, true, false), tdlib.typedOkHandler())
                                    }
                                }

                                TdApi.Error.CONSTRUCTOR -> {
                                    UI.showError(result)
                                    Log.e("setChatMemberStatus chatId:%d, status:%s error:%s", chatId, newStatus, TD.toErrorString(result))
                                }
                            }
                        }
                    }
                    tdlib.client().send(SetChatMemberStatus(chatId, MessageSenderUser(myUserId), newStatus), handler)
                    if (!TD.isMember(newStatus, false)) {
                        exitToChatScreen(context, chatId)
                    }
                }
            }
        }
        if (confirmButtonRes != 0) {
            val checkId = if (join) R.id.btn_returnToChat else R.id.btn_removeChatFromList
            if (!join && isBasicGroup(chatId)) {
                val b = SettingsWrapBuilder(R.id.btn_removeChatFromList).setSaveStr(R.string.LeaveDoneGroup).setSaveColorId(ColorId.textNegative)
                if (informationStr != null) {
                    b.addHeaderItem(informationStr)
                }
                b.setRawItems(
                    arrayOf<ListItem>(
                        ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_removeChatFromList, 0, R.string.LeaveRemoveFromList, R.id.btn_removeChatFromList, true)
                    )
                )
                b.setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                    val removeFromChatsList = result!!.get(R.id.btn_removeChatFromList) == R.id.btn_removeChatFromList
                    act.runWithBool(removeFromChatsList)
                })
                context.showSettings(b)
            } else {
                context.showOptions(
                    informationStr,
                    intArrayOf(checkId, R.id.btn_cancel),
                    arrayOf<String?>(Lang.getString(confirmButtonRes), Lang.getString(R.string.Cancel)),
                    intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                    intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
                    OptionDelegate { itemView: View?, id: Int ->
                        if (id == checkId) {
                            act.runWithBool(false)
                        }
                        true
                    })
            }
        } else {
            act.runWithBool(false)
        }
    }

    fun exitToChatScreen(context: ViewController<*>, chatId: Long) {
        val stack = context.context().navigation().getStack()
        val current = stack.getCurrent()
        if (stack.size() > 1 && context.getChatId() == chatId && (current === context || (current != null && current.getChatId() == chatId))) {
            for (i in stack.size() - 2 downTo 1) {
                if (stack.get(i) is ChatsController) break
                stack.destroy(i)
            }
            current.setShareCustomHeaderView(false)
            current.navigateBack()
        }
    }


    // Delete chats
    @StringRes
    private fun getDeleteChatStringRes(chatId: Long, allowBlock: Boolean): Int {
        val chat = tdlib.chat(chatId)
        if (chat == null) return R.string.DeleteChat
        when (getType(chatId)) {
            TdApi.ChatTypePrivate.CONSTRUCTOR, TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                return if (allowBlock && tdlib.suggestStopBot(chat)) R.string.DeleteAndStop else R.string.DeleteChat
            }

            TdApi.ChatTypeBasicGroup.CONSTRUCTOR -> {
                val basicGroup = tdlib.chatToBasicGroup(chatId)
                val status = tdlib.chatStatus(chatId)
                if (tdlib.chatAvailable(chat) && status != null) {
                    if (TD.isMember(status, false) || (basicGroup != null && !basicGroup.isActive)) {
                        return R.string.LeaveMegaMenu
                    } else {
                        return R.string.DeleteChat
                    }
                }
            }

            TdApi.ChatTypeSupergroup.CONSTRUCTOR -> {
                if (tdlib.chatAvailable(chat)) {
                    return if (tdlib.isChannel(chatId)) R.string.LeaveChannel else R.string.LeaveMegaMenu
                }
            }
        }

        return R.string.DeleteChat
    }

    private fun showClearHistoryConfirm(context: ViewController<*>, chatId: Long, after: Runnable?, isSecondaryConfirm: Boolean = false) {
        if (tdlib.canRevokeChat(chatId) || (tdlib.canClearHistoryForAllUsers(chatId) && tdlib.canClearHistoryOnlyForSelf(chatId))) {
            context.showSettings(
                SettingsWrapBuilder(R.id.btn_removeChatFromList)
                .setAllowResize(false)
                .addHeaderItem(
                    if (tdlib.isSelfChat(chatId)) Lang.getMarkdownString(
                        context,
                        R.string.ClearSavedMessagesConfirm
                    ) else Lang.getString(R.string.ClearHistoryConfirm)
                )
                .setSaveColorId(ColorId.textNegative)
                .setSaveStr(R.string.Delete)
                .setRawItems(
                    arrayOf<ListItem>(
                        ListItem(
                            ListItem.TYPE_CHECKBOX_OPTION,
                            R.id.btn_clearChatHistory,
                            0,
                            if (isUserChat(chatId)) Lang.getStringBold(
                                R.string.DeleteSecretChatHistoryForOtherParty,
                                tdlib.cache().userFirstName(tdlib.chatUserId(chatId))
                            ) else Lang.getString(R.string.DeleteChatHistoryForAllUsers),
                            R.id.btn_clearChatHistory,
                            tdlib.canRevokeChat(chatId) && tdlib.isUserChat(chatId) && tdlib.cache().userDeleted(tdlib.chatUserId(chatId))
                        )
                    )
                )
                .setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                    val clearHistory = result!!.get(R.id.btn_clearChatHistory) == R.id.btn_clearChatHistory
                    tdlib.send<TdApi.Ok?>(DeleteChatHistory(chatId, false, clearHistory), tdlib.typedOkHandler())
                    U.run(after)
                })
            )
        } else {
            val revoke = !tdlib.canClearHistoryOnlyForSelf(chatId)
            val needSecondaryConfirm: Boolean
            val info: CharSequence?
            val confirmButton: String
            @DrawableRes val confirmButtonIcon = if (isSecondaryConfirm) R.drawable.baseline_delete_forever_24 else R.drawable.templarian_baseline_broom_24
            if (tdlib.isSelfChat(chatId)) {
                needSecondaryConfirm = true
                info =
                    Lang.getMarkdownString(context, if (isSecondaryConfirm) R.string.ClearSavedMessagesSecondaryConfirm else R.string.ClearSavedMessagesConfirm)
                confirmButton = Lang.getString(if (isSecondaryConfirm) R.string.ClearSavedMessages else R.string.ClearHistory)
            } else if (tdlib.isChannel(chatId)) {
                needSecondaryConfirm = true
                info = Lang.getMarkdownString(context, if (isSecondaryConfirm) R.string.ClearChannelSecondaryConfirm else R.string.ClearChannelConfirm)
                confirmButton = Lang.getString(if (isSecondaryConfirm) R.string.ClearChannel else R.string.ClearHistoryAll)
            } else {
                needSecondaryConfirm = false
                info = Lang.getString(if (revoke) R.string.ClearHistoryAllConfirm else R.string.ClearHistoryConfirm)
                confirmButton = Lang.getString(if (revoke) R.string.ClearHistoryAll else R.string.ClearHistory)
            }
            context.showOptions(
                info,
                intArrayOf(R.id.btn_clearChatHistory, R.id.btn_cancel),
                arrayOf<String?>(confirmButton, Lang.getString(R.string.Cancel)),
                intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                intArrayOf(confirmButtonIcon, R.drawable.baseline_cancel_24), OptionDelegate { itemView: View?, id: Int ->
                    if (id == R.id.btn_clearChatHistory) {
                        if (needSecondaryConfirm && !isSecondaryConfirm) {
                            showClearHistoryConfirm(context, chatId, after, true)
                        } else {
                            tdlib.send<TdApi.Ok?>(DeleteChatHistory(chatId, false, revoke), tdlib.typedOkHandler())
                            U.run(after)
                        }
                    }
                    true
                })
        }
    }

    fun showDeleteChatConfirm(context: ViewController<*>, chatId: Long) {
        showDeleteChatConfirm(context, chatId, false, tdlib.suggestStopBot(chatId), null)
    }

    private fun showDeleteOrClearHistory(
        context: ViewController<*>,
        chatId: Long,
        chatName: CharSequence?,
        onDelete: Runnable,
        allowClearHistory: Boolean,
        allowBlock: Boolean,
        after: Runnable?
    ) {
        if (!allowClearHistory || !tdlib.canClearHistory(chatId)) {
            onDelete.run()
            return
        }
        context.showOptions(
            chatName,
            intArrayOf(R.id.btn_removeChatFromList, R.id.btn_clearChatHistory),
            arrayOf<String?>(Lang.getString(getDeleteChatStringRes(chatId, allowBlock)), Lang.getString(R.string.ClearHistory)),
            intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_delete_24, R.drawable.templarian_baseline_broom_24),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_removeChatFromList) {
                    onDelete.run()
                } else if (id == R.id.btn_clearChatHistory) {
                    showClearHistoryConfirm(context, chatId, after)
                }
                true
            })
    }

    private fun showHidePsaConfirm(context: ViewController<*>, chatList: ChatList?, chatId: Long, after: Runnable?) {
        val deleter = Runnable {
            tdlib.deleteChat(chatId, false, null)
            exitToChatScreen(context, chatId)
            U.run(after)
        }
        val source = tdlib.chatSource(chatList, chatId)
        if (source !is ChatSourcePublicServiceAnnouncement) return
        context.showOptions(
            Lang.getPsaHideConfirm(source, tdlib.chatTitle(chatId)),
            intArrayOf(R.id.btn_delete, R.id.btn_cancel),
            arrayOf<String?>(Lang.getString(R.string.PsaHideDone), Lang.getString(R.string.Cancel)),
            intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_delete_sweep_24, R.drawable.baseline_cancel_24),
            OptionDelegate { optionItemView: View?, id: Int ->
                if (id == R.id.btn_delete) {
                    deleter.run()
                }
                true
            })
    }

    private fun showDeleteChatConfirm(context: ViewController<*>, chatId: Long, allowClearHistory: Boolean, blockUser: Boolean, after: Runnable?) {
        val deleter = RunnableBool { revoke: Boolean ->
            tdlib.deleteChat(chatId, revoke, null)
            UI.post(Runnable {
                exitToChatScreen(context, chatId)
                U.run(after)
            })
        }
        when (getType(chatId)) {
            TdApi.ChatTypePrivate.CONSTRUCTOR -> {
                val userId = toUserId(chatId)
                val userName = tdlib.cache().userFirstName(userId)
                val deleteAndStop = blockUser && tdlib.isBotChat(chatId)
                showDeleteOrClearHistory(
                    context, chatId,
                    if (tdlib.isSelfUserId(userId)) Lang.getString(R.string.SavedMessages) else if (tdlib.isRepliesChat(fromUserId(userId))) Lang.getString(R.string.RepliesBot) else Lang.getString(
                        R.string.ChatWithUser,
                        userName
                    ), Runnable {
                        val info: CharSequence?
                        if (tdlib.isSelfUserId(userId)) {
                            info = Lang.getMarkdownString(context, R.string.DeleteSavedMessagesConfirm)
                        } else if (deleteAndStop) {
                            if (tdlib.isRepliesChat(fromUserId(userId))) {
                                info = Lang.getMarkdownString(context, R.string.DeleteAndStopRepliesConfirm)
                            } else {
                                info = Lang.getStringBold(R.string.DeleteAndStopBotConfirm, userName)
                            }
                        } else {
                            if (tdlib.isRepliesChat(fromUserId(userId))) {
                                info = Lang.getMarkdownString(context, R.string.DeleteRepliesConfirm)
                            } else {
                                info = Lang.getStringBold(R.string.DeleteUserChatConfirm, userName)
                            }
                        }
                        if (tdlib.canRevokeChat(chatId)) {
                            context.showSettings(
                                SettingsWrapBuilder(R.id.btn_removeChatFromList)
                                    .setAllowResize(false)
                                    .addHeaderItem(info)
                                    .setRawItems(
                                        arrayOf<ListItem>(
                                            ListItem(
                                                ListItem.TYPE_CHECKBOX_OPTION,
                                                R.id.btn_clearChatHistory,
                                                0,
                                                Lang.getString(R.string.DeleteSecretChatHistoryForOtherParty, userName),
                                                R.id.btn_clearChatHistory,
                                                false
                                            )
                                        )
                                    )
                                    .setSaveColorId(ColorId.textNegative)
                                    .setSaveStr(R.string.Delete)
                                    .setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                                        val clearHistory = result!!.get(R.id.btn_clearChatHistory) == R.id.btn_clearChatHistory
                                        if (blockUser) {
                                            tdlib.blockSender(
                                                MessageSenderUser(userId),
                                                BlockListMain(),
                                                Client.ResultHandler { blockResult: TdApi.Object? -> deleter.runWithBool(clearHistory) })
                                        } else {
                                            deleter.runWithBool(clearHistory)
                                        }
                                    })
                            )
                        } else {
                            context.showOptions(
                                info,
                                intArrayOf(R.id.btn_removeChatFromList, R.id.btn_cancel),
                                arrayOf<String?>(
                                    Lang.getString(if (deleteAndStop) R.string.DeleteAndStop else R.string.DeleteChat),
                                    Lang.getString(R.string.Cancel)
                                ),
                                intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                                intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
                                OptionDelegate { itemView: View?, resultId: Int ->
                                    if (resultId == R.id.btn_removeChatFromList) {
                                        if (blockUser) {
                                            tdlib.blockSender(
                                                MessageSenderUser(userId),
                                                BlockListMain(),
                                                Client.ResultHandler { blockResult: TdApi.Object? -> deleter.runWithBool(false) })
                                        } else {
                                            deleter.runWithBool(false)
                                        }
                                    }
                                    true
                                })
                        }
                    }, allowClearHistory, deleteAndStop, after
                )
            }

            TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                val userName = tdlib.cache().userFirstName(tdlib.chatUserId(chatId))
                showDeleteOrClearHistory(context, chatId, Lang.getStringBold(R.string.SecretChatWithUser, userName), Runnable {
                    val secretChat = tdlib.chatToSecretChat(chatId)
                    if (secretChat == null) {
                        return@Runnable
                    }
                    if (secretChat.state.getConstructor() == TdApi.SecretChatStateReady.CONSTRUCTOR && tdlib.canClearHistory(chatId) && (context is MessagesController || !tdlib.hasPasscode(
                            chatId
                        ))
                    ) {
                        val info = Lang.getStringBold(R.string.DeleteSecretChatConfirm, userName)
                        context.showSettings(
                            SettingsWrapBuilder(R.id.btn_removeChatFromList)
                                .setAllowResize(false)
                                .addHeaderItem(info)
                                .setRawItems(
                                    arrayOf<ListItem>(
                                        ListItem(
                                            ListItem.TYPE_CHECKBOX_OPTION,
                                            R.id.btn_clearChatHistory,
                                            0,
                                            Lang.getString(R.string.DeleteSecretChatHistoryForOtherParty, userName),
                                            R.id.btn_clearChatHistory,
                                            false
                                        )
                                    )
                                )
                                .setSaveColorId(ColorId.textNegative)
                                .setSaveStr(R.string.Delete)
                                .setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                                    val clearHistory = result!!.get(R.id.btn_clearChatHistory) == R.id.btn_clearChatHistory
                                    if (clearHistory) {
                                        tdlib.client().send(DeleteChatHistory(chatId, true, true), Client.ResultHandler { `object`: TdApi.Object? ->
                                            if (`object`!!.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                                                Log.e(
                                                    "Cannot clear secret chat history, secretChatId:%d, error: %s",
                                                    toSecretChatId(chatId),
                                                    TD.toErrorString(`object`)
                                                )
                                            }
                                            deleter.runWithBool(true)
                                        })
                                    } else {
                                        deleter.runWithBool(false)
                                    }
                                })
                        )
                    } else {
                        context.showOptions(
                            Lang.getStringBold(
                                if (secretChat.state.getConstructor() == TdApi.SecretChatStatePending.CONSTRUCTOR) R.string.DeleteSecretChatPendingConfirm else R.string.DeleteSecretChatClosedConfirm,
                                userName
                            ),
                            intArrayOf(R.id.btn_removeChatFromList, R.id.btn_cancel),
                            arrayOf<String?>(
                                Lang.getString(R.string.DeleteChat), Lang.getString(R.string.Cancel)
                            ),
                            intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                            intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
                            OptionDelegate { itemView: View?, id: Int ->
                                if (id == R.id.btn_removeChatFromList) {
                                    if (blockUser) {
                                        tdlib.blockSender(
                                            MessageSenderUser(secretChat.userId),
                                            BlockListMain(),
                                            Client.ResultHandler { blockResult: TdApi.Object? -> deleter.runWithBool(false) })
                                    } else {
                                        deleter.runWithBool(false)
                                    }
                                }
                                true
                            })
                    }
                }, allowClearHistory, blockUser, after)
            }

            TdApi.ChatTypeBasicGroup.CONSTRUCTOR -> {
                showDeleteOrClearHistory(context, chatId, tdlib.chatTitle(chatId), Runnable {
                    val status = tdlib.chatStatus(chatId)
                    if (status != null && TD.isMember(status, false)) {
                        leaveJoinChat(context, chatId, false, after)
                    } else {
                        context.showOptions(
                            Lang.getString(R.string.AreYouSureDeleteThisChat),
                            intArrayOf(R.id.btn_removeChatFromList, R.id.btn_cancel),
                            arrayOf<String?>(
                                Lang.getString(R.string.DeleteChat), Lang.getString(R.string.Cancel)
                            ),
                            intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                            intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
                            OptionDelegate { itemView: View?, id: Int ->
                                if (id == R.id.btn_removeChatFromList) {
                                    deleter.runWithBool(false)
                                }
                                true
                            })
                    }
                }, allowClearHistory, blockUser, after)
            }

            TdApi.ChatTypeSupergroup.CONSTRUCTOR -> {
                showDeleteOrClearHistory(
                    context,
                    chatId,
                    tdlib.chatTitle(chatId),
                    Runnable { leaveJoinChat(context, chatId, false, after) },
                    allowClearHistory,
                    blockUser,
                    after
                )
            }
        }
    }

    fun showArchiveOptions(context: ViewController<*>, archive: TdlibChatList) {
        val needMarkAsRead: Boolean = tdlib.hasUnreadChats(CHAT_LIST_ARCHIVE)
        val size = if (needMarkAsRead) 2 else 1

        val ids = IntList(size)
        val strings = StringList(size)
        val icons = IntList(size)

        if (needMarkAsRead) {
            ids.append(R.id.btn_markChatAsRead)
            strings.append(R.string.ArchiveRead)
            icons.append(Config.ICON_MARK_AS_READ)
        }

        val needHide = tdlib.settings().needHideArchive()

        ids.append(R.id.btn_pinUnpinChat)
        if (needHide) {
            icons.append(R.drawable.deproko_baseline_pin_24)
            strings.append(R.string.ArchivePin)
        } else {
            icons.append(R.drawable.baseline_arrow_upward_24)
            strings.append(R.string.ArchiveHide)
        }

        context.showOptions(
            Lang.pluralBold(R.string.xArchivedChats, archive.totalCount().toLong()),
            ids.get(),
            strings.get(),
            null,
            icons.get(),
            OptionDelegate { v: View?, optionId: Int ->
                if (optionId == R.id.btn_markChatAsRead) {
                    tdlib.readAllChats(
                        ChatListArchive(),
                        RunnableInt { readCount: Int -> UI.showToast(Lang.plural(R.string.ReadAllChatsDone, readCount.toLong()), Toast.LENGTH_SHORT) })
                } else if (optionId == R.id.btn_pinUnpinChat) {
                    tdlib.settings().toggleUserPreference(TdlibSettingsManager.PREFERENCE_HIDE_ARCHIVE)
                }
                true
            })
    }

    fun showChatOptions(
        context: ViewController<*>,
        chatList: ChatList,
        chatId: Long,
        messageThread: ThreadInfo?,
        source: TdApi.MessageSource?,
        canSelect: Boolean,
        isSelected: Boolean,
        onSelect: Runnable?
    ) {
        val chat = tdlib.chat(chatId)
        if (chat == null) return

        val available = tdlib.chatAvailable(chat)
        val position = chat.findPosition(chatList)
        if (!available) {
            if (position != null && position.source is ChatSourcePublicServiceAnnouncement) {
                showHidePsaConfirm(context, chatList, chatId, null)
            }
            return
        }

        val hasNotifications = tdlib.chatNotificationsEnabled(chat)

        val size = if (canSelect && onSelect != null) 8 else 7

        val ids = IntList(size)
        val strings = StringList(size)
        val colors = IntList(size)
        val icons = IntList(size)

        if (canSelect && onSelect != null) {
            ids.append(R.id.btn_selectChat)
            strings.append(if (isSelected) R.string.Unselect else R.string.Select)
            colors.append(ViewController.OptionColor.NORMAL)
            icons.append(R.drawable.baseline_playlist_add_check_24)
        }

        if (!tdlib.isSelfChat(chatId)) {
            ids.append(R.id.btn_notifications)
            strings.append(if (hasNotifications) R.string.MuteNotifications else R.string.EnableNotifications)
            colors.append(ViewController.OptionColor.NORMAL)
            icons.append(if (hasNotifications) R.drawable.baseline_notifications_off_24 else R.drawable.baseline_notifications_24)
        }

        if (position != null) {
            ids.append(if (position.isPinned) R.id.btn_unpinChat else R.id.btn_pinChat)
            strings.append(if (position.isPinned) R.string.UnpinFromTop else R.string.PinToTop)
            colors.append(ViewController.OptionColor.NORMAL)
            icons.append(if (position.isPinned) R.drawable.deproko_baseline_pin_undo_24 else R.drawable.deproko_baseline_pin_24)
        }

        if (tdlib.canArchiveOrUnarchiveChat(chat)) {
            val isArchived = tdlib.chatArchived(chat)
            ids.append(if (isArchived) R.id.btn_unarchiveChat else R.id.btn_archiveChat)
            strings.append(if (isArchived) R.string.UnarchiveChat else R.string.ArchiveChat)
            colors.append(ViewController.OptionColor.NORMAL)
            icons.append(if (isArchived) R.drawable.baseline_unarchive_24 else R.drawable.baseline_archive_24)
        }

        if (Settings.instance().chatFoldersEnabled()) {
            if (TD.isChatListMain(chatList) || TD.isChatListArchive(chatList)) {
                ids.append(R.id.btn_addChatToFolder)
                strings.append(R.string.AddToFolder)
                colors.append(ViewController.OptionColor.NORMAL)
                icons.append(R.drawable.templarian_baseline_folder_plus_24)
            } else if (TD.isChatListFolder(chatList)) {
                ids.append(R.id.btn_removeChatFromFolder)
                strings.append(R.string.RemoveFromFolder)
                colors.append(ViewController.OptionColor.NORMAL)
                icons.append(R.drawable.templarian_baseline_folder_remove_24)
            }
        }

        val hasPasscode = tdlib.hasPasscode(chat)

        val canRead = tdlib.canMarkAsRead(chat)
        if (!canRead || chat.unreadCount == 0 || !hasPasscode) { // when passcode is set, "mark as read" is unavailable, when there are some unread messages
            ids.append(if (canRead) R.id.btn_markChatAsRead else R.id.btn_markChatAsUnread)
            strings.append(if (canRead) R.string.MarkAsRead else R.string.MarkAsUnread)
            colors.append(ViewController.OptionColor.NORMAL)
            icons.append(if (canRead) Config.ICON_MARK_AS_READ else Config.ICON_MARK_AS_UNREAD)
        }

        if (!hasPasscode && tdlib.canClearHistory(chat)) {
            ids.append(R.id.btn_clearChatHistory)
            strings.append(R.string.ClearHistory)
            colors.append(ViewController.OptionColor.NORMAL)
            icons.append(R.drawable.templarian_baseline_broom_24)
        }

        colors.append(ViewController.OptionColor.RED)
        icons.append(R.drawable.baseline_delete_24)
        when (chat.type.getConstructor()) {
            TdApi.ChatTypePrivate.CONSTRUCTOR -> {
                strings.append(if (tdlib.isBotChat(chat)) R.string.DeleteAndStop else R.string.DeleteChat)
            }

            TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                strings.append(R.string.DeleteChat)
            }

            TdApi.ChatTypeBasicGroup.CONSTRUCTOR -> {
                val status = tdlib.chatStatus(chatId)
                strings.append(if (status != null && TD.isMember(status, false)) R.string.LeaveMegaMenu else R.string.DeleteChat)
            }

            TdApi.ChatTypeSupergroup.CONSTRUCTOR -> {
                strings.append(if (tdlib.isChannel(chatId)) R.string.LeaveChannel else R.string.LeaveMegaMenu)
            }
        }
        ids.append(R.id.btn_removeChatFromList)

        val chatName: CharSequence?
        when (chat.type.getConstructor()) {
            TdApi.ChatTypePrivate.CONSTRUCTOR -> {
                val userFirstName = tdlib.cache().userFirstName(tdlib.chatUserId(chat))
                chatName = if (tdlib.isSelfChat(chatId)) Lang.getString(R.string.ChatWithYourself) else Lang.getStringBold(R.string.ChatWithUser, userFirstName)
            }

            TdApi.ChatTypeSecret.CONSTRUCTOR -> {
                val userFirstName = tdlib.cache().userFirstName(tdlib.chatUserId(chat))
                chatName = Lang.getStringBold(R.string.SecretChatWithUser, userFirstName)
            }

            TdApi.ChatTypeBasicGroup.CONSTRUCTOR, TdApi.ChatTypeSupergroup.CONSTRUCTOR -> chatName = chat.title
            else -> throw IllegalArgumentException()
        }

        context.showOptions(chatName, ids.get(), strings.get(), colors.get(), icons.get(), OptionDelegate { itemView: View?, id: Int ->
            if (id == R.id.btn_selectChat) {
                onSelect!!.run()
                return@OptionDelegate true
            }
            processChatAction(context, chatList, chatId, messageThread, source, id, null)
        })
    }

    private fun showPinUnpinConfirm(context: ViewController<*>, chatList: ChatList, chatId: Long, source: TdApi.MessageSource?, after: Runnable?) {
        val isPinned = tdlib.chatPinned(chatList, chatId)
        context.showOptions(
            tdlib.chatTitle(chatId),
            intArrayOf(if (isPinned) R.id.btn_unpinChat else R.id.btn_pinChat, R.id.btn_cancel),
            arrayOf<String?>(Lang.getString(if (isPinned) R.string.UnpinFromTop else R.string.PinToTop), Lang.getString(R.string.Cancel)),
            null,
            intArrayOf(if (isPinned) R.drawable.deproko_baseline_pin_undo_24 else R.drawable.deproko_baseline_pin_24, R.drawable.baseline_cancel_24),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_unpinChat || id == R.id.btn_pinChat) {
                    processChatAction(context, chatList, chatId, null, source, id, after)
                }
                true
            })
    }

    private fun showArchiveUnarchiveChat(context: ViewController<*>, chatList: ChatList, chatId: Long, source: TdApi.MessageSource?, after: Runnable?) {
        val isUnarchive = tdlib.chatArchived(chatId)
        val title = tdlib.chatTitleShort(chatId)
        val isUserChat = tdlib.isUserChat(chatId)
        checkNeedArchiveInFolderHint(chatList, isUnarchive, RunnableBool { needHint: Boolean ->
            val hint: CharSequence?
            if (needHint) {
                hint = Lang.getStringBold(
                    if (isUnarchive) (if (isUserChat) R.string.UnarchiveXInFolder_user else R.string.UnarchiveXInFolder_chat) else (if (isUserChat) R.string.ArchiveXInFolder_user else R.string.ArchiveXInFolder_chat),
                    title
                )
            } else {
                hint = Lang.getStringBold(
                    if (isUnarchive) (if (isUserChat) R.string.UnarchiveX_user else R.string.UnarchiveX_chat) else (if (isUserChat) R.string.ArchiveX_user else R.string.ArchiveX_chat),
                    title
                )
            }
            context.showOptions(
                hint,
                intArrayOf(if (isUnarchive) R.id.btn_unarchiveChat else R.id.btn_archiveChat, R.id.btn_cancel),
                arrayOf<String?>(Lang.getString(if (isUnarchive) R.string.UnarchiveChat else R.string.ArchiveChat), Lang.getString(R.string.Cancel)),
                null,
                intArrayOf(if (isUnarchive) R.drawable.baseline_unarchive_24 else R.drawable.baseline_archive_24, R.drawable.baseline_cancel_24),
                OptionDelegate { itemView: View?, id: Int ->
                    if (id == R.id.btn_unarchiveChat || id == R.id.btn_archiveChat) {
                        processChatAction(context, chatList, chatId, null, source, id, after)
                    }
                    true
                })
        })
    }

    fun showInviteLinkOptionsPreload(
        context: ViewController<*>,
        link: ChatInviteLink,
        chatId: Long,
        showNavigatingToLinks: Boolean,
        onLinkDeleted: Runnable?,
        onLinkRevoked: RunnableData<ChatInviteLinks?>?
    ) {
        context.tdlib()!!
            .send<ChatInviteLink?>(GetChatInviteLink(chatId, link.inviteLink), Tdlib.ResultHandler { inviteLink: ChatInviteLink?, error: TdApi.Error? ->
                context.runOnUiThreadOptional(
                    Runnable {
                        if (error != null) {
                            showInviteLinkOptions(context, link, chatId, showNavigatingToLinks, true, onLinkDeleted, onLinkRevoked)
                        } else {
                            showInviteLinkOptions(context, inviteLink!!, chatId, showNavigatingToLinks, false, onLinkDeleted, onLinkRevoked)
                        }
                    })
            })
    }

    fun showInviteLinkOptions(
        context: ViewController<*>,
        link: ChatInviteLink,
        chatId: Long,
        showNavigatingToLinks: Boolean,
        deleted: Boolean,
        onLinkDeleted: Runnable?,
        onLinkRevoked: RunnableData<ChatInviteLinks?>?
    ) {
        val chat = tdlib.chat(chatId)

        val strings = StringList(6)
        val icons = IntList(6)
        val ids = IntList(6)
        val colors = IntList(6)

        if (!deleted && link.memberCount > 0) {
            ids.append(R.id.btn_viewInviteLinkMembers)
            strings.append(R.string.InviteLinkViewMembers)
            icons.append(R.drawable.baseline_visibility_24)
            colors.append(ViewController.OptionColor.NORMAL)
        }

        if (!deleted && link.createsJoinRequest && link.pendingJoinRequestCount > 0) {
            ids.append(R.id.btn_manageJoinRequests)
            strings.append(R.string.InviteLinkViewRequests)
            icons.append(R.drawable.baseline_pending_24)
            colors.append(ViewController.OptionColor.NORMAL)
        }

        if (showNavigatingToLinks && tdlib.canManageInviteLinks(chat)) {
            ids.append(R.id.btn_manageInviteLinks)
            strings.append(R.string.InviteLinkManage)
            icons.append(R.drawable.baseline_add_link_24)
            colors.append(ViewController.OptionColor.NORMAL)
        }

        if (!deleted && !link.isRevoked) {
            if (!link.isPrimary && context is ChatLinksController) {
                ids.append(R.id.btn_edit)
                strings.append(R.string.InviteLinkEdit)
                icons.append(R.drawable.baseline_edit_24)
                colors.append(ViewController.OptionColor.NORMAL)
            }

            ids.append(R.id.btn_copyLink)
            strings.append(R.string.InviteLinkCopy)
            icons.append(R.drawable.baseline_content_copy_24)
            colors.append(ViewController.OptionColor.NORMAL)

            ids.append(R.id.btn_shareLink)
            strings.append(R.string.ShareLink)
            icons.append(R.drawable.baseline_forward_24)
            colors.append(ViewController.OptionColor.NORMAL)

            icons.append(R.drawable.baseline_link_off_24)
            ids.append(R.id.btn_revokeLink)
            strings.append(R.string.RevokeLink)
            colors.append(ViewController.OptionColor.RED)
        } else {
            ids.append(R.id.btn_copyLink)
            strings.append(R.string.InviteLinkCopy)
            icons.append(R.drawable.baseline_content_copy_24)
            colors.append(ViewController.OptionColor.NORMAL)

            if (!deleted) {
                icons.append(R.drawable.baseline_delete_24)
                ids.append(R.id.btn_deleteLink)
                strings.append(R.string.InviteLinkDelete)
                colors.append(ViewController.OptionColor.RED)
            }
        }

        val info = TD.makeClickable(
            Lang.getString(
                R.string.CreatedByXOnDate,
                (SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, spanIndex: Int, needFakeBold: Boolean ->
                    if (spanIndex == 0) Lang.newUserSpan(
                        TdlibContext(context.context(), context.tdlib()),
                        link.creatorUserId
                    ) else null
                }),
                context.tdlib()!!.cache().userName(link.creatorUserId),
                Lang.getRelativeTimestamp(link.date.toLong(), TimeUnit.SECONDS)
            )
        )
        val firstBoldCreator = SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, spanIndex: Int, needFakeBold: Boolean ->
            if (spanIndex == 0) Lang.newBoldSpan(needFakeBold) else null
        }
        val desc: CharSequence?

        if (link.name != null && !link.name.isEmpty()) {
            desc = Lang.getString(R.string.format_nameAndSubtitleAndStatus, firstBoldCreator, link.inviteLink, link.name, info)
        } else {
            desc = Lang.getString(R.string.format_nameAndStatus, firstBoldCreator, link.inviteLink, info)
        }

        context.showOptions(desc, ids.get(), strings.get(), colors.get(), icons.get(), OptionDelegate { itemView: View?, id: Int ->
            if (id == R.id.btn_viewInviteLinkMembers) {
                val c2 = ChatLinkMembersController(context.context(), context.tdlib())
                c2.setArguments(ChatLinkMembersController.Args(chatId, link.inviteLink))
                context.navigateTo(c2)
            } else if (id == R.id.btn_manageJoinRequests) {
                val c3 = ChatJoinRequestsController(context.context(), context.tdlib())
                c3.setArguments(ChatJoinRequestsController.Args(chatId, link.inviteLink, context))
                context.navigateTo(c3)
            } else if (id == R.id.btn_edit) {
                val c = EditChatLinkController(context.context(), context.tdlib())
                c.setArguments(EditChatLinkController.Args(link, chatId, context as ChatLinksController))
                context.navigateTo(c)
            } else if (id == R.id.btn_manageInviteLinks) {
                val cc = ChatLinksController(context.context(), context.tdlib())
                cc.setArguments(
                    ChatLinksController.Args(
                        chatId,
                        context.tdlib()!!.myUserId(),
                        null,
                        null,
                        tdlib.chatStatus(chatId)!!.getConstructor() == ChatMemberStatusCreator.CONSTRUCTOR
                    )
                )
                context.navigateTo(cc)
            } else if (id == R.id.btn_copyLink) {
                UI.copyText(link.inviteLink, R.string.CopiedLink)
            } else if (id == R.id.btn_shareLink) {
                val chatName = context.tdlib()!!.chatTitle(chatId)
                val exportText = Lang.getString(
                    if (context.tdlib()!!.isChannel(chatId)) R.string.ShareTextChannelLink else R.string.ShareTextChatLink,
                    chatName,
                    link.inviteLink
                )
                val text = Lang.getString(R.string.ShareTextLink, chatName, link.inviteLink)
                val sc = ShareController(context.context(), context.tdlib())
                sc.setArguments(ShareController.Args(text).setShare(exportText, null))
                sc.show()
            } else if (id == R.id.btn_deleteLink) {
                context.showOptions(
                    Lang.getString(R.string.AreYouSureDeleteInviteLink),
                    intArrayOf(R.id.btn_deleteLink, R.id.btn_cancel),
                    arrayOf<String?>(Lang.getString(R.string.InviteLinkDelete), Lang.getString(R.string.Cancel)),
                    intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                    intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
                    OptionDelegate { itemView2: View?, id2: Int ->
                        if (id2 == R.id.btn_deleteLink) {
                            if (onLinkDeleted != null) onLinkDeleted.run()
                            context.tdlib()!!.send<TdApi.Ok?>(DeleteRevokedChatInviteLink(chatId, link.inviteLink), tdlib.typedOkHandler())
                        }
                        true
                    })
            } else if (id == R.id.btn_revokeLink) {
                context.showOptions(
                    Lang.getString(
                        if (context.tdlib()!!.isChannel(chatId)) R.string.AreYouSureRevokeInviteLinkChannel else R.string.AreYouSureRevokeInviteLinkGroup
                    ),
                    intArrayOf(R.id.btn_revokeLink, R.id.btn_cancel),
                    arrayOf<String?>(
                        Lang.getString(R.string.RevokeLink), Lang.getString(R.string.Cancel)
                    ),
                    intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                    intArrayOf(R.drawable.baseline_link_off_24, R.drawable.baseline_cancel_24),
                    OptionDelegate { itemView2: View?, id2: Int ->
                        if (id2 == R.id.btn_revokeLink) {
                            context.tdlib()!!.client().send(RevokeChatInviteLink(chatId, link.inviteLink), Client.ResultHandler { result: TdApi.Object? ->
                                if (result!!.getConstructor() == ChatInviteLinks.CONSTRUCTOR && onLinkRevoked != null) {
                                    context.runOnUiThreadOptional(Runnable { onLinkRevoked.runWithData(result as ChatInviteLinks) })
                                }
                            })
                        }
                        true
                    })
            }
            true
        })
    }

    fun showAddChatToFolderOptions(context: ViewController<*>, chatId: Long, after: Runnable?) {
        showAddChatsToFolderOptions(context, longArrayOf(chatId), after)
    }

    fun showAddChatsToFolderOptions(context: ViewController<*>, chatIds: LongArray, after: Runnable?) {
        if (chatIds.size == 0) return

        val chatFolders = tdlib.chatFolders() ?: emptyArray()
        val items: MutableList<ListItem?> = ArrayList<ListItem?>(chatFolders.size + 1)
        for (chatFolderInfo in chatFolders) {
            items.add(
                ListItem(
                    ListItem.TYPE_SETTING,
                    R.id.chatFolder,
                    TD.findFolderIcon(chatFolderInfo.icon, R.drawable.baseline_folder_24),
                    TD.toCharSequence(chatFolderInfo.name)
                ).setIntValue(chatFolderInfo.id)
            )
        }
        if (tdlib.canCreateChatFolder()) {
            items.add(
                ListItem(
                    ListItem.TYPE_SETTING,
                    R.id.btn_createNewFolder,
                    R.drawable.baseline_create_new_folder_24,
                    R.string.CreateNewFolder
                ).setTextColorId(ColorId.textNeutral)
            )
        }
        val settings = arrayOfNulls<SettingsWrap>(1)
        settings[0] = context.showSettings(
            SettingsWrapBuilder(R.id.btn_addChatToFolder)
            .addHeaderItem(Lang.getString(R.string.ChooseFolder))
            .setRawItems(items)
            .setNeedSeparators(false)
            .setDisableFooter(true)
            .setNeedRootInsets(true)
            .setSettingProcessor(CustomSettingProcessor { item: ListItem?, view: SettingView?, isUpdate: Boolean ->
                view!!.setIconColorId(if (item!!.getId() == R.id.btn_createNewFolder) ColorId.inlineIcon else ColorId.NONE)
            })
            .setOnSettingItemClick(object : ViewController.OnSettingItemClick { override fun onSettingItemClick(view: View?, id: Int, item: ListItem?, done: TextView?, adapter: SettingsAdapter?, window: PopupLayout?) {
                settings[0]!!.window.hideWindow(true)
                if (item!!.getId() == R.id.btn_createNewFolder) {
                    val chatFolder = TD.newChatFolder(chatIds)
                    context.context().navigation().navigateTo(EditChatFolderController.newFolder(context.context(), tdlib, chatFolder))
                } else {
                    val chatFolderId = item.getIntValue()
                    addChatsToChatFolder(context, chatFolderId, chatIds)
                }
                if (after != null) {
                    after.run()
                }
            }})
        )
    }

    fun showDeleteChatFolderOrLeaveChats(context: ViewController<*>, chatFolderId: Int) {
        val info = tdlib.chatFolderInfo(chatFolderId) ?: return
        if (info.isShareable) {
            tdlib.send<Chats?>(GetChatFolderChatsToLeave(chatFolderId), Tdlib.ResultHandler { result: Chats?, error: TdApi.Error? ->
                post(Runnable {
                    if (error != null) {
                        UI.showError(error)
                    } else if (result!!.totalCount > 0) {
                        val controller = ChatFolderInviteLinkController(context.context(), tdlib)
                        controller.setArguments(ChatFolderInviteLinkController.Arguments.deleteFolder(info, result.chatIds))
                        controller.show()
                    } else {
                        showDeleteChatFolderConfirm(context, chatFolderId, info.hasMyInviteLinks)
                    }
                })
            })
        } else {
            showDeleteChatFolderConfirm(context, chatFolderId, info.hasMyInviteLinks)
        }
    }

    private fun showDeleteChatFolderConfirm(context: ViewController<*>, chatFolderId: Int, hasMyInviteLinks: Boolean) {
        tdlib.ui().showDeleteChatFolderConfirm(context, hasMyInviteLinks, Runnable {
            tdlib.deleteChatFolder(chatFolderId, null, null)
        })
    }

    fun showDeleteChatFolderConfirm(context: ViewController<*>, hasMyInviteLinks: Boolean, after: Runnable) {
        // TODO(nikita-toropov) wording
        val infoRes = if (hasMyInviteLinks) R.string.DeleteFolderWithInviteLinksConfirm else R.string.RemoveFolderConfirm
        val actionRes = if (hasMyInviteLinks) R.string.Delete else R.string.Remove
        context.showConfirm(
            Lang.getMarkdownString(context, infoRes),
            Lang.getString(actionRes),
            R.drawable.baseline_delete_24,
            ViewController.OptionColor.RED,
            after
        )
    }

    fun addChatsToChatFolder(delegate: TdlibDelegate?, chatFolderId: Int, chatIds: LongArray) {
        if (chatIds.size == 0) {
            return
        }
        tdlib.send<ChatFolder?>(GetChatFolder(chatFolderId), Tdlib.ResultHandler { chatFolder: ChatFolder?, error: TdApi.Error? ->
            if (error != null) {
                UI.showError(chatFolder)
            } else {
                addChatsToChatFolderImpl(delegate, chatFolderId, chatFolder!!, chatIds)
            }
        })
    }

    fun addChatsToChatFolderImpl(delegate: TdlibDelegate?, chatFolderId: Int, chatFolder: ChatFolder, chatIds: LongArray) {
        if (chatIds.size == 0) {
            return
        }
        val pinnedChatIds = LongSet(chatFolder.pinnedChatIds)
        val includedChatIds = LongSet(chatFolder.includedChatIds)
        for (chatId in chatIds) {
            if (pinnedChatIds.has(chatId) || includedChatIds.has(chatId)) {
                continue
            }
            includedChatIds.add(chatId)
        }
        if (includedChatIds.size() == chatFolder.includedChatIds.size) {
            return
        }
        val chatCount = pinnedChatIds.size() + includedChatIds.size()
        var secretChatCount = 0
        for (pinnedChatId in pinnedChatIds) {
            if (isSecret(pinnedChatId)) secretChatCount++
        }
        for (includedChatId in includedChatIds) {
            if (isSecret(includedChatId)) secretChatCount++
        }
        val nonSecretChatCount = chatCount - secretChatCount
        val chosenChatCountMax = tdlib.chatFolderChosenChatCountMax().toLong()
        if (secretChatCount > chosenChatCountMax || nonSecretChatCount > chosenChatCountMax) {
            checkPremiumLimit(PremiumLimitTypeChatFolderChosenChatCount(), PremiumLimitCallback { currentLimit: Int, premiumLimit: Int ->
                // FIXME: use tdlib.ui().showPremiumAlert()?
                val text: CharSequence?
                if (currentLimit < premiumLimit) {
                    text = Lang.getMarkdownPlural(
                        delegate,
                        R.string.PremiumLimitChatsInFolder,
                        currentLimit.toLong(),
                        Lang.boldCreator(),
                        Strings.buildCounter(premiumLimit.toLong())
                    )
                } else {
                    text = Lang.getMarkdownPlural(delegate, R.string.LimitChatsInFolder, currentLimit.toLong(), Lang.boldCreator())
                }
                UI.showCustomToast(text, Toast.LENGTH_LONG, 0)
            })
            return
        }
        chatFolder.includedChatIds = includedChatIds.toArray()
        chatFolder.excludedChatIds = removeAll(chatFolder.excludedChatIds, chatIds)
        tdlib.send<ChatFolderInfo?>(EditChatFolder(chatFolderId, chatFolder), Tdlib.ResultHandler { chatFolderInfo: ChatFolderInfo?, error: TdApi.Error? ->
            if (error != null) {
                UI.showError(error)
            }
        })
    }

    fun showArchiveHint(chatList: ChatList, chatsCount: Int, isUnarchive: Boolean) {
        if (chatList.getConstructor() != ChatListFolder.CONSTRUCTOR) return
        UI.showToast(Lang.pluralBold(if (isUnarchive) R.string.UnarchivedXChats else R.string.ArchivedXChats, chatsCount.toLong()), Toast.LENGTH_SHORT)
    }

    fun checkNeedArchiveInFolderHint(chatList: ChatList, isUnarchive: Boolean, after: RunnableBool) {
        if (chatList.getConstructor() != ChatListFolder.CONSTRUCTOR) {
            after.runWithBool(false)
            return
        }
        if (isUnarchive) {
            after.runWithBool(true)
            return
        }
        tdlib.send<ChatFolder?>(GetChatFolder((chatList as ChatListFolder).chatFolderId), Tdlib.ResultHandler { chatFolder: ChatFolder?, error: TdApi.Error? ->
            if (chatFolder != null) {
                post(Runnable {
                    after.runWithBool(!chatFolder.excludeArchived)
                })
            }
        })
    }

    fun processChatAction(
        context: ViewController<*>,
        chatList: ChatList,
        chatId: Long,
        messageThread: ThreadInfo?,
        source: TdApi.MessageSource?,
        actionId: Int,
        after: Runnable?
    ): Boolean {
        val chat = tdlib.chat(chatId)
        if (chat == null) return false
        if (actionId == R.id.btn_notifications) {
            tdlib.ui().toggleMute(context, chatId, false, after)
            return true
        } else if (actionId == R.id.btn_pinUnpinChat) {
            showPinUnpinConfirm(context, chatList, chatId, source, after)
            return true
        } else if (actionId == R.id.btn_unpinChat) {
            tdlib.client().send(ToggleChatIsPinned(chatList, chatId, false), tdlib.okHandler(after))
            return true
        } else if (actionId == R.id.btn_pinChat) {
            tdlib.client().send(ToggleChatIsPinned(chatList, chatId, true), tdlib.okHandler(after))
            return true
        } else if (actionId == R.id.btn_archiveUnarchiveChat) {
            showArchiveUnarchiveChat(context, chatList, chatId, source, after)
            return true
        } else if (actionId == R.id.btn_archiveChat || actionId == R.id.btn_unarchiveChat) {
            val isUnarchive = actionId == R.id.btn_unarchiveChat
            val targetChatList: ChatList = if (isUnarchive) CHAT_LIST_MAIN else CHAT_LIST_ARCHIVE
            val archiveCallback: Runnable = Runnable {
                showArchiveHint(chatList, 1, isUnarchive)
                if (after != null) {
                    after.run()
                }
            }
            tdlib.send<TdApi.Ok?>(AddChatToList(chatId, targetChatList), tdlib.typedOkHandler(archiveCallback))
            return true
        } else if (actionId == R.id.btn_markChatAsRead) {
            if (messageThread != null) {
                tdlib.markChatAsRead(messageThread.getChatId(), source!!, false, after)
            } else {
                tdlib.markChatAsRead(chat.id, source!!, true, after)
            }
            return true
        } else if (actionId == R.id.btn_markChatAsUnread) {
            tdlib.markChatAsUnread(chat, after)
            return true
        } else if (actionId == R.id.btn_phone_call) {
            tdlib.context().calls().makeCallDelayed(context, TD.getUserId(chat), null, true)
            return true
        } else if (actionId == R.id.btn_addChatToFolder) {
            showAddChatToFolderOptions(context, chatId,  /* after */null)
            return true
        } else if (actionId == R.id.btn_removeChatFromFolder) {
            if (TD.isChatListFolder(chatList)) {
                val chatFolderId = (chatList as ChatListFolder).chatFolderId
                removeChatFromChatFolder(chatFolderId, chatId)
            }
            return true
        }
        return processLeaveButton(context, chatList, chatId, actionId, after)
    }

    fun removeChatFromChatFolder(chatFolderId: Int, chatId: Long) {
        removeChatsFromChatFolder(chatFolderId, longArrayOf(chatId))
    }

    fun removeChatsFromChatFolder(chatFolderId: Int, chatIds: LongArray) {
        if (chatIds.size == 0) {
            return
        }
        tdlib.send<ChatFolder?>(GetChatFolder(chatFolderId), Tdlib.ResultHandler { chatFolder: ChatFolder?, error: TdApi.Error? ->
            if (error != null) {
                UI.showError(error)
            } else {
                removeChatsFromChatFolderImpl(chatFolderId, chatFolder!!, chatIds)
            }
        })
    }

    private fun removeChatsFromChatFolderImpl(chatFolderId: Int, chatFolder: ChatFolder, chatIds: LongArray) {
        if (chatIds.size == 0) {
            return
        }
        val pinnedChatIds = LongList(chatFolder.pinnedChatIds)
        val includedChatIds = LongSet(chatFolder.includedChatIds)
        val excludedChatIds = LongSet(chatFolder.excludedChatIds)
        for (chatId in chatIds) {
            val removed = pinnedChatIds.remove(chatId) or includedChatIds.remove(chatId)
            if (removed && Config.CHAT_FOLDERS_SMART_CHAT_DELETION_ENABLED) {
                val chat = tdlib.chat(chatId)
                val isBotChat = tdlib.isBotChat(chatId)
                val isUserChat = tdlib.isUserChat(chat) && !isBotChat
                val isContactChat = isUserChat && tdlib.isContactChat(chat)
                if (!chatFolder.includeContacts && isUserChat && isContactChat) continue
                if (!chatFolder.includeNonContacts && isUserChat && !isContactChat) continue
                if (!chatFolder.includeGroups && TD.isMultiChat(chat)) continue
                if (!chatFolder.includeChannels && tdlib.isChannelChat(chat)) continue
                if (!chatFolder.includeBots && isBotChat) continue
            }
            excludedChatIds.add(chatId)
        }
        chatFolder.pinnedChatIds = pinnedChatIds.get()
        chatFolder.includedChatIds = includedChatIds.toArray()
        chatFolder.excludedChatIds = excludedChatIds.toArray()
        tdlib.send<ChatFolderInfo?>(EditChatFolder(chatFolderId, chatFolder), Tdlib.ResultHandler { chatFolderInfo: ChatFolderInfo?, error: TdApi.Error? ->
            if (error != null) {
                UI.showError(error)
            }
        })
    }

    fun createSimpleChatActions(
        context: ViewController<*>,
        chatList: ChatList,
        chatId: Long,
        messageThread: ThreadInfo?,
        source: TdApi.MessageSource?,
        ids: IntList,
        icons: IntList,
        strings: StringList,
        allowInteractions: Boolean,
        canSelect: Boolean,
        isSelected: Boolean,
        onSelect: Runnable?
    ): ForceTouchView.ActionListener? {
        val chat = tdlib.chat(chatId)
        if (chat == null) {
            return null
        }
        val position = chat.findPosition(chatList)
        val hasSelect = canSelect && onSelect != null
        if (allowInteractions) {
            if (tdlib.chatAvailable(chat)) {
                val userId = TD.getUserId(chat)
                if (!tdlib.isSelfUserId(userId)) {
                    if (userId != 0L) {
                        if (Config.CALL_FROM_PREVIEW && tdlib.cache().userGeneral(userId)) {
                            ids.append(R.id.btn_phone_call)
                            strings.append(R.string.Call)
                            icons.append(R.drawable.baseline_call_24)
                        }
                    }
                    val hasNotifications = tdlib.chatNotificationsEnabled(chat.id)
                    ids.append(R.id.btn_notifications)
                    strings.append(if (hasNotifications) R.string.Mute else R.string.Unmute)
                    icons.append(if (hasNotifications) R.drawable.baseline_notifications_off_24 else R.drawable.baseline_notifications_24)
                }

                if (!hasSelect && position != null) {
                    ids.append(R.id.btn_pinUnpinChat)
                    strings.append(if (position.isPinned) R.string.Unpin else R.string.Pin)
                    icons.append(if (position.isPinned) R.drawable.deproko_baseline_pin_undo_24 else R.drawable.deproko_baseline_pin_24)
                }

                val canRead = tdlib.canMarkAsRead(chat)
                ids.append(if (canRead) R.id.btn_markChatAsRead else R.id.btn_markChatAsUnread)
                strings.append(if (canRead) R.string.MarkAsRead else R.string.MarkAsUnread)
                icons.append(if (canRead) Config.ICON_MARK_AS_READ else Config.ICON_MARK_AS_UNREAD)

                if (tdlib.canArchiveOrUnarchiveChat(chat)) {
                    val isArchived = tdlib.chatArchived(chat)
                    ids.append(R.id.btn_archiveUnarchiveChat)
                    strings.append(if (isArchived) R.string.Unarchive else R.string.Archive)
                    icons.append(if (isArchived) R.drawable.baseline_unarchive_24 else R.drawable.baseline_archive_24)
                }

                ids.append(R.id.btn_removeChatFromListOrClearHistory)
                strings.append(R.string.Delete)
                icons.append(R.drawable.baseline_delete_24)
            } else if (position != null && position.source is ChatSourcePublicServiceAnnouncement) {
                ids.append(R.id.btn_removePsaChatFromList)
                strings.append(R.string.PsaHide)
                icons.append(R.drawable.baseline_delete_sweep_24)
            }
        }
        if (hasSelect) {
            ids.append(R.id.btn_selectChat)
            if (ids.size() > 1) {
                strings.append(R.string.MoreChatOptions)
                icons.append(R.drawable.baseline_more_horiz_24)
            } else {
                strings.append(if (isSelected) R.string.Unselect else R.string.Select)
                icons.append(R.drawable.baseline_playlist_add_check_24)
            }
        }

        return object : ForceTouchView.ActionListener {
            override fun onForceTouchAction(c: ForceTouchContext?, actionId: Int, arg: Any?) {
                if (actionId == R.id.btn_selectChat) {
                    onSelect!!.run()
                } else {
                    processChatAction(context, chatList, chatId, messageThread, source, actionId, null)
                }
            }

            override fun onAfterForceTouchAction(c: ForceTouchContext?, actionId: Int, arg: Any?) {}
        }
    }

    // Passcode
    private fun showPasscodeOptions(controller: ViewController<*>, chatId: Long) {
        val chat = tdlib.chat(chatId)
        if (chat == null || !tdlib.canSetPasscode(chat)) {
            return
        }
        val c = PasscodeSetupController(controller.context(), controller.tdlib())
        c.setArguments(PasscodeSetupController.Args(chat, tdlib.chatPasscode(chat)))
        controller.navigateTo(c)
    }

    // Map provider settings
    private var currentMapProviderWrap: PopupLayout? = null
    private var currentMapProviderCallbacks: MutableList<Runnable>? = null

    fun showMapProviderSettings(c: ViewController<*>, mode: Int, after: Runnable?) {
        if (!c.isFocused()) {
            c.addFocusListener(object : FocusStateListener {
                override fun onFocusStateChanged(c: ViewController<*>, isFocused: Boolean) {
                    if (isFocused) {
                        c.removeFocusListener(this)
                        showMapProviderSettings(c, mode, after)
                    }
                }
            })
            return
        }
        if (mode == MAP_PROVIDER_MODE_SECRET_TUTORIAL) {
            if (currentMapProviderWrap != null && currentMapProviderWrap!!.getContext() === c.context()) {
                if (after != null) {
                    currentMapProviderCallbacks!!.add(after)
                }
                return
            }
        }
        val b = SettingsWrapBuilder(R.id.btn_mapProvider)
        if (mode == MAP_PROVIDER_MODE_SECRET_TUTORIAL) {
            b.addHeaderItem(Lang.getString(R.string.MapPreviewProviderHint))
            b.setDismissListener(PopupLayout.DismissListener { popupLayout: PopupLayout? ->
                if (currentMapProviderWrap != null) {
                    currentMapProviderWrap = null
                    currentMapProviderCallbacks!!.clear()
                    currentMapProviderCallbacks = null
                }
            })
        }
        val type = Settings.instance().getMapProviderType(mode == MAP_PROVIDER_MODE_CLOUD)
        if (mode == MAP_PROVIDER_MODE_CLOUD) {
            b.setRawItems(
                arrayOf<ListItem>(
                    // new SettingItem(SettingItem.TYPE_RADIO_OPTION, R.id.btn_mapProviderGoogle, 0, R.string.MapPreviewProviderGoogle, R.id.btn_mapProvider, type == Settings.MAP_PROVIDER_GOOGLE),
                    ListItem(
                        ListItem.TYPE_RADIO_OPTION,
                        R.id.btn_mapProviderTelegram,
                        0,
                        R.string.MapPreviewProviderTelegram,
                        R.id.btn_mapProvider,
                        type == Settings.MAP_PROVIDER_TELEGRAM
                    ),
                )
            )
        } else {
            b.setRawItems(
                arrayOf<ListItem>(
                    // new SettingItem(SettingItem.TYPE_RADIO_OPTION, R.id.btn_mapProviderGoogle, 0, R.string.MapPreviewProviderGoogle, R.id.btn_mapProvider, type == Settings.MAP_PROVIDER_GOOGLE),
                    ListItem(
                        ListItem.TYPE_RADIO_OPTION,
                        R.id.btn_mapProviderTelegram,
                        0,
                        R.string.MapPreviewProviderTelegram,
                        R.id.btn_mapProvider,
                        type == Settings.MAP_PROVIDER_TELEGRAM || (type == Settings.MAP_PROVIDER_UNSET && mode == MAP_PROVIDER_MODE_SECRET_TUTORIAL)
                    ),
                    ListItem(
                        ListItem.TYPE_RADIO_OPTION,
                        R.id.btn_mapProviderNone,
                        0,
                        R.string.MapPreviewProviderNone,
                        R.id.btn_mapProvider,
                        type == Settings.MAP_PROVIDER_NONE
                    ),
                )
            )
        }
        val wrap = c.showSettings(b.setSaveStr(R.string.Save).setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
            val resultType: Int
            @IdRes val resultId = result!!.get(R.id.btn_mapProvider)
            if (resultId == R.id.btn_mapProviderGoogle) {
                resultType = Settings.MAP_PROVIDER_GOOGLE
            } else if (resultId == R.id.btn_mapProviderTelegram) {
                resultType = Settings.MAP_PROVIDER_TELEGRAM
            } else if (resultId == R.id.btn_mapProviderNone) {
                resultType = Settings.MAP_PROVIDER_NONE
            } else {
                return@SettingsIntDelegate
            }
            Settings.instance().setMapProviderType(resultType, mode == MAP_PROVIDER_MODE_CLOUD)
            if (mode == MAP_PROVIDER_MODE_SECRET_TUTORIAL && currentMapProviderWrap != null) {
                val callbacks = currentMapProviderCallbacks
                currentMapProviderWrap = null
                currentMapProviderCallbacks = null
                for (runnable in callbacks!!) {
                    runnable.run()
                }
            } else if (after != null) {
                after.run()
            }
        }))
        if (mode == MAP_PROVIDER_MODE_SECRET_TUTORIAL) {
            if (wrap != null) {
                currentMapProviderWrap = wrap.window
                currentMapProviderCallbacks = ArrayList<Runnable>()
                currentMapProviderCallbacks!!.add(after!!)
            } else {
                currentMapProviderWrap = null
                currentMapProviderCallbacks = null
            }
        }
    }

    class CustomLangPackResult(document: TdApi.Document) {
        lateinit var builtinStrings: Array<Array<String>>
        val strings: MutableMap<String?, LanguagePackStringValue?> = HashMap<String?, LanguagePackStringValue?>()
        var unknownStringsCount: Int = 0
        val fileName: String

        fun prepare() {
            builtinStrings = getAllKeys()
        }

        fun canBeInstalled(): Boolean {
            return !strings.isEmpty() && unknownStringsCount < strings.size && hasRequiredStrings()
        }

        fun hasRequiredStrings(): Boolean {
            val requiredStrings = Lang.getRequiredKeys()
            var hasRequiredStrings = true
            for (resId in requiredStrings) {
                val string = getString(resId)
                if (string !is LanguagePackStringValueOrdinary) {
                    hasRequiredStrings = false
                    Log.e("Language Pack is missing required string: %s", Lang.getResourceEntryName(resId))
                    continue
                }
                val value = string
                if (isEmpty(value.value.trim { it <= ' ' })) {
                    hasRequiredStrings = false
                    Log.e("Language Pack required string is empty: %s", Lang.getResourceEntryName(resId))
                    continue
                }
                if (resId == R.string.language_code && value.value.get(0) == 'X') {
                    hasRequiredStrings = false
                    Log.e("Language Pack language_code starts with 'X': %s", value.value)
                    continue
                }
            }
            return hasRequiredStrings
        }

        fun getString(@StringRes resId: Int): LanguagePackStringValue? {
            val key = Lang.getResourceEntryName(resId)
            return strings.get(key)
        }

        fun getStringValue(@StringRes resId: Int): String? {
            val key = Lang.getResourceEntryName(resId)
            val string = strings.get(key)
            return if (string is LanguagePackStringValueOrdinary) string.value else null
        }

        val languageCode: String
            get() {
                val code = getStringValue(R.string.language_code)
                return "X" + code
            }

        private var sourceName: String? = null

        init {
            this.fileName = document.fileName
        }

        fun setSourceName(sourceName: String?): CustomLangPackResult {
            this.sourceName = sourceName
            return this
        }

        val languageNameInEnglish: String?
            get() {
                val name = getStringValue(R.string.language_nameInEnglish)
                if (isEmpty(fileName)) return name
                val b = StringBuilder(name).append(" [")
                if (fileName.endsWith(".xml")) b.append(fileName, 0, fileName.length - ".xml".length)
                else b.append(fileName)
                /*if (!Strings.isEmpty(sourceName))
      b.append(", source: ").append(sourceName);*/
                return b.append("]").toString()
            }

        fun getStrings(): Array<LanguagePackString?> {
            val strings = arrayOfNulls<LanguagePackString>(this.strings.size)
            var i = 0
            for (string in this.strings.entries) {
                strings[i++] = LanguagePackString(string.key, string.value)
            }
            return strings
        }

        val stringsCount: Int
            get() = strings.size - unknownStringsCount

        val builtinStringsCount: Int
            get() = builtinStrings[0]!!.size + builtinStrings[1]!!.size

        val completenessPercentage: Int
            get() = floor((this.stringsCount.toFloat() / this.builtinStringsCount.toFloat() * 100f).toDouble()).toInt()

        val missingStringsCount: Int
            get() {
                val missingCount = this.builtinStringsCount - this.stringsCount
                if (missingCount > 0) {
                    val missingStrings: MutableList<String?> = ArrayList<String?>()
                    for (key in builtinStrings[0]!!) {
                        if (strings.get(key) == null) {
                            missingStrings.add(key)
                        }
                    }
                    for (key in builtinStrings[1]!!) {
                        if (strings.get(key) == null) {
                            missingStrings.add(key)
                        }
                    }
                    if (!missingStrings.isEmpty()) {
                        Log.e(
                            "Language pack %s misses following strings: %s",
                            fileName,
                            TextUtils.join(", ", missingStrings)
                        )
                    }
                }
                return missingCount
            }

        val isComplete: Boolean
            get() = this.stringsCount == this.builtinStringsCount
    }

    fun readCustomLanguage(c: ViewController<*>?, document: TdApi.Document, onDone: RunnableData<CustomLangPackResult>, onError: Runnable?) {
        if (!canInstallLanguage(document)) {
            if (onError != null) {
                onError.run()
            }
            return
        }
        Background.instance().post(Runnable {
            try {
                FileInputStream(document.document.local.path).use { `is` ->
                    val parserFactory = XmlPullParserFactory.newInstance()
                    val parser = parserFactory.newPullParser()
                    parser.setInput(`is`, "UTF-8")
                    val out = CustomLangPackResult(document)
                    while (parser.next() != XmlPullParser.END_DOCUMENT) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) continue
                        val name = parser.getName()
                        if ("resources" == name) {
                            readStrings(parser, out)
                        } else {
                            throw IllegalArgumentException("Unknown tag: " + name) // skip(parser);
                        }
                    }
                    if (out.canBeInstalled()) {
                        out.prepare()
                        tdlib.ui().post(Runnable { onDone.runWithData(out) })
                        return@use
                    }
                }
            } catch (t: Throwable) {
                Log.e("Cannot install custom language", t)
            }
            tdlib.ui().post(if (onError != null) onError else Runnable { UI.showToast(R.string.InvalidLocalisation, Toast.LENGTH_SHORT) })
        })
    }

    fun showLanguageInstallPrompt(c: TdlibDelegate, info: LanguagePackInfo) {
        val context = c.context().navigation().getCurrentStackItem()
        if (context == null || context.isDestroyed()) return
        if (Lang.packId() == info.id) {
            val text = Strings.buildMarkdown(c, Lang.getString(R.string.LanguageSame, info.name), null)
            context.showOptions(
                text,
                intArrayOf(R.id.btn_done, R.id.btn_settings),
                arrayOf<String?>(Lang.getString(R.string.OK), Lang.getString(R.string.Settings)),
                null,
                intArrayOf(R.drawable.baseline_check_circle_24, R.drawable.baseline_settings_24),
                OptionDelegate { itemView: View?, id: Int ->
                    if (id == R.id.btn_done) {
                        // Do nothing
                    } else if (id == R.id.btn_settings) {
                        context.navigateTo(SettingsLanguageController(c.context(), c.tdlib()))
                    }
                    true
                })
            return
        }
        val text = Strings.buildMarkdown(
            c,
            Lang.getString(
                if (info.isOfficial) R.string.LanguageAlert else R.string.LanguageCustomAlert,
                info.name,
                floor((info.translatedStringCount.toFloat() / info.totalStringCount.toFloat() * 100f).toDouble()).toInt(),
                info.translationUrl
            ),
            null
        )
        context.showOptions(
            text,
            intArrayOf(R.id.btn_done, R.id.btn_cancel),
            arrayOf<String?>(Lang.getString(R.string.LanguageChange), Lang.getString(R.string.Cancel)),
            intArrayOf(ViewController.OptionColor.BLUE, ViewController.OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_language_24, R.drawable.baseline_cancel_24),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_done) {
                    c.tdlib()!!.client().send(AddCustomServerLanguagePack(info.id), Client.ResultHandler { result: TdApi.Object? ->
                        when (result!!.getConstructor()) {
                            TdApi.Ok.CONSTRUCTOR -> {
                                c.tdlib()!!.applyLanguage(info, RunnableBool { boolResult: Boolean ->
                                    if (boolResult) {
                                        UI.showToast(R.string.LanguageChangeSuccess, Toast.LENGTH_SHORT)
                                    }
                                }, true)
                            }

                            TdApi.Error.CONSTRUCTOR -> {
                                UI.showError(result)
                            }
                        }
                    })
                }
                true
            })
    }

    fun showLanguageInstallPrompt(c: ViewController<*>, out: CustomLangPackResult, sourceMessage: TdApi.Message?) {
        if (sourceMessage != null) {
            val sourceChat: Chat?
            if (sourceMessage.forwardInfo != null && sourceMessage.forwardInfo!!.origin.getConstructor() == MessageOriginChannel.CONSTRUCTOR) {
                sourceChat = tdlib.chat((sourceMessage.forwardInfo!!.origin as MessageOriginChannel).chatId)
            } else {
                sourceChat = if (!sourceMessage.isOutgoing || sourceMessage.isChannelPost) tdlib.chat(sourceMessage.chatId) else null
            }
            if (sourceChat != null) {
                val username = tdlib.chatUsername(sourceChat.id)
                if (!isEmpty(username)) {
                    out.setSourceName("@" + username)
                } else {
                    out.setSourceName(tdlib.chatTitle(sourceChat))
                }
            }
        }

        c.showOptions(
            Lang.getStringBold(
                R.string.LanguageInfo,
                out.getStringValue(R.string.language_name),
                out.getStringValue(R.string.language_nameInEnglish),
                out.getStringValue(R.string.language_code),
                out.getStringValue(R.string.language_dateFormatLocale),
                out.completenessPercentage,
                if (out.isComplete) Lang.plural(R.string.xStrings, out.stringsCount.toLong()) else Lang.plural(
                    R.string.xStrings,
                    out.stringsCount.toLong()
                ) + ", " + Lang.plural(R.string.TranslationsMissing, out.missingStringsCount.toLong())
            ),
            intArrayOf(R.id.btn_messageApplyLocalization, R.id.btn_cancel),
            arrayOf<String?>(Lang.getString(R.string.LanguageInstall), Lang.getString(R.string.Cancel)),
            intArrayOf(ViewController.OptionColor.BLUE, ViewController.OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_language_24, R.drawable.baseline_cancel_24),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_messageApplyLocalization) {
                    applyLocalisation(c, out)
                }
                true
            })
    }

    private fun applyLocalisation(c: ViewController<*>, out: CustomLangPackResult) {
        val strings = out.getStrings()
        val code = out.languageCode
        val info = LanguagePackInfo(
            code,
            Lang.getBuiltinLanguagePackId(),
            out.languageNameInEnglish,
            out.getStringValue(R.string.language_name),
            Lang.cleanLanguageCode(code),
            false,
            "1" == out.getStringValue(R.string.language_rtl),
            false,
            true,
            strings.size,
            strings.size,
            strings.size,
            null
        )
        c.tdlib()!!.client().send(SetCustomLanguagePack(info, strings), Client.ResultHandler { result: TdApi.Object? ->
            when (result!!.getConstructor()) {
                TdApi.Ok.CONSTRUCTOR -> c.tdlib()!!.applyLanguage(info, RunnableBool { boolResult: Boolean ->
                    if (boolResult) {
                        UI.showToast(R.string.LocalisationApplied, Toast.LENGTH_SHORT)
                        // exitToChatScreen(c, c.getChatId());
                    }
                }, true)

                TdApi.Error.CONSTRUCTOR -> UI.showError(result)
            }
        })
    }

    fun switchInline(context: ViewController<*>, username: String?, query: String?, keepStack: Boolean) {
        val c = ChatsController(context.context(), context.tdlib())
        c.setArguments(ChatsController.Arguments(object : PickerDelegate {
            override fun onChatPicked(chat: Chat?, onDone: Runnable?): Boolean {
                if (!tdlib.canSendBasicMessage(chat)) {
                    UI.showToast(R.string.YouCantSendMessages, Toast.LENGTH_SHORT)
                    return false
                }
                return true
            }

            override fun getShareItem(): Any {
                return TGSwitchInline(username, query)
            }

            override fun modifyChatOpenParams(params: ChatOpenParameters) {
                if (keepStack) {
                    params.keepStack()
                }
            }
        }))
        context.navigateTo(c)
    }

    // Custom themes
    fun showDeleteThemeConfirm(context: ViewController<*>, theme: ThemeInfo, onDelete: Runnable?) {
        if (!ThemeManager.isCustomTheme(theme.getId())) return
        context.showOptions(
            Lang.getString(R.string.ThemeRemoveInfo),
            intArrayOf(R.id.btn_done, R.id.btn_cancel),
            arrayOf<String?>(Lang.getString(R.string.ThemeRemoveConfirm), Lang.getString(R.string.Cancel)),
            intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
            intArrayOf(R.drawable.baseline_delete_forever_24, R.drawable.baseline_cancel_24),
            OptionDelegate { itemView: View?, id: Int ->
                if (id == R.id.btn_done) {
                    ThemeManager.instance().removeCustomTheme(tdlib, theme.getId(), theme.parentThemeId(), onDelete)
                }
                true
            })
    }

    fun exportTheme(context: ViewController<*>, theme: ThemeInfo, includeDefault: Boolean, asJava: Boolean) {
        val themeName = theme.getName()
        val _customThemeId = ThemeManager.resolveCustomThemeId(theme.getId())
        val originalAuthor = Settings.instance().getThemeAuthor(_customThemeId)

        if (!theme.isCustom() || !Settings.instance().canEditAuthor(_customThemeId)) {
            exportThemeImpl(context, theme.getId(), themeName, originalAuthor, includeDefault, asJava)
            return
        }

        val after = RunnableData { author: String? -> exportThemeImpl(context, theme.getId(), themeName, author, includeDefault, asJava) }
        val currentUsername = if (!isEmpty(originalAuthor)) originalAuthor else context.tdlib()!!.myUserUsername()
        context.openInputAlert(
            Lang.getString(R.string.ThemeExportAddAuthorTitle),
            Lang.getString(R.string.ThemeExportAddAuthorInfo),
            R.string.ThemeExportDone,
            R.string.Cancel,
            currentUsername,
            null,
            object : ViewController.InputAlertCallback { override fun onAcceptInput(v: MaterialEditTextGroup?, resultAuthor: String?): Boolean {
                var resultAuthor = resultAuthor
                if (resultAuthor!!.startsWith("@")) {
                    resultAuthor = resultAuthor.substring(1)
                } else if (tdlib.isTmeUrl(resultAuthor)) {
                    var i = resultAuthor.indexOf("://")
                    if (i != -1) resultAuthor = resultAuthor.substring(i + 3)
                    i = resultAuthor.indexOf('/')
                    if (i == -1) return false
                    resultAuthor = resultAuthor.substring(i + 1)
                }
                if (!isEmpty(resultAuthor)) {
                    if (!TD.matchUsername(resultAuthor) || resultAuthor.length > MAX_USERNAME_LENGTH) return false
                } else {
                    resultAuthor = null
                }
                val result = resultAuthor
                tdlib.ui().postDelayed(Runnable { after.runWithData(result) }, 100)
                return true
            }},
            true,
            RunnableData { arg: ViewGroup? ->
                val textView = SettingHolder.createDescription(context.context(), ListItem.TYPE_DESCRIPTION, ColorId.textLight, null, context)
                textView.setText(Lang.getString(R.string.ThemeExportInfo))
                textView.setPadding(0, Screen.dp(12f), 0, 0)
                arg!!.addView(textView)
            },
            null
        )


        /* List<SettingItem> items = new ArrayList<>();
    items.add(new SettingItem(SettingItem.TYPE_CHECKBOX_OPTION, R.id.btn_themeExportSetAuthor, 0, R.string.ThemeExportAddAuthor, !Strings.isEmpty(originalAuthor)));
    items.add(new SettingItem(SettingItem.TYPE_CHECKBOX_OPTION, R.id.btn_themeExportNeedDefault, 0, R.string.ThemeExportNeedDefault, false));
    if (BuildConfig.DEBUG) {
      items.add(new SettingItem(SettingItem.TYPE_CHECKBOX_OPTION, R.id.btn_test, 0, "Export as Java", false));
    }
    context.showSettings(new SettingsWrapBuilder(R.id.btn_share)
      .setSaveStr(R.string.ThemeExportDone)
      .setAllowResize(false)
      .setRawItems(items)
      .addHeaderItem(Lang.getStringBold(R.string.ThemeExportInfo, theme.getName()))
      .setIntDelegate((ignoredId, result) -> {
        boolean includeDefault = result.get(R.id.btn_themeExportNeedDefault) == R.id.btn_themeExportNeedDefault;
        boolean includeAuthor = result.get(R.id.btn_themeExportSetAuthor) == R.id.btn_themeExportSetAuthor;
        boolean asJava = result.get(R.id.btn_test) == R.id.btn_test;

        RunnableData<String> after = author -> exportThemeImpl(context, customThemeId, themeName, author, includeDefault, asJava);
        if (!includeAuthor) {
          after.run(null);
          return;
        }
        String currentUsername = !Strings.isEmpty(originalAuthor) ? originalAuthor : context.tdlib()!!.myUserUsername();
        context.openInputAlert(Lang.getString(R.string.ThemeExportAddAuthorTitle), Lang.getString(R.string.ThemeExportAddAuthorInfo), R.string.ThemeExportDone, R.string.Cancel, currentUsername, (v, resultAuthor) -> {
          if (resultAuthor.startsWith("@")) {
            resultAuthor = resultAuthor.substring(1);
          } else if (tdlib.isTmeUrl(resultAuthor)) {
            int i = resultAuthor.indexOf("://");
            if (i != -1)
              resultAuthor = resultAuthor.substring(i + 3);
            i = resultAuthor.indexOf('/');
            if (i == -1)
              return false;
            resultAuthor = resultAuthor.substring(i + 1);
          }
          if (!Strings.isEmpty(resultAuthor)) {
            if (!TD.matchUsername(resultAuthor) || resultAuthor.length() > TD.MAX_USERNAME_LENGTH)
              return false;
          } else {
            resultAuthor = null;
          }
          after.run(resultAuthor);
          return true;
        }, true);
      }));*/
    }

    private fun exportThemeImpl(context: TdlibDelegate, themeId: Int, themeName: String?, author: String?, includeDefault: Boolean, asJava: Boolean) {
        var flags = 0
        if (includeDefault) {
            flags = flags or Theme.EXPORT_FLAG_INCLUDE_DEFAULT_VALUES
        }
        if (asJava) {
            flags = flags or Theme.EXPORT_FLAG_JAVA
        }

        val fileName: String?
        if (asJava) {
            fileName = themeName.secureFileName() + ".java"
        } else {
            fileName = themeName.secureFileName() + "." + BuildConfig.THEME_FILE_EXTENSION
        }

        var conversion = "theme_export_" + System.currentTimeMillis() + "_" + themeId + "," + flags
        if (!isEmpty(author)) {
            conversion += "," + author
        }
        val content: InputMessageContent = InputMessageDocument(InputFileGenerated(fileName, conversion, 0), null, false, null)
        val c = ShareController(context.context(), context.tdlib())
        c.setArguments(ShareController.Args(content))
        c.show()
    }

    class ImportedTheme : ThemeDelegate {
        @JvmField
        var name: String? = null
        var time: Long = 0
        @JvmField
        var author: String? = null
        @JvmField
        var wallpaper: String? = null

        @JvmField
        var parentThemeId: Int = ThemeId.NONE

        class Value : Comparable<Value> {
            @JvmField
            val name: String
            @JvmField
            val id: Int
            @JvmField
            var intValue: Int = 0
            @JvmField
            var floatValue: Float = 0f

            constructor(name: String, id: Int, intValue: Int) {
                this.name = name
                this.id = id
                this.intValue = intValue
            }

            constructor(name: String, id: Int, floatValue: Float) {
                this.name = name
                this.id = id
                this.floatValue = floatValue
            }

            override fun compareTo(o: Value): Int {
                return name.compareTo(o.name, ignoreCase = true)
            }
        }

        private val propertiesMap: MutableMap<Int?, Float?> = HashMap<Int?, Float?>()
        private val colorsMap: MutableMap<Int?, Int?> = HashMap<Int?, Int?>()
        @JvmField
        val propertiesList: MutableList<Value> = ArrayList<Value>()
        @JvmField
        val colorsList: MutableList<Value> = ArrayList<Value>()

        @JvmField
        var theme: ThemeCustom? = null

        override fun getId(): Int {
            throw RuntimeException("Stub!")
        }

        override fun getColor(colorId: Int): Int {
            val value = colorsMap.get(colorId)
            if (value != null) return value
            return ThemeSet.getBuiltinTheme(parentThemeId).getColor(colorId)
        }

        fun addColor(name: String, id: Int, value: Int) {
            require(!colorsMap.containsKey(id)) { "Duplicate color: " + Theme.getColorName(id) }
            colorsMap.put(id, value)
            colorsList.add(Value(name, id, value))
        }

        override fun getProperty(propertyId: Int): Float {
            val value = propertiesMap.get(propertyId)
            if (value != null) return value
            return ThemeSet.getBuiltinTheme(parentThemeId).getProperty(propertyId)
        }

        override fun getDefaultWallpaper(): String? {
            if (!isEmpty(wallpaper)) return wallpaper
            return ThemeSet.getBuiltinTheme(parentThemeId).getDefaultWallpaper()
        }

        fun addProperty(name: String, id: Int, value: Float) {
            require(!propertiesMap.containsKey(id)) { "Duplicate property: " + Theme.getPropertyName(id) }
            require(ThemeManager.isValidProperty(id, value)) { "Invalid property: " + Theme.getPropertyName(id) + "=" + value }
            propertiesMap.put(id, value)
            if (id == PropertyId.PARENT_THEME) this.parentThemeId = value.toInt()
            propertiesList.add(Value(name, id, value))
        }

        fun checkValidnessAndPrepare() {
            require(parentThemeId != ThemeId.NONE) { "theme.parentThemeId is missing" }
            require(!isEmpty(name)) { "theme.name is missing" }

            val defaultTheme = ThemeSet.getOrLoadTheme(parentThemeId, false)
            for (i in colorsList.indices.reversed()) {
                val value = colorsList.get(i)
                if (defaultTheme.getColor(value.id) == value.intValue) {
                    colorsList.removeAt(i)
                    colorsMap.remove(value.id)
                }
            }
            for (i in propertiesList.indices.reversed()) {
                val value = propertiesList.get(i)
                if (value.id != PropertyId.PARENT_THEME && defaultTheme.getProperty(value.id) == value.floatValue) {
                    propertiesList.removeAt(i)
                    propertiesMap.remove(value.id)
                }
            }

            val comparator = Comparator { obj: Value?, o: Value? -> obj!!.compareTo(o!!) }
            Collections.sort<Value?>(propertiesList, comparator)
            Collections.sort<Value?>(colorsList, comparator)
        }
    }

    fun readCustomTheme(context: ViewController<*>, doc: TdApi.Document?, onDone: RunnableData<ImportedTheme?>?, onError: Runnable?) {
        if (canInstallTheme(doc)) {
            readCustomTheme(context, doc!!.document, onDone, onError)
        } else {
            U.run(onError)
        }
    }

    fun readCustomTheme(context: ViewController<*>, doc: TdApi.File, onDone: RunnableData<ImportedTheme?>?, onError: Runnable?) {
        Background.instance().post(Runnable {
            var parse_context: Int = THEME_CONTEXT_NONE
            val propertyMap: Map<String, Int> = ThemeProperties.getMap()
            val colorMap: Map<String, Int> = ThemeColors.getMap()
            val theme = ImportedTheme()
            var success = false
            var lineIndex = 0
            try {
                BufferedReader(FileReader(doc.local.path)).use { br ->
                    var line: String?
                    while ((br.readLine().also { line = it }) != null) {
                        lineIndex++
                        line = line!!.trim { it <= ' ' }
                        if (line.isEmpty()) continue
                        val firstChar = line.get(0)
                        when (firstChar) {
                            '!' -> {
                                parse_context = THEME_CONTEXT_MAIN
                                continue
                            }

                            '@' -> {
                                parse_context = THEME_CONTEXT_ATTRIBUTES
                                continue
                            }

                            '#' -> {
                                parse_context = THEME_CONTEXT_COLORS
                                continue
                            }
                        }
                        if (parse_context == THEME_CONTEXT_NONE) continue
                        val endIndex = line.indexOf("//")
                        if (endIndex == 0) {
                            continue
                        } else if (endIndex != -1) {
                            line = line.substring(0, endIndex).trim { it <= ' ' }
                            if (line.isEmpty()) {
                                continue
                            }
                        }
                        // Trying to parse values variable
                        val split = line.indexOf(':')
                        if (split == -1) continue
                        val params = line.substring(0, split).trim { it <= ' ' }.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (params.size == 0) continue
                        val valueRaw = line.substring(split + 1).trim { it <= ' ' }
                        when (parse_context) {
                            THEME_CONTEXT_MAIN -> {
                                require(params.size <= 1) { "Parse error: multiset unavailable in the main block" }
                                val param = params[0].trim { it <= ' ' }
                                when (param) {
                                    "name" -> {
                                        theme.name = Strings.unwrap(param, valueRaw).trim { it <= ' ' }
                                        require(!isEmpty(theme.name)) { "Invalid value: " + valueRaw }
                                    }

                                    "time" -> theme.time = valueRaw.toLong()
                                    "author" -> {
                                        theme.author = Strings.unwrap(param, valueRaw).trim { it <= ' ' }
                                        require(!(isEmpty(theme.author) || !TD.matchUsername(theme.author) || theme.author!!.length > MAX_USERNAME_LENGTH)) { "Invalid value: " + valueRaw }
                                    }

                                    "wallpaper" -> {
                                        theme.wallpaper = Strings.unwrap(param, valueRaw).trim { it <= ' ' }
                                        require(!isEmpty(theme.wallpaper)) { "Invalid value: " + valueRaw }
                                    }
                                }
                            }

                            THEME_CONTEXT_ATTRIBUTES -> {
                                val value = valueRaw.toFloat()
                                for (name in params) {
                                    var name = name
                                    name = name.trim { it <= ' ' }
                                    val id = propertyMap.get(name)
                                    if (id == null) {
                                        Log.e("Unknown theme property: %s, line: %d", name, lineIndex)
                                        continue
                                    }
                                    theme.addProperty(name, id, value)
                                }
                            }

                            THEME_CONTEXT_COLORS -> {
                                val color = parseHexColor(if (valueRaw.startsWith("#")) valueRaw.substring(1) else valueRaw, true)
                                for (name in params) {
                                    var name = name
                                    name = name.trim { it <= ' ' }
                                    val id = colorMap.get(name)
                                    if (id == null) {
                                        Log.e("Unknown theme color: %s, line: %d", name, lineIndex)
                                        continue
                                    }
                                    theme.addColor(name, id, color)
                                }
                            }
                        }
                    }

                    theme.checkValidnessAndPrepare()
                    success = true
                }
            } catch (t: Throwable) {
                Log.e("Cannot parse custom theme, line:%d", t, lineIndex)
            }
            if (success) {
                if (onDone != null) {
                    tdlib.ui().post(Runnable { onDone.runWithData(theme) })
                } else {
                    tdlib.ui().post(Runnable {
                        val info: CharSequence?
                        if (theme.author != null) {
                            info = Lang.getString(
                                R.string.ThemeInstallAuthor,
                                SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, argIndex: Int, fakeBold: Boolean ->
                                    if (argIndex == 1) {
                                        val span = CustomTypefaceSpan(null, ColorId.textLink)
                                        span.setTextEntityType(TextEntityTypeMention())
                                        span.setForcedTheme(theme)
                                        return@SpanCreator span
                                    } else {
                                        return@SpanCreator Lang.newBoldSpan(fakeBold)
                                    }
                                },
                                theme.name,
                                "@" + theme.author
                            )
                        } else {
                            info = Lang.getStringBold(R.string.ThemeInstall, theme.name)
                        }
                        val size = if (onError != null) 3 else 2
                        val ids = IntList(size)
                        val colors = IntList(size)
                        val icons = IntList(size)
                        val strings = StringList(size)

                        ids.append(R.id.btn_done)
                        icons.append(R.drawable.baseline_palette_24)
                        colors.append(ViewController.OptionColor.BLUE)
                        strings.append(R.string.ThemeInstallDone)

                        if (onError != null) {
                            ids.append(R.id.btn_open)
                            icons.append(R.drawable.baseline_open_in_browser_24)
                            colors.append(ViewController.OptionColor.NORMAL)
                            strings.append(R.string.Open)
                        }

                        ids.append(R.id.btn_cancel)
                        icons.append(R.drawable.baseline_cancel_24)
                        colors.append(ViewController.OptionColor.NORMAL)
                        strings.append(R.string.Cancel)

                        if (info != null) {
                            val b = if (info is SpannableStringBuilder) info else SpannableStringBuilder(info)
                            b.append("\n\n")
                            b.append(Lang.getString(R.string.ThemeInstallHint))
                        }
                        context.showOptions(info, ids.get(), strings.get(), colors.get(), icons.get(), OptionDelegate { itemView: View?, id: Int ->
                            if (id == R.id.btn_done) {
                                tdlib.wallpaper().loadWallpaper(theme.wallpaper, 1000, Runnable {
                                    context.tdlib()!!.ui().installTheme(context, theme)
                                })
                            } else if (id == R.id.btn_open) {
                                U.run(onError)
                            }
                            true
                        }, theme)
                    })
                }
            } else if (onError != null) {
                tdlib.ui().post(onError)
            }
        })
    }

    fun installTheme(context: TdlibDelegate?, customTheme: ImportedTheme) {
        val themeId = ThemeManager.instance().installCustomTheme(customTheme)
        if (themeId != ThemeId.NONE) {
            ThemeManager.instance().changeGlobalTheme(tdlib, customTheme.theme!!, false, null)
        }
    }

    interface EraseCallback {
        fun onPrepareEraseData()
        fun onEraseDataCompleted()
    }

    fun eraseLocalData(context: ViewController<*>, tdlibAvailable: Boolean, callback: EraseCallback) {
        val info = Lang.getMarkdownString(context, R.string.EraseDatabaseWarn)
        val hint = if (context.tdlib()!!.context().isMultiUser()) Lang.getMarkdownString(context, R.string.EraseDatabaseMultiUser) else null
        context.showWarning(if (hint != null) TextUtils.concat(info, "\n\n", hint) else info, RunnableBool { success: Boolean ->
            if (success) {
                context.showWarning(Lang.getMarkdownString(context, R.string.EraseDatabaseWarn2), RunnableBool { success2: Boolean ->
                    if (success2 && !context.isDestroyed() && context.isFocused() && context.navigationController() != null) {
                        UI.showToast(R.string.EraseDatabaseProgress, Toast.LENGTH_SHORT)
                        context.navigationController()!!.getStack().setIsLocked(true)
                        callback.onPrepareEraseData()

                        UI.showToast(R.string.EraseDatabaseProgress, Toast.LENGTH_SHORT)
                        val databaseEraser = Runnable {
                            tdlib.eraseTdlibDatabase(RunnableBool { eraseTdlibSuccess: Boolean ->
                                val after = Runnable {
                                    tdlib.ui().post(Runnable {
                                        if (!context.isDestroyed() && context.navigationController() != null) {
                                            context.navigationController()!!.getStack().setIsLocked(false)
                                            callback.onEraseDataCompleted()
                                        }
                                        UI.showToast(R.string.EraseDatabaseDone, Toast.LENGTH_SHORT)
                                    })
                                }
                                if (tdlibAvailable) {
                                    tdlib.awaitInitialization(after)
                                } else {
                                    after.run()
                                }
                            })
                        }
                        if (tdlibAvailable) {
                            tdlib.deleteAllFiles(RunnableBool { deleteFilesSuccess: Boolean -> databaseEraser.run() })
                        } else {
                            databaseEraser.run()
                        }
                    }
                })
            }
        })
    }

    fun openCardNumber(context: TdlibDelegate?, cardNumber: String?) {
        tdlib.client().send(GetBankCardInfo(cardNumber), Client.ResultHandler { result: TdApi.Object? ->
            when (result!!.getConstructor()) {
                BankCardInfo.CONSTRUCTOR -> {
                    val bankCardInfo = result as BankCardInfo
                    tdlib.ui().post(Runnable {
                        val c = if (context is ViewController<*>) context else UI.getCurrentStackItem()
                        val hasAnyActions = bankCardInfo.actions.size > 0
                        if (c != null && !c.isDestroyed()) {
                            val ids = IntList(if (hasAnyActions) 1 else bankCardInfo.actions.size)
                            val strings = StringList(if (hasAnyActions) 1 else bankCardInfo.actions.size)
                            var icons: IntArray? = null

                            if (hasAnyActions) {
                                for (openUrl in bankCardInfo.actions) {
                                    ids.append(R.id.btn_openLink)
                                    strings.append(openUrl.text)
                                }
                            } else {
                                ids.append(R.id.btn_copyLink)
                                strings.append(R.string.CopyBankCard)
                                icons = intArrayOf(R.drawable.baseline_content_copy_24)
                            }

                            c.showOptions(bankCardInfo.title, ids.get(), strings.get(), null, icons, object : OptionDelegate {
                                override fun onOptionItemPressed(optionItemView: View?, id: Int): Boolean {
                                    if (id == R.id.btn_openLink) {
                                        Intents.openUri(optionItemView!!.getTag() as String?)
                                    } else if (id == R.id.btn_copyLink) {
                                        UI.copyText(cardNumber, R.string.CopiedBankCard)
                                    }

                                    return true
                                }

                                override fun getTagForItem(position: Int): Any? {
                                    return if (hasAnyActions) bankCardInfo.actions[position].url else null
                                }
                            })
                        }
                    })
                }

                TdApi.Error.CONSTRUCTOR -> UI.showError(result)
            }
        })
    }

    @JvmOverloads
    fun fillDefaultHapticMenu(
        chatId: Long,
        isEdit: Boolean,
        canToggleMarkdown: Boolean,
        canSendWithoutSound: Boolean,
        isForward: Boolean = false
    ): MutableList<HapticMenuHelper.MenuItem?> {
        val items: MutableList<HapticMenuHelper.MenuItem?> = ArrayList<HapticMenuHelper.MenuItem?>()
        if (!isEdit && !isSecret(chatId)) {
            if (tdlib.isSelfChat(chatId)) {
                items.add(
                    HapticMenuHelper.MenuItem(R.id.btn_sendScheduled, Lang.getString(R.string.SendReminder), R.drawable.baseline_date_range_24)
                        .bindTutorialFlag(
                            Settings.TUTORIAL_SET_REMINDER
                        )
                )
            } else {
                val userId = tdlib.chatUserId(chatId)
                if (userId != 0L) {
                    items.add(
                        HapticMenuHelper.MenuItem(R.id.btn_sendOnceOnline, Lang.getString(R.string.SendOnceOnline), R.drawable.baseline_visibility_24)
                            .bindToLastSeenAvailability(tdlib, userId)
                    )
                }
                items.add(
                    HapticMenuHelper.MenuItem(R.id.btn_sendScheduled, Lang.getString(R.string.SendSchedule), R.drawable.baseline_date_range_24)
                        .bindTutorialFlag(if (isForward) Settings.TUTORIAL_FORWARD_SCHEDULE else Settings.TUTORIAL_SCHEDULE)
                )
            }
        }
        if (!isEdit && canSendWithoutSound) {
            items.add(HapticMenuHelper.MenuItem(R.id.btn_sendNoSound, Lang.getString(R.string.SendNoSound), R.drawable.baseline_notifications_off_24))
        }
        if (canToggleMarkdown) {
            items.add(
                HapticMenuHelper.MenuItem(
                    R.id.btn_sendNoMarkdown,
                    Lang.getString(if (isEdit) R.string.SaveNoMarkdown else R.string.SendNoMarkdown),
                    R.drawable.baseline_code_24
                ).bindTutorialFlag(
                    Settings.TUTORIAL_SEND_WITHOUT_MARKDOWN
                )
            )
        }
        return items
    }

    fun interface SimpleSendCallback {
        fun onSendRequested(sendOptions: MessageSendOptions?, disableMarkdown: Boolean)
    }

    fun createSimpleHapticMenu(
        context: ViewController<*>?,
        chatId: Long,
        availabilityCallback: FutureBool?,
        canDisableMarkdownCallback: FutureBool?,
        canHideMedia: FutureBool?,
        customItemProvider: RunnableData<MutableList<HapticMenuHelper.MenuItem?>?>?,
        sendCallback: SimpleSendCallback,
        forcedTheme: ThemeDelegate?
    ): HapticMenuHelper {
        return HapticMenuHelper(HapticMenuHelper.Provider { list: View? ->
            if (availabilityCallback == null || availabilityCallback.getBoolValue()) {
                var items = fillDefaultHapticMenu(chatId, false, canDisableMarkdownCallback != null && canDisableMarkdownCallback.getBoolValue(), true)
                if (customItemProvider != null) {
                    if (items == null) items = ArrayList<HapticMenuHelper.MenuItem?>()
                    customItemProvider.runWithData(items)
                }
                return@Provider items
            }
            null
        }, HapticMenuHelper.OnItemClickListener { menuView: View?, parentView: View?, item: HapticMenuHelper.MenuItem? ->
            val menuItemId = menuView!!.getId()
            if (menuItemId == R.id.btn_sendScheduled) {
                if (context != null) {
                    tdlib.ui().pickSchedulingState(
                        context, RunnableData { schedulingState: MessageSendOptions? -> sendCallback.onSendRequested(newSendOptions(schedulingState), false) },
                        chatId, false, false, null, forcedTheme
                    )
                }
            } else if (menuItemId == R.id.btn_sendNoMarkdown) {
                sendCallback.onSendRequested(newSendOptions(), true)
            } else if (menuItemId == R.id.btn_sendNoSound) {
                sendCallback.onSendRequested(newSendOptions(null, true), false)
            } else if (menuItemId == R.id.btn_sendOnceOnline) {
                sendCallback.onSendRequested(newSendOptions(MessageSchedulingStateSendWhenOnline()), false)
            }
            true
        }, if (context != null) context.getThemeListeners() else null, forcedTheme)
    }

    fun showScheduleOptions(
        context: ViewController<*>,
        chatId: Long,
        needSendWithoutSound: Boolean,
        callback: SimpleSendCallback,
        defaultSendOptions: MessageSendOptions?,
        forcedTheme: ThemeDelegate?
    ): Boolean {
        return pickSchedulingState(
            context,
            RunnableData { initialSendOptions: MessageSendOptions? -> callback.onSendRequested(initialSendOptions, false) },
            chatId,
            tdlib.cache().userLastSeenAvailable(tdlib.chatUserId(chatId)),
            needSendWithoutSound,
            defaultSendOptions,
            forcedTheme
        )
    }

    fun pickSchedulingState(
        context: ViewController<*>,
        callback: RunnableData<MessageSendOptions?>,
        chatId: Long,
        needOnline: Boolean,
        needSendWithoutSound: Boolean,
        defaultSendOptions: MessageSendOptions?,
        forcedTheme: ThemeDelegate?
    ): Boolean {
        if (isSecret(chatId)) {
            return false
        }
        val isSelfChat = tdlib.isSelfChat(chatId)
        var size = 4
        if (needOnline) size++
        if (needSendWithoutSound) size++
        val ids = IntList(size)
        val strings = StringList(size)
        val icons = IntList(size)

        if (needSendWithoutSound) {
            ids.append(R.id.btn_sendNoSound)
            strings.append(R.string.SendNoSound)
            icons.append(R.drawable.baseline_notifications_off_24)
        }

        if (needOnline) {
            ids.append(R.id.btn_sendOnceOnline)
            strings.append(R.string.SendOnceOnline)
            icons.append(R.drawable.baseline_visibility_24)
        }

        ids.append(R.id.btn_sendScheduled30Min)
        strings.append(Lang.plural(if (isSelfChat) R.string.RemindInXMinutes else R.string.SendInXMinutes, 30))
        icons.append(R.drawable.dotvhs_baseline_time_30m_24)

        ids.append(R.id.btn_sendScheduled2Hr)
        strings.append(Lang.plural(if (isSelfChat) R.string.RemindInXHours else R.string.SendInXHours, 2))
        icons.append(R.drawable.dotvhs_baseline_time_2h_24)

        ids.append(R.id.btn_sendScheduled8Hr)
        strings.append(Lang.plural(if (isSelfChat) R.string.RemindInXHours else R.string.SendInXHours, 8))
        icons.append(R.drawable.dotvhs_baseline_time_8h_24)

        ids.append(R.id.btn_sendScheduled1Yr)
        strings.append(Lang.plural(if (isSelfChat) R.string.RemindInXYears else R.string.SendInXYears, 1))
        icons.append(R.drawable.dotvhs_baseline_time_1y_24)

        ids.append(R.id.btn_sendScheduledCustom)
        strings.append(Lang.getString(if (isSelfChat) R.string.RemindAtCustomTime else R.string.SendAtCustomTime))
        icons.append(R.drawable.baseline_date_range_24)

        context.showOptions(null, ids.get(), strings.get(), null, icons.get(), OptionDelegate { v: View?, optionId: Int ->
            var seconds: Long = 0
            if (optionId == R.id.btn_sendNoSound) {
                callback.runWithData(newSendOptions(defaultSendOptions, null, true))
                return@OptionDelegate true
            } else if (optionId == R.id.btn_sendOnceOnline) {
                callback.runWithData(newSendOptions(defaultSendOptions, MessageSchedulingStateSendWhenOnline()))
                return@OptionDelegate true
            } else if (optionId == R.id.btn_sendScheduled30Min) {
                seconds = TimeUnit.MINUTES.toSeconds(30)
            } else if (optionId == R.id.btn_sendScheduled2Hr) {
                seconds = TimeUnit.HOURS.toSeconds(2)
            } else if (optionId == R.id.btn_sendScheduled8Hr) {
                seconds = TimeUnit.HOURS.toSeconds(8)
            } else if (optionId == R.id.btn_sendScheduled1Yr) {
                val tdlibTimeMs = tdlib.currentTimeMillis()
                val c = calendarInstance(tdlibTimeMs)
                c.add(Calendar.YEAR, 1)
                val elapsedMs = c.getTimeInMillis() - tdlibTimeMs
                seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs)
            } else if (optionId == R.id.btn_sendScheduledCustom) {
                val titleRes: Int
                val todayRes: Int
                val tomorrowRes: Int
                val futureRes: Int
                if (isSelfChat) {
                    titleRes = R.string.SendReminder
                    todayRes = R.string.RemindTodayAt
                    tomorrowRes = R.string.RemindTomorrowAt
                    futureRes = R.string.RemindDateAt
                } else {
                    titleRes = R.string.SendSchedule
                    todayRes = R.string.SendTodayAt
                    tomorrowRes = R.string.SendTomorrowAt
                    futureRes = R.string.SendDateAt
                }
                context.showDateTimePicker(Lang.getString(titleRes), todayRes, tomorrowRes, futureRes, RunnableLong { millis: Long ->
                    val sendDate = TimeUnit.MILLISECONDS.toSeconds(millis).toInt()
                    callback.runWithData(newSendOptions(defaultSendOptions, MessageSchedulingStateSendAtDate(sendDate, 0 /*TODO: repeat period*/)))
                }, forcedTheme)
                return@OptionDelegate true
            }
            if (seconds > 0) {
                val sendDate = (tdlib.currentTime(TimeUnit.SECONDS) + seconds).toInt()
                callback.runWithData(newSendOptions(defaultSendOptions, MessageSchedulingStateSendAtDate(sendDate, 0 /*TODO: repeat period*/)))
            }
            true
        }, forcedTheme)
        return true
    }

    fun deleteContact(context: ViewController<*>, userId: Long) {
        if (tdlib.cache().userContact(userId)) {
            context.showOptions(
                Lang.getStringBold(R.string.DeleteContactConfirm, tdlib.cache().userName(userId)),
                intArrayOf(R.id.btn_delete, R.id.btn_cancel),
                arrayOf<String?>(
                    Lang.getString(R.string.Delete), Lang.getString(R.string.Cancel)
                ),
                intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                intArrayOf(R.drawable.baseline_delete_24, R.drawable.baseline_cancel_24),
                OptionDelegate { itemView: View?, id1: Int ->
                    if (!context.isDestroyed() && id1 == R.id.btn_delete) {
                        tdlib.send<TdApi.Ok?>(RemoveContacts(longArrayOf(userId)), tdlib.typedOkHandler())
                    }
                    true
                })
        }
    }

    interface OwnershipTransferListener {
        fun onOwnershipTransferAbilityChecked(result: TdApi.Object?) {}
        fun onOwnershipTransferConfirmed(password: String?)
    }

    fun requestTransferOwnership(context: ViewController<*>, finalAlertMessageText: CharSequence?, listener: OwnershipTransferListener) {
        tdlib.send<CanTransferOwnershipResult?>(
            CanTransferOwnership(),
            Tdlib.ResultHandler { canTransferOwnership: CanTransferOwnershipResult?, error: TdApi.Error? ->
                post(
                    Runnable {
                        listener.onOwnershipTransferAbilityChecked(if (canTransferOwnership != null) canTransferOwnership else error)
                        if (error != null) {
                            UI.showError(error)
                            return@Runnable
                        }
                        when (canTransferOwnership!!.getConstructor()) {
                            TdApi.CanTransferOwnershipResultOk.CONSTRUCTOR -> {
                                tdlib.send<PasswordState?>(GetPasswordState(), Tdlib.ResultHandler { passwordState: PasswordState?, error1: TdApi.Error? ->
                                    if (error1 != null) {
                                        UI.showError(error1)
                                        return@ResultHandler
                                    }
                                    post(Runnable {
                                        val controller = PasswordController(context.context(), context.tdlib())
                                        controller.setArguments(
                                            PasswordController.Args(PasswordController.MODE_TRANSFER_OWNERSHIP_CONFIRM, passwordState)
                                                .setSuccessListener(RunnableData { password: String? ->
                                                    // Ask if the user REALLY wants to transfer ownership, because this operation is serious
                                                    context.addOneShotFocusListener(Runnable {
                                                        context.showOptions(
                                                            ViewController.Options.Builder()
                                                                .info(
                                                                    Strings.getTitleAndText(
                                                                        Lang.getString(R.string.TransferOwnershipAlert),
                                                                        finalAlertMessageText
                                                                    )
                                                                )
                                                                .item(
                                                                    OptionItem(
                                                                        R.id.btn_next,
                                                                        Lang.getString(R.string.TransferOwnershipConfirm),
                                                                        ViewController.OptionColor.RED,
                                                                        R.drawable.templarian_baseline_account_switch_24
                                                                    )
                                                                )
                                                                .cancelItem()
                                                                .build(), OptionDelegate { optionView: View?, id: Int ->
                                                                if (id == R.id.btn_next) {
                                                                    listener.onOwnershipTransferConfirmed(password)
                                                                }
                                                                true
                                                            })
                                                    }
                                                    )
                                                })
                                        )
                                        context.navigateTo(controller)
                                    })
                                })
                            }

                            TdApi.CanTransferOwnershipResultPasswordNeeded.CONSTRUCTOR -> {
                                context.showOptions(
                                    ViewController.Options.Builder()
                                        .info(
                                            Strings.getTitleAndText(
                                                Lang.getString(R.string.TransferOwnershipSecurityAlert),
                                                Lang.getMarkdownString(context, R.string.TransferOwnershipSecurityPasswordNeeded)
                                            )
                                        )
                                        .item(
                                            OptionItem(
                                                R.id.btn_next,
                                                Lang.getString(R.string.TransferOwnershipSecurityActionSetPassword),
                                                ViewController.OptionColor.BLUE,
                                                R.drawable.mrgrigri_baseline_textbox_password_24
                                            )
                                        )
                                        .cancelItem()
                                        .build(), OptionDelegate { optionView: View?, id: Int ->
                                        if (id == R.id.btn_next) {
                                            val controller = Settings2FAController(context.context(), context.tdlib())
                                            controller.setArguments(Settings2FAController.Args(null))
                                            context.navigateTo(controller)
                                        }
                                        true
                                    }
                                )
                            }

                            CanTransferOwnershipResultPasswordTooFresh.CONSTRUCTOR -> {
                                context.showOptions(
                                    ViewController.Options.Builder()
                                        .info(
                                            Strings.getTitleAndText(
                                                Lang.getString(R.string.TransferOwnershipSecurityAlert),
                                                Lang.getMarkdownString(
                                                    context,
                                                    R.string.TransferOwnershipSecurityWaitPassword,
                                                    Lang.getDuration((canTransferOwnership as CanTransferOwnershipResultPasswordTooFresh).retryAfter)
                                                )
                                            )
                                        )
                                        .item(
                                            OptionItem(
                                                R.id.btn_next,
                                                Lang.getString(R.string.OK),
                                                ViewController.OptionColor.NORMAL,
                                                R.drawable.baseline_check_circle_24
                                            )
                                        )
                                        .cancelItem()
                                        .build(), OptionDelegate { optionView: View?, id: Int -> true }
                                )
                            }

                            CanTransferOwnershipResultSessionTooFresh.CONSTRUCTOR -> {
                                context.showOptions(
                                    ViewController.Options.Builder()
                                        .info(
                                            Strings.getTitleAndText(
                                                Lang.getString(R.string.TransferOwnershipSecurityAlert),
                                                Lang.getMarkdownString(
                                                    context,
                                                    R.string.TransferOwnershipSecurityWaitSession,
                                                    Lang.getDuration((canTransferOwnership as CanTransferOwnershipResultSessionTooFresh).retryAfter)
                                                )
                                            )
                                        )
                                        .item(
                                            OptionItem(
                                                R.id.btn_next,
                                                Lang.getString(R.string.OK),
                                                ViewController.OptionColor.NORMAL,
                                                R.drawable.baseline_check_circle_24
                                            )
                                        )
                                        .cancelItem()
                                        .build(), OptionDelegate { optionView: View?, id: Int -> true }
                                )
                            }

                            else -> {
                                assertCanTransferOwnershipResult_ac091006()
                                throw unsupported(canTransferOwnership)
                            }
                        }
                    })
            })
    }

    fun saveGifs(downloadedFiles: MutableList<TD.DownloadedFile>) {
        val remaining = AtomicInteger(downloadedFiles.size)
        val successful = AtomicInteger(0)
        for (downloadedFile in downloadedFiles) {
            tdlib.client().send(AddSavedAnimation(InputFileId(downloadedFile.getFileId())), Client.ResultHandler { `object`: TdApi.Object? ->
                when (`object`!!.getConstructor()) {
                    TdApi.Ok.CONSTRUCTOR -> successful.incrementAndGet()
                    TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
                }
                if (remaining.decrementAndGet() == 0) {
                    if (successful.get() == 1) {
                        UI.showToast(R.string.GifSaved, Toast.LENGTH_SHORT)
                    } else {
                        UI.showToast(Lang.pluralBold(R.string.XGifSaved, downloadedFiles.size.toLong()), Toast.LENGTH_SHORT)
                    }
                }
            })
        }
    }

    fun saveGif(fileId: Int) {
        if (fileId == 0) {
            return
        }
        tdlib.client().send(AddSavedAnimation(InputFileId(fileId)), Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                TdApi.Ok.CONSTRUCTOR -> UI.showToast(R.string.GifSaved, Toast.LENGTH_SHORT)
                TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
            }
        })
    }

    fun subscribeToBeta(context: TdlibDelegate) {
        openUrl(context, Lang.getStringSecure(R.string.url_betaSubscription), null)
    }

    // Telegram Premium
    fun interface PremiumLimitCallback {
        fun onPremiumLimitReached(currentLimit: Int, premiumLimit: Int)
    }

    @UiThread
    fun checkPremiumLimit(premiumLimitType: PremiumLimitType, callback: PremiumLimitCallback) {
        val effectiveLimit: Int
        when (premiumLimitType.getConstructor()) {
            PremiumLimitTypeChatFolderCount.CONSTRUCTOR -> effectiveLimit = tdlib.chatFolderCount()
            PremiumLimitTypeChatFolderInviteLinkCount.CONSTRUCTOR -> effectiveLimit = tdlib.chatFolderInviteLinkCountMax()
            PremiumLimitTypeChatFolderChosenChatCount.CONSTRUCTOR -> effectiveLimit = tdlib.chatFolderChosenChatCountMax()
            PremiumLimitTypeShareableChatFolderCount.CONSTRUCTOR -> effectiveLimit = tdlib.addedShareableChatFolderCountMax()
            else -> {
                assertPremiumLimitType_8710e45f()
                throw unsupported(premiumLimitType)
            }
        }

        if (tdlib.hasPremium()) {
            callback.onPremiumLimitReached(effectiveLimit, effectiveLimit)
            return
        }
        tdlib.send<TdApi.PremiumLimit?>(GetPremiumLimit(premiumLimitType), Tdlib.ResultHandler { limit: TdApi.PremiumLimit?, error: TdApi.Error? ->
            post(Runnable {
                if (limit != null && limit.defaultValue < limit.premiumValue && effectiveLimit < limit.premiumValue) {
                    callback.onPremiumLimitReached(effectiveLimit, limit.premiumValue)
                } else {
                    // Note: some users cannot purchase Telegram Premium, for such users GetPremiumLimit returns an error
                    callback.onPremiumLimitReached(effectiveLimit, effectiveLimit)
                }
            })
        })
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [PremiumFeature.Companion.STICKER, PremiumFeature.Companion.RESTRICT_VOICE_AND_VIDEO_MESSAGES, PremiumFeature.Companion.CUSTOM_EMOJI, PremiumFeature.Companion.NEW_CHATS_PRIVACY
        ]
    )
    annotation class PremiumFeature {
        companion object {
            const val STICKER: Int = 1
            const val RESTRICT_VOICE_AND_VIDEO_MESSAGES: Int = 2
            const val CUSTOM_EMOJI: Int = 3
            const val NEW_CHATS_PRIVACY: Int = 4
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [PremiumLimit.Companion.SHAREABLE_FOLDER_COUNT, PremiumLimit.Companion.CHAT_FOLDER_COUNT, PremiumLimit.Companion.CHAT_FOLDER_INVITE_LINK_COUNT
        ]
    )
    annotation class PremiumLimit {
        companion object {
            const val SHAREABLE_FOLDER_COUNT: Int = 1
            const val CHAT_FOLDER_COUNT: Int = 2
            const val CHAT_FOLDER_INVITE_LINK_COUNT: Int = 3
        }
    }

    fun showPremiumAlert(context: ViewController<*>, view: View?, @PremiumFeature premiumFeature: Int): Boolean {
        return showPremiumAlert(context, context.context().tooltipManager(), view, premiumFeature)
    }

    fun showPremiumAlert(context: ViewController<*>?, tooltipManager: TooltipOverlayView, view: View?, @PremiumFeature premiumFeature: Int): Boolean {
        if (tdlib.hasPremium()) return false
        val stringRes: Int
        when (premiumFeature) {
            PremiumFeature.Companion.STICKER -> stringRes = R.string.PremiumRequiredSticker
            PremiumFeature.Companion.RESTRICT_VOICE_AND_VIDEO_MESSAGES -> stringRes = R.string.PremiumRequiredVoiceVideo
            PremiumFeature.Companion.CUSTOM_EMOJI -> stringRes = R.string.MessageContainsPremiumFeatures
            PremiumFeature.Companion.NEW_CHATS_PRIVACY -> stringRes = R.string.PremiumRequiredNewChats
            else -> throw IllegalStateException()
        }
        showPremiumRequiredTooltip(context, tooltipManager, view, Lang.getMarkdownString(context, stringRes))
        return true
    }

    fun showLimitReachedInfo(context: ViewController<*>, view: View?, @PremiumLimit premiumLimit: Int) {
        showLimitReachedInfo(context, context.context().tooltipManager(), view, premiumLimit)
    }

    fun showLimitReachedInfo(context: ViewController<*>?, tooltipManager: TooltipOverlayView, view: View?, @PremiumLimit premiumLimit: Int) {
        val type: PremiumLimitType?
        val premiumPluralRes: Int
        val defaultPluralRes: Int
        when (premiumLimit) {
            PremiumLimit.Companion.SHAREABLE_FOLDER_COUNT -> {
                type = PremiumLimitTypeShareableChatFolderCount()
                premiumPluralRes = R.string.PremiumLimitAddShareableFolder
                defaultPluralRes = R.string.LimitAddShareableFolder
            }

            PremiumLimit.Companion.CHAT_FOLDER_COUNT -> {
                type = PremiumLimitTypeChatFolderCount()
                premiumPluralRes = R.string.PremiumLimitCreateFolder
                defaultPluralRes = R.string.LimitCreateFolder
            }

            PremiumLimit.Companion.CHAT_FOLDER_INVITE_LINK_COUNT -> {
                type = PremiumLimitTypeChatFolderInviteLinkCount()
                premiumPluralRes = R.string.PremiumLimitChatFolderInviteLink
                defaultPluralRes = R.string.LimitChatFolderInviteLink
            }

            else -> {
                throw IllegalArgumentException(premiumLimit.toString())
            }
        }
        showPremiumLimitTooltip(context, tooltipManager, view, premiumPluralRes, type, defaultPluralRes)
    }

    private fun showPremiumLimitTooltip(
        context: ViewController<*>?,
        tooltipManager: TooltipOverlayView,
        view: View?,
        @StringRes markdownStringRes: Int,
        premiumLimitType: PremiumLimitType,
        @StringRes defaultMarkdownStringRes: Int
    ) {
        checkPremiumLimit(premiumLimitType, PremiumLimitCallback { currentLimit: Int, premiumLimit: Int ->
            if (currentLimit < premiumLimit) {
                showLimitReachedTooltip(context, tooltipManager, view, markdownStringRes, currentLimit.toLong(), Strings.buildCounter(premiumLimit.toLong()))
            } else {
                showLimitReachedTooltip(context, tooltipManager, view, defaultMarkdownStringRes, currentLimit.toLong())
            }
        })
    }

    private fun showLimitReachedTooltip(
        context: ViewController<*>?,
        tooltipManager: TooltipOverlayView,
        view: View?,
        @StringRes pluralRes: Int,
        num: Long,
        vararg formatArgs: Any?
    ) {
        showPremiumRequiredTooltip(context, tooltipManager, view, Lang.getMarkdownPlural(context, pluralRes, num, Lang.boldCreator(), *formatArgs))
    }

    private fun showPremiumRequiredTooltip(context: ViewController<*>?, tooltipManager: TooltipOverlayView, view: View?, text: CharSequence) {
        tooltipManager
            .builder(view)
            .icon(R.drawable.baseline_warning_24)
            .controller(context)
            .show(tdlib, text)
            .hideDelayed()
    }

    // Video Chats & Live Streams
    fun openVoiceChatInvitation(context: ViewController<*>?, invitation: InternalLinkTypeVideoChat?) {
        // TODO some confirmation screen & join voice chat if agreed
    }

    fun openVoiceChat(context: ViewController<*>?, groupCallId: Int, openParameters: UrlOpenParameters?) {
        // TODO open voice chat layer
    }

    // Suggestions by emoji
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [StickersType.INSTALLED, StickersType.INSTALLED_EXTRA, StickersType.RECOMMENDED
        ]
    )
    annotation class StickersType {
        companion object {
            const val INSTALLED: Int = 0
            const val INSTALLED_EXTRA: Int = 1
            const val RECOMMENDED: Int = 2
        }
    }

    class EmojiStickers(
      private val tdlib: Tdlib,
      stickerType: StickerType?,
      @JvmField val query: String?,
      val isComplexQuery: Boolean,
      limit: Int,
      chatId: Long,
      needRecommended: Boolean
    ) {
        private var installedStickers: Stickers? = null
        private var installedExtraStickers: Stickers? = null
        private var recommendedStickers: Stickers? = null

        private val isLoading: Boolean
            get() = installedStickers == null || installedExtraStickers == null || recommendedStickers == null

        fun setStickers(stickersRaw: TdApi.Object, @StickersType type: Int) {
            val stickers = if (stickersRaw.getConstructor() == Stickers.CONSTRUCTOR) stickersRaw as Stickers else Stickers(arrayOfNulls<Sticker>(0))
            when (type) {
                StickersType.INSTALLED -> {
                    this.installedStickers = stickers
                    haveInstalledStickers.notifyConditionChanged()
                }

                StickersType.INSTALLED_EXTRA -> {
                    this.installedExtraStickers = stickers
                    haveInstalledExtraStickers.notifyConditionChanged()
                }

                StickersType.RECOMMENDED -> {
                    this.recommendedStickers = stickers
                    haveRecommendedStickers.notifyConditionChanged()
                }
            }
        }

        private val haveInstalledStickers: ConditionalExecutor
        private val haveInstalledExtraStickers: ConditionalExecutor
        private val haveRecommendedStickers: ConditionalExecutor

        init {
            this.haveInstalledStickers = ConditionalExecutor(FutureBool { this.installedStickers != null }
            )
            this.haveInstalledExtraStickers = ConditionalExecutor(FutureBool { this.installedExtraStickers != null }
            )
            this.haveRecommendedStickers = ConditionalExecutor(FutureBool { this.recommendedStickers != null }
            )

            if (!isComplexQuery) {
                setStickers(noStickers(), StickersType.INSTALLED_EXTRA)
            }
            if (!needRecommended) {
                setStickers(noStickers(), StickersType.RECOMMENDED)
            }
            tdlib.client().send(
                GetStickers(stickerType, query, limit, chatId),
                Client.ResultHandler { `object`: TdApi.Object? -> setStickers(`object`!!, StickersType.INSTALLED) }
            )
            if (isComplexQuery) {
                val inputLanguageCodes = U.getInputLanguages()
                tdlib.send<EmojiKeywords?>(SearchEmojis(query, inputLanguageCodes), Tdlib.ResultHandler { keywords: EmojiKeywords?, error: TdApi.Error? ->
                    if (keywords != null && keywords.emojiKeywords.size > 0) {
                        val emojis: Array<String> = keywords.emojiKeywords.findUniqueEmojis()
                        val emojisQuery = TextUtils.join(" ", emojis)
                        // Request 2x more than limit for the case all of the stickers returned by GetStickers
                        tdlib.client().send(
                            GetStickers(stickerType, emojisQuery, limit * 2, chatId),
                            Client.ResultHandler { `object`: TdApi.Object? -> setStickers(`object`!!, StickersType.INSTALLED_EXTRA) }
                        )
                        if (needRecommended) {
                            tdlib.client().send(
                                SearchStickers(
                                    stickerType, emojisQuery,
                                    query, inputLanguageCodes, 0, limit * 3
                                ), Client.ResultHandler { `object`: TdApi.Object? -> setStickers(`object`!!, StickersType.RECOMMENDED) }
                            )
                        }
                    } else {
                        setStickers(noStickers(), StickersType.INSTALLED_EXTRA)
                        if (needRecommended) {
                            setStickers(noStickers(), StickersType.RECOMMENDED)
                        }
                    }
                })
            } else {
                if (needRecommended) {
                    // Request 2x more than limit for the case all of the stickers returned by GetStickers
                    tdlib.client().send(
                        SearchStickers(
                            stickerType,
                            query, null, U.getInputLanguages(), 0, limit * 2
                        ), Client.ResultHandler { `object`: TdApi.Object? -> setStickers(`object`!!, StickersType.RECOMMENDED) }
                    )
                }
            }
        }

        private fun getInstalledStickers(onlyExtra: Boolean): Array<Sticker> {
            val installedCount = (if (this.installedStickers != null) this.installedStickers!!.stickers.size else 0)
            val maxCount = installedCount + (if (this.installedExtraStickers != null) this.installedExtraStickers!!.stickers.size else 0)
            val stickers: MutableList<Sticker> = ArrayList<Sticker>(maxCount)
            if (this.installedStickers != null && !onlyExtra) {
                Collections.addAll<Sticker>(stickers, *this.installedStickers!!.stickers)
            }
            if (this.installedExtraStickers != null && this.installedExtraStickers!!.stickers.size > 0) {
                val excludeStickerIds = LongSet(installedCount)
                if (installedCount > 0) {
                    for (sticker in this.installedStickers!!.stickers) {
                        excludeStickerIds.add(sticker.id)
                    }
                }
                stickers.ensureCapacity(stickers.size + this.installedExtraStickers!!.stickers.size)
                for (sticker in this.installedExtraStickers!!.stickers) {
                    if (!excludeStickerIds.has(sticker.id)) {
                        stickers.add(sticker)
                    }
                }
            }
            return stickers.toTypedArray<Sticker>()
        }

        private fun getRecommendedStickers(excludeStickers: Array<Sticker>?): Array<Sticker> {
            val maxCount = if (this.recommendedStickers != null) this.recommendedStickers!!.stickers.size else 0
            val stickers: MutableList<Sticker> = ArrayList<Sticker>(maxCount)
            if (this.recommendedStickers != null) {
                if (excludeStickers != null && excludeStickers.size > 0) {
                    val excludeStickerIds = LongSet(excludeStickers.size)
                    for (sticker in excludeStickers) {
                        excludeStickerIds.add(sticker.id)
                    }
                    for (sticker in this.recommendedStickers!!.stickers) {
                        if (!excludeStickerIds.has(sticker.id)) {
                            stickers.add(sticker)
                        }
                    }
                } else {
                    Collections.addAll<Sticker>(stickers, *this.recommendedStickers!!.stickers)
                }
            }
            return stickers.toTypedArray<Sticker>()
        }

        interface Callback {
            fun onStickersLoaded(context: EmojiStickers?, installedStickers: Array<Sticker>, recommendedStickers: Array<Sticker>?, expectMoreStickers: Boolean)

            fun onMoreInstalledStickersLoaded(context: EmojiStickers?, moreInstalledStickers: Array<Sticker>) {}
            fun onRecommendedStickersLoaded(context: EmojiStickers?, recommendedStickers: Array<Sticker>) {}
            fun onAllStickersFinishedLoading(context: EmojiStickers?) {}
        }

        fun getStickers(callback: Callback, totalTimeoutMs: Long) {
            if (totalTimeoutMs <= 0) {
                // Lazy path: wait for all methods to finish, invoke callback.onStickersLoaded
                haveInstalledStickers.executeOrPostponeTask(Runnable {
                    haveInstalledExtraStickers.executeOrPostponeTask(Runnable {
                        haveRecommendedStickers.executeOrPostponeTask(
                            Runnable {
                                val installedStickers = getInstalledStickers(false)
                                val recommendedStickers = getRecommendedStickers(installedStickers)
                                tdlib.uiExecute(Runnable { callback.onStickersLoaded(this, installedStickers, recommendedStickers, false) }
                                )
                            })
                    })
                })
                return
            }

            // Async path: wait up to max(totalTimeoutMs, first non-empty result)
            // Then invoke callback.onStickersLoaded with at least one sticker.
            // If `expectMoreStickers` was true:
            // - callback.onMoreInstalledStickersLoaded gets called if more installed stickers were loaded (non-empty)
            // - callback.onRecommendedStickersLoaded gets called if recommended stickers were loaded (might be empty)
            // - callback.onAllStickersFinishedLoading gets called after all requests are complete, no more stickers
            val startTime = SystemClock.uptimeMillis()

            haveInstalledStickers.executeOrPostponeTask(Runnable {
                val timeoutSignal = CancellationSignal()
                val timeoutPostponed = AtomicBoolean(false)
                val isExpectingMoreStickers = AtomicBoolean(false)
                val postponeTimeout = Runnable {
                    if (timeoutPostponed.getAndSet(true)) {
                        return@Runnable
                    }
                    val elapsedMs = SystemClock.uptimeMillis() - startTime
                    val timeoutMs = max(0, totalTimeoutMs - elapsedMs)
                    tdlib.runOnTdlibThread(Runnable {
                        synchronized(isExpectingMoreStickers) {
                            if (timeoutSignal.isCanceled()) {
                                // Do nothing, because result was already sent
                                return@Runnable
                            }
                            val installedStickers = getInstalledStickers(false)
                            val recommendedStickers = if (this.recommendedStickers != null) getRecommendedStickers(installedStickers) else null
                            val expectMoreStickers = this.isLoading
                            tdlib.ui().post(Runnable { callback.onStickersLoaded(this, installedStickers, recommendedStickers, expectMoreStickers) }
                            )
                            isExpectingMoreStickers.set(expectMoreStickers)
                            timeoutSignal.cancel()
                        }
                    }, timeoutMs.toDouble() / 1000.0, false)
                }
                haveInstalledExtraStickers.executeOrPostponeTask(Runnable {
                    if (installedExtraStickers!!.stickers.size > 0) {
                        synchronized(isExpectingMoreStickers) {
                            if (isExpectingMoreStickers.get()) {
                                val installedStickers = getInstalledStickers(true)
                                tdlib.ui().post(Runnable { callback.onMoreInstalledStickersLoaded(this, installedStickers) }
                                )
                            }
                        }
                    }
                    haveRecommendedStickers.executeOrPostponeTask(Runnable {
                        val installedStickers: Array<Sticker>?
                        val recommendedStickers: Array<Sticker>?
                        val callbackExecuted: Boolean
                        synchronized(isExpectingMoreStickers) {
                            timeoutSignal.cancel()
                            installedStickers = getInstalledStickers(false)
                            recommendedStickers = getRecommendedStickers(installedStickers)
                            callbackExecuted = isExpectingMoreStickers.get()
                            if (callbackExecuted) {
                                tdlib.ui().post(Runnable {
                                    callback.onRecommendedStickersLoaded(this, recommendedStickers)
                                    callback.onAllStickersFinishedLoading(this)
                                })
                            }
                        }
                        if (!callbackExecuted) {
                            tdlib.uiExecute(Runnable { callback.onStickersLoaded(this, installedStickers!!, recommendedStickers, false) }
                            )
                        }
                    })
                    if (installedExtraStickers!!.stickers.size > 0) {
                        postponeTimeout.run()
                    }
                })
                if (installedStickers!!.stickers.size > 0) {
                    postponeTimeout.run()
                }
            })
        }

        companion object {
            private fun noStickers(): Stickers {
                return Stickers(arrayOfNulls<Sticker>(0))
            }
        }
    }

    fun getEmojiStickers(stickerType: StickerType, query: String?, isComplexQuery: Boolean, limit: Int, chatId: Long): EmojiStickers {
        var mode: Int
        if (stickerType.getConstructor() == TdApi.StickerTypeCustomEmoji.CONSTRUCTOR) {
            mode = Settings.instance().emojiMode
        } else {
            mode = Settings.instance().stickerMode
        }
        if (tdlib.suggestOnlyApiStickers() && mode == Settings.STICKER_MODE_ALL) {
            mode = Settings.STICKER_MODE_ONLY_INSTALLED
        }
        return EmojiStickers(tdlib, stickerType, query, isComplexQuery, limit, chatId, mode == Settings.STICKER_MODE_ALL)
    }

    interface MessageProvider {
        val isSponsoredMessage: Boolean
            get() = false
        val visibleSponsoredMessage: SponsoredMessage?
            get() = null
        val isMediaGroup: Boolean
            get() = false
        val visibleMediaGroup: MutableList<TdApi.Message?>?
            get() = null
        val visibleMessage: TdApi.Message?

        @get:TdlibMessageViewer.Flags
        val visibleMessageFlags: Int
            get() = 0
        val visibleChatId: Long
            get() {
                val message = this.visibleMessage
                return if (message != null) message.chatId else 0
            }
    }

    interface MessageViewCallback {
        fun onMessageViewed(
            viewport: Viewport?,
            view: View?,
            message: TdApi.Message?,
            @TdlibMessageViewer.Flags flags: Long,
            viewId: Long,
            allowRequest: Boolean
        ): Boolean

        fun needForceRead(viewport: Viewport?): Boolean {
            return false
        }

        fun allowViewRequest(viewport: Viewport?): Boolean {
            return true
        }

        fun onSponsoredMessageViewed(
            viewport: Viewport?,
            view: View?,
            sponsoredMessage: SponsoredMessage?,
            @TdlibMessageViewer.Flags flags: Long,
            viewId: Long,
            allowRequest: Boolean
        ) {
            // Do nothing?
        }

        fun isMessageContentVisible(viewport: Viewport?, view: View?): Boolean {
            return true
        }
    }

    @JvmOverloads
    fun attachViewportToRecyclerView(viewport: Viewport, recyclerView: RecyclerView, callback: MessageViewCallback? = null): Runnable {
        val viewMessages = Runnable {
            if (viewport.isDestroyed()) {
                return@Runnable
            }
            val allowViewRequest = callback == null || callback.allowViewRequest(viewport)
            val forceRead = callback != null && callback.needForceRead(viewport)
            val manager = recyclerView.getLayoutManager() as LinearLayoutManager?
            checkNotNull(manager)
            val startIndex = manager.findFirstVisibleItemPosition()
            val endIndex = manager.findLastVisibleItemPosition()
            val viewId = SystemClock.uptimeMillis()
            var viewedMessageCount = 0
            if (startIndex != -1 && endIndex != -1) {
                for (index in startIndex..endIndex) {
                    val view = manager.findViewByPosition(index)
                    if (view is MessageProvider) {
                        val provider = view as MessageProvider
                        val canViewMessage = callback == null || callback.isMessageContentVisible(viewport, view)
                        if (!canViewMessage) {
                            continue
                        }
                        @TdlibMessageViewer.Flags val flags = provider.visibleMessageFlags
                        if (provider.isSponsoredMessage) {
                            val sponsoredMessage = provider.visibleSponsoredMessage
                            val chatId = provider.visibleChatId
                            if (sponsoredMessage != null && viewport.addVisibleMessage(chatId, sponsoredMessage, flags.toLong(), viewId, false)) {
                                if (callback != null) {
                                    callback.onSponsoredMessageViewed(viewport, view, sponsoredMessage, flags.toLong(), viewId, allowViewRequest)
                                }
                                viewedMessageCount++
                            }
                        } else if (provider.isMediaGroup) {
                            val mediaGroup = provider.visibleMediaGroup
                            if (mediaGroup != null) {
                                for (message in mediaGroup) {
                                    val forceMarkAsRecent =
                                        callback != null && callback.onMessageViewed(viewport, view, message, flags.toLong(), viewId, allowViewRequest)
                                    if (viewport.addVisibleMessage(message, flags.toLong(), viewId, forceMarkAsRecent)) {
                                        viewedMessageCount++
                                    }
                                }
                            }
                        } else {
                            val message = provider.visibleMessage
                            if (message != null) {
                                val forceMarkAsRecent =
                                    callback != null && callback.onMessageViewed(viewport, view, message, flags.toLong(), viewId, allowViewRequest)
                                if (viewport.addVisibleMessage(message, flags.toLong(), viewId, forceMarkAsRecent)) {
                                    viewedMessageCount++
                                }
                            }
                        }
                    }
                }
            }
            viewport.removeOtherVisibleMessagesByViewId(viewId)
            if (allowViewRequest && (viewedMessageCount > 0 || viewport.haveRecentlyViewedMessages())) {
                viewport.viewMessages(true, forceRead, null)
            }
        }
        val onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                viewMessages.run()
            }

            private var isScrolling = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val wasScrolling = this.isScrolling
                this.isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE
                if (this.isScrolling != wasScrolling && !this.isScrolling) {
                    viewMessages.run()
                }
            }
        }
        viewport.addOnDestroyListener(Runnable { recyclerView.removeOnScrollListener(onScrollListener) }
        )
        recyclerView.addOnScrollListener(onScrollListener)
        return viewMessages
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [BirthdateOpenOrigin.SUGGESTED_ACTION, BirthdateOpenOrigin.PROFILE, BirthdateOpenOrigin.PRIVACY_SETTINGS
        ]
    )
    annotation class BirthdateOpenOrigin {
        companion object {
            const val SUGGESTED_ACTION: Int = 0
            const val PROFILE: Int = 1
            const val PRIVACY_SETTINGS: Int = 2
        }
    }

    fun openBirthdateEditor(context: ViewController<*>, view: View?, @BirthdateOpenOrigin origin: Int) {
        val userFull = tdlib.myUserFull()
        if (userFull == null) return
        val currentBirthdate = userFull.birthdate

        val act = RunnableData { hint: String? ->
            val b = ViewController.Options.Builder()
            if (origin != BirthdateOpenOrigin.PRIVACY_SETTINGS && !isEmpty(hint)) {
                b.info(hint)
            }
            b.item(
                OptionItem.Builder()
                    .id(R.id.btn_birthdate)
                    .icon(R.drawable.baseline_date_range_24)
                    .name(if (currentBirthdate == null) R.string.MenuBirthdateSet else R.string.MenuBirthdateEdit)
                    .color(if (currentBirthdate == null) ViewController.OptionColor.BLUE else ViewController.OptionColor.NORMAL)
                    .build()
            )
            if (origin != BirthdateOpenOrigin.PRIVACY_SETTINGS) {
                b.item(
                    OptionItem.Builder()
                        .id(R.id.btn_privacySettings)
                        .icon(R.drawable.baseline_lock_24)
                        .name(R.string.MenuBirthdatePrivacy)
                        .build()
                )
            }
            if (currentBirthdate != null) {
                b.item(
                    OptionItem.Builder()
                        .id(R.id.btn_delete)
                        .icon(R.drawable.baseline_cancel_24)
                        .color(ViewController.OptionColor.RED)
                        .name(R.string.MenuBirthdateRemove)
                        .build()
                )
            }
            if (origin == BirthdateOpenOrigin.SUGGESTED_ACTION) {
                b.item(
                    OptionItem.Builder()
                        .id(R.id.btn_suggestion)
                        .icon(R.drawable.baseline_close_24)
                        .name(R.string.ReminderSetBirthdateHide)
                        .color(ViewController.OptionColor.RED)
                        .build()
                )
            }
            if (b.itemCount() == 1) {
                showBirthdatePicker(context, currentBirthdate)
                return@RunnableData
            }
            context.showOptions(b.build(), OptionDelegate { optionItemView: View?, id: Int ->
                if (id == R.id.btn_privacySettings) {
                    val c = SettingsPrivacyKeyController(context.context(), context.tdlib())
                    c.setArguments(SettingsPrivacyKeyController.Args(UserPrivacySettingShowBirthdate()))
                    context.navigateTo(c)
                } else if (id == R.id.btn_birthdate) {
                    showBirthdatePicker(context, currentBirthdate)
                } else if (id == R.id.btn_delete) {
                    context.tdlib()!!.send<TdApi.Ok?>(SetBirthdate(null), Tdlib.ResultHandler { ok: TdApi.Ok?, setError: TdApi.Error? ->
                        if (setError != null) {
                            context.runOnUiThreadOptional(Runnable {
                                context.showErrorTooltip(view, TD.toErrorString(setError))
                            })
                        }
                    })
                } else if (id == R.id.btn_suggestion) {
                    context.tdlib()!!.send<TdApi.Ok?>(HideSuggestedAction(SuggestedActionSetBirthdate()), tdlib.typedOkHandler())
                }
                true
            })
        }

        if (origin == BirthdateOpenOrigin.PRIVACY_SETTINGS) {
            // Skip privacy request
            act.runWithData(null)
            return
        }

        context.tdlib()!!.send<UserPrivacySettingRules?>(
            GetUserPrivacySettingRules(UserPrivacySettingShowBirthdate()),
            Tdlib.ResultHandler { rules: UserPrivacySettingRules?, error: TdApi.Error? ->
                context.runOnUiThreadOptional(
                    Runnable {
                        if (error != null) {
                            context.showErrorTooltip(view, TD.toErrorString(error))
                            return@Runnable
                        }
                        val privacy = PrivacySettings.valueOf(rules)
                        val hint = TD.getPrivacyRulesString(context.tdlib(), UserPrivacySettingShowBirthdate.CONSTRUCTOR, privacy)
                        act.runWithData(hint)
                    })
            })
    }

    fun showBirthdatePicker(controller: ViewController<*>, currentBirthdate: Birthdate?) {
        val day: Int
        val month: Int
        val year: Int
        if (currentBirthdate != null) {
            day = currentBirthdate.day
            month = currentBirthdate.month - 1
            year = currentBirthdate.year
        } else {
            val c = Calendar.getInstance()
            day = c.get(Calendar.DAY_OF_MONTH)
            month = c.get(Calendar.MONTH)
            year = 0
        }
        controller.showCalendarDatePicker(
            Lang.getString(R.string.BirthdayPopupTitle),
            Lang.getString(R.string.Save),
            day,
            month,
            year,
            true,
            object : ViewController.CalendarDatePickerListener { override fun onCommitDate(picker: ColumnDataPicker?, commitButtonView: View?, newDay: Int, newMonth: Int, newYear: Int): Boolean {
                val setBirthdate = RunnableData { birthdate: Birthdate? ->
                    tdlib.send<TdApi.Ok?>(SetBirthdate(birthdate), Tdlib.ResultHandler { ok: TdApi.Ok?, error: TdApi.Error? ->
                        controller.runOnUiThreadOptional(
                            Runnable {
                                if (error != null) {
                                    if (picker!!.hasVisiblePopUp()) {
                                        picker.popupLayout().showErrorTooltip(controller, commitButtonView, TD.toErrorString(error))
                                    }
                                } else {
                                    picker!!.dismissPopup(true)
                                }
                            })
                    })
                }
                val newBirthdate = Birthdate(newDay, newMonth + 1, newYear)
                val str = Lang.getBirthdate(newBirthdate, false, true)

                val options = controller.getOptions(
                    Lang.getStringBold(R.string.SetBirthdateWarning, str),
                    intArrayOf(R.id.btn_send, R.id.btn_cancel),
                    arrayOf<String?>(Lang.getString(R.string.SetBirthdateOk), Lang.getString(R.string.Cancel)),
                    intArrayOf(ViewController.OptionColor.BLUE, ViewController.OptionColor.NORMAL),
                    intArrayOf(R.drawable.baseline_check_24, R.drawable.baseline_cancel_24),
                    Text.LINE_COUNT_UNLIMITED
                )
                options.ignoreOtherPopUps = true
                controller.showOptions(options, OptionDelegate { optionItemView: View?, id: Int ->
                    if (id == R.id.btn_send) {
                        setBirthdate.runWithData(newBirthdate)
                    }
                    true
                })
                return false
            }}
        )
    }

    fun shareCallLogs(context: ViewController<*>, logFiles: VoIPLogs.Pair?, needWarning: Boolean) {
        if (logFiles != null && logFiles.exists()) {
            val act = Runnable {
                val c = ShareController(context.context(), tdlib)
                val list: MutableList<ShareController.FileInfo?> = ArrayList<ShareController.FileInfo?>()
                if (logFiles.hasPrimaryLogFile()) {
                    list.add(ShareController.FileInfo(logFiles.logFile.getPath(), "text/plain"))
                }
                if (logFiles.hasStatsLogFile()) {
                    list.add(ShareController.FileInfo(logFiles.statsLogFile.getPath(), "text/plain"))
                }
                val array = list.toTypedArray<ShareController.FileInfo?>()
                c.setArguments(ShareController.Args(array))
                c.show()
            }
            if (needWarning) {
                context.showWarning(Lang.getMarkdownStringSecure(context, R.string.CallLogsWarning), RunnableBool { accepted: Boolean ->
                    if (accepted) {
                        act.run()
                    }
                })
            } else {
                act.run()
            }
        }
    }

    companion object {
        @JvmStatic
        val availablePriorityOrImportanceList: IntArray
            get() = intArrayOf(
                NotificationManager.IMPORTANCE_HIGH,  // Sound and pop-up (default)
                NotificationManager.IMPORTANCE_DEFAULT,  // Sound
                NotificationManager.IMPORTANCE_LOW,  // Silent
                NotificationManager.IMPORTANCE_MIN,  // Silent and minimized
            )

        @get:Suppress("deprecation")
        private val availablePriorityListLegacy: IntArray
            get() = intArrayOf(
                Notification.PRIORITY_MAX,
                Notification.PRIORITY_HIGH,  // (default)
                Notification.PRIORITY_LOW,
            )

        @JvmStatic
        @IdRes
        fun getPriorityOrImportanceId(priorityOrImportance: Int): Int {
            when (priorityOrImportance) {
                NotificationManager.IMPORTANCE_HIGH -> return R.id.btn_importanceHigh
                NotificationManager.IMPORTANCE_DEFAULT -> return R.id.btn_importanceDefault
                NotificationManager.IMPORTANCE_LOW -> return R.id.btn_importanceLow
                NotificationManager.IMPORTANCE_MIN -> return R.id.btn_importanceMin
            }
            throw IllegalArgumentException("priorityOrImportance == " + priorityOrImportance)
        }

        @JvmStatic
        @StringRes
        fun getPriorityOrImportanceString(priorityOrImportance: Int, hasSound: Boolean, hasVibration: Boolean): Int {
            if (priorityOrImportance == TdlibNotificationManager.PRIORITY_OR_IMPORTANCE_UNSET) return R.string.Default
            when (priorityOrImportance) {
                NotificationManager.IMPORTANCE_MAX, NotificationManager.IMPORTANCE_HIGH -> return if (hasSound) R.string.NotificationImportanceHigh else if (hasVibration) R.string.NotificationImportanceHighNoSound else R.string.NotificationImportanceHighMuted
                NotificationManager.IMPORTANCE_DEFAULT -> return if (hasSound) R.string.NotificationImportanceDefault else if (hasVibration) R.string.NotificationImportanceDefaultNoSound else R.string.NotificationImportanceDefaultMuted
                NotificationManager.IMPORTANCE_LOW -> return if (hasSound || hasVibration) R.string.NotificationImportanceLow else R.string.NotificationImportanceLowMuted
                NotificationManager.IMPORTANCE_MIN -> return R.string.NotificationImportanceMin
                NotificationManager.IMPORTANCE_NONE -> return R.string.NotificationImportanceNone
                else -> throw IllegalArgumentException("priorityOrImportance == " + priorityOrImportance)
            }
        }

        // Unsorted UI-related common stuff
        private fun deleteSuperGroupMessages(context: ViewController<*>, deletingMessages: Array<MessageWithProperties>?, after: Runnable?): Boolean {
            val tdlib = context.tdlib()!!
            if (deletingMessages == null || deletingMessages.size == 0) {
                return false
            }
            val chatId = deletingMessages.findUniqueChatId()
            if (chatId == 0L || !context.tdlib()!!.isSupergroup(chatId)) {
                // Chat is not supergroup
                return false
            }
            val status = tdlib.chatStatus(chatId)
            if (status == null || !TD.isAdmin(status) || (status.getConstructor() == ChatMemberStatusAdministrator.CONSTRUCTOR && !(status as ChatMemberStatusAdministrator).rights.canDeleteMessages)) {
                // User is not a creator or admin with canDeleteMessages right
                return false
            }
            val senderId = deletingMessages.findUniqueSenderId()
            if (senderId == null || context.tdlib()!!.isSelfSender(senderId)) {
                // No need in "delete all" for outgoing messages
                return false
            }
            for (deletingMessage in deletingMessages) {
                // No need in "delete all" for outgoing messages
                // or some of the passed messages can't be deleted at all
                if (deletingMessage.message.isOutgoing ||
                    !(deletingMessage.properties.canBeDeletedForAllUsers || deletingMessage.properties.canBeDeletedOnlyForSelf)
                ) {
                    return false
                }
            }

            val name = tdlib.senderName(senderId, true)
            val text = Lang.pluralBold(R.string.QDeleteXMessagesFromY, deletingMessages.size.toLong(), name)

            val wrap = context.showSettings(
                SettingsWrapBuilder(R.id.btn_deleteSupergroupMessages).setHeaderItem(
                ListItem(ListItem.TYPE_INFO, R.id.text_title, 0, text, false)
            ).setRawItems(
                arrayOf<ListItem>(
                    ListItem(
                        ListItem.TYPE_CHECKBOX_OPTION,
                        R.id.btn_banMember,
                        0,
                        if (senderId.getConstructor() == MessageSenderUser.CONSTRUCTOR) R.string.RestrictUser else if (tdlib.isChannel((senderId as MessageSenderChat).chatId)) R.string.BanChannel else R.string.BanChat,
                        false
                    ),
                    ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_reportSpam, 0, R.string.ReportSpam, false),
                    ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_deleteAll, 0, Lang.getStringBold(R.string.DeleteAllFrom, name), false)
                )
            ).setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                if (id == R.id.btn_deleteSupergroupMessages) {
                    val banUser = result!!.get(R.id.btn_banMember) != 0
                    val reportSpam = result.get(R.id.btn_reportSpam) != 0
                    val deleteAll = result.get(R.id.btn_deleteAll) != 0

                    val messageIds = deletingMessages.toMessageIdsMap().valueAt(0)

                    if (banUser) {
                        tdlib.send<ChatMember?>(GetChatMember(chatId, senderId), Tdlib.ResultHandler { member: ChatMember?, error: TdApi.Error? ->
                            if (error != null) {
                                UI.showError(error)
                            } else {
                                context.runOnUiThreadOptional(Runnable {
                                    val myStatus = tdlib.chatStatus(chatId)
                                    if (myStatus != null) {
                                        val editController = EditRightsController(context.context(), context.tdlib()!!)
                                        editController.setArguments(EditRightsController.Args(chatId, senderId, true, myStatus, member))
                                        context.navigateTo(editController)
                                    }
                                })
                            }
                        })
                    }

                    if (reportSpam) {
                        tdlib.send<TdApi.Ok?>(ReportSupergroupSpam(toSupergroupId(chatId), messageIds), tdlib.typedOkHandler())
                    }

                    if (deleteAll) {
                        tdlib.send<TdApi.Ok?>(DeleteChatMessagesBySender(chatId, senderId), tdlib.typedOkHandler())
                    } else {
                        tdlib.deleteMessages(chatId, messageIds, true)
                    }

                    if (after != null) {
                        after.run()
                    }
                }
            }).setSaveStr(R.string.Delete).setSaveColorId(ColorId.textNegative)
            )
            if (wrap != null) {
                tdlib.client().send(GetChatMember(chatId, senderId), Client.ResultHandler { result: TdApi.Object? ->
                    if (result!!.getConstructor() == ChatMember.CONSTRUCTOR) {
                        val member = result as ChatMember
                        tdlib.ui().post(Runnable {
                            var role: CharSequence? = null
                            var newText: CharSequence? = null
                            if (member.status.getConstructor() == ChatMemberStatusCreator.CONSTRUCTOR) {
                                role = Lang.getString(R.string.RoleOwner)
                            } else if (member.status.getConstructor() == ChatMemberStatusBanned.CONSTRUCTOR) {
                                role = Lang.getString(R.string.RoleBanned)
                            } else if (!TD.isMember(member.status, false) && member.memberId.getConstructor() != MessageSenderChat.CONSTRUCTOR) {
                                role = Lang.getString(R.string.RoleLeft)
                            } else if (member.joinedChatDate != 0) {
                                role = Lang.getRelativeDate(
                                    member.joinedChatDate.toLong(),
                                    TimeUnit.SECONDS,
                                    tdlib.currentTimeMillis(),
                                    TimeUnit.MILLISECONDS,
                                    true,
                                    60,
                                    R.string.RoleMember,
                                    true
                                )
                            } else if (member.memberId.getConstructor() == MessageSenderChat.CONSTRUCTOR) {
                                role = Lang.getString(if (tdlib.isChannel(member.memberId.getSenderId())) R.string.RoleChannel else R.string.RoleGroup)
                            } else {
                                return@Runnable
                            }
                            if (newText == null) {
                                newText = Lang.plural(
                                    R.string.QDeleteXMessagesFromYRole,
                                    deletingMessages.size.toLong(),
                                    SpanCreator { target: CharSequence?, argStart: Int, argEnd: Int, argIndex: Int, needFakeBold: Boolean ->
                                        if (argIndex < 2) Lang.newBoldSpan(needFakeBold) else null
                                    },
                                    name,
                                    role
                                )
                            }
                            val i = wrap.adapter.indexOfViewById(R.id.text_title)
                            if (i != -1 && wrap.adapter.getItem(i)!!.setStringIfChanged(newText)) {
                                wrap.adapter.notifyItemChanged(i)
                            }
                        })
                    }
                })
                // TODO TDLib / server: ability to get totalCount with limit=0
                tdlib.client().send(SearchChatMessages(chatId, null, null, senderId, 0, 0, 1, null), Client.ResultHandler { result: TdApi.Object? ->
                    if (result!!.getConstructor() == FoundChatMessages.CONSTRUCTOR) {
                        val moreCount = (result as FoundChatMessages).totalCount - deletingMessages.size
                        if (moreCount > 0) {
                            tdlib.ui().post(Runnable {
                                val i = wrap.adapter.indexOfViewById(R.id.btn_deleteAll)
                                if (i != -1 && wrap.adapter.getItem(i)!!
                                        .setStringIfChanged(Lang.pluralBold(R.string.DeleteXMoreFrom, moreCount.toLong(), name))
                                ) {
                                    wrap.adapter.notifyItemChanged(i)
                                }
                            })
                        }
                    }
                })
            }

            return true
        }

        private fun deleteWithRevoke(context: ViewController<*>, deletingMessages: Array<MessageWithProperties>?, after: Runnable?): Boolean {
            if (deletingMessages == null || deletingMessages.size == 0) return false

            val tdlib = context.tdlib()!!
            val singleChatId = deletingMessages.findUniqueChatId()
            if (tdlib.isSelfChat(singleChatId)) {
                return false
            }

            val totalCount = deletingMessages.size
            var optionalCount = 0
            var outgoingMessageCount = 0
            var noRevokeCount = 0
            for (message in deletingMessages) {
                if (message.properties.canBeDeletedForAllUsers && message.properties.canBeDeletedOnlyForSelf) {
                    optionalCount++
                    if (message.message.isOutgoing) outgoingMessageCount++
                }
                if (!message.properties.canBeDeletedForAllUsers && message.properties.canBeDeletedOnlyForSelf) {
                    noRevokeCount++
                }
            }
            val revokeByDefault = outgoingMessageCount > 0
            if (optionalCount == 0) {
                return false
            }
            val title = SpannableStringBuilder(Lang.pluralBold(R.string.QDeleteXMessages, deletingMessages.size.toLong()))
            if (noRevokeCount > 0) {
                title.append("\n").append(Lang.pluralBold(R.string.DeleteXForMeWarning, noRevokeCount.toLong()))
            }
            val revokeFor: CharSequence?
            if (optionalCount == totalCount) {
                if (isMultiChat(singleChatId)) {
                    revokeFor = Lang.getString(R.string.DeleteForEveryone)
                } else {
                    revokeFor = Lang.getStringBold(R.string.DeleteForUser, tdlib.cache().userFirstName(tdlib.chatUserId(singleChatId)))
                }
            } else {
                if (isMultiChat(singleChatId)) {
                    revokeFor = Lang.pluralBold(R.string.DeleteXForEveryone, optionalCount.toLong())
                } else {
                    revokeFor = Lang.pluralBold(R.string.DeleteXForUser, optionalCount.toLong(), tdlib.cache().userFirstName(tdlib.chatUserId(singleChatId)))
                }
            }

            val noRevokeCountFinal = noRevokeCount

            context.showSettings(
                SettingsWrapBuilder(R.id.btn_deleteMessagesWithRevoke)
                .addHeaderItem(title)
                .setRawItems(
                    arrayOf<ListItem>(
                        ListItem(ListItem.TYPE_CHECKBOX_OPTION, R.id.btn_revokeMessages, 0, revokeFor, revokeByDefault)
                    )
                ).setIntDelegate(SettingsIntDelegate { id: Int, result: SparseIntArray? ->
                    if (id == R.id.btn_deleteMessagesWithRevoke) {
                        val revoke = result!!.get(R.id.btn_revokeMessages) != 0
                        if (revoke && noRevokeCountFinal > 0) {
                            val revokeMessages = arrayOfNulls<TdApi.Message>(deletingMessages.size - noRevokeCountFinal)
                            val noRevokeMessages = arrayOfNulls<TdApi.Message>(noRevokeCountFinal)
                            var revokeIndex = 0
                            var noRevokeIndex = 0
                            for (message in deletingMessages) {
                                if (message.properties.canBeDeletedForAllUsers) {
                                    revokeMessages[revokeIndex++] = message.message
                                } else {
                                    noRevokeMessages[noRevokeIndex++] = message.message
                                }
                            }
                            var messageIds = TD.getMessageIds(revokeMessages)
                            for (i in 0..<messageIds.size()) {
                                tdlib.deleteMessages(messageIds.keyAt(i), messageIds.valueAt(i), true)
                            }
                            messageIds = TD.getMessageIds(noRevokeMessages)
                            for (i in 0..<messageIds.size()) {
                                tdlib.deleteMessages(messageIds.keyAt(i), messageIds.valueAt(i), false)
                            }
                        } else {
                            val messageIds = deletingMessages.toMessageIdsMap()
                            for (i in 0..<messageIds.size()) {
                                tdlib.deleteMessages(messageIds.keyAt(i), messageIds.valueAt(i), revoke)
                            }
                        }
                        if (after != null) {
                            after.run()
                        }
                    }
                }).setSaveStr(R.string.Delete).setSaveColorId(ColorId.textNegative).setAllowResize(false)
            )
            return true
        }

        const val DURATION_MODE_FULL: Int = 0
        const val DURATION_MODE_SHORT: Int = 1
        const val DURATION_MODE_RELATIVE: Int = 2

        fun getDuration(unixTime: Long, timeUnit: TimeUnit, isFull: Boolean): String {
            return getDuration(unixTime, timeUnit, if (isFull) DURATION_MODE_FULL else DURATION_MODE_SHORT)
        }

        @JvmStatic
        fun getDuration(unixTime: Long, timeUnit: TimeUnit, mode: Int): String {
            var time = timeUnit.toSeconds(unixTime).toInt()
            if (time < 0) time = 0
            val resId: Int
            if (time < 60) {
                when (mode) {
                    DURATION_MODE_FULL -> resId = R.string.xSeconds
                    DURATION_MODE_SHORT -> resId = R.string.xSecondsShort
                    DURATION_MODE_RELATIVE -> resId = R.string.xSecondsRelative
                    else -> throw IllegalArgumentException("mode == " + mode)
                }
                return Lang.plural(resId, time.toLong())
            }
            time /= 60
            if (time < 60) {
                when (mode) {
                    DURATION_MODE_FULL -> resId = R.string.xMinutes
                    DURATION_MODE_SHORT -> resId = R.string.xMinutesShort
                    DURATION_MODE_RELATIVE -> resId = R.string.xMinutesRelative
                    else -> throw IllegalArgumentException("mode == " + mode)
                }
                return Lang.plural(resId, time.toLong())
            }
            time /= 60
            if (time < 24) {
                when (mode) {
                    DURATION_MODE_FULL -> resId = R.string.xHours
                    DURATION_MODE_SHORT -> resId = R.string.xHoursShort
                    DURATION_MODE_RELATIVE -> resId = R.string.xHoursRelative
                    else -> throw IllegalArgumentException("mode == " + mode)
                }
                return Lang.plural(resId, time.toLong())
            }
            time /= 24
            if (time < 7) {
                when (mode) {
                    DURATION_MODE_FULL -> resId = R.string.xDays
                    DURATION_MODE_SHORT -> resId = R.string.xDaysShort
                    DURATION_MODE_RELATIVE -> resId = R.string.xDaysRelative
                    else -> throw IllegalArgumentException("mode == " + mode)
                }
                return Lang.plural(resId, time.toLong())
            }
            time /= 7
            when (mode) {
                DURATION_MODE_FULL -> resId = R.string.xWeeks
                DURATION_MODE_SHORT -> resId = R.string.xWeeks
                DURATION_MODE_RELATIVE -> resId = R.string.xWeeksRelative
                else -> throw IllegalArgumentException("mode == " + mode)
            }
            return Lang.plural(resId, time.toLong())
        }

        const val MUTE_ENABLE: Int = 0
        const val MUTE_1HOUR: Int = 3600
        const val MUTE_8HOURS: Int = 28800
        const val MUTE_2DAYS: Int = 172800
        val MUTE_FOREVER: Int = Int.MAX_VALUE
        private val MUTE_MAX = 60 * 60 * 24 * 4 // 4 days

        @JvmStatic
        fun fillMuteOptions(
            ids: IntList,
            icons: IntList,
            strings: StringList,
            colors: IntList?,
            needEnable: Boolean,
            needDisable: Boolean,
            needTemporary: Boolean,
            allowCustomize: Boolean,
            enableDefault: Boolean,
            defaultInfo: String?,
            prioritizeCustomization: Boolean
        ) {
            if (needEnable) {
                if (enableDefault) {
                    strings.append(Lang.getString(R.string.NotificationsDefaultValue, Lang.getString(R.string.EnableNotifications)))
                } else {
                    strings.append(R.string.EnableNotifications)
                }
                icons.append(R.drawable.baseline_notifications_24)
                ids.append(R.id.btn_menu_enable)
                if (colors != null) {
                    colors.append(ViewController.OptionColor.NORMAL)
                }
            }

            if (defaultInfo != null) {
                strings.append(Lang.getString(R.string.NotificationsDefault, Lang.lowercase(defaultInfo)))
                icons.append(R.drawable.baseline_notifications_off_24)
                ids.append(R.id.btn_menu_resetToDefault)
                if (colors != null) {
                    colors.append(ViewController.OptionColor.NORMAL)
                }
            }

            if (needDisable) {
                strings.append(R.string.MuteForever)
                icons.append(R.drawable.baseline_notifications_off_24)
                ids.append(R.id.btn_menu_disable)
                if (colors != null) {
                    colors.append(ViewController.OptionColor.NORMAL)
                }
            }

            if (needTemporary) {
                strings.append(Lang.plural(R.string.MuteForXHours, 1))
                strings.append(Lang.plural(R.string.MuteForXHours, 8))
                strings.append(Lang.plural(R.string.MuteForXDays, 2))
                icons.append(R.drawable.baseline_notifications_paused_24)
                icons.append(R.drawable.baseline_notifications_paused_24)
                icons.append(R.drawable.baseline_notifications_paused_24)
                ids.append(R.id.btn_menu_1hour)
                ids.append(R.id.btn_menu_8hours)
                ids.append(R.id.btn_menu_2days)
                if (colors != null) {
                    colors.append(ViewController.OptionColor.NORMAL)
                    colors.append(ViewController.OptionColor.NORMAL)
                    colors.append(ViewController.OptionColor.NORMAL)
                }
            }

            if (allowCustomize) {
                ids.append(R.id.btn_menu_customize)
                strings.append(R.string.NotificationsCustomize)
                icons.append(R.drawable.baseline_settings_24)
                if (colors != null) {
                    colors.append(if (prioritizeCustomization) ViewController.OptionColor.RED else ViewController.OptionColor.NORMAL)
                }
            }
        }

        @JvmStatic
        fun getValueForSettings(muteFor: Int): String {
            return getValueForSettings(muteFor, false)
        }

        @JvmStatic
        fun getValueForSettings(muteFor: Int, isDefault: Boolean): String {
            if (muteFor > 0) {
                if (muteFor >= MUTE_MAX) {
                    return Lang.getString(if (isDefault) R.string.NotificationsDefaultDisabled else R.string.NotificationsDisabled)
                }

                val text: String?
                val minutes = Math.round(muteFor.toFloat() / 60f)
                val hours = Math.round(muteFor.toFloat() / (60f * 60f))
                val days = Math.round(muteFor.toFloat() / (60f * 60f * 24f))
                if (days > 0) {
                    text = Lang.plural(R.string.xDays, days.toLong())
                } else if (hours > 0) {
                    text = Lang.plural(R.string.xHours, hours.toLong())
                } else {
                    text = Lang.plural(R.string.xMinutes, max(0, minutes).toLong())
                }
                return Lang.getString(if (isDefault) R.string.NotificationsDefaultUnmutesIn else R.string.UnmutesInX, text)
            } else {
                return Lang.getString(if (isDefault) R.string.NotificationsDefaultEnabled else R.string.NotificationsEnabled)
            }
        }

        @JvmStatic
        fun getMuteDurationForId(@IdRes id: Int): Int {
            if (id == R.id.btn_menu_enable) {
                return MUTE_ENABLE
            } else if (id == R.id.btn_menu_1hour) {
                return MUTE_1HOUR
            } else if (id == R.id.btn_menu_8hours) {
                return MUTE_8HOURS
            } else if (id == R.id.btn_menu_2days) {
                return MUTE_2DAYS
            } else if (id == R.id.btn_menu_disable) {
                return MUTE_FOREVER
            }
            return -1
        }

        // Logs
        @JvmStatic fun sendTdlibLogs(context: ViewController<*>, old: Boolean, export: Boolean) {
            val tdlibLogFile = TdlibManager.getLogFile(old)
            if (tdlibLogFile == null || !tdlibLogFile.exists()) {
                UI.showToast("Log does not exist", Toast.LENGTH_SHORT)
                return
            }
            val size = tdlibLogFile.length()
            if (size == 0L) {
                UI.showToast("Log is empty", Toast.LENGTH_SHORT)
                return
            }

            val share = ShareController(context.context(), if (export) null else context.tdlib())
            share.setArguments(ShareController.Args(tdlibLogFile.getPath(), "text/plain"))
            share.show()
        }

        @JvmStatic
        fun clearLogs(old: Boolean, after: RunnableLong?) {
            Background.instance().post(Runnable {
                try {
                    val removedSize = TdlibManager.deleteLogFile(old)
                    val msg: String
                    if (removedSize == -1L) {
                        msg = Lang.getString(R.string.TdlibLogClearFail)
                    } else if (removedSize == 0L) {
                        msg = Lang.getString(R.string.TdlibLogClearEmpty)
                    } else {
                        msg = Lang.getString(R.string.TdlibLogClearOk, Strings.buildSize(removedSize))
                    }
                    UI.post(Runnable {
                        if (!isEmpty(msg)) {
                            UI.showToast(msg, Toast.LENGTH_SHORT)
                        }
                        if (after != null) {
                            after.runWithLong(TdlibManager.getLogFileSize(old))
                        }
                    })
                } catch (t: Throwable) {
                    Log.w("Cannot work with TG log", t)
                }
            })
        }

        // Open chat
        private fun showAccessError(context: TdlibDelegate, urlOpenParameters: UrlOpenParameters?, error: Int, isChannel: Boolean) {
            val res: Int
            when (error) {
                Tdlib.CHAT_ACCESS_BANNED -> res = if (isChannel) R.string.ChatAccessRestrictedChannel else R.string.ChatAccessRestrictedGroup
                Tdlib.CHAT_ACCESS_PRIVATE -> res = if (isChannel) R.string.ChatAccessPrivateChannel else R.string.ChatAccessPrivateGroup
                Tdlib.CHAT_ACCESS_FAIL -> res = R.string.ChatAccessFailed
                else -> res = R.string.ChatAccessFailed
            }
            showLinkTooltip(context.tdlib()!!, R.drawable.baseline_warning_24, Lang.getString(res), urlOpenParameters)
        }

        private const val CHAT_OPTION_KEEP_STACK = 1
        private val CHAT_OPTION_NO_OPEN = 1 shl 1
        private val CHAT_OPTION_NEED_PRIVATE_PROFILE = 1 shl 2
        private val CHAT_OPTION_ENSURE_HIGHLIGHT_AVAILABILITY = 1 shl 3
        private val CHAT_OPTION_PASSCODE_UNLOCKED = 1 shl 4
        private val CHAT_OPTION_REMOVE_DUPLICATES = 1 shl 5
        private val CHAT_OPTION_SCHEDULED_MESSAGES = 1 shl 6
        private val CHAT_OPTION_OPEN_PROFILE_IF_DUPLICATE = 1 shl 7
        private val CHAT_OPTION_OPEN_DIRECT_MESSAGES_CHAT = 1 shl 8
        private val CHAT_OPTION_FORCE_MESSAGES_VIEW = 1 shl 9

        private fun showLinkTooltip(tdlib: Tdlib, iconRes: Int, message: CharSequence, urlOpenParameters: UrlOpenParameters?) {
            requireNotNull(message)
            if (!UI.inUiThread()) {
                tdlib.ui().post(Runnable {
                    showLinkTooltip(tdlib, iconRes, message, urlOpenParameters)
                })
                return
            }
            if (urlOpenParameters != null && urlOpenParameters.tooltip != null && urlOpenParameters.tooltip!!.hasVisibleTarget()) {
                if (iconRes == 0) {
                    urlOpenParameters.tooltip!!.show(tdlib, message).hideDelayed(3500, TimeUnit.MILLISECONDS)
                } else {
                    TooltipBuilder(urlOpenParameters.tooltip!!).icon(iconRes).show(tdlib, message).hideDelayed(3500, TimeUnit.MILLISECONDS)
                }
            } else {
                UI.showToast(message, Toast.LENGTH_SHORT)
            }
        }

        private const val BOT_MODE_START = 0
        private const val BOT_MODE_START_IN_GROUP = 1
        private const val BOT_MODE_START_GAME = 2

        private fun checkChatFolderInviteLink(
            context: TdlibDelegate,
            inviteLinkUrl: String,
            invite: InternalLinkTypeChatFolderInvite,
            openParameters: UrlOpenParameters?
        ) {
            context.tdlib()!!.send<ChatFolderInviteLinkInfo?>(
                CheckChatFolderInviteLink(invite.inviteLink),
                Tdlib.ResultHandler { result: ChatFolderInviteLinkInfo?, error: TdApi.Error? ->
                    if (error != null) {
                        showLinkTooltip(context.tdlib()!!, R.drawable.baseline_error_24, TD.toErrorString(error), openParameters)
                    } else {
                        Companion.showChatFolderInviteLinkInfo(context, inviteLinkUrl, result!!)
                    }
                })
        }

        private fun showChatFolderInviteLinkInfo(context: TdlibDelegate, inviteLinkUrl: String, inviteLinkInfo: ChatFolderInviteLinkInfo) {
            if (TdlibManager.inBackgroundThread()) {
                context.tdlib()!!.ui().post(Runnable {
                    showChatFolderInviteLinkInfo(context, inviteLinkUrl, inviteLinkInfo)
                })
                return
            }
            val controller = ChatFolderInviteLinkController(context.context(), context.tdlib())
            controller.setArguments(ChatFolderInviteLinkController.Arguments(inviteLinkUrl, inviteLinkInfo))
            controller.show()
        }

        val INSTANT_VIEW_UNSPECIFIED: Int = -1
        const val INSTANT_VIEW_DISABLED: Int = 0
        const val INSTANT_VIEW_ENABLED: Int = 1

        val EMBED_VIEW_UNSPECIFIED: Int = -1
        const val EMBED_VIEW_ENABLED: Int = 0
        const val EMBED_VIEW_DISABLED: Int = 1

        const val TME_URL_NONE: Int = 0
        const val TME_URL_MESSAGE: Int = 1

        private const val TME_LINK_PROCESSED = "done"

        fun newProxyDescription(server: String?, port: String?): StringBuilder {
            val desc = StringBuilder("<b>")
            desc.append(Lang.getString(R.string.UseProxyServer))
            desc.append(":</b> ")
            desc.append(server)
            desc.append("<br/><b>")
            desc.append(Lang.getString(R.string.UseProxyPort))
            desc.append(":</b> ")
            desc.append(port)
            return desc
        }

        private fun addProxyField(desc: StringBuilder, name: String?, value: String?) {
            desc.append("<br/><b>")
            desc.append(name)
            desc.append(":</b> ")
            desc.append(value)
        }

        @JvmStatic
        fun stringForConnectionState(@ConnectionState state: Int): Int {
            when (state) {
                ConnectionState.CONNECTED -> return R.string.Connected
                ConnectionState.CONNECTING, ConnectionState.UNKNOWN -> return R.string.network_Connecting
                ConnectionState.CONNECTING_TO_PROXY -> return R.string.ConnectingToProxy
                ConnectionState.WAITING_FOR_NETWORK -> return R.string.network_WaitingForNetwork
                ConnectionState.UPDATING -> return R.string.network_Updating
            }
            throw RuntimeException()
        }

        const val MAP_PROVIDER_MODE_SECRET_TUTORIAL: Int = 0
        const val MAP_PROVIDER_MODE_SECRET: Int = 1
        const val MAP_PROVIDER_MODE_CLOUD: Int = 2

        @Throws(XmlPullParserException::class, IOException::class)
        private fun readStrings(parser: XmlPullParser, out: CustomLangPackResult) {
            parser.require(XmlPullParser.START_TAG, null, "resources")
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue
                val name = parser.getName()
                if ("string" == name) {
                    readString(parser, out)
                } else {
                    throw IllegalArgumentException("Unknown tag: " + name) // skip(parser);
                }
            }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun readString(parser: XmlPullParser, out: CustomLangPackResult) {
            parser.require(XmlPullParser.START_TAG, null, "string")
            val key = parser.getAttributeValue(null, "name").trim { it <= ' ' }
            require(!isEmpty(key))
            val b = StringBuilder()
            while (parser.next() != XmlPullParser.END_TAG) {
                when (parser.getEventType()) {
                    XmlPullParser.START_TAG -> skip(parser)
                    XmlPullParser.TEXT -> b.append(parser.getText())
                    XmlPullParser.ENTITY_REF -> b.append(parser.getText())
                    else -> throw IllegalArgumentException("eventType == " + parser.getEventType())
                }
            }
            val value = Strings.unwrap(key, b.toString())
            val i = key.lastIndexOf('_')
            if (i == -1) {
                out.strings.put(key, LanguagePackStringValueOrdinary(value))
            } else {
                val suffix = key.substring(i + 1)
                val pluralized: LanguagePackStringValuePluralized?
                when (suffix) {
                    "zero" -> {
                        pluralized = getPlural(key, suffix, out.strings)
                        pluralized.zeroValue = value
                    }

                    "one" -> {
                        pluralized = getPlural(key, suffix, out.strings)
                        pluralized.oneValue = value
                    }

                    "two" -> {
                        pluralized = getPlural(key, suffix, out.strings)
                        pluralized.twoValue = value
                    }

                    "few" -> {
                        pluralized = getPlural(key, suffix, out.strings)
                        pluralized.fewValue = value
                    }

                    "many" -> {
                        pluralized = getPlural(key, suffix, out.strings)
                        pluralized.manyValue = value
                    }

                    "other" -> {
                        pluralized = getPlural(key, suffix, out.strings)
                        pluralized.otherValue = value
                    }

                    else -> out.strings.put(key, LanguagePackStringValueOrdinary(value))
                }
            }
            val resourceId = Lang.getStringResourceIdentifier(key)
            if (resourceId == 0) {
                out.unknownStringsCount++
            }
        }

        private fun getPlural(key: String, suffix: String, map: MutableMap<String?, LanguagePackStringValue?>): LanguagePackStringValuePluralized {
            var key = key
            key = key.substring(0, key.length - suffix.length - 1)
            val string = map.get(key)
            if (string is LanguagePackStringValuePluralized) {
                return string
            }
            val pluralized = LanguagePackStringValuePluralized()
            map.put(key, pluralized)
            return pluralized
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun skip(parser: XmlPullParser) {
            check(parser.getEventType() == XmlPullParser.START_TAG)
            var depth = 1
            while (depth != 0) {
                when (parser.next()) {
                    XmlPullParser.END_TAG -> depth--
                    XmlPullParser.START_TAG -> depth++
                }
            }
        }

        @JvmStatic
        fun canInstallLanguage(document: TdApi.Document): Boolean {
            if ((!isEmpty(document.fileName) && document.fileName.endsWith(".xml")) || (!isEmpty(document.mimeType) && document.mimeType == "application/xml")) {
                // TODO quick check if file starts with <resources>?
                return document.document.size <= ByteUnit.MIB.toBytes(1.0) && TD.isFileLoadedAndExists(document.document)
            }
            return false
        }

        @JvmStatic fun removeAccount(context: ViewController<*>, account: TdlibAccount) {
            removeAccount(context, account, false)
        }

        private fun removeAccount(context: ViewController<*>, account: TdlibAccount, isSignOut: Boolean) {
            context.showOptions(
                Lang.getStringBold(if (isSignOut) R.string.SignOutHint2 else R.string.RemoveAccountHint2, account.getName()),
                intArrayOf(R.id.btn_removeAccount, R.id.btn_cancel),
                arrayOf<String?>(
                    Lang.getString(R.string.LogOut), Lang.getString(R.string.Cancel)
                ),
                intArrayOf(ViewController.OptionColor.RED, ViewController.OptionColor.NORMAL),
                intArrayOf(R.drawable.baseline_logout_24, R.drawable.baseline_cancel_24),
                OptionDelegate { itemView: View?, id: Int ->
                    if (id == R.id.btn_removeAccount) {
                        account.tdlib().signOut()
                    }
                    true
                })
        }

        private const val THEME_CONTEXT_NONE = 0
        private const val THEME_CONTEXT_MAIN = 1
        private const val THEME_CONTEXT_ATTRIBUTES = 2
        private const val THEME_CONTEXT_COLORS = 3

        @JvmStatic
        fun canInstallTheme(document: TdApi.Document?): Boolean {
            return document != null && !isEmpty(document.fileName) && document.fileName.endsWith(BuildConfig.THEME_FILE_EXTENSION) && TD.isFileLoadedAndExists(
                document.document
            )
        }

        @JvmStatic fun reportChats(context: ViewController<*>, chatIds: LongArray, after: Runnable?, forcedTheme: ThemeDelegate?) {
            val remaining = AtomicInteger(chatIds.size)
            val act: Runnable = object : Runnable {
                override fun run() {
                    val index = chatIds.size - remaining.getAndDecrement()
                    if (index < chatIds.size) {
                        val chatId = chatIds[index]
                        reportChat(context, chatId, null, forcedTheme, this, chatIds.size == 1)
                    } else if (after != null) {
                        after.run()
                    }
                }
            }
            act.run()
        }

        private const val REPORT_REASON_COUNT = 7

        private fun fillReportReasons(ids: IntList, strings: StringList) {
            ids.append(R.id.btn_reportChatSpam)
            // colors.append(ViewController.OPTION_COLOR_NORMAL);
            strings.append(R.string.Spam)

            ids.append(R.id.btn_reportChatFake)
            // colors.append(ViewController.OPTION_COLOR_RED);
            strings.append(R.string.Fake)

            ids.append(R.id.btn_reportChatViolence)
            // colors.append(ViewController.OPTION_COLOR_NORMAL);
            strings.append(R.string.Violence)

            ids.append(R.id.btn_reportChatPornography)
            // colors.append(ViewController.OPTION_COLOR_NORMAL);
            strings.append(R.string.Pornography)

            ids.append(R.id.btn_reportChatChildAbuse)
            // colors.append(ViewController.OPTION_COLOR_RED);
            strings.append(R.string.ChildAbuse)

            ids.append(R.id.btn_reportChatCopyright)
            // colors.append(ViewController.OPTION_COLOR_NORMAL);
            strings.append(R.string.Copyright)

            ids.append(R.id.btn_reportChatIllegalDrugs)
            // colors.append(ViewController.OPTION_COLOR_NORMAL);
            strings.append(R.string.IllegalDrugs)

            ids.append(R.id.btn_reportChatPersonalDetails)
            // colors.append(ViewController.OPTION_COLOR_NORMAL);
            strings.append(R.string.PersonalDetails)

            ids.append(R.id.btn_reportChatOther)
            // colors.append(ViewController.OPTION_COLOR_NORMAL);
            strings.append(R.string.Other)
        }

        private fun <T : TdApi.Function<*>?> toReportReasons(
            context: ViewController<*>,
            reportReasonId: Int,
            title: CharSequence?,
            request: T?,
            forceText: Boolean,
            reportCallback: RunnableData<T?>
        ) {
            var forceText = forceText
            val reason: ReportReason
            assertReportReason_cf03e541()
            if (reportReasonId == R.id.btn_reportChatSpam) {
                reason = ReportReasonSpam()
            } else if (reportReasonId == R.id.btn_reportChatFake) {
                reason = ReportReasonFake()
            } else if (reportReasonId == R.id.btn_reportChatViolence) {
                reason = ReportReasonViolence()
            } else if (reportReasonId == R.id.btn_reportChatPornography) {
                reason = ReportReasonPornography()
            } else if (reportReasonId == R.id.btn_reportChatCopyright) {
                reason = ReportReasonCopyright()
            } else if (reportReasonId == R.id.btn_reportChatChildAbuse) {
                reason = ReportReasonChildAbuse()
            } else if (reportReasonId == R.id.btn_reportChatIllegalDrugs) {
                reason = ReportReasonIllegalDrugs()
            } else if (reportReasonId == R.id.btn_reportChatPersonalDetails) {
                reason = ReportReasonPersonalDetails()
            } else if (reportReasonId == R.id.btn_reportChatOther) { // TODO replace with openInputAlert
                reason = ReportReasonCustom()
                forceText = true
            } else {
                throw IllegalArgumentException(Lang.getResourceEntryName(reportReasonId))
            }
            when (request!!.getConstructor()) {
                ReportChatPhoto.CONSTRUCTOR -> (request as ReportChatPhoto).reason = reason
                else -> throw UnsupportedOperationException(request.toString())
            }
            if (forceText) {
                val c = RequestController(context.context(), context.tdlib())
                c.setArguments(object : RequestController.Delegate {
                    override fun getName(): CharSequence? {
                        return title
                    }

                    override fun getPlaceholder(): Int {
                        return R.string.ReportReasonDescription
                    }

                    override fun performRequest(input: String?, callback: RunnableBool) {
                        if (isEmpty(input)) {
                            callback.runWithBool(false)
                            return
                        }
                        when (request.getConstructor()) {
                            ReportChatPhoto.CONSTRUCTOR -> (request as ReportChatPhoto).text = input
                        }
                        callback.runWithBool(true)
                        reportCallback.runWithData(request)
                    }
                })
                context.context().navigation().navigateTo(c)
            } else {
                reportCallback.runWithData(request)
            }
        }

        fun reportChatPhoto(context: ViewController<*>, chatId: Long, fileId: Int, after: Runnable?, forcedTheme: ThemeDelegate?) {
            val tdlib = context.tdlib()!!
            val title = Lang.getStringBold(R.string.ReportChatPhoto, tdlib.chatTitle(chatId))

            val ids: IntList = IntList(REPORT_REASON_COUNT)
            val strings: StringList = StringList(REPORT_REASON_COUNT)
            fillReportReasons(ids, strings)

            context.showOptions(title, ids.get(), strings.get(),  /*colors.get()*/null, null, OptionDelegate { itemView: View?, id: Int ->
                toReportReasons<ReportChatPhoto?>(
                    context,
                    id,
                    title,
                    ReportChatPhoto(chatId, fileId, null, null),
                    false,
                    RunnableData { request: ReportChatPhoto? ->
                        if (after != null) {
                            after.run()
                        }
                        tdlib.client().send(request, Client.ResultHandler { `object`: TdApi.Object? ->
                            when (`object`!!.getConstructor()) {
                                TdApi.Ok.CONSTRUCTOR -> UI.showToast(R.string.ReportChatSent, Toast.LENGTH_SHORT)
                                TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
                            }
                        })
                    })
                true
            }, forcedTheme)
        }

        @JvmStatic fun reportChat(
            context: ViewController<*>,
            chatId: Long,
            messages: Array<TdApi.Message>?,
            forcedTheme: ThemeDelegate?,
            after: Runnable?,
            needConfirmation: Boolean
        ) {
            val tdlib = context.tdlib()!!
            val messageIds: LongArray?
            val title: CharSequence?
            if (messages != null && messages.size > 0) {
                messageIds = LongArray(messages.size)

                var singleSender = true
                var senderId = messages[0].getSenderId()

                var i = 0
                for (message in messages) {
                    messageIds[i++] = message.id
                    if (singleSender && message.getSenderId() != senderId) {
                        singleSender = false
                        senderId = 0
                    }
                }
                if (singleSender) {
                    val confirmResId: Int
                    val resId: Int
                    if (isUserChat(senderId)) {
                        confirmResId = if (messages.size == 1) R.string.QReportMessageUser else R.string.QReportMessagesUser
                        resId = if (messages.size == 1) R.string.ReportMessageUser else R.string.ReportMessagesUser
                    } else {
                        confirmResId = if (messages.size == 1) R.string.QReportMessage else R.string.QReportMessages
                        resId = if (messages.size == 1) R.string.ReportMessage else R.string.ReportMessages
                    }
                    val name = tdlib.chatTitle(senderId)
                    title = Lang.getStringBold(if (needConfirmation) confirmResId else resId, name)
                } else {
                    title =
                        Lang.plural(if (needConfirmation) R.string.QReportXMessages else R.string.ReportXMessages, messages.size.toLong(), Lang.boldCreator())
                }
            } else {
                messageIds = null
                val chatTitle = tdlib.chatTitle(chatId)
                title = Lang.getStringBold(if (needConfirmation) R.string.QReportChat else R.string.ReportChat, chatTitle)
            }

            if (needConfirmation) {
                context.showOptions(
                    title,
                    intArrayOf(
                        R.id.btn_reportChat,
                        R.id.btn_cancel
                    ), arrayOf<String?>(
                        Lang.getString(R.string.ConfirmReportBtn),
                        Lang.getString(R.string.Cancel)
                    ), intArrayOf(
                        ViewController.OptionColor.RED,
                        ViewController.OptionColor.NORMAL
                    ),
                    intArrayOf(
                        R.drawable.baseline_warning_24,
                        R.drawable.baseline_cancel_24
                    ), OptionDelegate { optionItemView: View?, id: Int ->
                        if (id == R.id.btn_reportChat) {
                            reportChat(context, chatId, messages, forcedTheme, after, false)
                        }
                        true
                    }
                )
                return
            }

            val reportText = AtomicReference<String?>()

            //TODO(?): catch popup dismissal and call `after.run();`
            lateinit var reportChatHandler: Tdlib.ResultHandler<ReportChatResult?>
            reportChatHandler = object : Tdlib.ResultHandler<ReportChatResult?> {
                override fun onResult(result: ReportChatResult?, error: TdApi.Error?) {
                    if (error != null) {
                        UI.showError(error)
                        context.runOnUiThreadOptional(after)
                        return
                    }
                    when (result!!.getConstructor()) {
                        TdApi.ReportChatResultOk.CONSTRUCTOR -> {
                            context.runOnUiThreadOptional(Runnable {
                                UI.showToast(R.string.ReportChatSent, Toast.LENGTH_SHORT)
                                if (after != null) {
                                    after.run()
                                }
                            })
                        }

                        ReportChatResultOptionRequired.CONSTRUCTOR -> {
                            context.runOnUiThreadOptional(Runnable {
                                val optionRequired = result as ReportChatResultOptionRequired
                                val idToOptionId = SparseArrayCompat<ByteArray?>(optionRequired.options.size)

                                val b = ViewController.Options.Builder()
                                if (isEmpty(optionRequired.title)) {
                                    b.info(title)
                                } else {
                                    b.info(optionRequired.title)
                                }
                                var index = 0
                                for (option in optionRequired.options) {
                                    var id = ++index
                                    id = View.generateViewId()
                                    b.item(OptionItem(id, option.text, ViewController.OptionColor.NORMAL, 0))
                                    idToOptionId.put(id, option.id)
                                }
                                val popup = context.showOptions(b.build(), OptionDelegate { optionItemView: View?, id: Int ->
                                    val optionId = idToOptionId.get(id)
                                    if (optionId != null) {
                                        tdlib.send<ReportChatResult?>(ReportChat(chatId, optionId, messageIds, reportText.get()), reportChatHandler)
                                        return@OptionDelegate true
                                    }
                                    false
                                }, forcedTheme)
                                if (popup != null) {
                                    popup.setDisableCancelOnTouchDown(true)
                                }
                            })
                        }

                        ReportChatResultTextRequired.CONSTRUCTOR -> {
                            context.runOnUiThreadOptional(Runnable {
                                val textRequired = result as ReportChatResultTextRequired
                                val placeholder = Lang.getMarkdownString(
                                    context,
                                    if (textRequired.isOptional) R.string.ReportChatReasonOptional else R.string.ReportChatReasonRequired
                                )
                                context.openInputAlert(
                                    title,
                                    placeholder,
                                    R.string.ReportChatAlertBtn,
                                    R.string.Cancel,
                                    null,
                                    null,
                                    object : ViewController.InputAlertCallback { override fun onAcceptInput(inputView: MaterialEditTextGroup?, userInput: String?): Boolean {
                                        val text = trim(userInput)
                                        if (isEmpty(text) && !textRequired.isOptional) {
                                            return false
                                        } else {
                                            reportText.set(text)
                                            tdlib.send<ReportChatResult?>(ReportChat(chatId, textRequired.optionId, messageIds, text), reportChatHandler)
                                            return true
                                        }
                                    }},
                                    textRequired.isOptional,
                                    null,
                                    forcedTheme
                                )
                            })
                        }

                        TdApi.ReportChatResultMessagesRequired.CONSTRUCTOR -> {
                            UI.showToast(R.string.ReportChatMessagesRequired, Toast.LENGTH_SHORT)
                            context.runOnUiThreadOptional(after)
                        }

                        else -> {
                            assertReportChatResult_63f241a6()
                            throw unsupported(result)
                        }
                    }
                }
            }
            tdlib.send<ReportChatResult?>(ReportChat(chatId, null, messageIds, null), reportChatHandler)
        }

        @JvmStatic
        val tdlibVersionSignature: CharSequence?
            get() = Lang.getStringSecure(
                R.string.format_commit,
                Lang.codeCreator(),
                tdlibVersion(),
                tdlibCommitHash()
            )
    }
}
