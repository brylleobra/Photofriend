package com.example.photofriend.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun getValuesFlow(cameraId: String): Flow<Map<String, String>> =
        dataStore.data.map { prefs ->
            buildMap {
                prefs.asMap().forEach { (key, value) ->
                    if (key.name.startsWith("$cameraId:") && value is String) {
                        put(key.name.removePrefix("$cameraId:"), value)
                    }
                }
            }
        }

    suspend fun getValuesSnapshot(cameraId: String): Map<String, String> =
        getValuesFlow(cameraId).first()

    suspend fun setValue(cameraId: String, settingId: String, value: String) {
        dataStore.edit { it[stringPreferencesKey("$cameraId:$settingId")] = value }
    }

    suspend fun resetCamera(cameraId: String) {
        dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { it.name.startsWith("$cameraId:") }
                .forEach { prefs.remove(it) }
        }
    }
}
