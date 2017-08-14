package com.ichi2.anki.exception

class UnknownHttpResponseException(message: String, private val mCode: Int) : Exception(message) {
    val responseCode: Int
        get() = mCode
}