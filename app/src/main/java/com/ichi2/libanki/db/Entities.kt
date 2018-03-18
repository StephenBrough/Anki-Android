package com.ichi2.libanki.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

/**
 * Cards are what you review
 *
 * There can be multiple cards per note, determined by the template
 */
@Entity(indices = [
    Index(name = "ix_cards_nid", value = ["nid"]),
    Index(name = "ix_cards_sched", value = ["did", "queue", "due"]),
    Index(name = "ix_cards_usn", value = ["usn"])
])
data class Cards(

        @PrimaryKey
        // Milliseconds representing when the card was created
        val id: Long,

        // notes.id
        @ColumnInfo(name = "nid")
        val notesId: Long,

        // Deck id - available in the [Collection] table
        @ColumnInfo(name = "did")
        val deckId: Long,

        // Identifies which of the card templates it corresponds to; valid values are 0..num templates - 1
        @ColumnInfo(name = "ord")
        val ordinal: Int,

        // Timestamp in seconds for the last modified date
        @ColumnInfo(name = "mod")
        val modifiedAt: Long,

        // Used to figure out diffs when syncing:
        //      value of -1 indicates changes that need to be pushed to the server
        //      usn < server indicates changes that need to be pulled from the server
        @ColumnInfo(name = "usn")
        val updateSequenceNum: Int,

        // 0 = new, 1 = learning, 2 = due, 3 = filtered
        val type: Int,

        // -3 = sched buried, -2 = user buried, -1 = suspended, 0 = new, 1 = learning, 2 = due (as for type)
        //  3 = in learning, next review in at least a day after the previous review
        val queue: Int,

        // Due is used differently for different card types:
        //      - New: Note id or random int
        //      - Due: Integer day, relative to the collection's creation time
        //      - Learning: integer timestamp
        val due: Int,

        // Interval (used in the SRS algorithm).
        //      Negative = seconds
        //      Positive = days
        @ColumnInfo(name = "ivl")
        val interval: Int,

        // Used in SRS algorithm
        val factor: Int,

        // The number of times a card has been reviewed
        val reps: Int,

        // Tracks the state change from when a user answers a card correctly to incorrectly
        val lapses: Int,

        // Number of reps left until the user graduates from the card
        val left: Int,

        // Only used when the card is currently in a filtered deck
        @ColumnInfo(name = "odue")
        val originalDue: Int,

        // Only used when the card is currently in a filtered deck
        @ColumnInfo(name = "odid")
        val originalDeckId: Long,

        // Currently unused
        val flags: Int,

        // Currently unused
        val data: String
)

/**
 * Contains a single row that holds various information about the Anki collection
 */
@Entity(tableName = "col")
data class Collection(
        // Arbitrary number since there is only one row
        val id: Long,

        // Created timestamp
        @ColumnInfo(name = "crt")
        val createdAt: Long,

        // Last modified timestamp in milliseconds
        @ColumnInfo(name = "mod")
        val modifiedAt: Long,

        // Time when the "schema" was modified
        // If the server scm is different from the client scm a full-sync is required
        @ColumnInfo(name = "scm")
        val schemaModifiedAt: Long,

        @ColumnInfo(name = "ver")
        val version: Int,

        // Unused, set to 0
        @ColumnInfo(name = "dty")
        val dirty: Int,

        // Used to figure out diffs when syncing:
        //      value of -1 indicates changes that need to be pushed to the server
        //      usn < server indicates changes that need to be pulled from the server
        @ColumnInfo(name = "usn")
        val updateSequenceNum: Int,

        // Time we last synced
        @ColumnInfo(name = "ls")
        val lastSyncAt: Long,

        // Json object containing configuration options that are synced
        @ColumnInfo(name = "conf")
        val configuration: String,

        // Json array of json objects containing the models (aka Note types)
        val models: String,

        // Json array of json objects containing the decks
        val decks: String,

        // Json array of Json objects containing the deck options
        @ColumnInfo(name = "dconf")
        val deckConfiguration: String,

        // A cache of tags used in the collection
        // This list is displayed in the browser, potentially other places as well
        val tags: String
)

/**
 *  Contains deleted cards, notes, and decks that need to be synced
 */
@Entity
data class Graves(
        // Should be set to -1
        @ColumnInfo(name = "usn")
        val updateSequenceNum: Int,

        @ColumnInfo(name = "oid")
        val originalId: Long,

        // 0 = card, 1 = note, 2 = deck
        val type: Int
)

/**
 * Notes contain the raw information that is formatted into a number of cards,
 * according to the models
 */
@Entity(indices = [
    Index(name = "ix_notes_csum", value = ["csum"]),
    Index(name = "ix_notes_usn", value = ["usn"])
])
data class Notes(
        @PrimaryKey
        // Milliseconds representing when the note was created
        val id: Long,

        // Globally unique id, almost certainly used for syncing
        @ColumnInfo(name = "guid")
        val globallyUID: String,

        @ColumnInfo(name = "mid")
        val modelId: Int,

        @ColumnInfo(name = "mod")
        val modifiedAt: Long,

        // Used to figure out diffs when syncing:
        //      value of -1 indicates changes that need to be pushed to the server
        //      usn < server indicates changes that need to be pulled from the server
        @ColumnInfo(name = "usn")
        val updateSequenceNum: Int,

        // Space-separated string ot tags;
        // includes a space at the beginning and end, for LIKE "% tag %" queries
        val tags: String,

        // The values of the fields in this note, separated by 0x1f (31) character
        @ColumnInfo(name = "flds")
        val fieldIds: String,

        // Sort field; used for quick sorting and duplicate check
        @ColumnInfo(name = "sfld")
        val sortField: String,

        // Field checksum used for duplicate checking
        // Integer representation of first 8 digits of sha1 hash of the first field
        @ColumnInfo(name = "csum")
        val fieldChecksum: Int,

        // Currently unused
        val flags: Int,

        // Currently unused
        val data: String
)

/**
 * Keeps track of the user's review history; It has a row for every completed review
 */
@Entity(indices = [
    Index(name = "ix_revlog_cid", value = ["cid"]),
    Index(name = "ix_revlog_usn", value = ["usn"])
])
data class Revlog(
        // // Milliseconds representing when the review was done
        @PrimaryKey
        val id: Long,

        // cards.id
        @ColumnInfo(name = "cid")
        val cardId: Long,

        // Used to figure out diffs when syncing:
        //      value of -1 indicates changes that need to be pushed to the server
        //      usn < server indicates changes that need to be pulled from the server
        @ColumnInfo(name = "usn")
        val updateSequenceNum: Int,

        // Indicates which button was pressed when scoring recall
        // Review: 1 = wrong, 2 = hard, 3 = ok, 4 = easy
        // Learn/Relearn: 1 = wrong, 2 = ok, 3 = easy
        val ease: Int,

        @ColumnInfo(name = "ivl")
        val interval: Int,

        @ColumnInfo(name = "lastIvl")
        val lastInterval: Int,

        val factor: Int,

        // How many milliseconds your review took, up to 60000 (60s)
        @ColumnInfo(name = "time")
        val reviewTime: Long,

        // 0 = learn, 1 = review, 2 = relearn, 3 = cram
        val type: Int
)


