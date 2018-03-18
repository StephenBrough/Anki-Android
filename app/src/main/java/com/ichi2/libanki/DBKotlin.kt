package com.ichi2.libanki

import android.arch.persistence.room.Database
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.RoomDatabase
import org.json.JSONArray

/**
 * Database layer for AnkiDroid. Can read the native Anki format through Android's SQLite driver.
 */

//@Database(
//        entities = [Note::class, Card::class, Model::class, CardTemplate::class, ReviewInfo::class, Deck::class],
//        version = 1
//)
abstract class AnkiDb : RoomDatabase() {

}

@Entity
data class Notezz(
// Notes contain the raw information that is formatted into a number of cards according to the models
        @PrimaryKey
        val id: Long, // the epoch milliseconds of when the card was created
        val guid: String,  // globally unique id, almost certainly used for syncing
        val mod: String, // JSON  modification timestamp, epoch seconds
        val mid: Long, //  model id
        val usn: Int, // update sequence number: for finding diffs when syncing. See the description in the cards table for more info
        val tags: List<String>, //  space-separated string of tags. includes space at the beginning and end, for LIKE "% tag %" queries
        val flds: Array<String>, // the values of the fields in this note. separated by 0x1f (31) character.
        val sfld: String, // sort field: used for quick sorting and duplicate check
        val csum: Int, // field checksum used for duplicate check. integer representation of first 8 digits of sha1 hash of the first field
        val flags: Int, // unused
        val data: String // unused
)

@Entity
// A card is an instance of a note.
data class Cardzz(
        val id: Long, // the epoch milliseconds of when the card was created
        val note_id: Long,
        val ord: Long, // This is the ordinal of the card. A note has 1..n cards. The ordinal can also be used to directly access a card as describe in the class description.
        val card_name: String, // The card's name
        val question: String, // The question for this card
        val question_simple: String, // The question for this card in the simplified form, without card styling information (CSS)
        val answer_simple: String, // The answer for this card in the simplified form, without card styling information (CSS)
        val answer_pure: String // Purified version of the answer. In case the {@link #ANSWER} contains any additional elements (like a duplicate of the question) this is removed for {@link #ANSWER_PURE}. Like {@link #ANSWER_SIMPLE} it does not contain styling information (CSS).
)

@Entity
data class Modelzz(
        val id: Long, // Model ID
        val name: String, // Name of the model
        val field_name: String, // Names of all the fields, separate by the 0x1f character
        val field_names: String, //
        val num_cards: Int, // Number of card templates, which corresponds to the number of rows in the templates table
        val css: String, // CSS styling code which is shared across all the templates
        val sort_field_index: Int, // Which field is used as the main sort field
        val type: Int, // 0 for normal model, 1 for cloze model
        val latex_post: String, // Code to go at the end of LaTeX renderings in Anki Desktop
        val latex_pre: String, // ode to go at the front of LaTeX renderings in Anki Desktop
        val note_count: Int,
        val deck_id: Long // The default deck that cards should be added to
)

/**
 * Card template for a model. A template defines how to render the fields of a note into the actual HTML that
 * makes up a flashcard. A model can define multiple card templates, for example a Forward and Reverse Card could
 * be defined with the forward card allowing to review a word from Japanese->English (e.g. 犬 -> dog), and the
 * reverse card allowing review in the "reverse" direction (e.g dog -> 犬). When a Note is inserted, a Card will
 * be generated for each active CardTemplate which is defined.
 */
@Entity
data class CardTemplatezz(
        /**
         * Row ID. This is a virtual ID which actually does not exist in AnkiDroid's data base.
         * This column only exists so that this interface can be used with existing CursorAdapters
         * that require the existence of a "_id" column. This means, that it CAN NOT be used
         * reliably over subsequent queries. Especially if the number of cards or fields changes,
         * the _ID will change too.
         */
        val id: Long,
        val model_id: Long, // This is the ID of the model that this row belongs to (i.e. {@link Model#_ID})
        val ord: Long, // This is the ordinal / index of the card template (from 0 to number of cards - 1)
        val card_template_name: String, // The template name e.g. "Card 1"
//        val question_format: ?, // The definition of the template for the question
//        val answer_format: ?, // The definition of the template for the answer
//        val browser_question_format: ?, // Optional alternative definition of the template for the question when rendered with the browser
//        val browser_answer_format: ?, // Optional alternative definition of the template for the answer when rendered with the browser
        val card_count: Int //

)

//@Entity
data class ReviewInfozz(
        val id: Long,
        val note_id: Long, // This is the ID of the note that this card belongs to (i.e. {@link Note#_ID}).
        val ord: Long, // This is the ordinal of the card. A note has 1..n cards. The ordinal can also be used to directly access a card as describe in the class description.
        val button_count: Int, // This is the number of ease modes. It can take a value between 2 and 4
        val next_review_times: JSONArray, // This is a JSONArray containing the next review times for all buttons
        val media_files: String, // The names of the media files in the question and answer
        val answer_ease: Int, // Ease of an answer. Is not set when requesting the scheduled cards. Can take values of AbstractFlashcardViewer e.g. EASE_1
        val time_taken: Long // Time it took to answer the card (in ms)
)

// A Deck contains information about a deck contained in the users deck list.
//@Entity
data class Deckzz(
        val deck_name: String, // The name of the Deck
        val deck_id: Long, // The unique identifier of the Deck
        val deck_count: Int, //The number of cards in the Deck
//        val options: ?, // The options of the Deck
//        val deck_dyn: ?, // if dynamic (AKA filtered) deck
        val deck_desc: String // Deck description
)

@Entity
data class Col(
        val id: Long,
        val created_at: Long, // 'crt' - Date
        val modified_at: Long, // 'mod' - Date
        val scm: Long, // 'scm' - Date
        val version: Integer, // 'ver'
        val dty: Integer, // 'dty' - Dirty?
        val usn: Integer, // 'usn'
        val ls: Integer, // 'ls'
        val conf: String, // JSON text
        val models: String, // 'models' - JSON text
        val decks: String, // 'decks', - JSON text
        val dconf: String, // 'dconf' - JSON text
        val tags: String // 'tags' - JSON text

)