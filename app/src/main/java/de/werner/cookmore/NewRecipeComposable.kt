package de.werner.cookmore

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

enum class NewRecipeState {
    IDLE, LOADING, SELECTING_IMAGE, WAITING_TO_FINISH
}

val LOADING_TIMEOUT = 2000L

@Composable
fun NewRecipe(
    modifier: Modifier,
    get_app_state: () -> AppState,
    change_app_state: (s: AppState) -> Unit,
    new_recipe_callback: (recipe: Recipe?) -> Unit,
    autofill_url: String = "",
    force_user_interaction: (type: NavigationUIState) -> Unit,
    cancel_forced_user_interaction: () -> Unit,
    forced_interaction_result: ForcedInteractionResult
) {

    val context = LocalContext.current
    val parent_activity = context.findActivity<MainActivity>()
    if (parent_activity != null) {
        val font_size = parent_activity.settings_view_model.font_size.collectAsState()

        val keyboardController = LocalSoftwareKeyboardController.current
        var state by rememberSaveable  { mutableStateOf(NewRecipeState.IDLE) }
        var url by rememberSaveable  { mutableStateOf(autofill_url) }
        val image_candidates = rememberSaveable  { mutableStateListOf<String>() }

        BackHandler(state != NewRecipeState.IDLE) {
            state = NewRecipeState.IDLE
            cancel_forced_user_interaction()
        }

        if (forced_interaction_result == ForcedInteractionResult.CANCEL) {
            state = NewRecipeState.IDLE
            change_app_state(AppState.IDLE)
        }

        fun start_creation() {
            keyboardController?.hide()
            if (!Util.sanity_check_url(url)) {
                parent_activity.lifecycleScope.launch {
                    Toast.makeText(
                        parent_activity,
                        parent_activity.getString(R.string.malformed_url_error),
                        Toast.LENGTH_SHORT).show()
                }
                return
            }
            force_user_interaction(NavigationUIState.CANCEL)
            state = NewRecipeState.LOADING
            change_app_state(AppState.CREATING_RECIPE)
            image_candidates.clear()
            Util.download_webpage_and_process(
                url, parent_activity,
                get_app_state, image_candidates,
                success_callback = { recipe ->
                    state = NewRecipeState.IDLE
                    change_app_state(AppState.IDLE)
                    new_recipe_callback(recipe)
                },
                failure_callback = {
                    state = NewRecipeState.IDLE
                    change_app_state(AppState.IDLE)
                    new_recipe_callback(null)
                })
            parent_activity.lifecycleScope.launch {
                Util.call_back_after(
                    LOADING_TIMEOUT,
                    callback = {
                        if (state == NewRecipeState.LOADING) {
                            state = NewRecipeState.SELECTING_IMAGE
                            url = ""
                        }
                    })
            }
        }

        Column(
            modifier = modifier
                .padding(vertical = 16.dp, horizontal = 0.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .background(MaterialTheme.colorScheme.surface),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = {
                        url = Util.get_clipboard_text(parent_activity).toString()
                    },
                    enabled = state == NewRecipeState.IDLE
                ) {
                    Icon(
                        painter = painterResource(R.drawable.content_paste),
                        contentDescription = "Paste"
                    )
                }
                TextField(
                    value = url,                // current text
                    onValueChange = { url = it }, // update state on each keystroke
                    label = { Text(stringResource(R.string.paste_url), fontSize = font_size.value.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    enabled = state == NewRecipeState.IDLE
                )
                IconButton(
                    onClick = ::start_creation,
                    enabled = state == NewRecipeState.IDLE
                ) {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = "Scan"
                    )
                }
            }
            when (state) {
                NewRecipeState.LOADING -> ShowLoadingIcon()
                NewRecipeState.SELECTING_IMAGE -> ImageSelection(
                    image_candidates,
                    parent_activity,
                    recipe_success_callback = new_recipe_callback,
                    img_selected_callback = {
                        state = NewRecipeState.WAITING_TO_FINISH
                    })
                NewRecipeState.WAITING_TO_FINISH -> ShowLoadingIcon()
                else -> {}
            }
        }

        if (state == NewRecipeState.IDLE && autofill_url != "") {
            start_creation()
        }
    }
}

@Composable
fun ImageSelection(
    image_candidates: List<String>,
    parent_activity: MainActivity,
    recipe_success_callback: (recipe: Recipe) -> Unit,
    img_selected_callback: () -> Unit) {

    val font_size = parent_activity.settings_view_model.font_size.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                parent_activity.recipe_view_model.try_finalize_new_recipe(
                    no_image_needed = true,
                    success_callback = recipe_success_callback)
            }
        ) {
            Text(stringResource(R.string.use_no_image), fontSize = font_size.value.sp)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(image_candidates) { image_candidate ->
            AsyncImage(
                model = image_candidate,
                contentDescription = image_candidate,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 3.dp)
                    .fillMaxWidth()
                    .clickable {
                        parent_activity.lifecycleScope.launch {
                            Util.download_bitmap_and_call_back(
                                image_candidate,
                                callback = { image ->
                                    if (image == null) {
                                        Toast.makeText(
                                            parent_activity,
                                            parent_activity.getString(R.string.image_download_error),
                                            Toast.LENGTH_LONG).show()
                                    }
                                    else {
                                        parent_activity.recipe_view_model.try_finalize_new_recipe(
                                            image_url = image_candidate,
                                            success_callback=recipe_success_callback)
                                    }
                                })
                        }
                        img_selected_callback()
                }
            )
        }
    }
}

@Composable
fun ShowLoadingIcon() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            Modifier.size(80.dp),
            strokeWidth = 6.dp
            )
    }
}