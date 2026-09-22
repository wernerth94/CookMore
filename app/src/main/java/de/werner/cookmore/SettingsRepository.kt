package de.werner.cookmore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map



class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object  {
        val FONT_SIZE = intPreferencesKey("font_size")
        val BANNER_SIZE = stringPreferencesKey("banner_size")
        val RECIPE_SORTING = stringPreferencesKey("recipe_sorting")
        val RECIPE_LANGUAGE = stringPreferencesKey("recipe_language")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val font_size: Flow<Int> = dataStore.data
        .map { prefs ->
            prefs[FONT_SIZE] ?: 14
        }

    val banner_size: Flow<String> = dataStore.data
        .map { prefs ->
            prefs[BANNER_SIZE] ?: Constants.BANNER_SIZE_OPTIONS.first()
        }

    val recipe_sorting: Flow<String> = dataStore.data
        .map { prefs ->
            prefs[RECIPE_SORTING] ?: Constants.RECIPE_SORTING_OPTIONS.first()
        }
    val recipe_language: Flow<String> = dataStore.data
        .map { prefs ->
            prefs[RECIPE_LANGUAGE] ?: Constants.GEMINI_LANGUAGES.first()
        }
    val app_language: Flow<String> = dataStore.data
        .map { prefs ->
            prefs[APP_LANGUAGE] ?: Constants.APP_LANGUAGES.first()
        }


    suspend fun set_font_size(size: Int) {
        dataStore.edit { it[FONT_SIZE] = size }
    }
    suspend fun set_banner_size(mode: String) {
        dataStore.edit { it[BANNER_SIZE] = mode }
    }
    suspend fun set_recipe_sorting(mode: String) {
        dataStore.edit { it[RECIPE_SORTING] = mode }
    }
    suspend fun set_recipe_language(lang: String) {
        dataStore.edit { if (lang in Constants.GEMINI_LANGUAGES) it[RECIPE_LANGUAGE] = lang }
    }
    suspend fun set_app_language(lang: String) {
        dataStore.edit { if (lang in Constants.APP_LANGUAGES) it[APP_LANGUAGE] = lang }
    }
}

