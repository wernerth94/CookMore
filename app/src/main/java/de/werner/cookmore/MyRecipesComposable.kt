package de.werner.cookmore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

enum class MyRecipesState {
    Normal, Edit, Search
}


@Composable
fun MyRecipes(modifier: Modifier = Modifier,
              navigation_callback: (dest: AppDestinations) -> Unit,
              set_current_recipe: (recipe: Recipe?) -> Unit,
              force_user_interaction: (type: NavigationUIState, callback: ()->Unit) -> Unit
) {
    val context = LocalContext.current
    val parent_activity = context.findActivity<MainActivity>()

    if (parent_activity != null) {
        var three_dot_menu_expanded by remember { mutableStateOf(false) }
        var state by remember { mutableStateOf(MyRecipesState.Normal) }
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        val data_store = remember { parent_activity.recipe_repository }
        val search_query by data_store.search_query.collectAsState(initial = "")
        val recipes by data_store.recipes_flow.collectAsState(initial = emptyList())

        LaunchedEffect(state) {
            if (state == MyRecipesState.Search) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        Column(
            modifier = modifier
                .padding(vertical = 16.dp, horizontal = 0.dp)
        ) {
            // #################################
            // Header
            // #################################
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(65.dp)
                    .padding(bottom = 4.dp)
                    .background(MaterialTheme.colorScheme.surface)
                ,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedVisibility(
                    visible = state != MyRecipesState.Search,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Text(
                        text = "My Recipes",
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                AnimatedVisibility(
                    visible = state == MyRecipesState.Search,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    TextField(
                        value = search_query,
                        onValueChange = {
                            data_store.apply_search(it)
                        },
                        placeholder = { Text("Search") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                            }
                        ),
                        trailingIcon = {}
                    )
                }
                Row(modifier = Modifier.padding(0.dp)) {
                    if (state == MyRecipesState.Edit) {
                        force_user_interaction(
                            NavigationUIState.SAVE,
                            {
                                state = MyRecipesState.Normal
                                // todo: save all recipes
                            }
                        )
                    } else if (state == MyRecipesState.Search) {
                        IconButton(onClick = {
                            state = MyRecipesState.Normal
                            data_store.apply_search("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search"
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            state = MyRecipesState.Search
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = {
                            state = MyRecipesState.Edit
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit"
                            )
                        }
                        IconButton(onClick = { three_dot_menu_expanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = three_dot_menu_expanded,
                            onDismissRequest = { three_dot_menu_expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Create Backup") },
                                onClick = {
                                    parent_activity.lifecycleScope.launch {
                                        parent_activity.recipe_repository.export_recipes_to_file(
                                            parent_activity
                                        )
                                    }
                                    three_dot_menu_expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Backup") },
                                onClick = {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/zip"
                                    }
                                    parent_activity.select_backup_file_launcher.launch(intent)
                                    three_dot_menu_expanded = false
                                }
                            )
                        }
                    }
                }
            }
//            HorizontalDivider()
            // #################################
            // Content
            // #################################
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                LazyColumn() {
                    items(recipes) { recipe ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            if (RecipeUtil.still_exists(recipe)) {
                                val icon = RecipeUtil.get_icon(recipe)
                                if (icon != null) {
                                    Image(
                                        icon.asImageBitmap(), "Recipe Icon",
                                        Modifier
                                            .size(50.dp)
                                            .padding(
                                                start = 0.dp,
                                                top = 0.dp,
                                                end = 8.dp,
                                                bottom = 0.dp
                                            )
                                            .clickable {
                                                set_current_recipe(recipe)
                                                navigation_callback(AppDestinations.CURRENT_RECIPE)
                                            },
                                    )
                                }
                                if (state == MyRecipesState.Edit) {
                                    var title by remember { mutableStateOf(recipe.title) }
                                    TextField(
                                        value = title,
                                        onValueChange = {
                                            title = it
                                        }, // update state on each keystroke
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        enabled = recipe.title != title,
                                        onClick = {
                                            parent_activity.recipe_repository.update_recipe(
                                                recipe,
                                                new_title = title
                                            )
                                            recipe.title = title
                                            title = ""
                                            title = recipe.title // reload button state
                                        }) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Update Title"
                                        )
                                    }
                                    IconButton(onClick = {
                                        if (parent_activity.current_recipe == recipe) {
                                            set_current_recipe(null)
                                        }
                                        parent_activity.recipe_repository.delete_recipe(
                                            parent_activity,
                                            recipe
                                        )
                                        title = ""
                                        title = recipe.title // update row in list
                                        navigation_callback(AppDestinations.CURRENT_RECIPE)
                                        navigation_callback(AppDestinations.MY_RECIPES) // update navigation bar
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete"
                                        )
                                    }
                                } else {
                                    Text(
                                        recipe.title,
                                        modifier = Modifier
                                            .height(50.dp)  // Must have height first
                                            .wrapContentHeight(Alignment.CenterVertically)
                                            .fillMaxWidth()
                                            .clickable {
                                                set_current_recipe(recipe)
                                                navigation_callback(AppDestinations.CURRENT_RECIPE)
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}