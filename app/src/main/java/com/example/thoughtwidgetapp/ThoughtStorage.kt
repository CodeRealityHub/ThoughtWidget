
package com.example.thoughtwidgetapp

import android.content.Context

object ThoughtStorage {

    private const val PREFS = "thought_widget_prefs"

    private const val KEY_THOUGHT = "thought"
    private const val KEY_OCR = "ocr_text"
    private const val KEY_IMAGE = "image_path"
    private const val KEY_APPEARANCE = "widget_appearance"

    const val APPEARANCE_LIGHT = "light"
    const val APPEARANCE_DARK = "dark"
    const val APPEARANCE_GRADIENT = "gradient"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(
        context: Context,
        thought: String,
        ocrText: String,
        imagePath: String
    ) {
        prefs(context).edit()
            .putString(KEY_THOUGHT, thought)
            .putString(KEY_OCR, ocrText)
            .putString(KEY_IMAGE, imagePath)
            .apply()
    }

    fun getThought(context: Context): String =
        prefs(context).getString(KEY_THOUGHT, "") ?: ""

    fun getOcrText(context: Context): String =
        prefs(context).getString(KEY_OCR, "") ?: ""

    fun getImagePath(context: Context): String =
        prefs(context).getString(KEY_IMAGE, "") ?: ""

    fun saveAppearance(context: Context, appearance: String) {
        val validAppearance = when (appearance) {
            APPEARANCE_DARK -> APPEARANCE_DARK
            APPEARANCE_GRADIENT -> APPEARANCE_GRADIENT
            else -> APPEARANCE_LIGHT
        }

        prefs(context).edit()
            .putString(KEY_APPEARANCE, validAppearance)
            .apply()
    }

    fun getAppearance(context: Context): String =
        prefs(context).getString(
            KEY_APPEARANCE,
            APPEARANCE_LIGHT
        ) ?: APPEARANCE_LIGHT
}
