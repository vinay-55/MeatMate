package com.example.store.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName= "business")
data class BusinessEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val product: String,

    val price: Double,

    val weight: Double,

    val total: Double,

    val date: String,

    val slno: Int?=null
)
