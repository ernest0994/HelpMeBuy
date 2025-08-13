package com.helpmebuyapp.helpmebuy

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform