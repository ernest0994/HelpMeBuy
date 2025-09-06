package com.helpmebuyapp.helpmebuy

import androidx.compose.runtime.Composable

@Composable
fun App() {
    // Make Lists the default screen
    com.helpmebuyapp.helpmebuy.ui.ListsScreen(
        repo = com.helpmebuyapp.helpmebuy.platform.rememberListRepository()
    )
}