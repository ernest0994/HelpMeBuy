package com.helpmebuyapp.helpmebuy.repository

import com.helpmebuyapp.helpmebuy.model.ListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.DeleteItemRequest
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import aws.smithy.kotlin.runtime.net.url.Url

/**
 * JVM implementation backed by DynamoDB (LocalStack by default).
 * Table: "Lists" with HASH key: id (Number)
 */
class DynamoDbListRepository(
    private val tableName: String = System.getenv("HMB_DDB_TABLE") ?: "Lists",
    private val endpoint: String = System.getenv("AWS_ENDPOINT") ?: "http://localhost:4566",
    private val region: String = System.getenv("AWS_DEFAULT_REGION") ?: "us-east-1"
) : ListRepository {

    // Concrete data class that also keeps items; interface exposes minimal fields
    data class DdbList(
        override val id: Int,
        override val name: String,
        override val category: String,
        val items: List<Pair<String, Int>> = emptyList()
    ) : ListEntity

    private val json = Json
    private val itemsSerializer = ListSerializer(PairSerializer(String.serializer(), Int.serializer()))

    private val client: DynamoDbClient = DynamoDbClient {
        region = this@DynamoDbListRepository.region
        endpointUrl = Url.parse(endpoint)
    }

    override suspend fun insert(list: ListEntity): Unit = withContext(Dispatchers.IO) {
        val ddb = toDdbList(list)
        val req = PutItemRequest {
            this.tableName = tableName
            this.item = mapOf(
                "id" to AttributeValue.N(ddb.id.toString()),
                "name" to AttributeValue.S(ddb.name),
                "category" to AttributeValue.S(ddb.category),
                "itemsJson" to AttributeValue.S(json.encodeToString(itemsSerializer, ddb.items))
            )
        }
        client.putItem(req)
        Unit
    }

    override suspend fun update(list: ListEntity): Unit = withContext(Dispatchers.IO) {
        // Implement as a full put (upsert) for simplicity
        insert(list)
        Unit
    }

    override suspend fun delete(list: ListEntity): Unit = withContext(Dispatchers.IO) {
        val req = DeleteItemRequest {
            this.tableName = tableName
            this.key = mapOf("id" to AttributeValue.N(list.id.toString()))
        }
        client.deleteItem(req)
        Unit
    }

    override fun getById(id: Int): Flow<ListEntity?> = flow {
        val resp = withContext(Dispatchers.IO) {
            val req = GetItemRequest {
                this.tableName = tableName
                this.key = mapOf("id" to AttributeValue.N(id.toString()))
            }
            client.getItem(req)
        }
        val item = resp.item
        emit(if (item == null || item.isEmpty()) null else fromItem(item))
    }

    override fun getAll(): Flow<List<ListEntity>> = flow {
        val resp = withContext(Dispatchers.IO) { client.scan(ScanRequest { this.tableName = tableName }) }
        val lists = resp.items?.map { fromItem(it) } ?: emptyList()
        emit(lists)
    }

    override fun getAutoSuggestions(query: String): List<String> {
        // Very simple suggestion source; could be improved later
        val commonItems = listOf("Milk", "Eggs", "Bread", "Apples", "Chicken")
        return commonItems.filter { it.lowercase().contains(query.lowercase()) }
    }

    private fun toDdbList(list: ListEntity): DdbList {
        return when (list) {
            is DdbList -> list
            else -> DdbList(id = list.id, name = list.name, category = list.category, items = emptyList())
        }
    }

    private fun fromItem(item: Map<String, AttributeValue>): DdbList {
        val id = (item["id"] as? AttributeValue.N)?.value?.toInt() ?: 0
        val name = (item["name"] as? AttributeValue.S)?.value ?: ""
        val category = (item["category"] as? AttributeValue.S)?.value ?: ""
        val itemsJson = (item["itemsJson"] as? AttributeValue.S)?.value ?: "[]"
        val items = runCatching { json.decodeFromString(itemsSerializer, itemsJson) }.getOrElse { emptyList() }
        return DdbList(id = id, name = name, category = category, items = items)
    }
}
