package com.usbcam

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import java.util.Locale

/**
 * 进入全屏时抢音频焦点（让音乐/电台暂停）并播报“请小心驾驶”；
 * 退到后台时释放焦点，其他播放器会自动续播。
 */
object DrivingAudio {

    private const val TAG = "DrivingAudio"
    private const val UTTERANCE_ID = "usbcam_drive"

    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingText: String? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { }

    private val mainHandler = Handler(Looper.getMainLooper())

    /** 进入前音乐是否在播放：决定退出时要不要替用户补一个“播放”媒体键。 */
    private var pausedMusic = false

    fun enter(context: Context, speak: Boolean) {
        val app = context.applicationContext
        requestFocus(app)
        if (!speak) return

        // 优先播放打包进 APK 的内置语音；没有内置资源时退回系统 TTS。
        if (DrivePlayer.play(app)) return
        speak(app, "请小心驾驶")
    }

    fun exit(context: Context) {
        DrivePlayer.release()
        val shouldResume = hasFocus && pausedMusic
        abandonFocus()
        pausedMusic = false

        if (shouldResume) {
            // 有的车机播放器被抢焦点后不会自己续播，这里延迟补一个媒体“播放”键。
            mainHandler.postDelayed({ sendMediaPlay(context.applicationContext) }, 500)
        }
    }

    // ------------------------------------------------------------------
    // 音频焦点
    // ------------------------------------------------------------------

    private fun requestFocus(context: Context) {
        if (hasFocus) return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = am
        pausedMusic = try {
            am.isMusicActive
        } catch (e: Exception) {
            false
        }

        val result: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            focusRequest = request
            result = am.requestAudioFocus(request)
        } else {
            result = requestFocusLegacy(am)
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @Suppress("DEPRECATION")
    private fun requestFocusLegacy(am: AudioManager): Int =
        am.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (!hasFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = focusRequest
            if (request != null) {
                am.abandonAudioFocusRequest(request)
            }
        } else {
            abandonFocusLegacy(am)
        }
        hasFocus = false
    }

    @Suppress("DEPRECATION")
    private fun abandonFocusLegacy(am: AudioManager) {
        am.abandonAudioFocus(focusListener)
    }

    /** 通知系统媒体播放：部分播放器被抢焦点后不会自动续播。 */
    @Suppress("DEPRECATION")
    private fun sendMediaPlay(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
        } catch (e: Exception) {
            Log.e(TAG, "resume media failed", e)
        }
    }

    // ------------------------------------------------------------------
    // 语音播报
    // ------------------------------------------------------------------

    private fun speak(context: Context, text: String) {
        val engine = tts
        if (engine == null) {
            pendingText = text
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    try {
                        tts?.setLanguage(Locale.CHINA)
                    } catch (e: Exception) {
                        Log.e(TAG, "setLanguage failed", e)
                    }
                    val pending = pendingText
                    if (pending != null) {
                        pendingText = null
                        doSpeak(pending)
                    }
                } else {
                    Log.e(TAG, "tts init failed: $status")
                }
            }
            return
        }

        if (ttsReady) {
            doSpeak(text)
        } else {
            pendingText = text
        }
    }

    private fun doSpeak(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        } catch (e: Exception) {
            Log.e(TAG, "speak failed", e)
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "release failed", e)
        }
        tts = null
        ttsReady = false
    }
}
