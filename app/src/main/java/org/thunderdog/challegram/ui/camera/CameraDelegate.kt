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
 */
package org.thunderdog.challegram.ui.camera

import android.graphics.RectF
import org.thunderdog.challegram.loader.ImageGalleryFile

interface CameraDelegate {
  fun displayFatalErrorMessage (msg: String?)

  fun displayHint (hint: String)

  fun resolveExpectedError (@CameraError.Code code: Int)

  fun onAvailableCamerasCountChanged (availableCameraCount: Int)

  fun onFlashModeChanged (flashMode: Int)

  fun onZoomChanged (zoom: Float)

  fun onCameraSourceChange (completed: Boolean, isForward: Boolean, toFrontFace: Boolean)

  fun onPerformSuccessHint (isVideo: Boolean)

  fun onRenderedFirstFrame ()

  fun onResetRenderState (needBlurPreview: Boolean, after: Runnable?)

  fun onDisplayRotationChanged ()

  fun onUiBlocked (isBlocked: Boolean)

  fun onMediaTaken (file: ImageGalleryFile)

  fun usePrivateFolder (): Boolean

  fun useQrScanner (): Boolean

  fun onVideoCaptureStarted (startTimeMs: Long)

  fun onQrCodeFound (qrCodeData: String?, boundingBox: RectF?, height: Int, width: Int, rotation: Int, isLegacyZxing: Boolean)

  fun getCurrentCameraOrientation (): Int

  fun getCurrentCameraSensorOrientation (): Int

  fun onQrCodeNotFound ()

  fun onVideoCaptureEnded ()
}
