package com.niki.common.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.p1ay1s.base.appContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 摘自招新系统
 */
private const val PREFERENCE_NAME = "main_pref"

// 获取 DataStore 实例
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCE_NAME
)

/**
 * 全局都可获取的 DataStore 实例
 */
val dataStoreInstance: DataStore<Preferences> by lazy {
    appContext!!.dataStore
}

/**
 * 插入字符串型值元素到 DataStore 中
 */
suspend fun putStringData(
    preferencesKey: Preferences.Key<String>,
    value: String
) = dataStoreInstance.edit {
    it[preferencesKey] = value
}

/**
 * 插入布尔型值元素到 DataStore 中
 */
suspend fun putBooleanData(
    preferencesKey: Preferences.Key<Boolean>,
    value: Boolean
) = dataStoreInstance.edit {
    it[preferencesKey] = value
}

/**
 * 插入整型元素到 DataStore 中
 */
suspend fun putIntData(
    preferencesKey: Preferences.Key<Int>,
    value: Int
) = dataStoreInstance.edit {
    it[preferencesKey] = value
}

/**
 * 插入长整型元素到 DataStore 中
 */
suspend fun putLongData(
    preferencesKey: Preferences.Key<Long>,
    value: Long
) = dataStoreInstance.edit {
    it[preferencesKey] = value
}

/**
 * 获取 DataStore 对应的字符串值
 */
suspend fun getStringData(
    preferencesKey: Preferences.Key<String>,
    default: String = ""
): String = dataStoreInstance.data.map {
    it[preferencesKey] ?: default
}.first()

/**
 * 获取 DataStore 对应的布尔值
 */
suspend fun getBooleanData(
    preferencesKey: Preferences.Key<Boolean>,
    default: Boolean = false
): Boolean = dataStoreInstance.data.map {
    it[preferencesKey] ?: default
}.first()

/**
 * 获取 DataStore 对应的整型值
 */
suspend fun getIntData(
    preferencesKey: Preferences.Key<Int>,
    default: Int = 0
): Int = dataStoreInstance.data.map {
    it[preferencesKey] ?: default
}.first()

/**
 * 获取 DataStore 对应的长整形值
 */
suspend fun getLongData(
    preferencesKey: Preferences.Key<Long>,
    default: Long = 0L
): Long = dataStoreInstance.data.map {
    it[preferencesKey] ?: default
}.first()