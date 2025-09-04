package com.helpmebuyapp.helpmebuy.repository

import com.helpmebuyapp.helpmebuy.db.AppDatabase
import com.helpmebuyapp.helpmebuy.db.GroceryList
import com.helpmebuyapp.helpmebuy.model.ListEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * A repository for Android that writes to Room immediately and tries to keep
 * DynamoDB in sync in the background when network is available.
 *
 * Minimal, best-effort sync strategy:
 * - On insert/update/delete: perform Room operation first (offline-first UX).
 * - Then attempt to mirror operation to DynamoDB. If it fails, we keep going
 *   without crashing. Next explicit sync() will retry.
 * - sync():
 *   - Push: iterate over current local lists and upsert them to DynamoDB.
 *   - Pull: fetch all from DynamoDB and upsert into Room (id-based replace).
 *
 * Conflict resolution: last-write-wins cannot be guaranteed without timestamps,
 * so for minimal changes we choose server-wins on pull phase. Applications that
 * need stronger guarantees should add updatedAt fields to entities and compare.
 */
class AndroidSyncingListRepository(
    private val db: AppDatabase,
    private val dynamo: ListRepository,
    private val scope: CoroutineScope? = null // optional for background fire-and-forget
) : ListRepository {

    private val dao = db.listDao()

    // ---------- CRUD over Room first, then try to propagate ----------
    override suspend fun insert(list: ListEntity) {
        if (list !is GroceryList) throw IllegalArgumentException("Unsupported list type: ${list::class.simpleName}")
        dao.insert(list)
        tryPropagate { dynamo.insert(list) }
    }

    override suspend fun update(list: ListEntity) {
        if (list !is GroceryList) throw IllegalArgumentException("Unsupported list type: ${list::class.simpleName}")
        dao.update(list)
        tryPropagate { dynamo.update(list) }
    }

    override suspend fun delete(list: ListEntity) {
        if (list !is GroceryList) throw IllegalArgumentException("Unsupported list type: ${list::class.simpleName}")
        dao.delete(list)
        tryPropagate { dynamo.delete(list) }
    }

    override fun getById(id: Int): Flow<ListEntity?> = dao.getById(id).map { it as ListEntity? }

    override fun getAll(): Flow<List<ListEntity>> = dao.getAll().map { it as List<ListEntity> }

    override fun getAutoSuggestions(query: String): List<String> = AndroidListRepository(db).getAutoSuggestions(query)

    // ---------- Sync entrypoint ----------
    suspend fun sync() {
        // Push local to DynamoDB (best-effort)
        runCatching {
            val localLists = dao.getAll().first()
            localLists.forEach { list ->
                runCatching { dynamo.update(list) } // upsert
            }
        }
        // Pull remote to local (server-wins for simplicity)
        runCatching {
            val remote = dynamo.getAll().first()
            // Upsert/replace by id
            val current = dao.getAll().first().associateBy { it.id }
            remote.forEach { r ->
                if (r is GroceryList) {
                    if (current.containsKey(r.id)) dao.update(r) else dao.insert(r)
                } else {
                    // If remote entity is DynamoDbListRepository.DdbList, map minimally
                    val gr = GroceryList(id = r.id, name = r.name, category = r.category, items = emptyList())
                    if (current.containsKey(gr.id)) dao.update(gr) else dao.insert(gr)
                }
            }
            // Optionally, delete local items that no longer exist in remote. Skipped for minimal change.
        }
    }

    // Sync a single list explicitly (useful when network is restored)
    suspend fun syncList(list: ListEntity) {
        runCatching { dynamo.update(list) }
    }

    private fun tryPropagate(block: suspend () -> Unit) {
        // If a scope is provided, use it; otherwise, launch a lightweight IO scope.
        val s = scope ?: CoroutineScope(Dispatchers.IO)
        s.launch { runCatching { block() } }
    }
}
