package com.helpmebuyapp.helpmebuy

import com.helpmebuyapp.helpmebuy.auth.AuthClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthClientTest {
    private lateinit var authClient: AuthClient
    private val logger = KotlinLogging.logger {}

    @BeforeTest
    fun setUp() {
        authClient = AuthClient() // Fresh instance per test
    }

    @Test
    fun testLoginAndUserInfo() = runTest {
        val token = authClient.loginWithPassword()
        logger.debug { "Token: $token" }
        assertTrue(token.isNotEmpty(), "JWT token should not be empty")
        val userInfo = authClient.getUserInfo()
        logger.debug { "UserInfo: $userInfo" }
        @Suppress("UNCHECKED_CAST")
        val roles = userInfo["roles"] as List<String>
        @Suppress("UNCHECKED_CAST")
        val groups = userInfo["groups"] as List<String>
        val username = userInfo["username"] as String
        assertTrue(roles.contains("user"), "User role not found")
        assertTrue(groups.contains("List_1_Group"), "List_1_Group not found")
        assertEquals("test@helpmebuy.local", username, "Username mismatch")
    }

    @AfterTest
    fun tearDown() {
        authClient.close()
    }
}