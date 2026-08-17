package de.werner.cookmore

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.painter.BitmapPainter
import java.io.File

class Constants {
    companion object {
        val MAX_WEBPAGE_LENGTH = 40000
        val META_DATA_FILE_NAME = "meta.json"
        val BANNER_IMAGE_FILE_NAME = "banner.jpeg"
        val ICON_IMAGE_FILE_NAME = "icon.jpeg"
        val HTML_PAGE_FILE_NAME = "page.html"
        lateinit var FILES_DIR: File
        val RECIPES_DIR = File("recipes")

        val EMPTY_ICON_FILE = File("empty_icon.png")

        val LLM_API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent"

        val LLM_API_KEY = "AIzaSyB4FgF-lKAm1EmANV9RVHMYXUZtUey0yYE"

    }
}