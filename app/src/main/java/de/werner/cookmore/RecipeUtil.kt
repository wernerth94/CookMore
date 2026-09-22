package de.werner.cookmore

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.charset.Charset


class RecipeUtil {
    companion object {
        fun load_all_recipes(sorted: Boolean = true): List<Recipe>{
            val result = mutableListOf<Recipe>()
            val base_folder = Constants.FILES_DIR.resolve(Constants.RECIPES_DIR)
            base_folder.listFiles()?.forEach { folder_name ->
                val folder = base_folder.resolve(folder_name)
                if (folder.exists()) {
                    val meta = Util.load_json(folder.resolve(Constants.META_DATA_FILE_NAME))
                    if (meta != null) {
                        result.add(Recipe(
                            folder.toString(),
                            meta.getString("url"),
                            meta.getString("title"),
                            meta.getInt("id"),
                            if (meta.has("image_url")) meta.getString("image_url") else ""))
                    }
                }
            }
            if (sorted) {
               return result.sortedWith( compareBy { it.title } )
            }
            return result
        }

        fun still_exists(recipe: Recipe): Boolean {
            return File(recipe.path).exists()
        }

        fun get_icon(recipe: Recipe): Bitmap? {
            val icon_file = File(recipe.path, Constants.ICON_IMAGE_FILE_NAME)
            return Util.load_bitmap(icon_file)
        }


        fun get_banner(recipe: Recipe): Bitmap? {
            val banner_file = File(recipe.path, Constants.BANNER_IMAGE_FILE_NAME)
            return Util.load_bitmap(banner_file)
        }

        fun get_html_page(recipe: Recipe): String? {
            try {
                val inputStream = FileInputStream(File(recipe.path).resolve(Constants.HTML_PAGE_FILE_NAME))
                inputStream.use {
                    val bytes = it.readBytes()
                    val contents = bytes.toString(Charset.defaultCharset())
                    return contents

                }
            }
            catch (e: FileNotFoundException) {
                return null
            }
        }

        fun inject_css_for_viewer(recipe_html: String, background_color: Color, font_size: Int): String {
            val header_regex = Regex("</head>", RegexOption.IGNORE_CASE)
            val injection = """
                <style>
                    body {
                      background-color: ${background_color.hashCode().toHtmlColor()};
                      font-size: ${font_size}px;
                    }
                    .tagged {
                      display: inline;  
                      background: rgb(217, 234, 242, 0.5);  
                    }
                  </style>
                </head>
            """
            val result = header_regex.replace(recipe_html, injection)
            return result
        }


    }
}
