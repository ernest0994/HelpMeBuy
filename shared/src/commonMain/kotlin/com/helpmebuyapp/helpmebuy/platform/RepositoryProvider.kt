package com.helpmebuyapp.helpmebuy.platform

import androidx.compose.runtime.Composable
import com.helpmebuyapp.helpmebuy.repository.ListRepository

/**
 * Platform-specific provider for the list repository used by the UI.
 */
@Composable
expect fun rememberListRepository(): ListRepository
