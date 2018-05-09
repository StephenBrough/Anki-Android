package com.ichi2.libanki.importer

import com.ichi2.libanki.Collection

/**
 * This class is a stub. Nothing is implemented yet.
 */
open class NoteImporter(col: Collection, file: String) : Importer(col, file) {

    public override var total: Int
        get() = total
        set(value: Int) {
            super.total = value
        }

    override fun runImport() {

    }
}
