package de.werner.cookmore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {
    val recipesFlow = repository.recipes_flow

    fun addRecipe(id: String) {
        viewModelScope.launch {
            //repository.addRecipe(id)
        }
    }
}