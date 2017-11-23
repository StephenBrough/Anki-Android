package com.ichi2.anki.deckpicker.model

import com.ichi2.libanki.Collection

data class ExportTaskData (
        var col: Collection,
        var apkgPath: String,
        var deckId: Long?,
        var includeSched: Boolean,
        var includeMedia: Boolean
)