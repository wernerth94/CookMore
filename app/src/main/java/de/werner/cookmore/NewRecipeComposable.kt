package de.werner.cookmore

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import java.io.File
import java.io.FileOutputStream

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
    autofill_url: String = ""
) {

    val context = LocalContext.current
    val parent_activity = context.findActivity<MainActivity>()
    if (parent_activity != null) {
        val keyboardController = LocalSoftwareKeyboardController.current
        var state by remember { mutableStateOf(NewRecipeState.IDLE) }
        var url by remember { mutableStateOf(autofill_url) }
        val image_candidates = remember { mutableStateListOf<String>() }

        fun start_creation() {
            keyboardController?.hide()
            if (!Util.sanity_check_url(url)) {
                parent_activity.lifecycleScope.launch {
                    Toast.makeText(parent_activity, "This does not look like an URL", Toast.LENGTH_SHORT).show()
                }
                return
            }
            state = NewRecipeState.LOADING
            change_app_state(AppState.CREATING_RECIPE)
            Util.download_webpage_and_process(url, parent_activity, get_app_state, image_candidates,
                success_callback = { recipe ->
                    state = NewRecipeState.IDLE
                    new_recipe_callback(recipe)
                },
                failure_callback = {
                    state = NewRecipeState.IDLE
                    new_recipe_callback(null)
                })
            parent_activity.lifecycleScope.launch {
                Util.call_back_after(
                    LOADING_TIMEOUT,
                    callback = {
                        state = NewRecipeState.SELECTING_IMAGE
                        url = ""
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
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Paste"
                    )
                }
                TextField(
                    value = url,                // current text
                    onValueChange = { url = it }, // update state on each keystroke
                    label = { Text("<<Paste, or type an URL") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    enabled = state == NewRecipeState.IDLE
                )
                IconButton(
                    onClick = ::start_creation,
                    enabled = state == NewRecipeState.IDLE
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Scan"
                    )
                }
            }
            when (state) {
                NewRecipeState.LOADING -> show_loading_icon()
                NewRecipeState.SELECTING_IMAGE -> image_selection(
                    image_candidates,
                    parent_activity,
                    recipe_success_callback = new_recipe_callback,
                    img_selected_callback = {
                        state = NewRecipeState.WAITING_TO_FINISH
                    })
                NewRecipeState.WAITING_TO_FINISH -> show_loading_icon()
                else -> {}
            }
        }

        if (state == NewRecipeState.IDLE && autofill_url != "") {
            start_creation()
        }
    }
}

@Composable
fun image_selection(
    image_candidates: List<String>,
    parent_activity: MainActivity,
    recipe_success_callback: (recipe: Recipe) -> Unit,
    img_selected_callback: () -> Unit) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                RecipeUtil.try_finalize_new_recipe(parent_activity,
                    no_image_needed = true,
                    success_callback = recipe_success_callback)
            }
        ) {
            Text("Use no image")
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
                                        Toast.makeText(parent_activity, "Error downloading the image",
                                            Toast.LENGTH_LONG).show()
                                    }
                                    else {
                                        RecipeUtil.try_finalize_new_recipe(parent_activity, image=image, image_url = image_candidate,
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
fun show_loading_icon() {
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