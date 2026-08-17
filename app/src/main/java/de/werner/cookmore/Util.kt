package de.werner.cookmore

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.apache.commons.text.StringEscapeUtils
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import kotlin.math.max
import kotlin.text.format


class Util {
    companion object {

        fun download_webpage_and_process(
            url: String,
            main_activity: MainActivity,
            get_app_state: () ->AppState,
            image_candidate_list: MutableList<String>,
            success_callback: (recipe: Recipe) -> Unit,
            failure_callback: () -> Unit) {

            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            val base_url = URL(url).host

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    main_activity.runOnUiThread {
                        Toast.makeText(main_activity, e.toString(), Toast.LENGTH_LONG)
                    }
                    failure_callback()
                }

                override fun onResponse(call: Call, response: Response) {
                    // Handle response (background thread)
                    val responseData = response.body?.string()
                    if (responseData != null) {
                        if (get_app_state() != AppState.CREATING_RECIPE) {
                            return // The app_state can change during LLM eval
                        }
                        // Parse HTML with JSoup
                        val doc = Jsoup.parse(responseData)
                        // Remove script and style tags
                        doc.select("script, style, img").forEach { it.remove() }
                        var clean_text = doc.text()
                        clean_text = clean_text.replace("\"", "")
                        Log.d("CookMore", "Length of webpage content: ${clean_text.length}")

                        query_llm(clean_text, main_activity, get_app_state, success_callback, failure_callback)
                        if (get_app_state() != AppState.CREATING_RECIPE) {
                            return // The app_state can change during LLM eval
                        }

                        // get images
                        val unescaped_html = StringEscapeUtils.unescapeHtml4(responseData)
                        val regex = Regex("""<img\s+[^>]*src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        val matches = regex.findAll(unescaped_html)
                        val urls = matches.map { it.groupValues[1] }.toMutableList()
                        clean_urls(urls, base_url)
                        image_candidate_list.addAll(urls)

                        RecipeUtil.try_finalize_new_recipe(main_activity, url=url, success_callback=success_callback)
                    }
                }
            })
        }

        fun list_available_models() {
            val MEDIA_TYPE = "application/json".toMediaType()
            val requestBody = """"""
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/listmodels")
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", Constants.LLM_API_KEY)
                .post(requestBody.toRequestBody(MEDIA_TYPE))
                .build()

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val test = 0
                    throw Exception("list model failure")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    val json_object = JSONObject(responseData!!)
                    println(json_object)
                }
            })
        }

        fun query_llm(
            page_content: String,
            main_activity: MainActivity,
            get_app_state: () -> AppState,
            success_callback: (recipe: Recipe) -> Unit,
            failure_callback: () -> Unit) {

            if (page_content.length > Constants.MAX_WEBPAGE_LENGTH) {
                main_activity.runOnUiThread {
                    Toast.makeText(main_activity, "Webpage too long, this is currently not supported", Toast.LENGTH_LONG).show()
                }
                failure_callback()
                return
            }

            val prompt =  "Summarize this HTML page by extracting the recipe content and cooking instructions and removing all unnecessary information.\n" +
                    "Finally, format this as an HTML page again, so that I can display it in a webview. " +
//                    "Include the following a css class in the header to give all tagged ingredients the following background color rgb(217, 234, 242, 0.5)" +
//                    "Surround every ingredient in the recipe with the following HTML span <span class=tagged data-tag=ingredient></span> ." +
//                    "If the ingredient is vegetarian or vegan, add this information in the data-tag like ingredient,vegetarian or ingredient,vegan" +
                    "Don't give me any additional text, just the HTML code. \n\n"
            val MEDIA_TYPE = "application/json".toMediaType()
            val requestBody = """{"contents":[{"parts": [{"text": "$prompt$page_content"}]}]}"""
            val request = Request.Builder()
                .url(Constants.LLM_API_ENDPOINT)
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", Constants.LLM_API_KEY)
                .post(requestBody.toRequestBody(MEDIA_TYPE))
                .build()

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    main_activity.runOnUiThread {
                        Toast.makeText(main_activity, e.toString(), Toast.LENGTH_LONG).show()
                    }
                    failure_callback()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (get_app_state() != AppState.CREATING_RECIPE) {
                        return // The app_state can change during LLM eval
                    }

                    val responseData = response.body?.string()
                    val json_object = JSONObject(responseData!!)
                    if (json_object.has("error")) {
                        if ((json_object.get("error") as JSONObject).get("code") as Int == 503) {
                            main_activity.runOnUiThread {
                                Toast.makeText(main_activity, "Model is overloaded, please try again later", Toast.LENGTH_LONG).show()
                            }
                        }
                        failure_callback()
                        return
                    }
                    val content: JSONObject = ((json_object.get("candidates") as JSONArray).get(0) as JSONObject).get("content") as JSONObject
                    val response_text: String = ((content.get("parts") as JSONArray).get(0) as JSONObject).get("text") as String
                    val cleaned_text = response_text.replace("```html", "").replace("```", "")
                    RecipeUtil.try_finalize_new_recipe(main_activity, content=cleaned_text, success_callback=success_callback)
                }
            })
        }

        private fun clean_urls(urls: MutableList<String>, base_url: String){
            for ((index, value) in urls.withIndex()) {
                if (value.startsWith("/")) {
                    urls[index] = "https://$base_url$value"
                }
            }
            val seen: MutableSet<String?> = HashSet<String?>()
            urls.removeIf({ s -> !seen.add(s) })
        }

        suspend fun download_bitmap_and_call_back(url: String, callback: (img: Bitmap?) -> Unit) {
            withContext(Dispatchers.IO) {
                val input = URL(url).openStream()
                val image = BitmapFactory.decodeStream(input)
                callback(image)
            }
        }

        fun load_bitmap(file: File): Bitmap? {
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.toString())
            }
            return  null
        }

        fun get_clipboard_text(context: Context): String? {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (!clipboard.hasPrimaryClip()) {
                return null
            }
            val clipData: ClipData? = clipboard.primaryClip
            // Make sure clipData is not null and has at least one item
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                // Coerce the item to text
                return item.coerceToText(context).toString()
            }
            return null
        }

        fun place_clibboard_text(context: Context, text: String, label: String = "Recipe Link") {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
        }

        suspend fun call_back_after(
            millis: Long,
            callback: () -> Unit) {

            delay(millis)
            callback()
        }

        fun clean_generated_html(html: String) : String{
            // Remove stray images
            var regex = Regex("<img (.*?)>", RegexOption.IGNORE_CASE)
            var result = regex.replace(html, "")
            // simple cleanup
            return result.trimIndent()
        }

        fun save_bitmap_as_jpeg(img: Bitmap, file: File) {
            file.createNewFile()
            val outputStream = FileOutputStream(file)
            img.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        fun create_icon_from_banner(recipe_folder: File) {
            val banner = load_bitmap(recipe_folder.resolve(Constants.BANNER_IMAGE_FILE_NAME))
            if (banner == null) {
                return
            }
            val image_file = recipe_folder.resolve(Constants.ICON_IMAGE_FILE_NAME)
            image_file.createNewFile()
            val outputStream = FileOutputStream(image_file)
            val icon = square_center_crop(banner)
            icon.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        fun square_center_crop(bitmap: Bitmap): Bitmap {
            val size = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            return Bitmap.createBitmap(bitmap, x, y, size, size)
        }

        fun load_json(file: File): JSONObject? {
            try {
                val inputStream = FileInputStream(file)
                inputStream.use {
                    val bytes = it.readBytes()
                    val contents = bytes.toString(Charset.defaultCharset())
                    return JSONObject(contents)

                }
            }
            catch (e: FileNotFoundException) {
                return null
            }
        }

        fun find_highest_id(): Int {
            val recipe_dir = Constants.FILES_DIR.resolve(Constants.RECIPES_DIR)
            var id = 1
            recipe_dir.list()?.forEach { file ->
                if (recipe_dir.resolve(file).isDirectory) {
                    if (file.contains("_")) {
                        val parts = file.split("_")
                        try {
                            val new_id = parts[0].toInt()
                            id = max(id, new_id)
                        }
                        catch (e: Throwable) {}
                    }
                }
            }
            return id
        }

        fun sanity_check_url(url: String): Boolean {
            if (url == "") return false
            if (url.length < 5)  return false
            if (!url.contains("http"))  return false

            return true
        }
    }
}

fun Int.toHtmlColor(): String = String.format("#%06X", 0xFFFFFF and this)

inline fun <reified T : Activity> Context.findActivity(): T? {
    var context = this
    while (context is ContextWrapper) {
        if (context is T) return context
        context = context.baseContext
    }
    return null
}