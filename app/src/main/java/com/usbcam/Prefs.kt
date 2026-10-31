package com.usbcam

import android.content.Context
import android.view.KeyEvent

/**
 * 轻量偏好存储（避免引入 DataStore / 额外依赖）。
 */
object Prefs {

    private const val FILE = "usbcam_prefs"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---------- 快捷键 ----------
    fun isHotkeyEnabled(c: Context): Boolean = sp(c).getBoolean("hotkey_enabled", false)

    fun setHotkeyEnabled(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("hotkey_enabled", on).apply()
    }

    /** 启动键（拉起全屏）。旧版本的 hotkey_code 直接当作启动键，做一次兼容读取。 */
    fun hotkeyOpenCode(c: Context): Int {
        val p = sp(c)
        return p.getInt("hotkey_open_code", p.getInt("hotkey_code", KeyEvent.KEYCODE_F8))
    }

    fun hotkeyOpenMods(c: Context): Int {
        val p = sp(c)
        return p.getInt("hotkey_open_mods", p.getInt("hotkey_mods", 0))
    }

    fun setHotkeyOpen(c: Context, code: Int, mods: Int) {
        sp(c).edit()
            .putInt("hotkey_open_code", code)
            .putInt("hotkey_open_mods", mods)
            .apply()
    }

    /** 退出键（退到后台）。0 表示未绑定。 */
    fun hotkeyCloseCode(c: Context): Int = sp(c).getInt("hotkey_close_code", 0)

    fun hotkeyCloseMods(c: Context): Int = sp(c).getInt("hotkey_close_mods", 0)

    fun setHotkeyClose(c: Context, code: Int, mods: Int) {
        sp(c).edit()
            .putInt("hotkey_close_code", code)
            .putInt("hotkey_close_mods", mods)
            .apply()
    }

    // ---------- 语音提示 ----------
    fun isVoiceEnabled(c: Context): Boolean = sp(c).getBoolean("voice_enabled", true)

    fun setVoiceEnabled(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("voice_enabled", on).apply()
    }

    // ---------- 画面 ----------
    /** 是否把画面拉伸铺满全屏（不保持原始宽高比）。 */
    fun isFillScreen(c: Context): Boolean = sp(c).getBoolean("fill_screen", false)

    fun setFillScreen(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("fill_screen", on).apply()
    }

    // ---------- 开机自启 ----------
    fun isAutostart(c: Context): Boolean = sp(c).getBoolean("autostart", true)

    fun setAutostart(c: Context, on: Boolean) {
        sp(c).edit().putBoolean("autostart", on).apply()
    }

    // ---------- 悬浮窗位置 / 大小 ----------
    fun floatX(c: Context): Int = sp(c).getInt("float_x", -1)
    fun floatY(c: Context): Int = sp(c).getInt("float_y", 120)
    fun floatW(c: Context): Int = sp(c).getInt("float_w", 0)

    fun saveFloat(c: Context, x: Int, y: Int, w: Int) {
        sp(c).edit().putInt("float_x", x).putInt("float_y", y).putInt("float_w", w).apply()
    }
}
