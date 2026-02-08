package com.kelly.app

import com.kelly.app.domain.SettingsStore
import java.io.File
import java.util.Properties

class DesktopSettingsStore : SettingsStore {
    private val file = File(System.getProperty("user.home"), ".kelly-connect.properties")
    private val props = Properties()

    init {
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }
    }

    private fun save() {
        file.outputStream().use { props.store(it, "Kelly Connect Settings") }
    }

    override fun putString(key: String, value: String) {
        props.setProperty(key, value); save()
    }

    override fun getString(key: String, default: String): String =
        props.getProperty(key, default)

    override fun putInt(key: String, value: Int) {
        props.setProperty(key, value.toString()); save()
    }

    override fun getInt(key: String, default: Int): Int =
        props.getProperty(key)?.toIntOrNull() ?: default

    override fun putFloat(key: String, value: Float) {
        props.setProperty(key, value.toString()); save()
    }

    override fun getFloat(key: String, default: Float): Float =
        props.getProperty(key)?.toFloatOrNull() ?: default
}
