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
 * File created on 29/06/2019
 */
package org.thunderdog.challegram.telegram

import androidx.annotation.IntDef
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention

@Retention(AnnotationRetention.SOURCE)
@IntDef(
  RightId.READ_MESSAGES,
  RightId.SEND_BASIC_MESSAGES,
  RightId.SEND_AUDIO,
  RightId.SEND_DOCS,
  RightId.SEND_PHOTOS,
  RightId.SEND_VIDEOS,
  RightId.SEND_VOICE_NOTES,
  RightId.SEND_VIDEO_NOTES,
  RightId.SEND_OTHER_MESSAGES,
  RightId.SEND_POLLS_OR_CHECKLISTS,
  RightId.EMBED_LINKS,
  RightId.REACT_TO_MESSAGES,
  RightId.CHANGE_CHAT_INFO,
  RightId.EDIT_MESSAGES,
  RightId.DELETE_MESSAGES,
  RightId.BAN_USERS,
  RightId.INVITE_USERS,
  RightId.PIN_MESSAGES,
  RightId.MANAGE_VIDEO_CHATS,
  RightId.POST_STORIES,
  RightId.EDIT_STORIES,
  RightId.DELETE_STORIES,
  RightId.MANAGE_OR_CREATE_TOPICS,
  RightId.MANAGE_DIRECT_MESSAGES,
  RightId.EDIT_OR_MANAGE_TAGS,
  RightId.ADD_NEW_ADMINS,
  RightId.REMAIN_ANONYMOUS
)
annotation class RightId {
  companion object {
    const val READ_MESSAGES = 1
    const val SEND_BASIC_MESSAGES = 2
    const val SEND_AUDIO = 3
    const val SEND_DOCS = 4
    const val SEND_PHOTOS = 5
    const val SEND_VIDEOS = 6
    const val SEND_VOICE_NOTES = 7
    const val SEND_VIDEO_NOTES = 8
    const val SEND_OTHER_MESSAGES = 9
    const val SEND_POLLS_OR_CHECKLISTS = 10
    const val EMBED_LINKS = 11
    const val CHANGE_CHAT_INFO = 12
    const val EDIT_MESSAGES = 13
    const val DELETE_MESSAGES = 14
    const val BAN_USERS = 15
    const val INVITE_USERS = 16
    const val PIN_MESSAGES = 17
    const val MANAGE_VIDEO_CHATS = 18
    const val POST_STORIES = 19
    const val EDIT_STORIES = 20
    const val DELETE_STORIES = 21
    const val MANAGE_OR_CREATE_TOPICS = 22
    const val MANAGE_DIRECT_MESSAGES = 23
    const val EDIT_OR_MANAGE_TAGS = 24
    const val REACT_TO_MESSAGES = 25
    const val ADD_NEW_ADMINS = 100
    const val REMAIN_ANONYMOUS = 101
  }
}
