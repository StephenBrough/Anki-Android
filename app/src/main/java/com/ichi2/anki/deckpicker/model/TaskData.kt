package com.ichi2.anki.deckpicker.model

import android.content.Context
import com.ichi2.libanki.Card
import com.ichi2.libanki.Note
import java.util.Comparator

class TaskData {
    var card: Card? = null
    var note: Note? = null
    var int: Int = 0
    var string: String? = null
    var boolean = false
    var cards: List<Map<String, String>>? = null
    var long: Long = 0
    var context: Context? = null
    var type: Int = 0
    var comparator: Comparator<*>? = null
    var objArray: Array<Any?>? = null


    constructor(obj: Array<Any?>) {
        objArray = obj
    }

    constructor(value: Int, obj: Array<Any?>?, bool: Boolean) {
        objArray = obj
        int = value
        boolean = bool
    }


    constructor(value: Int, card: Card) : this(value) {
        this.card = card
    }


    constructor(value: Int, cardId: Long, bool: Boolean) : this(value) {
        long = cardId
        boolean = bool
    }


    constructor(card: Card) {
        this.card = card
    }


    constructor(card: Card, tags: String) {
        this.card = card
        string = tags
    }


    constructor(card: Card?, integer: Int) {
        this.card = card
        int = integer
    }


    constructor(context: Context, type: Int, period: Int) {
        this.context = context
        this.type = type
        int = period
    }


    constructor(cards: List<Map<String, String>>) {
        this.cards = cards
    }


    constructor(cards: List<Map<String, String>>, comparator: Comparator<*>) {
        this.cards = cards
        this.comparator = comparator
    }


    constructor(bool: Boolean) {
        boolean = bool
    }


    constructor(string: String, bool: Boolean) {
        this.string = string
        boolean = bool
    }


    constructor(value: Long, bool: Boolean) {
        long = value
        boolean = bool
    }


    constructor(value: Int, bool: Boolean) {
        int = value
        boolean = bool
    }


    constructor(card: Card, bool: Boolean) {
        boolean = bool
        this.card = card
    }


    constructor(value: Int) {
        int = value
    }


    constructor(l: Long) {
        long = l
    }


    constructor(msg: String) {
        string = msg
    }


    constructor(note: Note) {
        this.note = note
    }


    constructor(value: Int, msg: String) {
        string = msg
        int = value
    }


    constructor(msg: String, cardId: Long, bool: Boolean) {
        string = msg
        long = cardId
        boolean = bool
    }
}