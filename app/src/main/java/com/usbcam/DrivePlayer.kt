package com.usbcam

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * 播放打包在 res/raw 里的“请小心驾驶”提示音。
 *
 * 用运行时按名字查资源（getIdentifier），这样仓库里没有音频文件时也能正常编译，
 * 构建脚本生成了 drive_warning.wav 之后就会自动播放它。
 */
object DrivePlayer {

    private const val TAG = "DrivePlayer"
    private const val RES_NAME = "drive_warning"

    private var player: MediaPlayer? = null

    fun hasBuiltin(context: Context): Boolean =
        context.resources.getIdentifier(RES_NAME, "raw", context.packageName) != 0

    /** 播放内置提示音，成功返回 true；没有资源或播放失败返回 false（由调用方回退）。 */
    fun play(context: Context): Boolean {
        val resId = context.resources.getIdentifier(RES_NAME, "raw", context.packageName)
        if (resId == 0) {
            Log.d(TAG, "no builtin prompt resource")
            return false
        }

        return try {
            release()
            val mp = MediaPlayer.create(context, resId) ?: return false
            mp.setOnCompletionListener { p ->
                try {
                    p.release()
                } catch (e: Exception) {
                    Log.e(TAG, "release failed", e)
                }
                if (player === p) player = null
            }
            mp.setOnErrorListener { p, what, extra ->
                Log.e(TAG, "media error what=$what extra=$extra")
                try {
                    p.release()
                } catch (e: Exception) {
                    // ignore
                }
                if (player === p) player = null
                true
            }
            mp.start()
            player = mp
            true
        } catch (e: Exception) {
            Log.e(TAG, "play failed", e)
            false
        }
    }

    fun release() {
        val p = player ?: return
        player = null
        try {
            if (p.isPlaying) p.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            p.release()
        } catch (e: Exception) {
            // ignore
        }
    }
}
