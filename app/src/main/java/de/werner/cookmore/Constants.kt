package de.werner.cookmore

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

        val BANNER_SIZE_OPTIONS = listOf(
            "Full",
            "Compact",
            "None"
        )

        val RECIPE_SORTING_OPTIONS = listOf(
            "Title",
            "Date"
        )

        val APP_LANGUAGES = listOf(
            "English",
            "German",
        )

        val GEMINI_LANGUAGES = listOf(
            "Keep original",
            "Afrikaans",
            "Albanian",
            "Amharic",
            "Arabic",
            "Armenian",
            "Assamese",
            "Azerbijani",
            "Basque",
            "Belarusian",
            "Bengali",
            "Bosnian",
            "Bulgarian",
            "Catalan",
            "Chinese (Simplified/Traditional/Hong Kong)",
            "Croatian",
            "Czech",
            "Danish",
            "Dutch",
            "English",
            "Estonian",
            "Farsi",
            "Filipino",
            "Finnish",
            "French",
            "Galician",
            "Georgian",
            "German",
            "Greek",
            "Gujarati",
            "Hebrew",
            "Hindi",
            "Hungarian",
            "Icelandic",
            "Indonesian",
            "Italian",
            "Japanese",
            "Kannada",
            "Kazakh",
            "Khmer",
            "Korean",
            "Lao",
            "Latvian",
            "Lithuanian",
            "Macedonian",
            "Malay",
            "Malayalam",
            "Marathi",
            "Mongolian",
            "Nepali",
            "Norwegian",
            "Odia",
            "Polish",
            "Portuguese",
            "Punjabi",
            "Romanian",
            "Russian",
            "Serbian",
            "Slovak",
            "Slovenian",
            "Spanish",
            "Swahili",
            "Swedish",
            "Tamil",
            "Telugu",
            "Thai",
            "Turkish",
            "Ukrainian",
            "Urdu",
            "Uzbek",
            "Vietnamese",
            "Zulu"
        )

    }
}