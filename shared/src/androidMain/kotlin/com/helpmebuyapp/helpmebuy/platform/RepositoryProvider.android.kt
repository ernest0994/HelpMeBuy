package com.helpmebuyapp.helpmebuy.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.helpmebuyapp.helpmebuy.db.AppDatabase
import com.helpmebuyapp.helpmebuy.db.GroceryList
import com.helpmebuyapp.helpmebuy.model.ListEntity
import com.helpmebuyapp.helpmebuy.repository.AndroidListRepository
import com.helpmebuyapp.helpmebuy.repository.ListRepository
import kotlinx.coroutines.flow.Flow

@Composable
actual fun rememberListRepository(): ListRepository {
    val context = LocalContext.current
    return remember {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "hmb.db").build()
        AndroidRepoAdapter(AndroidListRepository(db))
    }
}

private class AndroidRepoAdapter(private val base: AndroidListRepository) : ListRepository {
    override suspend fun insert(list: ListEntity) {
        base.insert(toGrocery(list, forInsert = true))
    }

    override suspend fun update(list: ListEntity) {
        base.update(toGrocery(list, forInsert = false))
    }

    override suspend fun delete(list: ListEntity) {
        base.delete(toGrocery(list, forInsert = false))
    }

    override fun getById(id: Int): Flow<ListEntity?> = base.getById(id)

    override fun getAll(): Flow<List<ListEntity>> = base.getAll()

    override fun getAutoSuggestions(query: String): List<String> = base.getAutoSuggestions(query)

    private fun toGrocery(list: ListEntity, forInsert: Boolean): GroceryList {
        return when (list) {
            is GroceryList -> if (forInsert) list.copy(id = 0) else list
            else -> GroceryList(
                id = if (forInsert) 0 else list.id,
                name = list.name,
                category = list.category.ifBlank { "General" },
                items = emptyList()
            )
        }
    }
}
