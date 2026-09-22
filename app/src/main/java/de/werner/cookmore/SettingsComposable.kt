package de.werner.cookmore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsComposable(
    modifier: Modifier,
    viewModel: SettingsViewModel
) {
    val fontSize by viewModel.font_size.collectAsStateWithLifecycle()
    val banner_size by viewModel.banner_size.collectAsStateWithLifecycle()
    val recipe_sorting by viewModel.recipe_sorting.collectAsStateWithLifecycle()
//    val app_language by viewModel.app_language.collectAsStateWithLifecycle()
    val recipe_language by viewModel.recipe_language.collectAsStateWithLifecycle()

    // Header
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(65.dp)
                .padding(bottom = 4.dp)
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings),
                modifier = Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.headlineLarge,
//                fontSize = fontSize.sp
            )
        }

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ########################################
            item {
                Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium, fontSize = fontSize.sp)
            }

            // Font size
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text(stringResource(R.string.font_size), fontSize = fontSize.sp)
                        Text(
                            stringResource(R.string.font_size_explainer),
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = (fontSize-4).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            fontSize = (fontSize-4).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (fontSize > 10) viewModel.set_font_size(fontSize - 1) },
                            enabled = fontSize > 10
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                        }

                        Text(
                            text = fontSize.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = fontSize.sp
                        )

                        IconButton(
                            onClick = { if (fontSize < 24) viewModel.set_font_size(fontSize + 1) },
                            enabled = fontSize < 24
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                        }
                    }
                }
            }

            // Banner Size
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var banner_size_selector by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text(stringResource(R.string.banner_size), fontSize = fontSize.sp)
                        Text(
                            stringResource(R.string.banner_size_explainer),
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = (fontSize-4).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            fontSize = (fontSize-4).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        banner_size,
                        fontSize = fontSize.sp,
                        modifier = Modifier
                            .clickable { banner_size_selector = true }
                    )

                    if (banner_size_selector) {
                        SelectorDialog(
                            stringResource(R.string.banner_size),
                            Constants.BANNER_SIZE_OPTIONS,
                            banner_size,
                            on_option_selected = { viewModel.set_banner_size(it) },
                            on_dismiss = { banner_size_selector = false })
                    }
                }
            }

            // Recipe Sorting
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var recipe_sorting_selector by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text(stringResource(R.string.recipe_sorting), fontSize = fontSize.sp)
                        Text(
                            stringResource(R.string.recipe_sorting_explainer),
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = (fontSize-4).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            fontSize = (fontSize-4).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        recipe_sorting,
                        fontSize = fontSize.sp,
                        modifier = Modifier
                            .clickable { recipe_sorting_selector = true }
                    )

                    if (recipe_sorting_selector) {
                        SelectorDialog(
                            stringResource(R.string.recipe_sorting),
                            Constants.RECIPE_SORTING_OPTIONS,
                            recipe_sorting,
                            on_option_selected = { viewModel.set_recipe_sorting(it) },
                            on_dismiss = { recipe_sorting_selector = false })
                    }
                }
            }

            // ########################################
            item {
                HorizontalDivider(modifier = Modifier.padding(top=13.dp))
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, fontSize = fontSize.sp)
            }

            // Recipe Language
            item {
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var recipe_lang_selector by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text(stringResource(R.string.recipe_language), fontSize = fontSize.sp)
                        Text(
                            stringResource(R.string.recipe_language_explainer),
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = (fontSize-4).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            fontSize = (fontSize-4).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        recipe_language,
                        fontSize = fontSize.sp,
                        modifier = Modifier
                            .clickable { recipe_lang_selector = true }
                    )

                    if (recipe_lang_selector) {
                        SearchableSelectorDialog(
                            stringResource(R.string.select_language),
                            Constants.GEMINI_LANGUAGES,
                            recipe_language,
                            on_option_selected = { viewModel.set_recipe_language(it) },
                            on_dismiss = { recipe_lang_selector = false })
                    }
                }
            }
        }
    }
}


@Composable
fun SelectorDialog(
    title:String,
    options_list: List<String>,
    selected_option: String,
    on_option_selected: (String) -> Unit,
    on_dismiss: () -> Unit
) {
        AlertDialog(
        onDismissRequest = on_dismiss,
        title = { Text(title) },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(
                        items = options_list,
                        key = { it }
                    ) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    on_option_selected(option)
                                    on_dismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(option)
                            }
                            if (option == selected_option) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = on_dismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SearchableSelectorDialog(
    title: String,
    options_list: List<String>,
    selected_option: String,
    on_option_selected: (String) -> Unit,
    on_dismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered_options = remember(query) {
        val q = query.lowercase().trim()
        if (q.isEmpty()) {
            options_list
        } else {
            options_list.filter { lang ->
                lang.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = on_dismiss,
        title = { Text(title) },
        text = {
            Column {
                // Search field
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Scrollable list
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    if (filtered_options.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_languages_found),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    } else {
                        items(
                            items = filtered_options,
                            key = { it }
                        ) { language ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        on_option_selected(language)
                                        on_dismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(language)
                                }
                                if (language == selected_option) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = on_dismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}