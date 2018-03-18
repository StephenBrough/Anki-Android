package com.ichi2.anki.cardbrowser

import com.ichi2.libanki.Utils

fun formatQA(txt: String): String {
    /* Strips all formatting from the string txt for use in displaying question/answer in browser */
    var s = txt.replace("<br>", " ")
    s = s.replace("<br />", " ")
    s = s.replace("<div>", " ")
    s = s.replace("\n", " ")
    s = s.replace("\\[sound:[^]]+\\]".toRegex(), "")
    s = s.replace("\\[\\[type:[^]]+\\]\\]".toRegex(), "")
    s = Utils.stripHTMLMedia(s)
    s = s.trim { it <= ' ' }
    return s
}
