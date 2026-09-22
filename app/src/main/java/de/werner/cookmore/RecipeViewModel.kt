package de.werner.cookmore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {

    val recipes_flow = repository.recipes_flow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val search_query = repository.search_query.stateIn(viewModelScope, SharingStarted.Lazily, "")


    fun update_recipe(recipe: Recipe, new_title:String="", new_html:String="") {
        viewModelScope.launch {
            repository.update_recipe(recipe, new_title, new_html)
        }
    }

    fun apply_search(query:String) {
        viewModelScope.launch {
            repository.apply_search(query)
        }
    }

    fun delete_recipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.delete_recipe(recipe)
        }
    }

    fun try_finalize_new_recipe(
        url: String? = null, content: String? = null,
        image_url: String? = null,
        no_image_needed: Boolean? = null,
        success_callback: (recipe: Recipe) -> Unit) {

        viewModelScope.launch {
            repository.try_finalize_new_recipe(
                url,
                content,
                image_url,
                no_image_needed,
                success_callback
            )
        }
    }

    fun import_recipes_from_file(context: MainActivity, uri: Uri) {
        viewModelScope.launch {
            repository.import_recipes_from_file(context, uri)
        }
    }

    fun export_recipes_to_file(context: MainActivity) {
        viewModelScope.launch {
            repository.export_recipes_to_file(context)
        }
    }
}