package com.example.store.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BusinessEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao
}