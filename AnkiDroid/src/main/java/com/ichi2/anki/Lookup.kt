package com.ichi2.anki

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View

import com.afollestad.materialdialogs.MaterialDialog
import com.ichi2.anki.MetaDB.storeLanguage

import com.ichi2.libanki.Utils

import timber.log.Timber

object Lookup {

    /**
     * Searches
     */
    private val DICTIONARY_NONE = 0    // use no dictionary
    private val DICTIONARY_AEDICT = 1  // Japanese dictionary
    private val DICTIONARY_EIJIRO_WEB = 2 // japanese web dictionary
    private val DICTIONARY_LEO_WEB = 3 // German web dictionary for English, French, Spanish, Italian,
    // Chinese, Russian
    private val DICTIONARY_LEO_APP = 4 // German web dictionary for English, French, Spanish, Italian,
    // Chinese, Russian
    private val DICTIONARY_COLORDICT = 5
    private val DICTIONARY_FORA = 6
    private val DICTIONARY_NCIKU_WEB = 7 // chinese web dictionary

//    private var mContext: Context? = null
    var isAvailable: Boolean = false
        private set
    private var mDictionaryAction: String? = null
    private var mDictionary: Int = 0
    private var mLookupText: String? = null


    fun searchStringTitle(context: Context): String {
        return String.format(context.getString(R.string.menu_search),
                context.resources.getStringArray(R.array.dictionary_labels)[mDictionary])
    }


    fun initialize(context: Context): Boolean {
        val preferences = AnkiDroidApp.getSharedPrefs(AnkiDroidApp.getInstance().baseContext)
        mDictionary = Integer.parseInt(preferences.getString("dictionary", Integer.toString(DICTIONARY_NONE)))
        when (mDictionary) {
            DICTIONARY_NONE -> isAvailable = false
            DICTIONARY_AEDICT -> {
                mDictionaryAction = "sk.baka.aedict.action.ACTION_SEARCH_EDICT"
                isAvailable = Utils.isIntentAvailable(context, mDictionaryAction)
            }
            DICTIONARY_LEO_WEB, DICTIONARY_NCIKU_WEB, DICTIONARY_EIJIRO_WEB -> {
                mDictionaryAction = "android.intent.action.VIEW"
                isAvailable = Utils.isIntentAvailable(context, mDictionaryAction)
            }
            DICTIONARY_LEO_APP -> {
                mDictionaryAction = "android.intent.action.SEND"
                isAvailable = Utils.isIntentAvailable(context, mDictionaryAction, ComponentName(
                        "org.leo.android.dict", "org.leo.android.dict.LeoDict"))
            }
            DICTIONARY_COLORDICT -> {
                mDictionaryAction = "colordict.intent.action.SEARCH"
                isAvailable = Utils.isIntentAvailable(context, mDictionaryAction)
            }
            DICTIONARY_FORA -> {
                mDictionaryAction = "com.ngc.fora.action.LOOKUP"
                isAvailable = Utils.isIntentAvailable(context, mDictionaryAction)
            }
            else -> isAvailable = false
        }
        Timber.v("Is intent available = %b", isAvailable)
        return isAvailable
    }


    fun lookUp(context: Context, text: String): Boolean {
        var text = text
        if (!isAvailable) {
            return false
        }
        // clear text from leading and closing dots, commas, brackets etc.
        text = text.trim { it <= ' ' }.replace("[,;:\\s\\(\\[\\)\\]\\.]*$".toRegex(), "").replace("^[,;:\\s\\(\\[\\)\\]\\.]*".toRegex(), "")
        when (mDictionary) {
            DICTIONARY_NONE -> return false
            DICTIONARY_AEDICT -> {
                val aedictSearchIntent = Intent(mDictionaryAction)
                aedictSearchIntent.putExtra("kanjis", text)
                context!!.startActivity(aedictSearchIntent)
                return true
            }
            DICTIONARY_LEO_WEB, DICTIONARY_LEO_APP -> {
                mLookupText = text
                // localisation is needless here since leo.org translates only into or out of German
                val itemValues = arrayOf<CharSequence>("en", "fr", "es", "it", "ch", "ru")
                val language = getLanguage(MetaDB.LANGUAGES_QA_UNDEFINED)
                if (language.length > 0) {
                    for (itemValue in itemValues) {
                        if (language == itemValue) {
                            lookupLeo(context, language, mLookupText)
                            mLookupText = ""
                            return true
                        }
                    }
                }
                val items = arrayOf("Englisch", "Französisch", "Spanisch", "Italienisch", "Chinesisch", "Russisch")
                MaterialDialog.Builder(context)
                        .title("\"" + mLookupText + "\" nachschlagen")
                        .items(*items)
                        .itemsCallback { materialDialog, view, item, charSequence ->
                            val language = itemValues[item].toString()
                            storeLanguage(language, MetaDB.LANGUAGES_QA_UNDEFINED)
                            lookupLeo(context, language, mLookupText)
                            mLookupText = ""
                        }
                        .build().show()
                return true
            }
            DICTIONARY_COLORDICT -> {
                val colordictSearchIntent = Intent(mDictionaryAction)
                colordictSearchIntent.putExtra("EXTRA_QUERY", text)
                context!!.startActivity(colordictSearchIntent)
                return true
            }
            DICTIONARY_FORA -> {
                val foraSearchIntent = Intent(mDictionaryAction)
                foraSearchIntent.putExtra("HEADWORD", text.trim { it <= ' ' })
                context.startActivity(foraSearchIntent)
                return true
            }
            DICTIONARY_NCIKU_WEB -> {
                val ncikuWebIntent = Intent(mDictionaryAction, Uri.parse("http://m.nciku.com/en/entry/?query=" + text))
                context.startActivity(ncikuWebIntent)
                return true
            }
            DICTIONARY_EIJIRO_WEB -> {
                val eijiroWebIntent = Intent(mDictionaryAction, Uri.parse("http://eow.alc.co.jp/" + text))
                context.startActivity(eijiroWebIntent)
                return true
            }
        }
        return false
    }


    private fun lookupLeo(context: Context, language: String, text: CharSequence?) {
        when (mDictionary) {
            DICTIONARY_LEO_WEB -> {
                val leoSearchIntent = Intent(mDictionaryAction, Uri.parse("http://pda.leo.org/?lp=" + language
                        + "de&search=" + text))
                context.startActivity(leoSearchIntent)
            }
            DICTIONARY_LEO_APP -> {
                val leoAppSearchIntent = Intent(mDictionaryAction)
                leoAppSearchIntent.putExtra("org.leo.android.dict.DICTIONARY", language + "de")
                leoAppSearchIntent.putExtra(Intent.EXTRA_TEXT, text)
                leoAppSearchIntent.component = ComponentName("org.leo.android.dict",
                        "org.leo.android.dict.LeoDict")
                context.startActivity(leoAppSearchIntent)
            }
        }
    }


    private fun getLanguage(questionAnswer: Int): String {
        // if (mCurrentCard == null) {
        return ""
        // } else {
        // return MetaDB.getLanguage(mContext, mDeckFilename, Models.getModel(DeckManager.getMainDeck(),
        // mCurrentCard.getCardModelId(), false).getId(), mCurrentCard.getCardModelId(), questionAnswer);
        // }
    }


    private fun storeLanguage(language: String, questionAnswer: Int) {
        // if (mCurrentCard != null) {
        // MetaDB.storeLanguage(mContext, mDeckFilename, Models.getModel(DeckManager.getMainDeck(),
        // mCurrentCard.getCardModelId(), false).getId(), mCurrentCard.getCardModelId(), questionAnswer, language);
        // }
    }

}
