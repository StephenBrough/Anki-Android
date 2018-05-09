package com.ichi2.libanki.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.ichi2.libanki.db.entities.Card

@Dao
interface CardDao {

    @Query("SELECT * FROM cards")
    fun getCards(): List<Card>
}