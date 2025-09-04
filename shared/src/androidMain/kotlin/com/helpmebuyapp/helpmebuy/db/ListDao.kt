package com.helpmebuyapp.helpmebuy.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Insert
    suspend fun insert(list: GroceryList)

    @Update
    suspend fun update(list: GroceryList)

    @Delete
    suspend fun delete(list: GroceryList)

    @Query("SELECT * FROM lists WHERE id = :id")
    fun getById(id: Int): Flow<GroceryList?>

    @Query("SELECT * FROM lists")
    fun getAll(): Flow<kotlin.collections.List<GroceryList>>
}
