package tn.tp.atelierandroid

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Utilitaire pour la gestion et la persistance de la langue de l'application (Français / Anglais).
 * Utilise l'API moderne per-app language d'Android / AppCompat avec sauvegarde dans SharedPreferences.
 */
object LocaleHelper {
    private const val PREFS_NAME = "app_locale_prefs"
    private const val KEY_LANG = "app_language"

    const val LANG_FR = "fr"
    const val LANG_EN = "en"

    /**
     * Applique une nouvelle langue et la persiste.
     */
    fun setLocale(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, languageCode).apply()

        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Récupère le code de langue actuellement actif (par défaut "fr").
     */
    fun getLanguage(context: Context): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val tag = appLocales.toLanguageTags()
            if (tag.startsWith("en", ignoreCase = true)) return LANG_EN
            if (tag.startsWith("fr", ignoreCase = true)) return LANG_FR
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, LANG_FR) ?: LANG_FR
    }

    /**
     * S'assure que la langue sauvegardée est appliquée au lancement.
     */
    fun applySavedLocale(context: Context) {
        val savedLang = getLanguage(context)
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (!currentTag.startsWith(savedLang, ignoreCase = true)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLang))
        }
    }
}
