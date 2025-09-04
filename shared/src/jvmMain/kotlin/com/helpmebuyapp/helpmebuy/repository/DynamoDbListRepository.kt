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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.net.URI

/**
 * JVM implementation backed by DynamoDB (LocalStack by default).
 * Table: "Lists" with HASH key: id (Number)
 */
class DynamoDbListRepository(
    private val tableName: String = System.getenv("HMB_DDB_TABLE") ?: "Lists",
    endpoint: String = System.getenv("AWS_ENDPOINT") ?: "http://localhost:4566",
    region: String = System.getenv("AWS_DEFAULT_REGION") ?: "us-east-1"
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

    private val client: DynamoDbClient = DynamoDbClient.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        // LocalStack accepts any credentials; provide static placeholders
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        .build()

    override suspend fun insert(list: ListEntity): Unit = withContext(Dispatchers.IO) {
        val ddb = toDdbList(list)
        val request = PutItemRequest.builder()
            .tableName(tableName)
            .item(
                mapOf(
                    "id" to AttributeValue.builder().n(ddb.id.toString()).build(),
                    "name" to AttributeValue.builder().s(ddb.name).build(),
                    "category" to AttributeValue.builder().s(ddb.category).build(),
                    // store items as JSON string; optional
                    "itemsJson" to AttributeValue.builder().s(json.encodeToString(itemsSerializer, ddb.items)).build()
                )
            )
            .build()
        client.putItem(request)
        Unit
    }

    override suspend fun update(list: ListEntity): Unit = withContext(Dispatchers.IO) {
        // Implement as a full put (upsert) for simplicity
        insert(list)
        Unit
    }

    override suspend fun delete(list: ListEntity): Unit = withContext(Dispatchers.IO) {
        val key = mapOf("id" to AttributeValue.builder().n(list.id.toString()).build())
        val request = DeleteItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build()
        client.deleteItem(request)
        Unit
    }

    override fun getById(id: Int): Flow<ListEntity?> = flow {
        val key = mapOf("id" to AttributeValue.builder().n(id.toString()).build())
        val req = GetItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build()
        val resp = withContext(Dispatchers.IO) { client.getItem(req) }
        val item = resp.item()
        emit(if (item == null || item.isEmpty()) null else fromItem(item))
    }

    override fun getAll(): Flow<List<ListEntity>> = flow {
        val scanReq = ScanRequest.builder().tableName(tableName).build()
        val resp = withContext(Dispatchers.IO) { client.scan(scanReq) }
        val lists = resp.items().map { fromItem(it) }
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
        val id = item["id"]?.n()?.toInt() ?: 0
        val name = item["name"]?.s() ?: ""
        val category = item["category"]?.s() ?: ""
        val itemsJson = item["itemsJson"]?.s() ?: "[]"
        val items = runCatching { json.decodeFromString(itemsSerializer, itemsJson) }.getOrElse { emptyList() }
        return DdbList(id = id, name = name, category = category, items = items)
    }
}
