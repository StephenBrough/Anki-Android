package com.ichi2.anki.api

/**
 * Definitions of the basic model
 */
internal object BasicModel {
    val FIELDS = arrayOf("Front", "Back")
    // List of card names that will be used in AnkiDroid (one for each direction of learning)
    val CARD_NAMES = arrayOf("Card 1")
    // Template for the question of each card
    val QFMT = arrayOf("{{Front}}")
    val AFMT = arrayOf("{{FrontSide}}\n\n<hr id=answer>\n\n{{Back}}")
}
