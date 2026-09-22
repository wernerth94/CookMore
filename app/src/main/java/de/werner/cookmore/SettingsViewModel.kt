package de.werner.cookmore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch



class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {


    val font_size = repository.font_size
        .stateIn(viewModelScope, SharingStarted.Lazily, 14)

    val banner_size = repository.banner_size
        .stateIn(viewModelScope, SharingStarted.Lazily, Constants.BANNER_SIZE_OPTIONS.first())

    val recipe_sorting = repository.recipe_sorting
        .stateIn(viewModelScope, SharingStarted.Lazily, Constants.RECIPE_SORTING_OPTIONS.first())

    val recipe_language = repository.recipe_language
        .stateIn(viewModelScope, SharingStarted.Lazily, Constants.GEMINI_LANGUAGES.first())

    val app_language = repository.app_language
        .stateIn(viewModelScope, SharingStarted.Lazily, Constants.APP_LANGUAGES.first())


    fun set_font_size(size: Int) {
        viewModelScope.launch {
            repository.set_font_size(size)
        }
    }

    fun set_banner_size(mode: String) {
        viewModelScope.launch {
            repository.set_banner_size(mode)
        }
    }

    fun set_recipe_sorting(mode: String) {
        viewModelScope.launch {
            repository.set_recipe_sorting(mode)
        }
    }

    fun set_recipe_language(lang: String) {
        viewModelScope.launch {
            repository.set_recipe_language(lang)
        }
    }

    fun set_app_language(lang: String) {
        viewModelScope.launch {
            repository.set_app_language(lang)
        }
    }
}