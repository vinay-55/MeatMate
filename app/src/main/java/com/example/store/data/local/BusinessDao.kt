package com.example.store.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BusinessDao {

    @Insert
    suspend fun insertBusiness(business: BusinessEntity): Long

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("""
        SELECT * FROM business
        WHERE date = :date
        AND product = :product
    """)
    suspend fun getBusinessByDateAndProduct(
        date: String,
        product: String
    ): List<BusinessEntity>

    @Query("""
        SELECT * FROM business
        WHERE date = :date
    """)
    suspend fun getBusinessByDate(
        date: String
    ): List<BusinessEntity>

    @Query("""
        DELETE FROM business
        WHERE id = :id
    """)
    suspend fun deleteBusiness(id: Int)

    // Return all business records (used for backup/restore)
    @Query("""
        SELECT * FROM business
    """)
    suspend fun getAllBusiness(): List<BusinessEntity>
}