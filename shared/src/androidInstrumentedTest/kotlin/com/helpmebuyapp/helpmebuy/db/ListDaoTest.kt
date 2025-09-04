package com.helpmebuyapp.helpmebuy.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ListDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ListDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.listDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testCrud() = runTest {
        val list = GroceryList(
            name = "Test List",
            items = listOf("Milk" to 2, "Eggs" to 1),
            category = "Dairy"
        )
        dao.insert(list)
        val inserted = dao.getAll().first().first()
        assertEquals("Test List", inserted.name)

        val updated = inserted.copy(name = "Updated List")
        dao.update(updated)
        assertEquals("Updated List", dao.getById(inserted.id).first()?.name)

        dao.delete(updated)
        assertEquals(0, dao.getAll().first().size)
    }
}
