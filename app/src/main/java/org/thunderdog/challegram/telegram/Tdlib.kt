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
 * File created on 14/02/2018
 */
@file:OptIn(kotlin.contracts.ExperimentalContracts::class)

package org.thunderdog.challegram.telegram

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.SparseIntArray
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.collection.LongSparseArray
import androidx.collection.SparseArrayCompat
import androidx.core.os.CancellationSignal
import androidx.core.util.Pair
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import com.google.android.recaptcha.RecaptchaAction.Companion.custom
import com.google.android.recaptcha.RecaptchaTasksClient
import com.google.firebase.FirebaseOptions
import me.vkryl.android.AppInstallationUtil.getInitiatorPackageName
import me.vkryl.android.AppInstallationUtil.getInstallerPackageName
import me.vkryl.android.AppInstallationUtil.isAppSideLoaded
import me.vkryl.core.collection.LongSparseIntArray
import me.vkryl.core.collection.LongSparseLongArray
import me.vkryl.core.contains
import me.vkryl.core.delete
import me.vkryl.core.equalsOrBothEmpty
import me.vkryl.core.hasFlag
import me.vkryl.core.indexOf
import me.vkryl.core.isEmpty
import me.vkryl.core.lambda.CancellableRunnable
import me.vkryl.core.lambda.Future
import me.vkryl.core.lambda.FutureBool
import me.vkryl.core.lambda.RunnableBool
import me.vkryl.core.lambda.RunnableData
import me.vkryl.core.lambda.RunnableInt
import me.vkryl.core.lambda.RunnableLong
import me.vkryl.core.parseInt
import me.vkryl.core.pickNumber
import me.vkryl.core.random
import me.vkryl.core.removeElement
import me.vkryl.core.requireNonNull
import me.vkryl.core.setFlag
import me.vkryl.core.ucfirst
import me.vkryl.core.urlWithoutProtocol
import me.vkryl.core.util.ConditionalExecutor
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.AddChatMember
import org.drinkless.tdlib.TdApi.AddLocalMessage
import org.drinkless.tdlib.TdApi.AddProxy
import org.drinkless.tdlib.TdApi.AddedProxies
import org.drinkless.tdlib.TdApi.AddedProxy
import org.drinkless.tdlib.TdApi.AgeVerificationParameters
import org.drinkless.tdlib.TdApi.AnimatedEmoji
import org.drinkless.tdlib.TdApi.AuthorizationState
import org.drinkless.tdlib.TdApi.AvailableReactions
import org.drinkless.tdlib.TdApi.BasicGroup
import org.drinkless.tdlib.TdApi.BasicGroupFullInfo
import org.drinkless.tdlib.TdApi.Chat
import org.drinkless.tdlib.TdApi.ChatActiveStories
import org.drinkless.tdlib.TdApi.ChatAvailableReactions
import org.drinkless.tdlib.TdApi.ChatAvailableReactionsAll
import org.drinkless.tdlib.TdApi.ChatAvailableReactionsSome
import org.drinkless.tdlib.TdApi.ChatFolder
import org.drinkless.tdlib.TdApi.ChatFolderIcon
import org.drinkless.tdlib.TdApi.ChatFolderInfo
import org.drinkless.tdlib.TdApi.ChatFolderInviteLink
import org.drinkless.tdlib.TdApi.ChatInviteLink
import org.drinkless.tdlib.TdApi.ChatInviteLinkInfo
import org.drinkless.tdlib.TdApi.ChatList
import org.drinkless.tdlib.TdApi.ChatLocation
import org.drinkless.tdlib.TdApi.ChatMember
import org.drinkless.tdlib.TdApi.ChatMemberStatus
import org.drinkless.tdlib.TdApi.ChatMemberStatusAdministrator
import org.drinkless.tdlib.TdApi.ChatMemberStatusBanned
import org.drinkless.tdlib.TdApi.ChatMemberStatusLeft
import org.drinkless.tdlib.TdApi.ChatMemberStatusRestricted
import org.drinkless.tdlib.TdApi.ChatNotificationSettings
import org.drinkless.tdlib.TdApi.ChatPermissions
import org.drinkless.tdlib.TdApi.ChatPhoto
import org.drinkless.tdlib.TdApi.ChatSource
import org.drinkless.tdlib.TdApi.ChatType
import org.drinkless.tdlib.TdApi.ChatTypeBasicGroup
import org.drinkless.tdlib.TdApi.ChatTypePrivate
import org.drinkless.tdlib.TdApi.ChatTypeSecret
import org.drinkless.tdlib.TdApi.ChatTypeSupergroup
import org.drinkless.tdlib.TdApi.CloseBirthdayUser
import org.drinkless.tdlib.TdApi.CloseChat
import org.drinkless.tdlib.TdApi.CloseSecretChat
import org.drinkless.tdlib.TdApi.ConfirmQrCodeAuthentication
import org.drinkless.tdlib.TdApi.CreateChatFolderInviteLink
import org.drinkless.tdlib.TdApi.CreateForumTopic
import org.drinkless.tdlib.TdApi.CreatePrivateChat
import org.drinkless.tdlib.TdApi.DeleteChatFolder
import org.drinkless.tdlib.TdApi.DeleteChatFolderInviteLink
import org.drinkless.tdlib.TdApi.DeleteChatHistory
import org.drinkless.tdlib.TdApi.DeleteForumTopic
import org.drinkless.tdlib.TdApi.DeleteMessages
import org.drinkless.tdlib.TdApi.Destroy
import org.drinkless.tdlib.TdApi.DeviceToken
import org.drinkless.tdlib.TdApi.DeviceTokenFirebaseCloudMessaging
import org.drinkless.tdlib.TdApi.DeviceTokenHuaweiPush
import org.drinkless.tdlib.TdApi.DeviceTokenSimplePush
import org.drinkless.tdlib.TdApi.DiceStickers
import org.drinkless.tdlib.TdApi.DiceStickersRegular
import org.drinkless.tdlib.TdApi.DisableProxy
import org.drinkless.tdlib.TdApi.DownloadFile
import org.drinkless.tdlib.TdApi.EditChatFolder
import org.drinkless.tdlib.TdApi.EditChatFolderInviteLink
import org.drinkless.tdlib.TdApi.EditForumTopic
import org.drinkless.tdlib.TdApi.EditMessageCaption
import org.drinkless.tdlib.TdApi.EditMessageText
import org.drinkless.tdlib.TdApi.EmojiChatTheme
import org.drinkless.tdlib.TdApi.EmojiReaction
import org.drinkless.tdlib.TdApi.FailedToAddMember
import org.drinkless.tdlib.TdApi.FailedToAddMembers
import org.drinkless.tdlib.TdApi.FileTypeAnimation
import org.drinkless.tdlib.TdApi.FileTypeAudio
import org.drinkless.tdlib.TdApi.FileTypeDocument
import org.drinkless.tdlib.TdApi.FileTypePhoto
import org.drinkless.tdlib.TdApi.FileTypeProfilePhoto
import org.drinkless.tdlib.TdApi.FileTypeSecret
import org.drinkless.tdlib.TdApi.FileTypeSecretThumbnail
import org.drinkless.tdlib.TdApi.FileTypeSecure
import org.drinkless.tdlib.TdApi.FileTypeSticker
import org.drinkless.tdlib.TdApi.FileTypeThumbnail
import org.drinkless.tdlib.TdApi.FileTypeUnknown
import org.drinkless.tdlib.TdApi.FileTypeVideo
import org.drinkless.tdlib.TdApi.FileTypeVideoNote
import org.drinkless.tdlib.TdApi.FileTypeVoiceNote
import org.drinkless.tdlib.TdApi.FileTypeWallpaper
import org.drinkless.tdlib.TdApi.FinishFileGeneration
import org.drinkless.tdlib.TdApi.FirebaseAuthenticationSettings
import org.drinkless.tdlib.TdApi.FirebaseAuthenticationSettingsAndroid
import org.drinkless.tdlib.TdApi.ForumTopic
import org.drinkless.tdlib.TdApi.ForumTopicIcon
import org.drinkless.tdlib.TdApi.ForumTopicInfo
import org.drinkless.tdlib.TdApi.ForumTopics
import org.drinkless.tdlib.TdApi.ForwardMessages
import org.drinkless.tdlib.TdApi.ForwardSource
import org.drinkless.tdlib.TdApi.FoundChatMessages
import org.drinkless.tdlib.TdApi.GetActiveSessions
import org.drinkless.tdlib.TdApi.GetAnimatedEmoji
import org.drinkless.tdlib.TdApi.GetApplicationConfig
import org.drinkless.tdlib.TdApi.GetBasicGroupFullInfo
import org.drinkless.tdlib.TdApi.GetChat
import org.drinkless.tdlib.TdApi.GetChatActiveStories
import org.drinkless.tdlib.TdApi.GetChatFolder
import org.drinkless.tdlib.TdApi.GetChatFolderDefaultIconName
import org.drinkless.tdlib.TdApi.GetChatHistory
import org.drinkless.tdlib.TdApi.GetChatMember
import org.drinkless.tdlib.TdApi.GetCountryCode
import org.drinkless.tdlib.TdApi.GetCustomEmojiReactionAnimations
import org.drinkless.tdlib.TdApi.GetForumTopic
import org.drinkless.tdlib.TdApi.GetForumTopicDefaultIcons
import org.drinkless.tdlib.TdApi.GetForumTopics
import org.drinkless.tdlib.TdApi.GetInternalLink
import org.drinkless.tdlib.TdApi.GetLanguagePackInfo
import org.drinkless.tdlib.TdApi.GetLanguagePackStrings
import org.drinkless.tdlib.TdApi.GetMessage
import org.drinkless.tdlib.TdApi.GetMessageLink
import org.drinkless.tdlib.TdApi.GetMessageLocally
import org.drinkless.tdlib.TdApi.GetMessageProperties
import org.drinkless.tdlib.TdApi.GetOption
import org.drinkless.tdlib.TdApi.GetProxies
import org.drinkless.tdlib.TdApi.GetRemoteFile
import org.drinkless.tdlib.TdApi.GetScopeNotificationSettings
import org.drinkless.tdlib.TdApi.GetSupergroupFullInfo
import org.drinkless.tdlib.TdApi.GetWebPageInstantView
import org.drinkless.tdlib.TdApi.GroupCallMessageLevel
import org.drinkless.tdlib.TdApi.InputFileGenerated
import org.drinkless.tdlib.TdApi.InputFileId
import org.drinkless.tdlib.TdApi.InputMessageAnimation
import org.drinkless.tdlib.TdApi.InputMessageAudio
import org.drinkless.tdlib.TdApi.InputMessageContent
import org.drinkless.tdlib.TdApi.InputMessageDice
import org.drinkless.tdlib.TdApi.InputMessageReplyTo
import org.drinkless.tdlib.TdApi.InputMessageSticker
import org.drinkless.tdlib.TdApi.InputMessageText
import org.drinkless.tdlib.TdApi.InternalLinkTypeProxy
import org.drinkless.tdlib.TdApi.LanguagePackInfo
import org.drinkless.tdlib.TdApi.LanguagePackString
import org.drinkless.tdlib.TdApi.LanguagePackStrings
import org.drinkless.tdlib.TdApi.LoadChats
import org.drinkless.tdlib.TdApi.LogOut
import org.drinkless.tdlib.TdApi.MessageAnimatedEmoji
import org.drinkless.tdlib.TdApi.MessageContent
import org.drinkless.tdlib.TdApi.MessageDocument
import org.drinkless.tdlib.TdApi.MessageLocation
import org.drinkless.tdlib.TdApi.MessageOriginChannel
import org.drinkless.tdlib.TdApi.MessageOriginChat
import org.drinkless.tdlib.TdApi.MessageOriginHiddenUser
import org.drinkless.tdlib.TdApi.MessageOriginUser
import org.drinkless.tdlib.TdApi.MessagePaidMedia
import org.drinkless.tdlib.TdApi.MessagePoll
import org.drinkless.tdlib.TdApi.MessageProperties
import org.drinkless.tdlib.TdApi.MessageSchedulingState
import org.drinkless.tdlib.TdApi.MessageSendOptions
import org.drinkless.tdlib.TdApi.MessageSender
import org.drinkless.tdlib.TdApi.MessageSenderChat
import org.drinkless.tdlib.TdApi.MessageSenderUser
import org.drinkless.tdlib.TdApi.MessageSendingStatePending
import org.drinkless.tdlib.TdApi.MessageSourceChatList
import org.drinkless.tdlib.TdApi.MessageSourceScreenshot
import org.drinkless.tdlib.TdApi.MessageText
import org.drinkless.tdlib.TdApi.MessageTopic
import org.drinkless.tdlib.TdApi.NotificationGroup
import org.drinkless.tdlib.TdApi.NotificationSettingsScope
import org.drinkless.tdlib.TdApi.OpenChat
import org.drinkless.tdlib.TdApi.OptimizeStorage
import org.drinkless.tdlib.TdApi.OptionValueBoolean
import org.drinkless.tdlib.TdApi.OptionValueInteger
import org.drinkless.tdlib.TdApi.OptionValueString
import org.drinkless.tdlib.TdApi.PaidReactionType
import org.drinkless.tdlib.TdApi.PhoneNumberAuthenticationSettings
import org.drinkless.tdlib.TdApi.PingProxy
import org.drinkless.tdlib.TdApi.PreliminaryUploadFile
import org.drinkless.tdlib.TdApi.ProcessChatFolderNewChats
import org.drinkless.tdlib.TdApi.ProcessPushNotification
import org.drinkless.tdlib.TdApi.ProfileAccentColor
import org.drinkless.tdlib.TdApi.ProfilePhoto
import org.drinkless.tdlib.TdApi.PushReceiverId
import org.drinkless.tdlib.TdApi.ReactionType
import org.drinkless.tdlib.TdApi.ReactionTypeCustomEmoji
import org.drinkless.tdlib.TdApi.ReactionTypeEmoji
import org.drinkless.tdlib.TdApi.ReadAllChatMentions
import org.drinkless.tdlib.TdApi.RegisterDevice
import org.drinkless.tdlib.TdApi.RemoveProxy
import org.drinkless.tdlib.TdApi.ReplacePrimaryChatInviteLink
import org.drinkless.tdlib.TdApi.ResendMessages
import org.drinkless.tdlib.TdApi.RestrictionInfo
import org.drinkless.tdlib.TdApi.ScopeNotificationSettings
import org.drinkless.tdlib.TdApi.SearchChatMessages
import org.drinkless.tdlib.TdApi.SearchContacts
import org.drinkless.tdlib.TdApi.SearchMessagesFilter
import org.drinkless.tdlib.TdApi.SearchPublicChat
import org.drinkless.tdlib.TdApi.SearchStickerSet
import org.drinkless.tdlib.TdApi.SecretChat
import org.drinkless.tdlib.TdApi.SendBotStartMessage
import org.drinkless.tdlib.TdApi.SendInlineQueryResultMessage
import org.drinkless.tdlib.TdApi.SendMessage
import org.drinkless.tdlib.TdApi.Sessions
import org.drinkless.tdlib.TdApi.SetAlarm
import org.drinkless.tdlib.TdApi.SetApplicationVerificationToken
import org.drinkless.tdlib.TdApi.SetChatClientData
import org.drinkless.tdlib.TdApi.SetChatMemberStatus
import org.drinkless.tdlib.TdApi.SetChatMemberTag
import org.drinkless.tdlib.TdApi.SetChatMessageAutoDeleteTime
import org.drinkless.tdlib.TdApi.SetChatNotificationSettings
import org.drinkless.tdlib.TdApi.SetChatPermissions
import org.drinkless.tdlib.TdApi.SetForumTopicNotificationSettings
import org.drinkless.tdlib.TdApi.SetInactiveSessionTtl
import org.drinkless.tdlib.TdApi.SetMessageSenderBlockList
import org.drinkless.tdlib.TdApi.SetNetworkType
import org.drinkless.tdlib.TdApi.SetOption
import org.drinkless.tdlib.TdApi.SetPinnedForumTopics
import org.drinkless.tdlib.TdApi.SetScopeNotificationSettings
import org.drinkless.tdlib.TdApi.SetTdlibParameters
import org.drinkless.tdlib.TdApi.Sticker
import org.drinkless.tdlib.TdApi.StickerSetInfo
import org.drinkless.tdlib.TdApi.Stickers
import org.drinkless.tdlib.TdApi.SuggestedAction
import org.drinkless.tdlib.TdApi.SuggestedActionConvertToBroadcastGroup
import org.drinkless.tdlib.TdApi.Supergroup
import org.drinkless.tdlib.TdApi.SupergroupFullInfo
import org.drinkless.tdlib.TdApi.SynchronizeLanguagePack
import org.drinkless.tdlib.TdApi.TerminateAllOtherSessions
import org.drinkless.tdlib.TdApi.TerminateSession
import org.drinkless.tdlib.TdApi.TextEntityTypeCustomEmoji
import org.drinkless.tdlib.TdApi.ToggleChatIsMarkedAsUnread
import org.drinkless.tdlib.TdApi.ToggleForumTopicIsClosed
import org.drinkless.tdlib.TdApi.ToggleForumTopicIsPinned
import org.drinkless.tdlib.TdApi.ToggleGeneralForumTopicIsHidden
import org.drinkless.tdlib.TdApi.TransferChatOwnership
import org.drinkless.tdlib.TdApi.UnreadReaction
import org.drinkless.tdlib.TdApi.UpdateAccentColors
import org.drinkless.tdlib.TdApi.UpdateActiveEmojiReactions
import org.drinkless.tdlib.TdApi.UpdateActiveLiveLocationMessages
import org.drinkless.tdlib.TdApi.UpdateActiveNotifications
import org.drinkless.tdlib.TdApi.UpdateAgeVerificationParameters
import org.drinkless.tdlib.TdApi.UpdateAnimatedEmojiMessageClicked
import org.drinkless.tdlib.TdApi.UpdateAnimationSearchParameters
import org.drinkless.tdlib.TdApi.UpdateApplicationRecaptchaVerificationRequired
import org.drinkless.tdlib.TdApi.UpdateApplicationVerificationRequired
import org.drinkless.tdlib.TdApi.UpdateAttachmentMenuBots
import org.drinkless.tdlib.TdApi.UpdateAuthorizationState
import org.drinkless.tdlib.TdApi.UpdateAutosaveSettings
import org.drinkless.tdlib.TdApi.UpdateAvailableMessageEffects
import org.drinkless.tdlib.TdApi.UpdateBasicGroup
import org.drinkless.tdlib.TdApi.UpdateBasicGroupFullInfo
import org.drinkless.tdlib.TdApi.UpdateCall
import org.drinkless.tdlib.TdApi.UpdateChatAccentColors
import org.drinkless.tdlib.TdApi.UpdateChatAction
import org.drinkless.tdlib.TdApi.UpdateChatActionBar
import org.drinkless.tdlib.TdApi.UpdateChatActiveStories
import org.drinkless.tdlib.TdApi.UpdateChatAddedToList
import org.drinkless.tdlib.TdApi.UpdateChatAvailableReactions
import org.drinkless.tdlib.TdApi.UpdateChatBackground
import org.drinkless.tdlib.TdApi.UpdateChatBlockList
import org.drinkless.tdlib.TdApi.UpdateChatBusinessBotManageBar
import org.drinkless.tdlib.TdApi.UpdateChatDefaultDisableNotification
import org.drinkless.tdlib.TdApi.UpdateChatDraftMessage
import org.drinkless.tdlib.TdApi.UpdateChatEmojiStatus
import org.drinkless.tdlib.TdApi.UpdateChatFolders
import org.drinkless.tdlib.TdApi.UpdateChatHasProtectedContent
import org.drinkless.tdlib.TdApi.UpdateChatHasScheduledMessages
import org.drinkless.tdlib.TdApi.UpdateChatIsMarkedAsUnread
import org.drinkless.tdlib.TdApi.UpdateChatIsTranslatable
import org.drinkless.tdlib.TdApi.UpdateChatLastMessage
import org.drinkless.tdlib.TdApi.UpdateChatMessageAutoDeleteTime
import org.drinkless.tdlib.TdApi.UpdateChatMessageSender
import org.drinkless.tdlib.TdApi.UpdateChatNotificationSettings
import org.drinkless.tdlib.TdApi.UpdateChatOnlineMemberCount
import org.drinkless.tdlib.TdApi.UpdateChatPendingJoinRequests
import org.drinkless.tdlib.TdApi.UpdateChatPermissions
import org.drinkless.tdlib.TdApi.UpdateChatPhoto
import org.drinkless.tdlib.TdApi.UpdateChatPosition
import org.drinkless.tdlib.TdApi.UpdateChatReadInbox
import org.drinkless.tdlib.TdApi.UpdateChatReadOutbox
import org.drinkless.tdlib.TdApi.UpdateChatRemovedFromList
import org.drinkless.tdlib.TdApi.UpdateChatReplyMarkup
import org.drinkless.tdlib.TdApi.UpdateChatRevenueAmount
import org.drinkless.tdlib.TdApi.UpdateChatTheme
import org.drinkless.tdlib.TdApi.UpdateChatTitle
import org.drinkless.tdlib.TdApi.UpdateChatUnreadMentionCount
import org.drinkless.tdlib.TdApi.UpdateChatUnreadPollVoteCount
import org.drinkless.tdlib.TdApi.UpdateChatUnreadReactionCount
import org.drinkless.tdlib.TdApi.UpdateChatVideoChat
import org.drinkless.tdlib.TdApi.UpdateChatViewAsTopics
import org.drinkless.tdlib.TdApi.UpdateConnectionState
import org.drinkless.tdlib.TdApi.UpdateContactCloseBirthdays
import org.drinkless.tdlib.TdApi.UpdateDefaultBackground
import org.drinkless.tdlib.TdApi.UpdateDefaultPaidReactionType
import org.drinkless.tdlib.TdApi.UpdateDefaultReactionType
import org.drinkless.tdlib.TdApi.UpdateDeleteMessages
import org.drinkless.tdlib.TdApi.UpdateDiceEmojis
import org.drinkless.tdlib.TdApi.UpdateDirectMessagesChatTopic
import org.drinkless.tdlib.TdApi.UpdateEmojiChatThemes
import org.drinkless.tdlib.TdApi.UpdateFavoriteStickers
import org.drinkless.tdlib.TdApi.UpdateFile
import org.drinkless.tdlib.TdApi.UpdateFileAddedToDownloads
import org.drinkless.tdlib.TdApi.UpdateFileDownload
import org.drinkless.tdlib.TdApi.UpdateFileDownloads
import org.drinkless.tdlib.TdApi.UpdateFileGenerationStart
import org.drinkless.tdlib.TdApi.UpdateFileGenerationStop
import org.drinkless.tdlib.TdApi.UpdateFileRemovedFromDownloads
import org.drinkless.tdlib.TdApi.UpdateForumTopic
import org.drinkless.tdlib.TdApi.UpdateForumTopicInfo
import org.drinkless.tdlib.TdApi.UpdateFreezeState
import org.drinkless.tdlib.TdApi.UpdateGroupCall
import org.drinkless.tdlib.TdApi.UpdateGroupCallMessageLevels
import org.drinkless.tdlib.TdApi.UpdateGroupCallMessageSendFailed
import org.drinkless.tdlib.TdApi.UpdateGroupCallMessagesDeleted
import org.drinkless.tdlib.TdApi.UpdateGroupCallParticipant
import org.drinkless.tdlib.TdApi.UpdateGroupCallParticipants
import org.drinkless.tdlib.TdApi.UpdateGroupCallVerificationState
import org.drinkless.tdlib.TdApi.UpdateHavePendingNotifications
import org.drinkless.tdlib.TdApi.UpdateInstalledStickerSets
import org.drinkless.tdlib.TdApi.UpdateLanguagePackStrings
import org.drinkless.tdlib.TdApi.UpdateMessageContainsUnreadPollVotes
import org.drinkless.tdlib.TdApi.UpdateMessageContent
import org.drinkless.tdlib.TdApi.UpdateMessageContentOpened
import org.drinkless.tdlib.TdApi.UpdateMessageEdited
import org.drinkless.tdlib.TdApi.UpdateMessageFactCheck
import org.drinkless.tdlib.TdApi.UpdateMessageInteractionInfo
import org.drinkless.tdlib.TdApi.UpdateMessageIsPinned
import org.drinkless.tdlib.TdApi.UpdateMessageLiveLocationViewed
import org.drinkless.tdlib.TdApi.UpdateMessageMentionRead
import org.drinkless.tdlib.TdApi.UpdateMessageSendAcknowledged
import org.drinkless.tdlib.TdApi.UpdateMessageSendFailed
import org.drinkless.tdlib.TdApi.UpdateMessageSendSucceeded
import org.drinkless.tdlib.TdApi.UpdateMessageSuggestedPostInfo
import org.drinkless.tdlib.TdApi.UpdateMessageUnreadReactions
import org.drinkless.tdlib.TdApi.UpdateNewCallSignalingData
import org.drinkless.tdlib.TdApi.UpdateNewChat
import org.drinkless.tdlib.TdApi.UpdateNewGroupCallMessage
import org.drinkless.tdlib.TdApi.UpdateNewGroupCallPaidReaction
import org.drinkless.tdlib.TdApi.UpdateNewMessage
import org.drinkless.tdlib.TdApi.UpdateNotification
import org.drinkless.tdlib.TdApi.UpdateNotificationGroup
import org.drinkless.tdlib.TdApi.UpdateOption
import org.drinkless.tdlib.TdApi.UpdateOwnedStarCount
import org.drinkless.tdlib.TdApi.UpdateOwnedTonCount
import org.drinkless.tdlib.TdApi.UpdateProfileAccentColors
import org.drinkless.tdlib.TdApi.UpdateQuickReplyShortcut
import org.drinkless.tdlib.TdApi.UpdateQuickReplyShortcutDeleted
import org.drinkless.tdlib.TdApi.UpdateQuickReplyShortcutMessages
import org.drinkless.tdlib.TdApi.UpdateQuickReplyShortcuts
import org.drinkless.tdlib.TdApi.UpdateReactionNotificationSettings
import org.drinkless.tdlib.TdApi.UpdateRecentStickers
import org.drinkless.tdlib.TdApi.UpdateSavedAnimations
import org.drinkless.tdlib.TdApi.UpdateSavedMessagesTags
import org.drinkless.tdlib.TdApi.UpdateSavedMessagesTopic
import org.drinkless.tdlib.TdApi.UpdateSavedMessagesTopicCount
import org.drinkless.tdlib.TdApi.UpdateSavedNotificationSounds
import org.drinkless.tdlib.TdApi.UpdateScopeNotificationSettings
import org.drinkless.tdlib.TdApi.UpdateSecretChat
import org.drinkless.tdlib.TdApi.UpdateServiceNotification
import org.drinkless.tdlib.TdApi.UpdateSpeechRecognitionTrial
import org.drinkless.tdlib.TdApi.UpdateSpeedLimitNotification
import org.drinkless.tdlib.TdApi.UpdateStarRevenueStatus
import org.drinkless.tdlib.TdApi.UpdateStickerSet
import org.drinkless.tdlib.TdApi.UpdateStory
import org.drinkless.tdlib.TdApi.UpdateStoryDeleted
import org.drinkless.tdlib.TdApi.UpdateStoryListChatCount
import org.drinkless.tdlib.TdApi.UpdateStoryPostFailed
import org.drinkless.tdlib.TdApi.UpdateStoryPostSucceeded
import org.drinkless.tdlib.TdApi.UpdateStoryStealthMode
import org.drinkless.tdlib.TdApi.UpdateSuggestedActions
import org.drinkless.tdlib.TdApi.UpdateSupergroup
import org.drinkless.tdlib.TdApi.UpdateSupergroupFullInfo
import org.drinkless.tdlib.TdApi.UpdateTermsOfService
import org.drinkless.tdlib.TdApi.UpdateTonRevenueStatus
import org.drinkless.tdlib.TdApi.UpdateTopicMessageCount
import org.drinkless.tdlib.TdApi.UpdateTrendingStickerSets
import org.drinkless.tdlib.TdApi.UpdateTrustedMiniAppBots
import org.drinkless.tdlib.TdApi.UpdateUnconfirmedSession
import org.drinkless.tdlib.TdApi.UpdateUnreadChatCount
import org.drinkless.tdlib.TdApi.UpdateUnreadMessageCount
import org.drinkless.tdlib.TdApi.UpdateUser
import org.drinkless.tdlib.TdApi.UpdateUserFullInfo
import org.drinkless.tdlib.TdApi.UpdateUserPrivacySettingRules
import org.drinkless.tdlib.TdApi.UpdateUserStatus
import org.drinkless.tdlib.TdApi.UpdateVideoPublished
import org.drinkless.tdlib.TdApi.UpdateWebAppMessageSent
import org.drinkless.tdlib.TdApi.UpgradeBasicGroupChatToSupergroupChat
import org.drinkless.tdlib.TdApi.UserFullInfo
import org.drinkless.tdlib.TdApi.UserTypeBot
import org.drinkless.tdlib.TdApi.Usernames
import org.drinkless.tdlib.TdApi.Users
import org.drinkless.tdlib.TdApi.ViewMessages
import org.drinkless.tdlib.TdApi.WebPageInstantView
import org.drinkmore.Tracer.onOtherError
import org.drinkmore.Tracer.onTdlibFatalError
import org.drinkmore.Tracer.onTdlibHandlerError
import org.thunderdog.challegram.BuildConfig
import org.thunderdog.challegram.Log
import org.thunderdog.challegram.R
import org.thunderdog.challegram.TDLib
import org.thunderdog.challegram.U
import org.thunderdog.challegram.component.attach.MediaToReplacePickerManager.LocalPickedFile
import org.thunderdog.challegram.component.chat.TdlibSingleUnreadReactionsManager
import org.thunderdog.challegram.component.dialogs.ChatView
import org.thunderdog.challegram.config.Config
import org.thunderdog.challegram.core.Lang
import org.thunderdog.challegram.data.AvatarPlaceholder
import org.thunderdog.challegram.data.ContentPreview
import org.thunderdog.challegram.data.TD
import org.thunderdog.challegram.data.TGMessage
import org.thunderdog.challegram.data.TGReaction
import org.thunderdog.challegram.emoji.Emoji
import org.thunderdog.challegram.emoji.EmojiCodes
import org.thunderdog.challegram.filegen.TdlibFileGenerationManager
import org.thunderdog.challegram.loader.ImageFile
import org.thunderdog.challegram.loader.ImageLoader
import org.thunderdog.challegram.loader.gif.GifBridge
import org.thunderdog.challegram.navigation.ViewController
import org.thunderdog.challegram.sync.SyncHelper
import org.thunderdog.challegram.telegram.TdlibNotificationChannelGroup.ChannelCreationFailureException
import org.thunderdog.challegram.theme.ColorId
import org.thunderdog.challegram.theme.Theme
import org.thunderdog.challegram.tool.Strings
import org.thunderdog.challegram.tool.UI
import org.thunderdog.challegram.ui.EditRightsController
import org.thunderdog.challegram.unsorted.Passcode
import org.thunderdog.challegram.unsorted.Settings
import org.thunderdog.challegram.unsorted.Settings.CloudSetting
import org.thunderdog.challegram.unsorted.Settings.Companion.instance
import org.thunderdog.challegram.unsorted.Settings.EmojiPack
import org.thunderdog.challegram.unsorted.Settings.IconPack
import org.thunderdog.challegram.unsorted.Settings.SettingsChangeListener
import org.thunderdog.challegram.util.ChangeLogList
import org.thunderdog.challegram.util.DrawableProvider
import org.thunderdog.challegram.util.UserProvider
import org.thunderdog.challegram.util.WrapperProvider
import org.thunderdog.challegram.util.text.Letters
import org.thunderdog.challegram.voip.VoIPLogs
import org.thunderdog.challegram.voip.annotation.CallState
import tgx.app.RecaptchaProviderRegistry
import tgx.td.MessageId.Companion.toServerMessageId
import tgx.td.assertAuthorizationState_ba756b5f
import tgx.td.assertChatList_db6c93ab
import tgx.td.assertChatType_e562ec7d
import tgx.td.assertConnectionState_963d6b5f
import tgx.td.assertDeviceToken_de4a4f61
import tgx.td.assertInputMessageContent_eb9f33ef
import tgx.td.assertMessageContent_baa076bf
import tgx.td.assertMessageOrigin_f2224a59
import tgx.td.assertMessageSender_439d4c9c
import tgx.td.assertPaidMedia_a2956719
import tgx.td.assertStickerType_cc811bb7
import tgx.td.assertSuggestedAction_a78df4c9
import tgx.td.assertUpdate_ad02591
import tgx.td.client.RtcServer
import tgx.td.client.TdlibOptions
import tgx.td.copyOf
import tgx.td.copyTo
import tgx.td.customEmojiId
import tgx.td.data.MessageWithProperties
import tgx.td.equalsTo
import tgx.td.findPosition
import tgx.td.fromBasicGroupId
import tgx.td.fromSupergroupId
import tgx.td.CHAT_LIST_MAIN
import tgx.td.CHAT_LIST_ARCHIVE
import tgx.td.TELEGRAM_ACCOUNT_ID
import tgx.td.MAX_MESSAGE_GROUP_SIZE
import tgx.td.TELEGRAM_BOT_FATHER_ACCOUNT_ID
import tgx.td.TELEGRAM_BOT_FATHER_USERNAME
import tgx.td.TME_HOSTS
import tgx.td.TELEGRAM_HOSTS
import tgx.td.TELEGRAPH_HOSTS
import tgx.td.fromUserId
import tgx.td.getMessageAuthorId
import tgx.td.getSenderId
import tgx.td.getSenderUserId
import tgx.td.getType
import tgx.td.hasMessageSource
import tgx.td.hasUsername
import tgx.td.indexOf
import tgx.td.isAnonymous
import tgx.td.isBasicGroup
import tgx.td.isCustomEmoji
import tgx.td.isDocument
import tgx.td.isEmpty
import tgx.td.isFake
import tgx.td.isMonoforumChat
import tgx.td.isPinned
import tgx.td.isPrivate
import tgx.td.isScam
import tgx.td.isSecret
import tgx.td.isUserChat
import tgx.td.isVerified
import tgx.td.longValue
import tgx.td.parse
import tgx.td.primaryUsername
import tgx.td.sort
import tgx.td.stringValue
import tgx.td.stringify
import tgx.td.textOrCaption
import tgx.td.toBasicGroupId
import tgx.td.toObject
import tgx.td.toSecretChatId
import tgx.td.toSupergroupId
import tgx.td.toUserId
import tgx.td.unsupported
import java.io.File
import java.util.ArrayDeque
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import java.util.Queue
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern
import kotlin.math.max

class Tdlib internal constructor(account: TdlibAccount, @field:Mode @param:Mode private var instanceMode: Int) : TdlibProvider, SettingsChangeListener,
    DateChangeListener {
    override fun accountId(): Int {
        return id()
    }

    override fun tdlib(): Tdlib {
        return this
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        Mode.Companion.NORMAL, Mode.Companion.DEBUG, Mode.Companion.SERVICE
    )
    annotation class Mode {
        companion object {
            const val NORMAL: Int = 0
            const val DEBUG: Int = 1
            const val SERVICE: Int = 2
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        Status.Companion.UNKNOWN, Status.Companion.UNAUTHORIZED, Status.Companion.READY
    )
    annotation class Status {
        companion object {
            const val UNKNOWN: Int = 0
            const val UNAUTHORIZED: Int = 1
            const val READY: Int = 2
        }
    }

    private val handlerLock = Any()
    private var _handler: TdlibUi? = null

    private val parameters: SetTdlibParameters
    private val configHandler = Client.ResultHandler { `object`: TdApi.Object? ->
        if (`object` is TdApi.JsonValue) {
            val json = `object`
            setApplicationConfig(json, stringify(json))
        } else {
            Log.i("getApplicationConfig failed: %s", TD.toErrorString(`object`))
        }
    }
    private val messageHandler = Client.ResultHandler { `object`: TdApi.Object? ->
        when (`object`!!.getConstructor()) {
            TdApi.Ok.CONSTRUCTOR -> {}
            TdApi.Message.CONSTRUCTOR -> updateNewMessage(UpdateNewMessage(`object` as TdApi.Message), false)
            TdApi.Messages.CONSTRUCTOR ->         // FIXME send as a single update
                for (message in (`object` as TdApi.Messages).messages) {
                    if (message != null) {
                        updateNewMessage(UpdateNewMessage(message), false)
                    }
                }

            TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
        }
    }
    private val doneHandler = Client.ResultHandler { `object`: TdApi.Object? ->
        when (`object`!!.getConstructor()) {
            TdApi.Ok.CONSTRUCTOR -> UI.showToast(R.string.Done, Toast.LENGTH_SHORT)
            TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
        }
    }
    private val silentHandler = Client.ResultHandler { `object`: TdApi.Object? ->
        if (`object`!!.getConstructor() == TdApi.Error.CONSTRUCTOR) {
            Log.e("TDLib Error (silenced): %s", TD.toErrorString(`object`))
        }
    }
    private val imageLoadHandler = Tdlib.ResultHandler { file: TdApi.File?, error: TdApi.Error? ->
        if (error != null) {
            Log.e(Log.TAG_IMAGE_LOADER, "DownloadFile failed: %s", TD.toErrorString(error))
        } else {
            if (file!!.local.isDownloadingCompleted) {
                ImageLoader.instance().onLoad(this@Tdlib, file)
            } else if (!file.local.isDownloadingActive) {
                Log.e(Log.TAG_IMAGE_LOADER, "WARNING: Image load not started")
            }
        }
    }
    private val profilePhotoHandler = Client.ResultHandler { `object`: TdApi.Object? ->
        if (`object`!!.getConstructor() == TdApi.Error.CONSTRUCTOR) {
            Log.e("setProfilePhoto failed: %s", TD.toErrorString(`object`))
            UI.showToast(Lang.getString(R.string.SetProfilePhotoError, TD.toErrorString(`object`)), Toast.LENGTH_SHORT)
        }
    }

    private class ClientHolder(tdlib: Tdlib) : Client.ResultHandler, Client.ExceptionHandler {
        private val tdlib: Tdlib
        val client: Client

        val resources: TdlibResourceManager
        val updates: TdlibResourceManager
        private var running = true

        private var initializationTime: Long = 0
        private var timeWasted: Long = 0

        private var logged = false

        fun hasLogged(): Boolean {
            val logged = this.logged
            this.logged = true
            return logged
        }

        val closeLatch: CountDownLatch = CountDownLatch(1)
        var closeState: Int = 0

        init {
            Log.i(Log.TAG_ACCOUNTS, "Creating client #%d", runningClients.incrementAndGet())
            this.tdlib = tdlib
            this.client = Client.create(this, this, this)
            tdlib.updateParameters(client)
            if (Config.NEED_ONLINE) {
                if (tdlib.isOnline) {
                    client.send(SetOption("online", OptionValueBoolean(true)), tdlib.okHandler())
                }
            }
            tdlib.context.modifyClient(tdlib, client)
            this.resources = TdlibResourceManager(tdlib, BuildConfig.TELEGRAM_RESOURCES_CHANNEL)
            this.updates = TdlibResourceManager(tdlib, BuildConfig.TELEGRAM_UPDATES_CHANNEL)
            if (!tdlib.inRecoveryMode()) {
                init()
            }
        }

        @JvmOverloads
        fun runOnTdlibThread(after: Runnable, timeoutSeconds: Double = 0.0) {
            runOnTdlibThread(after, timeoutSeconds, null)
        }

        fun runOnTdlibThread(after: Runnable, timeout: Double, cancellationSignal: CancellationSignal?) {
            client.send(SetAlarm(timeout), Client.ResultHandler { ignored: TdApi.Object? ->
                if (cancellationSignal == null || !cancellationSignal.isCanceled()) {
                    after.run()
                }
            })
        }

        fun init() {
            if (initializationTime != 0L) return
            initializationTime = SystemClock.uptimeMillis()
            val time = SystemClock.uptimeMillis()
            val stackTrace = RuntimeException().getStackTrace()
            val openTimeoutSignal = CancellationSignal()
            client.send(tdlib.parameters, Client.ResultHandler { result: TdApi.Object? ->
                val elapsed = SystemClock.uptimeMillis() - time
                TDLib.Tag.td_init(
                    "SetTdlibParameters response in %dms, accountId:%d, ok:%b",
                    elapsed,
                    tdlib.accountId,
                    result!!.getConstructor() == TdApi.Ok.CONSTRUCTOR
                )
                if (result.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                    onTdlibFatalError(tdlib, SetTdlibParameters::class.java, result as TdApi.Error, stackTrace)
                }
                openTimeoutSignal.cancel()
                tdlib.isOptimizing = false
            })
            runOnTdlibThread(
                Runnable { tdlib.isOptimizing = true },
                1.0, openTimeoutSignal
            )
            val startup: TdApi.Function<*>
            if (tdlib.accountId == 0 && instance().needProxyLegacyMigrateCheck()) {
                startup = GetProxies()
            } else {
                startup = SetAlarm(0.0)
            }
            client.send(startup, Client.ResultHandler { result: TdApi.Object? ->
                if (result!!.getConstructor() == AddedProxies.CONSTRUCTOR) {
                    val proxies = (result as AddedProxies).proxies
                    var foundEnabledProxy = false
                    for (addedProxy in proxies) {
                        val proxyId = instance().addOrUpdateProxy(addedProxy.proxy, null, addedProxy.isEnabled)
                        if (addedProxy.isEnabled) {
                            tdlib.setEffectiveProxyId(proxyId)
                            tdlib.setProxy(proxyId, addedProxy.proxy)
                            foundEnabledProxy = true
                        }
                    }
                    if (!foundEnabledProxy) {
                        tdlib.setEffectiveProxyId(Settings.PROXY_ID_NONE)
                    }
                } else {
                    val proxyId = instance().effectiveProxyId
                    val proxy = instance().getProxyConfig(proxyId)
                    if (proxy != null) {
                        tdlib.setProxy(proxyId, proxy.proxy)
                    } else {
                        tdlib.disableProxy()
                    }
                }
            })
            client.send(GetApplicationConfig(), tdlib.configHandler)
        }

        fun sendFakeUpdate(update: TdApi.Update) {
            runOnTdlibThread(Runnable { tdlib.processUpdate(this, update) })
        }

        override fun onException(e: Throwable) {
            onTdlibHandlerError(e)
        }

        fun stop() {
            running = false
        }

        override fun onResult(`object`: TdApi.Object) {
            if (running) {
                var ms = SystemClock.uptimeMillis()
                if (`object` is TdApi.Update) {
                    tdlib.processUpdate(this, `object`)
                } else {
                    Log.e("Invalid update type: %s", `object`)
                }
                if (Log.needMeasureLaunchSpeed()) {
                    ms = SystemClock.uptimeMillis() - ms
                    if (ms > 100) {
                        Log.e("%s took %dms", `object`.toString(), ms)
                    }
                    timeWasted += ms
                }
            } else {
                Log.w("Ignored update: %s", `object`)
            }
        }

        fun sendClose() {
            client.send(TdApi.Close(), tdlib.okHandler())
        }

        fun close() {
            Log.i(Log.TAG_ACCOUNTS, "Calling client.close(), accountId:%d", tdlib.accountId)
            val ms = SystemClock.uptimeMillis()
            // Nothing to do anymore?
            // client.close();
            Log.i(
                Log.TAG_ACCOUNTS,
                "client.close() done in %dms, accountId:%d, accountsNum:%d",
                SystemClock.uptimeMillis() - ms,
                tdlib.accountId,
                runningClients.decrementAndGet()
            )
        }

        fun timeSinceInitializationMs(): Long {
            return if (initializationTime != 0L) SystemClock.uptimeMillis() - initializationTime else -1
        }

        fun timeWasted(): Long {
            return timeWasted
        }

        companion object {
            private val runningClients = AtomicInteger()
        }
    }

    private val userComparator: java.util.Comparator<TdApi.User> = UserComparator(this)
    private val userProviderComparator: java.util.Comparator<UserProvider> = UserProviderComparator(userComparator)

    // Context
    private val context: TdlibManager
    private val accountId: Int

    private var client: ClientHolder? = null

    @ConnectionState
    private var connectionState = ConnectionState.UNKNOWN

    private val clientLock = Any()
    private val dataLock = Any()
    private val chats = HashMap<Long?, Chat>()
    private val activeStories = HashMap<Long?, ChatActiveStories?>()
    private val storyListChatCount = SparseIntArray()
    private val storyLists = SparseArrayCompat<StoryList?>()
    private val forumTopicInfos = HashMap<String?, ForumTopicInfo?>()
    private val chatLists = HashMap<String?, TdlibChatList>()
    private val animatedTgxEmoji = StickerSet(AnimatedEmojiListener.TYPE_TGX, "AnimatedTgxEmojies", false)
    private val animatedDiceExplicit = StickerSet(AnimatedEmojiListener.TYPE_DICE, "BetterDice", true)
    private val knownChatIds = HashSet<Long?>()
    private val chatOnlineMemberCount = HashMap<Long?, Int?>()
    private val cache: TdlibCache
    private val emoji: TdlibEmojiManager
    private val reactions: TdlibEmojiReactionsManager
    private val outline: TdlibOutlineManager
    private val genericReactionEffects: TdlibSingleton<Stickers?>
    private val listeners: TdlibListeners
    private val filesManager: TdlibFilesManager
    private val statusManager: TdlibStatusManager
    private val contactManager: TdlibContactManager
    private val quickAckManager: TdlibQuickAckManager
    private val settingsManager: TdlibSettingsManager
    private val wallpaperManager: TdlibWallpaperManager
    private val notificationManager: TdlibNotificationManager
    private val fileGenerationManager: TdlibFileGenerationManager
    private val unreadReactionsManager: TdlibSingleUnreadReactionsManager
    private val editMediaManager: TdlibEditMediaManager
    private val messageViewer: TdlibMessageViewer

    private val channels = HashSet<Long?>()
    private val accessibleChatTimers = LongSparseLongArray()

    private var options = TdlibOptions()
    private var diceEmoji: Array<String?>? = null
    private var activeEmojiReactions: MutableSet<String>? = null
    private var defaultReactionType: ReactionType? = null
    private var defaultPaidReactionType: PaidReactionType? = null
    private val cachedReactions: MutableMap<String?, TGReaction?> = HashMap<String?, TGReaction?>()

    private var storyStealthModeActiveUntilDate = 0
    private var storyStealthModeCooldownUntilDate = 0

    private var languagePackId: String? = null
    private var suggestedLanguagePackId: String? = null
    private var suggestedLanguagePackInfo: LanguagePackInfo? = null

    private var connectionLossTime = SystemClock.uptimeMillis()

    private val counters: MutableMap<String?, TdlibCounter?> = HashMap<String?, TdlibCounter?>()
    private val tempCounter = TdlibBadgeCounter()
    private val unreadCounter = TdlibBadgeCounter()

    fun getCounter(chatList: ChatList): TdlibCounter {
        val key = TD.makeChatListKey(chatList)
        var counter = counters.get(key)
        if (counter == null) {
            counter = TdlibCounter(-1, -1, -1, -1, -1, -1, -1)
            counters.put(key, counter)
        }
        return counter
    }

    val installedStickerSetLimit: Int = 200
        get() {
            synchronized(dataLock) {
                return field
            }
        }

    private var favoriteStickerIds: IntArray? = null
    private var unreadTrendingStickerSetsCount = 0
    private var trustedMiniAppBotUserIds: LongArray? = null
    private var groupCallMessageLevels: Array<GroupCallMessageLevel?>? = null

    private var instancePaused = false
    private val referenceCount = AtomicInteger(0)

    override fun onSettingsChanged(newSettings: Long, oldSettings: Long) {
        if (hasFlag(newSettings, Settings.SETTING_FLAG_EXPLICIT_DICE) != hasFlag(oldSettings, Settings.SETTING_FLAG_EXPLICIT_DICE) && !this.isDebugInstance) {
            if (hasFlag(newSettings, Settings.SETTING_FLAG_EXPLICIT_DICE) && !animatedDiceExplicit.isLoaded) {
                animatedDiceExplicit.load(this)
            } else {
                listeners().notifyAnimatedEmojiListeners(AnimatedEmojiListener.TYPE_DICE)
            }
        }
    }

    // Device token
    fun checkDeviceToken() {
        runOnTdlibThread(Runnable { checkDeviceTokenImpl(null) })
    }

    @Synchronized
    fun checkDeviceTokenImpl(onDone: Runnable?) {
        val deviceToken = context.getToken()
        if (deviceToken == null || authorizationStatus() != Status.Companion.READY) return
        val myUserId = myUserId()
        if (myUserId == 0L) return
        val availableUserIds = context.availableUserIds(instanceMode)
        val otherUserIds = availableUserIds.removeElement(Arrays.binarySearch(availableUserIds, myUserId))
        if (TdlibSettingsManager.checkRegisteredDeviceToken(id(), myUserId, deviceToken, otherUserIds, false)) {
            TDLib.Tag.notifications("Device token already registered. accountId:%d", accountId)
            context.setDeviceRegistered(accountId, true)
            U.run(onDone)
            return
        }
        TDLib.Tag.notifications("Registering device token... accountId:%d", accountId)
        context.setDeviceRegistered(accountId, false)
        incrementReferenceCount(REFERENCE_TYPE_JOB)
        send<PushReceiverId?>(RegisterDevice(deviceToken, otherUserIds), Tdlib.ResultHandler { pushReceiverId: PushReceiverId?, error: TdApi.Error? ->
            if (pushReceiverId != null) {
                TDLib.Tag.notifications(
                    "Successfully registered device token:%s, accountId:%d, otherUserIdsCount:%d",
                    deviceToken,
                    accountId,
                    otherUserIds.size
                )
                instance().putNotificationReceiverId(pushReceiverId.id, accountId)
                TdlibSettingsManager.setRegisteredDevice(accountId, myUserId, deviceToken, otherUserIds)
                context().setDeviceRegistered(accountId, true)
                context().unregisterDevices(instanceMode, accountId, availableUserIds)
                U.run(onDone)
            } else {
                val seconds = max(5, TD.getFloodErrorSeconds(error!!.code, error.message, 5))
                if (seconds > 60 && this.isDebugInstance) {
                    TDLib.Tag.notifications(
                        "Unable to register device token, flood is %d seconds, ignoring: %s, accountId:%d",
                        seconds,
                        TD.toErrorString(error),
                        accountId
                    )
                    context.setDeviceRegistered(accountId, true)
                    U.run(onDone)
                } else {
                    TDLib.Tag.notifications(
                        "Unable to register device token, retrying in %d seconds: %s, accountId:%d",
                        seconds,
                        TD.toErrorString(error),
                        accountId
                    )
                    client().send(SetAlarm(seconds.toDouble()), Client.ResultHandler { ignored: TdApi.Object? -> checkDeviceTokenImpl(onDone) })
                }
            }
            decrementReferenceCount(REFERENCE_TYPE_JOB)
        })
    }

    fun changeLocationReferenceCount(deltaCount: Int) {
        var deltaCount = deltaCount
        if (deltaCount > 0) {
            do {
                incrementReferenceCount(REFERENCE_TYPE_LOCATION)
            } while (--deltaCount > 0)
        } else if (deltaCount < 0) {
            do {
                decrementReferenceCount(REFERENCE_TYPE_LOCATION)
            } while (++deltaCount < 0)
        }
    }

    fun incrementCallReferenceCount() {
        incrementReferenceCount(REFERENCE_TYPE_CALL)
    }

    fun decrementCallReferenceCount() {
        decrementReferenceCount(REFERENCE_TYPE_CALL)
    }

    fun incrementUiReferenceCount() {
        incrementReferenceCount(REFERENCE_TYPE_UI)
    }

    fun decrementUiReferenceCount() {
        decrementReferenceCount(REFERENCE_TYPE_UI)
    }

    fun incrementNotificationReferenceCount() {
        incrementReferenceCount(REFERENCE_TYPE_NOTIFICATION)
    }

    fun decrementNotificationReferenceCount() {
        decrementReferenceCount(REFERENCE_TYPE_NOTIFICATION)
    }

    fun incrementJobReferenceCount() {
        incrementReferenceCount(REFERENCE_TYPE_JOB)
    }

    fun decrementJobReferenceCount() {
        decrementReferenceCount(REFERENCE_TYPE_JOB)
    }

    private fun incrementReferenceCount(type: Int) {
        val wakeup: Boolean
        val referenceCount: Int
        synchronized(clientLock) {
            referenceCount = this.referenceCount.incrementAndGet()
            wakeup = referenceCount == 1
            Log.v(Log.TAG_ACCOUNTS, "accountId:%d, referenceCount:%d, type:%d", accountId, referenceCount, type)
            if (type == REFERENCE_TYPE_UI) account()!!.markAsUsed()
            schedulePause()
        }
        if (wakeup) {
            wakeUp()
            if (type == REFERENCE_TYPE_UI) context.checkPauseTimeouts(account())
        }
        if (wakeup) {
            noReferenceListeners.notifyConditionChanged()
        }
    }

    private fun decrementReferenceCount(type: Int) {
        val referenceCount: Int
        synchronized(clientLock) {
            referenceCount = this.referenceCount.decrementAndGet()
            if (referenceCount < 0) {
                val e: RuntimeException = IllegalStateException("type == " + type)
                onOtherError(e)
                throw e
            }
            Log.v(Log.TAG_ACCOUNTS, "accountId:%d, referenceCount:%d, type:%d", accountId, referenceCount, type)
            schedulePause()
        }
        if (referenceCount == 0) {
            noReferenceListeners.notifyConditionChanged()
        }
    }

    private var restartTimeout: Long = 0
    private var restartScheduledTime: Long = 0

    // keep_alive
    private var keepAlive = false
    private var hasUnprocessedPushes: Boolean
    private var isLoggingOut: Boolean
    private var ignoreNotificationUpdates = false

    private fun setHasUnprocessedPushes(hasUnprocessedPushes: Boolean) {
        if (this.hasUnprocessedPushes != hasUnprocessedPushes) {
            if (hasUnprocessedPushes && this.isConnected) {
                return
            }
            this.hasUnprocessedPushes = hasUnprocessedPushes
            context().setHasUnprocessedPushes(accountId, hasUnprocessedPushes)
        }
    }

    private fun checkKeepAlive(): Boolean {
        val keepAlive = havePendingNotifications || hasUnprocessedPushes
        if (this.keepAlive != keepAlive) {
            this.keepAlive = keepAlive
            context().setKeepAlive(accountId, keepAlive)
        }
        return keepAlive
    }

    // logging_out, logged_out
    @TdlibThread
    private fun setLoggingOut(isLoggingOut: Boolean) {
        if (this.isLoggingOut != isLoggingOut) {
            this.isLoggingOut = isLoggingOut
            if (isLoggingOut) {
                ignoreNotificationUpdates = true
            }
            context().setLoggingOut(accountId, isLoggingOut)
            checkPauseTimeout()
            if (isLoggingOut) {
                notifications().onDropNotificationData(true)
            }
        }
    }

    fun deleteAllFiles(after: RunnableBool?) {
        Log.i("Clearing data... accountId:%d", accountId)
        val ms = SystemClock.uptimeMillis()
        client().send(
            OptimizeStorage(
                0, 0, 0, 0, arrayOf<TdApi.FileType>( // new TdApi.FileTypeNone(),
                    FileTypeAnimation(),
                    FileTypeAudio(),
                    FileTypeDocument(),
                    FileTypePhoto(),
                    FileTypeProfilePhoto(),
                    FileTypeSecret(),
                    FileTypeSecretThumbnail(),
                    FileTypeSecure(),
                    FileTypeSticker(),
                    FileTypeThumbnail(),
                    FileTypeUnknown(),
                    FileTypeVideo(),
                    FileTypeVideoNote(),
                    FileTypeVoiceNote(),
                    FileTypeWallpaper()
                ), null, null, false, 0
            ), Client.ResultHandler { result: TdApi.Object? ->
                when (result!!.getConstructor()) {
                    TdApi.StorageStatistics.CONSTRUCTOR -> Log.i("Cleared files in %dms, accountId:%d", SystemClock.uptimeMillis() - ms, accountId)
                    TdApi.Error.CONSTRUCTOR -> Log.e("Unable to delete files data:%s, accountId:%d", TD.toErrorString(result), accountId)
                }
                if (after != null) {
                    after.runWithBool(result.getConstructor() == TdApi.StorageStatistics.CONSTRUCTOR)
                }
            })
    }

    fun closeAllSecretChats(after: Runnable?) {
        val remaining = AtomicInteger(2)
        val closingCount = AtomicInteger(0)
        val perChatCallback = RunnableData { chat: Chat? ->
            val secretChat = chatToSecretChat(chat!!.id)
            if (secretChat != null && secretChat.state.getConstructor() != TdApi.SecretChatStateClosed.CONSTRUCTOR) {
                closingCount.incrementAndGet()
                client().send(CloseSecretChat(secretChat.id), Client.ResultHandler { closeResult: TdApi.Object? ->
                    if (closingCount.decrementAndGet() == 0 && remaining.get() == 0) {
                        U.run(after)
                    }
                })
            }
        }
        val onEndCallback = RunnableBool { isFinal: Boolean ->
            if (isFinal && remaining.decrementAndGet() == 0) {
                if (closingCount.get() == 0) {
                    U.run(after)
                }
            }
        }
        chatList(CHAT_LIST_MAIN).loadAll(perChatCallback, onEndCallback)
        chatList(CHAT_LIST_ARCHIVE).loadAll(perChatCallback, onEndCallback)
    }

    fun cleanupUnauthorizedData(after: Runnable?) {
        awaitInitialization(Runnable {
            if (!account()!!.isUnauthorized() || account()!!.isLoggingOut()) {
                if (after != null) after.run()
            } else {
                incrementReferenceCount(REFERENCE_TYPE_JOB)
                deleteAllFiles(RunnableBool { success: Boolean ->
                    if (success) {
                        context().markNoPrivateData(accountId)
                    }
                    decrementReferenceCount(REFERENCE_TYPE_JOB)
                })
            }
        })
    }

    // restart
    fun pause(onPause: Runnable) {
        if (!shouldPause()) {
            U.run(onPause)
            return
        }
        runOnUiThread(Runnable {
            if (this.isPaused) {
                U.run(onPause)
            } else {
                awaitClose(onPause, true)
                doPause()
            }
        })
    }

    private fun shouldPause(): Boolean {
        if (instancePaused) return false // Instance already paused

        if (authorizationStatus() == Status.Companion.UNKNOWN) return false // TDLib takes too long to launch. Give it a chance to initialize

        if ((authorizationState != null && authorizationState!!.getConstructor() != TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR)) return false // User has started authorization process. Give them a chance to complete it.

        /*if (context().hasUi()) {
        // TODO limit couple most recent used accounts?
      }*/
        return !checkKeepAlive() && !account()!!.keepAlive() && account()!!.isDeviceRegistered() && getReferenceCount() == 0
    }

    fun checkPauseTimeout() {
        synchronized(clientLock) {
            schedulePause()
        }
    }

    private val pauseTimeout: Long
        get() {
            if (Config.TEST_TDLIB_RESTARTS || instance()
                    .forceTdlibRestart()
            ) return TimeUnit.SECONDS.toMillis(1)
            if (!context().hasUi()) return TimeUnit.SECONDS.toMillis(5) // No UI (running in the background), no limits

            val num = context.getActiveAccountsNum()
            if (num == 1) return TimeUnit.MINUTES.toMillis(15) // User has only one account

            val index = context.getAccountUsageIndex(accountId, TimeUnit.MINUTES.toMillis(15))
            if (index == -1) return TimeUnit.SECONDS.toMillis(5) // Instance was not used within last 15 minutes

            if (index < 3) return TimeUnit.MINUTES.toMillis(15) // Account is within 3 last used accounts (including current)

            if (index < 5) return TimeUnit.MINUTES.toMillis(7) // Account is within 5 last used accounts

            if (index < 10) return TimeUnit.MINUTES.toMillis(2) // Account is within 10 last used accounts

            // User has used more than 10 accounts, killing the oldest ones within 5 seconds
            return TimeUnit.SECONDS.toMillis(5) //  seconds
        }

    private fun schedulePause() {
        if (shouldPause()) {
            var timeout = this.pauseTimeout
            if (restartScheduledTime == 0L) {
                Log.i(Log.TAG_ACCOUNTS, "Scheduling TDLib restart, accountId:%d, timeout:%d", accountId, timeout)
                restartScheduledTime = SystemClock.uptimeMillis()
                restartTimeout = timeout
            } else if (restartTimeout != timeout) {
                val oldTimeout = restartTimeout
                restartTimeout = timeout
                val newTimeout = timeout + restartScheduledTime - SystemClock.uptimeMillis()
                Log.i(Log.TAG_ACCOUNTS, "Rescheduling TDLib restart, accountId:%d, timeout:%d (%d->%d)", accountId, newTimeout, oldTimeout, timeout)
                ui()!!.removeMessages(MSG_ACTION_PAUSE)
                timeout = newTimeout
            } else {
                return
            }
            if (timeout > 0) {
                ui()!!.sendMessageDelayed(ui()!!.obtainMessage(MSG_ACTION_PAUSE), timeout)
            } else {
                doPauseImpl()
            }
        } else if (restartScheduledTime != 0L) {
            restartScheduledTime = 0
            Log.i(
                Log.TAG_ACCOUNTS,
                "Canceling TDLib restart, accountId:%d, referenceCount:%d, keepAlive:%b",
                accountId,
                getReferenceCount(),
                account()!!.keepAlive()
            )
            ui()!!.removeMessages(MSG_ACTION_PAUSE)
        }
    }

    private fun doPauseImpl() {
        if (restartScheduledTime == 0L) return
        if (client == null || instancePaused) {
            Log.e(Log.TAG_ACCOUNTS, "Cannot pause TDLib instance, because it is already paused. accountId:%d", accountId)
            return
        }
        restartScheduledTime = 0
        if (!shouldPause()) {
            Log.i(Log.TAG_ACCOUNTS, "Cannot restart TDLib, because it is in use. referenceCount:%d, accountId:%d", getReferenceCount(), accountId)
            return
        }
        Log.i(Log.TAG_ACCOUNTS, "Pausing TDLib instance, because it is unused, accountId:%d", accountId)
        instancePaused = true
        client!!.sendClose()
    }

    private fun doPause() {
        synchronized(clientLock) {
            doPauseImpl()
        }
    }

    // Stress-test
    private var stressTest = 0

    fun stressTest(count: Int) {
        if (count <= 0) return
        this.stressTest += count
        clientHolder().sendClose()
    }

    // Database erasing
    private var pendingEraseActor: RunnableBool? = null

    fun eraseTdlibDatabase(onDelete: RunnableBool) { // FIXME TDLib: Proper database optimization
        val success: Boolean
        synchronized(clientLock) {
            if (pendingEraseActor != null) return
            if (client != null && !instancePaused && !inRecoveryMode()) {
                pendingEraseActor = onDelete
                client!!.sendClose()
                return
            } else {
                pendingEraseActor = null
                success = eraseTdlibDatabaseImpl()
            }
        }
        onDelete.runWithBool(success)
    }

    private fun eraseTdlibFilesImpl(): Boolean {
        var success = true
        for (directory in TdlibManager.getAllTdlibDirectories(false)) {
            val file = File(parameters.filesDirectory, directory)
            if (file.exists()) {
                success = delete(file, true) && success
            }
        }
        for (directory in TdlibManager.getAllTdlibDirectories(true)) {
            val file = File(parameters.databaseDirectory, directory)
            if (file.exists()) {
                success = delete(file, true) && success
            }
        }
        return success
    }

    private var needDropNotificationIdentifiers = false

    private fun eraseTdlibDatabaseImpl(): Boolean {
        var dbFile: File?

        var success = true
        dbFile = File(parameters.databaseDirectory, "db.sqlite-wal")
        success = (!dbFile.exists() || dbFile.delete()) && success
        dbFile = File(parameters.databaseDirectory, "db.sqlite-shm")
        success = (!dbFile.exists() || dbFile.delete()) && success
        dbFile = File(parameters.databaseDirectory, "db.sqlite")
        success = (!dbFile.exists() || dbFile.delete()) && success

        if (success) {
            needDropNotificationIdentifiers = true
            // notifications().onDropNotificationData(true);
        }

        success = eraseTdlibFilesImpl() && success

        if (success) {
            Log.i("Successfully deleted TDLib database, accountId:%d", accountId)
        } else {
            Log.e("Failed to delete TDLib database, accountId:%d", accountId)
        }

        return success
    }

    // Base
    private var authorizationState: AuthorizationState? = null

    fun authorizationState(): AuthorizationState? {
        return authorizationState
    }

    fun switchToNextAuthorizedAccount(): Boolean {
        if (context().preferredAccountId() == accountId) {
            val nextAccountId = context().findNextAccountId(accountId)
            if (nextAccountId != TdlibAccount.NO_ID) {
                context().changePreferredAccountId(nextAccountId, TdlibManager.SWITCH_REASON_UNAUTHORIZED)
                return true
            }
        }
        return false
    }

    fun signOut() {
        switchToNextAuthorizedAccount()
        val isMulti = context().isMultiUser()
        val name = if (isMulti) TD.getUserName(account()!!.getFirstName(), account()!!.getLastName()) else null
        incrementReferenceCount(REFERENCE_TYPE_JOB)
        /*deleteAllFiles(ignored -> */
        client().send(LogOut(), Client.ResultHandler { result: TdApi.Object? ->
            if (isMulti) {
                UI.showToast(Lang.getString(R.string.SignedOutAs, name), Toast.LENGTH_SHORT)
            }
            decrementReferenceCount(REFERENCE_TYPE_JOB)
        }) /*)*/
    }

    fun destroy() {
        send<TdApi.Ok?>(Destroy(), typedOkHandler())
    }

    val isCurrent: Boolean
        get() = context().preferredAccountId() == accountId

    fun tdlibCommitHash(): String? {
        return context().tdlibCommitHash()
    }

    fun tdlibCommitHashFull(): String? {
        return context().tdlibCommitHashFull()
    }

    fun tdlibVersion(): String? {
        return context().tdlibVersion()
    }

    val isAuthorized: Boolean
        get() = authorizationState != null && authorizationState!!.getConstructor() == TdApi.AuthorizationStateReady.CONSTRUCTOR

    val isUnauthorized: Boolean
        get() {
            @Status val status: Int = getStatus(authorizationState)
            return status == Status.Companion.UNAUTHORIZED
        }

    @Status
    fun authorizationStatus(): Int {
        synchronized(dataLock) {
            return getStatus(authorizationState)
        }
    }

    @TdlibThread
    private fun updateAuthState(context: ClientHolder, newAuthState: AuthorizationState) {
        @Status val prevStatus = authorizationStatus()
        synchronized(dataLock) {
            this.authorizationState = newAuthState
        }
        when (newAuthState.getConstructor()) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR, TdApi.AuthorizationStateClosed.CONSTRUCTOR -> this.connectionState =
                ConnectionState.UNKNOWN
        }
        @Status val newStatus = authorizationStatus()

        closeListeners.notifyConditionChanged(true)
        readyOrWaitingForDataListeners.notifyConditionChanged(false)

        if (prevStatus == Status.Companion.UNKNOWN && newStatus != Status.Companion.UNKNOWN) {
            synchronized(dataLock) {
                resetChatsData()
            }
        }

        when (newStatus) {
            Status.Companion.UNKNOWN -> {
                if (newAuthState.getConstructor() == TdApi.AuthorizationStateClosed.CONSTRUCTOR) {
                    val eraseActor: RunnableBool?
                    val eraseSuccess: Boolean

                    var forceErase = false
                    if (isLoggingOut) { // FIXME TDLib: AuthorizationStateLoggedOut
                        setLoggingOut(false)
                        forceErase = true

                        activeCalls.clear()
                        setHaveActiveCalls(false)
                    }

                    synchronized(clientLock) {
                        client!!.closeState++ // 1
                        client!!.stop()
                        client!!.closeState++ // 2
                        client!!.close()
                        client!!.closeState++ // 3
                        resetState()
                        synchronized(dataLock) {
                            resetContextualData()
                        }
                        client!!.closeState++ // 4
                        listeners.performRestart()
                        client!!.closeState++ // 5
                        ImageLoader.instance().clear(accountId, true)
                        client!!.closeState++ // 6

                        if (pendingEraseActor != null || forceErase) {
                            eraseActor = pendingEraseActor
                            pendingEraseActor = null
                            eraseSuccess = eraseTdlibDatabaseImpl()
                        } else {
                            eraseActor = null
                            eraseSuccess = false
                        }

                        client!!.closeState++ // 7

                        val prevLatch = client!!.closeLatch
                        if (stressTest == 0 && (instancePaused || shouldPause())) {
                            client = null
                        } else {
                            if (stressTest > 0) stressTest--
                            client = newClient()
                            schedulePause()
                        }
                        prevLatch.countDown()
                    }

                    if (eraseActor != null) {
                        eraseActor.runWithBool(eraseSuccess)
                    }
                }
            }

            Status.Companion.UNAUTHORIZED -> {
                if (newAuthState.getConstructor() == TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR) {
                    setLoggingOut(true)
                } else {
                    synchronized(dataLock) {
                        resetChatsData()
                    }
                }
            }

            Status.Companion.READY -> {
                setLoggingOut(false)
            }

            else -> throw IllegalStateException(newStatus.toString())
        }
        if (newStatus != Status.Companion.UNKNOWN && Log.needMeasureLaunchSpeed() && !context.hasLogged()) {
            val timeSinceInitialization = context.timeSinceInitializationMs()
            val timeWasted = context.timeWasted()
            Log.v(
                "INITIALIZATION: TDLIB FINISHED INITIALIZATION & SENT VALID AUTH STATE IN %dMS, WASTED: %dMS",
                timeSinceInitialization - timeWasted,
                timeWasted
            )
        }
        Log.i(Log.TAG_ACCOUNTS, "updateAuthState accountId:%d %s", accountId, newAuthState.javaClass.getSimpleName())
        if (newStatus == Status.Companion.READY) {
            setNeedPeriodicSync(true)
        } else if (newStatus == Status.Companion.UNAUTHORIZED) {
            setNeedPeriodicSync(false)
        }

        val myUserId = myUserId()
        context().onAuthStateChanged(this, newAuthState, newStatus, myUserId)
        listeners().updateAuthorizationState(newAuthState)
        if (newStatus != Status.Companion.READY) {
            this.isStartupPerformed = false
        }
        setNeedTimeZoneListener(newStatus != Status.Companion.UNKNOWN)
        if (prevStatus == Status.Companion.UNKNOWN && newStatus != prevStatus) {
            onInitialized()
        }
        if (prevStatus != Status.Companion.READY && newStatus == Status.Companion.READY) {
            runStartupChecks()
        }
        if (prevStatus != Status.Companion.UNAUTHORIZED && newStatus == Status.Companion.UNAUTHORIZED) {
            Log.i("Performing account cleanup for accountId:%d", accountId)
            listeners().performCleanup()
        } else if (newStatus == Status.Companion.READY) {
            onPerformStartup()
            val myUser = cache().myUser()
            if (myUser != null) {
                context().onUpdateAccountProfile(id(), myUser, true)
            }
        }
        if (newStatus == Status.Companion.UNAUTHORIZED) checkChangeLogs(false, false)

        if (newStatus == Status.Companion.READY && stressTest > 0) {
            clientHolder().sendClose()
        }
    }

    @TdlibThread
    private fun updateFreezeState(context: ClientHolder?, update: UpdateFreezeState?) {
        // TODO freeze handling
    }

    private var needTimeZoneListener = false

    private fun setNeedTimeZoneListener(needTimeZoneListener: Boolean) {
        if (this.needTimeZoneListener != needTimeZoneListener) {
            this.needTimeZoneListener = needTimeZoneListener
            if (needTimeZoneListener) {
                context.dateManager().addListener(this)
            } else {
                context.dateManager().removeListener(this)
            }
        }
    }

    override fun onTimeChanged() {
        updateUtcTimeOffset()
    }

    override fun onTimeZoneChanged() {
        updateUtcTimeOffset()
    }

    @set:TdlibThread
    var isOptimizing: Boolean = false
        get() {
            synchronized(dataLock) {
                return field
            }
        }
        private set(isOptimizing) {
            synchronized(dataLock) {
                if (field == isOptimizing) return
                field = isOptimizing
            }
            context().global().notifyOptimizing(this, isOptimizing)
        }

    fun checkChangeLogs(alreadySent: Boolean, test: Boolean): Boolean {
        val status = authorizationStatus()
        if (status != Status.Companion.READY && status != Status.Companion.UNAUTHORIZED) {
            return false
        }
        val key = accountId.toString() + "_app_version"
        if (status == Status.Companion.UNAUTHORIZED || alreadySent) {
            instance().putInt(key, BuildConfig.ORIGINAL_VERSION_CODE)
            return alreadySent
        }
        val prevVersion = if (test || Config.TEST_CHANGELOG) 0 else instance().getInt(key, 0)
        if (prevVersion != BuildConfig.ORIGINAL_VERSION_CODE) {
            val updates: MutableList<InputMessageContent?> = ArrayList<InputMessageContent?>()
            val functions: MutableList<TdApi.Function<*>?> = ArrayList<TdApi.Function<*>?>()
            ChangeLogList.collectChangeLogs(prevVersion, functions, updates, test)
            if (!updates.isEmpty()) {
                incrementReferenceCount(REFERENCE_TYPE_JOB) // starting task
                functions.add(CreatePrivateChat(TELEGRAM_ACCOUNT_ID, false))
                if (options.telegramServiceNotificationsChatId != 0L && options.telegramServiceNotificationsChatId != TELEGRAM_ACCOUNT_ID) {
                    functions.add(GetChat(options.telegramServiceNotificationsChatId))
                }
                val remainingFunctions = AtomicInteger(functions.size)
                val handler = Client.ResultHandler { `object`: TdApi.Object? ->
                    if (`object`!!.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                        Log.e("Received error while posting change log: %s", TD.toErrorString(`object`))
                    }
                    if (remainingFunctions.decrementAndGet() == 0) {
                        val remainingUpdates = AtomicInteger(updates.size)
                        val chatId = serviceNotificationsChatId()
                        val localMessageHandler = Client.ResultHandler { message: TdApi.Object? ->
                            if (message!!.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                                Log.e("Received error while sending change log: %s", TD.toErrorString(`object`))
                            }
                            if (remainingUpdates.decrementAndGet() == 0) {
                                decrementReferenceCount(REFERENCE_TYPE_JOB) // ending task
                            }
                        }
                        for (content in updates) {
                            client().send(
                                AddLocalMessage(chatId, MessageSenderUser(TELEGRAM_ACCOUNT_ID),  /*TODO: @tgx_android?*/null, true, content),
                                localMessageHandler
                            )
                        }
                    }
                }
                for (function in functions) {
                    client().send(function, handler)
                }
            }
            instance().putInt(key, BuildConfig.ORIGINAL_VERSION_CODE)
        }
        return true
    }

    // Startup
    var isStartupPerformed: Boolean = false
        private set

    private fun onPerformStartup() {
        Log.i("Performing account startup for accountId:%d, isAfterRestart:%b", accountId, this.isStartupPerformed)
        listeners().performStartup(this.isStartupPerformed)
        this.isStartupPerformed = true
    }

    // Phone number
    private var authPhoneCode: String? = null
    private var authPhoneNumber: String? = null

    fun setAuthPhoneNumber(code: String, number: String) {
        this.authPhoneCode = code
        this.authPhoneNumber = number
    }

    fun hasAuthPhoneNumber(): Boolean {
        return !isEmpty(authPhoneNumber) || !isEmpty(authPhoneCode)
    }

    fun authPhoneCode(): String? {
        return authPhoneCode
    }

    fun authPhoneNumber(): String? {
        return authPhoneNumber
    }

    fun authPhoneNumberFormatted(): String? {
        return if (hasAuthPhoneNumber()) Strings.formatPhone("+" + authPhoneCode + authPhoneNumber, false, true) else ""
    }

    fun robotLoginCode(): String {
        val number = robotPhoneNumber()
        if (isEmpty(number)) {
            return ""
        } else {
            val b = StringBuilder(5)
            for (i in 0..4) {
                b.append(number, 5, 6)
            }
            return b.toString()
        }
    }

    fun robotId(): Int {
        val number = robotPhoneNumber()
        if (isEmpty(number)) {
            return 0
        } else {
            return parseInt(number!!.substring("99966173".length)) - Config.ROBOT_ID_PREFIX
        }
    }

    fun robotPhoneNumber(): String? {
        val user = myUser()
        var phoneNumber: String? = if (user != null) user.phoneNumber else null
        if (isEmpty(phoneNumber)) {
            phoneNumber = authPhoneCode + authPhoneNumber
        }
        if (isEmpty(phoneNumber) || !phoneNumber!!.startsWith("999661") || phoneNumber.length != "999661".length + 4) {
            return null
        }
        return phoneNumber
    }

    // Getters
    fun id(): Int {
        return accountId
    }

    val isProduction: Boolean
        get() = instanceMode == Mode.Companion.NORMAL

    private val isDebugInstance: Boolean
        get() = instanceMode == Mode.Companion.DEBUG

    private val isServiceInstance: Boolean
        get() = instanceMode == Mode.Companion.SERVICE

    fun inRecoveryMode(): Boolean {
        return !this.isServiceInstance && context.inRecoveryMode()
    }

    fun setInstanceMode(@Mode mode: Int) {
        synchronized(clientLock) {
            setInstanceModeImpl(mode)
        }
    }

    private fun getReferenceCount(): Int {
        return this.referenceCount.get()
    }

    fun hasActiveReferences(): Boolean {
        return getReferenceCount() > 0
    }

    private fun setInstanceModeImpl(@Mode mode: Int) {
        if (this.instanceMode != mode) {
            this.instanceMode = mode
            if (instancePaused) return
            client!!.sendClose()
        }
    }

    fun account(): TdlibAccount? {
        return context.account(accountId)
    }

    fun wakeUp() {
        synchronized(clientLock) {
            if (client == null || !instancePaused || !inTdlibThread()) {
                clientHolderUnsafe()
                return
            }
        }
        //FIXME?
        Thread(Runnable { this.clientHolder() }).start()
    }

    fun ownsClient(client: Client?): Boolean {
        synchronized(clientLock) {
            return this.client != null && this.client!!.client == client
        }
    }

    private fun clientHolder(): ClientHolder {
        val latch: CountDownLatch?
        synchronized(clientLock) {
            val holder = clientHolderUnsafe()
            if (holder != null) return holder
            latch = client!!.closeLatch
        }
        if (instance().forceTdlibRestart()) {
            if (!U.awaitLatch(latch, 10, TimeUnit.SECONDS)) {
                val e =
                    RuntimeException("Long close detected. authState: " + authorizationState + ", closeState: " + (if (client != null) client!!.closeState else -1))
                onOtherError(e)
                throw e
            }
        } else {
            U.awaitLatch(latch)
        }
        return clientHolder()
    }

    fun checkDeadlocks(after: Runnable?) {
        if (!Config.PROFILE_DEADLOCKS) {
            if (after != null) {
                after.run()
            }
            return
        }
        val crashSignal = CancellationSignal()
        val forceAnr = Runnable {
            if (!crashSignal.isCanceled()) {
                // Force ANR to cause system report, because TDLib thread is unavailable at least for 7 seconds
                clientExecute(SetAlarm(0.0), 0, false)
            }
        }
        crashSignal.setOnCancelListener(CancellationSignal.OnCancelListener { ui()!!.removeCallbacks(forceAnr) }
        )
        client().send(SetAlarm(0.0), Client.ResultHandler { ignored: TdApi.Object? ->
            crashSignal.cancel()
            if (after != null) {
                after.run()
            }
        })
        ui()!!.postDelayed(forceAnr, 7500)
    }

    fun client(): Client { // TODO migrate all tdlib.client().send(..) to tdlib.send(..)
        return clientHolder().client
    }

    abstract class CancellableResultHandler<T : TdApi.Object?> : ResultHandler<T?> {
        private val signal = CancellationSignal()

        fun cancel() {
            signal.cancel()
        }

        val isPending: Boolean
            get() = !signal.isCanceled()

        abstract fun act(result: T?, error: TdApi.Error?)

        override fun onResult(result: T?, error: TdApi.Error?) {
            if (this.isPending) {
                act(result, error)
            }
        }
    }

    fun interface ResultHandler<T : TdApi.Object?> {
        fun onResult(result: T?, error: TdApi.Error?)

        fun doOnResult(action: RunnableData<T?>): ResultHandler<T?> {
            return Tdlib.ResultHandler { result: T?, error: TdApi.Error? ->
                onResult(result, error)
                if (result != null) {
                    action.runWithData(result)
                }
            }
        }

        companion object {
            fun <T : TdApi.Object?> toTdlibHandler(handler: ResultHandler<T?>): Client.ResultHandler {
                return Client.ResultHandler { result: TdApi.Object? ->
                    if (result!!.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                        handler.onResult(null, result as TdApi.Error)
                    } else {
                        handler.onResult(result as T, null)
                    }
                }
            }
        }
    }

    fun <T : TdApi.Object?> send(function: TdApi.Function<T?>?, successHandler: RunnableData<T?>?, errorHandler: RunnableData<TdApi.Error?>?) {
        send<T?>(function, Tdlib.ResultHandler { result: T?, error: TdApi.Error? ->
            if (error != null) {
                if (errorHandler != null) {
                    errorHandler.runWithData(error)
                }
            } else {
                if (successHandler != null) {
                    successHandler.runWithData(result)
                }
            }
        })
    }

    fun <T : TdApi.Object?> send(function: TdApi.Function<T?>?, handler: ResultHandler<T?>) {
        send<T?>(client(), function, handler)
    }

    fun <T : TdApi.Object?> sendAll(functions: Array<TdApi.Function<T?>?>, handler: ResultHandler<T?>, after: Runnable?) {
        sendAll<T?>(functions, ResultHandler.Companion.toTdlibHandler<T?>(handler), after)
    }

    private fun <T : TdApi.Object?> send(function: TdApi.Function<T?>?, handler: Client.ResultHandler?) {
        send<T?>(client(), function, handler)
    }

    fun <T : TdApi.Object?> sendAll(functions: Array<TdApi.Function<T?>?>, handler: Client.ResultHandler, after: Runnable?) {
        if (functions.size == 0) {
            if (after != null) {
                after.run()
            }
            return
        }
        if (functions.size == 1) {
            send<T?>(functions[0], if (after != null) Client.ResultHandler { result: TdApi.Object? ->
                handler.onResult(result)
                after.run()
            } else handler)
            return
        }
        val remaining: AtomicInteger?
        val actualHandler: Client.ResultHandler?
        if (after != null) {
            remaining = AtomicInteger(functions.size)
            actualHandler = Client.ResultHandler { result: TdApi.Object? ->
                handler.onResult(result)
                if (remaining.decrementAndGet() == 0) {
                    after.run()
                }
            }
        } else {
            remaining = null
            actualHandler = handler
        }
        for (function in functions) {
            send<T?>(function, actualHandler)
        }
    }

    private fun clientHolderUnsafe(): ClientHolder? {
        if (client == null) {
            client = newClient()
            ui()!!.postDelayed(Runnable { this.checkPauseTimeout() }, 350)
        }
        if (!instancePaused) return client
        check(!inTdlibThread())
        return null
    }

    private fun performOptional(runnable: RunnableData<Client?>, onFailure: Runnable?) {
        val client: Client?
        synchronized(clientLock) {
            if (instancePaused) return
            val holder = clientHolderUnsafe()
            client = if (holder != null) holder.client else null
        }
        if (client != null) {
            runnable.runWithData(client)
        } else if (onFailure != null) {
            onFailure.run()
        }
    }

    @JvmOverloads
    fun runOnUiThread(runnable: Runnable, timeoutMs: Long = 0) {
        incrementUiReferenceCount()
        val act = Runnable {
            runnable.run()
            decrementUiReferenceCount()
        }
        if (timeoutMs > 0) {
            ui()!!.postDelayed(act, timeoutMs)
        } else {
            ui()!!.post(act)
        }
    }

    @JvmOverloads
    fun runOnTdlibThread(runnable: Runnable, timeoutSeconds: Double = 0.0, acquireReference: Boolean = true) {
        if (acquireReference) {
            incrementReferenceCount(REFERENCE_TYPE_JOB)
        }
        clientHolder().runOnTdlibThread(Runnable {
            runnable.run()
            if (acquireReference) {
                decrementReferenceCount(REFERENCE_TYPE_JOB)
            }
        }, timeoutSeconds)
    }

    fun searchContacts(searchQuery: String?, limit: Int, handler: Client.ResultHandler?) {
        Log.ensureReturnType<Users?>(SearchContacts::class.java, Users::class.java)
        client().send(SearchContacts(searchQuery, limit), handler)
    }

    fun loadMoreChats(chatList: ChatList, limit: Int, handler: Client.ResultHandler?) {
        Log.ensureReturnType<TdApi.Ok?>(LoadChats::class.java, TdApi.Ok::class.java)
        client().send(LoadChats(chatList, limit), handler)
    }

    fun readAllChats(chatList: ChatList, after: RunnableInt?) {
        val readChatsCount = AtomicInteger(0)
        getAllChats(chatList, RunnableData { chat: Chat? ->
            var read = false
            if (chat!!.unreadCount != 0 && chat.lastMessage != null) {
                readMessages(chat.id, longArrayOf(chat.lastMessage!!.id), MessageSourceChatList())
                read = true
            }
            if (chat.isMarkedAsUnread) {
                client().send(ToggleChatIsMarkedAsUnread(chat.id, false), okHandler())
                read = true
            }
            if (chat.unreadMentionCount > 0) {
                client().send(ReadAllChatMentions(chat.id), okHandler())
                read = true
            }
            if (read) {
                readChatsCount.incrementAndGet()
            }
        }, RunnableBool { isFinal: Boolean ->
            if (isFinal && after != null) {
                after.runWithInt(readChatsCount.get())
            }
        }, false)
    }

    fun getAllChats(chatList: ChatList, perChatCallback: RunnableData<Chat?>, after: RunnableBool?, callMiddle: Boolean) {
        chatList(chatList).loadAll(perChatCallback, after)
    }

    class Generation {
        var latch: CountDownLatch? = null

        @JvmField
        var file: TdApi.File? = null
        @JvmField
        var destinationPath: String? = null

        var generationId: Long = 0
        var isPending = false

        var onCancel: Runnable? = null
    }

    private val awaitingGenerations = HashMap<String?, Generation?>()
    private val pendingGenerations = HashMap<Long?, Generation?>()
    private val fileWaitingGenerations = HashMap<Int?, Generation?>()

    fun generateFile(id: String?, fileType: TdApi.FileType?, isSecret: Boolean, priority: Int, timeoutMs: Long): Generation? {
        val latch = CountDownLatch(2)
        val generation = Generation()
        generation.latch = latch

        synchronized(awaitingGenerations) {
            awaitingGenerations.put(id, generation)
        }

        client().send(
            PreliminaryUploadFile(InputFileGenerated(null, id, 0), if (isSecret) FileTypeSecret() else fileType, priority),
            Client.ResultHandler { `object`: TdApi.Object? ->
                when (`object`!!.getConstructor()) {
                    TdApi.File.CONSTRUCTOR -> generation.file = `object` as TdApi.File
                    TdApi.Error.CONSTRUCTOR -> {
                        Log.w("Error starting file generation: %s", TD.toErrorString(`object`))
                        latch.countDown() // since file generation may have not started
                    }
                }
                latch.countDown()
            })

        try {
            if (timeoutMs > 0) {
                latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                latch.await()
            }
        } catch (e: InterruptedException) {
            Log.i(e)
        }
        synchronized(awaitingGenerations) {
            if (generation.isPending) {
                pendingGenerations.remove(generation.generationId)
            } else {
                awaitingGenerations.remove(id)
            }
        }
        if (generation.file == null || generation.destinationPath == null) {
            return null
        }
        return generation
    }

    fun finishGeneration(generation: Generation, error: TdApi.Error?) {
        synchronized(awaitingGenerations) {
            pendingGenerations.remove(generation.generationId)
            if (error == null && generation.file != null) {
                fileWaitingGenerations.put(generation.file!!.id, generation)
                files().subscribe(generation.file!!.id, obtainGeneratedFilesListener())
            }
        }
        client().send(FinishFileGeneration(generation.generationId, error), silentHandler())
    }

    private var generatedFilesListener: TdlibFilesManager.SimpleListener? = null

    private fun obtainGeneratedFilesListener(): TdlibFilesManager.SimpleListener {
        if (generatedFilesListener == null) {
            generatedFilesListener = TdlibFilesManager.SimpleListener { file: TdApi.File? ->
                if (!isEmpty(file!!.local.path) && file.size != 0L && file.local.isDownloadingCompleted) {
                    synchronized(awaitingGenerations) {
                        val generation = fileWaitingGenerations.remove(file.id)
                        files().unsubscribe(file.id, generatedFilesListener!!)
                        if (generation != null) {
                            generation.file = file
                        }
                    }
                }
            }
        }

        return generatedFilesListener!!
    }

    fun isBadInstantView(instantView: WebPageInstantView?): Boolean {
        return instantView == null || !instantView.isFull || instantView.pageBlocks == null || instantView.pageBlocks.size == 0 || !TD.hasInstantView(
            instantView.version
        )
    }

    fun fetchInstantView(url: String?, callback: ResultHandler<WebPageInstantView?>) {
        send<WebPageInstantView?>(GetWebPageInstantView(url, true), Tdlib.ResultHandler { instantView: WebPageInstantView?, error: TdApi.Error? ->
            if (error != null || isBadInstantView(instantView)) {
                send<WebPageInstantView?>(GetWebPageInstantView(url, false), callback)
            } else {
                callback.onResult(instantView, null)
            }
        })
    }

    interface MessagePropertyChecker {
        fun checkProperty(properties: MessageProperties?): Boolean
    }

    fun checkMessageProperties(messages: MutableList<TdApi.Message?>, checker: MessagePropertyChecker, callback: RunnableBool) {
        getMessageProperties(messages, RunnableData { allProperties: Array<MessageProperties?>? ->
            var first = true
            var result = false
            for (properties in allProperties!!) {
                val value = checker.checkProperty(properties)
                if (first) {
                    result = value
                    first = false
                } else if (!value) {
                    result = false
                    break
                }
            }
            callback.runWithBool(result)
        })
    }

    fun getMessageProperties(messages: MutableList<TdApi.Message?>, callback: RunnableData<Array<MessageProperties?>?>) {
        val allProperties = arrayOfNulls<MessageProperties>(messages.size)
        val remaining = AtomicInteger(messages.size)
        var i = 0
        for (message in messages) {
            val index = i
            getMessageProperties(message, RunnableData { properties: MessageProperties? ->
                allProperties[index] = properties
                if (remaining.decrementAndGet() == 0) {
                    callback.runWithData(allProperties)
                }
            })
            i++
        }
    }

    fun getMessagePropertiesSync(message: TdApi.Message): MessageProperties {
        return getMessagePropertiesSync(message.chatId, message.id)
    }

    fun getMessagePropertiesSync(chatId: Long, messageId: Long): MessageProperties {
        val properties = clientExecuteT<MessageProperties?>(GetMessageProperties(chatId, messageId), false)
        if (properties != null) {
            return properties
        } else {
            return defaultMessageProperties()
        }
    }

    fun getMessageProperties(message: TdApi.Message?, callback: RunnableData<MessageProperties?>) {
        if (message != null) {
            getMessageProperties(message.chatId, message.id, callback)
        }
    }

    fun getMessageProperties(chatId: Long, messageId: Long, callback: RunnableData<MessageProperties?>) {
        send<MessageProperties?>(GetMessageProperties(chatId, messageId), Tdlib.ResultHandler { properties: MessageProperties?, error: TdApi.Error? ->
            if (properties != null) {
                callback.runWithData(properties)
            } else {
                callback.runWithData(defaultMessageProperties())
            }
        })
    }

    fun getMessage(chatId: Long, messageId: Long, callback: RunnableData<TdApi.Message?>?) {
        client().send(GetMessageLocally(chatId, messageId), Client.ResultHandler { localResult: TdApi.Object? ->
            if (localResult is TdApi.Message) {
                if (callback != null) callback.runWithData(localResult)
            } else {
                client().send(GetMessage(chatId, messageId), Client.ResultHandler { serverResult: TdApi.Object? ->
                    if (serverResult is TdApi.Message) {
                        if (callback != null) callback.runWithData(serverResult)
                    } else {
                        if (callback != null) callback.runWithData(null)
                        Log.i("Could not get message from server: %s, chatId:%s, messageId:%s", TD.toErrorString(serverResult), chatId, messageId)
                    }
                })
            }
        })
    }

    fun getMessageLocally(chatId: Long, messageId: Long): TdApi.Message? {
        return getMessageLocally(chatId, messageId, 0)
    }

    fun getMessageLocally(chatId: Long, messageId: Long, timeoutMs: Long): TdApi.Message? {
        if (inTdlibThread()) {
            return null
        }
        val result = clientExecute(GetMessageLocally(chatId, messageId), timeoutMs)
        if (result is TdApi.Message) return result
        if (result is TdApi.Error) Log.i("Could not get message: %s, chatId:%s, messageId:%s", TD.toErrorString(result), chatId, messageId)
        return null
    }

    class Album @JvmOverloads constructor(
      @JvmField val messages: MutableList<TdApi.Message>,
      val mayHaveNewerItems: Boolean = false,
      val mayHaveOlderItems: Boolean = false
    ) {
        fun mayHaveMoreItems(): Boolean {
            return mayHaveNewerItems || mayHaveOlderItems
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) return true
            if (obj == null || obj !is Album) return false
            val cmp = obj.messages
            if (cmp.size != messages.size) {
                return false
            }
            for (i in cmp.indices) {
                val a = cmp.get(i)
                val b = messages.get(i)
                if (a.chatId != b.chatId || a.id != b.id || a.mediaAlbumId != b.mediaAlbumId) {
                    return false
                }
            }
            return true
        }
    }

    fun getAlbum(message: TdApi.Message, onlyLocal: Boolean, prevAlbum: Album?, callback: RunnableData<Album?>?) {
        if (message.mediaAlbumId == 0L) {
            if (callback != null) {
                callback.runWithData(null)
            }
            return
        }
        if (prevAlbum != null) {
            getAlbum(prevAlbum.messages, onlyLocal, prevAlbum, callback)
        } else {
            val album: MutableList<TdApi.Message> = ArrayList<TdApi.Message>(MAX_MESSAGE_GROUP_SIZE)
            album.add(message)
            getAlbum(album, onlyLocal, prevAlbum, callback)
        }
    }

    fun getAlbum(album: MutableList<TdApi.Message>, onlyLocal: Boolean, prevAlbum: Album?, callback: RunnableData<Album?>?) {
        val newestMessage = album.get(0)
        val oldestMessage = album.get(album.size - 1)

        val olderMessages: MutableList<TdApi.Message> = ArrayList<TdApi.Message>()
        val newerMessages: MutableList<TdApi.Message> = ArrayList<TdApi.Message>()

        val endFound = AtomicBoolean(prevAlbum != null && !prevAlbum.mayHaveOlderItems) // oldest message found
        val startFound = AtomicBoolean(prevAlbum != null && !prevAlbum.mayHaveNewerItems) // newest message found

        var reqCount = 0
        if (!endFound.get()) reqCount++
        if (!startFound.get()) reqCount++

        if (reqCount == 0) {
            if (callback != null) {
                callback.runWithData(prevAlbum)
            }
            return
        }

        val requestCount = AtomicInteger(reqCount)

        val after = Runnable {
            val result: MutableList<TdApi.Message>?
            val addedCount = olderMessages.size + newerMessages.size
            if (addedCount > 0) {
                result = ArrayList<TdApi.Message>(newerMessages.size + album.size + olderMessages.size)
                result.addAll(newerMessages)
                result.addAll(album)
                result.addAll(olderMessages)
            } else {
                result = album
            }
            val mayHaveNewerItems = result.size < MAX_MESSAGE_GROUP_SIZE && !startFound.get()
            val mayHaveOlderItems = result.size < MAX_MESSAGE_GROUP_SIZE && !endFound.get()
            if (callback != null) {
                callback.runWithData(Album(result, mayHaveNewerItems, mayHaveOlderItems))
            }
        }

        val count: Int = MAX_MESSAGE_GROUP_SIZE - album.size + 1
        if (!endFound.get()) {
            client().send(GetChatHistory(oldestMessage.chatId, oldestMessage.id, 0, count, onlyLocal), Client.ResultHandler { result: TdApi.Object? ->
                when (result!!.getConstructor()) {
                    TdApi.Messages.CONSTRUCTOR -> {
                        val messages = (result as TdApi.Messages).messages
                        for (message in messages) {
                            if (message!!.id >= oldestMessage.id) continue
                            if (message.mediaAlbumId != oldestMessage.mediaAlbumId) {
                                endFound.set(true)
                                break
                            }
                            olderMessages.add(message)
                        }
                        if (olderMessages.isEmpty() && !onlyLocal) {
                            endFound.set(true)
                        }
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        Log.i("Failed to fetch part of an album: %s", TD.toErrorString(result))
                    }
                }
                if (requestCount.decrementAndGet() == 0) {
                    after.run()
                }
            })
        }
        if (!startFound.get()) {
            client().send(GetChatHistory(newestMessage.chatId, newestMessage.id, -count, count, onlyLocal), Client.ResultHandler { result: TdApi.Object? ->
                when (result!!.getConstructor()) {
                    TdApi.Messages.CONSTRUCTOR -> {
                        val messages = (result as TdApi.Messages).messages
                        var i = messages.size - 1
                        while (i >= 0) {
                            val message: TdApi.Message = messages[i]!!
                            if (message.id <= newestMessage.id) {
                                i--
                                continue
                            }
                            if (message.mediaAlbumId != newestMessage.mediaAlbumId) {
                                startFound.set(true)
                                break
                            }
                            newerMessages.add(message)
                            i--
                        }
                        Collections.reverse(newerMessages)
                        if (newerMessages.isEmpty() && !onlyLocal) {
                            startFound.set(true)
                        }
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        Log.i("Failed to fetch part of an album: %s", TD.toErrorString(result))
                    }
                }
                if (requestCount.decrementAndGet() == 0) {
                    after.run()
                }
            })
        }
    }

    private fun updateTdlibThread() {
        tdlibThread = Thread.currentThread()
    }

    fun ensureTdlibThread() {
        check(inTdlibThread()) { Thread.currentThread().getName() }
    }

    fun inTdlibThread(): Boolean {
        if (tdlibThread != null) {
            // FIXME[tdlib]: it is safe as long as there's just one thread for all apps
            return Thread.currentThread() === tdlibThread
        } else {
            // FIXME[tdlib]: more reliable way
            val tdlibThreadName = "TDLib thread"
            return tdlibThreadName == Thread.currentThread().getName()
        }
    }

    fun clientExecute(function: TdApi.Function<*>, timeoutMs: Long): TdApi.Object? {
        return clientExecute(function, timeoutMs, true)
    }

    @Throws(TdlibException::class)
    fun <T : TdApi.Object?> clientExecuteT(function: TdApi.Function<T?>, throwErrors: Boolean): T? {
        return clientExecuteT<T?>(function, 0, throwErrors)
    }

    @Throws(TdlibException::class)
    fun <T : TdApi.Object?> clientExecuteT(function: TdApi.Function<T?>, timeoutMs: Long, throwErrors: Boolean): T? {
        val result = clientExecute(function, timeoutMs)
        if (result is TdApi.Error) {
            if (throwErrors) {
                throw TdlibException(result)
            }
            Log.i("clientExecute %s failed: %s", function.javaClass.getName(), TD.toErrorString(result))
            return null
        }
        return result as T?
    }

    private fun clientExecute(function: TdApi.Function<*>, timeoutMs: Long, requiresTdlibInitialization: Boolean): TdApi.Object? {
        check(!inTdlibThread()) { "Cannot call from TDLib thread: " + function }
        val latch = CountDownLatch(1)
        val response = AtomicReference<TdApi.Object?>()
        val act = Runnable {
            if (requiresTdlibInitialization) {
                incrementReferenceCount(REFERENCE_TYPE_REQUEST_EXECUTION)
            }
            client().send(function, Client.ResultHandler { `object`: TdApi.Object? ->
                synchronized(response) {
                    response.set(`object`)
                    latch.countDown()
                }
                if (requiresTdlibInitialization) {
                    decrementReferenceCount(REFERENCE_TYPE_REQUEST_EXECUTION)
                }
            })
        }
        when (function.getConstructor()) {
            SetAlarm.CONSTRUCTOR, TdApi.Close.CONSTRUCTOR, TdApi.GetAuthorizationState.CONSTRUCTOR, TdApi.GetCurrentState.CONSTRUCTOR -> act.run()
            else -> if (requiresTdlibInitialization) {
                awaitInitialization(act)
            } else {
                act.run()
            }
        }
        try {
            if (timeoutMs > 0) {
                latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                latch.await()
            }
        } catch (e: InterruptedException) {
            Log.i(e)
        }
        synchronized(response) {
            return response.get()
        }
    }

    fun getRemoteFile(remoteId: String?, fileType: TdApi.FileType?, timeoutMs: Long): TdApi.File? {
        val result = clientExecute(GetRemoteFile(remoteId, fileType), timeoutMs)
        return if (result is TdApi.File) result else null
    }

    fun clientParameters(): SetTdlibParameters {
        return parameters
    }

    fun cache(): TdlibCache {
        return cache
    }

    fun emoji(): TdlibEmojiManager {
        return emoji
    }

    internal fun reactions(): TdlibEmojiReactionsManager {
        return reactions
    }

    internal fun outline(): TdlibOutlineManager {
        return outline
    }

    fun genericAnimationEffects(): TdlibSingleton<Stickers?> {
        return genericReactionEffects
    }

    fun listeners(): TdlibListeners {
        return listeners
    }

    fun status(): TdlibStatusManager {
        return statusManager
    }

    fun filegen(): TdlibFileGenerationManager {
        return fileGenerationManager
    }

    fun messageViewer(): TdlibMessageViewer {
        return messageViewer
    }

    fun qack(): TdlibQuickAckManager {
        return quickAckManager
    }

    fun files(): TdlibFilesManager {
        return filesManager
    }

    fun settings(): TdlibSettingsManager {
        return settingsManager
    }

    fun notifications(): TdlibNotificationManager {
        return notificationManager
    }

    fun contacts(): TdlibContactManager {
        return contactManager
    }

    fun wallpaper(): TdlibWallpaperManager {
        return wallpaperManager
    }

    fun context(): TdlibManager {
        return context
    }

    fun ui(): TdlibUi {
        if (_handler == null) {
            synchronized(handlerLock) {
                if (_handler == null) {
                    val ms = SystemClock.uptimeMillis()
                    _handler = TdlibUi(this)
                    Log.i(Log.TAG_ACCOUNTS, "Created UI handler in %dms", SystemClock.uptimeMillis() - ms)
                }
            }
        }
        return _handler!!
    }

    fun uiExecute(act: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            act.run()
        } else {
            ui()!!.post(act)
        }
    }

    fun okHandler(): Client.ResultHandler {
        return Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                TdApi.Ok.CONSTRUCTOR -> {}
                TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
            }
        }
    }

    fun <T : TdApi.Object?> errorHandler(): ResultHandler<T?> {
        return Tdlib.ResultHandler { ok: T?, error: TdApi.Error? ->
            if (error != null) {
                UI.showError(error)
            }
        }
    }

    fun typedOkHandler(): ResultHandler<TdApi.Ok?> {
        return Tdlib.ResultHandler { ok: TdApi.Ok?, error: TdApi.Error? ->
            if (error != null) {
                UI.showError(error)
            }
        }
    }

    fun okHandler(after: Runnable?): Client.ResultHandler? {
        return if (after != null) Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                TdApi.Ok.CONSTRUCTOR -> tdlib().ui()!!.post(after)
                TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
            }
        } else okHandler()
    }

    fun typedOkHandler(after: Runnable?): ResultHandler<TdApi.Ok?> {
        return if (after != null) Tdlib.ResultHandler { ok: TdApi.Ok?, error: TdApi.Error? ->
            if (error != null) {
                UI.showError(error)
            } else {
                tdlib().ui()!!.post(after)
            }
        } else typedOkHandler()
    }

    fun <T : TdApi.Object?> successHandler(after: Runnable?): ResultHandler<T?> {
        return Tdlib.ResultHandler { data: T?, error: TdApi.Error? ->
            if (error != null) {
                UI.showError(error)
            } else {
                tdlib().ui()!!.post(after!!)
            }
        }
    }

    fun doneHandler(): Client.ResultHandler {
        return doneHandler
    }

    fun profilePhotoHandler(): Client.ResultHandler {
        return profilePhotoHandler
    }

    fun silentHandler(): Client.ResultHandler {
        return silentHandler
    }

    fun silentHandler(after: Runnable?): Client.ResultHandler? {
        return if (after != null) Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                TdApi.Ok.CONSTRUCTOR -> tdlib().ui()!!.post(after)
                TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
            }
        } else silentHandler()
    }

    fun messageHandler(): Client.ResultHandler {
        return messageHandler
    }

    fun imageLoadHandler(): ResultHandler<TdApi.File?> {
        return imageLoadHandler
    }

    fun userComparator(): java.util.Comparator<TdApi.User> {
        return userComparator
    }

    fun userProviderComparator(): java.util.Comparator<UserProvider> {
        return userProviderComparator
    }

    // Self User
    fun myUserId(allowCached: Boolean): Long {
        val myUserId = myUserId()
        if (myUserId != 0L || !allowCached) return myUserId
        return account()!!.getKnownUserId()
    }

    fun myUserId(): Long {
        // TODO move myUserId management to TdlibContext
        return cache().myUserId()
    }

    fun myUser(): TdApi.User? {
        // TODO move myUser management to TdlibContext
        return cache().myUser()
    }

    fun hasPremium(): Boolean {
        val user = cache().myUser()
        if (user != null) {
            return user.isPremium
        }
        return options.isPremium
    }

    fun mySender(): MessageSender {
        val userId = myUserId()
        return MessageSenderUser(userId)
    }

    fun myUserUsernames(): Usernames? {
        val user = myUser()
        return if (user != null) user.usernames else null
    }

    fun myUserUsername(): String? {
        val user = myUser()
        return user.primaryUsername()
    }

    fun myUserFull(): UserFullInfo? {
        val myUserId = myUserId()
        return if (myUserId != 0L) cache().userFull(myUserId) else null
    }

    // Chats
    fun loadChats(chatIds: LongArray?, after: Runnable?) {
        if (chatIds == null || chatIds.size == 0) return
        if (after != null) {
            val counter = intArrayOf(chatIds.size)
            for (chatId in chatIds) {
                client().send(GetChat(chatId), Client.ResultHandler { result: TdApi.Object? ->
                    silentHandler.onResult(result)
                    if (--counter[0] == 0) {
                        after.run()
                    }
                })
            }
        } else {
            for (chatId in chatIds) {
                client().send(GetChat(chatId), silentHandler)
            }
        }
    }

    fun chatOnline(chatId: Long): Boolean {
        if (chatId == 0L || isSelfChat(chatId)) return false
        when (getType(chatId)) {
            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                val userId = chatUserId(chatId)
                return userId != 0L && cache().isOnline(userId)
            }

            ChatTypeSupergroup.CONSTRUCTOR, ChatTypeBasicGroup.CONSTRUCTOR -> {}
            else -> {
                assertChatType_e562ec7d()
                throw UnsupportedOperationException(chatId.toString())
            }
        }
        return false
    }

    fun chatOnlineMemberCount(chatId: Long): Int {
        if (chatId == 0L) return 0
        val onlineMemberCount: Int?
        synchronized(dataLock) {
            onlineMemberCount = chatOnlineMemberCount.get(chatId)
        }
        return if (onlineMemberCount != null && onlineMemberCount > 1) onlineMemberCount else 0
    }

    fun chatMemberCount(chatId: Long): Int {
        if (chatId == 0L) return 0
        when (getType(chatId)) {
            ChatTypeBasicGroup.CONSTRUCTOR -> {
                val basicGroup = cache().basicGroup(toBasicGroupId(chatId))
                return if (basicGroup != null) basicGroup.memberCount else 0
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroupId = toSupergroupId(chatId)
                val supergroupFullInfo = cache().supergroupFull(supergroupId, false)
                val memberCount = if (supergroupFullInfo != null) supergroupFullInfo.memberCount else 0
                if (memberCount > 0) return memberCount
                val supergroup = cache().supergroup(supergroupId)
                return if (supergroup != null) supergroup.memberCount else 0
            }
        }
        return 0
    }

    fun privateChat(userId: Long, callback: RunnableData<Chat?>?) {
        val chat = chat(fromUserId(userId))
        if (chat != null) {
            if (callback != null) {
                callback.runWithData(chat)
            }
            return
        }
        client().send(CreatePrivateChat(userId, false), Client.ResultHandler { result: TdApi.Object? ->
            if (callback != null) {
                ui()!!.post(Runnable { callback.runWithData(chat(fromUserId(userId))) })
            }
        })
    }

    fun forumTopicInfo(chatId: Long, messageThreadId: Long): ForumTopicInfo? {
        val cacheKey = chatId.toString() + "_" + messageThreadId
        synchronized(dataLock) {
            return forumTopicInfos.get(cacheKey)
        }
    }

    // Forum topics
    fun getForumTopics(
        chatId: Long,
        query: String?,
        offsetDate: Int,
        offsetMessageId: Long,
        offsetForumTopicId: Int,
        limit: Int,
        success: RunnableData<ForumTopics?>,
        error: RunnableData<TdApi.Error?>?
    ) {
        send<ForumTopics?>(
            GetForumTopics(chatId, query, offsetDate, offsetMessageId, offsetForumTopicId, limit),
            success,
            if (error != null) error else RunnableData { err: TdApi.Error? -> Log.i("getForumTopics failed chatId:%d %s", chatId, TD.toErrorString(err)) })
    }

    fun getForumTopics(chatId: Long, query: String?, limit: Int, success: RunnableData<ForumTopics?>, error: RunnableData<TdApi.Error?>?) {
        getForumTopics(chatId, query, 0, 0, 0, limit, success, error)
    }

    fun getForumTopic(chatId: Long, forumTopicId: Int, success: RunnableData<ForumTopic?>, error: RunnableData<TdApi.Error?>?) {
        send<ForumTopic?>(
            GetForumTopic(chatId, forumTopicId),
            success,
            if (error != null) error else RunnableData { err: TdApi.Error? ->
                Log.i(
                    "getForumTopic failed chatId:%d topicId:%d %s",
                    chatId,
                    forumTopicId,
                    TD.toErrorString(err)
                )
            })
    }

    fun getForumTopicDefaultIcons(success: RunnableData<Stickers?>, error: RunnableData<TdApi.Error?>?) {
        send<Stickers?>(
            GetForumTopicDefaultIcons(),
            success,
            if (error != null) error else RunnableData { err: TdApi.Error? -> Log.i("getForumTopicDefaultIcons failed %s", TD.toErrorString(err)) })
    }

    fun createForumTopic(
        chatId: Long,
        name: String?,
        isNameImplicit: Boolean,
        icon: ForumTopicIcon?,
        success: RunnableData<ForumTopicInfo?>,
        error: RunnableData<TdApi.Error?>?
    ) {
        send<ForumTopicInfo?>(
            CreateForumTopic(chatId, name, isNameImplicit, icon),
            success,
            if (error != null) error else RunnableData { err: TdApi.Error? -> Log.i("createForumTopic failed chatId:%d %s", chatId, TD.toErrorString(err)) })
    }

    fun editForumTopic(chatId: Long, forumTopicId: Int, name: String?, editIconCustomEmoji: Boolean, iconCustomEmojiId: Long, after: Runnable?) {
        send<TdApi.Ok?>(EditForumTopic(chatId, forumTopicId, name, editIconCustomEmoji, iconCustomEmojiId), typedOkHandler(after))
    }

    fun deleteForumTopic(chatId: Long, forumTopicId: Int, after: Runnable?) {
        send<TdApi.Ok?>(DeleteForumTopic(chatId, forumTopicId), typedOkHandler(after))
    }

    fun toggleForumTopicIsClosed(chatId: Long, forumTopicId: Int, isClosed: Boolean, after: Runnable?) {
        send<TdApi.Ok?>(ToggleForumTopicIsClosed(chatId, forumTopicId, isClosed), typedOkHandler(after))
    }

    fun toggleForumTopicIsPinned(chatId: Long, forumTopicId: Int, isPinned: Boolean, after: Runnable?) {
        send<TdApi.Ok?>(ToggleForumTopicIsPinned(chatId, forumTopicId, isPinned), typedOkHandler(after))
    }

    fun setPinnedForumTopics(chatId: Long, forumTopicIds: IntArray?, after: Runnable?) {
        send<TdApi.Ok?>(SetPinnedForumTopics(chatId, forumTopicIds), typedOkHandler(after))
    }

    fun toggleGeneralForumTopicIsHidden(chatId: Long, isHidden: Boolean, after: Runnable?) {
        send<TdApi.Ok?>(ToggleGeneralForumTopicIsHidden(chatId, isHidden), typedOkHandler(after))
    }

    fun setForumTopicNotificationSettings(chatId: Long, forumTopicId: Int, notificationSettings: ChatNotificationSettings?, after: Runnable?) {
        send<TdApi.Ok?>(SetForumTopicNotificationSettings(chatId, forumTopicId, notificationSettings), typedOkHandler(after))
    }

    fun chat(chatId: Long): Chat? {
        if (chatId == 0L) {
            return null
        }
        val chat: Chat?
        synchronized(dataLock) {
            chat = chats.get(chatId)
        }
        return chat
    }

    fun chatStrict(chatId: Long): Chat {
        val chat: Chat
        synchronized(dataLock) {
            chat = chats.get(chatId)!!
            checkNotNull(chat) { "updateChat not received for id:" + chatId }
        }
        return chat
    }

    fun chat(chatId: Long, callback: RunnableData<Chat?>) {
        runOnTdlibThread(Runnable {
            val chat = chat(chatId)
            if (chat != null) {
                callback.runWithData(chat)
            } else {
                client().send(
                    GetChat(chatId),
                    Client.ResultHandler { result: TdApi.Object? -> callback.runWithData(if (result!!.getConstructor() == Chat.CONSTRUCTOR) chat(chatId) else null) })
            }
        })
    }

    fun chat(chatId: Long, createFunction: Future<TdApi.Function<*>?>?, callback: RunnableData<Chat?>) {
        if (createFunction == null) {
            chat(chatId, callback)
        } else {
            chat(chatId, RunnableData { chat: Chat? ->
                if (chat != null) {
                    callback.runWithData(chat)
                } else {
                    client().send(createFunction.getValue(), Client.ResultHandler { ignored: TdApi.Object? ->
                        val createdChat = chat(chatId)
                        callback.runWithData(createdChat)
                    })
                }
            })
        }
    }

    fun syncChat(chat: Chat): Chat? {
        return if (knownChatIds.contains(chat.id)) chat else chatSync(chat.id)
    }

    @JvmOverloads
    fun chatSync(chatId: Long, timeoutMs: Long = TimeUnit.SECONDS.toMillis(5)): Chat? {
        if (chatId == 0L) return null
        val chat = if (knownChatIds.contains(chatId)) chat(chatId) else null
        if (chat != null) return chat
        val result = clientExecute(GetChat(chatId), timeoutMs)
        if (result != null) {
            when (result.getConstructor()) {
                Chat.CONSTRUCTOR -> return if (knownChatIds.contains(chatId)) chat(chatId) else null
                TdApi.Error.CONSTRUCTOR -> {
                    Log.e("chatSync failed: %s, chatId:%d", TD.toErrorString(result), chatId)
                    return null
                }
            }
        }
        return null
    }

    fun chatList(chatList: ChatList): TdlibChatList {
        synchronized(dataLock) {
            return chatListImpl(chatList)
        }
    }

    private fun chatListImpl(chatList: ChatList): TdlibChatList {
        val key = TD.makeChatListKey(chatList)
        var list: TdlibChatList?
        synchronized(chatLists) {
            list = chatLists.get(key)
            if (list == null) {
                list = TdlibChatList(this, chatList)
                chatLists.put(key, list)
            }
        }
        return list!!
    }

    private fun chatListsImpl(positions: Array<TdApi.ChatPosition>?): Array<TdlibChatList?>? {
        if (positions == null || positions.size == 0) {
            return null
        }
        val chatLists = arrayOfNulls<TdlibChatList>(positions.size)
        for (i in positions.indices) {
            chatLists[i] = chatListImpl(positions[i].list)
        }
        return chatLists
    }

    fun chats(chatIds: LongArray): MutableList<Chat?> {
        val result = ArrayList<Chat?>(chatIds.size)
        synchronized(dataLock) {
            for (chatId in chatIds) {
                val chat = chats.get(chatId)
                if (TdlibUtils.assertChat(chatId, chat)) continue
                result.add(chat)
            }
        }
        return result
    }

    fun chatUsers(chatIds: LongArray): MutableList<TdApi.User?> {
        val result = ArrayList<TdApi.User?>(chatIds.size)
        synchronized(dataLock) {
            for (chatId in chatIds) {
                val user = chatUser(chatId)
                if (user != null) result.add(user)
            }
        }
        return result
    }

    fun stickerSet(name: String?, callback: RunnableData<TdApi.StickerSet?>) {
        if (isEmpty(name)) {
            callback.runWithData(null)
        } else {
            client().send(SearchStickerSet(name, false), Client.ResultHandler { result: TdApi.Object? ->
                when (result!!.getConstructor()) {
                    TdApi.StickerSet.CONSTRUCTOR -> callback.runWithData(result as TdApi.StickerSet)
                    TdApi.Error.CONSTRUCTOR -> {
                        Log.e(
                            "Failed to find animated emoji sticker set: %s, %s, isDebugInstance: %b", name, TD.toErrorString(result),
                            this.isDebugInstance
                        )
                        callback.runWithData(null)
                    }
                }
            })
        }
    }

    fun isAdminOrOwner(chatId: Long): Boolean {
        val status = chatStatus(chatId)
        return status != null && TD.isAdmin(status)
    }

    fun isAnonymousAdmin(chatId: Long): Boolean {
        val status = chatStatus(chatId)
        return status != null && status.isAnonymous()
    }

    fun isAnonymousAdminNonCreator(chatId: Long): Boolean {
        val status = chatStatus(chatId)
        val supergroup = chatToSupergroup(chatId)
        return status != null && status.isAnonymous() && !TD.isCreator(status) && !(supergroup != null && supergroup.isAdministeredDirectMessagesGroup)
    }

    fun chatStatus(chatId: Long): ChatMemberStatus? {
        if (chatId == 0L) {
            return null
        }
        when (getType(chatId)) {
            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> return null
            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroupId = toSupergroupId(chatId)
                val supergroup = cache().supergroup(supergroupId)
                return if (supergroup != null) supergroup.status else null
            }

            ChatTypeBasicGroup.CONSTRUCTOR -> {
                val basicGroupId = toBasicGroupId(chatId)
                val basicGroup = cache().basicGroup(basicGroupId)
                return if (basicGroup != null) basicGroup.status else null
            }
        }
        throw RuntimeException()
    }

    fun chatType(chatId: Long): ChatType? {
        if (chatId == 0L) {
            return null
        }
        when (getType(chatId)) {
            ChatTypePrivate.CONSTRUCTOR -> return ChatTypePrivate(toUserId(chatId))
            ChatTypeBasicGroup.CONSTRUCTOR -> return ChatTypeBasicGroup(toBasicGroupId(chatId))
            ChatTypeSecret.CONSTRUCTOR -> {
                val secretChatId = toSecretChatId(chatId)
                val secretChat = cache().secretChat(secretChatId)
                return ChatTypeSecret(secretChatId, if (secretChat != null) secretChat.userId else 0)
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroupId = toSupergroupId(chatId)
                val supergroup = cache().supergroup(supergroupId)
                return ChatTypeSupergroup(supergroupId, supergroup != null && supergroup.isChannel)
            }
        }
        throw IllegalArgumentException("chatId == " + chatId)
    }

    fun chatSettings(chatId: Long): ChatNotificationSettings? {
        val chat = chat(chatId)
        return if (chat != null) chat.notificationSettings else null
    }

    fun chatPermissions(chatId: Long): ChatPermissions? {
        val chat = chat(chatId)
        return if (chat != null) chat.permissions else null
    }

    // Data Utils
    fun objectToChat(`object`: TdApi.Object): Chat? {
        return chat((`object` as Chat).id)
    }

    fun isSelfChat(chatId: Long): Boolean {
        return isSelfUserId(toUserId(chatId))
    }

    fun isSelfChat(chat: Chat?): Boolean {
        return chat != null && isSelfChat(chat.id)
    }

    fun isSelfUserId(userId: Long): Boolean {
        return userId != 0L && userId == myUserId(true)
    }

    fun selfChatId(): Long {
        return fromUserId(myUserId())
    }

    fun selfChat(): Chat? {
        return chat(selfChatId())
    }

    fun canClearHistory(chatId: Long): Boolean {
        return chatId != 0L && canClearHistory(chat(chatId))
    }

    fun canClearHistory(chat: Chat?): Boolean {
        return chat != null && chat.lastMessage != null && (chat.canBeDeletedOnlyForSelf || chat.canBeDeletedForAllUsers)
    }

    fun canClearHistoryForAllUsers(chatId: Long): Boolean {
        return chatId != 0L && canClearHistoryForAllUsers(chat(chatId))
    }

    fun canClearHistoryForAllUsers(chat: Chat?): Boolean {
        return chat != null && chat.lastMessage != null && chat.canBeDeletedForAllUsers
    }

    fun canClearHistoryOnlyForSelf(chatId: Long): Boolean {
        return chatId != 0L && canClearHistoryOnlyForSelf(chat(chatId))
    }

    fun canClearHistoryOnlyForSelf(chat: Chat?): Boolean {
        return chat != null && chat.lastMessage != null && chat.canBeDeletedOnlyForSelf
    }

    fun canAddToOtherChat(chat: Chat): Boolean {
        val user = chatUser(chat)
        if (user == null) {
            return false
        }
        when (user.type.getConstructor()) {
            TdApi.UserTypeRegular.CONSTRUCTOR -> return true
            UserTypeBot.CONSTRUCTOR -> return (user.type as UserTypeBot).canJoinGroups
        }
        return false
    }

    fun chatToSecretChat(chatId: Long): SecretChat? {
        val secretChatId = toSecretChatId(chatId)
        return if (secretChatId != 0) cache().secretChat(secretChatId) else null
    }

    fun chatToBasicGroup(chatId: Long): BasicGroup? {
        val basicGroupId = toBasicGroupId(chatId)
        return if (basicGroupId != 0L) cache().basicGroup(basicGroupId) else null
    }

    fun chatToSupergroup(chatId: Long): Supergroup? {
        val supergroupId = toSupergroupId(chatId)
        return if (supergroupId != 0L) cache().supergroup(supergroupId) else null
    }

    @JvmOverloads
    fun chatAvatar(chatId: Long, @Px size: Int = ChatView.getDefaultAvatarCacheSize()): ImageFile? {
        if (chatId == 0L) return null
        val chat = chat(chatId)
        val photo = if (chat != null) chat.photo else null
        if (photo == null) return null
        val avatarFile = ImageFile(this, photo.small)
        avatarFile.setSize(size)
        return avatarFile
    }

    fun chatPlaceholder(chat: Chat?, allowSavedMessages: Boolean, radius: Float, provider: DrawableProvider?): AvatarPlaceholder {
        return AvatarPlaceholder(radius, chatPlaceholderMetadata(chat, allowSavedMessages), provider)
    }

    fun chatPlaceholder(chatId: Long, chat: Chat?, allowSavedMessages: Boolean, radius: Float, provider: DrawableProvider?): AvatarPlaceholder {
        return AvatarPlaceholder(radius, chatPlaceholderMetadata(chatId, chat, allowSavedMessages), provider)
    }

    fun chatPlaceholderMetadata(chatId: Long, allowSavedMessages: Boolean): AvatarPlaceholder.Metadata? {
        return chatPlaceholderMetadata(chatId, chat(chatId), allowSavedMessages)
    }

    fun chatPlaceholderMetadata(chatId: Long, chat: Chat?, allowSavedMessages: Boolean): AvatarPlaceholder.Metadata? {
        if (chat != null || chatId == 0L) {
            return chatPlaceholderMetadata(chat, allowSavedMessages)
        } else if (allowSavedMessages && isSelfChat(chatId)) {
            return AvatarPlaceholder.Metadata(accentColor(TdlibAccentColor.InternalId.SAVED_MESSAGES))
        } else if (isRepliesChat(chatId)) {
            return AvatarPlaceholder.Metadata(accentColor(TdlibAccentColor.InternalId.REPLIES))
        } else if (isDeletedAccountChat(chatId)) {
            return AvatarPlaceholder.Metadata(accentColor(TdlibAccentColor.InternalId.INACTIVE))
        } else {
            return AvatarPlaceholder.Metadata(chatAccentColor(chatId))
        }
    }

    fun chatPlaceholderMetadata(chat: Chat?, allowSavedMessages: Boolean): AvatarPlaceholder.Metadata? {
        if (chat == null) {
            return null
        }
        val accentColor: TdlibAccentColor
        val avatarLetters: Letters?
        val desiredDrawableRes = 0
        var extraDrawableRes = 0
        if (isUserChat(chat)) {
            val userId = chatUserId(chat)
            return cache().userPlaceholderMetadata(userId, cache().user(userId), allowSavedMessages)
        } else {
            accentColor = chatAccentColor(chat)
            avatarLetters = chatLetters(chat)
            when (chat.type.getConstructor()) {
                ChatTypeBasicGroup.CONSTRUCTOR -> extraDrawableRes =
                    if (canChangeInfo(chat)) R.drawable.baseline_add_a_photo_56 else R.drawable.baseline_group_56

                ChatTypeSupergroup.CONSTRUCTOR -> extraDrawableRes =
                    if (canChangeInfo(chat)) R.drawable.baseline_add_a_photo_56 else if (isChannelChat(chat)) R.drawable.baseline_bullhorn_56 else R.drawable.baseline_group_56
            }
        }
        return AvatarPlaceholder.Metadata(accentColor, avatarLetters, desiredDrawableRes, extraDrawableRes)
    }

    fun chatLetters(chat: Chat?): Letters {
        if (chat != null) {
            when (chat.type.getConstructor()) {
                ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                    return TD.getLetters(chatUser(chat))
                }

                ChatTypeBasicGroup.CONSTRUCTOR, ChatTypeSupergroup.CONSTRUCTOR -> {
                    return TD.getLetters(chatTitle(chat))
                }
            }
        }
        return TD.getLetters()
    }

    fun chatLetters(chatId: Long): Letters {
        if (chatId != 0L) {
            when (getType(chatId)) {
                ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                    return TD.getLetters(chatUser(chatUserId(chatId)))
                }

                ChatTypeBasicGroup.CONSTRUCTOR, ChatTypeSupergroup.CONSTRUCTOR -> {
                    return TD.getLetters(chatTitle(chatId))
                }
            }
        }
        return TD.getLetters()
    }

    fun chatAccentColorId(chatId: Long): Int {
        if (chatId == 0L) {
            return TdlibAccentColor.InternalId.INACTIVE
        }
        if (isRepliesChat(chatId)) {
            return TdlibAccentColor.InternalId.REPLIES
        }
        when (getType(chatId)) {
            ChatTypeBasicGroup.CONSTRUCTOR, ChatTypeSupergroup.CONSTRUCTOR -> {
                val chat = chat(chatId)
                if (chat != null) {
                    return chat.accentColorId
                } else {
                    return TdlibAccentColor.InternalId.INACTIVE
                }
            }

            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                val userId = chatUserId(chatId)
                return cache().userAccentColorId(userId)
            }

            else -> {
                assertChatType_e562ec7d()
                throw UnsupportedOperationException(chatId.toString())
            }
        }
    }

    fun chatAccentColorId(chat: Chat?): Int {
        if (chat == null) {
            return TdlibAccentColor.InternalId.INACTIVE
        }
        if (isRepliesChat(chat.id)) {
            return TdlibAccentColor.InternalId.REPLIES
        }
        when (chat.type.getConstructor()) {
            ChatTypeBasicGroup.CONSTRUCTOR, ChatTypeSupergroup.CONSTRUCTOR -> {
                return chat.accentColorId
            }

            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                val userId = chatUserId(chat)
                return cache().userAccentColorId(userId)
            }

            else -> {
                assertChatType_e562ec7d()
                throw unsupported(chat.type)
            }
        }
    }

    fun messageAccentColor(message: TdApi.Message): TdlibAccentColor? {
        if (message.forwardInfo != null) {
            when (message.forwardInfo!!.origin.getConstructor()) {
                MessageOriginChat.CONSTRUCTOR -> return chatAccentColor((message.forwardInfo!!.origin as MessageOriginChat).senderChatId)
                MessageOriginUser.CONSTRUCTOR -> return cache().userAccentColor((message.forwardInfo!!.origin as MessageOriginUser).senderUserId)
                MessageOriginChannel.CONSTRUCTOR -> return chatAccentColor((message.forwardInfo!!.origin as MessageOriginChannel).chatId)
                MessageOriginHiddenUser.CONSTRUCTOR -> return null
                else -> {
                    assertMessageOrigin_f2224a59()
                    throw unsupported(message.forwardInfo!!.origin)
                }
            }
        }
        return senderAccentColor(message.senderId)
    }

    fun chatAccentColor(chatId: Long): TdlibAccentColor {
        val accentColorId = chatAccentColorId(chatId)
        return accentColor(accentColorId)
    }

    fun chatAccentColor(chat: Chat?): TdlibAccentColor {
        val accentColorId = chatAccentColorId(chat)
        return accentColor(accentColorId)
    }

    fun senderAccentColor(sender: MessageSender): TdlibAccentColor? {
        when (sender.getConstructor()) {
            MessageSenderUser.CONSTRUCTOR -> {
                val user = sender as MessageSenderUser
                return cache().userAccentColor(user.userId)
            }

            MessageSenderChat.CONSTRUCTOR -> {
                val chat = sender as MessageSenderChat
                return chatAccentColor(chat.chatId)
            }

            else -> {
                assertMessageSender_439d4c9c()
                throw unsupported(sender)
            }
        }
    }

    fun messageAuthor(message: TdApi.Message?): String? {
        return messageAuthor(message, true, false)
    }

    fun messageAuthorUsername(message: TdApi.Message?): String? {
        val chatId = message.getMessageAuthorId()
        if (chatId != 0L) {
            if (isPrivate(chatId)) {
                return cache().userUsername(toUserId(chatId))
            } else {
                return chatUsername(chatId)
            }
        }
        return null
    }

    fun messageAuthor(message: TdApi.Message?, allowSignature: Boolean, shorten: Boolean): String? {
        if (message == null) return null
        if (message.forwardInfo != null) {
            when (message.forwardInfo!!.origin.getConstructor()) {
                MessageOriginUser.CONSTRUCTOR -> {
                    val userId = (message.forwardInfo!!.origin as MessageOriginUser).senderUserId
                    return if (shorten) cache().userFirstName(userId) else cache().userName(userId)
                }

                MessageOriginChannel.CONSTRUCTOR -> {
                    val info = message.forwardInfo!!.origin as MessageOriginChannel
                    if (allowSignature && !isEmpty(info.authorSignature)) return info.authorSignature
                    val chat = chat(info.chatId)
                    if (chat != null) return chat.title
                }

                MessageOriginChat.CONSTRUCTOR -> {
                    val info = message.forwardInfo!!.origin as MessageOriginChat
                    if (allowSignature && !isEmpty(info.authorSignature)) return info.authorSignature
                    val chat = chat(info.senderChatId)
                    if (chat != null) return chat.title
                }

                MessageOriginHiddenUser.CONSTRUCTOR -> {}
                else -> {
                    assertMessageOrigin_f2224a59()
                    throw unsupported(message.forwardInfo!!.origin)
                }
            }
        }
        if (message.senderId == null) return null
        return senderName(message.senderId, shorten)
    }

    fun chatTitle(chatId: Long): String? {
        return chatTitle(chatId, true, false)
    }

    fun chatTitleShort(chatId: Long): String? {
        return chatTitle(chatId, true, true)
    }

    fun chatTitle(chatId: Long, allowSavedMessages: Boolean, shorten: Boolean): String? {
        val chat = chat(chatId)
        if (chat != null) {
            return chatTitle(chat, allowSavedMessages, shorten)
        }
        if (chatId == CLOUD_RESOURCES_CHAT_ID && "." == chat!!.title) {
            return Lang.getString(R.string.EmojiSets)
        }
        val userId = chatUserId(chatId)
        return if (userId != 0L) cache().userDisplayName(userId, allowSavedMessages, shorten) else null
    }

    fun canReportMessage(message: TdApi.Message?): Boolean {
        return message != null && !isSelfChat(message.chatId) && !message.isOutgoing && message.sendingState == null && canReportChatSpam(message.chatId)
    }

    fun canReportChatSpam(chatId: Long): Boolean {
        return canReportChatSpam(chat(chatId))
    }

    fun canReportChatSpam(chat: Chat?): Boolean {
        return chat != null && chat.canBeReported
    }

    @JvmOverloads
    fun chatTitle(chat: Chat, allowSavedMessages: Boolean = true, shorten: Boolean = false): String? {
        if (chat.id == CLOUD_RESOURCES_CHAT_ID && "." == chat.title) {
            return Lang.getString(R.string.EmojiSets)
        }
        val userId = chatUserId(chat.id)
        return if (userId != 0L) cache().userDisplayName(userId, allowSavedMessages, shorten) else chat.title
    }

    fun chatUserId(chat: Chat): Long {
        when (chat.type.getConstructor()) {
            ChatTypePrivate.CONSTRUCTOR -> return (chat.type as ChatTypePrivate).userId
            ChatTypeSecret.CONSTRUCTOR -> return (chat.type as ChatTypeSecret).userId
        }
        return 0
    }

    fun chatUser(chat: Chat): TdApi.User? {
        val userId = chatUserId(chat)
        return if (userId != 0L) cache().user(userId) else null
    }

    fun chatUserId(chatId: Long): Long {
        if (isPrivate(chatId)) {
            return toUserId(chatId)
        } else if (isSecret(chatId)) {
            val secretChatId = toSecretChatId(chatId)
            val secretChat = cache().secretChat(secretChatId)
            if (secretChat != null) {
                return secretChat.userId
            }
        }
        return 0
    }

    @JvmOverloads
    fun chatPhoto(chatId: Long, allowRequest: Boolean = true): ChatPhoto? {
        if (chatId == 0L) {
            return null
        }
        when (getType(chatId)) {
            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                val userId = chatUserId(chatId)
                val userFullInfo = if (userId != 0L) cache().userFull(userId, allowRequest) else null
                return if (userFullInfo != null) userFullInfo.photo else null
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroupId = toSupergroupId(chatId)
                val supergroupFullInfo = if (supergroupId != 0L) cache().supergroupFull(supergroupId, allowRequest) else null
                return if (supergroupFullInfo != null) supergroupFullInfo.photo else null
            }

            ChatTypeBasicGroup.CONSTRUCTOR -> {
                val basicGroupId = toBasicGroupId(chatId)
                val basicGroupFullInfo = if (basicGroupId != 0L) cache().basicGroupFull(basicGroupId, allowRequest) else null
                return if (basicGroupFullInfo != null) basicGroupFullInfo.photo else null
            }

            else -> {
                assertChatType_e562ec7d()
                throw UnsupportedOperationException(chatId.toString())
            }
        }
    }

    fun sender(chatId: Long): MessageSender {
        val userId = chatUserId(chatId)
        return if (userId != 0L) MessageSenderUser(userId) else MessageSenderChat(chatId)
    }

    fun isSelfSender(sender: MessageSender?): Boolean {
        return sender != null && isSelfUserId(sender.getSenderUserId())
    }

    fun isSelfSender(message: TdApi.Message?): Boolean {
        return message != null && (message.isOutgoing || isSelfSender(message.senderId))
    }

    fun senderContactOrCloseFirend(sender: MessageSender?): Boolean {
        val userId = sender.getSenderUserId()
        val user = if (userId != 0L) cache().user(userId) else null
        return user != null && (user.isContact || user.isCloseFriend)
    }

    fun chatUser(chatId: Long): TdApi.User? {
        val userId = chatUserId(chatId)
        if (userId != 0L) {
            return cache().user(userId)
        } else {
            return null
        }
    }

    fun chatUserDeleted(chat: Chat): Boolean {
        val user = chatUser(chat)
        return user != null && user.type.getConstructor() == TdApi.UserTypeDeleted.CONSTRUCTOR
    }

    fun isForum(chatId: Long): Boolean {
        val supergroup = chatToSupergroup(chatId)
        return supergroup != null && supergroup.isForum
    }

    fun chatBlockList(chat: Chat?): TdApi.BlockList? {
        return if (chat != null) chatBlockList(chat.id) else null
    }

    fun chatBlockList(chatId: Long): TdApi.BlockList? {
        val chat = chat(chatId)
        return if (chat != null) chat.blockList else null
    }

    fun chatFullyBlocked(chatId: Long): Boolean {
        val blockList = chatBlockList(chatId)
        return blockList != null && blockList.getConstructor() == TdApi.BlockListMain.CONSTRUCTOR
    }

    fun chatUsername(chat: Chat?): String? {
        val usernames = chatUsernames(chat)
        return usernames.primaryUsername()
    }

    fun chatUsernames(chat: Chat?): Usernames? {
        if (chat == null) {
            return null
        }
        when (chat.type.getConstructor()) {
            ChatTypeBasicGroup.CONSTRUCTOR -> {
                return null
            }

            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                val userId = chatUserId(chat)
                return cache().userUsernames(userId)
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroupId = (chat.type as ChatTypeSupergroup).supergroupId
                return cache().supergroupUsernames(supergroupId)
            }

            else -> {
                assertChatType_e562ec7d()
                throw unsupported(chat.type)
            }
        }
    }

    fun chatUsernames(chatId: Long): Usernames? {
        if (chatId == 0L) {
            return null
        }
        when (getType(chatId)) {
            ChatTypeBasicGroup.CONSTRUCTOR -> {
                return null
            }

            ChatTypePrivate.CONSTRUCTOR -> {
                val userId = toUserId(chatId)
                return cache().userUsernames(userId)
            }

            ChatTypeSecret.CONSTRUCTOR -> {
                val secretChatId = toSecretChatId(chatId)
                val secretChat = cache().secretChat(secretChatId)
                if (secretChat != null) {
                    return cache().userUsernames(secretChat.userId)
                }
                return null
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroupId = toSupergroupId(chatId)
                return cache().supergroupUsernames(supergroupId)
            }

            else -> {
                assertChatType_e562ec7d()
                throw UnsupportedOperationException(chatId.toString())
            }
        }
    }

    fun chatUsername(chatId: Long): String? {
        val usernames = chatUsernames(chatId)
        return usernames.primaryUsername()
    }

    fun chatLocation(chatId: Long): ChatLocation? {
        if (chatId == 0L) {
            return null
        }
        if (getType(chatId) == ChatTypeSupergroup.CONSTRUCTOR) {
            val fullInfo = cache().supergroupFull(toSupergroupId(chatId))
            return if (fullInfo != null) fullInfo.location else null
        }
        return null
    }

    fun chatBasicGroupActive(chatId: Long): Boolean {
        val basicGroupId = toBasicGroupId(chatId)
        return basicGroupId != 0L && cache().basicGroupActive(basicGroupId)
    }

    fun withChannelBotUserId(runnable: RunnableLong) {
        client().send(SearchPublicChat("Channel_Bot"), Client.ResultHandler { result: TdApi.Object? ->
            ui()!!.post(Runnable {
                val userId = if (result!!.getConstructor() == Chat.CONSTRUCTOR) chatUserId(result as Chat) else 0
                runnable.runWithLong(if (userId != 0L) userId else telegramChannelBotUserId())
            })
        })
    }

    fun canCopyPublicMessageLinks(chatId: Long): Boolean {
        if (chatId == 0L) {
            return false
        }
        val supergroupId = toSupergroupId(chatId)
        if (supergroupId == 0L) {
            return false
        }
        val usernames = cache().supergroupUsernames(supergroupId)
        return usernames.hasUsername()
    }

    fun chatPublic(chatId: Long): Boolean {
        if (chatId == 0L) {
            return false
        }
        val supergroupId = toSupergroupId(chatId)
        if (supergroupId == 0L) {
            return false
        }
        val supergroup = cache().supergroup(supergroupId)
        return supergroup != null && (supergroup.hasUsername() || supergroup.hasLocation)
    }

    fun chatSource(chatList: ChatList?, chatId: Long): ChatSource? {
        if (chatId == 0L) return null
        val chat = chat(chatId)
        val position = chat.findPosition(chatList)
        return if (position != null) position.source else null
    }

    fun chatPinned(chatList: ChatList?, chatId: Long): Boolean {
        if (chatId == 0L) {
            return false
        }
        val chat = chat(chatId)
        return chat.isPinned(chatList)
    }

    fun getPinnedChats(chatList: ChatList?): MutableList<Long?> {
        synchronized(dataLock) {
            var pinnedChats: MutableList<Chat>? = null
            for (chat in chats.values) {
                val position = chat.findPosition(chatList)
                if (position != null && position.isPinned) {
                    if (pinnedChats == null) pinnedChats = ArrayList<Chat>()
                    pinnedChats.add(chat)
                }
            }
            if (pinnedChats != null) {
                pinnedChats.sort(chatList)
                val pinnedChatIds: MutableList<Long?> = ArrayList<Long?>(pinnedChats.size)
                for (chat in pinnedChats) {
                    pinnedChatIds.add(chat.id)
                }
                return pinnedChatIds
            }
            return ArrayList<Long?>()
        }
    }

    fun isFolderShareable(chatFolderId: Int): Boolean {
        val chatFolderInfo = chatFolderInfo(chatFolderId)
        return chatFolderInfo != null && chatFolderInfo.isShareable
    }

    fun canAddShareableFolder(): Boolean {
        return shareableChatFolderCount() < addedShareableChatFolderCountMax()
    }

    fun shareableChatFolderCount(): Int {
        synchronized(dataLock) {
            var count = 0
            for (chatFolder in chatFolders!!) {
                if (chatFolder.isShareable) {
                    count++
                }
            }
            return count
        }
    }

    fun addedShareableChatFolderCountMax(): Int {
        return options.addedShareableChatFolderCountMax
    }

    fun canCreateChatFolder(): Boolean {
        return chatFolderCount() < chatFolderCountMax()
    }

    fun chatFolderCount(): Int {
        synchronized(dataLock) {
            return chatFolders!!.size
        }
    }

    fun chatFolders(): Array<ChatFolderInfo>? {
        synchronized(dataLock) {
            return chatFolders
        }
    }

    fun mainChatListPosition(): Int {
        synchronized(dataLock) {
            return mainChatListPosition
        }
    }

    fun chatFolderInfo(chatFolderId: Int): ChatFolderInfo? {
        if (chatFolderId == 0) {
            return null
        }
        synchronized(dataLock) {
            if (chatFolders != null) {
                for (filter in chatFolders) {
                    if (filter.id == chatFolderId) return filter
                }
            }
        }
        return null
    }

    fun hasFolders(): Boolean {
        synchronized(dataLock) {
            return chatFolders != null && chatFolders!!.size > 0
        }
    }

    fun canArchiveOrUnarchiveChat(chat: Chat?): Boolean {
        if (chat == null) return false
        val chatLists = chat.chatLists
        if (chatLists != null) {
            val canBeArchived = !isSelfChat(chat.id) && !isServiceNotificationsChat(chat.id)
            for (chatList in chatLists) {
                when (chatList.getConstructor()) {
                    TdApi.ChatListMain.CONSTRUCTOR -> return canBeArchived
                    TdApi.ChatListArchive.CONSTRUCTOR -> return true
                    TdApi.ChatListFolder.CONSTRUCTOR -> continue
                    else -> {
                        assertChatList_db6c93ab()
                        throw unsupported(chatList)
                    }
                }
            }
        }
        return false
    }

    fun chatArchived(chatId: Long): Boolean {
        return chatId != 0L && chatArchived(chat(chatId))
    }

    fun chatArchived(chat: Chat?): Boolean {
        val chatLists = if (chat != null) chat.chatLists else null
        if (chatLists != null) {
            for (chatList in chatLists) {
                when (chatList.getConstructor()) {
                    TdApi.ChatListMain.CONSTRUCTOR -> return false
                    TdApi.ChatListArchive.CONSTRUCTOR -> return true
                    TdApi.ChatListFolder.CONSTRUCTOR -> continue
                    else -> {
                        assertChatList_db6c93ab()
                        throw unsupported(chatList)
                    }
                }
            }
        }
        return false
    }

    fun chatNeedsMuteIcon(chatId: Long): Boolean {
        return chatNeedsMuteIcon(chat(chatId))
    }

    fun chatNeedsMuteIcon(chat: Chat?): Boolean {
        return chat != null && TD.needMuteIcon(chat.notificationSettings, scopeNotificationSettings(chat.id))
    }

    fun chatNotificationsEnabled(chatId: Long): Boolean {
        return chatNotificationsEnabled(chat(chatId))
    }

    fun chatNotificationsEnabled(chat: Chat?): Boolean {
        if (chat == null) return true
        if (!chat.notificationSettings.useDefaultMuteFor) {
            return chat.notificationSettings.muteFor == 0
        } else {
            return scopeMuteFor(notificationManager.scope(chat)) == 0
        }
    }

    fun scopeMuteFor(scope: NotificationSettingsScope): Int {
        val settings = notificationManager.getScopeNotificationSettings(scope)
        return if (settings != null) settings.muteFor else 0
    }

    fun chatDefaultDisableNotifications(chatId: Long): Boolean {
        if (Config.NEED_SILENT_BROADCAST) {
            val chat = chat(chatId)
            return chat != null && chat.defaultDisableNotification
        }
        return false
    }

    fun chatMuteFor(chatId: Long): Int {
        return chatMuteFor(chat(chatId))
    }

    fun chatMuteFor(chat: Chat?): Int {
        if (chat == null) return 0
        if (chat.notificationSettings.useDefaultMuteFor) {
            val notificationSettings = notificationManager.getScopeNotificationSettings(chat)
            return if (notificationSettings != null) notificationSettings.muteFor else 0
        }
        return chat.notificationSettings.muteFor
    }

    fun chatTTL(chatId: Long): Int {
        val chat = chat(chatId)
        return if (chat != null) chat.messageAutoDeleteTime else 0
    }

    fun chatSupportsRoundVideos(chatId: Long): Boolean {
        if (chatId == 0L) {
            return false
        }
        if (isSecret(chatId)) {
            val secretChat = chatToSecretChat(chatId)
            return secretChat != null && secretChat.layer >= 66
        }
        return true
    }

    fun messageSending(msg: TdApi.Message?): Boolean {
        return msg != null && msg.sendingState != null && !qack().isMessageAcknowledged(msg.chatId, msg.id)
    }

    fun messageBeingEdited(msg: TdApi.Message?): Boolean {
        return msg != null && getPendingFormattedText(msg.chatId, msg.id) != null
    }

    fun albumBeingEdited(album: Album?): Boolean {
        if (album != null) {
            for (message in album.messages) {
                if (messageBeingEdited(message)) {
                    return true
                }
            }
        }
        return false
    }

    fun canCopyPostLink(msg: TdApi.Message?): Boolean {
        return msg != null && msg.sendingState == null && toSupergroupId(msg.chatId) != 0L && !TD.isScheduled(msg)
    }

    fun messageUsernames(msg: TdApi.Message?): Usernames? {
        if (msg != null) {
            val userId = if (msg.viaBotUserId != 0L) msg.viaBotUserId else msg.getSenderUserId()
            if (userId != 0L) {
                val user = cache().user(userId)
                return if (user != null) user.usernames else null
            }
        }
        return null
    }

    fun messageUsername(msg: TdApi.Message?): String? {
        val usernames = messageUsernames(msg)
        return usernames.primaryUsername()
    }

    fun isSameSender(a: TdApi.Message, b: TdApi.Message): Boolean {
        val psa1 = if (a.forwardInfo != null) a.forwardInfo!!.publicServiceAnnouncementType else null
        val psa2 = if (b.forwardInfo != null) b.forwardInfo!!.publicServiceAnnouncementType else null
        return a.senderId.equalsTo(b.senderId) && psa1.equalsOrBothEmpty(psa2)
    }

    fun senderUserId(msg: TdApi.Message?): Long {
        if (msg == null) {
            return 0
        }
        if (isSelfChat(msg.chatId)) {
            if (msg.forwardInfo != null && msg.forwardInfo!!.origin.getConstructor() == MessageOriginUser.CONSTRUCTOR) return (msg.forwardInfo!!.origin as MessageOriginUser).senderUserId
        }
        return msg.getSenderUserId()
    }

    fun senderUserId(senderId: MessageSender): Long {
        when (senderId.getConstructor()) {
            MessageSenderChat.CONSTRUCTOR -> {
                val chatId = (senderId as MessageSenderChat).chatId
                return chatUserId(chatId)
            }

            MessageSenderUser.CONSTRUCTOR -> {
                return (senderId as MessageSenderUser).userId
            }

            else -> {
                assertMessageSender_439d4c9c()
                throw unsupported(senderId)
            }
        }
    }

    fun senderUser(senderId: MessageSender): TdApi.User? {
        val userId = senderUserId(senderId)
        return if (userId != 0L) cache().user(userId) else null
    }

    fun senderName(msg: TdApi.Message, allowForward: Boolean, shorten: Boolean): String? {
        var authorId = msg.getMessageAuthorId(allowForward)
        if (authorId == 0L && allowForward) {
            if (msg.forwardInfo != null) {
                if (msg.forwardInfo!!.origin.getConstructor() == MessageOriginHiddenUser.CONSTRUCTOR) {
                    return (msg.forwardInfo!!.origin as MessageOriginHiddenUser).senderName
                } else {
                    authorId = msg.getMessageAuthorId(false)
                }
            }
            if (msg.importInfo != null && authorId == 0L) {
                return msg.importInfo!!.senderName
            }
        }
        if (isUserChat(authorId)) {
            val userId = chatUserId(authorId)
            return if (shorten) cache().userFirstName(userId) else cache().userName(userId)
        } else if (authorId == msg.chatId && !isEmpty(msg.authorSignature)) {
            return if (shorten) msg.authorSignature else Lang.getString(
                if (isChannel(msg.chatId)) R.string.format_channelAndSignature else R.string.format_chatAndSignature,
                chatTitle(authorId),
                msg.authorSignature
            )
        } else {
            return chatTitle(authorId)
        }
    }

    @JvmOverloads
    fun senderName(sender: MessageSender, shorten: Boolean = false): String? {
        when (sender.getConstructor()) {
            MessageSenderChat.CONSTRUCTOR -> {
                val chatId = (sender as MessageSenderChat).chatId
                return chatTitle(chatId)
            }

            MessageSenderUser.CONSTRUCTOR -> {
                val userId = (sender as MessageSenderUser).userId
                return if (shorten) cache().userFirstName(userId) else cache().userName(userId)
            }
        }
        throw RuntimeException(sender.toString())
    }

    fun needUserAvatarPreviewAnimation(userId: Long): Boolean {
        if (userId != 0L) {
            val user = cache().user(userId)
            return user != null && user.isPremium
        }
        return false
    }

    fun needAvatarPreviewAnimation(chatId: Long): Boolean {
        if (chatId != 0L) {
            val userId = chatUserId(chatId)
            return needUserAvatarPreviewAnimation(chatUserId(chatId))
        }
        return false
    }

    fun needAvatarPreviewAnimation(sender: MessageSender?): Boolean {
        return senderPremium(sender)
    }

    fun senderPremium(sender: MessageSender?): Boolean {
        if (sender == null) {
            return false
        }
        val userId: Long
        when (sender.getConstructor()) {
            MessageSenderChat.CONSTRUCTOR -> userId = chatUserId((sender as MessageSenderChat).chatId)
            MessageSenderUser.CONSTRUCTOR -> userId = (sender as MessageSenderUser).userId
            else -> {
                assertMessageSender_439d4c9c()
                throw unsupported(sender)
            }
        }
        val user = cache().user(userId)
        return user != null && user.isPremium
    }

    fun senderUsername(sender: MessageSender): String? {
        when (sender.getConstructor()) {
            MessageSenderChat.CONSTRUCTOR -> {
                return chatUsername((sender as MessageSenderChat).chatId)
            }

            MessageSenderUser.CONSTRUCTOR -> {
                return cache().userUsername((sender as MessageSenderUser).userId)
            }
        }
        throw IllegalArgumentException(sender.toString())
    }

    fun isFromAnonymousGroupAdmin(message: TdApi.Message?): Boolean {
        return message != null && message.chatId == message.getSenderId() && isMultiChat(message.chatId)
    }

    fun isContactChat(chat: Chat?): Boolean {
        if (chat != null) {
            val user = chatUser(chat)
            return TD.isContact(user) && !isSelfUserId(user!!.id)
        }
        return false
    }

    fun markChatAsRead(chatId: Long, source: TdApi.MessageSource, allowRemoveMarkedAsUnrad: Boolean, after: Runnable?) {
        val chat = chat(chatId)
        if (chat != null) {
            if (chat.isMarkedAsUnread && allowRemoveMarkedAsUnrad) {
                when (source.getConstructor()) {
                    TdApi.MessageSourceChatHistory.CONSTRUCTOR, TdApi.MessageSourceHistoryPreview.CONSTRUCTOR, MessageSourceChatList.CONSTRUCTOR, TdApi.MessageSourceSearch.CONSTRUCTOR, TdApi.MessageSourceChatEventLog.CONSTRUCTOR, TdApi.MessageSourceNotification.CONSTRUCTOR, TdApi.MessageSourceOther.CONSTRUCTOR -> client().send(
                        ToggleChatIsMarkedAsUnread(chat.id, false),
                        okHandler(after)
                    )

                    TdApi.MessageSourceMessageThreadHistory.CONSTRUCTOR, TdApi.MessageSourceForumTopicHistory.CONSTRUCTOR -> {}
                }
            }
            if (!hasPasscode(chat) && chat.lastMessage != null) {
                client().send(ViewMessages(chatId, longArrayOf(chat.lastMessage!!.id), source, true), okHandler(after))
            }
        }
    }

    fun markChatAsUnread(chat: Chat?, after: Runnable?) {
        if (chat != null && chat.unreadCount == 0) {
            if (!chat.isMarkedAsUnread) {
                client().send(ToggleChatIsMarkedAsUnread(chat.id, true), okHandler(after))
            }
        }
    }

    class MessageLink(@JvmField val url: String, @JvmField val isPublic: Boolean) {
        override fun toString(): String {
            return url
        }
    }

    fun getMessageLink(message: TdApi.Message, forAlbum: Boolean, forComment: Boolean, after: RunnableData<MessageLink?>) {
        val signal = AtomicBoolean(false)
        val fallback: CancellableRunnable?
        if (message.sendingState == null && !isUserChat(message.chatId) && toServerMessageId(message.id) != 0L && !forComment) {
            fallback = object : CancellableRunnable() {
                public override fun act() {
                    synchronized(signal) {
                        if (signal.get()) return
                        var fallbackUrl: String?
                        val fallbackPrivate: Boolean
                        val username = chatUsername(message.chatId)
                        if (!isEmpty(username)) {
                            fallbackPrivate = false
                            fallbackUrl = tMeMessageUrl(username, toServerMessageId(message.id))
                            if (!forAlbum && message.mediaAlbumId != 0L) fallbackUrl += "?single"
                        } else {
                            fallbackPrivate = true
                            val supergroupId = toSupergroupId(message.chatId)
                            if (supergroupId != 0L) fallbackUrl = tMePrivateMessageUrl(supergroupId, toServerMessageId(message.id))
                            else fallbackUrl = null
                        }
                        if (!isEmpty(fallbackUrl) && !signal.getAndSet(true)) {
                            after.runWithData(Tdlib.MessageLink(fallbackUrl!!, fallbackPrivate))
                        }
                    }
                }
            }
        } else {
            fallback = null
        }
        send<TdApi.MessageLink?>(
            GetMessageLink(message.chatId, message.id, 0, 0, "", forAlbum, forComment),
            Tdlib.ResultHandler { messageLink: TdApi.MessageLink?, error: TdApi.Error? ->
                if (messageLink != null) {
                    ui()!!.post(Runnable {
                        synchronized(signal) {
                            if (!signal.getAndSet(true)) {
                                if (fallback != null) fallback.cancel()
                                after.runWithData(MessageLink(messageLink.link, messageLink.isPublic))
                            }
                        }
                    })
                } else {
                    Log.e("Could not fetch message link: %s", TD.toErrorString(error))
                    if (fallback != null) {
                        ui()!!.post(fallback)
                    }
                }
            })
        if (fallback != null) {
            ui()!!.postDelayed(fallback, 500L)
        }
    }

    fun canMarkAsRead(chat: Chat?): Boolean {
        return chat != null && (chat.unreadCount > 0 || chat.isMarkedAsUnread)
    }

    fun isMultiChat(chatId: Long): Boolean {
        when (getType(chatId)) {
            ChatTypeBasicGroup.CONSTRUCTOR -> return true
            ChatTypeSupergroup.CONSTRUCTOR -> return isSupergroup(chatId)
        }
        return false
    }


    fun isMultiChat(chat: Chat?): Boolean {
        if (chat != null) {
            when (chat.type.getConstructor()) {
                ChatTypeBasicGroup.CONSTRUCTOR -> return true
                ChatTypeSupergroup.CONSTRUCTOR -> return !(chat.type as ChatTypeSupergroup).isChannel
            }
        }
        return false
    }

    fun isChannelFast(chatId: Long): Boolean {
        val supergroupId = toSupergroupId(chatId)
        return supergroupId != 0L && channels.contains(supergroupId)
    }

    fun isChannel(senderId: MessageSender?): Boolean {
        val chatId = senderId.getSenderId()
        return chatId != 0L && isChannel(chatId)
    }

    fun isUser(senderId: MessageSender?): Boolean {
        val chatId = senderId.getSenderId()
        return chatUserId(chatId) != 0L
    }

    fun isChannel(source: ForwardSource?): Boolean {
        return source != null && source.chatId != 0L && isChannel(source.chatId)
    }

    fun isChannel(chatId: Long): Boolean {
        val supergroup = chatToSupergroup(chatId)
        return supergroup != null && supergroup.isChannel
    }

    fun isSupergroup(chatId: Long): Boolean {
        val supergroup = chatToSupergroup(chatId)
        return supergroup != null && !supergroup.isChannel
    }

    fun isChannelAutoForward(message: TdApi.Message?): Boolean {
        return message != null && message.chatId != 0L && message.forwardInfo != null &&
                message.forwardInfo.hasMessageSource() && message.forwardInfo!!.source!!.chatId != message.chatId && message.senderId.getConstructor() == MessageSenderChat.CONSTRUCTOR && (message.senderId as MessageSenderChat).chatId == message.forwardInfo!!.source!!.chatId &&
                isSupergroup(message.chatId) && isChannel(message.forwardInfo!!.source!!.chatId)
    }

    fun canDisablePinnedMessageNotifications(chatId: Long): Boolean {
        return !isUserChat(chatId)
    }

    fun canDisableMentions(chatId: Long): Boolean {
        return isMultiChat(chatId)
    }

    fun isSupergroupChat(chat: Chat?): Boolean {
        return chat != null && chat.type.getConstructor() == ChatTypeSupergroup.CONSTRUCTOR && !(chat.type as ChatTypeSupergroup).isChannel
    }

    fun isChannelChat(chat: Chat?): Boolean {
        return chat != null && chat.type.getConstructor() == ChatTypeSupergroup.CONSTRUCTOR && (chat.type as ChatTypeSupergroup).isChannel
    }

    fun isUserChat(chat: Chat?): Boolean {
        if (chat != null) {
            when (chat.type.getConstructor()) {
                ChatTypeSecret.CONSTRUCTOR, ChatTypePrivate.CONSTRUCTOR -> return true
            }
        }
        return false
    }

    fun isUserChat(chatId: Long): Boolean {
        return tgx.td.isUserChat(chatId)
    }

    fun isDirectMessagesChat(chatId: Long): Boolean {
        val supergroup = chatToSupergroup(chatId)
        return supergroup != null && supergroup.isDirectMessagesGroup
    }

    fun hasDirectMessagesChat(chatId: Long): Boolean {
        val supergroup = chatToSupergroup(chatId)
        return supergroup != null && supergroup.hasDirectMessagesGroup
    }

    fun isRepliesChat(chatId: Long): Boolean {
        return (options.repliesBotChatId != 0L && options.repliesBotChatId == chatId) || (chatId == fromUserId(options.repliesBotUserId))
    }

    fun hasMessageThreads(chatId: Long): Boolean {
        return tgx.td.isSupergroup(chatId) && !isMonoforumChat(chatId) && !isChannel(chatId)
    }

    fun isServiceNotificationsChat(chatId: Long): Boolean {
        return (options.telegramServiceNotificationsChatId != 0L && options.telegramServiceNotificationsChatId == chatId) || (chatId == fromUserId(
            TELEGRAM_ACCOUNT_ID
        ))
    }

    fun serviceNotificationsChatId(): Long {
        return if (options.telegramServiceNotificationsChatId != 0L) options.telegramServiceNotificationsChatId else fromUserId(TELEGRAM_ACCOUNT_ID)
    }

    fun isBotFatherChat(chatId: Long): Boolean {
        return chatId == fromUserId(TELEGRAM_BOT_FATHER_ACCOUNT_ID) || TELEGRAM_BOT_FATHER_USERNAME == chatUsername(chatId)
    }

    fun suggestStopBot(chatId: Long): Boolean {
        return suggestStopBot(chat(chatId))
    }

    fun suggestStopBot(chat: Chat?): Boolean {
        if (chat != null && isBotChat(chat)) {
            val user = chatUser(chat)
            return user == null || !user.isSupport
        }
        return false
    }

    fun isBotChat(chatId: Long): Boolean {
        val userId = chatUserId(chatId)
        return cache().userBot(userId)
    }

    fun canEditBotChat(chatId: Long): Boolean {
        val user = chatUser(chatId)
        return user != null && TD.canEditBot(user)
    }

    fun isBotChat(chat: Chat): Boolean {
        val userId = chatUserId(chat)
        return cache().userBot(userId)
    }

    fun isSupportChat(chat: Chat): Boolean {
        val user = chatUser(chat)
        return user != null && user.isSupport
    }

    fun isDeletedAccountChat(chatId: Long): Boolean {
        val user = chatUser(chatId)
        return TD.isUserDeleted(user)
    }

    fun chatVerified(chat: Chat?): Boolean {
        if (chat == null) {
            return false
        }
        when (chat.type.getConstructor()) {
            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                val user = chatUser(chat)
                return user.isVerified()
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroup = cache().supergroup(toSupergroupId(chat.id))
                return supergroup.isVerified()
            }

            ChatTypeBasicGroup.CONSTRUCTOR -> {}
        }
        return false
    }

    fun chatScam(chat: Chat?): Boolean {
        if (chat == null) {
            return false
        }
        when (chat.type.getConstructor()) {
            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                val user = chatUser(chat)
                return user.isScam()
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroup = cache().supergroup(toSupergroupId(chat.id))
                return supergroup.isScam()
            }

            ChatTypeBasicGroup.CONSTRUCTOR -> {}
        }
        return false
    }

    fun chatFake(chat: Chat?): Boolean {
        if (chat == null) {
            return false
        }
        when (chat.type.getConstructor()) {
            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                val user = chatUser(chat)
                return user.isFake()
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroup = cache().supergroup(toSupergroupId(chat.id))
                return supergroup.isFake()
            }
        }
        return false
    }

    fun chatRestricted(chatId: Long): Boolean {
        return chatRestriction(chatId) != null
    }

    fun chatRestricted(chat: Chat?): Boolean {
        return chatRestriction(chat) != null
    }

    fun chatRestriction(chatId: Long): RestrictionInfo? {
        return chatRestriction(chatStrict(chatId))
    }

    fun chatRestriction(chat: Chat?): RestrictionInfo? {
        if (chat == null || !instance().needRestrictContent()) {
            return null
        }
        when (chat.type.getConstructor()) {
            ChatTypePrivate.CONSTRUCTOR, ChatTypeSecret.CONSTRUCTOR -> {
                val user = chatUser(chat)
                return if (user != null) user.restrictionInfo else null
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                val supergroup = cache().supergroup(toSupergroupId(chat.id))
                return if (supergroup != null) supergroup.restrictionInfo else null
            }
        }
        return null
    }

    fun chatAvailable(chat: Chat?): Boolean {
        if (chat == null) return false
        val status = chatStatus(chat.id)
        val isMember = TD.isMember(status, false)
        if (isMember || chat.type.getConstructor() == ChatTypeSupergroup.CONSTRUCTOR) return isMember
        if (chat.lastMessage != null) return true
        val positions = chat.positions
        if (positions != null) {
            for (position in positions) {
                if (position.order != 0L) return true
            }
        }
        return false
    }

    fun chatReactionsEnabled(chatId: Long): Boolean {
        val chat = chat(chatId)
        if (chat == null) return false
        val availableReactions = chat.availableReactions
        if (availableReactions == null) return false
        if (availableReactions.getConstructor() == ChatAvailableReactionsSome.CONSTRUCTOR) return (availableReactions as ChatAvailableReactionsSome).reactions.size > 0
        return true
    }

    fun chatHasScheduled(chatId: Long): Boolean {
        val chat = chat(chatId)
        return chat != null && chat.hasScheduledMessages
    }

    fun calleeUserId(message: TdApi.Message): Long {
        return if (message.isOutgoing) chatUserId(message.chatId) else senderUserId(message)
    }

    fun getDiceEmoji(text: TdApi.FormattedText): String? {
        if (!text.isEmpty() && (text.entities == null || text.entities.size == 0)) {
            val trimmed = text.text.trim { it <= ' ' }
            if (isDiceEmoji(trimmed)) {
                return trimmed
            }
        }
        return null
    }

    fun isActiveEmojiReaction(emoji: String?): Boolean {
        if (isEmpty(emoji)) {
            return false
        }
        synchronized(dataLock) {
            return activeEmojiReactions != null && activeEmojiReactions!!.contains(emoji!!)
        }
    }

    fun getActiveEmojiReactions(): MutableSet<String>? {
        synchronized(dataLock) {
            return activeEmojiReactions
        }
    }

    fun defaultEmojiReaction(): String {
        synchronized(dataLock) {
            if (defaultReactionType != null && defaultReactionType!!.getConstructor() == ReactionTypeEmoji.CONSTRUCTOR) {
                return (defaultReactionType as ReactionTypeEmoji).emoji
            }
        }
        return EmojiCodes.THUMBS_UP
    }

    fun defaultPaidReaction(): PaidReactionType? {
        synchronized(dataLock) {
            return defaultPaidReactionType
        }
    }

    fun ensureEmojiReactionsAvailable(after: RunnableBool?) {
        ensureReactionsAvailable(ChatAvailableReactionsAll(), after)
    }

    fun ensureReactionsAvailable(reactionKeys: Array<String?>, after: RunnableBool?) {
        val uniqueReactionKeys: MutableSet<String> = HashSet<String>(reactionKeys.size)
        Collections.addAll<String?>(uniqueReactionKeys, *reactionKeys)
        val reactionTypes = arrayOfNulls<ReactionType>(uniqueReactionKeys.size)
        var index = 0
        for (reactionKey in uniqueReactionKeys) {
            reactionTypes[index] = TD.toReactionType(reactionKey)
            index++
        }
        ensureReactionsAvailable(ChatAvailableReactionsSome(reactionTypes, 0), after)
    }

    fun ensureReactionsAvailable(reactions: AvailableReactions, after: RunnableBool?) {
        val reactionTypes: MutableList<ReactionType?> = ArrayList<ReactionType?>(
            reactions.recentReactions.size +
                    reactions.topReactions.size +
                    reactions.popularReactions.size
        )
        for (availableReaction in reactions.recentReactions) {
            reactionTypes.add(availableReaction.type)
        }
        for (availableReaction in reactions.topReactions) {
            reactionTypes.add(availableReaction.type)
        }
        for (availableReaction in reactions.popularReactions) {
            reactionTypes.add(availableReaction.type)
        }
        ensureReactionsAvailable(ChatAvailableReactionsSome(reactionTypes.toTypedArray<ReactionType?>(), 0), after)
    }

    fun ensureReactionsAvailable(reactions: ChatAvailableReactions, after: RunnableBool?) {
        val remaining = AtomicInteger()
        val emojiReactionWatcher: TdlibEmojiReactionsManager.Watcher =
            object : TdlibEmojiReactionsManager.Watcher {
                override fun onEntryLoaded(context: TdlibDataManager<String, TdApi.EmojiReaction, TdlibEmojiReactionsManager.Entry>?, entry: TdlibEmojiReactionsManager.Entry?) {
                    /*if (entry.value != null) {
        synchronized (dataLock) {
          cachedReactions.put(entry.key, new TGReaction(this, entry.value));
        }
      }*/
                    if (remaining.decrementAndGet() == 0) {
                        if (after != null) {
                            after.runWithBool(true)
                        }
                    }
                }
            }
        val customReactionWatcher = TdlibEmojiManager.Watcher { context: TdlibEmojiManager?, entry: TdlibEmojiManager.Entry? ->
            if (remaining.decrementAndGet() == 0) {
                if (after != null) {
                    after.runWithBool(true)
                }
            }
        }
        when (reactions.getConstructor()) {
            ChatAvailableReactionsAll.CONSTRUCTOR -> {
                val activeEmojiReactions = getActiveEmojiReactions()
                if (activeEmojiReactions == null || activeEmojiReactions.isEmpty()) {
                    if (after != null) {
                        after.runWithBool(false)
                    }
                    return
                }
                var requestedCount = 0
                remaining.set(activeEmojiReactions.size)
                for (activeEmojiReaction in activeEmojiReactions) {
                    val entry = reactions().findOrPostponeRequest(activeEmojiReaction, emojiReactionWatcher, true)
                    if (entry != null) {
                        remaining.decrementAndGet()
                    } else {
                        requestedCount++
                    }
                }
                if (requestedCount == 0) {
                    if (after != null) {
                        after.runWithBool(false)
                    }
                } else {
                    reactions().performPostponedRequests()
                }
            }

            ChatAvailableReactionsSome.CONSTRUCTOR -> {
                val some = reactions as ChatAvailableReactionsSome
                if (some.reactions.size == 0) {
                    if (after != null) {
                        after.runWithBool(false)
                    }
                    return
                }
                remaining.set(some.reactions.size)
                var requestedEmojiReactionCount = 0
                var requestedCustomReactionCount = 0
                for (reactionType in some.reactions) {
                    when (reactionType.getConstructor()) {
                        ReactionTypeEmoji.CONSTRUCTOR -> {
                            val emoji = reactionType as ReactionTypeEmoji
                            val entry = reactions().findOrPostponeRequest(emoji.emoji, emojiReactionWatcher, true)
                            if (entry != null) {
                                remaining.decrementAndGet()
                            } else {
                                requestedEmojiReactionCount++
                            }
                        }

                        ReactionTypeCustomEmoji.CONSTRUCTOR -> {
                            val customEmoji = reactionType as ReactionTypeCustomEmoji
                            val entry = emoji().findOrPostponeRequest(customEmoji.customEmojiId, customReactionWatcher, true)
                            if (entry != null) {
                                remaining.decrementAndGet()
                            } else {
                                requestedCustomReactionCount++
                            }
                        }
                    }
                }
                if (requestedEmojiReactionCount == 0 && requestedCustomReactionCount == 0) {
                    if (after != null) {
                        after.runWithBool(false)
                    }
                } else {
                    if (requestedEmojiReactionCount > 0) {
                        reactions().performPostponedRequests()
                    }
                    if (requestedCustomReactionCount > 0) {
                        emoji().performPostponedRequests()
                    }
                }
            }
        }
    }

    fun pickRandomGenericOverlaySticker(after: RunnableData<Sticker?>) {
        genericReactionEffects.get(RunnableData { stickers: Stickers? ->
            if (stickers != null && stickers.stickers.size > 0) {
                val sticker = stickers.stickers[random(0, stickers.stickers.size - 1)]
                after.runWithData(sticker)
            } else {
                after.runWithData(null)
            }
        })
    }

    fun getReaction(reactionType: ReactionType?): TGReaction? {
        return getReaction(reactionType, true)
    }

    fun getReaction(reactionType: ReactionType?, allowRequest: Boolean): TGReaction? {
        if (reactionType == null) {
            return null
        }
        val key = TD.makeReactionKey(reactionType)
        synchronized(dataLock) {
            val reaction = cachedReactions.get(key)
            if (reaction != null) {
                return reaction
            }
        }
        when (reactionType.getConstructor()) {
            ReactionTypeEmoji.CONSTRUCTOR -> {
                val emoji = reactionType as ReactionTypeEmoji
                val emojiReactionWatcher = RunnableData { newEntry: TdlibEmojiReactionsManager.Entry? ->
                    if (newEntry!!.value != null) {
                        val reaction = TGReaction(this, newEntry.value)
                        synchronized(dataLock) {
                            cachedReactions.put(key, reaction)
                        }
                        listeners().notifyReactionLoaded(key)
                    }
                }
                val entry: TdlibEmojiReactionsManager.Entry?
                if (allowRequest) {
                    entry = reactions().findOrRequest(emoji.emoji, emojiReactionWatcher)
                } else {
                    entry = reactions().find(emoji.emoji)
                }
                if (entry != null) {
                    if (entry.value == null) {
                        return null
                    }
                    val reaction = TGReaction(this, entry.value)
                    synchronized(dataLock) {
                        cachedReactions.put(key, reaction)
                    }
                    return reaction
                }
            }

            ReactionTypeCustomEmoji.CONSTRUCTOR -> {
                val customEmoji = reactionType as ReactionTypeCustomEmoji
                val customReactionWatcher = RunnableData { newEntry: TdlibEmojiManager.Entry? ->
                    if (newEntry!!.value != null) {
                        val reaction = TGReaction(this, newEntry.value)
                        synchronized(dataLock) {
                            cachedReactions.put(key, reaction)
                        }
                        listeners().notifyReactionLoaded(key)
                    }
                }
                val entry: TdlibEmojiManager.Entry?
                if (allowRequest) {
                    entry = emoji().findOrRequest(customEmoji.customEmojiId, customReactionWatcher)
                } else {
                    entry = emoji().find(customEmoji.customEmojiId)
                }
                if (entry != null) {
                    if (entry.value == null) {
                        return null
                    }
                    val reaction = TGReaction(this, entry.value)
                    synchronized(dataLock) {
                        cachedReactions.put(key, reaction)
                    }
                    return reaction
                }
            }
        }
        return null
    }

    fun shouldSendAsDice(text: TdApi.FormattedText): Boolean {
        return getDiceEmoji(text) != null
    }

    fun isDiceEmoji(text: String?): Boolean {
        if (isEmpty(text)) return false
        synchronized(dataLock) {
            if (diceEmoji != null) {
                return contains<String?>(diceEmoji, text)
            }
        }
        return false
    }

    // Chat open/close
    private val openedChats = LongSparseArray<ArrayList<ViewController<*>?>?>(8)
    private val openedChatsTimes = LongSparseIntArray()
    private val chatOpenMutex = Any()

    @JvmOverloads
    fun openChat(chatId: Long, controller: ViewController<*>?, after: Runnable? = null) {
        val okHandler = okHandler()
        synchronized(chatOpenMutex) {
            var controllers = openedChats.get(chatId)
            if (controllers == null) {
                controllers = ArrayList<ViewController<*>?>()
                controllers.add(controller)
                openedChats.put(chatId, controllers)
            } else {
                controllers.add(controller)
            }
            if (controllers.size == 1) {
                openedChatsTimes.put(chatId, (System.currentTimeMillis() / 1000L).toInt())
                if (Log.isEnabled(Log.TAG_MESSAGES_LOADER)) {
                    Log.v(Log.TAG_MESSAGES_LOADER, "openChat, chatId=%d", chatId)
                }
                client().send(OpenChat(chatId), if (after != null) Client.ResultHandler { result: TdApi.Object? ->
                    okHandler.onResult(result)
                    after.run()
                } else okHandler)
            } else if (after != null) {
                client().send(SetAlarm(0.0), Client.ResultHandler { result: TdApi.Object? -> after.run() }
                )
            }
        }
        notifications().onChatOpened(chatId)
    }

    fun isChatOpen(chatId: Long): Boolean {
        synchronized(chatOpenMutex) {
            return openedChatsTimes.get(chatId) != 0
        }
    }

    fun closeChat(chatId: Long, controller: ViewController<*>?, needDelay: Boolean) {
        if (needDelay) {
            ui()!!.postDelayed(Runnable { closeChatImpl(chatId, controller) }, 1000)
        } else {
            closeChatImpl(chatId, controller)
        }
    }

    private fun closeChatImpl(chatId: Long, controller: ViewController<*>?) {
        synchronized(chatOpenMutex) {
            val controllers = openedChats.get(chatId)
            if (controllers != null && controllers.remove(controller) && controllers.isEmpty()) {
                openedChatsTimes.delete(chatId)
                openedChats.remove(chatId)
                if (Log.isEnabled(Log.TAG_MESSAGES_LOADER)) {
                    Log.v(Log.TAG_MESSAGES_LOADER, "closeChat, chatId=%d", chatId)
                }
                client().send(CloseChat(chatId), okHandler())
            }
        }
    }

    fun onScreenshotTaken(timeSeconds: Int) {
        /* synchronized (chatOpenMutex) {
      final int size = openedChatsTimes.size();
      for (int i = 0; i < size; i++) {
        final int openTime = openedChatsTimes.valueAt(i);
        if (timeSeconds >= openTime) {
          final long chatId = openedChatsTimes.keyAt(i);
          TdApi.Chat chat = chat(chatId);
          if ((ChatId.isSecret(chatId) || ui().shouldSendScreenshotHint(chat))) {
            sendScreenshotMessage(chatId, null);
          }
        }
      }
    }*/
        ui()!!.execute(Runnable { messageViewer.onScreenshotTaken(timeSeconds) }
        )
    }

    fun hasPotentiallyVisibleMessages(): Boolean {
        return (openedChats != null && openedChats.size() > 0) || messageViewer.hasPotentiallyVisibleMessages()
    }

    // Metadata
    fun emailMetadata(): String {
        return Lang.getString(
            R.string.email_metadata,
            BuildConfig.VERSION_NAME,
            languagePackId,
            TdlibManager.getSystemLanguageCode(),
            TdlibManager.getSystemVersion()
        )
    }

    // Notificaitons
    fun accountColor(chatId: Long): Int {
        return getColor(if (isSecret(chatId)) ColorId.notificationSecure else ColorId.notification)
    }

    fun accountColor(): Int {
        return getColor(ColorId.notification)
    }

    fun accountPlayerColor(): Int {
        return getColor(ColorId.notificationPlayer)
    }

    fun getColor(@ColorId colorId: Int): Int {
        val colorTheme = settings().globalTheme()
        return Theme.getColor(colorId, colorTheme)
    }

    fun accountShortName(): String? {
        return context.account(accountId).getShortName()
    }

    fun accountShortName(category: Int): String? {
        val name = accountShortName()
        return if (category != TdlibNotificationGroup.CATEGORY_DEFAULT) Lang.getString(
            R.string.format_accountAndCategory,
            name,
            Lang.getNotificationCategory(category)
        ) else name
    }

    fun accountLongName(): String? {
        return context.account(accountId).getLongName()
    }

    fun accountName(): String? {
        return context.account(accountId).getName()
    }

    // Actions
    fun sendScreenshotMessage(chatId: Long, messageIds: LongArray?) {
        client().send(ViewMessages(chatId, messageIds, MessageSourceScreenshot(), false), messageHandler())
    }

    fun sendMessage(chatId: Long, topicId: MessageTopic?, replyTo: InputMessageReplyTo?, options: MessageSendOptions?, animation: TdApi.Animation) {
        val inputMessageContent: InputMessageContent =
            InputMessageAnimation(InputFileId(animation.animation.id), null, null, animation.duration, animation.width, animation.height, null, false, false)
        sendMessage(chatId, topicId, replyTo, options, inputMessageContent)
    }

    fun sendMessage(chatId: Long, topicId: MessageTopic?, replyTo: InputMessageReplyTo?, options: MessageSendOptions?, audio: TdApi.Audio) {
        val inputMessageContent: InputMessageContent = InputMessageAudio(InputFileId(audio.audio.id), null, audio.duration, audio.title, audio.performer, null)
        sendMessage(chatId, topicId, replyTo, options, inputMessageContent)
    }

    fun sendMessage(chatId: Long, topicId: MessageTopic?, replyTo: InputMessageReplyTo?, options: MessageSendOptions?, sticker: Sticker, emoji: String?) {
        val inputMessageContent: InputMessageContent = InputMessageSticker(InputFileId(sticker.sticker.id), null, 0, 0, emoji)
        sendMessage(chatId, topicId, replyTo, options, inputMessageContent)
    }

    @JvmOverloads
    fun sendMessage(
        chatId: Long,
        topicId: MessageTopic?,
        replyTo: InputMessageReplyTo?,
        options: MessageSendOptions?,
        inputMessageContent: InputMessageContent?,
        after: RunnableData<TdApi.Message?>? = null
    ) {
        client().send(
            SendMessage(chatId, topicId, replyTo, options, null, inputMessageContent),
            if (after != null) Client.ResultHandler { result: TdApi.Object? ->
                messageHandler.onResult(result)
                after.runWithData(if (result is TdApi.Message) result else null)
            } else messageHandler())
    }

    fun resendMessages(chatId: Long, messageIds: LongArray?) {
        client().send(ResendMessages(chatId, messageIds, null, 0), messageHandler())
    }

    private val pendingMessageTexts = HashMap<String?, MessageContent?>()
    private val pendingMessageCaptions = HashMap<String?, TdApi.FormattedText?>()

    fun canEditMedia(message: MessageWithProperties?, photoVideoOnly: Boolean): Boolean {
        if (message != null) {
            return canEditMedia(message.message, message.properties, photoVideoOnly)
        } else {
            return false
        }
    }

    fun canEditMedia(message: TdApi.Message?, properties: MessageProperties?, photoVideoOnly: Boolean): Boolean {
        if (message == null || message.content == null || properties == null || !properties.canBeEdited) {
            return false
        }

        when (message.content.getConstructor()) {
            TdApi.MessagePhoto.CONSTRUCTOR, TdApi.MessageVideo.CONSTRUCTOR -> return true
            TdApi.MessageAudio.CONSTRUCTOR, MessageDocument.CONSTRUCTOR, TdApi.MessageAnimation.CONSTRUCTOR -> return !photoVideoOnly
            else -> assertMessageContent_baa076bf()
        }

        return false
    }


    fun editMessageText(chatId: Long, messageId: Long, content: InputMessageText, linkPreview: TdApi.LinkPreview?) {
        var linkPreview = linkPreview
        if (content.linkPreviewOptions != null && content.linkPreviewOptions!!.isDisabled) {
            linkPreview = null
        }
        TD.parseEntities(content.text)
        val messageText = MessageText(content.text, linkPreview, content.linkPreviewOptions)
        if (!Emoji.instance().isSingleEmoji(content.text)) {
            performEdit<MessageContent?>(chatId, messageId, messageText, EditMessageText(chatId, messageId, null, content), pendingMessageTexts)
            return
        }
        var customEmojiId: Long = 0
        if (content.text.entities != null) {
            for (entity in content.text.entities) {
                if (entity.type.isCustomEmoji()) {
                    customEmojiId = (entity.type as TextEntityTypeCustomEmoji).customEmojiId
                    break
                }
            }
        }
        val animatedEmojiFallback = Runnable {
            client().send(GetAnimatedEmoji(content.text.text), Client.ResultHandler { result: TdApi.Object? ->
                if (result!!.getConstructor() == AnimatedEmoji.CONSTRUCTOR) {
                    val animatedEmoji = MessageAnimatedEmoji(result as AnimatedEmoji, content.text.text)
                    performEdit<MessageContent?>(chatId, messageId, animatedEmoji, EditMessageText(chatId, messageId, null, content), pendingMessageTexts)
                } else {
                    performEdit<MessageContent?>(chatId, messageId, messageText, EditMessageText(chatId, messageId, null, content), pendingMessageTexts)
                }
            })
        }
        if (customEmojiId != 0L) {
            emoji().findOrRequest(customEmojiId, RunnableData { entry: TdlibEmojiManager.Entry? ->
                if (entry != null && !entry.isNotFound()) {
                    val customEmojiSticker = entry.value
                    val animatedEmoji = MessageAnimatedEmoji(
                        AnimatedEmoji(customEmojiSticker, customEmojiSticker!!.width, customEmojiSticker.height, 0, null),
                        content.text.text
                    )
                    performEdit<MessageContent?>(chatId, messageId, animatedEmoji, EditMessageText(chatId, messageId, null, content), pendingMessageTexts)
                } else {
                    animatedEmojiFallback.run()
                }
            })
        } else {
            animatedEmojiFallback.run()
        }
    }

    fun editMessageCaption(chatId: Long, messageId: Long, caption: TdApi.FormattedText?, showCaptionAboveMedia: Boolean) {
        TD.parseEntities(caption)
        performEdit<TdApi.FormattedText?>(
            chatId,
            messageId,
            caption,
            EditMessageCaption(chatId, messageId, null, caption, showCaptionAboveMedia),
            pendingMessageCaptions
        )
    }

    fun cancelEditMessageMedia(chatId: Long, messageId: Long): Boolean {
        return editMediaManager.editMediaCancel(chatId, messageId)
    }

    fun editMessageMedia(chatId: Long, messageId: Long, content: InputMessageContent?, localPickedFile: LocalPickedFile) {
        editMediaManager.editMediaStart(chatId, messageId, content, localPickedFile)
    }

    fun getFormattedText(message: TdApi.Message?): TdApi.FormattedText? {
        if (message == null) return null
        val pendingText = getPendingFormattedText(message.chatId, message.id)
        if (pendingText != null) return pendingText
        return message.content.textOrCaption()
    }

    fun getPendingFormattedText(chatId: Long, messageId: Long): TdApi.FormattedText? {
        val messageText = getPendingMessageText(chatId, messageId)
        if (messageText != null) {
            when (messageText.getConstructor()) {
                MessageText.CONSTRUCTOR -> return (messageText as MessageText).text
                MessageAnimatedEmoji.CONSTRUCTOR -> return messageText.textOrCaption()
            }
            assertMessageContent_baa076bf()
            throw unsupported(messageText)
        }
        val pendingEditMedia = getPendingMessageMedia(chatId, messageId)
        if (pendingEditMedia != null) {
            return pendingEditMedia.content.textOrCaption()
        }

        return getPendingMessageCaption(chatId, messageId)
    }

    fun getPendingMessageText(chatId: Long, messageId: Long): MessageContent? {
        synchronized(pendingMessageTexts) {
            return pendingMessageTexts.get(chatId.toString() + "_" + messageId)
        }
    }

    fun getPendingMessageCaption(chatId: Long, messageId: Long): TdApi.FormattedText? {
        val pending = getPendingMessageMedia(chatId, messageId)
        if (pending != null) {
            return pending.getCaption()
        }

        synchronized(pendingMessageCaptions) {
            return pendingMessageCaptions.get(chatId.toString() + "_" + messageId)
        }
    }

    fun getPendingMessageMedia(chatId: Long, messageId: Long): MessageEditMediaPending? {
        return editMediaManager.getPendingMessageMedia(chatId, messageId)
    }

    private fun <T : TdApi.Object?> performEdit(chatId: Long, messageId: Long, pendingData: T?, function: TdApi.Function<*>?, map: MutableMap<String?, T?>) {
        val key = chatId.toString() + "_" + messageId
        synchronized(map) {
            map.put(key, pendingData)
        }
        listeners.updateMessagePendingContentChanged(chatId, messageId)
        client().send(function, Client.ResultHandler { result: TdApi.Object? ->
            if (result!!.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                UI.showError(result)
            }
            tdlib().ui()!!.post(Runnable {
                val currentData = map.get(key)
                if (currentData === pendingData) {
                    synchronized(map) {
                        map.remove(key)
                    }
                    listeners.updateMessagePendingContentChanged(chatId, messageId)
                }
            })
        })
    }

    private var messageCallbacks: MutableMap<String?, Queue<Runnable>?>? = null

    fun awaitMessageSent(message: TdApi.Message, callback: Runnable) {
        if (message.sendingState == null || message.sendingState!!.getConstructor() != MessageSendingStatePending.CONSTRUCTOR || qack().isMessageAcknowledged(
                message.chatId,
                message.id
            )
        ) {
            callback.run()
        } else {
            synchronized(dataLock) {
                if (messageCallbacks == null) messageCallbacks = HashMap<String?, Queue<Runnable>?>()
                val key = message.chatId.toString() + "_" + message.id
                var callbacks = messageCallbacks!!.get(key)
                if (callbacks == null) {
                    callbacks = ArrayDeque<Runnable>()
                    messageCallbacks!!.put(key, callbacks)
                }
                callbacks.add(callback)
            }
        }
    }

    private fun notifyMessageSendCallbacks(chatId: Long, messageId: Long) {
        val callbacks: Queue<Runnable>?
        synchronized(dataLock) {
            if (messageCallbacks == null) return
            callbacks = messageCallbacks!!.remove(chatId.toString() + "_" + messageId)
        }
        if (callbacks != null) {
            for (callback in callbacks) {
                callback.run()
            }
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TesterLevel.NONE, TesterLevel.READER, TesterLevel.TESTER, TesterLevel.TRANSLATOR, TesterLevel.ADMIN, TesterLevel.DEVELOPER, TesterLevel.CREATOR
    )
    annotation class TesterLevel {
        companion object {
            const val NONE: Int = 0
            const val READER: Int = 1
            const val TESTER: Int = 2
            const val TRANSLATOR: Int = 3
            const val ADMIN: Int = 4
            const val DEVELOPER: Int = 5
            const val CREATOR: Int = 6

            const val UNKNOWN: Int = -1

            const val MIN_LEVEL_FOR_DEBUG_DC: Int = TRANSLATOR
            const val MIN_LEVEL_FOR_BATMAN_EFFECT: Int = READER
        }
    }

    fun isRedTeam(chatId: Long): Boolean {
        if (this.isDebugInstance) {
            return false
        }
        val supergroupId = toSupergroupId(chatId)
        if (supergroupId == 0L) return false
        return supergroupId == 1084287520L || supergroupId == 1266791237L || supergroupId == 1492016544L || supergroupId == 1227585106L || supergroupId == 1116030833L
    }

    fun getTesterLevel(callback: RunnableInt) {
        getTesterLevel(callback, false)
    }

    fun getTesterLevel(callback: RunnableInt, onlyLocal: Boolean) {
        if (inRecoveryMode() || this.isDebugInstance) {
            callback.runWithInt(TesterLevel.TESTER)
            return
        }
        val myUserId = myUserId()
        if (myUserId == TGX_CREATOR_USER_ID) {
            callback.runWithInt(TesterLevel.CREATOR)
            return
        }
        if (myUserId == TDLIB_CREATOR_USER_ID) {
            callback.runWithInt(TesterLevel.DEVELOPER)
            return
        }

        val pairs: ArrayList<Pair<Long?, Int?>> = object : ArrayList<Pair<Long?, Int?>>() {
            init {
                add(Pair<Long?, Int?>(ADMIN_CHAT_ID, TesterLevel.ADMIN))
                add(Pair<Long?, Int?>(TRANSLATORS_CHAT_ID, TesterLevel.TRANSLATOR))
                add(Pair<Long?, Int?>(TESTER_CHAT_ID, TesterLevel.TESTER))
                add(Pair<Long?, Int?>(READER_CHAT_ID, TesterLevel.READER))
            }
        }

        if (onlyLocal) {
            for (pair in pairs) {
                val chat = chat(pair.first!!)
                if (chat != null && TD.isMember(chatStatus(chat.id))) {
                    callback.runWithInt(pair.second!!)
                    return
                }
            }
            callback.runWithInt(TesterLevel.NONE)
        } else {
            val act: Runnable = object : Runnable {
                override fun run() {
                    if (pairs.isEmpty()) {
                        callback.runWithInt(TesterLevel.NONE)
                        return
                    }
                    val pair = pairs.removeAt(0)
                    chat(pair.first!!, RunnableData { chat: Chat? ->
                        if (chat != null && TD.isMember(chatStatus(chat.id))) {
                            callback.runWithInt(pair.second!!)
                        } else {
                            this.run()
                        }
                    })
                }
            }
            act.run()
        }
    }

    fun forwardMessage(chatId: Long, topicId: MessageTopic?, fromChatId: Long, messageId: Long, options: MessageSendOptions?) {
        client().send(ForwardMessages(chatId, topicId, fromChatId, longArrayOf(messageId), options, false, false), messageHandler())
    }

    fun sendInlineQueryResult(
        chatId: Long,
        topicId: MessageTopic?,
        replyTo: InputMessageReplyTo?,
        options: MessageSendOptions?,
        queryId: Long,
        resultId: String?
    ) {
        client().send(SendInlineQueryResultMessage(chatId, topicId, replyTo, options, queryId, resultId, false), messageHandler())
    }

    fun sendBotStartMessage(botUserId: Long, chatId: Long, parameter: String?) {
        client().send(SendBotStartMessage(botUserId, chatId, parameter), messageHandler())
    }

    fun setChatMessageAutoDeleteTime(chatId: Long, ttl: Int) {
        client().send(SetChatMessageAutoDeleteTime(chatId, ttl), okHandler())
    }

    fun getPrimaryChatInviteLink(chatId: Long, handler: ResultHandler<ChatInviteLink?>) {
        val linkHandler: Client.ResultHandler = object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                val inviteLink: ChatInviteLink?
                when (result.getConstructor()) {
                    BasicGroupFullInfo.CONSTRUCTOR -> {
                        inviteLink = (result as BasicGroupFullInfo).inviteLink
                    }

                    SupergroupFullInfo.CONSTRUCTOR -> {
                        inviteLink = (result as SupergroupFullInfo).inviteLink
                    }

                    ChatInviteLink.CONSTRUCTOR -> {
                        handler.onResult(result as ChatInviteLink, null)
                        return
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        handler.onResult(null, result as TdApi.Error)
                        return
                    }

                    else -> throw UnsupportedOperationException(result.toString())
                }
                if (inviteLink != null) {
                    handler.onResult(inviteLink, null)
                } else {
                    client().send(ReplacePrimaryChatInviteLink(chatId), this)
                }
            }
        }
        when (getType(chatId)) {
            ChatTypeBasicGroup.CONSTRUCTOR -> {
                client().send(GetBasicGroupFullInfo(toBasicGroupId(chatId)), linkHandler)
            }

            ChatTypeSupergroup.CONSTRUCTOR -> {
                client().send(GetSupergroupFullInfo(toSupergroupId(chatId)), linkHandler)
            }

            else -> {
                handler.onResult(null, TdApi.Error(-1, "Invalid chat type"))
            }
        }
    }

    @TdlibThread
    fun traceInviteLink(inviteLinkInfo: ChatInviteLinkInfo) {
        if (inviteLinkInfo.chatId == 0L) return
        synchronized(dataLock) {
            if (inviteLinkInfo.isPublic || inviteLinkInfo.accessibleFor == 0) {
                accessibleChatTimers.delete(inviteLinkInfo.chatId)
            } else {
                accessibleChatTimers.put(
                    inviteLinkInfo.chatId,
                    SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(inviteLinkInfo.accessibleFor.toLong())
                )
            }
        }
    }

    fun isPublicChat(chatId: Long): Boolean {
        if (chatId == 0L) return false
        val chat = chat(chatId)
        if (chat == null) return false
        if (!isEmpty(chatUsername(chat))) return true
        val supergroup = chatToSupergroup(chatId)
        return supergroup != null && (supergroup.hasLinkedChat || supergroup.hasLocation)
    }

    fun isTemporaryAccessible(chatId: Long): Boolean {
        synchronized(dataLock) {
            val openUntil = accessibleChatTimers.get(chatId)
            if (openUntil > SystemClock.elapsedRealtime()) return true
            if (openUntil != 0L) accessibleChatTimers.delete(chatId)
            return false
        }
    }

    fun chatAccessState(chatId: Long): Int {
        return if (chatId != 0L) chatAccessState(chat(chatId)) else CHAT_ACCESS_FAIL
    }

    fun chatAccessState(chat: Chat?): Int {
        if (chat == null) {
            return CHAT_ACCESS_FAIL
        }
        if (chat.type.getConstructor() == ChatTypeSupergroup.CONSTRUCTOR) {
            val supergroup = chatToSupergroup(chat.id)
            if (supergroup == null) {
                return CHAT_ACCESS_FAIL
            }
            if (supergroup.isAdministeredDirectMessagesGroup) {
                return CHAT_ACCESS_OK
            }
            val isPublic = supergroup.hasUsername() || supergroup.hasLinkedChat || supergroup.hasLocation || supergroup.isDirectMessagesGroup
            val isTemporary = isTemporaryAccessible(chat.id)
            when (supergroup.status.getConstructor()) {
                ChatMemberStatusLeft.CONSTRUCTOR -> return if (isPublic) CHAT_ACCESS_OK else if (isTemporary) CHAT_ACCESS_TEMPORARY else CHAT_ACCESS_PRIVATE
                ChatMemberStatusRestricted.CONSTRUCTOR -> return if (isPublic || (supergroup.status as ChatMemberStatusRestricted).isMember) CHAT_ACCESS_OK else if (isTemporary) CHAT_ACCESS_TEMPORARY else CHAT_ACCESS_PRIVATE
                ChatMemberStatusBanned.CONSTRUCTOR -> return CHAT_ACCESS_BANNED
                ChatMemberStatusAdministrator.CONSTRUCTOR, TdApi.ChatMemberStatusCreator.CONSTRUCTOR, TdApi.ChatMemberStatusMember.CONSTRUCTOR -> return CHAT_ACCESS_OK
            }
        }
        return CHAT_ACCESS_OK
    }

    fun blockSender(sender: MessageSender?, blockList: TdApi.BlockList?, handler: Client.ResultHandler?) {
        client().send(SetMessageSenderBlockList(sender, blockList), handler)
    }

    fun unblockSender(sender: MessageSender?, handler: Client.ResultHandler?) {
        blockSender(sender, null, handler)
    }

    fun setScopeNotificationSettings(scope: NotificationSettingsScope?, settings: ScopeNotificationSettings?) {
        client().send(SetScopeNotificationSettings(scope, settings), okHandler())
    }

    fun scopeNotificationSettings(chatId: Long): ScopeNotificationSettings {
        return notificationManager.getScopeNotificationSettings(chatId)
    }

    fun scopeNotificationSettings(chat: Chat?): ScopeNotificationSettings? {
        return notificationManager.getScopeNotificationSettings(chat)
    }

    fun scopeNotificationSettings(scope: NotificationSettingsScope): ScopeNotificationSettings? {
        return notificationManager.getScopeNotificationSettings(scope)
    }

    fun setChatNotificationSettings(chatId: Long, settings: ChatNotificationSettings?) {
        client().send(SetChatNotificationSettings(chatId, settings), okHandler())
    }

    fun setMuteForSync(chatId: Long, muteFor: Int) {
        val chat = chatSync(chatId)
        if (chat == null) throw NullPointerException()
        val scope = notifications().scope(chat)
        var settings = scopeNotificationSettings(scope)
        if (settings == null) {
            val result = clientExecute(GetScopeNotificationSettings(scope), 0)
            if (result is ScopeNotificationSettings) settings = result
        }
        if (settings == null) throw NullPointerException()
        val chatSettings = chat.notificationSettings
        chatSettings.muteFor = muteFor
        chatSettings.useDefaultMuteFor = (muteFor == 0 && settings.muteFor == 0) // || (TD.isMutedForever(muteFor) && TD.isMutedForever(settings.muteFor));
        setChatNotificationSettings(chatId, chatSettings)
    }

    fun setMuteFor(chatId: Long, muteFor: Int) {
        val settings = scopeNotificationSettings(chatId)
        val chatSettings = chatSettings(chatId)
        if (settings == null) throw NullPointerException()
        if (chatSettings == null) throw NullPointerException()
        chatSettings.muteFor = muteFor
        chatSettings.useDefaultMuteFor = (muteFor == 0 && settings.muteFor == 0) // || (TD.isMutedForever(muteFor) && TD.isMutedForever(settings.muteFor));
        setChatNotificationSettings(chatId, chatSettings)
    }

    fun setScopeMuteFor(scope: NotificationSettingsScope, muteFor: Int) {
        val settings = scopeNotificationSettings(scope)
        if (settings != null) {
            settings.muteFor = muteFor
            setScopeNotificationSettings(scope, settings)
        } else {
            client().send(GetScopeNotificationSettings(scope), Client.ResultHandler { result: TdApi.Object? ->
                if (result!!.getConstructor() == ScopeNotificationSettings.CONSTRUCTOR) {
                    val scopeSettings = result as ScopeNotificationSettings
                    scopeSettings.muteFor = muteFor
                    setScopeNotificationSettings(scope, scopeSettings)
                }
            })
        }
    }

    fun setChatPermissions(chatId: Long, permissions: ChatPermissions?, callback: RunnableBool) {
        client().send(SetChatPermissions(chatId, permissions), Client.ResultHandler { result: TdApi.Object? ->
            when (result!!.getConstructor()) {
                TdApi.Ok.CONSTRUCTOR -> callback.runWithBool(true)
                TdApi.Error.CONSTRUCTOR -> {
                    UI.showError(result)
                    callback.runWithBool(false)
                }
            }
        })
    }

    fun canRevokeChat(chatId: Long): Boolean {
        return isPrivate(chatId) && !isSelfChat(chatId) && !isBotChat(chatId)
    }

    fun deleteChat(chatId: Long, revoke: Boolean, after: Runnable?) {
        when (getType(chatId)) {
            ChatTypePrivate.CONSTRUCTOR -> {
                client().send(DeleteChatHistory(chatId, true, revoke), okHandler(after))
            }

            ChatTypeSecret.CONSTRUCTOR -> {
                val secretChat = chatToSecretChat(chatId)
                if (secretChat == null || secretChat.state.getConstructor() == TdApi.SecretChatStateClosed.CONSTRUCTOR) {
                    client().send(DeleteChatHistory(chatId, true, revoke), silentHandler(after))
                } else {
                    client().send(CloseSecretChat(toSecretChatId(chatId)), Client.ResultHandler { result: TdApi.Object? ->
                        if (result!!.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                            Log.e("Cannot close secret chat, secretChatId:%d, error: %s", toSecretChatId(chatId), TD.toErrorString(result))
                        }
                        client().send(DeleteChatHistory(chatId, true, revoke), silentHandler(after))
                    })
                }
            }

            ChatTypeBasicGroup.CONSTRUCTOR, ChatTypeSupergroup.CONSTRUCTOR -> {
                val status = chatStatus(chatId)
                if (!TD.isMember(status, false)) {
                    client().send(DeleteChatHistory(chatId, true, revoke), okHandler(after))
                } else {
                    client().send(SetChatMemberStatus(chatId, mySender(), ChatMemberStatusLeft()), Client.ResultHandler { result: TdApi.Object? ->
                        if (isBasicGroup(chatId)) {
                            client().send(DeleteChatHistory(chatId, true, revoke), okHandler(after))
                        }
                    })
                }
            }
        }
    }

    fun setChatMemberTag(chatId: Long, userId: Long, newTag: String?, after: RunnableData<TdApi.Error?>?) {
        send<TdApi.Ok?>(SetChatMemberTag(chatId, userId, newTag), Tdlib.ResultHandler { ok: TdApi.Ok?, setTagError: TdApi.Error? ->
            if (setTagError == null) {
                send<ChatMember?>(GetChatMember(chatId, MessageSenderUser(userId)), Tdlib.ResultHandler { member: ChatMember?, getMemberError: TdApi.Error? ->
                    if (member != null) {
                        cache().onChatMemberStatusChanged(chatId, member)
                    }
                    if (after != null) {
                        after.runWithData(getMemberError)
                    }
                })
            } else {
                if (after != null) {
                    after.runWithData(setTagError)
                }
            }
        })
    }

    private fun setChatMemberStatusImpl(
        chatId: Long,
        sender: MessageSender,
        newStatus: ChatMemberStatus?,
        forwardLimit: Int,
        currentStatus: ChatMemberStatus?,
        callback: ChatMemberStatusChangeCallback?
    ) {
        val needForward = isBasicGroup(chatId) && forwardLimit > 0 && !TD.isMember(currentStatus, false) && TD.isMember(
            newStatus,
            false
        ) && sender.getConstructor() == MessageSenderUser.CONSTRUCTOR
        val oneShot = if (needForward && TD.isAdmin(newStatus)) AtomicBoolean(false) else null

        val function: TdApi.Function<*>?
        if (needForward) {
            function = AddChatMember(chatId, (sender as MessageSenderUser).userId, forwardLimit) as TdApi.Function<FailedToAddMembers?>
        } else {
            function = SetChatMemberStatus(chatId, sender, newStatus) as TdApi.Function<TdApi.Ok?>
        }

        val error = AtomicReference<TdApi.Error?>()
        val retryCount = AtomicInteger(0)
        client().send(function, object : Client.ResultHandler {
            override fun onResult(`object`: TdApi.Object) {
                when (`object`.getConstructor()) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        if (oneShot != null && !oneShot.getAndSet(true)) {
                            client().send(SetChatMemberStatus(chatId, sender, newStatus), this)
                        } else {
                            client().send(GetChatMember(chatId, sender), this)
                        }
                        return
                    }

                    FailedToAddMembers.CONSTRUCTOR -> {
                        val failedToAddMembers = `object` as FailedToAddMembers
                        if (failedToAddMembers.failedToAddMembers.size == 0) {
                            if (oneShot != null && !oneShot.getAndSet(true)) {
                                client().send(SetChatMemberStatus(chatId, sender, newStatus), this)
                            } else {
                                client().send(GetChatMember(chatId, sender), this)
                            }
                        } else {
                            if (callback != null) {
                                callback.onMemberStatusUpdated(false, null, failedToAddMembers.failedToAddMembers[0])
                            }
                        }
                        return
                    }

                    ChatMember.CONSTRUCTOR -> {
                        val newMember = `object` as ChatMember
                        if (error.get() == null && !newStatus.equalsTo(newMember.status) && retryCount.incrementAndGet() <= 3) {
                            client().send(SetAlarm(.5 + .5 * retryCount.get()), this)
                        } else {
                            cache().onChatMemberStatusChanged(chatId, newMember)
                            if (callback != null) {
                                val result = error.get()
                                callback.onMemberStatusUpdated(result == null, result, null)
                            }
                        }
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        val originalError = error.getAndSet(`object` as TdApi.Error)
                        if (originalError == null) {
                            client().send(GetChatMember(chatId, sender), this)
                        } else if (callback != null) {
                            callback.onMemberStatusUpdated(false, originalError, null)
                        } else {
                            UI.showError(originalError)
                        }
                    }
                }
            }
        })
    }

    fun terminateSession(session: TdApi.Session, after: RunnableData<TdApi.Error?>) {
        client().send(TerminateSession(session.id), Client.ResultHandler { result: TdApi.Object? ->
            after.runWithData(if (result!!.getConstructor() == TdApi.Error.CONSTRUCTOR) result as TdApi.Error else null)
            if (result.getConstructor() == TdApi.Ok.CONSTRUCTOR) {
                this.sessionsInfo = null
                listeners.notifySessionTerminated(session)
            }
        })
    }

    fun terminateAllOtherSessions(currentSession: TdApi.Session?, after: RunnableData<TdApi.Error?>?) {
        client().send(TerminateAllOtherSessions(), Client.ResultHandler { result: TdApi.Object? ->
            if (after != null) {
                after.runWithData(if (result!!.getConstructor() == TdApi.Error.CONSTRUCTOR) result as TdApi.Error else null)
            }
            if (result!!.getConstructor() == TdApi.Ok.CONSTRUCTOR) {
                this.sessionsInfo = null
                listeners.notifyAllSessionsTerminated(currentSession)
            }
        })
    }

    fun setInactiveSessionTtl(ttlDays: Int, after: RunnableData<TdApi.Error?>?) {
        client().send(SetInactiveSessionTtl(ttlDays), Client.ResultHandler { result: TdApi.Object? ->
            if (after != null) {
                after.runWithData(if (result!!.getConstructor() == TdApi.Error.CONSTRUCTOR) result as TdApi.Error else null)
            }
            if (result!!.getConstructor() == TdApi.Ok.CONSTRUCTOR) {
                this.sessionsInfo = null
                listeners.notifyInactiveSessionTtlChanged(ttlDays)
            }
        })
    }

    class SessionsInfo(sessions: Sessions) {
        @JvmField
        val allSessions: Array<TdApi.Session>
        @JvmField
        val otherActiveSessions: Array<TdApi.Session>
        @JvmField
        val incompleteLoginAttempts: Array<TdApi.Session>
        @JvmField
        val currentSession: TdApi.Session?
        @JvmField
        val onlyCurrent: Boolean

        @JvmField
        val otherDevicesCount: Int
        @JvmField
        val sessionCountOnCurrentDevice: Int
        @JvmField
        val activeSessionCount: Int

        @JvmField
        val inactiveSessionTtlDays: Int

        init {
            this.inactiveSessionTtlDays = sessions.inactiveSessionTtlDays

            sessions.sessions.sort()

            var currentSession: TdApi.Session? = null
            var pendingPasswords: MutableList<TdApi.Session>? = null
            var otherSessions: MutableList<TdApi.Session>? = null
            val devicesMap: MutableMap<String?, AtomicInteger?> = HashMap<String?, AtomicInteger?>()
            var activeSessionCount = 0
            var totalSessionCount = 0
            for (session in sessions.sessions) {
                totalSessionCount++
                if (session.isCurrent) {
                    currentSession = session
                } else if (session.isPasswordPending) {
                    if (pendingPasswords == null) {
                        pendingPasswords = ArrayList<TdApi.Session>()
                    }
                    pendingPasswords.add(session)
                    continue
                } else {
                    if (otherSessions == null) {
                        otherSessions = ArrayList<TdApi.Session>()
                    }
                    otherSessions.add(session)
                }

                val signature = getSignature(session)
                val counter = devicesMap.get(signature)
                if (counter != null) {
                    counter.incrementAndGet()
                } else {
                    devicesMap.put(signature, AtomicInteger(1))
                }
                activeSessionCount++
            }

            val currentDeviceCounter = if (currentSession != null) devicesMap.remove(getSignature(currentSession)) else null

            this.allSessions = sessions.sessions
            this.currentSession = currentSession
            this.sessionCountOnCurrentDevice = if (currentDeviceCounter != null) currentDeviceCounter.get() else 1
            this.otherDevicesCount = devicesMap.size
            this.activeSessionCount = activeSessionCount
            this.onlyCurrent = totalSessionCount == 1
            this.incompleteLoginAttempts =
                if (pendingPasswords != null && !pendingPasswords.isEmpty()) pendingPasswords.toTypedArray<TdApi.Session>() else emptyArray<TdApi.Session>()
            this.otherActiveSessions =
                if (otherSessions != null && !otherSessions.isEmpty()) otherSessions.toTypedArray<TdApi.Session>() else emptyArray<TdApi.Session>()
        }

        companion object {
            private fun getSignature(session: TdApi.Session): String {
                return session.deviceModel + " " + session.platform + " " + session.systemVersion
            }
        }
    }

    private var sessionsInfo: SessionsInfo? = null

    fun currentSession(): TdApi.Session? {
        synchronized(dataLock) {
            return sessionsInfo!!.currentSession
        }
    }

    fun getSessions(allowCached: Boolean, callback: RunnableData<SessionsInfo?>?) {
        if (allowCached) {
            runOnTdlibThread(Runnable {
                if (sessionsInfo != null) {
                    if (callback != null) {
                        callback.runWithData(sessionsInfo)
                    }
                } else {
                    getSessions(false, callback)
                }
            })
        } else {
            client().send(GetActiveSessions(), Client.ResultHandler { result: TdApi.Object? ->
                when (result!!.getConstructor()) {
                    Sessions.CONSTRUCTOR -> {
                        val sessions = result as Sessions
                        synchronized(dataLock) {
                            this.sessionsInfo = SessionsInfo(sessions)
                        }
                        if (callback != null) {
                            callback.runWithData(this.sessionsInfo)
                        }
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        Log.e("Unable to fetch sessions", TD.toErrorString(result))
                        if (callback != null) {
                            callback.runWithData(null)
                        }
                    }
                }
            })
        }
    }

    fun confirmQrCodeAuthentication(qrLoginUri: String?, onDone: RunnableData<TdApi.Session?>?, onError: RunnableData<TdApi.Error?>?) {
        client().send(ConfirmQrCodeAuthentication(qrLoginUri), Client.ResultHandler { result: TdApi.Object? ->
            when (result!!.getConstructor()) {
                TdApi.Session.CONSTRUCTOR -> {
                    val session = result as TdApi.Session
                    if (onDone != null) {
                        onDone.runWithData(session)
                    }
                    listeners.notifySessionCreatedViaQrCode(session)
                }

                TdApi.Error.CONSTRUCTOR -> {
                    if (onError != null) {
                        onError.runWithData(result as TdApi.Error)
                    }
                }
            }
        })
    }

    fun interface SupergroupUpgradeCallback {
        fun onSupergroupUpgraded(fromChatId: Long, toChatId: Long, error: TdApi.Error?)
    }

    fun upgradeToSupergroup(chatId: Long, callback: SupergroupUpgradeCallback?) {
        client().send(UpgradeBasicGroupChatToSupergroupChat(chatId), Client.ResultHandler { result: TdApi.Object? ->
            when (result!!.getConstructor()) {
                Chat.CONSTRUCTOR -> {
                    val newChatId = (result as Chat).id
                    client().send(GetSupergroupFullInfo(toSupergroupId(newChatId)), Client.ResultHandler { ignored: TdApi.Object? ->
                        if (callback != null) callback.onSupergroupUpgraded(
                            chatId,
                            newChatId,
                            if (ignored!!.getConstructor() == TdApi.Error.CONSTRUCTOR) ignored as TdApi.Error else null
                        )
                        else if (ignored!!.getConstructor() == TdApi.Error.CONSTRUCTOR) UI.showError(ignored)
                    })
                }

                TdApi.Error.CONSTRUCTOR -> {
                    if (callback != null) callback.onSupergroupUpgraded(chatId, chatId, result as TdApi.Error)
                    else UI.showError(result)
                }
            }
        })
    }

    fun interface ChatMemberStatusChangeCallback {
        fun onMemberStatusUpdated(success: Boolean, error: TdApi.Error?, failedToAddMember: FailedToAddMember?)
    }

    private fun refreshChatMemberStatus(
        chatId: Long,
        sender: MessageSender?,
        @ChatMemberStatus.Constructors expectedType: Int,
        match: Boolean,
        callback: ChatMemberStatusChangeCallback?
    ) {
        val retryCount = AtomicInteger()
        val anyError = AtomicReference<TdApi.Error?>()
        send<ChatMember?>(GetChatMember(chatId, sender), object : ResultHandler<ChatMember?> {
            override fun onResult(member: ChatMember?, error: TdApi.Error?) {
                if (member != null) {
                    val statusMatches = member.status.getConstructor() == expectedType
                    val success = statusMatches == match
                    if (success && retryCount.incrementAndGet() <= 3) {
                        runOnTdlibThread(Runnable { send<ChatMember?>(GetChatMember(chatId, sender), this) }, .5 + .5 * retryCount.get(), false)
                    } else {
                        cache().onChatMemberStatusChanged(chatId, member)
                        if (callback != null) {
                            callback.onMemberStatusUpdated(success, anyError.get(), null)
                        }
                    }
                } else {
                    val originalError = anyError.getAndSet(error)
                    if (originalError == null) {
                        send<ChatMember?>(GetChatMember(chatId, sender), this)
                    } else if (callback != null) {
                        callback.onMemberStatusUpdated(false, originalError, null)
                    } else {
                        UI.showError(originalError)
                    }
                }
            }
        })
    }

    fun transferOwnership(chatId: Long, toUserId: Long, password: String?, callback: ChatMemberStatusChangeCallback?) {
        send<TdApi.Ok?>(TransferChatOwnership(chatId, toUserId, password), Tdlib.ResultHandler { ok: TdApi.Ok?, transferError: TdApi.Error? ->
            if (ok != null) {
                val statusChangeCallback: ChatMemberStatusChangeCallback?
                if (callback != null) {
                    val remaining = AtomicInteger(2)
                    val hasFailures = AtomicBoolean(false)
                    val anyError = AtomicReference<TdApi.Error?>()
                    val anyFailure = AtomicReference<FailedToAddMember?>()
                    statusChangeCallback = ChatMemberStatusChangeCallback { success: Boolean, error: TdApi.Error?, failedToAddMember: FailedToAddMember? ->
                        if (error != null) {
                            anyError.set(error)
                        }
                        if (failedToAddMember != null) {
                            anyFailure.set(failedToAddMember)
                        }
                        if (!success) {
                            hasFailures.set(true)
                        }
                        if (remaining.decrementAndGet() == 0) {
                            if (hasFailures.get()) {
                                callback.onMemberStatusUpdated(false, anyError.get(), anyFailure.get())
                            } else {
                                callback.onMemberStatusUpdated(true, null, null)
                            }
                        }
                    }
                } else {
                    statusChangeCallback = null
                }
                refreshChatMemberStatus(chatId, MessageSenderUser(toUserId), TdApi.ChatMemberStatusCreator.CONSTRUCTOR, true, statusChangeCallback)
                refreshChatMemberStatus(chatId, MessageSenderUser(myUserId()), TdApi.ChatMemberStatusCreator.CONSTRUCTOR, false, statusChangeCallback)
            } else {
                if (callback != null) {
                    callback.onMemberStatusUpdated(false, transferError, null)
                } else {
                    UI.showError(transferError)
                }
            }
        })
    }

    fun setChatMemberStatus(
        chatId: Long,
        sender: MessageSender,
        newStatus: ChatMemberStatus?,
        currentStatus: ChatMemberStatus?,
        callback: ChatMemberStatusChangeCallback?
    ) {
        setChatMemberStatus(chatId, sender, newStatus, 0, currentStatus, callback)
    }

    fun setChatMemberStatus(
        chatId: Long,
        sender: MessageSender,
        newStatus: ChatMemberStatus?,
        forwardLimit: Int,
        currentStatus: ChatMemberStatus?,
        callback: ChatMemberStatusChangeCallback?
    ) {
        if (isBasicGroup(chatId) && TD.needUpgradeToSupergroup(newStatus)) {
            val act = Runnable {
                upgradeToSupergroup(chatId, SupergroupUpgradeCallback { oldChatId: Long, newChatId: Long, error: TdApi.Error? ->
                    if (newChatId != 0L) setChatMemberStatusImpl(newChatId, sender, newStatus, 0, currentStatus, callback)
                    else if (callback != null) callback.onMemberStatusUpdated(false, error, null)
                })
            }
            if (forwardLimit > 0 && sender.getConstructor() == MessageSenderUser.CONSTRUCTOR &&
                TD.isMember(newStatus, false) && !TD.isMember(currentStatus, false)
            ) {
                send<FailedToAddMembers?>(
                    AddChatMember(chatId, (sender as MessageSenderUser).userId, forwardLimit),
                    Tdlib.ResultHandler { failedToAddMembers: FailedToAddMembers?, error: TdApi.Error? ->
                        if (failedToAddMembers != null) {
                            if (failedToAddMembers.failedToAddMembers.size == 0) {
                                act.run()
                            } else {
                                if (callback != null) {
                                    ui()!!.post(Runnable { callback.onMemberStatusUpdated(false, null, failedToAddMembers.failedToAddMembers[0]) }
                                    )
                                }
                            }
                        } else if (callback != null) {
                            ui()!!.post(Runnable { callback.onMemberStatusUpdated(false, error, null) }
                            )
                        }
                    })
            } else {
                act.run()
            }
        } else {
            setChatMemberStatusImpl(chatId, sender, newStatus, forwardLimit, currentStatus, callback)
        }
    }

    fun deleteMessages(chatId: Long, messageIds: LongArray?, revoke: Boolean) {
        send<TdApi.Ok?>(DeleteMessages(chatId, messageIds, revoke), typedOkHandler())
    }

    fun deleteMessagesIfOk(chatId: Long, messageIds: LongArray?, revoke: Boolean) {
        send<TdApi.Ok?>(DeleteMessages(chatId, messageIds, revoke), typedOkHandler())
    }

    fun readMessages(chatId: Long, messageIds: LongArray?, source: TdApi.MessageSource?) {
        if (Log.isEnabled(Log.TAG_FCM)) {
            Log.i(Log.TAG_FCM, "Reading messages chatId:%d messageIds:%s", Log.generateSingleLineException(2), chatId, messageIds.contentToString())
        }
        send<TdApi.Ok?>(ViewMessages(chatId, messageIds, source, true), typedOkHandler())
    }

    // TDLib config
    /*public boolean updateCustomLanguageCode (String languageCode) {
    if (this.languageCode.equals(languageCode)) {
      updateLanguageCode();
      return true;
    }
    return false;
  }*/
    fun setLanguage(languagePackInfo: LanguagePackInfo): Boolean {
        return setLanguagePackIdImpl(languagePackInfo.id, true)
    }

    private fun setLanguagePackIdImpl(languagePackId: String, dispatch: Boolean): Boolean {
        if (!languagePackId.equalsOrBothEmpty(languagePackId)) {
            this.languagePackId = languagePackId
            if (dispatch) {
                updateLanguageParameters(client(), false)
            }
            return true
        }
        return false
    }

    fun suggestedLanguagePackId(): String? {
        return suggestedLanguagePackId
    }

    fun suggestedLanguagePackInfo(): LanguagePackInfo? {
        return suggestedLanguagePackInfo
    }

    private fun setSuggestedLanguagePackInfo(languagePackId: String?, languagePackInfo: LanguagePackInfo?) {
        if (this.suggestedLanguagePackId.equalsOrBothEmpty(languagePackId) && this.suggestedLanguagePackInfo == null) {
            this.suggestedLanguagePackInfo = languagePackInfo
            listeners().updateSuggestedLanguageChanged(languagePackId, languagePackInfo)
        }
    }

    fun syncLanguage(info: LanguagePackInfo, callback: RunnableBool?) {
        if (isEmpty(info.baseLanguagePackId)) {
            syncLanguage(info.id, callback)
        } else {
            syncLanguage(info.id, RunnableBool { success: Boolean ->
                if (success) {
                    syncLanguage(info.baseLanguagePackId, callback)
                } else if (callback != null) {
                    callback.runWithBool(false)
                }
            })
        }
    }

    private fun syncLanguage(languagePackId: String, callback: RunnableBool?) {
        send<TdApi.Ok?>(SynchronizeLanguagePack(languagePackId), Tdlib.ResultHandler { ok: TdApi.Ok?, error: TdApi.Error? ->
            var success: Boolean
            if (error != null) {
                Log.e("Unable to synchronize languagePackId %s: %s", languagePackId, TD.toErrorString(error))
                success = languagePackId == Lang.getBuiltinLanguagePackId()
                if (!success) {
                    success = Config.NEED_LANGUAGE_WORKAROUND
                    UI.showError(error)
                    /*if (success = Config.NEED_LANGUAGE_WORKAROUND) {
            UI.showToast("Warning: language not synced. It's temporary issue of current beta version. " + TD.makeErrorString(result), Toast.LENGTH_LONG);
          } else {
            UI.showError(result);
          }*/
                }
            } else {
                Log.v("%s language is successfully synchronized", languagePackId)
                success = true
            }
            if (callback != null) {
                callback.runWithBool(success)
            }
        })
    }

    private fun getStrings(languagePackId: String, keys: Array<String?>, callback: RunnableData<MutableMap<String?, LanguagePackString?>?>?) {
        send<LanguagePackStrings?>(
            GetLanguagePackStrings(languagePackId, keys),
            Tdlib.ResultHandler { languagePackStrings: LanguagePackStrings?, error: TdApi.Error? ->
                if (languagePackStrings != null) {
                    if (callback != null) {
                        val strings = languagePackStrings.strings
                        val map: MutableMap<String?, LanguagePackString?> = HashMap<String?, LanguagePackString?>(strings.size)
                        for (string in strings) {
                            if (string.value.getConstructor() != TdApi.LanguagePackStringValueDeleted.CONSTRUCTOR) map.put(string.key, string)
                        }
                        callback.runWithData(map)
                    }
                } else {
                    Log.e("Failed to fetch %d strings: %s, languagePackId: %s", keys.size, TD.toErrorString(error), languagePackId)
                    if (callback != null) {
                        callback.runWithData(null)
                    }
                }
            })
    }

    fun getStrings(info: LanguagePackInfo, keys: Array<String?>, callback: RunnableData<MutableMap<String?, LanguagePackString?>?>?) {
        if (isEmpty(info.baseLanguagePackId)) {
            getStrings(info.id, keys, callback)
        } else {
            getStrings(info.id, keys, RunnableData { result: MutableMap<String?, LanguagePackString?>? ->
                val keySet: MutableSet<String> = HashSet<String>(keys.size)
                Collections.addAll<String?>(keySet, *keys)
                if (result != null) keySet.removeAll(result.keys)
                if (!keySet.isEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    val missingKeys = keySet.toTypedArray() as Array<String?>
                    getStrings(info.baseLanguagePackId, missingKeys, RunnableData { missingResult: MutableMap<String?, LanguagePackString?>? ->
                        if (callback != null) {
                            if (result == null && missingResult == null) {
                                callback.runWithData(null)
                            } else if (result == null) {
                                callback.runWithData(missingResult)
                            } else {
                                if (missingResult != null) result.putAll(missingResult)
                                callback.runWithData(result)
                            }
                        }
                    })
                }
            })
        }
    }

    private fun updateLanguageParameters(client: Client, isInitialization: Boolean) {
        if (isInitialization) {
            this.languagePackId = instance().languagePackInfo.id
            client.send(SetOption("language_pack_database_path", OptionValueString(context.languageDatabasePath())), okHandler())
            client.send(SetOption("localization_target", OptionValueString(BuildConfig.LANGUAGE_PACK)), okHandler())
        }
        client.send(SetOption("language_pack_id", OptionValueString(languagePackId)), okHandler())
    }

    fun applyLanguage(languagePack: LanguagePackInfo, callback: RunnableBool?, needSync: Boolean) {
        val act = Runnable {
            send<TdApi.Ok?>(SetOption("language_pack_id", OptionValueString(languagePack.id)), Tdlib.ResultHandler { ok: TdApi.Ok?, error: TdApi.Error? ->
                ui()!!.post(
                    Runnable {
                        if (ok != null) {
                            Lang.changeLanguage(languagePack)
                            if (callback != null) callback.runWithBool(true)
                        } else {
                            UI.showError(error)
                            if (callback != null) callback.runWithBool(false)
                        }
                    })
            })
        }
        if (needSync && !TD.isLocalLanguagePackId(languagePack.id)) {
            syncLanguage(languagePack, RunnableBool { success: Boolean ->
                if (success) {
                    act.run()
                } else {
                    if (callback != null) {
                        ui()!!.post(Runnable { callback.runWithBool(false) })
                    }
                }
            })
        } else {
            act.run()
        }
    }

    private fun updateNotificationParameters(client: Client) {
        val notificationGroupCountMax: Int
        val notificationGroupSizeMax: Int

        if (Config.FORCE_DISABLE_NOTIFICATIONS || this.isServiceInstance) {
            // Disable Notifications API if we are running experimental build
            notificationGroupCountMax = 0
            notificationGroupSizeMax = 1
        } else {
            notificationGroupCountMax = 25
            notificationGroupSizeMax = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 7 else 10
        }

        val okHandler = okHandler()
        client.send(SetOption("notification_group_count_max", OptionValueInteger(notificationGroupCountMax.toLong())), okHandler)
        client.send(SetOption("notification_group_size_max", OptionValueInteger(notificationGroupSizeMax.toLong())), okHandler)
    }

    private var lastReportedConnectionParams: String? = null

    fun checkConnectionParams() {
        checkConnectionParams(client(), false)
    }

    private val registeredDeviceToken: DeviceToken?
        get() {
            val deviceToken = context.getToken()
            return if (deviceToken == null) instance().deviceToken else deviceToken
        }

    fun safetyNetApiKey(): String {
        // TODO: server config
        return BuildConfig.SAFETYNET_API_KEY
    }

    fun phoneNumberAuthenticationSettings(context: Context?): PhoneNumberAuthenticationSettings {
        var firebaseAuthenticationSettings: FirebaseAuthenticationSettings? = null
        val safetyNetApiKey = safetyNetApiKey()
        if (isEmpty(safetyNetApiKey)) {
            TDLib.Tag.safetyNet("Ignoring Firebase authentication, because SafetyNet API_KEY is unset")
        } else if (Config.REQUIRE_FIREBASE_SERVICES_FOR_SAFETYNET && !U.isGooglePlayServicesAvailable(context)) {
            TDLib.Tag.safetyNet("Ignoring Firebase authentication, because Firebase services are unavailable")
        } else {
            TDLib.Tag.safetyNet("Enabling Firebase authentication for the next request")
            firebaseAuthenticationSettings = FirebaseAuthenticationSettingsAndroid()
        }
        return PhoneNumberAuthenticationSettings(
            false,  // TODO transparently request permission & enter flash call
            true,
            false,  // TODO check if passed phone number is inserted in the current phone
            true,  // TODO check current phone number when possible
            false,  // TODO for faster login when SMS method is chosen
            firebaseAuthenticationSettings,
            instance().getAuthenticationTokens()
        )
    }

    private fun newConnectionParams(): MutableMap<String, Any?> {
        val params: MutableMap<String, Any?> = LinkedHashMap<String, Any?>()
        if (this.isServiceInstance) {
            params.put("device_token", "HIDDEN")
        } else {
            var state = context().getTokenState()
            val deviceToken = this.registeredDeviceToken
            if (deviceToken != null && (state == TdlibManager.TokenState.NONE || state == TdlibManager.TokenState.INITIALIZING)) {
                state = TdlibManager.TokenState.OK
            }
            val tokenProvider = TdlibNotificationUtils.getDeviceTokenRetriever().name
            val error = context().getTokenError()
            when (state) {
                TdlibManager.TokenState.ERROR -> {
                    params.put("device_token", tokenProvider.uppercase(Locale.getDefault()) + "_ERROR")
                    if (!isEmpty(error)) {
                        params.put(tokenProvider + "_error", error)
                    }
                }

                TdlibManager.TokenState.INITIALIZING -> {
                    params.put("device_token", tokenProvider.uppercase(Locale.getDefault()) + "_INITIALIZING")
                }

                TdlibManager.TokenState.OK -> {
                    var tokenOrEndpoint: String
                    when (requireNonNull<DeviceToken?>(deviceToken)!!.getConstructor()) {
                        DeviceTokenFirebaseCloudMessaging.CONSTRUCTOR -> tokenOrEndpoint = (deviceToken as DeviceTokenFirebaseCloudMessaging).token
                        DeviceTokenHuaweiPush.CONSTRUCTOR -> {
                            tokenOrEndpoint = (deviceToken as DeviceTokenHuaweiPush).token
                            val huaweiTokenPrefix = "huawei://"
                            if (!tokenOrEndpoint.startsWith(huaweiTokenPrefix)) {
                                tokenOrEndpoint = huaweiTokenPrefix + tokenOrEndpoint
                            }
                        }

                        DeviceTokenSimplePush.CONSTRUCTOR -> tokenOrEndpoint = (deviceToken as DeviceTokenSimplePush).endpoint
                        else -> {
                            assertDeviceToken_de4a4f61()
                            throw unsupported(deviceToken!!)
                        }
                    }
                    params.put("device_token", tokenOrEndpoint)
                }

                TdlibManager.TokenState.NONE -> {}
                else -> throw IllegalStateException(state.toString())
            }
        }
        val timeZoneOffset: Long = timeZoneOffset()
        val context = UI.getAppContext()
        params.put("package_id", context.getPackageName())
        val installerName = getInstallerPackageName(context)
        if (!isEmpty(installerName)) {
            params.put("installer", installerName)
        }
        val initiatorName = getInitiatorPackageName(context)
        if (!isEmpty(initiatorName) && initiatorName != installerName) {
            params.put("initiator", initiatorName)
        }
        if (BuildConfig.DEBUG) {
            params.put("debug", true)
        }
        val fingerprint = U.getApkFingerprint("SHA1", false)
        if (!isEmpty(fingerprint)) {
            params.put("data", fingerprint)
        }
        params.put("tz_offset", timeZoneOffset)
        params.put("recaptcha", BuildConfig.RECAPTCHA_VERSION)

        val git: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
        git.put("remote", BuildConfig.REMOTE_URL.replace("^(https?://)?github\\.com/".toRegex(), ""))
        git.put("commit", BuildConfig.COMMIT)
        git.put("tdlib", tdlibCommitHash())
        git.put("date", BuildConfig.COMMIT_DATE)
        var pullRequests: MutableList<MutableMap<String?, Any?>?>? = null
        for (i in BuildConfig.PULL_REQUEST_ID.indices) {
            val pr: MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>()
            pr.put("id", BuildConfig.PULL_REQUEST_ID[i])
            pr.put("commit", BuildConfig.PULL_REQUEST_COMMIT[i])
            pr.put("date", BuildConfig.PULL_REQUEST_COMMIT_DATE[i])
            if (pullRequests == null) {
                pullRequests = ArrayList<MutableMap<String?, Any?>?>()
            }
            pullRequests.add(pr)
        }
        if (pullRequests != null) {
            git.put("prs", pullRequests)
        }
        params.put("git", git)

        return params
    }

    private fun updateUtcTimeOffset() {
        performOptional(RunnableData { client: Client? ->
            val timeZoneOffset: Long = timeZoneOffset()
            if (options.utcTimeOffset != timeZoneOffset) {
                client!!.send(SetOption("utc_time_offset", OptionValueInteger(timeZoneOffset)), silentHandler())
            }
        }, null)
    }

    private fun checkConnectionParams(client: Client, force: Boolean) {
        val params = newConnectionParams()
        val connectionParams: String? = stringify(toObject(params))
        if (connectionParams != null && (force || !lastReportedConnectionParams.equalsOrBothEmpty(connectionParams))) {
            this.lastReportedConnectionParams = connectionParams
            client.send(SetOption("connection_parameters", OptionValueString(connectionParams)), okHandler())
        }
    }

    private var tdlibThread: Thread? = null

    private fun updateParameters(client: Client) {
        val okHandler = Client.ResultHandler { `object`: TdApi.Object? ->
            updateTdlibThread()
            when (`object`!!.getConstructor()) {
                TdApi.Ok.CONSTRUCTOR -> {}
                TdApi.Error.CONSTRUCTOR -> UI.showError(`object`)
            }
        }
        val isService = this.isServiceInstance
        client.send(SetOption("use_quick_ack", OptionValueBoolean(true)), okHandler)
        client.send(SetOption("use_pfs", OptionValueBoolean(true)), okHandler)
        client.send(SetOption("is_emulator", OptionValueBoolean(instance().isEmulator.also { isEmulator = it })), okHandler)
        if (isService) {
            updateNotificationParameters(client)
        } else {
            updateLanguageParameters(client, true)
            updateNotificationParameters(client)
            client.send(SetOption("storage_max_files_size", OptionValueInteger(Int.MAX_VALUE.toLong())), okHandler)
            client.send(SetOption("ignore_default_disable_notification", OptionValueBoolean(true)), okHandler)
            client.send(SetOption("ignore_platform_restrictions", OptionValueBoolean(isAppSideLoaded(UI.getAppContext()))), okHandler)
            client.send(SetOption("process_pinned_messages_as_mentions", OptionValueBoolean(true)), okHandler)
        }
        checkConnectionParams(client, true)

        if (needDropNotificationIdentifiers) {
            client.send(SetOption("drop_notification_ids", OptionValueBoolean(true)), Client.ResultHandler { result: TdApi.Object? ->
                notifications().onDropNotificationData(true)
            })
            needDropNotificationIdentifiers = false
        }

        parameters.useTestDc = this.isDebugInstance
        parameters.databaseDirectory = TdlibManager.getTdlibDirectory(accountId, false)
        parameters.filesDirectory = TdlibManager.getTdlibDirectory(accountId, !isService)
        parameters.systemLanguageCode = TdlibManager.getSystemLanguageCode()
        parameters.deviceModel = TdlibManager.getDeviceModel()
        parameters.systemVersion = TdlibManager.getSystemVersion()
    }

    private var applicationConfigJson: String?

    private fun setApplicationConfig(config: TdApi.JsonValue?, json: String?) {
        if (applicationConfigJson == null || applicationConfigJson != json) {
            applicationConfigJson = json
            settings().setApplicationConfig(json)
            if (config != null) {
                options.handleApplicationConfig(config)
            }
        }
    }

    fun hasUrgentInAppUpdate(): Boolean {
        return options.forceInAppUpdate
    }

    fun language(): String? {
        return parameters.systemLanguageCode
    }

    fun timeElapsedSinceDate(unixTime: Long, unit: TimeUnit): Long {
        val now = System.currentTimeMillis()
        val unixTimeMs = unit.toMillis(unixTime)
        return max(now - unixTimeMs, 0)
    }

    fun currentTimeMillis(): Long {
        return options.unixTime.currentTimeMillis()
    }

    fun currentTime(unit: TimeUnit): Long {
        return unit.convert(currentTimeMillis(), TimeUnit.MILLISECONDS)
    }

    fun toTdlibTimeMillis(systemTime: Long, unit: TimeUnit): Long {
        return unit.toMillis(systemTime) + (currentTimeMillis() - System.currentTimeMillis())
    }

    fun toSystemTimeMillis(tdlibTime: Long, unit: TimeUnit): Long {
        return unit.toMillis(tdlibTime) + (System.currentTimeMillis() - currentTimeMillis())
    }

    private var effectiveProxyId = Settings.PROXY_ID_UNKNOWN

    @TdlibThread
    private fun setEffectiveProxyId(proxyId: Int) {
        if (this.effectiveProxyId != proxyId) {
            val hadProxy = this.effectiveProxyId > Settings.PROXY_ID_NONE
            val hasProxy = proxyId > Settings.PROXY_ID_NONE
            this.effectiveProxyId = proxyId
            if (hadProxy != hasProxy && connectionState == ConnectionState.CONNECTING) {
                notifyConnectionDisplayStatusChanged()
            }
        }
    }

    fun disableProxy() {
        setProxy(Settings.PROXY_ID_NONE, null)
    }

    fun setProxy(proxyId: Int, proxy: TdApi.Proxy?) {
        if (proxy != null) {
            send<AddedProxy?>(AddProxy(proxy, true, ""), Tdlib.ResultHandler { addedProxy: AddedProxy?, error: TdApi.Error? ->
                if (addedProxy != null) {
                    setEffectiveProxyId(proxyId)
                }
            })
        } else {
            send<TdApi.Ok?>(DisableProxy(), Tdlib.ResultHandler { ok: TdApi.Ok?, error: TdApi.Error? ->
                if (ok != null) {
                    setEffectiveProxyId(Settings.PROXY_ID_NONE)
                }
            })
        }
    }

    fun cleanupProxies() {
        send<AddedProxies?>(GetProxies(), Tdlib.ResultHandler { addedProxies: AddedProxies?, error: TdApi.Error? ->
            if (addedProxies != null) {
                for (addedProxy in addedProxies.proxies) {
                    if (!addedProxy.isEnabled) {
                        send<TdApi.Ok?>(RemoveProxy(addedProxy.id), typedOkHandler())
                    }
                }
            }
        })
    }

    fun removeProxies(excludeProxyId: Int) {
        send<AddedProxies?>(GetProxies(), Tdlib.ResultHandler { addedProxies: AddedProxies?, error: TdApi.Error? ->
            if (addedProxies != null) {
                for (addedProxy in addedProxies.proxies) {
                    if (addedProxy.id != excludeProxyId) {
                        send<TdApi.Ok?>(RemoveProxy(addedProxy.id), typedOkHandler())
                    }
                }
            }
        })
    }

    fun getProxyLink(proxy: Settings.Proxy, callback: RunnableData<String?>) {
        requireNotNull(proxy.proxy)
        send<TdApi.HttpUrl?>(GetInternalLink(InternalLinkTypeProxy(proxy.proxy), true), Tdlib.ResultHandler { httpUrl: TdApi.HttpUrl?, error: TdApi.Error? ->
            val url: String?
            if (error != null) {
                Log.e("Proxy link unavailable: %s", TD.toErrorString(error))
                url = null
            } else {
                url = httpUrl!!.url
            }
            ui()!!.post(Runnable { callback.runWithData(url) })
        })
    }

    @AnyThread
    private fun notifyPingValueChanged(proxy: Settings.Proxy) {
        val pingMs = proxy.pingMs
        ui()!!.post(Runnable { context().global().notifyProxyPingChanged(proxy, pingMs) }
        )
    }

    fun pingProxy(proxy: Settings.Proxy, after: RunnableLong?) {
        val proxyId = proxy.id
        val pingId = ++proxy.pingCount
        proxy.pingMs = Settings.PROXY_TIME_LOADING.toLong()
        proxy.pingErrorCount = 0
        notifyPingValueChanged(proxy)
        val function = if (proxyId != Settings.PROXY_ID_NONE) AddProxy(proxy.proxy, false, "") else PingProxy(null)
        val uptimeMillis = AtomicLong(SystemClock.uptimeMillis())
        client().send(function, object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                if (pingId != proxy.pingCount) {
                    return
                }
                val now = SystemClock.uptimeMillis()
                val elapsed = now - uptimeMillis.getAndSet(now)
                val pingMs: Long
                when (result.getConstructor()) {
                    TdApi.Ok.CONSTRUCTOR -> {
                        client().send(function, this)
                        return
                    }

                    AddedProxy.CONSTRUCTOR -> {
                        val addedProxy = result as AddedProxy
                        client().send(PingProxy(addedProxy.proxy), this)
                        return
                    }

                    TdApi.Seconds.CONSTRUCTOR -> {
                        val timestampMs = currentTimeMillis()
                        pingMs = Math.round((result as TdApi.Seconds).seconds * 1000.0)
                        proxy.pingErrorCount = 0
                        instance().trackSuccessfulConnection(proxyId, timestampMs, pingMs, true)
                        if (routeSelector != null) {
                            routeSelector!!.markAsSuccessful(proxyId)
                        }
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        if (++proxy.pingErrorCount < 3 && elapsed <= 1000) {
                            client().send(SetAlarm(0.35 * proxy.pingErrorCount), this)
                            return
                        }
                        pingMs = Settings.PROXY_TIME_EMPTY.toLong()
                        proxy.pingError = result as TdApi.Error
                    }

                    else -> throw UnsupportedOperationException(result.toString())
                }
                proxy.pingMs = pingMs
                notifyPingValueChanged(proxy)
                if (after != null) {
                    after.runWithLong(pingMs)
                }
            }
        })
        // return pingId;
    }

    private fun newClient(): ClientHolder {
        Log.i(
            "Creating TDLib client, hasInstance:%b, accountId:%d, debug:%b, wasPaused:%b", client != null, accountId,
            this.isDebugInstance, instancePaused
        )
        instancePaused = false
        return ClientHolder(this)
    }

    val isPaused: Boolean
        get() {
            synchronized(clientLock) {
                return client == null || instancePaused
            }
        }

    fun initializeIfWaiting() {
        clientHolder().init()
    }

    private var networkType: TdApi.NetworkType? = null

    fun setNetworkType(networkType: TdApi.NetworkType) {
        this.networkType = networkType
        performOptional(RunnableData { client: Client? -> client!!.send(SetNetworkType(networkType), okHandler()) }, null)
        listeners().updateConnectionType(networkType)
    }

    val isWaitingForNetwork: Boolean
        get() = (networkType != null && networkType!!.getConstructor() == TdApi.NetworkTypeNone.CONSTRUCTOR) || context.watchDog().isWaitingForNetwork()

    fun resendNetworkTypeIfNeeded(networkType: TdApi.NetworkType) {
        if (connectionState != ConnectionState.CONNECTED) {
            setNetworkType(networkType)
        }
    }

    // Options
    fun isStickerFavorite(stickerId: Int): Boolean {
        synchronized(dataLock) {
            return favoriteStickerIds != null && indexOf(favoriteStickerIds, stickerId) != -1
        }
    }

    fun canSetPasscode(chat: Chat?): Boolean {
        return chat != null && chat.type.getConstructor() == ChatTypeSecret.CONSTRUCTOR
    }

    fun hasPasscode(chatId: Long): Boolean {
        return hasPasscode(chat(chatId))
    }

    fun hasPasscode(chat: Chat?): Boolean {
        return chat != null && !isEmpty(chat.clientData)
    }

    fun setPasscode(chat: Chat?, passcode: ChatPasscode?) {
        if (chat != null) {
            if (passcode != null && Passcode.isValidMode(passcode.mode)) {
                setChatClientData(chat, passcode.toString())
            } else {
                setChatClientData(chat, null)
            }
        }
    }

    fun chatPasscode(chat: Chat?): ChatPasscode? {
        if (chat == null) {
            return null
        }
        var clientData = chat.clientData
        if (isEmpty(clientData)) {
            return null
        }
        var data = clientData.split("_".toRegex(), limit = 2).toTypedArray()
        if (data.size != 2) {
            return null
        }
        var version = parseInt(data[0])
        if (version < 0 || version > CLIENT_DATA_VERSION) {
            return null
        }
        if (version < CLIENT_DATA_VERSION) {
            version = version + 1
            while (version <= CLIENT_DATA_VERSION) {
                clientData = upgradeChatClientData(chat, version)
                version++
            }
            setChatClientData(chat, clientData)
            if (isEmpty(clientData)) {
                return null
            }
            data = clientData.split("_".toRegex(), limit = 2).toTypedArray()
            if (data.size != 2 || parseInt(data[0]) != CLIENT_DATA_VERSION) {
                return null
            }
        }
        try {
            val arg = data[1]
            var i = arg.indexOf('_')
            val mode = parseInt(arg.substring(0, i))
            if (!Passcode.isValidMode(mode)) {
                return null
            }
            var startIndex = i + 1
            i = arg.indexOf('_', startIndex)
            val flags = parseInt(arg.substring(startIndex, i))
            startIndex = i + 1
            i = arg.indexOf('_', startIndex)
            val passcodeHashLength = parseInt(arg.substring(startIndex, i))
            if (passcodeHashLength < 0) {
                return null
            }
            startIndex = i + 1
            val passcodeHash = arg.substring(startIndex, startIndex + passcodeHashLength)
            var fingerHash: String? = null
            if (arg.length > startIndex + passcodeHashLength) {
                startIndex = startIndex + passcodeHashLength + 1
                i = arg.indexOf('_', startIndex)
                val fingerHashLength = parseInt(arg.substring(startIndex, i))
                if (fingerHashLength > 0) {
                    fingerHash = arg.substring(i + 1, i + 1 + fingerHashLength)
                }
            }
            return ChatPasscode(mode, flags, passcodeHash, fingerHash)
        } catch (t: Throwable) {
            Log.w("Unable to parse clientData", t)
        }
        return null
    }

    private fun upgradeChatClientData(chat: Chat, version: Int): String {
        when (version) {
        }
        throw RuntimeException("version: " + version + ", clientData: " + chat.clientData)
    }

    private fun setChatClientData(chat: Chat?, data: String?) {
        if (chat == null) {
            return
        }
        synchronized(dataLock) {
            if (chat.clientData.equalsOrBothEmpty(data)) {
                return
            }
            chat.clientData = data
        }
        val chatId = chat.id
        client().send(SetChatClientData(chatId, data), Client.ResultHandler { `object`: TdApi.Object? ->
            when (`object`!!.getConstructor()) {
                TdApi.Ok.CONSTRUCTOR -> {
                    listeners.updateChatClientDataChanged(chatId, data)
                }

                TdApi.Error.CONSTRUCTOR -> {
                    Log.e("Cannot set clientData: %s, chatId:%d, clientData:%s", TD.toErrorString(`object`), chatId, data)
                    UI.showError(`object`)
                }
            }
        })
    }

    private var isOnline = false

    fun isOnline(): Boolean {
        return isOnline
    }

    fun setOnline(isOnline: Boolean) {
        if (this.isOnline != isOnline) {
            this.isOnline = isOnline
            Log.i("SetOnline accountId:%d -> %b", accountId, isOnline)
            if (Config.NEED_ONLINE) {
                performOptional(RunnableData { client: Client? -> client!!.send(SetOption("online", OptionValueBoolean(isOnline)), okHandler()) }, null)
            }
            // cache().setPauseStatusRefreshers(!isOnline);
        }
    }

    private var isEmulator = false

    fun setIsEmulator(isEmulator: Boolean) {
        if (this.isEmulator != isEmulator) {
            this.isEmulator = isEmulator
            performOptional(RunnableData { client: Client? -> client!!.send(SetOption("is_emulator", OptionValueBoolean(isEmulator)), okHandler()) }, null)
        }
    }

    fun isEmulator(): Boolean {
        return isEmulator
    }

    fun sync(pushId: Long, after: Runnable?, needNotifications: Boolean, needNetworkRequest: Boolean) {
        TDLib.Tag.notifications(
            pushId,
            accountId,
            "Performing sync needNotification: %b, needNetworkRequest: %b, hasAfter: %b. Awaiting connection. Connection state: %d, status: %d",
            needNotifications,
            needNetworkRequest,
            after != null,
            connectionState,
            authorizationStatus()
        )
        incrementReferenceCount(REFERENCE_TYPE_SYNC)
        val onDone = Runnable {
            if (after != null) {
                if (needNotifications) {
                    TDLib.Tag.notifications(pushId, accountId, "Making sure havePendingNotifications is false.")
                    awaitNotifications(Runnable {
                        TDLib.Tag.notifications(pushId, accountId, "Sync task finished.")
                        after.run()
                    })
                } else {
                    TDLib.Tag.notifications(pushId, accountId, "Sync task finished.")
                    after.run()
                }
            } else {
                TDLib.Tag.notifications(pushId, accountId, "Sync task finished, but there's no callback.")
            }
            decrementReferenceCount(REFERENCE_TYPE_SYNC)
        }
        if (Config.NEED_NETWORK_SYNC_REQUEST || needNetworkRequest) {
            awaitConnection(Runnable {
                TDLib.Tag.notifications(pushId, accountId, "Connection available. Performing network request to make sure it's still active.")
                performOptional(RunnableData { client: Client? ->
                    client!!.send(GetCountryCode(), Client.ResultHandler { ignored: TdApi.Object? -> onDone.run() })
                }, onDone)
            })
        } else {
            awaitConnection(onDone)
        }
    }

    fun notifyPushProcessingTakesTooLong(pushId: Long): Boolean { // Called from Firebase thread
        val authorizationState = this.authorizationState
        if (authorizationState != null) {
            when (authorizationState.getConstructor()) {
                TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR, TdApi.AuthorizationStateClosing.CONSTRUCTOR ->           // Make sure action finishes even if it causes ANR.
                    return false
            }
        }
        notifications().notifyPushProcessingTakesTooLong()
        return true
    }

    fun processPushOrSync(pushId: Long, payload: String?, after: Runnable?) {
        TDLib.Tag.notifications(pushId, accountId, "Started processing push notification, hasAfter:%b", after != null)
        incrementNotificationReferenceCount()
        send<TdApi.Ok?>(ProcessPushNotification(payload), Tdlib.ResultHandler { ok: TdApi.Ok?, error: TdApi.Error? ->
            val notificationChecker = Runnable {
                TDLib.Tag.notifications(pushId, accountId, "Making sure all notifications displayed")
                incrementNotificationReferenceCount()
                if (after != null) {
                    notifications().releaseTdlibReference(Runnable {
                        TDLib.Tag.notifications(pushId, accountId, "Making sure we're not in AuthorizationStateLoggingOut")
                        awaitClose(Runnable {
                            TDLib.Tag.notifications(pushId, accountId, "Finished processing push. Invoking after()")
                            after.run()
                        }, false)
                    })
                } else {
                    notifications().releaseTdlibReference(Runnable {
                        TDLib.Tag.notifications(pushId, accountId, "All notifications displayed. But there's no after() callback.")
                    })
                }
            }
            if (ok != null) {
                TDLib.Tag.notifications(
                    pushId,
                    accountId,
                    "Ensuring updateActiveNotifications was sent. ignoreNotificationUpdates:%b, receivedActiveNotificationsTime:%d, receivedActiveNotificationsIgnored: %b",
                    ignoreNotificationUpdates,
                    receivedActiveNotificationsTime,
                    receivedActiveNotificationsIgnored
                )
                awaitNotificationInitialization(notificationChecker)
            } else if (error != null) {
                if (error.code == 401) {
                    TDLib.Tag.notifications(pushId, accountId, "TDLib tells to expect AuthorizationStateLoggingOut: %s, waiting.", error)
                    awaitClose(Runnable {
                        if (after != null) {
                            TDLib.Tag.notifications(pushId, accountId, "Finished processing push. Invoking after()")
                            after.run()
                        } else {
                            TDLib.Tag.notifications(pushId, accountId, "All notifications displayed. But there's no after() callback.")
                        }
                    }, true)
                } else {
                    TDLib.Tag.notifications(pushId, accountId, "Failed to process push: %s, performing full sync.", TD.toErrorString(error))
                    setHasUnprocessedPushes(true)
                    sync(pushId, Runnable {
                        setHasUnprocessedPushes(false)
                        notificationChecker.run()
                    }, true, false)
                }
            }
            decrementNotificationReferenceCount()
        })
    }

    private fun reloadOption(name: String?) {
        send<TdApi.OptionValue?>(GetOption(name), Tdlib.ResultHandler { value: TdApi.OptionValue?, error: TdApi.Error? ->
            if (value != null) {
                sendFakeUpdate(UpdateOption(name, value))
            }
        })
    }

    fun disableContactRegisteredNotifications(allowRequest: Boolean): Boolean {
        if (allowRequest) {
            reloadOption("disable_contact_registered_notifications")
        }
        return options.disableContactRegisteredNotifications
    }

    fun setDisableContactRegisteredNotifications(disable: Boolean) {
        if (options.disableContactRegisteredNotifications != disable) {
            options.disableContactRegisteredNotifications = disable
            send<TdApi.Ok?>(SetOption("disable_contact_registered_notifications", OptionValueBoolean(disable)), typedOkHandler())
            listeners().updateContactRegisteredNotificationsDisabled(disable)
        }
    }

    /*public boolean disablePinnedMessageNotifications () {
    return disablePinnedMessageNotifications;
  }

  public void setDisablePinnedMessageNotifications (boolean disable) {
    if (this.disablePinnedMessageNotifications != disable) {
      this.disablePinnedMessageNotifications = disable;
      client().send(new TdApi.SetOption("disable_pinned_message_notifications", new TdApi.OptionValueBoolean(disable)), okHandler);
    }
  }*/
    fun options(): TdlibOptions {
        return options
    }

    fun canIgnoreSensitiveContentRestriction(): Boolean {
        return options.canIgnoreSensitiveContentRestrictions
    }

    fun ignoreSensitiveContentRestrictions(): Boolean {
        return options.ignoreSensitiveContentRestrictions
    }

    fun setIgnoreSensitiveContentRestrictions(ignoreSensitiveContentRestrictions: Boolean) {
        if (options.ignoreSensitiveContentRestrictions != ignoreSensitiveContentRestrictions) {
            options.ignoreSensitiveContentRestrictions = ignoreSensitiveContentRestrictions
            send<TdApi.Ok?>(SetOption("ignore_sensitive_content_restrictions", OptionValueBoolean(ignoreSensitiveContentRestrictions)), typedOkHandler())
        }
    }

    fun uniqueSuffix(): String {
        return accountId.toString() + "." + options.authorizationDate
    }

    fun uniqueSuffix(id: Long): String {
        return accountId.toString() + "." + options.authorizationDate + "." + id
    }

    fun supergroupSizeMax(): Int {
        return options.supergroupSizeMax
    }

    fun maxBioLength(): Int {
        return options.bioLengthMax
    }

    fun forwardMaxCount(): Int {
        return options.forwardedMessageCountMax
    }

    fun basicGroupSizeMax(): Int {
        return options.basicGroupSizeMax
    }

    fun pinnedChatsMaxCount(): Int {
        return options.pinnedChatCountMax
    }

    fun pinnedArchivedChatsMaxCount(): Int {
        return options.pinnedArchivedChatCountMax
    }

    fun emojiesAnimatedZoom(): Double {
        return options.emojiesAnimatedZoom.toDouble()
    }

    fun youtubePipEnabled(): Boolean {
        return !options.youtubePipDisabled || isAppSideLoaded(UI.getAppContext())
    }

    fun rtcServers(): List<RtcServer> {
        return options.rtcServers
    }

    fun autoArchiveAvailable(): Boolean {
        return options.canArchiveAndMuteNewChatsFromUnknownUsers
    }

    fun canSetNewChatPrivacySettings(): Boolean {
        return options.canSetNewChatPrivacySettings
    }

    fun tMeUrl(): String {
        return if (isEmpty(options.tMeUrl)) "https://" + TME_HOSTS[0] + "/" else options.tMeUrl
    }

    fun tMeMessageUrl(username: String?, messageId: Long): String {
        return tMeUrl(username + "/" + messageId)
    }

    fun tMePrivateMessageUrl(supergroupId: Long, messageId: Long): String {
        return tMeUrl("c/" + supergroupId + "/" + messageId)
    }

    fun tMeUrlBuilder(): Uri.Builder? {
        return Uri.Builder()
            .scheme("https")
            .authority(tMeAuthority())
    }

    @JvmOverloads
    fun tMeUrl(usernames: Usernames, excludeProtocol: Boolean = false): String {
        return tMeUrl(usernames.primaryUsername(), excludeProtocol)
    }

    @JvmOverloads
    fun tMeUrl(path: String?, excludeProtocol: Boolean = false): String {
        val result = tMeUrlBuilder()!!
            .path(path)
            .build()
            .toString()
        if (excludeProtocol) {
            val i = result.indexOf("://")
            if (i != -1) {
                return result.substring(i + "://".length)
            }
        }
        return result
    }

    fun tMeStartUrl(botUsername: String?, parameter: String?, inGroup: Boolean): String {
        return tMeUrlBuilder()!!
            .path(botUsername)
            .appendQueryParameter(if (inGroup) "startgroup" else "start", parameter)
            .build()
            .toString()
    }

    fun tMeChatUrl(chatId: Long): String? {
        val username = chatUsername(chatId)
        if (!isEmpty(username)) {
            return tMeUrl(username)
        }
        if (tgx.td.isSupergroup(chatId)) {
            return tMeUrl("c/" + toSupergroupId(chatId))
        }
        return null
    }

    fun tMeBackgroundUrl(backgroundId: String?): String {
        return tMeUrl("bg/" + backgroundId)
    }

    fun tMeStickerSetUrl(stickerSetName: String?): String {
        return tMeUrl("addstickers/" + stickerSetName)
    }

    fun tMeLanguageUrl(languagePackId: String?): String {
        return tMeUrl("setlanguage/" + languagePackId)
    }

    fun tMeStickerSetUrl(stickerSetInfo: StickerSetInfo): String {
        when (stickerSetInfo.stickerType.getConstructor()) {
            TdApi.StickerTypeCustomEmoji.CONSTRUCTOR -> return tMeUrl("addemoji/" + stickerSetInfo.name)
            TdApi.StickerTypeMask.CONSTRUCTOR, TdApi.StickerTypeRegular.CONSTRUCTOR -> return tMeUrl("addstickers/" + stickerSetInfo.name)
            else -> {
                assertStickerType_cc811bb7()
                throw unsupported(stickerSetInfo.stickerType)
            }
        }
    }

    fun tMeGiftCodeUrl(giftCode: String): String {
        return tMeUrl("giftcode/" + giftCode)
    }

    fun tMeHost(): String {
        return urlWithoutProtocol(tMeUrl())
    }

    fun tMeAuthority(): String? {
        return Uri.parse(options.tMeUrl).getHost()
    }

    fun areTopChatsDisabled(): Boolean {
        return options.disableTopChats
    }

    fun setDisableTopChats(disableTopChats: Boolean) {
        options.disableTopChats = disableTopChats
        send<TdApi.Ok?>(SetOption("disable_top_chats", OptionValueBoolean(disableTopChats)), typedOkHandler())
    }

    fun areSentScheduledMessageNotificationsDisabled(): Boolean {
        return options.disableSentScheduledMessageNotifications
    }

    fun setDisableSentScheduledMessageNotifications(disable: Boolean) {
        options.disableSentScheduledMessageNotifications = disable
        send<TdApi.Ok?>(SetOption("disable_sent_scheduled_message_notifications", OptionValueBoolean(disable)), typedOkHandler())
    }

    val animationSearchBotUsername: String
        get() = options.animationSearchBotUsername

    val venueSearchBotUsername: String
        get() = options.venueSearchBotUsername

    val photoSearchBotUsername: String
        get() = options.photoSearchBotUsername

    fun isTmeUrl(url: String): Boolean {
        var url = url
        if (isEmpty(url)) return false
        if (url.startsWith("tg://")) return true
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url
            return isKnownHost(Uri.parse(url).getHost(), false)
        } catch (ignored: Throwable) {
            return false
        }
    }

    fun getWallpaperData(url: String?): String? {
        if (isEmpty(url)) return null
        try {
            var uri = Uri.parse(url)
            if ("tg" == uri.getScheme()) {
                if ("bg" != uri.getHost()) return null
                var data = uri.getQueryParameter("slug")
                if (!isEmpty(data)) {
                    val bgColor = uri.getQueryParameter("bg_color")
                    val intensity = uri.getQueryParameter("intensity")
                    if (!isEmpty(bgColor) || !isEmpty(intensity)) {
                        val b = StringBuilder(data).append("?")
                        if (!isEmpty(bgColor)) b.append("bg_color=").append(bgColor)
                        if (!isEmpty(intensity)) {
                            if (!isEmpty(bgColor)) b.append("&")
                            b.append("intensity=").append(intensity)
                        }
                        data = b.toString()
                    }
                } else {
                    data = uri.getQueryParameter("color")
                }
                return data
            }
            if (isEmpty(uri.getScheme())) {
                uri = Uri.parse("http://" + url)
            }
            if (!isKnownHost(uri.getHost(), false)) return null
            val segments = uri.getPathSegments()
            if (segments != null && segments.size == 2 && "bg".equals(segments.get(0), ignoreCase = true)) {
                val query = uri.getQuery()
                return if (isEmpty(query)) segments.get(1) else segments.get(1) + "?" + query
            }
            return null
        } catch (ignored: Throwable) {
            return null
        }
    }

    fun isTrustedHost(host: String?, allowSubdomains: Boolean): Boolean {
        // No prompt when pressing links from these hosts.
        var host = host
        if (isEmpty(host)) {
            return false
        }
        val uri = Strings.wrapHttps(host)
        if (uri == null) {
            return false
        }
        host = uri.getHost()!!.lowercase(Locale.getDefault())
        for (knownHost in TELEGRAM_HOSTS) {
            if (host.equalsOrBothEmpty(knownHost) || (allowSubdomains && host.endsWith("." + knownHost))) {
                return true
            }
        }
        return false
    }

    fun isKnownHost(host: String?, allowTelegraph: Boolean): Boolean {
        var host = host
        if (isEmpty(host)) {
            return false
        }
        val uri = Strings.wrapHttps(host)
        if (uri == null) {
            return false
        }
        host = uri.getHost()!!.lowercase(Locale.getDefault())
        if (!isEmpty(options.tMeUrl)) {
            val tMeHost = urlWithoutProtocol(options.tMeUrl)
            if (host.equalsOrBothEmpty(tMeHost) || host.endsWith("." + tMeHost)) {
                return true
            }
        }
        for (knownHost in TME_HOSTS) {
            if (host.equalsOrBothEmpty(knownHost) || host.endsWith("." + knownHost)) {
                return true
            }
        }
        if (allowTelegraph) {
            for (knownHost in TELEGRAM_HOSTS) {
                if (host.equalsOrBothEmpty(knownHost) || host.endsWith("." + knownHost)) {
                    return true
                }
            }
            for (knownHost in TELEGRAPH_HOSTS) {
                if (host.equalsOrBothEmpty(knownHost) || host.endsWith("." + knownHost)) {
                    return true
                }
            }
        }
        return false
    }

    fun callConnectTimeoutMs(): Long {
        return options.callConnectTimeoutMs
    }

    fun allowQrLoginCamera(): Boolean {
        return (options.qrLoginCamera && Config.QR_AVAILABLE)
    }

    fun callPacketTimeoutMs(): Long {
        return options.callPacketTimeoutMs
    }

    fun maxCaptionLength(): Int {
        return options.messageCaptionLengthMax
    }

    fun suggestOnlyApiStickers(): Boolean {
        return options.stickersEmojiSuggestOnlyApi
    }

    fun maxMessageTextLength(): Int {
        return options.messageTextLengthMax
    }

    fun chatFolderCountMax(): Int {
        return options.chatFolderCountMax
    }

    fun chatFolderChosenChatCountMax(): Int {
        return options.chatFolderChosenChatCountMax
    }

    fun chatFolderInviteLinkCountMax(): Int {
        return options.chatFolderInviteLinkCountMax
    }

    fun chatFolderUpdatePeriodMillis(): Long {
        return TimeUnit.SECONDS.toMillis(options.chatFolderNewChatsUpdatePeriod.toLong())
    }

    fun telegramAntiSpamUserId(): Long {
        return options.antiSpamBotUserId
    }

    fun telegramChannelBotUserId(): Long {
        return options.channelBotUserId
    }

    @ConnectionState
    fun connectionState(): Int {
        return connectionState
    }

    fun connectionStateText(): String? {
        when (connectionState) {
            ConnectionState.UNKNOWN -> return Lang.getString(R.string.Initializing)
            ConnectionState.CONNECTED -> return Lang.getString(R.string.Connected)
            ConnectionState.WAITING_FOR_NETWORK -> return Lang.getString(R.string.network_WaitingForNetwork)
            ConnectionState.CONNECTING -> {
                if (routeSelector != null) {
                    if (routeSelector!!.isLookingUpForRoute()) {
                        return Lang.getString(R.string.network_Lookup)
                    } else if (routeSelector!!.isEmpty()) {
                        return Lang.getString(
                            if (effectiveProxyId > Settings.PROXY_ID_NONE) R.string.network_LookupFailedProxy else R.string.network_LookupFailed
                        )
                    } else if (routeSelector!!.isPending(effectiveProxyId)) {
                        return Lang.getString(
                            if (effectiveProxyId > Settings.PROXY_ID_NONE) R.string.network_LookupAttemptProxy else R.string.network_LookupAttempt
                        )
                    }
                }
                return Lang.getString(
                    if (effectiveProxyId > Settings.PROXY_ID_NONE) R.string.ConnectingWithProxy else R.string.network_Connecting
                )
            }

            ConnectionState.CONNECTING_TO_PROXY -> return Lang.getString(R.string.ConnectingToProxy)
            ConnectionState.UPDATING -> return Lang.getString(R.string.network_Updating)
        }
        throw UnsupportedOperationException(connectionState.toString())
    }

    fun networkType(): TdApi.NetworkType? {
        return networkType
    }

    val isConnected: Boolean
        get() = connectionState == ConnectionState.CONNECTED

    val isConnectingOrUpdating: Boolean
        get() {
            when (connectionState) {
                ConnectionState.CONNECTING, ConnectionState.CONNECTING_TO_PROXY, ConnectionState.UPDATING -> return true
                ConnectionState.CONNECTED, ConnectionState.WAITING_FOR_NETWORK, ConnectionState.UNKNOWN -> {}
            }
            return false
        }

    fun scheduleUiMessageAction(msg: TGMessage?, action: Int, arg1: Int, arg2: Int, delay: Long) {
        val what: Int = MSG_ACTION_MESSAGE_ACTION_PREFIX + action
        if (delay > 0) {
            ui()!!.sendMessageDelayed(ui()!!.obtainMessage(what, arg1, arg2, msg), delay)
        } else {
            ui()!!.sendMessage(ui()!!.obtainMessage(what, arg1, arg2, msg))
        }
    }

    fun cancelUiMessageActions(msg: TGMessage?, action: Int) {
        ui()!!.removeMessages(MSG_ACTION_MESSAGE_ACTION_PREFIX + action, msg)
    }

    fun handleUiMessage(msg: Message) {
        when (msg.what) {
            MSG_ACTION_UPDATE_CHAT_ACTION -> statusManager.onUpdateChatUserAction(msg.obj as UpdateChatAction?)
            MSG_ACTION_UPDATE_CALL -> cache.onUpdateCall(msg.obj as UpdateCall?)
            MSG_ACTION_DISPATCH_UNREAD_COUNTER -> dispatchUnreadCounters((msg.obj as ChatList?)!!, msg.arg1, msg.arg2 == 1)
            MSG_ACTION_CALL_STATE -> cache().onCallStateChanged(msg.arg1, msg.arg2)
            MSG_ACTION_CALL_BARS -> cache().onCallSignalBarsChanged(msg.arg1, msg.arg2)
            MSG_ACTION_PAUSE -> doPause()
            MSG_ACTION_USER_STATUS -> cache().onUpdateUserStatusInternal(msg.obj as UpdateUserStatus?, msg.arg1 == 1)
            MSG_ACTION_DISPATCH_TERMS_OF_SERVICE -> ui()!!.handleTermsOfService(msg.obj as UpdateTermsOfService?)
            MSG_ACTION_UPDATE_LANG_PACK -> Lang.updateLanguagePack(msg.obj as UpdateLanguagePackStrings?)
            else -> if (msg.what >= MSG_ACTION_MESSAGE_ACTION_PREFIX) {
                (msg.obj as TGMessage).handleUiMessage(msg.what - MSG_ACTION_MESSAGE_ACTION_PREFIX, msg.arg1, msg.arg2)
            }
        }
    }

    // UI
    @AnyThread
    fun dispatchUserStatus(update: UpdateUserStatus?, uiOnly: Boolean) {
        ui()!!.sendMessage(ui()!!.obtainMessage(MSG_ACTION_USER_STATUS, if (uiOnly) 1 else 0, 0, update))
    }

    @AnyThread
    fun dispatchCallStateChanged(callId: Int, @CallState newState: Int) {
        ui()!!.sendMessage(ui()!!.obtainMessage(MSG_ACTION_CALL_STATE, callId, newState))
    }

    @AnyThread
    fun dispatchCallBarsCount(callId: Int, barsCount: Int) {
        ui()!!.sendMessage(ui()!!.obtainMessage(MSG_ACTION_CALL_BARS, callId, barsCount))
    }

    // Updates: NOTIFICATIONS
    private var havePendingNotifications = false
    private var haveInitializedNotifications = false
    private fun resetState() {
        haveInitializedNotifications = false
        ignoreNotificationUpdates = false
    }

    @TdlibThread
    private fun resetChatsData() {
        knownChatIds.clear()
        chats.clear()
        synchronized(chatLists) {
            for (chatList in chatLists.values) {
                chatList.clear()
            }
        }
        forumTopicInfos.clear()
    }

    @TdlibThread
    private fun resetContextualData() {
        resetChatsData()
        activeCalls.clear()
        activeStories.clear()
        storyLists.clear()
        storyStealthModeCooldownUntilDate = 0
        storyStealthModeActiveUntilDate = storyStealthModeCooldownUntilDate
        accessibleChatTimers.clear()
        chatOnlineMemberCount.clear()
        myProfilePhoto = null
        myEmojiStatusId = 0
        pendingMessageTexts.clear()
        pendingMessageCaptions.clear()
        animatedDiceExplicit.clear()
        suggestedActions.clear()
        resetOptions()
        sessionsInfo = null
        animatedTgxEmoji.clear()
        cachedReactions.clear()
        activeEmojiReactions = null
        closeBirthdayUsers = null
    }

    private fun resetOptions() {
        options = TdlibOptions()
        if (!isEmpty(applicationConfigJson)) {
            val value = parse(applicationConfigJson)
            if (value != null) {
                options.handleApplicationConfig(value)
            }
        }
    }

    private class StickerSet(private val type: Int, private var currentSetName: String?, private val onlySingle: Boolean) {
        private val stickers = ConcurrentHashMap<String?, Sticker?>()
        var stickerSet: TdApi.StickerSet? = null

        private var isLoading = false

        fun find(emoji: String): Sticker? {
            return stickers.get(Emoji.instance().cleanEmojiCode(emoji))
        }

        fun find(value: Int): Sticker? {
            return stickers.get(Emoji.toEmoji(value))
        }

        fun clear() {
            isLoading = false
            stickers.clear()
            stickerSet = null
        }

        fun reload(tdlib: Tdlib, newStickerSetName: String?) {
            if (this.currentSetName == null || !this.currentSetName.equals(newStickerSetName, ignoreCase = true)) {
                this.currentSetName = newStickerSetName
                this.isLoading = false
                load(tdlib)
            }
        }

        val isLoaded: Boolean
            get() = !stickers.isEmpty()

        fun load(tdlib: Tdlib) {
            if (!isLoading && !isEmpty(currentSetName)) {
                isLoading = true
                tdlib.stickerSet(currentSetName, RunnableData { stickerSet: TdApi.StickerSet? -> reset(tdlib, stickerSet) })
            }
        }

        fun update(tdlib: Tdlib, stickerSet: TdApi.StickerSet) {
            if (this.stickerSet != null && this.stickerSet!!.id == stickerSet.id) {
                reset(tdlib, stickerSet)
            }
        }

        fun reset(tdlib: Tdlib, stickerSet: TdApi.StickerSet?) {
            clear()
            this.stickerSet = stickerSet
            if (stickerSet != null) {
                var index = 0
                for (sticker in stickerSet.stickers) {
                    val emojis = stickerSet.emojis[index]
                    if (onlySingle && emojis.emojis.size > 1) continue
                    for (emoji in emojis.emojis) {
                        var emoji = emoji
                        emoji = Emoji.instance().cleanEmojiCode(emoji)
                        if (!stickers.containsKey(emoji)) {
                            stickers.put(emoji, sticker)
                        }
                    }
                    index++
                }
            }
            tdlib.listeners().notifyAnimatedEmojiListeners(type)
            if (type == AnimatedEmojiListener.TYPE_DICE) {
                val sticker = find(0)
                if (sticker != null && !TD.isFileLoaded(sticker.sticker)) {
                    tdlib.files().downloadFile(sticker.sticker)
                }
            }
        }
    }

    private fun findExplicitDiceEmoji(value: Int): Sticker? {
        if (!instance().getNewSetting(Settings.SETTING_FLAG_EXPLICIT_DICE)) return null
        val stickerSet = animatedDiceExplicit.stickerSet
        if (stickerSet != null) {
            val languageEmoji = Lang.getLanguageEmoji()
            val builtinLanguageEmoji = Lang.getBuiltinLanguageEmoji()
            val numberEmoji = Emoji.toEmoji(value)
            var index = 0
            var bestStickerGuess: Sticker? = null
            for (sticker in stickerSet.stickers) {
                val emojis = stickerSet.emojis[index]
                var stickerValue = -1
                var matchedLanguageLevel = 0
                for (diceEmoji in emojis.emojis) {
                    if (diceEmoji == languageEmoji) {
                        matchedLanguageLevel = 2
                    } else if (diceEmoji == builtinLanguageEmoji) {
                        matchedLanguageLevel = 1
                    } else if (diceEmoji == ContentPreview.EMOJI_DICE.textRepresentation) {
                        stickerValue = 1
                    } else if (diceEmoji == numberEmoji) {
                        stickerValue = value
                    } else {
                        continue
                    }
                    if (stickerValue == value) {
                        if (matchedLanguageLevel == 2) return sticker
                        if (matchedLanguageLevel == 1) bestStickerGuess = sticker
                    }
                }
                index++
            }
            if (bestStickerGuess != null) return bestStickerGuess
        }
        var explicitDice = animatedDiceExplicit.find(value)
        if (explicitDice != null) return explicitDice
        if (value != 1) return null
        explicitDice = animatedDiceExplicit.find(Lang.getLanguageEmoji())
        if (explicitDice != null) return explicitDice
        explicitDice = animatedDiceExplicit.find(Lang.getBuiltinLanguageEmoji())
        if (explicitDice != null) return explicitDice
        explicitDice = animatedDiceExplicit.find(ContentPreview.EMOJI_DICE.textRepresentation)
        return explicitDice
    }

    fun findDiceEmoji(emoji: String?, value: Int, defaultValue: DiceStickers?): DiceStickers? {
        if (ContentPreview.EMOJI_DICE.textRepresentation == emoji) {
            val explicitDice = findExplicitDiceEmoji(value)
            if (explicitDice != null) return DiceStickersRegular(explicitDice)
        }
        return defaultValue
    }

    fun findTgxEmoji(emoji: String): Sticker? {
        return animatedTgxEmoji.find(emoji)
    }

    @TdlibThread
    private fun onUpdateHavePendingNotifications(update: UpdateHavePendingNotifications) {
        val havePendingNotifications = update.haveDelayedNotifications || update.haveUnreceivedNotifications
        if (this.havePendingNotifications != havePendingNotifications) {
            this.havePendingNotifications = havePendingNotifications
            checkKeepAlive()
        }
        if (!update.haveUnreceivedNotifications) {
            this.notificationConsistencyListeners.notifyConditionChanged(true)
        }
        incrementNotificationReferenceCount()
        notificationManager.releaseTdlibReference(Runnable { notificationListeners.notifyConditionChanged(!havePendingNotifications) }
        )
    }

    private var receivedActiveNotificationsTime: Long = 0
    private var receivedActiveNotificationsIgnored = false

    @TdlibThread
    private fun onUpdateActiveNotifications(update: UpdateActiveNotifications) {
        var update = update
        TDLib.Tag.notifications(0, accountId, "Received updateActiveNotifications, ignore: %b", ignoreNotificationUpdates)
        if (ignoreNotificationUpdates && update.groups.size > 0) {
            update = UpdateActiveNotifications(arrayOfNulls<NotificationGroup>(0))
            receivedActiveNotificationsIgnored = true
        } else {
            receivedActiveNotificationsIgnored = false
        }
        receivedActiveNotificationsTime = SystemClock.uptimeMillis()
        notificationManager.onUpdateActiveNotifications(update, Runnable { this.dispatchNotificationsInitialized() })
    }

    @TdlibThread
    private fun onUpdateNotificationGroup(update: UpdateNotificationGroup) {
        TDLib.Tag.notifications(
            0,
            accountId,
            "Received updateNotificationGroup, groupId: %d, elapsed: %d, ignore: %b",
            update.notificationGroupId,
            SystemClock.uptimeMillis() - receivedActiveNotificationsTime,
            ignoreNotificationUpdates
        )
        if (!ignoreNotificationUpdates) {
            notificationManager.onUpdateNotificationGroup(update)
        }
    }

    @TdlibThread
    private fun onUpdateNotification(update: UpdateNotification?) {
        if (!ignoreNotificationUpdates) {
            notificationManager.onUpdateNotification(update)
        }
    }

    // Updates: MESSAGES
    private val sendingMessages: MutableSet<String?> = HashSet<String?>()

    private fun addRemoveSendingMessage(chatId: Long, messageId: Long, add: Boolean) {
        val delta: Int
        synchronized(sendingMessages) {
            val prevSize = sendingMessages.size
            val key = chatId.toString() + "_" + messageId
            if (add) {
                sendingMessages.add(key)
            } else {
                sendingMessages.remove(key)
            }
            delta = sendingMessages.size - prevSize
        }
        if (delta > 0) {
            for (i in 0..<delta) {
                incrementReferenceCount(REFERENCE_TYPE_MESSAGE)
            }
        } else if (delta < 0) {
            for (i in 0..<-delta) {
                decrementReferenceCount(REFERENCE_TYPE_MESSAGE)
            }
        }
    }

    private fun updateNewMessage(update: UpdateNewMessage, isUpdate: Boolean) {
        if (update.message.sendingState is MessageSendingStatePending && update.message.content.getConstructor() != TdApi.MessageChatSetMessageAutoDeleteTime.CONSTRUCTOR) {
            addRemoveSendingMessage(update.message.chatId, update.message.id, true)
            if (isUpdate) return
        }

        listeners.updateNewMessage(update)

        notificationManager.onUpdateNewMessage(update)

        context.global().notifyUpdateNewMessage(this, update)

        if (!this.isDebugInstance && update.message.chatId == fromUserId(TELEGRAM_ACCOUNT_ID)) {
            listeners.notifySessionListPossiblyChanged(true)
        }

        if (update.message.content.getConstructor() == TdApi.MessageCall.CONSTRUCTOR) {
            updateSuitableCallLogInformation(update.message.chatId, update.message.isOutgoing, update.message)
        }
    }

    private fun updateMessageSendSucceeded(update: UpdateMessageSendSucceeded) {
        synchronized(dataLock) {
            instance().updateScrollMessageId(accountId, update.message.chatId, update.oldMessageId, update.message.id)
        }

        notifyMessageSendCallbacks(update.message.chatId, update.oldMessageId)

        listeners.updateMessageSendSucceeded(update)

        notificationManager.onUpdateMessageSendSucceeded(update)
        quickAckManager.onMessageSendSucceeded(update.message.chatId, update.oldMessageId)

        context.global().notifyUpdateMessageSendSucceeded(this, update)

        addRemoveSendingMessage(update.message.chatId, update.oldMessageId, false)
    }

    private fun updateVideoPublished(update: UpdateVideoPublished?) {
        // TODO?
    }

    private fun updateMessageSendFailed(update: UpdateMessageSendFailed) {
        UI.showError(update.error)
        synchronized(dataLock) {
            instance().updateScrollMessageId(accountId, update.message.chatId, update.oldMessageId, update.message.id)
        }

        listeners.updateMessageSendFailed(update)
        quickAckManager.onMessageSendFailed(update.message.chatId, update.oldMessageId)

        context.global().notifyUpdateMessageSendFailed(this, update)

        addRemoveSendingMessage(update.message.chatId, update.oldMessageId, false)
    }

    fun onMessageSendAcknowledged(update: UpdateMessageSendAcknowledged) {
        notifyMessageSendCallbacks(update.chatId, update.messageId)
        listeners.updateMessageSendAcknowledged(update)
    }

    @TdlibThread
    private fun updateMessageContent(update: UpdateMessageContent) {
        val chat: Chat?
        synchronized(dataLock) {
            chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
        }

        listeners.updateMessageContent(update)
        context.global().notifyUpdateMessageContent(this, update)

        when (update.newContent.getConstructor()) {
            MessageLocation.CONSTRUCTOR -> {
                cache().updateLiveLocation(update.chatId, update.messageId, update.newContent as MessageLocation?)
            }

            MessagePoll.CONSTRUCTOR -> {
                val poll = (update.newContent as MessagePoll).poll
                listeners().updatePoll(poll)
            }
        }
    }

    @TdlibThread
    private fun updateMessageEdited(update: UpdateMessageEdited) {
        listeners.updateMessageEdited(update)
        // TODO notifications per-edit?
    }

    @TdlibThread
    private fun updateMessageContentOpened(update: UpdateMessageContentOpened) {
        listeners.updateMessageContentOpened(update)
    }

    @TdlibThread
    private fun updateAnimatedEmojiMessageClicked(update: UpdateAnimatedEmojiMessageClicked) {
        listeners.updateAnimatedEmojiMessageClicked(update)
    }

    @TdlibThread
    private fun updateMessageIsPinned(update: UpdateMessageIsPinned) {
        listeners.updateMessageIsPinned(update)
    }

    @TdlibThread
    private fun updateLiveLocationViewed(update: UpdateMessageLiveLocationViewed) {
        listeners.updateMessageLiveLocationViewed(update)
        context.liveLocation().requestUpdate()
    }

    @TdlibThread
    private fun updateMessageMentionRead(update: UpdateMessageMentionRead) {
        val counterChanged: Boolean
        val availabilityChanged: Boolean
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            availabilityChanged = (chat!!.unreadMentionCount > 0) != (update.unreadMentionCount > 0)
            counterChanged = chat.unreadMentionCount != update.unreadMentionCount
            chat.unreadMentionCount = update.unreadMentionCount
        }

        listeners.updateMessageMentionRead(update, counterChanged, availabilityChanged)
    }

    @TdlibThread
    private fun updateMessageInteractionInfo(update: UpdateMessageInteractionInfo) {
        listeners.updateMessageInteractionInfo(update)
    }

    @TdlibThread
    private fun updateMessageContainsUnreadPollVotes(update: UpdateMessageContainsUnreadPollVotes) {
        listeners.updateMessageContainsUnreadPollVotes(update)
    }

    @TdlibThread
    private fun updateMessageUnreadReactions(update: UpdateMessageUnreadReactions) {
        val counterChanged: Boolean
        val availabilityChanged: Boolean
        val chat: Chat?
        val chatLists: Array<TdlibChatList?>?
        synchronized(dataLock) {
            chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            availabilityChanged = (chat!!.unreadReactionCount > 0) != (update.unreadReactionCount > 0)
            counterChanged = chat.unreadReactionCount != update.unreadReactionCount
            chat.unreadReactionCount = update.unreadReactionCount
            chatLists = if (counterChanged || availabilityChanged) chatListsImpl(chat.positions) else null
        }


        listeners.updateMessageUnreadReactions(update, counterChanged, availabilityChanged, chat, chatLists)
    }

    @TdlibThread
    private fun updateMessageFactCheck(update: UpdateMessageFactCheck?) {
        // TODO
    }

    @TdlibThread
    private fun updateSuggestedPostInfo(update: UpdateMessageSuggestedPostInfo?) {
        // TODO
    }

    @TdlibThread
    private fun updateMessagesDeleted(update: UpdateDeleteMessages) {
        if (update.fromCache) {
            return
        }

        Arrays.sort(update.messageIds)

        listeners.updateMessagesDeleted(update)

        context.global().notifyUpdateMessagesDeleted(this, update)
    }

    // Updates: SAVED MESSAGES
    @TdlibThread
    private fun updateSavedMessagesTopic(update: UpdateSavedMessagesTopic?) {
    }

    @TdlibThread
    private fun updateSavedMessagesTopicCount(update: UpdateSavedMessagesTopicCount?) {
    }

    @TdlibThread
    private fun updateSavedMessagesTags(update: UpdateSavedMessagesTags?) {
    }

    // Updates: SHORTCUTS
    @TdlibThread
    private fun updateQuickReplyShortcuts(update: UpdateQuickReplyShortcuts?) {
    }

    @TdlibThread
    private fun updateQuickReplyShortcut(update: UpdateQuickReplyShortcut?) {
    }

    @TdlibThread
    private fun updateQuickReplyShortcutMessages(update: UpdateQuickReplyShortcutMessages?) {
    }

    @TdlibThread
    private fun updateQuickReplyShortcutDeleted(update: UpdateQuickReplyShortcutDeleted?) {
    }

    // Updates: CHATS
    @TdlibThread
    private fun updateNewChat(update: UpdateNewChat) {
        val chatLists: MutableList<TdlibChatList>?
        synchronized(dataLock) {
            if (Config.TEST_CHAT_COUNTERS) {
                update.chat.unreadCount = random(1, 250000)
            }
            chats.put(update.chat.id, update.chat)
            knownChatIds.add(update.chat.id)
            if (update.chat.type.getConstructor() == ChatTypeSupergroup.CONSTRUCTOR) {
                val supergroupId = (update.chat.type as ChatTypeSupergroup).supergroupId
                if ((update.chat.type as ChatTypeSupergroup).isChannel) channels.add(supergroupId)
                else channels.remove(supergroupId)
            }
            if (update.chat.positions != null && update.chat.positions.size > 0) {
                chatLists = ArrayList<TdlibChatList>(update.chat.positions.size)
                for (position in update.chat.positions) {
                    if (position.order != 0L) {
                        chatLists.add(chatListImpl(position.list))
                    }
                }
            } else {
                chatLists = null
            }
        }
        if (chatLists != null) {
            for (chatList in chatLists) {
                chatList.onUpdateNewChat(update.chat)
            }
        }
    }

    fun refreshChatState(chatId: Long) {
        client().send(GetChat(chatId), Client.ResultHandler { result: TdApi.Object? ->
            when (result!!.getConstructor()) {
                Chat.CONSTRUCTOR -> {
                    updateChatState(result as Chat)
                }

                TdApi.Error.CONSTRUCTOR -> {
                    Log.v("Unable to refresh chat state: %s", TD.toErrorString(result))
                }
            }
        })
    }

    @TdlibThread
    private fun updateChatState(chat: Chat) {
        val notificationSettingsChanged: Boolean
        synchronized(dataLock) {
            val existingChat = chats.get(chat.id)
            if (TdlibUtils.assertChat(chat.id, chat)) {
                return
            }
            existingChat!!.canBeDeletedForAllUsers = chat.canBeDeletedForAllUsers
            existingChat.canBeDeletedOnlyForSelf = chat.canBeDeletedOnlyForSelf
            existingChat.canBeReported = chat.canBeReported
            notificationSettingsChanged =
                !existingChat.notificationSettings.useDefaultMuteFor && !chat.notificationSettings.useDefaultMuteFor && existingChat.notificationSettings.muteFor != chat.notificationSettings.muteFor
        }
        if (notificationSettingsChanged) {
            listeners.updateNotificationSettings(UpdateChatNotificationSettings(chat.id, chat.notificationSettings))
        }
    }

    @TdlibThread
    private fun updateChatDefaultDisableNotifications(update: UpdateChatDefaultDisableNotification) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.defaultDisableNotification = update.defaultDisableNotification
        }
        listeners.updateChatDefaultDisableNotifications(update)
    }

    @TdlibThread
    private fun updateChatDefaultMessageSenderId(update: UpdateChatMessageSender) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.messageSenderId = update.messageSenderId
        }
        listeners.updateChatDefaultMessageSenderId(update)
    }

    @TdlibThread
    private fun updateChatUnreadMentionCount(update: UpdateChatUnreadMentionCount) {
        val availabilityChanged: Boolean
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            availabilityChanged = (chat!!.unreadMentionCount > 0) != (update.unreadMentionCount > 0)
            chat.unreadMentionCount = update.unreadMentionCount
        }
        listeners.updateChatUnreadMentionCount(update, availabilityChanged)
    }

    @TdlibThread
    private fun updateChatUnreadPollVoteCount(update: UpdateChatUnreadPollVoteCount) {
        val availabilityChanged: Boolean
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            availabilityChanged = (chat!!.unreadPollVoteCount > 0) != (update.unreadPollVoteCount > 0)
            chat.unreadPollVoteCount = update.unreadPollVoteCount
        }
        listeners.updateChatUnreadPollVoteCount(update, availabilityChanged)
    }

    @TdlibThread
    private fun updateChatUnreadReactionCount(update: UpdateChatUnreadReactionCount) {
        val availabilityChanged: Boolean
        val chat: Chat?
        val chatLists: Array<TdlibChatList?>?
        synchronized(dataLock) {
            chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            availabilityChanged = (chat!!.unreadReactionCount > 0) != (update.unreadReactionCount > 0)
            chat.unreadReactionCount = update.unreadReactionCount
            chatLists = chatListsImpl(chat.positions)
        }
        listeners.updateChatUnreadReactionCount(update, availabilityChanged, chat, chatLists)
    }

    @TdlibThread
    private fun updateChatLastMessage(update: UpdateChatLastMessage) {
        if (Log.isEnabled(Log.TAG_MESSAGES_LOADER)) {
            Log.i(
                Log.TAG_MESSAGES_LOADER,
                "updateChatTopMessage chatId=%d messageId=%d",
                update.chatId,
                if (update.lastMessage != null) update.lastMessage!!.id else 0
            )
        }
        val listChanges: MutableList<ChatListChange?>?
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.lastMessage = update.lastMessage
            listChanges = setChatPositions(chat, update.positions)
        }
        listeners.updateChatLastMessage(update, listChanges)
    }

    internal class ChatListChange(@JvmField val list: TdlibChatList?, @JvmField val chat: Chat?, @JvmField val change: ChatChange?)

    class ChatChange(@JvmField val position: TdApi.ChatPosition?, val flags: Int) {
        fun orderChanged(): Boolean {
            return hasFlag(flags, ORDER)
        }

        fun metadataChanged(): Boolean {
            return setFlag(flags, ORDER, false) != 0
        }

        fun sourceChanged(): Boolean {
            return hasFlag(flags, SOURCE)
        }

        fun pinStateChanged(): Boolean {
            return hasFlag(flags, PIN_STATE)
        }

        companion object {
            const val ORDER: Int = 1
            val SOURCE: Int = 1 shl 1
            val PIN_STATE: Int = 1 shl 2
            @JvmField
            val ALL: Int = ORDER or SOURCE or PIN_STATE
        }
    }

    private fun setChatPositions(chat: Chat, newPositions: Array<TdApi.ChatPosition>): MutableList<ChatListChange?>? {
        var changes: MutableList<ChatListChange?>? = null
        val oldPositions = chat.positions
        var extraPositionCount = oldPositions.size
        for (newPositionIndex in newPositions.indices.reversed()) {
            var flags = 0
            val newPosition = newPositions[newPositionIndex]
            val existingIndex = indexOf(oldPositions, newPosition.list, newPositionIndex)
            if (existingIndex != -1) { // Updated position
                extraPositionCount--
                val oldPosition = oldPositions[existingIndex]
                if (oldPosition.order != newPosition.order) flags = flags or ChatChange.ORDER
                if (!oldPosition.source.equalsTo(newPosition.source)) flags = flags or ChatChange.SOURCE
                if (oldPosition.isPinned != newPosition.isPinned) flags = flags or ChatChange.PIN_STATE
            } else if (newPosition.order != 0L) { // Added position
                flags = flags or ChatChange.ORDER
                if (newPosition.source != null) flags = flags or ChatChange.SOURCE
                if (newPosition.isPinned) flags = flags or ChatChange.PIN_STATE
            }
            if (flags != 0) {
                if (changes == null) changes = ArrayList<ChatListChange?>(newPositionIndex + 1)
                val positionChange = ChatChange(newPosition, flags)
                val chatListChange = ChatListChange(
                    chatListImpl(newPosition.list),
                    chat,
                    positionChange
                )
                changes.add(chatListChange)
            }
        }
        if (extraPositionCount > 0) {
            for (oldPositionIndex in oldPositions.indices.reversed()) {
                val oldPosition = oldPositions[oldPositionIndex]
                val newPositionIndex = indexOf(newPositions, oldPosition.list)
                if (newPositionIndex == -1) { // Removed position
                    if (oldPosition.order != 0L) {
                        var flags = ChatChange.ORDER
                        if (oldPosition.source != null) flags = flags or ChatChange.SOURCE
                        if (oldPosition.isPinned) flags = flags or ChatChange.PIN_STATE
                        if (changes == null) changes = ArrayList<ChatListChange?>(extraPositionCount)
                        val positionChange = ChatChange(TdApi.ChatPosition(oldPosition.list, 0, false, null), flags)
                        changes.add(
                            ChatListChange(
                                chatListImpl(oldPosition.list),
                                chat,
                                positionChange
                            )
                        )
                    }
                    if (--extraPositionCount == 0) break
                }
            }
        }
        chat.positions = newPositions
        return changes
    }

    @TdlibThread
    private fun updateChatPosition(update: UpdateChatPosition) {
        val chatListChange: ChatListChange?
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            val changeFlags: Int = Companion.setChatPosition(chat!!, update.position)
            if (changeFlags != 0) {
                val chatList = chatListImpl(update.position.list)
                chatListChange = ChatListChange(
                    chatList,
                    chat,
                    ChatChange(update.position, changeFlags)
                )
            } else {
                chatListChange = null
            }
        }
        if (chatListChange != null) {
            listeners.updateChatPosition(update, chatListChange)
        }
    }

    @TdlibThread
    private fun updateChatAddedToList(update: UpdateChatAddedToList) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            val oldChatLists = chat!!.chatLists
            if (oldChatLists != null) {
                for (chatList in oldChatLists) {
                    if (chatList.equalsTo(update.chatList)) {
                        return
                    }
                }
            }
            val list: MutableList<ChatList> = ArrayList<ChatList>((if (oldChatLists != null) oldChatLists.size else 0) + 1)
            if (oldChatLists != null) {
                Collections.addAll<ChatList?>(list, *oldChatLists)
            }
            list.add(update.chatList)
            // TODO: sort?
            chat.chatLists = list.toTypedArray<ChatList?>()
        }
        listeners.updateChatAddedToList(update)
    }

    @TdlibThread
    private fun updateChatRemovedFromList(update: UpdateChatRemovedFromList) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }

            val oldChatLists = chat!!.chatLists
            var foundIndex = -1
            if (oldChatLists != null) {
                for (i in oldChatLists.indices) {
                    if (oldChatLists[i].equalsTo(update.chatList)) {
                        foundIndex = i
                        break
                    }
                }
            }
            if (foundIndex == -1) {
                return
            }
            chat.chatLists = oldChatLists.removeElement<ChatList?>(foundIndex, arrayOfNulls<ChatList>(oldChatLists!!.size - 1))
        }
        listeners.updateChatRemovedFromList(update)
    }

    @TdlibThread
    private fun updateChatAvailableReactions(update: UpdateChatAvailableReactions) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.availableReactions = update.availableReactions
        }
        listeners.updateChatAvailableReactions(update)
    }

    private var mainChatListPosition = 0
    private var areTagsEnabled = false
    private var chatFolders: Array<ChatFolderInfo>? = emptyArray<ChatFolderInfo>()
    private val chatFoldersById = SparseArrayCompat<ChatFolderInfo?>()

    @TdlibThread
    private fun updateChatFolders(update: UpdateChatFolders) {
        synchronized(dataLock) {
            val chatFolders = update.chatFolders
            this.chatFolders = chatFolders
            this.chatFoldersById.clear()
            if (chatFolders != null) {
                for (chatFolder in chatFolders) {
                    this.chatFoldersById.put(chatFolder.id, chatFolder)
                }
            }
            this.mainChatListPosition = update.mainChatListPosition
            this.areTagsEnabled = update.areTagsEnabled
        }
        listeners.updateChatFolders(update)
    }

    @TdlibThread
    private fun updateChatPermissions(update: UpdateChatPermissions) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.permissions = update.permissions
        }

        listeners.updateChatPermissions(update)
    }

    @TdlibThread
    private fun updateChatTitle(update: UpdateChatTitle) {
        val chat: Chat?
        val chatLists: Array<TdlibChatList?>?
        synchronized(dataLock) {
            chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.title = update.title
            chatLists = chatListsImpl(chat.positions)
        }

        listeners.updateChatTitle(update, chat, chatLists)

        val myUserId = myUserId()
        if (myUserId != 0L) {
            try {
                TdlibNotificationChannelGroup.updateChat(this, myUserId, chat)
            } catch (e: ChannelCreationFailureException) {
                TDLib.Tag.notifications(
                    "Unable to update notification channel title for chat %d:\n%s",
                    update.chatId,
                    Log.toString(e)
                )
                settings().trackNotificationChannelProblem(e, chat!!.id)
            }
        }
    }

    @TdlibThread
    private fun updateChatTheme(update: UpdateChatTheme) {
        val chat: Chat?
        val chatLists: Array<TdlibChatList?>?
        synchronized(dataLock) {
            chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.theme = update.theme
            chatLists = chatListsImpl(chat.positions)
        }
        listeners.updateChatTheme(update, chat, chatLists)
    }

    @TdlibThread
    private fun updateChatActionBar(update: UpdateChatActionBar) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.actionBar = update.actionBar
        }

        listeners.updateChatActionBar(update)
    }

    @TdlibThread
    private fun updateChatBusinessBotManageBar(update: UpdateChatBusinessBotManageBar) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.businessBotManageBar = update.businessBotManageBar
        }

        listeners.updateChatBusinessBotManageBar(update)
    }

    @TdlibThread
    private fun updateChatHasScheduledMessages(update: UpdateChatHasScheduledMessages) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.hasScheduledMessages = update.hasScheduledMessages
        }

        listeners.updateChatHasScheduledMessages(update)
    }

    @TdlibThread
    private fun updateChatHasProtectedContent(update: UpdateChatHasProtectedContent) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.hasProtectedContent = update.hasProtectedContent
        }

        listeners.updateChatHasProtectedContent(update)
    }

    @TdlibThread
    private fun updateChatPhoto(update: UpdateChatPhoto) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.photo = update.photo
        }

        listeners.updateChatPhoto(update)
    }

    @TdlibThread
    private fun updateChatReadInbox(update: UpdateChatReadInbox) {
        val chat: Chat?
        val availabilityChanged: Boolean
        val chatLists: Array<TdlibChatList?>?
        synchronized(dataLock) {
            chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.lastReadInboxMessageId = update.lastReadInboxMessageId
            if (Config.TEST_CHAT_COUNTERS) {
                update.unreadCount = random(1, 250000)
            }
            availabilityChanged = (chat.unreadCount > 0) != (update.unreadCount > 0)
            chat.unreadCount = update.unreadCount
            chatLists = chatListsImpl(chat.positions)
        }
        listeners.updateChatReadInbox(update, availabilityChanged, chat, chatLists)
    }

    @TdlibThread
    private fun updateChatReadOutbox(update: UpdateChatReadOutbox) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.lastReadOutboxMessageId = update.lastReadOutboxMessageId
        }

        listeners.updateChatReadOutbox(update)
    }

    @TdlibThread
    private fun updateChatReplyMarkup(update: UpdateChatReplyMarkup) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.replyMarkupMessageId = if (update.replyMarkupMessage != null) update.replyMarkupMessage!!.id else 0
        }

        listeners.updateChatReplyMarkup(update)
    }

    @TdlibThread
    private fun updateChatDraftMessage(update: UpdateChatDraftMessage) {
        val listChanges: MutableList<ChatListChange?>?
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.draftMessage = update.draftMessage
            listChanges = setChatPositions(chat, update.positions)
        }
        listeners.updateChatDraftMessage(update, listChanges)
    }

    @TdlibThread
    private fun updateChatOnlineMemberCount(update: UpdateChatOnlineMemberCount) {
        synchronized(dataLock) {
            val onlineCountObj = chatOnlineMemberCount.get(update.chatId)
            val count = if (onlineCountObj != null) onlineCountObj else 0
            if (update.onlineMemberCount == count) return
            if (update.onlineMemberCount != 0) chatOnlineMemberCount.put(update.chatId, update.onlineMemberCount)
            else chatOnlineMemberCount.remove(update.chatId)
        }
        listeners.updateChatOnlineMemberCount(update)
    }

    @TdlibThread
    private fun updateChatMessageAutoDeleteTime(update: UpdateChatMessageAutoDeleteTime) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.messageAutoDeleteTime = update.messageAutoDeleteTime
        }
        listeners.updateChatMessageAutoDeleteTime(update)
    }

    @TdlibThread
    private fun updateChatVideoChat(update: UpdateChatVideoChat) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.videoChat = update.videoChat
        }
        listeners.updateChatVideoChat(update)
    }

    @TdlibThread
    private fun updateForumTopicInfo(update: UpdateForumTopicInfo) {
        val cacheKey = update.info.chatId.toString() + "_" + update.info.forumTopicId
        synchronized(dataLock) {
            forumTopicInfos.put(cacheKey, update.info)
        }
        listeners.updateForumTopicInfo(update)
    }

    @TdlibThread
    private fun updateForumTopic(update: UpdateForumTopic) {
        listeners.updateForumTopic(update)
    }

    @TdlibThread
    private fun updateChatViewAsTopics(update: UpdateChatViewAsTopics) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.viewAsTopics = update.viewAsTopics
        }
        listeners.updateChatViewAsTopics(update)
    }

    @TdlibThread
    private fun updateDirectMessagesChatTopic(update: UpdateDirectMessagesChatTopic?) {
    }

    @TdlibThread
    private fun updateTopicMessageCount(update: UpdateTopicMessageCount?) {
    }

    @TdlibThread
    private fun updateChatPendingJoinRequests(update: UpdateChatPendingJoinRequests) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.pendingJoinRequests = update.pendingJoinRequests
        }
        listeners.updateChatPendingJoinRequests(update)
    }

    @TdlibThread
    private fun updateChatIsMarkedAsUnread(update: UpdateChatIsMarkedAsUnread) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.isMarkedAsUnread = update.isMarkedAsUnread
        }

        listeners.updateChatIsMarkedAsUnread(update)
    }

    @TdlibThread
    private fun updateChatIsTranslatable(update: UpdateChatIsTranslatable) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.isTranslatable = update.isTranslatable
        }

        listeners.updateChatIsTranslatable(update)
    }

    @TdlibThread
    private fun updateChatIsBlocked(update: UpdateChatBlockList) {
        synchronized(dataLock) {
            val chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.blockList = update.blockList
        }

        listeners.updateChatBlockList(update)
    }

    // Updates: STORIES
    private val storiesComparator = Comparator { o1: ChatActiveStories?, o2: ChatActiveStories? ->
        if (o1!!.order != o2!!.order) {
            return@Comparator if (o1.order > o2.order) -1 else 1
        }
        if (o1.chatId != o2.chatId) {
            return@Comparator if (o1.chatId > o2.chatId) -1 else 1
        }
        0
    }

    fun storiesComparator(): Comparator<ChatActiveStories?> {
        return storiesComparator
    }

    fun getStoryList(list: TdApi.StoryList): StoryList {
        synchronized(dataLock) {
            var storyList = storyLists.get(list.getConstructor())
            if (storyList == null) {
                storyList = StoryList(this, list)
                storyLists.put(list.getConstructor(), storyList)
            }
            return storyList
        }
    }

    fun getActiveStories(chatId: Long, allowRequest: Boolean, onLoaded: RunnableData<ChatActiveStories?>?): ChatActiveStories? {
        synchronized(dataLock) {
            val stories = this.activeStories.get(chatId)
            if (stories != null) {
                return stories
            }
        }
        if (allowRequest) {
            client().send(GetChatActiveStories(chatId), Client.ResultHandler { result: TdApi.Object? ->
                when (result!!.getConstructor()) {
                    ChatActiveStories.CONSTRUCTOR -> {
                        val stories = getActiveStories(chatId, false, null)
                        checkNotNull(stories)
                        if (onLoaded != null) {
                            onLoaded.runWithData(stories)
                        }
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        UI.showError(result)
                    }
                }
            })
        }
        return null
    }

    @TdlibThread
    private fun updateStoryListChatCount(update: UpdateStoryListChatCount) {
        synchronized(dataLock) {
            storyListChatCount.put(update.storyList.getConstructor(), update.chatCount)
        }
        val storyList = getStoryList(update.storyList)
        storyList.notifyApproximateTotalItemCountChanged()
    }

    fun getStoryListChatCount(list: TdApi.StoryList): Int {
        synchronized(dataLock) {
            return storyListChatCount.get(list.getConstructor())
        }
    }

    @TdlibThread
    private fun updateChatActiveStories(update: UpdateChatActiveStories) {
        val prevActiveStories: ChatActiveStories?
        synchronized(dataLock) {
            val chatId = update.activeStories.chatId
            prevActiveStories = activeStories.remove(chatId)
            activeStories.put(chatId, update.activeStories)
        }
        listeners.updateChatActiveStories(update)
        val wasPresent = prevActiveStories != null && prevActiveStories.stories.size > 0 && prevActiveStories.list != null
        val nowPresent = update.activeStories.stories.size > 0 && update.activeStories.list != null
        val sameList = wasPresent && nowPresent && prevActiveStories.list.equalsTo(update.activeStories.list)
        if (sameList) {
            // Moved or just updated within the same list
            val storyList = getStoryList(update.activeStories.list!!)
            storyList.moveItem(update.activeStories, prevActiveStories)
        } else {
            if (wasPresent) {
                // Removed from prevActiveStories.list
                val storyList = getStoryList(prevActiveStories.list!!)
                storyList.removeItem(prevActiveStories)
            }
            if (nowPresent) {
                // Added to update.activeStories.list
                val storyList = getStoryList(update.activeStories.list!!)
                storyList.addItem(update.activeStories)
            }
        }
    }

    @TdlibThread
    private fun updateStory(update: UpdateStory) {
        listeners.updateStory(update)
    }

    @TdlibThread
    private fun updateStoryDeleted(update: UpdateStoryDeleted) {
        listeners.updateStoryDeleted(update)
    }

    @TdlibThread
    private fun updateStoryPostSucceeded(update: UpdateStoryPostSucceeded) {
        listeners.updateStoryPostSucceeded(update)
    }

    @TdlibThread
    private fun updateStoryPostFailed(update: UpdateStoryPostFailed) {
        listeners.updateStoryPostFailed(update)
    }

    @TdlibThread
    private fun updateStoryStealthMode(update: UpdateStoryStealthMode) {
        synchronized(dataLock) {
            this.storyStealthModeActiveUntilDate = update.activeUntilDate
            this.storyStealthModeCooldownUntilDate = update.cooldownUntilDate
        }
        listeners.updateStoryStealthMode(update)
    }

    // Updates: CHAT STATUS
    @TdlibThread
    private fun updateChatUserAction(update: UpdateChatAction) {
        if (update.chatId != myUserId()) {
            ui()!!.sendMessage(ui()!!.obtainMessage(MSG_ACTION_UPDATE_CHAT_ACTION, 0, 0, update))
        }
    }

    // Updates: CALLS
    private val activeCalls = SparseArrayCompat<TdApi.Call?>()
    private var haveActiveCalls = false

    fun haveActiveCalls(): Boolean {
        return haveActiveCalls
    }

    private fun setHaveActiveCalls(haveActiveCalls: Boolean) {
        if (this.haveActiveCalls != haveActiveCalls) {
            this.haveActiveCalls = haveActiveCalls
            if (haveActiveCalls) {
                incrementCallReferenceCount()
            } else {
                decrementCallReferenceCount()
            }
        }
    }

    private class CallLog(call: TdApi.Call, files: VoIPLogs.Pair?) {
        val callId: Int
        val chatId: Long
        val isOutgoing: Boolean
        val files: VoIPLogs.Pair?

        var guessedMessageId: Long = 0

        init {
            this.callId = call.id
            this.chatId = fromUserId(call.userId)
            this.isOutgoing = call.isOutgoing
            this.files = files
        }
    }

    private var callLogs: MutableMap<Long, MutableList<CallLog>>? = null

    @AnyThread
    fun findCallLogInformation(chatId: Long, messageId: Long): VoIPLogs.Pair? {
        synchronized(dataLock) {
            val list = if (callLogs != null) callLogs!!.get(chatId) else null
            if (list != null) {
                for (callLog in list) {
                    if (callLog.chatId == chatId && callLog.guessedMessageId == messageId) {
                        return callLog.files
                    }
                }
            }
        }
        return null
    }

    @AnyThread
    fun updateSuitableCallLogInformation(chatId: Long, isOutgoing: Boolean, callMessage: TdApi.Message) {
        synchronized(dataLock) {
            val list = if (callLogs != null) callLogs!!.get(chatId) else null
            if (list != null) {
                for (callLog in list) {
                    if (callLog.guessedMessageId == 0L && callLog.chatId == chatId && callLog.isOutgoing == isOutgoing) {
                        callLog.guessedMessageId = callMessage.id
                        // TODO some persistence?
                        break
                    }
                }
            }
        }
    }

    @AnyThread
    fun storeCallLogInformation(call: TdApi.Call, logFiles: VoIPLogs.Pair?) {
        val callLog = CallLog(call, logFiles)
        synchronized(dataLock) {
            if (callLogs == null) {
                callLogs = LinkedHashMap<Long, MutableList<CallLog>>()
            }
            val chatId = fromUserId(call.userId)
            var list = callLogs!!.get(chatId)
            if (list == null) {
                list = ArrayList<CallLog>()
                callLogs!!.put(chatId, list)
            }
            list.add(callLog)
        }
    }

    @TdlibThread
    private fun updateCall(update: UpdateCall) {
        val haveActiveCalls: Boolean
        synchronized(dataLock) {
            val wasActive = activeCalls.containsKey(update.call.id)
            val nowActive = TD.isActive(update.call)
            if (nowActive) {
                activeCalls.put(update.call.id, update.call)
            } else {
                activeCalls.remove(update.call.id)
            }
            haveActiveCalls = !activeCalls.isEmpty()
        }
        setHaveActiveCalls(haveActiveCalls)
        context.global().onUpdateCall(this, update)

        ui()!!.sendMessage(ui()!!.obtainMessage(MSG_ACTION_UPDATE_CALL, update))
        listeners.updateCall(update)
    }

    @TdlibThread
    private fun updateCallSignalingData(update: UpdateNewCallSignalingData) {
        listeners.updateNewCallSignalingData(update)
    }

    @TdlibThread
    private fun updateGroupCall(update: UpdateGroupCall) {
        context.global().onUpdateGroupCall(this, update)
        listeners.updateGroupCall(update)
    }

    @TdlibThread
    private fun updateGroupCallParticipant(update: UpdateGroupCallParticipant) {
        listeners.updateGroupCallParticipant(update)
    }

    @TdlibThread
    private fun updateGroupCallParticipants(update: UpdateGroupCallParticipants) {
        listeners.updateGroupCallParticipants(update)
    }

    @TdlibThread
    private fun updateNewGroupCallMessage(update: UpdateNewGroupCallMessage) {
        listeners.updateNewGroupCallMessage(update)
    }

    @TdlibThread
    private fun updateNewGroupCallPaidReaction(update: UpdateNewGroupCallPaidReaction) {
        listeners.updateNewGroupCallPaidReaction(update)
    }

    @TdlibThread
    private fun updateGroupCallMessageLevels(update: UpdateGroupCallMessageLevels) {
        synchronized(dataLock) {
            this.groupCallMessageLevels = update.levels
        }
        listeners.updateGroupCallMessageLevels(update)
    }

    @TdlibThread
    private fun updateGroupCallMessageSendFailed(update: UpdateGroupCallMessageSendFailed) {
        listeners.updateGroupCallMessageSendFailed(update)
    }

    @TdlibThread
    private fun updateGroupCallMessagesDeleted(update: UpdateGroupCallMessagesDeleted) {
        listeners.updateGroupCallMessagesDeleted(update)
    }

    @TdlibThread
    private fun updateGroupCallVerificationState(update: UpdateGroupCallVerificationState) {
        listeners.updateGroupCallVerificationState(update)
    }

    // Updates: LANG PACK
    @TdlibThread
    private fun updateLanguagePack(update: UpdateLanguagePackStrings) {
        if (BuildConfig.LANGUAGE_PACK == update.localizationTarget) {
            ui()!!.sendMessage(ui()!!.obtainMessage(MSG_ACTION_UPDATE_LANG_PACK, update))
        }
    }

    // Updates: ATTACH MENU BOTS
    @TdlibThread
    private fun updateAttachmentMenuBots(update: UpdateAttachmentMenuBots?) {
        // TODO
    }

    @TdlibThread
    private fun updateWebAppMessageSent(update: UpdateWebAppMessageSent?) {
        // TODO
    }

    // Updates: NOTIFICATIONS
    @TdlibThread
    private fun updateNotificationSettings(update: UpdateChatNotificationSettings) {
        val chatId = update.chatId
        val oldNotificationSettings: ChatNotificationSettings
        synchronized(dataLock) {
            val chat = chats.get(chatId)
            if (TdlibUtils.assertChat(chatId, chat, update)) {
                return
            }
            oldNotificationSettings = chat!!.notificationSettings
            chat.notificationSettings = update.notificationSettings
        }

        listeners.updateNotificationSettings(update)
        notificationManager.onUpdateNotificationSettings(update, chatId, oldNotificationSettings)
    }

    @TdlibThread
    private fun updateNotificationSettings(update: UpdateScopeNotificationSettings) {
        listeners.updateNotificationSettings(update)
        notificationManager.onUpdateNotificationSettings(update)
    }

    @TdlibThread
    private fun updateReactionNotificationSettings(update: UpdateReactionNotificationSettings?) {
        listeners.updateReactionNotificationSettings(update)
    }

    @TdlibThread
    private fun onUpdateSavedNotificationSounds(update: UpdateSavedNotificationSounds?) {
        // TODO
    }

    // Updates: PRIVACY
    @TdlibThread
    private fun updatePrivacySettingRules(update: UpdateUserPrivacySettingRules) {
        listeners.updatePrivacySettingRules(update.setting, update.rules)
    }

    // Updates: CHAT ACTION
    @TdlibThread
    private fun updateSupergroup(update: UpdateSupergroup) {
        val chat: Chat?
        synchronized(dataLock) {
            val chatId = fromSupergroupId(update.supergroup.id)
            chat = chats.get(chatId)
            if (chat != null) {
                val prevIsChannel = (chat.type as ChatTypeSupergroup).isChannel
                val nowIsChannel = update.supergroup.isChannel
                if (prevIsChannel != nowIsChannel) {
                    (chat.type as ChatTypeSupergroup).isChannel = nowIsChannel
                    val supergroupId = (chat.type as ChatTypeSupergroup).supergroupId
                    if (nowIsChannel) {
                        channels.add(supergroupId)
                    } else {
                        channels.remove(supergroupId)
                    }
                }
            }
        }
        cache.onUpdateSupergroup(update, chat)
    }

    // Updates: SECURITY
    @Suppress("deprecation")
    private fun updateServiceNotification(update: UpdateServiceNotification) {
        val text = update.content.textOrCaption()
        val msg = TD.toDisplayCharSequence(text)
        if (msg == null) return
        ui()!!.post(Runnable {
            val c = UI.getCurrentStackItem()
            if (c != null) {
                if (!isEmpty(update.type) && (update.type.startsWith("AUTH_KEY_DROP") || update.type.startsWith("AUTHKEYDROP"))) {
                    c.openAlert(
                        R.string.AppName,
                        msg,
                        Lang.getString(R.string.LogOut),
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> destroy() },
                        ViewController.ALERT_NO_CANCELABLE
                    )
                } else {
                    c.openAlert(R.string.AppName, msg)
                }
            }
        })
    }

    @TdlibThread
    private fun updateUnconfirmedSession(update: UpdateUnconfirmedSession?) {
        // TODO
    }

    // Updates: FILES
    private fun updateFile(update: UpdateFile) {
        listeners.updateFile(update)

        // TODO
        context.player().onUpdateFile(this, update)

        files().onFileUpdate(update)

        if (update.file.local.isDownloadingActive || update.file.remote.isUploadingActive) {
            files().onFileProgress(update)

            val fileId = update.file.id
            if (!ImageLoader.instance().onProgress(this, update.file)) {
                if (!GifBridge.instance().onProgress(this, fileId, TD.getFileProgress(update.file))) {
                    // Nothing?
                }
            }

            return
        }

        if (TD.isFileLoaded(update.file)) {
            files().onFileLoaded(update)
            if (!ImageLoader.instance().onLoad(this, update.file)) {
                if (!GifBridge.instance().onLoad(this, update.file)) {
                    // Nothing?
                }
            }
        } else {
            files().onFileUpdated(update)
        }
    }

    @TdlibThread
    private fun updateFileAddedToDownloads(update: UpdateFileAddedToDownloads?) {
        listeners.updateFileAddedToDownloads(update)
    }

    @TdlibThread
    private fun updateFileDownload(update: UpdateFileDownload?) {
        listeners.updateFileDownload(update)
    }

    @TdlibThread
    private fun updateFileDownloads(update: UpdateFileDownloads?) {
        listeners.updateFileDownloads(update)
    }

    @TdlibThread
    private fun updateFileRemovedFromDownloads(update: UpdateFileRemovedFromDownloads?) {
        listeners.updateFileRemovedFromDownloads(update)
    }

    // Updates: CONFIG
    @TdlibThread
    private fun updateConnectionState(update: UpdateConnectionState) {
        val state: Int
        when (update.state.getConstructor()) {
            TdApi.ConnectionStateWaitingForNetwork.CONSTRUCTOR -> state = ConnectionState.WAITING_FOR_NETWORK
            TdApi.ConnectionStateConnectingToProxy.CONSTRUCTOR -> state = ConnectionState.CONNECTING_TO_PROXY
            TdApi.ConnectionStateConnecting.CONSTRUCTOR -> state = ConnectionState.CONNECTING
            TdApi.ConnectionStateUpdating.CONSTRUCTOR -> state = ConnectionState.UPDATING
            TdApi.ConnectionStateReady.CONSTRUCTOR -> state = ConnectionState.CONNECTED
            else -> {
                assertConnectionState_963d6b5f()
                throw unsupported(update.state)
            }
        }

        if (this.connectionState != state) {
            val prevState = this.connectionState
            this.connectionState = state
            if (state == ConnectionState.CONNECTED || state == ConnectionState.WAITING_FOR_NETWORK) {
                val connectionLossTime = this.connectionLossTime
                this.connectionLossTime = 0
                val proxyId = effectiveProxyId
                cancelConnectionResolver()
                if (state == ConnectionState.CONNECTED && connectionLossTime != 0L && proxyId != Settings.PROXY_ID_UNKNOWN) {
                    val connectedWithinMs = SystemClock.uptimeMillis() - connectionLossTime
                    instance().trackSuccessfulConnection(
                        proxyId,
                        currentTimeMillis(),
                        connectedWithinMs,
                        false
                    )
                }
            } else {
                if (connectionLossTime == 0L || connectionResolver == null) {
                    scheduleConnectionResolver()
                }
                if (connectionLossTime == 0L) {
                    connectionLossTime = SystemClock.uptimeMillis()
                }
            }
            listeners.updateConnectionState(state, prevState)
            context.onConnectionStateChanged(this, state)
            notifyConnectionDisplayStatusChanged()
            if (state == ConnectionState.CONNECTED) {
                onConnected()
            } else if (prevState == ConnectionState.CONNECTED) {
                context().watchDog().helpDogeIfInBackground()
            }
        }
    }

    private fun notifyConnectionDisplayStatusChanged() {
        listeners.updateConnectionDisplayStatusChanged()
        context.onConnectionDisplayStatusChanged(this)
    }

    private var connectionResolver: CancellableRunnable? = null

    @TdlibThread
    private fun scheduleConnectionResolver() {
        cancelConnectionResolver()
        connectionResolver = object : CancellableRunnable() {
            public override fun act() {
                resolveConnectionIssues(false)
            }
        }
        val timeoutSeconds: Double
        if (effectiveProxyId == Settings.PROXY_ID_NONE) {
            timeoutSeconds = if (options.expectBlocking) CONNECTION_TIMEOUT_DIRECT_CENSORED else CONNECTION_TIMEOUT_DIRECT
        } else {
            timeoutSeconds = CONNECTION_TIMEOUT_PROXY
        }
        runOnTdlibThread(connectionResolver!!, timeoutSeconds, false)
    }

    @TdlibThread
    private fun cancelConnectionResolver() {
        if (routeSelector != null) {
            routeSelector!!.cancel()
            routeSelector = null
        }
        if (connectionResolver != null) {
            connectionResolver!!.cancel()
            connectionResolver = null
        }
    }

    fun resolveConnectionIssues() {
        runOnTdlibThread(Runnable { resolveConnectionIssues(true) })
    }

    private var routeSelector: TdlibRouteSelector? = null

    @TdlibThread
    private fun resolveConnectionIssues(byUserRequest: Boolean) {
        val settings = instance()
        if ((!byUserRequest && !settings.checkProxySetting(Settings.PROXY_FLAG_SWITCH_AUTOMATICALLY)) || !settings.hasProxyConfiguration()) {
            return
        }
        if (routeSelector != null && !byUserRequest) {
            routeSelector!!.markAsFailed(effectiveProxyId)
        }
        val routeSelector: TdlibRouteSelector?
        if (this.routeSelector == null || this.routeSelector!!.isEmpty()) {
            routeSelector = TdlibRouteSelector(
                this,
                settings.checkProxySetting(Settings.PROXY_FLAG_SWITCH_ALLOW_DIRECT),
                if (!byUserRequest) effectiveProxyId else Settings.PROXY_ID_UNKNOWN
            )
            this.routeSelector = routeSelector
        } else {
            routeSelector = this.routeSelector
        }
        if (routeSelector!!.isEmpty()) {
            notifyConnectionDisplayStatusChanged()
            return
        }
        routeSelector.findBestRoute(RunnableData { suggestedProxy: Settings.Proxy? ->
            if (!this.isConnectingOrUpdating) {
                return@RunnableData
            }
            if (suggestedProxy == null) {
                // dispatch "No better route, connecting…"
                notifyConnectionDisplayStatusChanged()
                return@RunnableData
            }
            ui()!!.post(Runnable {
                if (!routeSelector.isCanceled() && this.isConnectingOrUpdating) {
                    routeSelector.markAsPending(suggestedProxy.id)
                    if (suggestedProxy.isDirect) {
                        instance().disableProxy()
                    } else {
                        instance().addOrUpdateProxy(suggestedProxy.proxy!!, suggestedProxy.description, true, suggestedProxy.id)
                    }
                    // dispatch "Trying better route…"
                    runOnTdlibThread(Runnable { this.notifyConnectionDisplayStatusChanged() })
                }
            })
        })
        // dispatch "Finding better route…"
        notifyConnectionDisplayStatusChanged()
    }

    fun calculateConnectionTimeoutMs(timeoutMs: Long): Long {
        val time = timeSinceFirstConnectionAttemptMs()
        return if (time != 0L) max(0, timeoutMs - time) else timeoutMs
    }

    /**
     * @return Zero if connection is established or network is unavailable and amount of milliseconds otherwise.
     */
    fun timeSinceFirstConnectionAttemptMs(): Long {
        val time = connectionLossTime
        return if (time != 0L) SystemClock.uptimeMillis() - time else 0
    }

    @TdlibThread
    private fun onUpdateMyUserId(myUserId: Long) {
        context.onKnownUserIdChanged(accountId, myUserId)
        cache().onUpdateMyUserId(myUserId)
        notificationManager.onUpdateMyUserId(myUserId)
    }

    @TdlibThread
    private fun updateUnreadMessageCount(update: UpdateUnreadMessageCount) {
        if (setUnreadCounters(update.chatList, update.unreadCount, update.unreadUnmutedCount) && tempCounter.reset(this)) {
            ui()!!.sendMessage(
                ui()!!.obtainMessage(
                    MSG_ACTION_DISPATCH_UNREAD_COUNTER,
                    tempCounter.getCount(),
                    if (tempCounter.isMuted()) 1 else 0,
                    update.chatList
                )
            )
        }
    }

    @TdlibThread
    private fun updateUnreadChatCount(update: UpdateUnreadChatCount) {
        if (setUnreadChatCounters(
                update.chatList,
                update.totalCount,
                update.unreadCount,
                update.unreadUnmutedCount,
                update.markedAsUnreadCount,
                update.markedAsUnreadUnmutedCount
            ) && tempCounter.reset(this)
        ) {
            ui()!!.sendMessage(
                ui()!!.obtainMessage(
                    MSG_ACTION_DISPATCH_UNREAD_COUNTER,
                    tempCounter.getCount(),
                    if (tempCounter.isMuted()) 1 else 0,
                    update.chatList
                )
            )
        }
    }

    @TdlibThread
    private fun updateTermsOfService(update: UpdateTermsOfService?) {
        ui()!!.sendMessage(ui()!!.obtainMessage(MSG_ACTION_DISPATCH_TERMS_OF_SERVICE, update))
    }

    fun interface ApplicationVerificationCallback {
        fun onApplicationVerificationResult(data: String)
    }

    class ApplicationVerificationException : RuntimeException {
        constructor(message: String) : super(message)

        constructor(message: String, cause: Throwable?) : super(message, cause)

        companion object {
            fun formatPlayIntegrityMessage(e: Exception?): String {
                if (e == null) return "NULL"
                var str: String? = ""
                if (e.javaClass != null && e.javaClass.getSimpleName() != null) {
                    str = e.javaClass.getSimpleName()
                    if (str == null) str = ""
                }
                if (e.message != null) {
                    if (str!!.length > 0) str += " "
                    str += e.message
                }
                return str!!.uppercase(Locale.getDefault()).replace(" ".toRegex(), "_")
            }

            fun formatReCaptchaMessage(e: Exception?): String {
                if (e == null) return "NULL"
                if (e.message == null) return "MSG_NULL"
                return e.message!!.replace(" ".toRegex(), "_").uppercase(Locale.getDefault())
            }
        }
    }

    fun requestPlayIntegrity(verificationId: Long, nonce: String?, callback: ApplicationVerificationCallback) {
        TDLib.Tag.playIntegrity("Received Play Integrity request verificationId=%d", verificationId)
        val onError = RunnableData { e: Exception? ->
            TDLib.Tag.playIntegrity("failure verificationId=%d: %s", verificationId, Log.toString(e))
            val error: String?
            if (e is ApplicationVerificationException) {
                error = e.message
            } else {
                error = "PLAYINTEGRITY_FAILED_EXCEPTION_" + ApplicationVerificationException.formatPlayIntegrityMessage(e)
            }
            callback.onApplicationVerificationResult(error!!)
        }
        var projectId: Long = 0
        try {
            val options = FirebaseOptions.fromResource(UI.getAppContext())
            val projectIdRaw = if (options != null) options.getGcmSenderId() else ""
            if (!isEmpty(projectIdRaw)) {
                projectId = projectIdRaw!!.toLong()
            } else {
                throw IllegalStateException()
            }
        } catch (e: Exception) {
            onError.runWithData(ApplicationVerificationException("PLAYINTEGRITY_FAILED_EXCEPTION_NOPROJECT"))
            return
        }
        try {
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .setCloudProjectNumber(projectId)
                .build()
            val integrityManager = IntegrityManagerFactory.create(UI.getAppContext())
            val integrityTokenResponse = integrityManager.requestIntegrityToken(request)
            integrityTokenResponse
                .addOnSuccessListener(OnSuccessListener { r: IntegrityTokenResponse? ->
                    val token = r!!.token()
                    if (token != null) {
                        TDLib.Tag.playIntegrity("success verificationId=%d: %s", verificationId, token)
                        callback.onApplicationVerificationResult(token)
                    } else {
                        onError.runWithData(ApplicationVerificationException("PLAYINTEGRITY_FAILED_EXCEPTION_NULL"))
                    }
                })
                .addOnFailureListener(OnFailureListener { arg: Exception? -> onError.runWithData(arg) })
        } catch (e: Exception) {
            onError.runWithData(e)
        }
    }

    fun requestRecaptcha(verificationId: Long, action: String, recaptchaKeyId: String, callback: ApplicationVerificationCallback) {
        TDLib.Tag.recaptcha("Received ReCaptcha request verificationId=%d action=%s", verificationId, action)
        val onError = RunnableData { e: ApplicationVerificationException? ->
            TDLib.Tag.recaptcha("failure verificationId=%d: %s", verificationId, Log.toString(e))
            callback.onApplicationVerificationResult(e!!.message!!)
        }
        val actor = RunnableData<RecaptchaTasksClient> { client: RecaptchaTasksClient ->
            client.executeTask(custom(action))
                .addOnSuccessListener(OnSuccessListener { token: String? ->
                    if (token != null) {
                        TDLib.Tag.recaptcha("success verificationId=%d", verificationId)
                        callback.onApplicationVerificationResult(token)
                    } else {
                        onError.runWithData(ApplicationVerificationException("RECAPTCHA_FAILED_TOKEN_NULL"))
                    }
                })
                .addOnFailureListener(OnFailureListener { taskError: Exception? ->
                    onError.runWithData(
                        ApplicationVerificationException(
                            "RECAPTCHA_FAILED_TASK_EXCEPTION_" + ApplicationVerificationException.formatReCaptchaMessage(
                                taskError
                            ), taskError
                        )
                    )
                }
                )
        }
        RecaptchaProviderRegistry.execute(
            recaptchaKeyId,
            actor,
            RunnableData { clientError: Exception? ->
                onError.runWithData(
                    ApplicationVerificationException(
                        "RECAPTCHA_FAILED_GETCLIENT_EXCEPTION_" + ApplicationVerificationException.formatReCaptchaMessage(clientError), clientError
                    )
                )
            }
        )
    }

    private var ageVerificationParameters: AgeVerificationParameters? = null

    @TdlibThread
    private fun updateAgeVerificationParameters(update: UpdateAgeVerificationParameters) {
        synchronized(dataLock) {
            this.ageVerificationParameters = update.parameters
        }
    }

    fun ageVerificationParameters(): AgeVerificationParameters? {
        synchronized(dataLock) {
            return ageVerificationParameters
        }
    }

    @TdlibThread
    private fun updateApplicationVerificationRequired(update: UpdateApplicationVerificationRequired) {
        incrementJobReferenceCount()
        val after = Runnable { this.decrementJobReferenceCount() }
        requestPlayIntegrity(
            update.verificationId,
            update.nonce,
            ApplicationVerificationCallback { data: String ->
                send<TdApi.Ok?>(
                    SetApplicationVerificationToken(update.verificationId, data),
                    typedOkHandler(after)
                )
            }
        )
    }

    @TdlibThread
    private fun updateApplicationRecaptchaVerificationRequired(update: UpdateApplicationRecaptchaVerificationRequired) {
        incrementJobReferenceCount()
        val after = Runnable { this.decrementJobReferenceCount() }
        requestRecaptcha(
            update.verificationId,
            update.action,
            update.recaptchaKeyId,
            ApplicationVerificationCallback { data: String ->
                send<TdApi.Ok?>(
                    SetApplicationVerificationToken(update.verificationId, data),
                    typedOkHandler(after)
                )
            }
        )
    }

    @TdlibThread
    private fun updateAutosaveSettings(update: UpdateAutosaveSettings?) {
        // TODO?
    }

    private val suggestedActions: MutableList<SuggestedAction> = ArrayList<SuggestedAction>()

    @TdlibThread
    private fun updateSuggestedActions(update: UpdateSuggestedActions) {
        synchronized(dataLock) {
            for (removedAction in update.removedActions) {
                for (i in suggestedActions.indices.reversed()) {
                    if (suggestedActions.get(i).equalsTo(removedAction)) {
                        suggestedActions.removeAt(i)
                    }
                }
            }
            Collections.addAll<SuggestedAction?>(suggestedActions, *update.addedActions)
        }
        listeners().updateSuggestedActions(update)
        context().global().notifyResolvableProblemAvailabilityMightHaveChanged()
    }

    @TdlibThread
    private fun updateSpeedLimitNotification(update: UpdateSpeedLimitNotification?) {
        listeners().updateSpeedLimitNotification(update)
    }

    @AnyThread
    fun getSuggestedActions(): Array<SuggestedAction?> {
        synchronized(dataLock) {
            return suggestedActions.toTypedArray<SuggestedAction?>()
        }
    }

    private var closeBirthdayUsers: Array<CloseBirthdayUser?>?

    @TdlibThread
    private fun updateContactCloseBirthdays(update: UpdateContactCloseBirthdays) {
        synchronized(dataLock) {
            closeBirthdayUsers = update.closeBirthdayUsers
        }
        listeners().updateContactCloseBirthdayUsers(update) // TODO move to some SuggestionListener?
    }

    @AnyThread
    fun closeBirthdayUsers(): Array<CloseBirthdayUser?>? {
        synchronized(dataLock) {
            return closeBirthdayUsers
        }
    }

    private val emojiChatThemes: MutableMap<String?, EmojiChatTheme?> = HashMap<String?, EmojiChatTheme?>()

    @TdlibThread
    private fun updateChatThemes(update: UpdateEmojiChatThemes) {
        synchronized(dataLock) {
            emojiChatThemes.clear()
            for (emojiChatTheme in update.chatThemes) {
                emojiChatThemes.put(emojiChatTheme.name, emojiChatTheme)
            }
        }
    }

    @TdlibThread
    private fun updateChatBackground(update: UpdateChatBackground) {
        val chat: Chat?
        synchronized(dataLock) {
            chat = chats.get(update.chatId)
            if (TdlibUtils.assertChat(update.chatId, chat, update)) {
                return
            }
            chat!!.background = update.background
        }

        listeners.updateChatBackground(update)
    }

    private fun <T : TdApi.Update?> updateChat(update: T?, chatId: Long, chatModifier: RunnableData<Chat?>?, updateDispatcher: RunnableData<T?>?) {
        val chat: Chat?
        synchronized(dataLock) {
            chat = chats.get(chatId)
            if (TdlibUtils.assertChat(chatId, chat, update)) {
                return
            }
            if (chatModifier != null) {
                chatModifier.runWithData(chat)
            }
        }
        if (updateDispatcher != null) {
            updateDispatcher.runWithData(update)
        }
    }

    @TdlibThread
    private fun updateChatAccentColors(update: UpdateChatAccentColors) {
        updateChat<UpdateChatAccentColors?>(
            update, update.chatId, RunnableData { chat: Chat? ->
                chat!!.accentColorId = update.accentColorId
                chat.backgroundCustomEmojiId = update.backgroundCustomEmojiId
                chat.profileAccentColorId = update.profileAccentColorId
                chat.profileBackgroundCustomEmojiId = update.profileBackgroundCustomEmojiId
            },
            RunnableData { update: UpdateChatAccentColors? -> listeners.updateChatAccentColors(update) }
        )
    }

    @TdlibThread
    private fun updateChatEmojiStatus(update: UpdateChatEmojiStatus) {
        updateChat<UpdateChatEmojiStatus?>(
            update, update.chatId, RunnableData { chat: Chat? -> chat!!.emojiStatus = update.emojiStatus },
            RunnableData { update: UpdateChatEmojiStatus? -> listeners.updateChatEmojiStatus(update) }
        )
    }

    @TdlibThread
    private fun updateChatRevenueAmount(update: UpdateChatRevenueAmount?) {
        listeners.updateChatRevenueAmount(update)
    }

    @TdlibThread
    private fun updateStarRevenueStatus(update: UpdateStarRevenueStatus?) {
        listeners.updateStarRevenueStatus(update)
    }

    @TdlibThread
    private fun updateTonRevenueStatus(update: UpdateTonRevenueStatus?) {
        listeners.updateTonRevenueStatus(update)
    }

    @AnyThread
    fun emojiChatTheme(themeName: String?): EmojiChatTheme? {
        synchronized(dataLock) {
            return emojiChatThemes.get(themeName)
        }
    }

    @TdlibThread
    private fun setUnreadCounters(chatList: ChatList, unreadMessageCount: Int, unreadUnmutedCount: Int): Boolean {
        val counter = getCounter(chatList)
        if (counter.messageCount == unreadMessageCount && counter.messageUnmutedCount == unreadUnmutedCount) {
            return false
        }

        val oldUnreadCount = max(counter.messageCount, 0)
        val oldUnreadUnmutedCount = max(counter.messageUnmutedCount, 0)

        counter.messageCount = unreadMessageCount
        counter.messageUnmutedCount = unreadUnmutedCount

        account()!!.storeCounter(chatList, counter, false)
        context.incrementBadgeCounters(chatList, unreadMessageCount - oldUnreadCount, unreadUnmutedCount - oldUnreadUnmutedCount, false)
        listeners().notifyMessageCountersChanged(chatList, counter, unreadMessageCount, unreadUnmutedCount)

        return true
    }

    @UiThread
    private fun dispatchUnreadCounters(chatList: ChatList, count: Int, isMuted: Boolean) {
        context.global().notifyCountersChanged(this, chatList, count, isMuted)
    }

    @TdlibThread
    private fun setUnreadChatCounters(
        chatList: ChatList,
        totalCount: Int,
        unreadChatCount: Int,
        unreadUnmutedCount: Int,
        markedAsUnreadCount: Int,
        markedAsUnreadUnmutedCount: Int
    ): Boolean {
        val counter = getCounter(chatList)

        val oldUnreadCount = max(counter.chatCount, 0)
        val oldUnreadUnmutedCount = max(counter.chatUnmutedCount, 0)
        val oldTotalChatCount = counter.totalChatCount

        if (counter.setChatCounters(totalCount, unreadChatCount, unreadUnmutedCount, markedAsUnreadCount, markedAsUnreadUnmutedCount)) {
            account()!!.storeCounter(chatList, counter, true)
            context.incrementBadgeCounters(chatList, unreadChatCount - oldUnreadCount, unreadUnmutedCount - oldUnreadUnmutedCount, true)
            listeners().notifyChatCountersChanged(
                chatList,
                counter,
                (totalCount > 0) != (oldTotalChatCount > 0),
                totalCount,
                unreadChatCount,
                unreadUnmutedCount
            )
            return true
        }

        return false
    }

    @get:AnyThread
    val unreadBadge: TdlibBadgeCounter
        get() {
            unreadCounter.reset(this)
            return unreadCounter
        }

    @get:AnyThread
    val mainCounter: TdlibCounter
        get() = getCounter(CHAT_LIST_MAIN)

    @AnyThread
    fun hasArchivedChats(): Boolean {
        return getTotalChatsCount(CHAT_LIST_ARCHIVE) > 0
    }

    @AnyThread
    fun hasUnreadChats(chatLists: Iterable<ChatList>): Boolean {
        for (chatList in chatLists) {
            if (hasUnreadChats(chatList)) {
                return true
            }
        }
        return false
    }

    @AnyThread
    fun hasUnreadChats(chatList: ChatList): Boolean {
        return getCounter(chatList).chatCount > 0
    }

    @AnyThread
    fun getTotalChatsCount(chatList: ChatList): Int {
        synchronized(dataLock) {
            val counter = getCounter(chatList)
            return counter.totalChatCount
        }
    }

    @get:AnyThread
    val unreadBadgeCount: Int
        get() = this.unreadBadge.getCount()

    @TdlibThread
    private fun updateSpeechRecognitionTrial(update: UpdateSpeechRecognitionTrial?) {
        // TODO
    }

    @TdlibThread
    private fun updateDefaultBackground(update: UpdateDefaultBackground?) {
        // TODO ?
    }

    @TdlibThread
    private fun updateOwnedStarCount(update: UpdateOwnedStarCount?) {
        // TODO(stars)
    }

    @TdlibThread
    private fun updateOwnedTonCount(update: UpdateOwnedTonCount?) {
        // TODO(ton)
    }

    @TdlibThread
    private fun updateOption(context: ClientHolder, update: UpdateOption) {
        @TdlibOptions.UpdateResult val updateResult = options.handleUpdate(update)

        val name = update.name
        when (name) {
            "version" -> context().setTdlibVersion(update.value.stringValue())
            "commit_hash" -> context().setTdlibCommitHash(update.value.stringValue())
            "my_id" -> onUpdateMyUserId(update.value.longValue())
            "authentication_token" -> {
                val token = update.value.stringValue()
                if (!isEmpty(token)) {
                    instance().trackAuthenticationToken(token)
                }
            }

            "disable_top_chats" -> {
                if (updateResult == TdlibOptions.UpdateResult.VALUE_UPDATED) {
                    listeners().updateTopChatsDisabled(options.disableTopChats)
                }
            }

            "disable_contact_registered_notifications" -> {
                if (updateResult == TdlibOptions.UpdateResult.VALUE_UPDATED) {
                    listeners().updateContactRegisteredNotificationsDisabled(options.disableContactRegisteredNotifications)
                }
            }

            "disable_sent_scheduled_message_notifications" -> {
                if (updateResult == TdlibOptions.UpdateResult.VALUE_UPDATED) {
                    listeners().updatedSentScheduledMessageNotificationsDisabled(options.disableSentScheduledMessageNotifications)
                }
            }

            "language_pack_id" -> setLanguagePackIdImpl(update.value.stringValue(), false)
            "suggested_language_pack_id" -> {
                val languagePackId = update.value.stringValue()
                if (this.suggestedLanguagePackId == null || this.suggestedLanguagePackId != languagePackId) {
                    this.suggestedLanguagePackId = languagePackId
                    this.suggestedLanguagePackInfo = null
                    listeners().updateSuggestedLanguageChanged(languagePackId, null)
                    Companion.send<LanguagePackInfo?>(
                        context.client,
                        GetLanguagePackInfo(languagePackId),
                        Tdlib.ResultHandler { languagePackInfo: LanguagePackInfo?, error: TdApi.Error? ->
                            if (error != null) {
                                Log.e("Failed to fetch suggested language, code: %s %s", languagePackId, TD.toErrorString(error))
                                setSuggestedLanguagePackInfo(languagePackId, null)
                            } else {
                                setSuggestedLanguagePackInfo(languagePackId, languagePackInfo)
                            }
                        })
                }
            }
        }
    }

    // Updates: Accent colors
    private var availableAccentColorIds: IntArray?
    private val accentColors = SparseArrayCompat<TdlibAccentColor?>()

    @TdlibThread
    private fun updateAccentColors(update: UpdateAccentColors) {
        val listChanged: Boolean
        var updatedColorsCount = 0
        synchronized(accentColors) {
            listChanged = !this.availableAccentColorIds.contentEquals(update.availableAccentColorIds)
            this.availableAccentColorIds = update.availableAccentColorIds
            for (newColor in update.colors) {
                val oldColor = accentColors.get(newColor.id)
                if (oldColor == null) {
                    accentColors.put(newColor.id, TdlibAccentColor(newColor))
                }
                if (oldColor == null || oldColor.updateColor(newColor)) {
                    updatedColorsCount++
                }
            }
        }
        if (listChanged || updatedColorsCount > 0) {
            listeners.updateAccentColors(update)
        }
    }

    fun accentColor(accentColorId: Int): TdlibAccentColor {
        synchronized(accentColors) {
            var color = accentColors.get(accentColorId)
            if (color == null) {
                color = TdlibAccentColor(accentColorId)
                accentColors.put(accentColorId, color)
            }
            return color
        }
    }

    fun accentColorForString(any: String): TdlibAccentColor {
        return accentColor(pickNumber(TdlibAccentColor.BUILT_IN_COLOR_COUNT, any))
    }

    private var availableProfileAccentColorIds: IntArray?
    private val profileAccentColors = SparseArrayCompat<ProfileAccentColor?>()

    @TdlibThread
    private fun updateProfileAccentColors(update: UpdateProfileAccentColors) {
        val listChanged: Boolean
        synchronized(profileAccentColors) {
            listChanged = this.availableProfileAccentColorIds.contentEquals(update.availableAccentColorIds)
            this.availableProfileAccentColorIds = update.availableAccentColorIds
            for (profileAccentColor in update.colors) {
                profileAccentColors.put(profileAccentColor.id, profileAccentColor)
            }
        }
        listeners.updateProfileAccentColors(update, listChanged)
    }

    // Updates: MEDIA
    private fun updateAnimationSearchParameters(update: UpdateAnimationSearchParameters?) {
        // TODO
    }

    private fun updateInstalledStickerSets(update: UpdateInstalledStickerSets?) {
        listeners.updateInstalledStickerSets(update)
    }

    private fun updateFavoriteStickers(update: UpdateFavoriteStickers) {
        synchronized(dataLock) {
            this.favoriteStickerIds = update.stickerIds
        }
        listeners.updateFavoriteStickers(update)
    }

    private fun updateRecentStickers(update: UpdateRecentStickers?) {
        listeners.updateRecentStickers(update)
    }

    private fun updateTrendingStickerSets(update: UpdateTrendingStickerSets) {
        val unreadCount: Int
        synchronized(dataLock) {
            this.unreadTrendingStickerSetsCount = TD.calculateUnreadStickerSetCount(update.stickerSets)
            unreadCount = this.unreadTrendingStickerSetsCount
        }
        listeners.updateTrendingStickerSets(update, unreadCount)
    }

    private fun updateTrustedMiniAppBots(update: UpdateTrustedMiniAppBots) {
        synchronized(dataLock) {
            this.trustedMiniAppBotUserIds = update.botUserIds
        }
        listeners.updateTrustedMiniAppBots(update)
    }

    private fun updateSavedAnimations(update: UpdateSavedAnimations?) {
        listeners.updateSavedAnimations(update)
    }

    private fun updateDiceEmoji(emoji: Array<String?>?) {
        synchronized(dataLock) {
            this.diceEmoji = emoji
        }
    }

    @TdlibThread
    private fun updateActiveEmojiReactions(update: UpdateActiveEmojiReactions) {
        synchronized(dataLock) {
            val activeEmojiReactions: MutableSet<String> = LinkedHashSet<String>()
            Collections.addAll<String?>(activeEmojiReactions, *update.emojis)
            this.activeEmojiReactions = activeEmojiReactions
        }
    }

    @TdlibThread
    private fun updateAvailableMessageEffects(update: UpdateAvailableMessageEffects?) {
        // TODO(message-effects)
    }

    @TdlibThread
    private fun updateDefaultReactionType(update: UpdateDefaultReactionType) {
        synchronized(dataLock) {
            this.defaultReactionType = update.reactionType
        }
    }

    @TdlibThread
    private fun updateDefaultPaidReactionType(update: UpdateDefaultPaidReactionType) {
        synchronized(dataLock) {
            this.defaultPaidReactionType = update.type
        }
    }

    private fun updateStickerSet(stickerSet: TdApi.StickerSet) {
        animatedTgxEmoji.update(this, stickerSet)
        animatedDiceExplicit.update(this, stickerSet)
        listeners.updateStickerSet(stickerSet)
    }

    // Active live locations
    private fun updateActiveLiveLocationMessages(update: UpdateActiveLiveLocationMessages) {
        cache.replaceOutputLocationList(update.messages)
    }

    // Filegen
    private fun updateFileGenerationStart(update: UpdateFileGenerationStart) {
        synchronized(awaitingGenerations) {
            val generation = awaitingGenerations.remove(update.conversion)
            if (generation != null) {
                generation.isPending = true
                generation.generationId = update.generationId
                generation.destinationPath = update.destinationPath
                pendingGenerations.put(update.generationId, generation)
                generation.latch!!.countDown()
                return
            }
        }
        filegen().updateFileGenerationStart(update)
    }

    private fun updateFileGenerationStop(update: UpdateFileGenerationStop) {
        synchronized(awaitingGenerations) {
            val generation = pendingGenerations.remove(update.generationId)
            if (generation != null) {
                if (generation.file != null) {
                    fileWaitingGenerations.remove(generation.file!!.id)
                    if (generatedFilesListener != null) {
                        files().unsubscribe(generation.file!!.id, generatedFilesListener!!)
                    }
                }
                if (generation.onCancel != null) {
                    generation.onCancel!!.run()
                }
                return
            }
        }
        filegen().updateFileGenerationStop(update)
    }

    fun sendFakeUpdate(update: TdApi.Update) {
        clientHolder().sendFakeUpdate(update)
    }

    private fun processUpdate(context: ClientHolder, update: TdApi.Update) {
        when (update.getConstructor()) {
            UpdateHavePendingNotifications.CONSTRUCTOR -> onUpdateHavePendingNotifications(update as UpdateHavePendingNotifications)
            UpdateActiveNotifications.CONSTRUCTOR -> onUpdateActiveNotifications(update as UpdateActiveNotifications)
            UpdateNotificationGroup.CONSTRUCTOR -> onUpdateNotificationGroup(update as UpdateNotificationGroup)
            UpdateNotification.CONSTRUCTOR -> onUpdateNotification(update as UpdateNotification)
            UpdateSavedNotificationSounds.CONSTRUCTOR -> onUpdateSavedNotificationSounds(update as UpdateSavedNotificationSounds)
            UpdateNewMessage.CONSTRUCTOR -> {
                updateNewMessage(update as UpdateNewMessage, true)
            }

            UpdateMessageSendSucceeded.CONSTRUCTOR -> {
                updateMessageSendSucceeded(update as UpdateMessageSendSucceeded)
            }

            UpdateVideoPublished.CONSTRUCTOR -> {
                updateVideoPublished(update as UpdateVideoPublished)
            }

            UpdateMessageSendFailed.CONSTRUCTOR -> {
                updateMessageSendFailed(update as UpdateMessageSendFailed)
            }

            UpdateMessageSendAcknowledged.CONSTRUCTOR -> {
                quickAckManager.onMessageSendAcknowledged(update as UpdateMessageSendAcknowledged)
            }

            UpdateMessageContent.CONSTRUCTOR -> {
                updateMessageContent(update as UpdateMessageContent)
            }

            UpdateMessageEdited.CONSTRUCTOR -> {
                updateMessageEdited(update as UpdateMessageEdited)
            }

            UpdateMessageContentOpened.CONSTRUCTOR -> {
                updateMessageContentOpened(update as UpdateMessageContentOpened)
            }

            UpdateAnimatedEmojiMessageClicked.CONSTRUCTOR -> {
                updateAnimatedEmojiMessageClicked(update as UpdateAnimatedEmojiMessageClicked)
            }

            UpdateMessageIsPinned.CONSTRUCTOR -> {
                updateMessageIsPinned(update as UpdateMessageIsPinned)
            }

            UpdateMessageLiveLocationViewed.CONSTRUCTOR -> {
                updateLiveLocationViewed(update as UpdateMessageLiveLocationViewed)
            }

            UpdateMessageMentionRead.CONSTRUCTOR -> {
                updateMessageMentionRead(update as UpdateMessageMentionRead)
            }

            UpdateMessageInteractionInfo.CONSTRUCTOR -> {
                updateMessageInteractionInfo(update as UpdateMessageInteractionInfo)
            }

            UpdateMessageContainsUnreadPollVotes.CONSTRUCTOR -> {
                updateMessageContainsUnreadPollVotes(update as UpdateMessageContainsUnreadPollVotes)
            }

            UpdateDeleteMessages.CONSTRUCTOR -> {
                updateMessagesDeleted(update as UpdateDeleteMessages)
            }

            UpdateActiveLiveLocationMessages.CONSTRUCTOR -> {
                updateActiveLiveLocationMessages(update as UpdateActiveLiveLocationMessages)
            }

            UpdateStoryListChatCount.CONSTRUCTOR -> {
                updateStoryListChatCount(update as UpdateStoryListChatCount)
            }

            UpdateChatActiveStories.CONSTRUCTOR -> {
                updateChatActiveStories(update as UpdateChatActiveStories)
            }

            UpdateStory.CONSTRUCTOR -> {
                updateStory(update as UpdateStory)
            }

            UpdateStoryDeleted.CONSTRUCTOR -> {
                updateStoryDeleted(update as UpdateStoryDeleted)
            }

            UpdateStoryPostSucceeded.CONSTRUCTOR -> {
                updateStoryPostSucceeded(update as UpdateStoryPostSucceeded)
            }

            UpdateStoryPostFailed.CONSTRUCTOR -> {
                updateStoryPostFailed(update as UpdateStoryPostFailed)
            }

            UpdateStoryStealthMode.CONSTRUCTOR -> {
                updateStoryStealthMode(update as UpdateStoryStealthMode)
            }

            UpdateChatVideoChat.CONSTRUCTOR -> {
                updateChatVideoChat(update as UpdateChatVideoChat)
            }

            UpdateForumTopic.CONSTRUCTOR -> {
                updateForumTopic(update as UpdateForumTopic)
            }

            UpdateForumTopicInfo.CONSTRUCTOR -> {
                updateForumTopicInfo(update as UpdateForumTopicInfo)
            }

            UpdateChatViewAsTopics.CONSTRUCTOR -> {
                updateChatViewAsTopics(update as UpdateChatViewAsTopics)
            }

            UpdateDirectMessagesChatTopic.CONSTRUCTOR -> {
                updateDirectMessagesChatTopic(update as UpdateDirectMessagesChatTopic)
            }

            UpdateTopicMessageCount.CONSTRUCTOR -> {
                updateTopicMessageCount(update as UpdateTopicMessageCount)
            }

            UpdateChatPendingJoinRequests.CONSTRUCTOR -> {
                updateChatPendingJoinRequests(update as UpdateChatPendingJoinRequests)
            }

            UpdateSavedMessagesTopic.CONSTRUCTOR -> {
                updateSavedMessagesTopic(update as UpdateSavedMessagesTopic)
            }

            UpdateSavedMessagesTopicCount.CONSTRUCTOR -> {
                updateSavedMessagesTopicCount(update as UpdateSavedMessagesTopicCount)
            }

            UpdateSavedMessagesTags.CONSTRUCTOR -> {
                updateSavedMessagesTags(update as UpdateSavedMessagesTags)
            }

            UpdateActiveEmojiReactions.CONSTRUCTOR -> {
                updateActiveEmojiReactions(update as UpdateActiveEmojiReactions)
            }

            UpdateAvailableMessageEffects.CONSTRUCTOR -> {
                updateAvailableMessageEffects(update as UpdateAvailableMessageEffects)
            }

            UpdateDefaultReactionType.CONSTRUCTOR -> {
                updateDefaultReactionType(update as UpdateDefaultReactionType)
            }

            UpdateDefaultPaidReactionType.CONSTRUCTOR -> {
                updateDefaultPaidReactionType(update as UpdateDefaultPaidReactionType)
            }

            UpdateMessageUnreadReactions.CONSTRUCTOR -> {
                updateMessageUnreadReactions(update as UpdateMessageUnreadReactions)
            }

            UpdateMessageFactCheck.CONSTRUCTOR -> {
                updateMessageFactCheck(update as UpdateMessageFactCheck)
            }

            UpdateMessageSuggestedPostInfo.CONSTRUCTOR -> {
                updateSuggestedPostInfo(update as UpdateMessageSuggestedPostInfo)
            }

            UpdateChatUnreadReactionCount.CONSTRUCTOR -> {
                updateChatUnreadReactionCount(update as UpdateChatUnreadReactionCount)
            }

            UpdateChatAvailableReactions.CONSTRUCTOR -> {
                updateChatAvailableReactions(update as UpdateChatAvailableReactions)
            }

            UpdateNewChat.CONSTRUCTOR -> {
                updateNewChat(update as UpdateNewChat)
            }

            UpdateChatPermissions.CONSTRUCTOR -> {
                updateChatPermissions(update as UpdateChatPermissions)
            }

            UpdateChatOnlineMemberCount.CONSTRUCTOR -> {
                updateChatOnlineMemberCount(update as UpdateChatOnlineMemberCount)
            }

            UpdateChatMessageAutoDeleteTime.CONSTRUCTOR -> {
                updateChatMessageAutoDeleteTime(update as UpdateChatMessageAutoDeleteTime)
            }

            UpdateChatFolders.CONSTRUCTOR -> {
                updateChatFolders(update as UpdateChatFolders)
            }

            UpdateChatPosition.CONSTRUCTOR -> {
                updateChatPosition(update as UpdateChatPosition)
            }

            UpdateChatAddedToList.CONSTRUCTOR -> {
                updateChatAddedToList(update as UpdateChatAddedToList)
            }

            UpdateChatRemovedFromList.CONSTRUCTOR -> {
                updateChatRemovedFromList(update as UpdateChatRemovedFromList)
            }

            UpdateChatIsMarkedAsUnread.CONSTRUCTOR -> {
                updateChatIsMarkedAsUnread(update as UpdateChatIsMarkedAsUnread)
            }

            UpdateChatIsTranslatable.CONSTRUCTOR -> {
                updateChatIsTranslatable(update as UpdateChatIsTranslatable)
            }

            UpdateChatBlockList.CONSTRUCTOR -> {
                updateChatIsBlocked(update as UpdateChatBlockList)
            }

            UpdateChatDefaultDisableNotification.CONSTRUCTOR -> {
                updateChatDefaultDisableNotifications(update as UpdateChatDefaultDisableNotification)
            }

            UpdateChatMessageSender.CONSTRUCTOR -> {
                updateChatDefaultMessageSenderId(update as UpdateChatMessageSender)
            }

            UpdateChatUnreadMentionCount.CONSTRUCTOR -> {
                updateChatUnreadMentionCount(update as UpdateChatUnreadMentionCount)
            }

            UpdateChatUnreadPollVoteCount.CONSTRUCTOR -> {
                updateChatUnreadPollVoteCount(update as UpdateChatUnreadPollVoteCount)
            }

            UpdateChatLastMessage.CONSTRUCTOR -> {
                updateChatLastMessage(update as UpdateChatLastMessage)
            }

            UpdateChatTitle.CONSTRUCTOR -> {
                updateChatTitle(update as UpdateChatTitle)
            }

            UpdateChatTheme.CONSTRUCTOR -> {
                updateChatTheme(update as UpdateChatTheme)
            }

            UpdateChatActionBar.CONSTRUCTOR -> {
                updateChatActionBar(update as UpdateChatActionBar)
            }

            UpdateChatBusinessBotManageBar.CONSTRUCTOR -> {
                updateChatBusinessBotManageBar(update as UpdateChatBusinessBotManageBar)
            }

            UpdateChatHasScheduledMessages.CONSTRUCTOR -> {
                updateChatHasScheduledMessages(update as UpdateChatHasScheduledMessages)
            }

            UpdateChatHasProtectedContent.CONSTRUCTOR -> {
                updateChatHasProtectedContent(update as UpdateChatHasProtectedContent)
            }

            UpdateChatPhoto.CONSTRUCTOR -> {
                updateChatPhoto(update as UpdateChatPhoto)
            }

            UpdateChatReadInbox.CONSTRUCTOR -> {
                updateChatReadInbox(update as UpdateChatReadInbox)
            }

            UpdateChatReadOutbox.CONSTRUCTOR -> {
                updateChatReadOutbox(update as UpdateChatReadOutbox)
            }

            UpdateChatReplyMarkup.CONSTRUCTOR -> {
                updateChatReplyMarkup(update as UpdateChatReplyMarkup)
            }

            UpdateChatDraftMessage.CONSTRUCTOR -> {
                updateChatDraftMessage(update as UpdateChatDraftMessage)
            }

            UpdateChatAction.CONSTRUCTOR -> {
                updateChatUserAction(update as UpdateChatAction)
            }

            UpdateCall.CONSTRUCTOR -> {
                updateCall(update as UpdateCall)
            }

            UpdateNewCallSignalingData.CONSTRUCTOR -> {
                updateCallSignalingData(update as UpdateNewCallSignalingData)
            }

            UpdateGroupCall.CONSTRUCTOR -> {
                updateGroupCall(update as UpdateGroupCall)
            }

            UpdateNewGroupCallMessage.CONSTRUCTOR -> {
                updateNewGroupCallMessage(update as UpdateNewGroupCallMessage)
            }

            UpdateNewGroupCallPaidReaction.CONSTRUCTOR -> {
                updateNewGroupCallPaidReaction(update as UpdateNewGroupCallPaidReaction)
            }

            UpdateGroupCallMessageLevels.CONSTRUCTOR -> {
                updateGroupCallMessageLevels(update as UpdateGroupCallMessageLevels)
            }

            UpdateGroupCallMessageSendFailed.CONSTRUCTOR -> {
                updateGroupCallMessageSendFailed(update as UpdateGroupCallMessageSendFailed)
            }

            UpdateGroupCallMessagesDeleted.CONSTRUCTOR -> {
                updateGroupCallMessagesDeleted(update as UpdateGroupCallMessagesDeleted)
            }

            UpdateGroupCallVerificationState.CONSTRUCTOR -> {
                updateGroupCallVerificationState(update as UpdateGroupCallVerificationState)
            }

            UpdateGroupCallParticipant.CONSTRUCTOR -> {
                updateGroupCallParticipant(update as UpdateGroupCallParticipant)
            }

            UpdateGroupCallParticipants.CONSTRUCTOR -> {
                updateGroupCallParticipants(update as UpdateGroupCallParticipants)
            }

            UpdateLanguagePackStrings.CONSTRUCTOR -> {
                updateLanguagePack(update as UpdateLanguagePackStrings)
            }

            UpdateAttachmentMenuBots.CONSTRUCTOR -> {
                updateAttachmentMenuBots(update as UpdateAttachmentMenuBots)
            }

            UpdateWebAppMessageSent.CONSTRUCTOR -> {
                updateWebAppMessageSent(update as UpdateWebAppMessageSent)
            }

            UpdateChatNotificationSettings.CONSTRUCTOR -> {
                updateNotificationSettings(update as UpdateChatNotificationSettings)
            }

            UpdateScopeNotificationSettings.CONSTRUCTOR -> {
                updateNotificationSettings(update as UpdateScopeNotificationSettings)
            }

            UpdateReactionNotificationSettings.CONSTRUCTOR -> {
                updateReactionNotificationSettings(update as UpdateReactionNotificationSettings)
            }

            UpdateUserPrivacySettingRules.CONSTRUCTOR -> {
                updatePrivacySettingRules(update as UpdateUserPrivacySettingRules)
            }

            UpdateUser.CONSTRUCTOR -> {
                cache.onUpdateUser(update as UpdateUser)
            }

            UpdateUserFullInfo.CONSTRUCTOR -> {
                cache.onUpdateUserFull(update as UpdateUserFullInfo)
            }

            UpdateUserStatus.CONSTRUCTOR -> {
                cache.onUpdateUserStatus(update as UpdateUserStatus)
            }

            UpdateBasicGroup.CONSTRUCTOR -> {
                cache.onUpdateBasicGroup(update as UpdateBasicGroup)
            }

            UpdateBasicGroupFullInfo.CONSTRUCTOR -> {
                val updateBasicGroupFullInfo = update as UpdateBasicGroupFullInfo
                cache.onUpdateBasicGroupFull(updateBasicGroupFullInfo)
                refreshChatState(fromBasicGroupId(updateBasicGroupFullInfo.basicGroupId))
            }

            UpdateSupergroup.CONSTRUCTOR -> {
                updateSupergroup(update as UpdateSupergroup)
            }

            UpdateSupergroupFullInfo.CONSTRUCTOR -> {
                val updateSupergroupFullInfo = update as UpdateSupergroupFullInfo
                cache.onUpdateSupergroupFull(updateSupergroupFullInfo)
                refreshChatState(fromSupergroupId(updateSupergroupFullInfo.supergroupId))
            }

            UpdateSecretChat.CONSTRUCTOR -> {
                cache.onUpdateSecretChat(update as UpdateSecretChat)
            }

            UpdateServiceNotification.CONSTRUCTOR -> {
                updateServiceNotification(update as UpdateServiceNotification)
            }

            UpdateUnconfirmedSession.CONSTRUCTOR -> {
                updateUnconfirmedSession(update as UpdateUnconfirmedSession)
            }

            UpdateFile.CONSTRUCTOR -> {
                val updateFile = update as UpdateFile
                if (Log.isEnabled(Log.TAG_TDLIB_FILES)) {
                    Log.i(
                        Log.TAG_TDLIB_FILES,
                        "updateFile id=%d size=%d expectedSize=%d remote=%s local=%s",
                        updateFile.file.id,
                        updateFile.file.size,
                        updateFile.file.expectedSize,
                        updateFile.file.remote.toString(),
                        updateFile.file.local.toString()
                    )
                }
                updateFile(updateFile)
            }

            UpdateFileAddedToDownloads.CONSTRUCTOR -> {
                updateFileAddedToDownloads(update as UpdateFileAddedToDownloads)
            }

            UpdateFileDownload.CONSTRUCTOR -> {
                updateFileDownload(update as UpdateFileDownload)
            }

            UpdateFileDownloads.CONSTRUCTOR -> {
                updateFileDownloads(update as UpdateFileDownloads)
            }

            UpdateFileRemovedFromDownloads.CONSTRUCTOR -> {
                updateFileRemovedFromDownloads(update as UpdateFileRemovedFromDownloads)
            }

            UpdateAuthorizationState.CONSTRUCTOR -> {
                updateAuthState(context, (update as UpdateAuthorizationState).authorizationState)
            }

            UpdateFreezeState.CONSTRUCTOR -> {
                updateFreezeState(context, update as UpdateFreezeState)
            }

            UpdateConnectionState.CONSTRUCTOR -> {
                updateConnectionState(update as UpdateConnectionState)
            }

            UpdateOption.CONSTRUCTOR -> {
                updateOption(context, update as UpdateOption)
            }

            UpdateOwnedStarCount.CONSTRUCTOR -> {
                updateOwnedStarCount(update as UpdateOwnedStarCount)
            }

            UpdateOwnedTonCount.CONSTRUCTOR -> {
                updateOwnedTonCount(update as UpdateOwnedTonCount)
            }

            UpdateSpeechRecognitionTrial.CONSTRUCTOR -> {
                updateSpeechRecognitionTrial(update as UpdateSpeechRecognitionTrial)
            }

            UpdateDefaultBackground.CONSTRUCTOR -> {
                updateDefaultBackground(update as UpdateDefaultBackground)
            }

            UpdateQuickReplyShortcuts.CONSTRUCTOR -> {
                updateQuickReplyShortcuts(update as UpdateQuickReplyShortcuts)
            }

            UpdateQuickReplyShortcut.CONSTRUCTOR -> {
                updateQuickReplyShortcut(update as UpdateQuickReplyShortcut)
            }

            UpdateQuickReplyShortcutDeleted.CONSTRUCTOR -> {
                updateQuickReplyShortcutDeleted(update as UpdateQuickReplyShortcutDeleted)
            }

            UpdateQuickReplyShortcutMessages.CONSTRUCTOR -> {
                updateQuickReplyShortcutMessages(update as UpdateQuickReplyShortcutMessages)
            }


            UpdateAccentColors.CONSTRUCTOR -> {
                updateAccentColors(update as UpdateAccentColors)
            }

            UpdateProfileAccentColors.CONSTRUCTOR -> {
                updateProfileAccentColors(update as UpdateProfileAccentColors)
            }

            UpdateAnimationSearchParameters.CONSTRUCTOR -> {
                updateAnimationSearchParameters(update as UpdateAnimationSearchParameters)
            }

            UpdateInstalledStickerSets.CONSTRUCTOR -> {
                updateInstalledStickerSets(update as UpdateInstalledStickerSets)
            }

            UpdateFavoriteStickers.CONSTRUCTOR -> {
                updateFavoriteStickers(update as UpdateFavoriteStickers)
            }

            UpdateRecentStickers.CONSTRUCTOR -> {
                updateRecentStickers(update as UpdateRecentStickers)
            }

            UpdateDiceEmojis.CONSTRUCTOR -> {
                updateDiceEmoji((update as UpdateDiceEmojis).emojis)
            }

            UpdateStickerSet.CONSTRUCTOR -> {
                updateStickerSet((update as UpdateStickerSet).stickerSet)
            }

            UpdateTrendingStickerSets.CONSTRUCTOR -> {
                updateTrendingStickerSets(update as UpdateTrendingStickerSets)
            }

            UpdateTrustedMiniAppBots.CONSTRUCTOR -> {
                updateTrustedMiniAppBots(update as UpdateTrustedMiniAppBots)
            }

            UpdateSavedAnimations.CONSTRUCTOR -> {
                updateSavedAnimations(update as UpdateSavedAnimations)
            }

            UpdateUnreadMessageCount.CONSTRUCTOR -> {
                updateUnreadMessageCount(update as UpdateUnreadMessageCount)
            }

            UpdateUnreadChatCount.CONSTRUCTOR -> {
                updateUnreadChatCount(update as UpdateUnreadChatCount)
            }

            UpdateTermsOfService.CONSTRUCTOR -> {
                updateTermsOfService(update as UpdateTermsOfService)
            }

            UpdateAgeVerificationParameters.CONSTRUCTOR -> {
                updateAgeVerificationParameters(update as UpdateAgeVerificationParameters)
            }

            UpdateApplicationVerificationRequired.CONSTRUCTOR -> {
                updateApplicationVerificationRequired(update as UpdateApplicationVerificationRequired)
            }

            UpdateApplicationRecaptchaVerificationRequired.CONSTRUCTOR -> {
                updateApplicationRecaptchaVerificationRequired(update as UpdateApplicationRecaptchaVerificationRequired)
            }

            UpdateAutosaveSettings.CONSTRUCTOR -> {
                updateAutosaveSettings(update as UpdateAutosaveSettings)
            }

            UpdateSuggestedActions.CONSTRUCTOR -> {
                updateSuggestedActions(update as UpdateSuggestedActions)
            }

            UpdateSpeedLimitNotification.CONSTRUCTOR -> {
                updateSpeedLimitNotification(update as UpdateSpeedLimitNotification)
            }

            UpdateContactCloseBirthdays.CONSTRUCTOR -> {
                updateContactCloseBirthdays(update as UpdateContactCloseBirthdays)
            }

            UpdateEmojiChatThemes.CONSTRUCTOR -> {
                updateChatThemes(update as UpdateEmojiChatThemes)
            }

            UpdateChatBackground.CONSTRUCTOR -> {
                updateChatBackground(update as UpdateChatBackground)
            }

            UpdateChatAccentColors.CONSTRUCTOR -> {
                updateChatAccentColors(update as UpdateChatAccentColors)
            }

            UpdateChatEmojiStatus.CONSTRUCTOR -> {
                updateChatEmojiStatus(update as UpdateChatEmojiStatus)
            }

            UpdateChatRevenueAmount.CONSTRUCTOR -> {
                updateChatRevenueAmount(update as UpdateChatRevenueAmount)
            }

            UpdateStarRevenueStatus.CONSTRUCTOR -> {
                updateStarRevenueStatus(update as UpdateStarRevenueStatus)
            }

            UpdateTonRevenueStatus.CONSTRUCTOR -> {
                updateTonRevenueStatus(update as UpdateTonRevenueStatus)
            }

            UpdateFileGenerationStart.CONSTRUCTOR -> {
                updateFileGenerationStart(update as UpdateFileGenerationStart)
            }

            UpdateFileGenerationStop.CONSTRUCTOR -> {
                updateFileGenerationStop(update as UpdateFileGenerationStop)
            }


            TdApi.UpdatePendingTextMessage.CONSTRUCTOR, TdApi.UpdateLiveStoryTopDonors.CONSTRUCTOR, TdApi.UpdateGiftAuctionState.CONSTRUCTOR, TdApi.UpdateActiveGiftAuctions.CONSTRUCTOR, TdApi.UpdateStakeDiceState.CONSTRUCTOR, TdApi.UpdateNewOauthRequest.CONSTRUCTOR, TdApi.UpdateTextCompositionStyles.CONSTRUCTOR -> {}
            TdApi.UpdateManagedBot.CONSTRUCTOR, TdApi.UpdateNewChatJoinRequest.CONSTRUCTOR, TdApi.UpdateNewCustomEvent.CONSTRUCTOR, TdApi.UpdateNewCustomQuery.CONSTRUCTOR, TdApi.UpdateNewInlineQuery.CONSTRUCTOR, TdApi.UpdateNewChosenInlineResult.CONSTRUCTOR, TdApi.UpdateNewCallbackQuery.CONSTRUCTOR, TdApi.UpdateNewInlineCallbackQuery.CONSTRUCTOR, TdApi.UpdateNewPreCheckoutQuery.CONSTRUCTOR, TdApi.UpdateNewShippingQuery.CONSTRUCTOR, TdApi.UpdatePoll.CONSTRUCTOR, TdApi.UpdatePollAnswer.CONSTRUCTOR, TdApi.UpdateChatMember.CONSTRUCTOR, TdApi.UpdateChatBoost.CONSTRUCTOR, TdApi.UpdateMessageReaction.CONSTRUCTOR, TdApi.UpdateMessageReactions.CONSTRUCTOR, TdApi.UpdateBusinessConnection.CONSTRUCTOR, TdApi.UpdateNewBusinessMessage.CONSTRUCTOR, TdApi.UpdateBusinessMessageEdited.CONSTRUCTOR, TdApi.UpdateBusinessMessagesDeleted.CONSTRUCTOR, TdApi.UpdateNewBusinessCallbackQuery.CONSTRUCTOR, TdApi.UpdateNewGuestQuery.CONSTRUCTOR, TdApi.UpdatePaidMediaPurchased.CONSTRUCTOR -> {
                // Must never come from TDLib. If it does, there's a bug on TDLib side.
                throw unsupported(update)
            }

            else -> {
                assertUpdate_ad02591()
                throw unsupported(update)
            }
        }
    }

    // Loading user data
    @TdlibThread
    fun downloadMyUser(user: TdApi.User?) {
        val emojiStatus = if (user != null && user.isPremium) user.emojiStatus else null
        val newEmojiStatusId = emojiStatus.customEmojiId()
        val emojiEntry = if (newEmojiStatusId != 0L) emoji().find(newEmojiStatusId) else null
        val accentColor = cache().userAccentColor(user)
        val emojiStatusSticker = if (emojiEntry != null && !emojiEntry.isNotFound()) emojiEntry.value else null

        account()!!.storeUserInformation(user, if (accentColor != null) accentColor.getRemoteAccentColor() else null, emojiStatusSticker)
        downloadMyProfilePhoto(user)
        downloadMyUserEmojiStatus(user)
    }

    // Loading user data: profile photo
    private var myProfilePhoto: ProfilePhoto? = null

    @TdlibThread
    private fun downloadMyProfilePhoto(user: TdApi.User?) {
        val newProfilePhoto = if (user != null) user.profilePhoto else null
        if (newProfilePhoto == null && myProfilePhoto == null) return
        if (newProfilePhoto != null && myProfilePhoto != null && (newProfilePhoto.id == myProfilePhoto!!.id && newProfilePhoto.small.id == myProfilePhoto!!.small.id && newProfilePhoto.big.id == myProfilePhoto!!.big.id)) return
        myProfilePhoto = newProfilePhoto
        if (newProfilePhoto != null) {
            client().send(DownloadFile(newProfilePhoto.small.id, TdlibFilesManager.PRIORITY_SELF_AVATAR_SMALL, 0, 0, true), profilePhotoHandler(false))
            client().send(DownloadFile(newProfilePhoto.big.id, TdlibFilesManager.PRIORITY_SELF_AVATAR_BIG, 0, 0, true), profilePhotoHandler(true))
        }
    }

    private fun profilePhotoHandler(isBig: Boolean): Client.ResultHandler {
        return Client.ResultHandler { result: TdApi.Object? ->
            when (result!!.getConstructor()) {
                TdApi.File.CONSTRUCTOR -> {
                    val downloadedFile = result as TdApi.File
                    if (TD.isFileLoaded(downloadedFile)) {
                        val currentFile = if (myProfilePhoto != null) (if (isBig) myProfilePhoto!!.big else myProfilePhoto!!.small) else null
                        if (currentFile != null && currentFile.id == downloadedFile.id) {
                            downloadedFile.copyTo(currentFile)
                            account()!!.storeUserProfilePhotoPath(isBig, downloadedFile.local.path)
                            context.onUpdateAccountProfilePhoto(accountId, isBig)
                        }
                    }
                }

                TdApi.Error.CONSTRUCTOR -> {
                    Log.e("Failed to load avatar, accountId:%d, big:%b", accountId, isBig)
                }
            }
        }
    }

    // Loading user data: emoji status
    private var myEmojiStatusId: Long = 0

    @TdlibThread
    private fun downloadMyUserEmojiStatus(user: TdApi.User?) {
        val emojiStatus = if (user != null && user.isPremium) user.emojiStatus else null
        val newEmojiStatusId = emojiStatus.customEmojiId()
        if (newEmojiStatusId == myEmojiStatusId) return
        myEmojiStatusId = newEmojiStatusId
        if (newEmojiStatusId != 0L) {
            emoji().findOrRequest(newEmojiStatusId, RunnableData { entry: TdlibEmojiManager.Entry? ->
                if (!entry!!.isNotFound() && newEmojiStatusId == myEmojiStatusId) {
                    account()!!.storeUserEmojiStatusMetadata(newEmojiStatusId, entry.value!!)
                    client().send(
                        DownloadFile(entry.value.sticker.id, TdlibFilesManager.PRIORITY_SELF_EMOJI_STATUS, 0, 0, true),
                        emojiStatusHandler(entry, false)
                    )
                    if (entry.value.thumbnail != null) {
                        client().send(
                            DownloadFile(entry.value.thumbnail!!.file.id, TdlibFilesManager.PRIORITY_SELF_EMOJI_STATUS_THUMBNAIL, 0, 0, true),
                            emojiStatusHandler(entry, true)
                        )
                    }
                }
            })
        }
    }

    private fun emojiStatusHandler(entry: TdlibEmojiManager.Entry, isThumbnail: Boolean): Client.ResultHandler {
        return Client.ResultHandler { result: TdApi.Object? ->
            when (result!!.getConstructor()) {
                TdApi.File.CONSTRUCTOR -> {
                    val downloadedFile = result as TdApi.File
                    val targetFile = if (isThumbnail) entry.value!!.thumbnail!!.file else entry.value!!.sticker
                    if (TD.isFileLoaded(downloadedFile) && myEmojiStatusId == entry.key && targetFile.id == downloadedFile.id) {
                        downloadedFile.copyTo(targetFile)
                        account()!!.storeUserEmojiStatusPath(entry.key, entry.value, isThumbnail, downloadedFile.local.path)
                        context.onUpdateEmojiStatus(accountId, isThumbnail)
                    }
                }

                TdApi.Error.CONSTRUCTOR -> {
                    Log.e("Failed to load emoji status, accountId:%d, isThumbnail:%b", accountId, isThumbnail)
                }
            }
        }
    }

    // Periodic sync
    private var needPeriodicSync = false
    private var hasPeriodicSync = false

    private fun setNeedPeriodicSync(need: Boolean) {
        if (this.needPeriodicSync != need) {
            this.needPeriodicSync = need
            if (need) {
                schedulePeriodicSync()
            } else {
                cancelPeriodicSync()
            }
        }
    }

    private fun schedulePeriodicSync() {
        if (!hasPeriodicSync) {
            SyncHelper.register(UI.getContext(), accountId)
            this.hasPeriodicSync = true
        }
    }

    private fun cancelPeriodicSync() {
        if (hasPeriodicSync) {
            SyncHelper.cancel(UI.getContext(), accountId)
            this.hasPeriodicSync = false
        }
    }

    // Events
    private fun onJobAdded(isAboutToExecute: Boolean) {
        incrementReferenceCount(if (isAboutToExecute) REFERENCE_TYPE_TASK_EXECUTION else REFERENCE_TYPE_TASK)
    }

    private fun onJobRemoved(justFinishedExecution: Boolean) {
        decrementReferenceCount(if (justFinishedExecution) REFERENCE_TYPE_TASK_EXECUTION else REFERENCE_TYPE_TASK)
    }

    private val readyOrWaitingForDataListeners = ConditionalExecutor(FutureBool {
        val state = authorizationState
        if (state != null) {
            when (state.getConstructor()) {
                TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR, TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR, TdApi.AuthorizationStateClosing.CONSTRUCTOR, TdApi.AuthorizationStateClosed.CONSTRUCTOR -> return@FutureBool false
                TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR, TdApi.AuthorizationStateWaitEmailAddress.CONSTRUCTOR, TdApi.AuthorizationStateWaitEmailCode.CONSTRUCTOR, TdApi.AuthorizationStateWaitCode.CONSTRUCTOR, TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR, TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR, TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR, TdApi.AuthorizationStateWaitPremiumPurchase.CONSTRUCTOR, TdApi.AuthorizationStateReady.CONSTRUCTOR -> return@FutureBool true
                else -> {
                    assertAuthorizationState_ba756b5f()
                    throw unsupported(state)
                }
            }
        }
        false
    }).onAddRemove(
        RunnableBool { isAboutToExecute: Boolean -> this.onJobAdded(isAboutToExecute) },
        RunnableBool { justFinishedExecution: Boolean -> this.onJobRemoved(justFinishedExecution) })
    private val initializationListeners =
        ConditionalExecutor(FutureBool { authorizationStatus() != Status.Companion.UNKNOWN }).onAddRemove(RunnableBool { isAboutToExecute: Boolean ->
            this.onJobAdded(isAboutToExecute)
        }, RunnableBool { justFinishedExecution: Boolean -> this.onJobRemoved(justFinishedExecution) }) // Executed once received authorization state
    private val myUserOrUnauthorizedListeners = ConditionalExecutor(FutureBool {
        val status = authorizationStatus()
        status != Status.Companion.UNKNOWN && (status == Status.Companion.UNAUTHORIZED || myUser() != null)
    }).onAddRemove(
        RunnableBool { isAboutToExecute: Boolean -> this.onJobAdded(isAboutToExecute) },
        RunnableBool { justFinishedExecution: Boolean -> this.onJobRemoved(justFinishedExecution) })
    private val connectionListeners =
        ConditionalExecutor(FutureBool { authorizationStatus() != Status.Companion.UNKNOWN && connectionState == ConnectionState.CONNECTED }).onAddRemove(
            RunnableBool { isAboutToExecute: Boolean -> this.onJobAdded(isAboutToExecute) },
            RunnableBool { justFinishedExecution: Boolean -> this.onJobRemoved(justFinishedExecution) }) // Executed once connected
    private val notificationInitListeners = ConditionalExecutor(FutureBool {
        val status = authorizationStatus()
        status == Status.Companion.UNAUTHORIZED || (status == Status.Companion.READY && haveInitializedNotifications)
    }).onAddRemove(
        RunnableBool { isAboutToExecute: Boolean -> this.onJobAdded(isAboutToExecute) },
        RunnableBool { justFinishedExecution: Boolean -> this.onJobRemoved(justFinishedExecution) })
    private val notificationListeners = ConditionalExecutor(FutureBool { !havePendingNotifications }).onAddRemove(
        RunnableBool { isAboutToExecute: Boolean -> this.onJobAdded(isAboutToExecute) },
        RunnableBool { justFinishedExecution: Boolean -> this.onJobRemoved(justFinishedExecution) })
    private val notificationConsistencyListeners = ConditionalExecutor(FutureBool { false })
    private val closeListeners =
        ConditionalExecutor(FutureBool { authorizationState != null && authorizationState!!.getConstructor() == TdApi.AuthorizationStateClosed.CONSTRUCTOR }) // Executed once no pending notifications remaining
    private val noReferenceListeners = ConditionalExecutor(FutureBool { !hasActiveReferences() })

    @AnyThread
    fun awaitInitialization(after: Runnable) {
        initializationListeners.executeOrPostponeTask(after)
    }

    @AnyThread
    fun awaitMyUserOrUnauthorizedState(after: Runnable) {
        myUserOrUnauthorizedListeners.executeOrPostponeTask(after)
    }

    @AnyThread
    fun awaitAllReferencesReleased(after: Runnable) {
        noReferenceListeners.executeOrPostponeTask(after)
    }

    @AnyThread
    fun awaitNotificationInitialization(after: Runnable) {
        notificationInitListeners.executeOrPostponeTask(after)
    }

    fun awaitReadyOrWaitingForData(after: Runnable) {
        readyOrWaitingForDataListeners.executeOrPostponeTask(after)
    }

    fun awaitClose(after: Runnable, force: Boolean) {
        if (force || (authorizationState != null && authorizationState!!.getConstructor() == TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR)) {
            closeListeners.executeOrPostponeTask(after)
        } else {
            after.run()
        }
    }

    fun awaitNotifications(after: Runnable) {
        notificationListeners.executeOrPostponeTask(after)
    }

    fun awaitConnection(after: Runnable) {
        connectionListeners.executeOrPostponeTask(after)
    }

    // Events
    @TdlibThread
    private fun onInitialized() {
        initializationListeners.notifyConditionChanged(true)
        notificationInitListeners.notifyConditionChanged()
        connectionListeners.notifyConditionChanged()
    }

    @TdlibThread
    private fun runStartupChecks() {
        checkDeviceToken()
        animatedTgxEmoji.load(this)
        if (instance().getNewSetting(Settings.SETTING_FLAG_EXPLICIT_DICE) && !this.isDebugInstance) {
            animatedDiceExplicit.load(this)
        }
    }

    private fun onConnected() {
        setHasUnprocessedPushes(false)
        connectionListeners.notifyConditionChanged()
    }

    private fun onNotificationsInitialized() {
        notificationInitListeners.notifyConditionChanged(true)
    }

    fun dispatchNotificationsInitialized() {
        runOnTdlibThread(Runnable {
            if (!haveInitializedNotifications) {
                haveInitializedNotifications = true
                onNotificationsInitialized()
            }
        })
    }

    // Emoji
    fun fetchAllMessages(chatId: Long, query: String?, filter: SearchMessagesFilter?, callback: RunnableData<MutableList<TdApi.Message>>) {
        val messages: MutableList<TdApi.Message> = ArrayList<TdApi.Message>()
        val needFilter = !isEmpty(query) || filter != null
        val function: TdApi.Function<*>?
        if (needFilter) {
            function = SearchChatMessages(chatId, null, query, null, 0, 0, 100, filter)
        } else {
            function = GetChatHistory(chatId, 0, 0, 0, false)
        }
        client().send(function, object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object) {
                when (result.getConstructor()) {
                    FoundChatMessages.CONSTRUCTOR -> {
                        val fetchedMessages = result as FoundChatMessages
                        Collections.addAll<TdApi.Message?>(messages, *fetchedMessages.messages)
                        if (fetchedMessages.nextFromMessageId == 0L) {
                            callback.runWithData(messages)
                        } else {
                            (function as SearchChatMessages).fromMessageId = fetchedMessages.nextFromMessageId
                            client().send(function, this)
                        }
                    }

                    TdApi.Messages.CONSTRUCTOR -> {
                        val fetchedMessages = result as TdApi.Messages
                        if (fetchedMessages.messages.size == 0) {
                            callback.runWithData(messages)
                        } else {
                            Collections.addAll<TdApi.Message?>(messages, *fetchedMessages.messages)
                            val fromMessageId = messages.get(messages.size - 1).id
                            (function as GetChatHistory).fromMessageId = fromMessageId
                            client().send(function, this)
                        }
                    }

                    TdApi.Error.CONSTRUCTOR -> {
                        if (messages.isEmpty()) {
                            UI.showError(result)
                        }
                        callback.runWithData(messages)
                    }
                }
            }
        })
    }

    class UpdateFileInfo(@JvmField val document: TdApi.Document?, val buildNo: Int, @JvmField val version: String?, @JvmField val commit: String?)

    fun findUpdateFile(onDone: RunnableData<UpdateFileInfo?>) {
        val abiFlavor = U.getPreferredAbiFlavor()
        if (abiFlavor == null) {
            onDone.runWithData(null)
            return
        }
        val hashtag: String?
        if (!BuildConfig.LATEST_FLAVOR) {
            hashtag = abiFlavor + ucfirst(BuildConfig.FLAVOR_SDK)
        } else {
            hashtag = abiFlavor
        }
        val query = "#apk " +
                (if (instance().getNewSetting(Settings.SETTING_FLAG_DOWNLOAD_BETAS)) "" else "#stable ") +
                "#" + hashtag
        clientHolder().updates.findResource(RunnableData { message: TdApi.Message? ->
            if (message != null && message.content.isDocument()) {
                val document = (message.content as MessageDocument).document
                val caption = (message.content as MessageDocument).caption
                var ok = false
                var buildNo = 0
                var version: String? = null
                var commit: String? = null
                val prefix = "Telegram-X-"
                if (!isEmpty(document.fileName) && document.fileName.startsWith(prefix)) {
                    val i = document.fileName.indexOf('-', prefix.length)
                    version = document.fileName.substring(prefix.length, if (i == -1) document.fileName.length else i)
                    if (version.matches("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$".toRegex())) {
                        buildNo = parseInt(version.substring(version.lastIndexOf('.') + 1))
                        if (buildNo > BuildConfig.ORIGINAL_VERSION_CODE) {
                            ok = true
                        }
                    }
                }
                if (!caption.isEmpty()) {
                    val pattern = Pattern.compile("(?<=Commit:)\\s*([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE)
                    val matcher = pattern.matcher(caption.text)
                    if (matcher.find()) {
                        commit = matcher.group(1)
                        if (isEmpty(commit)) {
                            commit = null
                        }
                    }
                }
                onDone.runWithData(if (ok) UpdateFileInfo(document, buildNo, version, commit) else null)
            }
        }, query, BuildConfig.COMMIT_DATE)
    }

    fun <T : CloudSetting?> fetchCloudSettings(
        callback: RunnableData<MutableList<T?>?>,
        requiredHashtag: String?,
        currentSettingProvider: Future<T?>,
        builtinItemProvider: Future<T?>,
        instanceProvider: WrapperProvider<T?, TdApi.Message?>
    ) {
        clientHolder().resources.fetchResources(RunnableData { messages: MutableList<TdApi.Message>? ->
            val settings: MutableList<T?> = ArrayList<T?>()
            val pendingPreviews: MutableMap<String?, TdApi.File?> = HashMap<String?, TdApi.File?>()

            var foundBuiltIn = false
            val currentSetting = currentSettingProvider.getValue()
            var foundCurrent = currentSetting!!.isBuiltIn()

            for (message in messages!!) {
                val doc = (message.content as MessageDocument).document
                val caption = (message.content as MessageDocument).caption
                if (TD.hasHashtag(caption, "#preview")) {
                    pendingPreviews.put(U.getSecureFileName(doc.fileName), doc.document)
                } else {
                    try {
                        val setting = instanceProvider.getWrap(message)
                        if (!foundBuiltIn && setting!!.isBuiltIn()) foundBuiltIn = true
                        else if (!foundCurrent && setting!!.identifier == currentSetting.identifier) foundCurrent = true
                        settings.add(setting)
                    } catch (ignored: Throwable) {
                    }
                }
            }

            if (!foundBuiltIn) settings.add(0, builtinItemProvider.getValue())
            if (!foundCurrent) settings.add(currentSetting)

            Collections.sort<T?>(settings)
            for (setting in settings) {
                setting!!.setPreviewFile(this, pendingPreviews.remove(setting.identifier))
            }
            callback.runWithData(settings)
        }, me.vkryl.core.lambda.Filter { message: TdApi.Message? ->
            message!!.content.getConstructor() == MessageDocument.CONSTRUCTOR &&
                    TD.hasHashtag((message.content as MessageDocument).caption, requiredHashtag)
        }
        )
    }

    fun getEmojiPacks(callback: RunnableData<MutableList<EmojiPack?>?>) {
        fetchCloudSettings<EmojiPack?>(
            callback,
            "#emoji",
            Future { instance().getEmojiPack() },
            Future { EmojiPack() },
            WrapperProvider { message: TdApi.Message? -> Settings.EmojiPack(message!!) })
    }

    fun getIconPacks(callback: RunnableData<MutableList<IconPack?>?>) {
        fetchCloudSettings<IconPack?>(
            callback,
            "#icons",
            Future { instance().iconPack },
            Future { IconPack() },
            WrapperProvider { message: TdApi.Message? -> Settings.IconPack(message!!) })
    }

    // Chat Permissions
    @JvmOverloads
    fun canInviteUsers(chat: Chat?, allowDefault: Boolean = true): Boolean {
        if (chat == null || chat.id == 0L) {
            return false
        }
        val status = chatStatus(chat.id)
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                ChatMemberStatusAdministrator.CONSTRUCTOR -> if ((status as ChatMemberStatusAdministrator).rights.canInviteUsers) {
                    return true
                }

                ChatMemberStatusRestricted.CONSTRUCTOR -> if (!(status as ChatMemberStatusRestricted).permissions.canInviteUsers) {
                    return false
                }

                ChatMemberStatusLeft.CONSTRUCTOR, ChatMemberStatusBanned.CONSTRUCTOR, TdApi.ChatMemberStatusMember.CONSTRUCTOR -> {}
            }
        }
        return allowDefault && chat.permissions.canInviteUsers
    }

    fun canManageInviteLinks(chat: Chat?): Boolean {
        if (chat == null || chat.id == 0L) {
            return false
        }
        val status = chatStatus(chat.id)
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                ChatMemberStatusAdministrator.CONSTRUCTOR -> return ((status as ChatMemberStatusAdministrator).rights.canInviteUsers)
                else -> return false
            }
        } else {
            return false
        }
    }

    fun canCreateInviteLink(chat: Chat?): Boolean {
        return canInviteUsers(chat, false)
    }

    fun canSendPolls(chatId: Long): Boolean {
        if (chatId == 0L) return false
        if (isUserChat(chatId)) {
            return  /*isSelfChat(chatId) ||*/isBotChat(chatId)
        }
        val status = chatStatus(chatId)
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                ChatMemberStatusAdministrator.CONSTRUCTOR -> return true
                ChatMemberStatusRestricted.CONSTRUCTOR -> if (!(status as ChatMemberStatusRestricted).permissions.canSendPolls) {
                    return false
                }

                ChatMemberStatusLeft.CONSTRUCTOR, ChatMemberStatusBanned.CONSTRUCTOR, TdApi.ChatMemberStatusMember.CONSTRUCTOR -> {}
            }
        }
        val chat = chat(chatId)
        return chat != null && chat.permissions.canSendPolls
    }

    @JvmOverloads
    fun canChangeInfo(chat: Chat?, allowDefault: Boolean = true): Boolean {
        if (chat == null || chat.id == 0L || isUserChat(chat)) {
            return false
        }
        val status = chatStatus(chat.id)
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                ChatMemberStatusAdministrator.CONSTRUCTOR -> if ((status as ChatMemberStatusAdministrator).rights.canChangeInfo) {
                    return true
                }

                ChatMemberStatusRestricted.CONSTRUCTOR -> if (!(status as ChatMemberStatusRestricted).permissions.canChangeInfo) {
                    return false
                }

                ChatMemberStatusLeft.CONSTRUCTOR, ChatMemberStatusBanned.CONSTRUCTOR, TdApi.ChatMemberStatusMember.CONSTRUCTOR -> {}
            }
        }
        return allowDefault && chat.permissions.canChangeInfo
    }

    fun canRestrictMembers(chatId: Long): Boolean {
        val status = chatStatus(chatId)
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                ChatMemberStatusAdministrator.CONSTRUCTOR -> if ((status as ChatMemberStatusAdministrator).rights.canRestrictMembers) {
                    return true
                }

                ChatMemberStatusRestricted.CONSTRUCTOR, ChatMemberStatusLeft.CONSTRUCTOR, ChatMemberStatusBanned.CONSTRUCTOR, TdApi.ChatMemberStatusMember.CONSTRUCTOR -> {}
            }
        }
        return false
    }

    fun canManageTopics(chatId: Long): Boolean {
        if (!isForum(chatId)) {
            return false
        }
        val status = chatStatus(chatId)
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                ChatMemberStatusAdministrator.CONSTRUCTOR -> return (status as ChatMemberStatusAdministrator).rights.canManageTopics
                ChatMemberStatusRestricted.CONSTRUCTOR, ChatMemberStatusLeft.CONSTRUCTOR, ChatMemberStatusBanned.CONSTRUCTOR, TdApi.ChatMemberStatusMember.CONSTRUCTOR -> {}
            }
        }
        return false
    }

    fun inSlowMode(chatId: Long): Boolean {
        return cache.getSlowModeDelayExpiresIn(toSupergroupId(chatId), TimeUnit.SECONDS) > 0
    }

    fun canEditSlowMode(chatId: Long): Boolean {
        if (canRestrictMembers(chatId)) {
            val supergroup = chatToSupergroup(chatId)
            if (supergroup != null) {
                return !supergroup.isChannel && !supergroup.isBroadcastGroup
            }
            return isBasicGroup(chatId) && canUpgradeChat(chatId)
        }
        return false
    }

    fun isBroadcastGroup(chatId: Long): Boolean {
        val supergroup = chatToSupergroup(chatId)
        return supergroup != null && supergroup.isBroadcastGroup
    }

    fun suggestConvertToBroadcastGroup(chatId: Long): Boolean {
        val supergroup = chatToSupergroup(chatId)
        if (supergroup == null || supergroup.isChannel || supergroup.isBroadcastGroup || !TD.isCreator(supergroup.status)) {
            return false
        }
        if (this.isDebugInstance || BuildConfig.DEBUG) {
            return true
        }
        synchronized(dataLock) {
            for (action in suggestedActions) {
                if (action.getConstructor() == SuggestedActionConvertToBroadcastGroup.CONSTRUCTOR) {
                    val convertToBroadcastGroup = action as SuggestedActionConvertToBroadcastGroup
                    if (convertToBroadcastGroup.supergroupId == supergroup.id) {
                        return true
                    }
                }
            }
        }
        val fullInfo = cache().supergroupFull(supergroup.id, false)
        val memberCount = if (fullInfo != null) fullInfo.memberCount else supergroup.memberCount
        return memberCount + (options.supergroupSizeMax * 0.0005f) /* +50 per 100000*/ >= options.supergroupSizeMax
    }

    fun canDeleteMessages(chatId: Long): Boolean {
        val status = chatStatus(chatId)
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                ChatMemberStatusAdministrator.CONSTRUCTOR -> if ((status as ChatMemberStatusAdministrator).rights.canDeleteMessages) {
                    return true
                }

                ChatMemberStatusRestricted.CONSTRUCTOR, ChatMemberStatusLeft.CONSTRUCTOR, ChatMemberStatusBanned.CONSTRUCTOR, TdApi.ChatMemberStatusMember.CONSTRUCTOR -> {}
            }
        }
        return false
    }

    fun canToggleSignMessages(chat: Chat?): Boolean {
        return isChannelChat(chat) && canChangeInfo(chat, false)
    }

    fun haveCreatorRights(chatId: Long): Boolean {
        val status = chatStatus(chatId)
        return status != null && status.getConstructor() == TdApi.ChatMemberStatusCreator.CONSTRUCTOR
    }

    fun canToggleJoinByRequest(chat: Chat): Boolean {
        if (isMultiChat(chat)) {
            val username = chatUsername(chat)
            if (isEmpty(username)) {
                return false
            }
            if (isSupergroupChat(chat)) {
                return canRestrictMembers(chat.id)
            } else if (chat.type.getConstructor() == ChatTypeBasicGroup.CONSTRUCTOR) {
                return haveCreatorRights(chat.id)
            }
        }
        return false
    }

    fun canToggleContentProtection(chatId: Long): Boolean {
        return haveCreatorRights(chatId)
    }

    fun canToggleAllHistory(chat: Chat): Boolean {
        return ((isSupergroupChat(chat) && canChangeInfo(chat, false)) || canUpgradeChat(chat.id)) && isEmpty(chatUsername(chat.id))
    }

    fun canUpgradeChat(chatId: Long): Boolean {
        return isBasicGroup(chatId) && TD.isCreator(chatStatus(chatId))
    }

    fun canChangeMessageAutoDeleteTime(chatId: Long): Boolean {
        // Changes the message auto-delete or self-destruct (for secret chats) time in a chat.
        // Requires changeInfo administrator right in basic groups, supergroups and channels
        // Message auto-delete time can't be changed in a chat with the current user (Saved Messages) and the chat 777000 (Telegram).
        if (chatId == 0L || isSelfChat(chatId) || chatUserId(chatId) == TELEGRAM_ACCOUNT_ID) {
            return false
        }
        val status = chatStatus(chatId)
        val chat = chat(chatId)
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                ChatMemberStatusAdministrator.CONSTRUCTOR -> return (status as ChatMemberStatusAdministrator).rights.canChangeInfo
                TdApi.ChatMemberStatusMember.CONSTRUCTOR -> {}
                ChatMemberStatusRestricted.CONSTRUCTOR -> if (!(status as ChatMemberStatusRestricted).isMember || !status.permissions.canChangeInfo) {
                    return false
                }

                ChatMemberStatusBanned.CONSTRUCTOR, ChatMemberStatusLeft.CONSTRUCTOR -> return false
            }
            return chat != null && chat.permissions.canChangeInfo
        }
        return chat != null && chat.permissions.canSendBasicMessages
    }

    fun canPinMessages(chat: Chat?): Boolean {
        if (chat == null || chat.id == 0L || isSecret(chat.id)) return false
        if (isUserChat(chat.id)) return true
        val isChannel = isChannelChat(chat)
        val status = chatStatus(chat.id)
        if (isChannel) {
            if (status != null) {
                when (status.getConstructor()) {
                    TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                    ChatMemberStatusAdministrator.CONSTRUCTOR -> return (status as ChatMemberStatusAdministrator).rights.canEditMessages
                }
            }
            return false
        } else {
            if (status != null) {
                when (status.getConstructor()) {
                    TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                    ChatMemberStatusAdministrator.CONSTRUCTOR -> if ((status as ChatMemberStatusAdministrator).rights.canPinMessages) return true
                    ChatMemberStatusRestricted.CONSTRUCTOR -> if (!(status as ChatMemberStatusRestricted).permissions.canPinMessages) return false
                    ChatMemberStatusLeft.CONSTRUCTOR, ChatMemberStatusBanned.CONSTRUCTOR, TdApi.ChatMemberStatusMember.CONSTRUCTOR -> {}
                }
            }
            return chat.permissions.canPinMessages
        }
    }

    fun canKickMember(chatId: Long, member: ChatMember): Boolean {
        if (chatId == 0L) return false
        val status = chatStatus(chatId)
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR -> return true
                ChatMemberStatusAdministrator.CONSTRUCTOR -> return !TD.isAdmin(member.status) || (member.status.getConstructor() == ChatMemberStatusAdministrator.CONSTRUCTOR && (member.status as ChatMemberStatusAdministrator).canBeEdited)
            }
        }
        return member.inviterUserId == myUserId()
    }

    /*package*/
    init {
        this.context = account.context
        this.accountId = account.id
        this.hasUnprocessedPushes = account.hasUnprocessedPushes()
        this.isLoggingOut = account.isLoggingOut()

        if (instanceMode == Mode.Companion.SERVICE) {
            this.parameters = SetTdlibParameters(
                false,
                null, null, null,  // updateParameters
                false,
                false,
                false,
                false,
                BuildConfig.TELEGRAM_API_ID,
                BuildConfig.TELEGRAM_API_HASH,
                null, null, null,  // updateParameters
                BuildConfig.VERSION_NAME
            )
        } else {
            this.parameters = SetTdlibParameters(
                false,
                null, null, null,  // updateParameters
                true,
                true,
                true,
                true,
                BuildConfig.TELEGRAM_API_ID,
                BuildConfig.TELEGRAM_API_HASH,
                null, null, null,  // updateParameters
                BuildConfig.VERSION_NAME
            )
        }

        val needMeasure = Log.needMeasureLaunchSpeed()
        var ms = if (needMeasure) SystemClock.uptimeMillis() else 0
        this.listeners = TdlibListeners(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.listeners -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.cache = TdlibCache(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.cache -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.emoji = TdlibEmojiManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.emoji -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.reactions = TdlibEmojiReactionsManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.reaction -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.outline = TdlibOutlineManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.stickerOutline -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.genericReactionEffects = TdlibSingleton<Stickers?>(this, Future { GetCustomEmojiReactionAnimations() })
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.genericReactionEffects -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.filesManager = TdlibFilesManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.filesManager -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.statusManager = TdlibStatusManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.statusManager -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.contactManager = TdlibContactManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.contactManager -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.quickAckManager = TdlibQuickAckManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.quickAckManager -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.settingsManager = TdlibSettingsManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.settingsManager -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.wallpaperManager = TdlibWallpaperManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.wallpaperManager -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.notificationManager = TdlibNotificationManager(this, context.notificationQueue())
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.notificationManager -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.fileGenerationManager = TdlibFileGenerationManager(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.fileGenerationManager -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.messageViewer = TdlibMessageViewer(this)
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.messageViewer -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }
        this.unreadReactionsManager = TdlibSingleUnreadReactionsManager(this)
        this.editMediaManager = TdlibEditMediaManager(this)
        this.applicationConfigJson = settings().getApplicationConfig()
        if (!isEmpty(applicationConfigJson)) {
            val value = parse(applicationConfigJson)
            if (value != null) {
                options.handleApplicationConfig(value)
            }
        }
        if (needMeasure) {
            Log.v("INITIALIZATION: Tdlib.applicationConfig -> %dms", SystemClock.uptimeMillis() - ms)
            ms = SystemClock.uptimeMillis()
        }

        synchronized(clientLock) {
            if (client == null) client = newClient()
        }
        if (needMeasure) {
            Log.i("INITIALIZATION: Tdlib.newClient() -> %dms", SystemClock.uptimeMillis() - ms)
        }

        instance().addNewSettingsListener(this)
    }

    class RestrictionStatus(val chatId: Long, val status: Int, val untilDate: Int) {
        val isGlobal: Boolean
            get() =// Affects all users, not just me
                status != RESTRICTION_STATUS_RESTRICTED && status != RESTRICTION_STATUS_BANNED

        val isUserChat: Boolean
            get() = isUserChat(chatId)
    }

    fun hasRestriction(chatId: Long, @RightId rightId: Int): Boolean {
        return getRestrictionStatus(chat(chatId), rightId) != null
    }

    fun getRestrictionStatus(chat: Chat?, @RightId rightId: Int): RestrictionStatus? {
        if (chat == null || chat.id == 0L || !TD.isValidRight(rightId)) return null
        val status = chatStatus(chat.id)
        val isNotSpecificallyRestricted =
            status != null && (status.getConstructor() == TdApi.ChatMemberStatusMember.CONSTRUCTOR || (status.getConstructor() == ChatMemberStatusRestricted.CONSTRUCTOR &&
                    TD.checkRight((status as ChatMemberStatusRestricted).permissions, rightId)
                    )
                    )
        if (status != null) {
            when (status.getConstructor()) {
                TdApi.ChatMemberStatusCreator.CONSTRUCTOR, ChatMemberStatusAdministrator.CONSTRUCTOR -> return null
                ChatMemberStatusRestricted.CONSTRUCTOR -> return if (!TD.checkRight(chat.permissions, rightId)) RestrictionStatus(
                    chat.id,
                    RESTRICTION_STATUS_EVERYONE,
                    0
                ) else if (!TD.checkRight((status as ChatMemberStatusRestricted).permissions, rightId)) RestrictionStatus(
                    chat.id,
                    RESTRICTION_STATUS_RESTRICTED,
                    status.restrictedUntilDate
                ) else null

                ChatMemberStatusBanned.CONSTRUCTOR -> return RestrictionStatus(
                    chat.id,
                    RESTRICTION_STATUS_BANNED,
                    (status as ChatMemberStatusBanned).bannedUntilDate
                )

                ChatMemberStatusLeft.CONSTRUCTOR -> {}
                TdApi.ChatMemberStatusMember.CONSTRUCTOR -> if (isChannelChat(chat)) return RestrictionStatus(chat.id, RESTRICTION_STATUS_UNAVAILABLE, 0)
            }
        }
        when (chat.type.getConstructor()) {
            ChatTypePrivate.CONSTRUCTOR -> {
                val userId = toUserId(chat.id)
                val user = cache().user(userId)
                var isUnavailable = user == null
                if (!isUnavailable) {
                    when (user!!.type.getConstructor()) {
                        TdApi.UserTypeDeleted.CONSTRUCTOR, TdApi.UserTypeUnknown.CONSTRUCTOR -> isUnavailable = true
                    }
                }
                if (isUnavailable) return RestrictionStatus(chat.id, RESTRICTION_STATUS_UNAVAILABLE, 0)
                if (rightId == RightId.SEND_VOICE_NOTES || rightId == RightId.SEND_VIDEO_NOTES) {
                    val userFullInfo = cache().userFull(userId)
                    if (userFullInfo != null && userFullInfo.hasRestrictedVoiceAndVideoNoteMessages) {
                        return RestrictionStatus(chat.id, RESTRICTION_STATUS_RESTRICTED, 0)
                    }
                }
                if (!TD.checkRight(chat.permissions, rightId)) {
                    return RestrictionStatus(chat.id, RESTRICTION_STATUS_RESTRICTED, 0)
                }
                return null
            }

            ChatTypeSecret.CONSTRUCTOR -> {
                val secretChatId = toSecretChatId(chat.id)
                val secretChat = cache().secretChat(secretChatId)
                if (secretChat != null && secretChat.state.getConstructor() == TdApi.SecretChatStateReady.CONSTRUCTOR) {
                    val user = cache().user(secretChat.userId)
                    if (user != null) {
                        when (user.type.getConstructor()) {
                            TdApi.UserTypeDeleted.CONSTRUCTOR, TdApi.UserTypeUnknown.CONSTRUCTOR -> return RestrictionStatus(
                                chat.id,
                                RESTRICTION_STATUS_UNAVAILABLE,
                                0
                            )
                        }
                        return null
                    }
                    if (rightId == RightId.SEND_VOICE_NOTES || rightId == RightId.SEND_VIDEO_NOTES) {
                        val userFullInfo = cache().userFull(secretChat.userId)
                        if (userFullInfo != null && userFullInfo.hasRestrictedVoiceAndVideoNoteMessages) {
                            return RestrictionStatus(chat.id, RESTRICTION_STATUS_RESTRICTED, 0)
                        }
                    }
                    if (!TD.checkRight(chat.permissions, rightId)) {
                        return RestrictionStatus(chat.id, RESTRICTION_STATUS_RESTRICTED, 0)
                    }
                }
                return RestrictionStatus(chat.id, RESTRICTION_STATUS_UNAVAILABLE, 0)
            }

            ChatTypeBasicGroup.CONSTRUCTOR, ChatTypeSupergroup.CONSTRUCTOR -> {}
        }
        if (!TD.checkRight(chat.permissions, rightId)) return RestrictionStatus(
            chat.id,
            if (isNotSpecificallyRestricted) RESTRICTION_STATUS_EVERYONE else RESTRICTION_STATUS_RESTRICTED,
            0
        )
        return null
    }

    fun getSlowModeRestrictionText(chatId: Long): CharSequence? {
        return getSlowModeRestrictionText(chatId, null)
    }

    fun getSlowModeRestrictionText(chatId: Long, schedulingState: MessageSchedulingState?): CharSequence? {
        if (schedulingState != null) {
            return null
        }

        val timeToSend = cache().getSlowModeDelayExpiresIn(toSupergroupId(chatId), TimeUnit.SECONDS).toInt()
        if (timeToSend == 0) {
            return null
        }

        val minutes = timeToSend / 60
        val seconds = timeToSend % 60

        return if (minutes > 0) Lang.pluralBold(
            R.string.xSlowModeRestrictionMinutes,
            minutes.toLong()
        ) else Lang.pluralBold(R.string.xSlowModeRestrictionSeconds, seconds.toLong())
    }

    fun getRestrictionText(chat: Chat, message: TdApi.Message?): CharSequence? {
        if (message != null) {
            when (message.content.getConstructor()) {
                TdApi.MessageAnimation.CONSTRUCTOR -> return getGifRestrictionText(chat)
                TdApi.MessageSticker.CONSTRUCTOR, TdApi.MessageDice.CONSTRUCTOR -> return getStickerRestrictionText(chat)
                MessagePoll.CONSTRUCTOR, TdApi.MessageChecklist.CONSTRUCTOR -> return getDefaultRestrictionText(chat, RightId.SEND_POLLS_OR_CHECKLISTS)
                TdApi.MessageAudio.CONSTRUCTOR -> return getDefaultRestrictionText(chat, RightId.SEND_AUDIO)
                MessageDocument.CONSTRUCTOR -> return getDefaultRestrictionText(chat, RightId.SEND_DOCS)
                TdApi.MessagePhoto.CONSTRUCTOR, TdApi.MessageExpiredPhoto.CONSTRUCTOR -> return getDefaultRestrictionText(chat, RightId.SEND_PHOTOS)
                TdApi.MessageVideo.CONSTRUCTOR, TdApi.MessageExpiredVideo.CONSTRUCTOR -> return getDefaultRestrictionText(chat, RightId.SEND_VIDEOS)
                MessagePaidMedia.CONSTRUCTOR -> {
                    return getDefaultRestrictionText(chat, (message.content as MessagePaidMedia?)!!)
                }

                TdApi.MessageStory.CONSTRUCTOR -> return getStoryRestrictionText(chat)
                TdApi.MessageVideoNote.CONSTRUCTOR, TdApi.MessageExpiredVideoNote.CONSTRUCTOR -> return getDefaultRestrictionText(
                    chat,
                    RightId.SEND_VIDEO_NOTES
                )

                TdApi.MessageVoiceNote.CONSTRUCTOR, TdApi.MessageExpiredVoiceNote.CONSTRUCTOR -> return getDefaultRestrictionText(
                    chat,
                    RightId.SEND_VOICE_NOTES
                )

                MessageText.CONSTRUCTOR, MessageAnimatedEmoji.CONSTRUCTOR, TdApi.MessageVenue.CONSTRUCTOR, MessageLocation.CONSTRUCTOR, TdApi.MessageProximityAlertTriggered.CONSTRUCTOR, TdApi.MessageContact.CONSTRUCTOR, TdApi.MessageInvoice.CONSTRUCTOR, TdApi.MessagePaymentSuccessful.CONSTRUCTOR, TdApi.MessagePaymentSuccessfulBot.CONSTRUCTOR, TdApi.MessagePaymentRefunded.CONSTRUCTOR -> return getBasicMessageRestrictionText(
                    chat
                )

                TdApi.MessageGame.CONSTRUCTOR, TdApi.MessageGameScore.CONSTRUCTOR -> return getGameRestrictionText(chat)

                TdApi.MessageCall.CONSTRUCTOR, TdApi.MessageGroupCall.CONSTRUCTOR, TdApi.MessageBasicGroupChatCreate.CONSTRUCTOR, TdApi.MessageBotWriteAccessAllowed.CONSTRUCTOR, TdApi.MessageChatAddMembers.CONSTRUCTOR, TdApi.MessageChatChangePhoto.CONSTRUCTOR, TdApi.MessageChatChangeTitle.CONSTRUCTOR, TdApi.MessageChatDeleteMember.CONSTRUCTOR, TdApi.MessageChatDeletePhoto.CONSTRUCTOR, TdApi.MessageChatJoinByLink.CONSTRUCTOR, TdApi.MessageChatJoinByRequest.CONSTRUCTOR, TdApi.MessageChatSetMessageAutoDeleteTime.CONSTRUCTOR, TdApi.MessageChatSetTheme.CONSTRUCTOR, TdApi.MessageChatSetBackground.CONSTRUCTOR, TdApi.MessageChatShared.CONSTRUCTOR, TdApi.MessageChatUpgradeFrom.CONSTRUCTOR, TdApi.MessageChatUpgradeTo.CONSTRUCTOR, TdApi.MessageContactRegistered.CONSTRUCTOR, TdApi.MessageCustomServiceAction.CONSTRUCTOR, TdApi.MessageForumTopicCreated.CONSTRUCTOR, TdApi.MessageForumTopicEdited.CONSTRUCTOR, TdApi.MessageForumTopicIsClosedToggled.CONSTRUCTOR, TdApi.MessageForumTopicIsHiddenToggled.CONSTRUCTOR, TdApi.MessageGiftedPremium.CONSTRUCTOR, TdApi.MessageGiftedStars.CONSTRUCTOR, TdApi.MessageGiftedTon.CONSTRUCTOR, TdApi.MessageGift.CONSTRUCTOR, TdApi.MessageUpgradedGift.CONSTRUCTOR, TdApi.MessageUpgradedGiftPurchaseOffer.CONSTRUCTOR, TdApi.MessageUpgradedGiftPurchaseOfferRejected.CONSTRUCTOR, TdApi.MessageStakeDice.CONSTRUCTOR, TdApi.MessageRefundedUpgradedGift.CONSTRUCTOR, TdApi.MessagePaidMessagePriceChanged.CONSTRUCTOR, TdApi.MessagePaidMessagesRefunded.CONSTRUCTOR, TdApi.MessageChatBoost.CONSTRUCTOR, TdApi.MessagePremiumGiftCode.CONSTRUCTOR, TdApi.MessageGiveawayCreated.CONSTRUCTOR, TdApi.MessageGiveawayCompleted.CONSTRUCTOR, TdApi.MessageGiveawayWinners.CONSTRUCTOR, TdApi.MessageGiveaway.CONSTRUCTOR, TdApi.MessageGiveawayPrizeStars.CONSTRUCTOR, TdApi.MessageInviteVideoChatParticipants.CONSTRUCTOR, TdApi.MessagePassportDataReceived.CONSTRUCTOR, TdApi.MessagePassportDataSent.CONSTRUCTOR, TdApi.MessagePinMessage.CONSTRUCTOR, TdApi.MessageScreenshotTaken.CONSTRUCTOR, TdApi.MessageSuggestProfilePhoto.CONSTRUCTOR, TdApi.MessageSuggestBirthdate.CONSTRUCTOR, TdApi.MessageSupergroupChatCreate.CONSTRUCTOR, TdApi.MessageUnsupported.CONSTRUCTOR, TdApi.MessageUsersShared.CONSTRUCTOR, TdApi.MessageVideoChatEnded.CONSTRUCTOR, TdApi.MessageVideoChatScheduled.CONSTRUCTOR, TdApi.MessageVideoChatStarted.CONSTRUCTOR, TdApi.MessageWebAppDataReceived.CONSTRUCTOR, TdApi.MessageWebAppDataSent.CONSTRUCTOR, TdApi.MessageChecklistTasksAdded.CONSTRUCTOR, TdApi.MessageChecklistTasksDone.CONSTRUCTOR, TdApi.MessageDirectMessagePriceChanged.CONSTRUCTOR, TdApi.MessageSuggestedPostApprovalFailed.CONSTRUCTOR, TdApi.MessageSuggestedPostApproved.CONSTRUCTOR, TdApi.MessageSuggestedPostDeclined.CONSTRUCTOR, TdApi.MessageSuggestedPostPaid.CONSTRUCTOR, TdApi.MessageSuggestedPostRefunded.CONSTRUCTOR, TdApi.MessageChatHasProtectedContentDisableRequested.CONSTRUCTOR, TdApi.MessageChatHasProtectedContentToggled.CONSTRUCTOR, TdApi.MessageChatOwnerChanged.CONSTRUCTOR, TdApi.MessageChatOwnerLeft.CONSTRUCTOR, TdApi.MessageManagedBotCreated.CONSTRUCTOR, TdApi.MessagePollOptionAdded.CONSTRUCTOR, TdApi.MessagePollOptionDeleted.CONSTRUCTOR ->           // None of these messages ever passed to this method,
                    // assuming we want to check RightId.SEND_BASIC_MESSAGES
                    return getBasicMessageRestrictionText(chat)

                else -> {
                    assertMessageContent_baa076bf()
                    throw unsupported(message.content)
                }
            }
        }
        // Assuming if null is passed, we want to check if we can write text messages
        return getBasicMessageRestrictionText(chat)
    }

    fun getRestrictionText(chat: Chat, content: InputMessageContent?): CharSequence? {
        if (content != null) {
            return when (content.getConstructor()) {
                InputMessageAudio.CONSTRUCTOR -> getDefaultRestrictionText(chat, RightId.SEND_AUDIO)
                TdApi.InputMessageDocument.CONSTRUCTOR -> getDefaultRestrictionText(chat, RightId.SEND_DOCS)
                TdApi.InputMessagePhoto.CONSTRUCTOR -> getDefaultRestrictionText(chat, RightId.SEND_PHOTOS)
                TdApi.InputMessageVideo.CONSTRUCTOR -> getDefaultRestrictionText(chat, RightId.SEND_VIDEOS)
                TdApi.InputMessageVideoNote.CONSTRUCTOR -> getDefaultRestrictionText(chat, RightId.SEND_VIDEO_NOTES)
                TdApi.InputMessageVoiceNote.CONSTRUCTOR -> getDefaultRestrictionText(chat, RightId.SEND_VOICE_NOTES)
                TdApi.InputMessagePoll.CONSTRUCTOR, TdApi.InputMessageChecklist.CONSTRUCTOR -> getDefaultRestrictionText(chat, RightId.SEND_POLLS_OR_CHECKLISTS)
                InputMessageAnimation.CONSTRUCTOR -> getGifRestrictionText(chat)
                InputMessageSticker.CONSTRUCTOR -> getStickerRestrictionText(chat)
                InputMessageDice.CONSTRUCTOR -> getDiceRestrictionText(chat, (content as InputMessageDice).emoji)
                TdApi.InputMessageGame.CONSTRUCTOR -> getGameRestrictionText(chat)
                TdApi.InputMessageForwarded.CONSTRUCTOR, TdApi.InputMessageInvoice.CONSTRUCTOR, TdApi.InputMessageLocation.CONSTRUCTOR, InputMessageText.CONSTRUCTOR, TdApi.InputMessageVenue.CONSTRUCTOR, TdApi.InputMessageContact.CONSTRUCTOR, TdApi.InputMessageStory.CONSTRUCTOR, TdApi.InputMessagePaidMedia.CONSTRUCTOR, TdApi.InputMessageStakeDice.CONSTRUCTOR -> getBasicMessageRestrictionText(
                    chat
                )

                else -> {
                    assertInputMessageContent_eb9f33ef()
                    throw unsupported(content)
                }
            }
        }
        // Assuming if null is passed, we want to check if we can write text messages
        return getBasicMessageRestrictionText(chat)
    }

    fun hasReplaceMediaRestriction(message: TdApi.Message, @RightId rightId: Int): Boolean {
        if (message.mediaAlbumId == 0L || message.content == null) {
            return false
        }

        when (message.content.getConstructor()) {
            TdApi.MessagePhoto.CONSTRUCTOR, TdApi.MessageVideo.CONSTRUCTOR -> return (rightId != RightId.SEND_PHOTOS && rightId != RightId.SEND_VIDEOS)
            TdApi.MessageAudio.CONSTRUCTOR -> return rightId != RightId.SEND_AUDIO
            TdApi.MessageAnimation.CONSTRUCTOR -> return rightId != RightId.SEND_OTHER_MESSAGES
            MessageDocument.CONSTRUCTOR -> return rightId != RightId.SEND_DOCS
        }

        return true
    }

    fun getBasicMessageRestrictionText(chat: Chat): CharSequence? {
        return getDefaultRestrictionText(chat, RightId.SEND_BASIC_MESSAGES)
    }

    fun getDefaultRestrictionText(chat: Chat, content: MessagePaidMedia): CharSequence? {
        // FIXME(?): Is this a proper way to check permission for paid media?
        var photoCount = 0
        var videoCount = 0
        var otherCount = 0
        for (media in content.media) {
            when (media.getConstructor()) {
                TdApi.PaidMediaPhoto.CONSTRUCTOR -> photoCount++
                TdApi.PaidMediaVideo.CONSTRUCTOR -> videoCount++
                TdApi.PaidMediaPreview.CONSTRUCTOR -> otherCount++
                TdApi.PaidMediaUnsupported.CONSTRUCTOR -> otherCount++
                else -> {
                    assertPaidMedia_a2956719()
                    throw unsupported(media)
                }
            }
        }
        if (otherCount > 0 || (photoCount > 0 && videoCount > 0)) {
            val photoRestriction = getDefaultRestrictionText(chat, RightId.SEND_PHOTOS)
            if (!isEmpty(photoRestriction)) {
                return photoRestriction
            }
            return getDefaultRestrictionText(chat, RightId.SEND_VIDEOS)
        }
        val rightId = if (photoCount > 0) RightId.SEND_PHOTOS else RightId.SEND_VIDEOS
        return getDefaultRestrictionText(chat, rightId)
    }

    fun getDefaultRestrictionText(chat: Chat, @RightId rightId: Int): CharSequence? {
        @StringRes val disabledMediaRes: Int
        @StringRes val restrictedMediaRes: Int
        @StringRes val restrictedMediaUntilRes: Int
        @StringRes val specificRes = R.string.UserDisabledMessages
        @StringRes var specificUserRes = 0
        when (rightId) {
            RightId.SEND_BASIC_MESSAGES -> {
                disabledMediaRes = R.string.ChatDisabledMessages
                restrictedMediaRes = R.string.ChatRestrictedMessages
                restrictedMediaUntilRes = R.string.ChatRestrictedMessagesUntil
            }

            RightId.SEND_AUDIO -> {
                disabledMediaRes = R.string.ChatDisabledAudio
                restrictedMediaRes = R.string.ChatRestrictedAudio
                restrictedMediaUntilRes = R.string.ChatRestrictedAudioUntil
            }

            RightId.SEND_DOCS -> {
                disabledMediaRes = R.string.ChatDisabledDocs
                restrictedMediaRes = R.string.ChatRestrictedDocs
                restrictedMediaUntilRes = R.string.ChatRestrictedDocsUntil
            }

            RightId.SEND_PHOTOS -> {
                disabledMediaRes = R.string.ChatDisabledPhoto
                restrictedMediaRes = R.string.ChatRestrictedPhoto
                restrictedMediaUntilRes = R.string.ChatRestrictedPhotoUntil
            }

            RightId.SEND_VIDEOS -> {
                disabledMediaRes = R.string.ChatDisabledVideo
                restrictedMediaRes = R.string.ChatRestrictedVideo
                restrictedMediaUntilRes = R.string.ChatRestrictedVideoUntil
            }

            RightId.SEND_VOICE_NOTES -> {
                disabledMediaRes = R.string.ChatDisabledVoice
                restrictedMediaRes = R.string.ChatRestrictedVoice
                restrictedMediaUntilRes = R.string.ChatRestrictedVoiceUntil
                specificUserRes = R.string.XRestrictedVoiceMessages
            }

            RightId.SEND_VIDEO_NOTES -> {
                disabledMediaRes = R.string.ChatDisabledVideoNotes
                restrictedMediaRes = R.string.ChatRestrictedVideoNotes
                restrictedMediaUntilRes = R.string.ChatRestrictedVideoNotesUntil
                specificUserRes = R.string.XRestrictedVideoMessages
            }

            RightId.SEND_OTHER_MESSAGES -> {
                disabledMediaRes = R.string.ChatDisabledOther
                restrictedMediaRes = R.string.ChatRestrictedOther
                restrictedMediaUntilRes = R.string.ChatRestrictedOtherUntil
            }

            RightId.SEND_POLLS_OR_CHECKLISTS -> {
                disabledMediaRes = R.string.ChatDisabledPolls
                restrictedMediaRes = R.string.ChatRestrictedPolls
                restrictedMediaUntilRes = R.string.ChatRestrictedPollsUntil
            }

            else -> throw IllegalArgumentException(Lang.getResourceEntryName(rightId))
        }
        return buildRestrictionText(
            chat,
            rightId,
            disabledMediaRes, restrictedMediaRes, restrictedMediaUntilRes,
            specificRes, specificUserRes
        )
    }

    fun getVoiceVideoRestrictionText(chat: Chat, needVideo: Boolean): CharSequence? {
        return getDefaultRestrictionText(chat, if (needVideo) RightId.SEND_VIDEO_NOTES else RightId.SEND_VOICE_NOTES)
    }

    fun getGifRestrictionText(chat: Chat): CharSequence? {
        return buildRestrictionText(chat, RightId.SEND_OTHER_MESSAGES, R.string.ChatDisabledGifs, R.string.ChatRestrictedGifs, R.string.ChatRestrictedGifsUntil)
    }

    fun getStickerRestrictionText(chat: Chat): CharSequence? {
        return buildRestrictionText(
            chat,
            RightId.SEND_OTHER_MESSAGES,
            R.string.ChatDisabledStickers,
            R.string.ChatRestrictedStickers,
            R.string.ChatRestrictedStickersUntil
        )
    }

    fun getGameRestrictionText(chat: Chat): CharSequence? {
        return buildRestrictionText(
            chat,
            RightId.SEND_OTHER_MESSAGES,
            R.string.ChatDisabledGames,
            R.string.ChatRestrictedGames,
            R.string.ChatRestrictedGamesUntil
        )
    }

    fun getStoryRestrictionText(chat: Chat): CharSequence? {
        val photoStatus = getRestrictionStatus(chat, RightId.SEND_PHOTOS)
        val videoStatus = getRestrictionStatus(chat, RightId.SEND_VIDEOS)
        val status: RestrictionStatus?
        @RightId val rightId: Int
        if (photoStatus == null || videoStatus == null) {
            if (videoStatus != null) {
                rightId = RightId.SEND_VIDEOS
                status = videoStatus
            } else {
                rightId = RightId.SEND_PHOTOS
                status = photoStatus
            }
        } else if (photoStatus.isGlobal != videoStatus.isGlobal) {
            if (photoStatus.isGlobal) {
                rightId = RightId.SEND_VIDEOS
                status = videoStatus
            } else {
                rightId = RightId.SEND_PHOTOS
                status = photoStatus
            }
        } else {
            status = videoStatus
            rightId = RightId.SEND_VIDEOS
        }
        return buildRestrictionText(
            status,
            chat, rightId,
            R.string.ChatDisabledStory, R.string.ChatRestrictedStory, R.string.ChatRestrictedStoryUntil
        )
    }

    fun getInlineRestrictionText(chat: Chat): CharSequence? {
        return buildRestrictionText(chat, RightId.SEND_OTHER_MESSAGES, R.string.ChatDisabledBots, R.string.ChatRestrictedBots, R.string.ChatRestrictedBotsUntil)
    }

    fun getPollRestrictionText(chat: Chat): CharSequence? {
        return getDefaultRestrictionText(chat, RightId.SEND_POLLS_OR_CHECKLISTS)
    }

    fun getDiceRestrictionText(chat: Chat, emoji: String?): CharSequence? {
        val disabledRes: Int
        val restrictedRes: Int
        val restrictedUntilRes: Int
        if (ContentPreview.EMOJI_DART.textRepresentation == emoji) {
            disabledRes = R.string.ChatDisabledDart
            restrictedRes = R.string.ChatRestrictedDart
            restrictedUntilRes = R.string.ChatRestrictedDartUntil
        } else if (ContentPreview.EMOJI_DICE.textRepresentation == emoji) {
            disabledRes = R.string.ChatDisabledDice
            restrictedRes = R.string.ChatRestrictedDice
            restrictedUntilRes = R.string.ChatRestrictedDiceUntil
        } else {
            disabledRes = R.string.ChatDisabledStickers
            restrictedRes = R.string.ChatRestrictedStickers
            restrictedUntilRes = R.string.ChatRestrictedStickersUntil
        }
        return buildRestrictionText(chat, RightId.SEND_OTHER_MESSAGES, disabledRes, restrictedRes, restrictedUntilRes)
    }

    @JvmOverloads
    fun buildRestrictionText(
        chat: Chat, @RightId rightId: Int,
        @StringRes defaultRes: Int, @StringRes specificRes: Int, @StringRes specificUntilRes: Int,
        @StringRes defaultUserRes: Int = R.string.UserDisabledMessages, @StringRes specificUserRes: Int = 0
    ): CharSequence? {
        val status = getRestrictionStatus(chat, rightId)
        return buildRestrictionText(
            status,
            chat, rightId,
            defaultRes, specificRes, specificUntilRes,
            defaultUserRes, specificUserRes
        )
    }

    fun buildRestrictionText(
        status: RestrictionStatus?,
        chat: Chat,
        @RightId rightId: Int,
        @StringRes defaultRes: Int,
        @StringRes specificRes: Int,
        @StringRes specificUntilRes: Int
    ): CharSequence? {
        return buildRestrictionText(status, chat, rightId, defaultRes, specificRes, specificUntilRes, R.string.UserDisabledMessages, 0)
    }

    fun buildRestrictionText(
        status: RestrictionStatus?,
        chat: Chat, @RightId rightId: Int,
        @StringRes defaultRes: Int, @StringRes specificRes: Int, @StringRes specificUntilRes: Int,
        @StringRes defaultUserRes: Int, @StringRes specificUserRes: Int
    ): CharSequence? {
        if (status != null) {
            when (rightId) {
                RightId.SEND_BASIC_MESSAGES, RightId.SEND_AUDIO, RightId.SEND_DOCS, RightId.SEND_PHOTOS, RightId.SEND_VIDEOS, RightId.SEND_VOICE_NOTES, RightId.SEND_VIDEO_NOTES, RightId.SEND_OTHER_MESSAGES, RightId.SEND_POLLS_OR_CHECKLISTS, RightId.REACT_TO_MESSAGES -> {}
                RightId.EMBED_LINKS -> {
                    // check if there is any restriction text for RightId.SEND_BASIC_MESSAGES
                    val restriction = getBasicMessageRestrictionText(chat)
                    if (restriction != null) return restriction
                }

                RightId.ADD_NEW_ADMINS, RightId.BAN_USERS, RightId.CHANGE_CHAT_INFO, RightId.DELETE_MESSAGES, RightId.EDIT_MESSAGES, RightId.INVITE_USERS, RightId.MANAGE_VIDEO_CHATS, RightId.MANAGE_OR_CREATE_TOPICS, RightId.EDIT_OR_MANAGE_TAGS, RightId.MANAGE_DIRECT_MESSAGES, RightId.POST_STORIES, RightId.EDIT_STORIES, RightId.DELETE_STORIES, RightId.PIN_MESSAGES, RightId.READ_MESSAGES, RightId.REMAIN_ANONYMOUS -> {}
            }
            if (status.isUserChat) {
                when (status.status) {
                    RESTRICTION_STATUS_RESTRICTED -> if (specificUserRes != 0) {
                        return Lang.getStringBold(specificUserRes, cache().userFirstName(chatUserId(chat)))
                    }

                    RESTRICTION_STATUS_EVERYONE -> return Lang.getString(defaultUserRes)
                }
            }
            when (status.status) {
                RESTRICTION_STATUS_BANNED -> return if (status.untilDate != 0) Lang.getString(
                    R.string.ChatBannedUntil,
                    Lang.getUntilDate(status.untilDate.toLong(), TimeUnit.SECONDS)
                ) else Lang.getString(R.string.ChatBanned)

                RESTRICTION_STATUS_RESTRICTED -> return if (status.untilDate != 0) Lang.getString(
                    specificUntilRes,
                    Lang.getUntilDate(status.untilDate.toLong(), TimeUnit.SECONDS)
                ) else Lang.getString(specificRes)

                RESTRICTION_STATUS_UNAVAILABLE, RESTRICTION_STATUS_EVERYONE -> return Lang.getString(defaultRes)
            }

            throw UnsupportedOperationException()
        }
        return null
    }

    fun showRestriction(chat: Chat, @RightId rightId: Int, @StringRes defaultRes: Int, @StringRes specificRes: Int, @StringRes specificUntilRes: Int): Boolean {
        val res = buildRestrictionText(chat, rightId, defaultRes, specificRes, specificUntilRes)
        if (res != null) {
            UI.showToast(res, Toast.LENGTH_SHORT)
            return true
        }
        return false
    }

    fun canAddWebPagePreviews(chat: Chat?): Boolean {
        return getRestrictionStatus(chat, RightId.EMBED_LINKS) == null
    }

    fun canSendBasicMessage(chatId: Long): Boolean {
        return chatId != 0L && canSendBasicMessage(chat(chatId))
    }

    fun canSendBasicMessage(chat: Chat?): Boolean {
        return canSendMessage(chat, RightId.SEND_BASIC_MESSAGES)
    }

    @JvmOverloads
    fun canSendSendSomeMedia(chat: Chat?, checkGlobal: Boolean = false): Boolean {
        for (rightId in EditRightsController.SEND_MEDIA_RIGHT_IDS) {
            val restrictionStatus = getRestrictionStatus(chat, rightId)
            if (restrictionStatus == null || checkGlobal && !restrictionStatus.isGlobal) {
                return true
            }
        }

        return false
    }

    fun canSendMessage(chat: Chat?, @RightId kindResId: Int): Boolean {
        when (kindResId) {
            RightId.SEND_BASIC_MESSAGES, RightId.SEND_AUDIO, RightId.SEND_DOCS, RightId.SEND_PHOTOS, RightId.SEND_VIDEOS, RightId.SEND_VOICE_NOTES, RightId.SEND_VIDEO_NOTES, RightId.SEND_OTHER_MESSAGES, RightId.SEND_POLLS_OR_CHECKLISTS -> {}
            RightId.EMBED_LINKS, RightId.REACT_TO_MESSAGES, RightId.ADD_NEW_ADMINS, RightId.BAN_USERS, RightId.CHANGE_CHAT_INFO, RightId.DELETE_MESSAGES, RightId.EDIT_MESSAGES, RightId.INVITE_USERS, RightId.MANAGE_VIDEO_CHATS, RightId.MANAGE_OR_CREATE_TOPICS, RightId.EDIT_OR_MANAGE_TAGS, RightId.MANAGE_DIRECT_MESSAGES, RightId.POST_STORIES, RightId.EDIT_STORIES, RightId.DELETE_STORIES, RightId.PIN_MESSAGES, RightId.READ_MESSAGES, RightId.REMAIN_ANONYMOUS -> throw IllegalArgumentException(
                Lang.getResourceEntryName(kindResId)
            )

            else -> throw IllegalArgumentException(Lang.getResourceEntryName(kindResId))
        }
        return getRestrictionStatus(chat, kindResId) == null
    }

    val settingSuggestionCount: Int
        get() {
            synchronized(dataLock) {
                var count = 0
                for (action in suggestedActions) {
                    if (isSettingSuggestion(action)) {
                        count++
                    }
                }
                return count
            }
        }

    fun singleUnreadReactionsManager(): TdlibSingleUnreadReactionsManager {
        return unreadReactionsManager
    }

    fun getSingleUnreadReaction(chatId: Long): UnreadReaction? {
        // If chat has one unread reaction, returns it. May be null
        return unreadReactionsManager.getSingleUnreadReaction(chatId)
    }

    fun haveAnySettingsSuggestions(): Boolean {
        synchronized(dataLock) {
            for (action in suggestedActions) {
                if (isSettingSuggestion(action)) return true
            }
            return false
        }
    }

    fun singleSettingsSuggestion(): SuggestedAction? {
        synchronized(dataLock) {
            var suggestedAction: SuggestedAction? = null
            for (action in suggestedActions) {
                if (isSettingSuggestion(action)) {
                    if (suggestedAction == null) {
                        suggestedAction = action
                    } else {
                        return null
                    }
                }
            }
            return suggestedAction
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ResolvableProblem.NONE, ResolvableProblem.MIXED, ResolvableProblem.NOTIFICATIONS, ResolvableProblem.CHECK_PASSWORD, ResolvableProblem.CHECK_PHONE_NUMBER, ResolvableProblem.SET_BIRTHDATE, ResolvableProblem.SET_LOGIN_EMAIL
    )
    annotation class ResolvableProblem {
        companion object {
            const val NONE: Int = 0
            const val MIXED: Int = 1
            const val NOTIFICATIONS: Int = 2
            const val CHECK_PASSWORD: Int = 3
            const val CHECK_PHONE_NUMBER: Int = 4
            const val SET_BIRTHDATE: Int = 5
            const val SET_LOGIN_EMAIL: Int = 6
        }
    }

    @ResolvableProblem
    fun findResolvableProblem(): Int {
        val haveNotificationsProblem = notifications().hasLocalNotificationProblem()
        val singleAction = singleSettingsSuggestion()
        val haveSuggestions = singleAction != null || haveAnySettingsSuggestions()
        val totalCount = (if (singleAction != null) 1 else if (haveSuggestions) 2 else 0) + (if (haveNotificationsProblem) 1 else 0)
        if (totalCount > 1) {
            return ResolvableProblem.MIXED
        } else if (haveNotificationsProblem) {
            return ResolvableProblem.NOTIFICATIONS
        } else if (singleAction != null) {
            when (singleAction.getConstructor()) {
                TdApi.SuggestedActionCheckPassword.CONSTRUCTOR -> return ResolvableProblem.CHECK_PASSWORD
                TdApi.SuggestedActionCheckPhoneNumber.CONSTRUCTOR -> return ResolvableProblem.CHECK_PHONE_NUMBER
                TdApi.SuggestedActionSetBirthdate.CONSTRUCTOR -> return ResolvableProblem.SET_BIRTHDATE
                TdApi.SuggestedActionSetLoginEmailAddress.CONSTRUCTOR -> return ResolvableProblem.SET_LOGIN_EMAIL
                else -> {
                    assertSuggestedAction_a78df4c9()
                    throw unsupported(singleAction)
                }
            }
        }
        return ResolvableProblem.NONE
    }

    fun getMessageSenderTitle(sender: MessageSender?): String? {
        if (isSelfSender(sender)) {
            return Lang.getString(R.string.YourAccount)
        } else if (!isChannel(sender)) {
            return Lang.getString(R.string.AnonymousAdmin)
        } else {
            return chatTitle(sender.getSenderId())
        }
    }

    fun defaultChatFolderIcon(chatFolder: ChatFolder): ChatFolderIcon? {
        var checkChatFolder = chatFolder
        if (checkChatFolder.icon != null) {
            checkChatFolder = checkChatFolder.copyOf()
            checkChatFolder.icon = null
        }
        val defaultIcon: ChatFolderIcon? = executeOrNull<ChatFolderIcon?>(GetChatFolderDefaultIconName(checkChatFolder))
        if (!defaultIcon.isEmpty()) {
            return defaultIcon
        }
        return null
    }

    fun chatFolderIcon(chatFolder: ChatFolder): ChatFolderIcon? {
        if (!chatFolder.icon.isEmpty()) {
            return chatFolder.icon
        }
        return defaultChatFolderIcon(chatFolder)
    }

    fun setChatFolderIcon(chatFolderId: Int, icon: ChatFolderIcon?, unsetOnDefault: Boolean) {
        send<ChatFolder?>(GetChatFolder(chatFolderId), Tdlib.ResultHandler { chatFolder: ChatFolder?, error: TdApi.Error? ->
            if (chatFolder != null) {
                var newIcon = icon
                if (!newIcon.isEmpty() && unsetOnDefault) {
                    val defaultIcon = defaultChatFolderIcon(chatFolder)
                    if (newIcon.equalsTo(defaultIcon)) {
                        newIcon = null
                    }
                }
                if (!chatFolder.icon.equalsTo(newIcon)) {
                    chatFolder.icon = newIcon
                    send<ChatFolderInfo?>(
                        EditChatFolder(chatFolderId, chatFolder),
                        Tdlib.ResultHandler { chatFolderInfo: ChatFolderInfo?, setIconError: TdApi.Error? ->
                            if (setIconError != null) {
                                UI.showError(setIconError)
                            }
                        })
                }
            }
        })
    }

    fun chatFolderIconName(chatFolder: ChatFolder): String? {
        val icon = chatFolderIcon(chatFolder)
        return if (icon != null) icon.name else ""
    }

    @DrawableRes
    fun chatFolderIconDrawable(chatFolder: ChatFolder, @DrawableRes defaultIcon: Int): Int {
        return TD.findFolderIcon(chatFolderIcon(chatFolder), defaultIcon)
    }

    fun processChatFolderNewChats(chatFolderId: Int, addedChatIds: LongArray?, resultHandler: ResultHandler<TdApi.Ok?>) {
        send<TdApi.Ok?>(ProcessChatFolderNewChats(chatFolderId, addedChatIds), resultHandler.doOnResult(RunnableData { result: TdApi.Ok? ->
            listeners().notifyChatFolderNewChatsChanged(chatFolderId)
        }))
    }

    fun deleteChatFolder(chatFolderId: Int, leaveChatIds: LongArray?, after: Runnable?) {
        val update: UpdateChatFolders?
        synchronized(dataLock) {
            var foundIndex = -1
            for (i in chatFolders!!.indices) {
                val chatFolderInfo = chatFolders!![i]
                if (chatFolderInfo.id == chatFolderId) {
                    foundIndex = i
                    break
                }
            }
            if (foundIndex != -1) {
                @Suppress("UNCHECKED_CAST")
                chatFolders = chatFolders!!.removeElement(foundIndex, arrayOfNulls<ChatFolderInfo>(chatFolders!!.size - 1) as Array<ChatFolderInfo>)
                if (mainChatListPosition > foundIndex) {
                    mainChatListPosition--
                }
                update = UpdateChatFolders(chatFolders, mainChatListPosition, areTagsEnabled)
            } else {
                update = null
            }
        }
        if (update != null) {
            // Send fake update without a deleting folder.
            updateChatFolders(update)
        }
        send<TdApi.Ok?>(DeleteChatFolder(chatFolderId, leaveChatIds), typedOkHandler(Runnable {
            settings().forgetChatFolder(chatFolderId)
            if (after != null) {
                after.run()
            }
        }))
    }

    fun deleteChatFolderInviteLink(chatFolderId: Int, inviteLink: String?, resultHandler: ResultHandler<TdApi.Ok?>) {
        send<TdApi.Ok?>(DeleteChatFolderInviteLink(chatFolderId, inviteLink), resultHandler.doOnResult(RunnableData { result: TdApi.Ok? ->
            listeners().notifyChatFolderInviteLinkDeleted(chatFolderId, inviteLink)
        }))
    }

    fun createChatFolderInviteLink(chatFolderId: Int, name: String?, chatIds: LongArray?, resultHandler: ResultHandler<ChatFolderInviteLink?>) {
        send<ChatFolderInviteLink?>(
            CreateChatFolderInviteLink(chatFolderId, name, chatIds),
            resultHandler.doOnResult(RunnableData { result: ChatFolderInviteLink? ->
                listeners().notifyChatFolderInviteLinkCreated(chatFolderId, result)
            })
        )
    }

    fun editChatFolderInviteLink(
        chatFolderId: Int,
        inviteLink: String?,
        name: String?,
        chatIds: LongArray?,
        resultHandler: ResultHandler<ChatFolderInviteLink?>
    ) {
        send<ChatFolderInviteLink?>(
            EditChatFolderInviteLink(chatFolderId, inviteLink, name, chatIds),
            resultHandler.doOnResult(RunnableData { result: ChatFolderInviteLink? ->
                listeners().notifyChatFolderInviteLinkChanged(chatFolderId, result)
            })
        )
    }

    class UploadFutureSimple(
        private val tdlib: Tdlib,
        private val inputFile: TdApi.InputFile?,
        private val fileType: TdApi.FileType?,
        private val callback: Callback
    ) : TdlibFilesManager.FileListener {
        private var flags = 0

        interface Callback {
            fun onUploadStart(future: UploadFutureSimple?, file: TdApi.File?, error: TdApi.Error?)
            fun onUploadFinish(future: UploadFutureSimple?)
        }

        @JvmField
        var file: TdApi.File? = null

        fun init() {
            tdlib.send<TdApi.File?>(PreliminaryUploadFile(inputFile, fileType, 1), Tdlib.ResultHandler { file: TdApi.File?, err: TdApi.Error? ->
                this.file = file
                if (hasFlag(flags, FLAG_FAILED)) {
                    return@ResultHandler
                }

                val isUploaded = TD.isFileUploaded(file)
                if (file != null && err == null && !isUploaded) {
                    tdlib.files().subscribe(file, this)
                }
                UI.post(Runnable {
                    if (hasFlag(flags, FLAG_FAILED)) {
                        return@Runnable
                    }
                    update(file)
                    flags = setFlag(flags, FLAG_STARTED, true)
                    callback.onUploadStart(this, file, err)

                    if (err != null) {
                        fail(true)
                    }
                    if (isUploaded) {
                        complete()
                    }
                })
            })
        }

        @UiThread
        private fun complete() {
            if (!hasFlag(flags, FLAG_COMPLETED or FLAG_FAILED)) {
                flags = setFlag(flags, FLAG_COMPLETED, true)
                if (file != null) {
                    tdlib.files().unsubscribe(file!!.id, this)
                }
                callback.onUploadFinish(this)
            }
        }

        @UiThread
        private fun update(file: TdApi.File?) {
            this.file = file
        }

        @UiThread
        private fun fail(runCallback: Boolean) {
            if (!hasFlag(flags, FLAG_COMPLETED or FLAG_FAILED)) {
                flags = setFlag(flags, FLAG_FAILED, true)
                if (file != null) {
                    tdlib.files().unsubscribe(file!!.id, this)
                }
                if (runCallback) {
                    callback.onUploadFinish(this)
                }
            }
        }

        fun cancel(runCallback: Boolean) {
            fail(runCallback)
        }

        override fun onFileLoadProgress(file: TdApi.File?) {
            UI.execute(Runnable { update(file) })
        }

        override fun onFileLoadStateChanged(tdlib: Tdlib?, fileId: Int, state: Int, downloadedFile: TdApi.File?) {
            UI.execute(Runnable {
                if (downloadedFile != null) {
                    update(downloadedFile)
                }
                if (state == TdlibFilesManager.STATE_DOWNLOADED_OR_UPLOADED) {
                    complete()
                }
                if (state == TdlibFilesManager.STATE_FAILED || state == TdlibFilesManager.STATE_PAUSED) {
                    fail(true)
                }
            })
        }

        val isFinished: Boolean
            get() = hasFlag(flags, FLAG_COMPLETED or FLAG_FAILED)

        val isCompleted: Boolean
            get() = hasFlag(flags, FLAG_COMPLETED)

        val isFailed: Boolean
            get() = hasFlag(flags, FLAG_FAILED)

        val isStarted: Boolean
            get() = hasFlag(flags, FLAG_STARTED)

        companion object {
            private const val FLAG_STARTED = 1
            private val FLAG_COMPLETED = 1 shl 1
            private val FLAG_FAILED = 1 shl 2
        }
    }

    companion object {
        const val CHAT_ACCESS_TEMPORARY: Int = 1
        const val CHAT_ACCESS_OK: Int = 0
        val CHAT_ACCESS_FAIL: Int = -1
        val CHAT_ACCESS_BANNED: Int = -2 // Access banned
        val CHAT_ACCESS_PRIVATE: Int = -3 // Needs an invitation

        // Use count
        private const val REFERENCE_TYPE_UI = 0
        private const val REFERENCE_TYPE_JOB = 1
        private const val REFERENCE_TYPE_SYNC = 3
        private const val REFERENCE_TYPE_NOTIFICATION = 4
        private const val REFERENCE_TYPE_LOCATION = 5
        private const val REFERENCE_TYPE_CALL = 6
        private const val REFERENCE_TYPE_REQUEST_EXECUTION = 7
        private const val REFERENCE_TYPE_MESSAGE = 8
        private const val REFERENCE_TYPE_TASK = 9
        private const val REFERENCE_TYPE_TASK_EXECUTION = 10

        @Status
        private fun getStatus(state: AuthorizationState?): Int {
            if (state == null) return Status.Companion.UNKNOWN
            when (state.getConstructor()) {
                TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR, TdApi.AuthorizationStateClosing.CONSTRUCTOR, TdApi.AuthorizationStateClosed.CONSTRUCTOR -> return Status.Companion.UNKNOWN
                TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR, TdApi.AuthorizationStateWaitEmailAddress.CONSTRUCTOR, TdApi.AuthorizationStateWaitCode.CONSTRUCTOR, TdApi.AuthorizationStateWaitEmailCode.CONSTRUCTOR, TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR, TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR, TdApi.AuthorizationStateWaitPremiumPurchase.CONSTRUCTOR, TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR, TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR -> return Status.Companion.UNAUTHORIZED
                TdApi.AuthorizationStateReady.CONSTRUCTOR -> return Status.Companion.READY
                else -> {
                    assertAuthorizationState_ba756b5f()
                    throw unsupported(state)
                }
            }
        }

        fun <T : TdApi.Object?> send(client: Client, function: TdApi.Function<T?>?, handler: ResultHandler<T?>) {
            send<T?>(client, function, ResultHandler.Companion.toTdlibHandler<T?>(handler))
        }

        private fun <T : TdApi.Object?> send(client: Client, function: TdApi.Function<T?>?, handler: Client.ResultHandler?) {
            client.send(function, handler)
        }

        private fun defaultMessageProperties(): MessageProperties {
            return MessageProperties() // Default to false.
        }

        const val TGX_CREATOR_USER_ID: Long = 163957826
        const val TDLIB_CREATOR_USER_ID: Long = 7736885

        val ADMIN_CHAT_ID: Long = fromSupergroupId(1112283549) // TGX Alpha and Admins
        val TRANSLATORS_CHAT_ID: Long = fromSupergroupId(1126790716)
        val TESTER_CHAT_ID: Long = fromSupergroupId(1336679475) // Telegram X Android: t.me/tgandroidtests
        val READER_CHAT_ID: Long = fromSupergroupId(1136101327) // Telegram X: t.me/tgx_android
        val CLOUD_RESOURCES_CHAT_ID: Long = fromSupergroupId(1247387696) // Telegram X: Resources
        @JvmField
        val TRENDING_STICKERS_CHAT_ID: Long = fromSupergroupId(1140222267) // Trending Stickers: t.me/TrendingStickers

        fun timeZoneOffset(): Long {
            return TimeUnit.MILLISECONDS.toSeconds(
                (TimeZone.getDefault().getRawOffset() +
                        TimeZone.getDefault().getDSTSavings()).toLong()
            )
        }

        const val CLIENT_DATA_VERSION: Int = 0

        // UI handler
        private const val MSG_ACTION_UPDATE_CHAT_ACTION = 1
        private const val MSG_ACTION_UPDATE_CALL = 2
        private const val MSG_ACTION_DISPATCH_UNREAD_COUNTER = 3
        private const val MSG_ACTION_CALL_STATE = 5
        private const val MSG_ACTION_CALL_BARS = 6
        private const val MSG_ACTION_PAUSE = 7
        private const val MSG_ACTION_USER_STATUS = 8
        private const val MSG_ACTION_DISPATCH_TERMS_OF_SERVICE = 9
        private const val MSG_ACTION_UPDATE_LANG_PACK = 11
        private const val MSG_ACTION_MESSAGE_ACTION_PREFIX = 100000

        @JvmField
        var CHAT_MARKED_AS_UNREAD: Int = -1
        @JvmField
        var CHAT_FAILED: Int = -2
        @JvmField
        var CHAT_LOADING: Int = -3

        @JvmStatic
        fun updateChatPosition(chat: Chat, position: TdApi.ChatPosition): Boolean {
            return setChatPosition(chat, position) != 0
        }

        private fun removeChatPosition(chat: Chat, existingPosition: Int): Int {
            var flags = 0
            val position = chat.positions[existingPosition]
            if (position.order != 0L) {
                flags = flags or ChatChange.ORDER
            }
            chat.positions = if (chat.positions.size == 1) arrayOfNulls<TdApi.ChatPosition>(0) else chat.positions.removeElement<TdApi.ChatPosition?>(
                existingPosition,
                arrayOfNulls<TdApi.ChatPosition>(chat.positions.size - 1)
            )
            return flags
        }

        private fun addChatPosition(chat: Chat, position: TdApi.ChatPosition): Int {
            if (position.order == 0L) return 0
            if (chat.positions != null && chat.positions.size > 0) {
                val positions = arrayOfNulls<TdApi.ChatPosition>(chat.positions.size + 1)
                System.arraycopy(chat.positions, 0, positions, 0, chat.positions.size)
                positions[chat.positions.size] = position
                chat.positions = positions
            } else {
                chat.positions = arrayOf<TdApi.ChatPosition>(position)
            }
            return ChatChange.ORDER or ChatChange.SOURCE
        }

        private fun updateOrRemoveChatPosition(chat: Chat, existingPosition: Int, position: TdApi.ChatPosition): Int {
            var flags = 0
            val prevPosition = chat.positions[existingPosition]
            if (prevPosition.order != position.order) flags = flags or ChatChange.ORDER
            if (position.order == 0L) {
                chat.positions = chat.positions.removeElement<TdApi.ChatPosition?>(existingPosition, arrayOfNulls<TdApi.ChatPosition>(chat.positions.size - 1))
            } else {
                if (!prevPosition.source.equalsTo(position.source)) {
                    flags = flags or ChatChange.SOURCE
                }
                if (prevPosition.isPinned != position.isPinned) {
                    flags = flags or ChatChange.PIN_STATE
                }
                if (flags == 0) {
                    return 0
                }
                chat.positions[existingPosition] = position
            }
            return flags
        }

        private fun setChatPosition(chat: Chat, position: TdApi.ChatPosition): Int {
            val existingPosition = indexOf(chat.positions, position.list)
            if (existingPosition != -1) {
                return updateOrRemoveChatPosition(chat, existingPosition, position)
            } else {
                return addChatPosition(chat, position)
            }
        }

        private const val CONNECTION_TIMEOUT_PROXY = 5.0
        private const val CONNECTION_TIMEOUT_DIRECT = 5.0
        private const val CONNECTION_TIMEOUT_DIRECT_CENSORED = 9.0

        const val RESTRICTION_STATUS_EVERYONE: Int = 0
        const val RESTRICTION_STATUS_RESTRICTED: Int = 1
        const val RESTRICTION_STATUS_BANNED: Int = 2
        const val RESTRICTION_STATUS_UNAVAILABLE: Int = 3

        @JvmStatic
        fun isSettingSuggestion(action: SuggestedAction): Boolean {
            when (action.getConstructor()) {
                TdApi.SuggestedActionCheckPhoneNumber.CONSTRUCTOR, TdApi.SuggestedActionCheckPassword.CONSTRUCTOR, TdApi.SuggestedActionSetBirthdate.CONSTRUCTOR, TdApi.SuggestedActionSetLoginEmailAddress.CONSTRUCTOR -> {
                    return true
                }

                else -> {
                    assertSuggestedAction_a78df4c9()
                }
            }
            return false
        }

        fun <T : TdApi.Object?> executeOrNull(query: TdApi.Function<T?>?): T? {
            try {
                return Client.execute<T?>(query)
            } catch (e: Client.ExecutionException) {
                return null
            }
        }
    }
}
