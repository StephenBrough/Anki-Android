package com.ichi2.libanki.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.ichi2.libanki.db.entities.Note

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes")
    fun getAllNotes(): List<Note>
}