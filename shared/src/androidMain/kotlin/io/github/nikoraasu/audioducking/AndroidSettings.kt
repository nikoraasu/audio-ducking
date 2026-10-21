package io.github.nikoraasu.audioducking

import android.content.Context

private class SharedPrefs(context: Context) : Prefs {
    // Same file name as the original SettingsStore so existing settings survive the update.
    private val prefs = context.getSharedPreferences("audio_ducking_settings", Context.MODE_PRIVATE)

    override fun getLong(key: String, default: Long) = prefs.getLong(key, default)
    override fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    override fun getFloat(key: String, default: Float) = prefs.getFloat(key, default)
    override fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    override fun getString(key: String, default: String) = prefs.getString(key, default) ?: default
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
}

fun Settings(context: Context) = Settings(SharedPrefs(context))
