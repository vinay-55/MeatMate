package com.example.store.data.local

import android.content.Context
import androidx.room.Room
import com.example.store.data.local.AppDatabase

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {

        return INSTANCE ?: synchronized(this) {

            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "business_database"
            ).build()

            INSTANCE = instance

            instance
        }
    }
}