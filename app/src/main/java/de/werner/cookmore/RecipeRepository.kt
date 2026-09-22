package de.werner.cookmore

import android.content.ContentValues
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class RecipeRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val RECIPES_KEY = stringSetPreferencesKey("recipes")
        val LAST_ID_KEY = intPreferencesKey("last_id")

        val NEW_RECIPE_URL_KEY = stringPreferencesKey("new_recipe_url")
        val NEW_RECIPE_HTML_KEY = stringPreferencesKey("new_recipe_html")
        val NEW_RECIPE_IMG_URL_KEY = stringPreferencesKey("new_recipe_img_url")
        val NEW_RECIPE_NO_IMG_KEY = booleanPreferencesKey("new_recipe_no_img")
    }

    var new_recipe_url: Flow<String> = dataStore.data
        .map { prefs ->
            prefs[NEW_RECIPE_URL_KEY] ?: ""
        }
    var new_recipe_html: Flow<String> = dataStore.data
        .map { prefs ->
            prefs[NEW_RECIPE_HTML_KEY] ?: ""
        }
    var new_recipe_image_url: Flow<String> = dataStore.data
        .map { prefs ->
            prefs[NEW_RECIPE_IMG_URL_KEY] ?: ""
        }
    var new_recipe_no_image_needed: Flow<Boolean> = dataStore.data
        .map { prefs ->
            prefs[NEW_RECIPE_NO_IMG_KEY] ?: false
        }

//    var new_recipe_image: Bitmap? = null


    var _search_query  =  MutableStateFlow("")
    val search_query: StateFlow<String> = _search_query.asStateFlow()

    fun apply_search(query:String) {
        _search_query.value = query
    }

    val recipes_flow: Flow<List<Recipe>> = dataStore.data
        .map { prefs ->
            prefs[RECIPES_KEY] ?: emptySet()
            RecipeUtil.load_all_recipes()
        }
        .combine(search_query) { recipes, query ->
            if (query.isBlank()) {
                recipes
            } else {
                val q = query.lowercase()
                recipes.filter { recipe ->
                    recipe.title.lowercase().contains(q)
                }
            }
        }

        .catch {
            emit(emptyList())
        }

    val last_id: Flow<Int> = dataStore.data
        .map { prefs ->
            prefs[LAST_ID_KEY] ?: Util.find_highest_id()
        }
        .catch {
            emit(0)
        }


    suspend fun store_new_recipe(): Recipe {
        // Meta Data
        val meta_data = JSONObject()
        val regex = Regex("<h1>(.*?)</h1>", RegexOption.IGNORE_CASE)
        val result = regex.find(new_recipe_html.first())
        if (result == null) {
            throw kotlin.IllegalArgumentException("The HTML page contains no H1 tag")
        }
        val recipe_id = last_id.first()
        val title = result.groupValues[1]
        meta_data.put("url", new_recipe_url.first())
        if (new_recipe_url.first() != "") meta_data.put("image_url", new_recipe_image_url)
        meta_data.put("title", title)
        meta_data.put("id", recipe_id)

        val base_folder = Constants.FILES_DIR.resolve(Constants.RECIPES_DIR)
        val recipe_folder = base_folder.resolve(recipe_id.toString() + "_" + title.trim().replace(" ", ""))
        if (recipe_folder.exists()) {
            recipe_folder.deleteRecursively()
        }
        recipe_folder.mkdirs()
        val meta_file = recipe_folder.resolve(Constants.META_DATA_FILE_NAME)
        meta_file.createNewFile()
        var outputStream = FileOutputStream(meta_file)
        outputStream.use {
            it.write(meta_data.toString(4).toByteArray())
        }

        // HTML Page
        val cleaned_html_data = Util.clean_generated_html(new_recipe_html.first())
        val html_file = recipe_folder.resolve("page.html")
        html_file.createNewFile()
        outputStream = FileOutputStream(html_file)
        outputStream.use {
            it.write(cleaned_html_data.toByteArray())
        }

        if (!new_recipe_no_image_needed.first()) {
            // Banner Image
            val image_file = recipe_folder.resolve(Constants.BANNER_IMAGE_FILE_NAME)
            withContext(Dispatchers.IO) {
                val input = URL(new_recipe_image_url.first()).openStream()
                val image = BitmapFactory.decodeStream(input)
                Util.save_bitmap_as_jpeg(image, image_file)
            }

            // Icon
            Util.create_icon_from_banner(recipe_folder)
        }

        val recipe = Recipe(
                recipe_folder.toString(),
                new_recipe_url.first(),
                title,
                recipe_id,
                new_recipe_url.first()
            )

        dataStore.edit { prefs ->
            prefs[NEW_RECIPE_URL_KEY] = ""
            prefs[NEW_RECIPE_HTML_KEY] = ""
            prefs[NEW_RECIPE_IMG_URL_KEY] = ""
            prefs[NEW_RECIPE_NO_IMG_KEY] = false
            prefs[LAST_ID_KEY] = recipe_id + 1
        }

        return recipe
    }


    suspend fun delete_recipe(recipe: Recipe) {
        val recipe_folder = File(recipe.path)
        recipe_folder.deleteRecursively()

        dataStore.edit { prefs ->
            prefs[RECIPES_KEY] = setOf("Reload plz") // Trigger reload of recipes
        }

    }


    fun update_recipe(recipe: Recipe, new_title:String="", new_html:String="") {
        if (new_title != "") {
            val meta_file = File(recipe.path, Constants.META_DATA_FILE_NAME)
            val meta_data = Util.load_json(meta_file)
            if (meta_data != null) {
                meta_data.put("title", new_title)
                val outputStream = FileOutputStream(meta_file)
                outputStream.use {
                    it.write(meta_data.toString(4).toByteArray())
                }
            }
        }

        if (new_html != "") {
            val html_file = File(recipe.path, Constants.HTML_PAGE_FILE_NAME)
            if (html_file.exists()) {
                val outputStream = FileOutputStream(html_file)
                outputStream.use {
                    it.write(new_html.toByteArray())
                }
            }
        }
    }


    suspend fun try_finalize_new_recipe(
        url: String? = null, content: String? = null,
        image_url: String? = null,
        no_image_needed: Boolean? = null,
        success_callback: (recipe: Recipe) -> Unit)
    {
        if (url != null) {
            dataStore.edit { prefs ->
                prefs[NEW_RECIPE_URL_KEY] = url
            }
        }
        if (content != null) {
            dataStore.edit { prefs ->
                prefs[NEW_RECIPE_HTML_KEY] = content
            }
        }
        if (image_url != null) {
            dataStore.edit { prefs ->
                prefs[NEW_RECIPE_IMG_URL_KEY] = image_url
            }
        }
        if (no_image_needed != null) {
            dataStore.edit { prefs ->
                prefs[NEW_RECIPE_NO_IMG_KEY] = no_image_needed
            }
        }

        if (new_recipe_url.first() != "" &&
            new_recipe_html.first() != "" &&
            (new_recipe_no_image_needed.first() || new_recipe_image_url.first() != "")
        ) {
            Log.d("CookMore", "Storing recipe")
            val new_recipe = store_new_recipe()
            success_callback(new_recipe)
        }
    }

    suspend fun import_recipes_from_file(context: MainActivity, uri: Uri, file_name: String = "cookmore_backup.zip") = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val dest_backup_file = File(Constants.FILES_DIR,file_name)
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest_backup_file).use { output ->
                input.copyTo(output)
            }
        }

        val dest_recipe_dir = Constants.FILES_DIR.resolve(Constants.RECIPES_DIR)
        if (dest_recipe_dir.exists()) {
            dest_recipe_dir.deleteRecursively()
        }
        dest_recipe_dir.mkdirs()

        ZipFile(dest_backup_file).use { zip->
            zip.entries().asSequence().forEach { entry ->
                if (!entry.isDirectory) {
                    val outputFile = File(dest_recipe_dir, entry.name)
                    outputFile.parentFile?.mkdirs()

                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outputFile).use {output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }

        dest_backup_file.delete() // Remove copied zip
        dataStore.edit { prefs ->
            prefs[RECIPES_KEY] = setOf("Reload plz") // Trigger reload of recipes
        }
    }


    suspend fun export_recipes_to_file(context: MainActivity, file_name: String = "cookmore_backup.zip" ) = withContext(Dispatchers.IO) {
        val src_dir = Constants.FILES_DIR.resolve(Constants.RECIPES_DIR)
        val dest_backup_file = File(Constants.FILES_DIR, file_name)
        if (dest_backup_file.exists()) dest_backup_file.delete()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(dest_backup_file))).use { zos ->
            src_dir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val entryName = file.relativeTo(src_dir).path.replace("\\", "/")
                    val entry = ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    file.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val content_values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file_name)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
        }

        val item_uri = resolver.insert(collection, content_values) ?: return@withContext
        resolver.openOutputStream(item_uri)?.use { output ->
            dest_backup_file.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        context.runOnUiThread {
            Toast.makeText(context, "Backup copied to Downloads", Toast.LENGTH_LONG).show()
        }
    }
}
