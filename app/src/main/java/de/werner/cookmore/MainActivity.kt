package de.werner.cookmore

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import de.werner.cookmore.ui.theme.CookMoreTheme


enum class ForcedInteractionResult {
    NONE, CANCEL, SAVE
}

enum class AppState {
    IDLE, CREATING_RECIPE
}

class CookMoreApplication : Application() {
    val data_store: DataStore<Preferences> by preferencesDataStore(name="settings")
    var current_recipe: Recipe? = null
}


class MainActivity : ComponentActivity() {

    lateinit var recipe_view_model: RecipeViewModel
    lateinit var settings_view_model: SettingsViewModel
    lateinit var select_backup_file_launcher: ActivityResultLauncher<Intent>
    lateinit var app: CookMoreApplication


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Constants.FILES_DIR = filesDir
        app = application as CookMoreApplication

        recipe_view_model = RecipeViewModel(RecipeRepository(app.data_store))
        settings_view_model = SettingsViewModel(SettingsRepository(app.data_store))

        select_backup_file_launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri: Uri ->
                    recipe_view_model.import_recipes_from_file(this@MainActivity, uri)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            CookMoreTheme {
                CookMoreApp()
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@PreviewScreenSizes
@Composable
fun CookMoreApp() {
    val context = LocalContext.current
    val parent_activity = context.findActivity<MainActivity>()

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MY_RECIPES) }
    var app_state by rememberSaveable  { mutableStateOf(AppState.IDLE) }
    var navigation_ui_state by rememberSaveable  { mutableStateOf(NavigationUIState.NAV) }
    var regen_this_recipe by rememberSaveable  { mutableStateOf<Recipe?>(null) }
    var forced_interaction_result by rememberSaveable  { mutableStateOf(ForcedInteractionResult.NONE) }

    BackHandler(enabled = currentDestination != AppDestinations.MY_RECIPES) {
        currentDestination = AppDestinations.MY_RECIPES
    }

    if (parent_activity != null) {

        fun change_app_state(s: AppState) {
            app_state = s
        }
        fun get_app_state(): AppState {
            return app_state
        }

        fun navigate_to(dest: AppDestinations) {
            currentDestination = dest
        }

        fun force_user_interaction(type: NavigationUIState) {
            navigation_ui_state = type
        }

        fun cancel_forced_user_interaction() {
            navigation_ui_state = NavigationUIState.NAV
        }

        fun set_current_recipe(recipe: Recipe?) {
            parent_activity.app.current_recipe = recipe
        }

        fun new_recipe_callback(recipe: Recipe?) {
            app_state = AppState.IDLE
            cancel_forced_user_interaction()
            if (recipe != null) {
                if (regen_this_recipe != null) {
                    regen_this_recipe?.let {
                        parent_activity.recipe_view_model.delete_recipe(it)
                    }
                    regen_this_recipe = null
                }
                parent_activity.app.current_recipe = recipe
                currentDestination = AppDestinations.CURRENT_RECIPE
            } else {
                currentDestination = AppDestinations.MY_RECIPES
            }
        }

        fun regenerate_recipe(recipe: Recipe) {
            regen_this_recipe = recipe
            currentDestination = AppDestinations.NEW_RECIPE
        }


        val font_size = parent_activity.settings_view_model.font_size.collectAsState()

        NavigationSuiteScaffold(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.surfaceVariant,
            navigationSuiteItems = {
                if(navigation_ui_state == NavigationUIState.SAVE) {
                    item(
                        icon = { Icon(Icons.Default.Done, contentDescription = "Save") },
                        label = { Text(stringResource(R.string.save), fontSize = font_size.value.sp) },
                        selected = true,
                        enabled = true,
                        onClick = {
                            forced_interaction_result = ForcedInteractionResult.SAVE
                            navigation_ui_state = NavigationUIState.NAV
                        },
                    )
                }
                else if (navigation_ui_state == NavigationUIState.CANCEL) {
                    item(
                        icon = { Icon(Icons.Default.Close, contentDescription = "Cancel") },
                        label = { Text(stringResource(R.string.cancel), fontSize = font_size.value.sp) },
                        selected = true,
                        enabled = true,
                        onClick = {
                            forced_interaction_result = ForcedInteractionResult.CANCEL
                            navigation_ui_state = NavigationUIState.NAV
                        },
                    )
                }
                else {
                    item(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.list),
                                contentDescription = "My Recipes",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(
                            stringResource(R.string.my_recipes),
                            fontSize = font_size.value.sp,
                            textAlign = TextAlign.Center, // optional but explicit
                            overflow = TextOverflow.Ellipsis
                        ) },
                        selected = AppDestinations.MY_RECIPES == currentDestination,
                        onClick = {
                            currentDestination = AppDestinations.MY_RECIPES
                        }
                    )
                    item(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.article),
                                contentDescription = "Current Recipe",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(
                            stringResource(R.string.current_recipe),
                            fontSize = font_size.value.sp,
                            textAlign = TextAlign.Center, // optional but explicit
                            overflow = TextOverflow.Ellipsis
                        ) },
                        enabled = parent_activity.app.current_recipe != null,
                        selected = AppDestinations.CURRENT_RECIPE == currentDestination,
                        onClick = {
                            currentDestination = AppDestinations.CURRENT_RECIPE
                        }
                    )
                    item(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.add_circle),
                                contentDescription = "New Recipe",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(
                            stringResource(R.string.new_recipe),
                            fontSize = font_size.value.sp,
                            textAlign = TextAlign.Center, // optional but explicit
                            overflow = TextOverflow.Ellipsis
                        ) },
                        selected = AppDestinations.NEW_RECIPE == currentDestination,
                        onClick = {
                            currentDestination = AppDestinations.NEW_RECIPE
                        }
                    )
                }
            }
        ) {
            // App Content
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (currentDestination) {

                    AppDestinations.MY_RECIPES -> {
                        MyRecipes(
                            modifier = Modifier.padding(innerPadding),
                            navigation_callback = ::navigate_to,
                            set_current_recipe = ::set_current_recipe,
                            force_user_interaction = ::force_user_interaction,
                            cancel_forced_user_interaction = ::cancel_forced_user_interaction,
                            forced_interaction_result = forced_interaction_result
                        )
                        forced_interaction_result = ForcedInteractionResult.NONE
                    }

                    AppDestinations.CURRENT_RECIPE -> {
                        CurrentRecipe(
                            modifier = Modifier.padding(innerPadding),
                            parent_activity.app.current_recipe,
                            regenerate_callback = ::regenerate_recipe,
                            force_user_interaction = ::force_user_interaction,
                            cancel_forced_user_interaction = ::cancel_forced_user_interaction,
                            forced_interaction_result = forced_interaction_result
                        )
                        forced_interaction_result = ForcedInteractionResult.NONE
                    }

                    AppDestinations.NEW_RECIPE -> {
                        var autofill_url = ""
                        regen_this_recipe?.let { autofill_url = it.url }
                        NewRecipe(
                            modifier = Modifier.padding(innerPadding),
                            get_app_state = ::get_app_state,
                            change_app_state = ::change_app_state,
                            new_recipe_callback = ::new_recipe_callback,
                            autofill_url = autofill_url,
                            force_user_interaction = ::force_user_interaction,
                            cancel_forced_user_interaction = ::cancel_forced_user_interaction,
                            forced_interaction_result = forced_interaction_result
                        )
                        forced_interaction_result = ForcedInteractionResult.NONE
                    }

                    AppDestinations.SETTINGS -> {
                        SettingsComposable(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = parent_activity.settings_view_model
                        )
                    }
                }
            }
        }

        Util.check_runtime_permissions(context)
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
    val display_in_nav: Boolean
) {
    MY_RECIPES("My Recipes", R.drawable.list, true),
    CURRENT_RECIPE("Current Recipe", R.drawable.article, true),
    NEW_RECIPE("New Recipe", R.drawable.add_circle, true),
    SETTINGS("Settings", R.drawable.empty_icon, false),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.no_recipe_available),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CookMoreTheme {
        Greeting("Android")
    }
}