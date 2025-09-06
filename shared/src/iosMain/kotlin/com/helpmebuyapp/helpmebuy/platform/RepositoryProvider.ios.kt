package com.helpmebuyapp.helpmebuy.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.helpmebuyapp.helpmebuy.repository.InMemoryListRepository
import com.helpmebuyapp.helpmebuy.repository.ListRepository

@Composable
actual fun rememberListRepository(): ListRepository {
    return remember { InMemoryListRepository() }
}
