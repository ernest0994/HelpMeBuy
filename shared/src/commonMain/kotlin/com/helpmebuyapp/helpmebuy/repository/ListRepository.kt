package com.helpmebuyapp.helpmebuy.repository

import com.helpmebuyapp.helpmebuy.model.ListEntity
import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic repository contract. Platform modules can provide implementations.
 */
interface ListRepository {
    suspend fun insert(list: ListEntity)
    suspend fun update(list: ListEntity)
    suspend fun delete(list: ListEntity)
    fun getById(id: Int): Flow<ListEntity?>
    fun getAll(): Flow<List<ListEntity>>
    fun getAutoSuggestions(query: String): List<String>
}