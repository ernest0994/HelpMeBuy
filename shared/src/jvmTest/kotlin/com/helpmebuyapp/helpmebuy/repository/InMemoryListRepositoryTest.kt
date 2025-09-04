package com.helpmebuyapp.helpmebuy.repository

import com.helpmebuyapp.helpmebuy.model.ListEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private data class TestList(
    override val id: Int,
    override val name: String,
    override val category: String,
) : ListEntity

class InMemoryListRepositoryTest {
    @Test
    fun testCrudFlow() = runTest {
        val repo = InMemoryListRepository()
        val a = TestList(1, "A", "cat")
        val b = TestList(2, "B", "cat")

        repo.insert(a)
        repo.insert(b)
        assertEquals(listOf(a, b).map { it.id }, repo.getAll().first().map { it.id })

        val b2 = b.copy(name = "B2")
        repo.update(b2)
        assertEquals("B2", repo.getById(2).first()!!.name)

        repo.delete(a)
        assertEquals(listOf(2), repo.getAll().first().map { it.id })

        val suggestions = repo.getAutoSuggestions("mi")
        assertEquals(true, suggestions.any { it.contains("Milk", ignoreCase = true) })
    }
}
