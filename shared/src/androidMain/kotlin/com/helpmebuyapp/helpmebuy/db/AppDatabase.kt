package com.helpmebuyapp.helpmebuy.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [GroceryList::class], version = 1, exportSchema = true)
@TypeConverters(ItemsConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
}
