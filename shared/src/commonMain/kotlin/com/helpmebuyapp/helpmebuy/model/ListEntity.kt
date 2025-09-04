package com.helpmebuyapp.helpmebuy.model

/**
 * Platform-agnostic minimal contract for a shopping list entity.
 * Note: Platform-specific concrete entities may include additional fields like
 * `items: List<Pair<String, Int>>` for Room/DynamoDB persistence.
 */
interface ListEntity {
    val id: Int
    val name: String
    val category: String
}
