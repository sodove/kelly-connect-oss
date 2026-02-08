package com.kelly.app.domain

interface SettingsStore {
    fun putString(key: String, value: String)
    fun getString(key: String, default: String): String
    fun putInt(key: String, value: Int)
    fun getInt(key: String, default: Int): Int
    fun putFloat(key: String, value: Float)
    fun getFloat(key: String, default: Float): Float
}
