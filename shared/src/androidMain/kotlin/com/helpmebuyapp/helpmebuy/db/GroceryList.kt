package com.helpmebuyapp.helpmebuy.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.helpmebuyapp.helpmebuy.model.ListEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Entity(tableName = "lists")
data class GroceryList(
    @PrimaryKey(autoGenerate = true) override val id: Int = 0,
    override val name: String,
    override val category: String,
    val items: kotlin.collections.List<kotlin.Pair<String, Int>>
) : ListEntity

class ItemsConverter {
    @TypeConverter
    fun fromItems(items: kotlin.collections.List<kotlin.Pair<String, Int>>): String {
        val serializer = ListSerializer(PairSerializer(String.serializer(), Int.serializer()))
        return Json.encodeToString(serializer, items)
    }

    @TypeConverter
    fun toItems(itemsString: String): kotlin.collections.List<kotlin.Pair<String, Int>> {
        val serializer = ListSerializer(PairSerializer(String.serializer(), Int.serializer()))
        return Json.decodeFromString(serializer, itemsString)
    }
}
