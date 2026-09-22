package de.werner.cookmore

import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class EditHTMLComponent(val content:String, val type: String, val start_idx: Int, val end_idx: Int)

@Composable
fun CurrentRecipe(
    modifier: Modifier = Modifier,
    current_recipe: Recipe?,
    regenerate_callback: (recipe: Recipe) -> Unit,
    force_user_interaction: (type: NavigationUIState) -> Unit,
    cancel_forced_user_interaction: () -> Unit,
    forced_interaction_result: ForcedInteractionResult
) {

    val configuration = LocalConfiguration.current
    val screen_width = configuration.screenWidthDp.dp
    val context = LocalContext.current
    val parent_activity = context.findActivity<MainActivity>()
    if (parent_activity != null) {

        val font_size = parent_activity.settings_view_model.font_size.collectAsState()
        val banner_size = parent_activity.settings_view_model.banner_size.collectAsState()

        val scroll_state = rememberScrollState()
        var three_dot_menu_expanded by remember { mutableStateOf(false) }

        var edit_mode by rememberSaveable { mutableStateOf(false) }
        val callback_list = rememberSaveable { mutableStateListOf<(String) -> String>() }
        val editables = rememberSaveable { mutableStateListOf<EditHTMLComponent>() }

        DisposableEffect(Unit) {
            parent_activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                parent_activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        BackHandler(edit_mode) {
            edit_mode = false
            callback_list.clear()
            editables.clear()
            cancel_forced_user_interaction()
        }

        if (current_recipe != null) {
            var html_page by remember { mutableStateOf(RecipeUtil.get_html_page(current_recipe)) }
            if (html_page != null) {

                if (forced_interaction_result == ForcedInteractionResult.SAVE) {
                    callback_list.reversed().forEach {
                        html_page = it(html_page!!)
                    }
                    callback_list.clear()
                    editables.clear()
                    parent_activity.recipe_view_model.update_recipe(current_recipe, new_html = html_page!!)
                    edit_mode = false
                }

                val banner = RecipeUtil.get_banner(current_recipe)

                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll_state)
                ) {
                    // Banner
                    // ######################################
                    when(banner_size.value) {
                        "None" -> {}
                        "Compact" -> {
                            if (banner != null) {
                                Image(Util.banner_crop(banner).asImageBitmap(), "Recipe Banner",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(0.dp),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                        else -> {
                            if (banner != null) {
                                Image(banner.asImageBitmap(), "Recipe Banner",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(0.dp),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    }
                    // Title and Options
                    // ######################################

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp, bottom = 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 0.9f * screen_width)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = current_recipe.title,
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                        // only show 3-dots when not editing
                        if (!edit_mode) {
                            Column(
                            ) {
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
                                        text = { Text(stringResource(R.string.edit_recipe)) },
                                        onClick = {
                                            callback_list.clear()
                                            editables.clear()
                                            editables.addAll( parse_html_for_editables(parent_activity, html_page!!) )
                                            edit_mode = true
                                            three_dot_menu_expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.copy_recipe_link)) },
                                        onClick = {
                                            Util.place_clibboard_text(
                                                parent_activity,
                                                current_recipe.url
                                            )
                                            parent_activity.lifecycleScope.launch {
                                                Toast.makeText(
                                                    parent_activity,
                                                    "Link copied",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            three_dot_menu_expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.re_generate)) },
                                        onClick = {
                                            three_dot_menu_expanded = false
                                            regenerate_callback(current_recipe)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // Recipe Content
                    // ######################################
                    if (!edit_mode) {

                        val regex = Regex("<h1>(.*?)</h1>", RegexOption.IGNORE_CASE)
                        if (regex.findAll(html_page!!).count() > 1) {
                            Toast.makeText(context, "Multiple titles found", Toast.LENGTH_LONG).show()
                        }
                        else {
                            val updated_html = regex.replace(html_page!!, "")
                            if (updated_html.length != html_page!!.length) {
                                parent_activity.recipe_view_model.update_recipe(current_recipe, new_html = updated_html)
                                html_page = RecipeUtil.get_html_page(current_recipe)
                            }
                        }

                        val html_with_css = RecipeUtil.inject_css_for_viewer(
                            html_page!!,
                            MaterialTheme.colorScheme.background,
                            font_size.value
                        )

                        LocalHtmlViewer(html_with_css, scroll_state, modifier)
                    }
                    else {
                        // Edit Mode
                        // ##########################################
                        val add_callbacks = callback_list.isEmpty()
                        Column() {
                            for (e in editables) {
                                EditComponent(e, callback_list, add_callbacks)
                            }
                        }
                        force_user_interaction(
                            NavigationUIState.SAVE
                        )
                    }
                }
                return
            }
        }
    }

    Text(stringResource(R.string.no_recipe_info_text), modifier=modifier)
}

@Composable
fun EditComponent(element: EditHTMLComponent, callback_list: MutableList<(String) -> String>, add_callbacks: Boolean) {
    var content by remember { mutableStateOf(element.content) }
    val font_size = when (element.type) {
        "<h1>" -> 24.sp
        "<h2>" -> 18.sp
        else -> 16.sp
    }
    val font_weight = when (element.type) {
        "<h1>" -> FontWeight.Bold
        "<h2>" -> FontWeight.Bold
        else -> FontWeight.Normal
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,

    ) {
        if (element.type == "<li>") {
            Text(
                "-",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        TextField(
            value = content,
            onValueChange = {
                content = it
            },
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.LightGray,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,   // remove the lower line
                unfocusedIndicatorColor = Color.Transparent, // of the TextField
            ),
            textStyle = TextStyle(
//                color = Color.DarkGray,
                fontSize = font_size,
                fontWeight = font_weight
            )
        )

        if (add_callbacks) {
            callback_list += { html_page ->
                html_page.replaceRange(element.start_idx, element.end_idx, content)
            }
        }
    }

}

fun parse_html_for_editables(parent_activity: MainActivity,
                             html_page: String): List<EditHTMLComponent> {
    val opening_list = mutableListOf("<p>", "<li>", "<h1>", "<h2>")
    val closing_list = mutableListOf("</p>", "</li>", "</h1>", "</h2>")
    var result_list: MutableList<EditHTMLComponent> = mutableListOf()
    var active_element = ""
    var active_element_idx = 0
    var start_idx = 0
    for (i in 0..<html_page.length) {
        if (active_element != "") {
            // close active element
            for ((tag_idx, close_tag) in closing_list.withIndex()) {
                val tag_length = close_tag.length
                if (html_page.substring(i, i+tag_length) == close_tag) {
                    if (tag_idx != active_element_idx) {
                        Log.d("Recipe Parsing", "Wrong closing tag found - Opening $active_element; closing $close_tag")
                        Log.d("Recipe Parsing", html_page.substring(start_idx - active_element.length, i + tag_length))
                        parent_activity.lifecycleScope.launch {
                            Toast.makeText(parent_activity, "HTML parsing failed", Toast.LENGTH_SHORT).show()
                        }
                        return emptyList()
                    }

                    result_list += EditHTMLComponent(
                        html_page.substring(start_idx, i), active_element,
                        start_idx, i
                    )
                    active_element = ""
                    active_element_idx = 0
                    start_idx = 0
                    break
                }
            }

        }
        else {
            // find new element
            for ((tag_idx, open_tag) in opening_list.withIndex()) {
                val tag_length = open_tag.length
                if (i+tag_length < html_page.length && html_page.substring(i, i+tag_length) == open_tag) {
                    active_element = open_tag
                    active_element_idx = tag_idx
                    start_idx = i + tag_length
                    break
                }
            }
        }
    }
    if (active_element != "") {
        Log.d("Recipe Parsing", "$active_element tag is still open")
        Log.d("Recipe Parsing", html_page.substring(start_idx - active_element.length, html_page.length))
        parent_activity.lifecycleScope.launch {
            Toast.makeText(parent_activity, "HTML parsing failed", Toast.LENGTH_SHORT).show()
        }
        return emptyList()
    }
    return result_list
}

@Composable
fun LocalHtmlViewer(html_content: String, scroll_state: ScrollState, modifier: Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Capture WebView scroll events and pass to parent
                val delta = available.y
                if (delta < 0) { // Scrolling down
                    coroutineScope.launch {
                        scroll_state.scrollBy(-delta) // Scroll parent UP
                    }
                    return Offset(0f, delta) // Consume event
                }
                return Offset.Zero
            }
        }
    }

    AndroidView(
        modifier = modifier.nestedScroll(nestedScrollConnection),
        factory = { context ->
            WebView(context).apply {
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
//                isScrollable = false

                settings.apply {
                    // Enable JavaScript (optional)
                    javaScriptEnabled = false
                    loadWithOverviewMode = true
//                    useWideViewPort = true
                    // Enable zoom (optional)
                    builtInZoomControls = false
                    displayZoomControls = false
                }

                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            // Load local HTML from assets
            webView.loadDataWithBaseURL(null, html_content, "text/html", "UTF-8", null)
        }
    )
}