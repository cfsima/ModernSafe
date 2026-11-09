package io.github.cfsima.modernsafe.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.cfsima.modernsafe.R
import java.util.Locale

object LanguageManager {

    data class LanguageItem(
        val tag: String,
        val nativeName: String
    )

    // Supported language tags matching res/values-* folders
    val SUPPORTED_TAGS = listOf(
        "en",
        "sv",
        "de",
        "es",
        "fr",
        "it",
        "nl",
        "nb",
        "pl",
        "pt",
        "pt-BR",
        "ru",
        "zh-CN",
        "ja",
        "ko",
        "ar",
        "be",
        "bg",
        "cs",
        "el",
        "en-GB",
        "fi",
        "hi",
        "iw",
        "lv",
        "oc",
        "ro",
        "sc",
        "sd",
        "sk",
        "tl",
        "tr",
        "ug"
    )

    fun getLanguageItems(context: Context): List<LanguageItem> {
        val list = ArrayList<LanguageItem>(SUPPORTED_TAGS.size + 1)
        list.add(LanguageItem("", context.getString(R.string.pref_language_system_default)))

        for (tag in SUPPORTED_TAGS) {
            val locale = Locale.forLanguageTag(tag)
            val displayName = locale.getDisplayName(locale)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            list.add(LanguageItem(tag, displayName))
        }
        return list
    }

    fun getCurrentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return ""
        val currentTag = locales[0]?.toLanguageTag() ?: return ""
        if (SUPPORTED_TAGS.contains(currentTag)) return currentTag
        val lang = locales[0]?.language ?: ""
        if (SUPPORTED_TAGS.contains(lang)) return lang
        if (lang == "en") return "en"
        return currentTag
    }

    fun setLanguage(tag: String) {
        val localeList = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
