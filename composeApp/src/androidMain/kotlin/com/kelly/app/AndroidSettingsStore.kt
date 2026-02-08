package com.kelly.app

import android.content.Context
import com.kelly.app.domain.SettingsStore

class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("kelly_settings", Context.MODE_PRIVATE)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getInt(key: String, default: Int): Int =
        prefs.getInt(key, default)

    override fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    override fun getFloat(key: String, default: Float): Float =
        prefs.getFloat(key, default)
}
