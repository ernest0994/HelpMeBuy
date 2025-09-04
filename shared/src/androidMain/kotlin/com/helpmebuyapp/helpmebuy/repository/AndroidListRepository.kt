package com.helpmebuyapp.helpmebuy.repository

import com.helpmebuyapp.helpmebuy.db.AppDatabase
import com.helpmebuyapp.helpmebuy.db.GroceryList
import com.helpmebuyapp.helpmebuy.model.ListEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AndroidListRepository(private val db: AppDatabase) : ListRepository {
    private val dao = db.listDao()

    override suspend fun insert(list: ListEntity) {
        if (list is GroceryList) dao.insert(list)
        else throw IllegalArgumentException("Unsupported list type: ${'$'}{list::class.simpleName}")
    }

    override suspend fun update(list: ListEntity) {
        if (list is GroceryList) dao.update(list)
        else throw IllegalArgumentException("Unsupported list type: ${'$'}{list::class.simpleName}")
    }

    override suspend fun delete(list: ListEntity) {
        if (list is GroceryList) dao.delete(list)
        else throw IllegalArgumentException("Unsupported list type: ${'$'}{list::class.simpleName}")
    }

    override fun getById(id: Int): Flow<ListEntity?> = dao.getById(id).map { it as ListEntity? }

    override fun getAll(): Flow<List<ListEntity>> = dao.getAll().map { it as List<ListEntity> }

    override fun getAutoSuggestions(query: String): List<String> {
        val commonItems = listOf("Milk", "Eggs", "Bread", "Apples", "Chicken")
        return commonItems.filter { it.lowercase().contains(query.lowercase()) }
    }
}
