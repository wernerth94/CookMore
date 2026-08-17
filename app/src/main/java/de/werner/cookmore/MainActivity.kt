package de.werner.cookmore

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigationevent.NavigationEventInfo
import de.werner.cookmore.ui.theme.CookMoreTheme
import kotlinx.coroutines.launch
import okhttp3.Callback


enum class AppState {
    IDLE, CREATING_RECIPE
}



class MainActivity : ComponentActivity() {

    val data_store: DataStore<Preferences> by preferencesDataStore(name="settings")
    lateinit var recipe_repository: RecipeRepository
    lateinit var select_backup_file_launcher: ActivityResultLauncher<Intent>
    var current_recipe: Recipe? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Constants.FILES_DIR = filesDir

        recipe_repository = RecipeRepository(data_store)

        select_backup_file_launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri: Uri ->
                    lifecycleScope.launch {
                        recipe_repository.import_recipes_from_file(this@MainActivity, uri)
                    }
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

@PreviewScreenSizes
@Composable
fun CookMoreApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MY_RECIPES) }
    val context = LocalContext.current
    val parent_activity = context.findActivity<MainActivity>()

    BackHandler(enabled = currentDestination != AppDestinations.MY_RECIPES) {
        currentDestination = AppDestinations.MY_RECIPES
    }

    // Lock in portrait mode
    LaunchedEffect(Unit) {
        parent_activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }


    if (parent_activity != null) {
        var app_state by remember { mutableStateOf(AppState.IDLE) }
        var navigation_ui_state by remember { mutableStateOf(NavigationUIState.NAV) }
        var force_user_interaction_callback: ()->Unit = {}
        var regen_this_recipe by remember { mutableStateOf<Recipe?>(null) }

        fun change_app_state(s: AppState) {
            app_state = s
        }
        fun get_app_state(): AppState {
            return app_state
        }

        fun navigate_to(dest: AppDestinations) {
            currentDestination = dest
        }

        fun set_current_recipe(recipe: Recipe?) {
            parent_activity.current_recipe = recipe
        }

        fun new_recipe_callback(recipe: Recipe?) {
            app_state = AppState.IDLE
            if (recipe != null) {
                if (regen_this_recipe != null) {
                    regen_this_recipe?.let {
                        parent_activity.recipe_repository.delete_recipe(
                            parent_activity,
                            it
                        )
                    }
                    regen_this_recipe = null
                }
                parent_activity.current_recipe = recipe
                currentDestination = AppDestinations.CURRENT_RECIPE
            } else {
                currentDestination = AppDestinations.MY_RECIPES
            }
        }

        fun regenerate_recipe(recipe: Recipe) {
            regen_this_recipe = recipe
            currentDestination = AppDestinations.NEW_RECIPE
        }

        fun force_user_interaction(type: NavigationUIState, callback: ()->Unit) {
            navigation_ui_state = type
            force_user_interaction_callback = callback
        }

        NavigationSuiteScaffold(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.surfaceVariant,
            navigationSuiteItems = {
                if(navigation_ui_state == NavigationUIState.SAVE) {
                    item(
                        icon = { Icon(Icons.Default.Done, contentDescription = "Save") },
                        label = { Text("Save") },
                        selected = true,
                        enabled = true,
                        onClick = {
                            force_user_interaction_callback()
                            navigation_ui_state = NavigationUIState.NAV
                        },
                    )
                }
                else if (navigation_ui_state == NavigationUIState.CANCEL) {
                    item(
                        icon = { Icon(Icons.Default.Close, contentDescription = "Cancel") },
                        label = { Text("Cancel") },
                        selected = true,
                        enabled = true,
                        onClick = {
                            force_user_interaction_callback()
                            navigation_ui_state = NavigationUIState.NAV
                        },
                    )
                }
                else {
                    AppDestinations.entries.forEach {
                        var enabled: Boolean
                        if (it == AppDestinations.CURRENT_RECIPE) {
                            enabled =
                                app_state == AppState.IDLE && parent_activity.current_recipe != null && RecipeUtil.still_exists(
                                    parent_activity.current_recipe!!
                                )
                        } else {
                            enabled = app_state == AppState.IDLE
                        }
                        item(
                            icon = {
                                Icon(
                                    painter = painterResource(it.icon),
                                    contentDescription = it.label,
                                    modifier = Modifier.heightIn(max = 38.dp)
                                )
                            },
                            label = { Text(it.label) },
                            selected = it == currentDestination,
                            enabled = enabled,
                            onClick = {
                                currentDestination = it
                            },
                        )
                    }
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (currentDestination) {

                    AppDestinations.MY_RECIPES ->
                        MyRecipes(
                            modifier = Modifier.padding(innerPadding),
                            navigation_callback = ::navigate_to,
                            set_current_recipe = ::set_current_recipe,
                            force_user_interaction = ::force_user_interaction
                        )


                    AppDestinations.CURRENT_RECIPE ->
                        CurrentRecipe(
                            modifier = Modifier.padding(innerPadding),
                            regenerate_callback = ::regenerate_recipe,
                            force_user_interaction = ::force_user_interaction
                        )


                    AppDestinations.NEW_RECIPE -> {
                        var autofill_url = ""
                        regen_this_recipe?.let { autofill_url = it.url }
                        NewRecipe(
                            modifier = Modifier.padding(innerPadding),
                            get_app_state = ::get_app_state,
                            change_app_state = ::change_app_state,
                            new_recipe_callback = ::new_recipe_callback,
                            autofill_url = autofill_url
                        )
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    MY_RECIPES("My Recipes", R.drawable.cookbook),
    CURRENT_RECIPE("Current Recipe", R.drawable.current_recipe),
    NEW_RECIPE("New Recipe", R.drawable.new_recipe),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "No Recipe available",
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