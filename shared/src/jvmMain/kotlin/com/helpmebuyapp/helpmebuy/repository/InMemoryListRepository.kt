package com.helpmebuyapp.helpmebuy.repository

import com.helpmebuyapp.helpmebuy.model.ListEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryListRepository : ListRepository {
    private val state = MutableStateFlow<List<ListEntity>>(emptyList())

    override suspend fun insert(list: ListEntity) {
        state.value = state.value + list
    }

    override suspend fun update(list: ListEntity) {
        state.value = state.value.map { if (it.id == list.id) list else it }
    }

    override suspend fun delete(list: ListEntity) {
        state.value = state.value.filterNot { it.id == list.id }
    }

    override fun getById(id: Int): Flow<ListEntity?> = state.map { lists -> lists.find { it.id == id } }

    override fun getAll(): Flow<List<ListEntity>> = state

    override fun getAutoSuggestions(query: String): List<String> {
        val commonItems = listOf("Milk", "Eggs", "Bread", "Apples", "Chicken")
        return commonItems.filter { it.lowercase().contains(query.lowercase()) }
    }
}
