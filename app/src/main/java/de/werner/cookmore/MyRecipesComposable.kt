package de.werner.cookmore

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class MyRecipesState {
    Normal, Edit, Search
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyRecipes(modifier: Modifier = Modifier,
              navigation_callback: (dest: AppDestinations) -> Unit,
              set_current_recipe: (recipe: Recipe?) -> Unit,
              force_user_interaction: (type: NavigationUIState) -> Unit,
              cancel_forced_user_interaction: () -> Unit,
              forced_interaction_result: ForcedInteractionResult
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val parent_activity = context.findActivity<MainActivity>()

    if (parent_activity != null) {
        val font_size = parent_activity.settings_view_model.font_size.collectAsState()
        val recipe_sorting = parent_activity.settings_view_model.recipe_sorting.collectAsState()

        var three_dot_menu_expanded by remember { mutableStateOf(false) }
        var state by rememberSaveable { mutableStateOf(MyRecipesState.Normal) }
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        val search_query by parent_activity.recipe_view_model.search_query.collectAsState()
        val updated_titles by rememberSaveable { mutableStateOf(HashMap<Int, String>()) }
        val recipes by parent_activity.recipe_view_model.recipes_flow
            .map{ recipes ->
                when (recipe_sorting.value) {
                    "Date" -> recipes.sortedBy { it.id }
                    else -> recipes.sortedBy { it.title }
                }
            }
            .collectAsState(initial = emptyList())

        BackHandler(state == MyRecipesState.Edit) {
            updated_titles.clear()
            state = MyRecipesState.Normal
            cancel_forced_user_interaction()
        }

        LaunchedEffect(state) {
            if (state == MyRecipesState.Search) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        if (forced_interaction_result == ForcedInteractionResult.SAVE) {
            recipes.forEach {
                if(it.id in updated_titles.keys) {
                    parent_activity.recipe_view_model.update_recipe(
                        it,
                        new_title = updated_titles.getOrDefault(it.id, it.title)
                    )
                    // update UI to reflect the change
                    it.title = updated_titles.getOrDefault(it.id, it.title)
                }
            }
            state = MyRecipesState.Normal
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = state != MyRecipesState.Search,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Text(
                        text = stringResource(R.string.my_recipes),
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.headlineLarge,
//                        fontSize = font_size.value.sp
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
                            parent_activity.recipe_view_model.apply_search(it)
                        },
                        placeholder = { Text(stringResource(R.string.search), fontSize = font_size.value.sp) },
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
                            NavigationUIState.SAVE
                        )
                    } else if (state == MyRecipesState.Search) {
                        IconButton(onClick = {
                            state = MyRecipesState.Normal
                            parent_activity.recipe_view_model.apply_search("")
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
                                text = { Text(stringResource(R.string.create_backup), fontSize = font_size.value.sp) },
                                onClick = {
                                    parent_activity.lifecycleScope.launch {
                                        parent_activity.recipe_view_model.export_recipes_to_file(
                                            parent_activity
                                        )
                                    }
                                    three_dot_menu_expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_backup), fontSize = font_size.value.sp) },
                                onClick = {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/zip"
                                    }
                                    parent_activity.select_backup_file_launcher.launch(intent)
                                    three_dot_menu_expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings), fontSize = font_size.value.sp) },
                                onClick = {
                                    three_dot_menu_expanded = false
                                    navigation_callback(AppDestinations.SETTINGS)
                                }
                            )
                        }
                    }
                }
            }
            // #################################
            // Recipe List
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
                                    var title by remember { mutableStateOf(updated_titles.getOrDefault(recipe.id, recipe.title)) }
                                    TextField(
                                        value = title,
                                        onValueChange = {
                                            title = it
                                            updated_titles[recipe.id] = it
                                        }, // update state on each keystroke
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        if (parent_activity.app.current_recipe == recipe) {
                                            set_current_recipe(null)
                                        }
                                        parent_activity.recipe_view_model.delete_recipe(recipe)
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
                                }
                                else {
                                    Text(
                                        recipe.title,
                                        fontSize = font_size.value.sp,
                                        modifier = Modifier
                                            .height(50.dp)  // Must have height first
                                            .wrapContentHeight(Alignment.CenterVertically)
                                            .fillMaxWidth()
                                            .combinedClickable (
                                                onClick = {
                                                    set_current_recipe(recipe)
                                                    navigation_callback(AppDestinations.CURRENT_RECIPE)
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    state = MyRecipesState.Edit
                                                    updated_titles.clear()
                                                }
                                            )
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