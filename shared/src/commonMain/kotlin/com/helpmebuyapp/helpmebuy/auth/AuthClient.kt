package com.helpmebuyapp.helpmebuy.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.forms.*
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Minimal Keycloak OAuth client for KMP using Ktor.
 * Uses Password grant for development purposes against local Keycloak.
 * Token JSON is parsed with a lightweight regex to avoid adding JSON libs for now.
 */
object KeycloakConfig {
    const val realm = "HelpMeBuyRealm"
    const val base = "http://localhost:8081"
    const val clientId = "HelpMeBuyApp"
    const val clientSecret = "71JRsys77O0XqRtIjlexkAmzMZly1g5S" // Replace with HelpMeBuyApp secret from Keycloak
    val tokenUrl: String get() = "$base/realms/$realm/protocol/openid-connect/token"
    val userInfoUrl: String get() = "$base/realms/$realm/protocol/openid-connect/userinfo"
}

class AuthClient {
    private var accessToken: String? = null
    private val client = HttpClient(CIO) {
        install(Auth) {
            bearer {
                loadTokens {
                    accessToken?.let { BearerTokens(it, "") } // Refresh deferred to HMB-6
                }
            }
        }
    }
    private val logger = KotlinLogging.logger {}

    suspend fun loginWithPassword(
        username: String = "test@helpmebuy.local",
        password: String = "testpass",
        scope: String = "openid"
    ): String {
        val tempClient = HttpClient(CIO)
        try {
            val response: HttpResponse = tempClient.post(KeycloakConfig.tokenUrl) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(Parameters.build {
                        append("grant_type", "password")
                        append("client_id", KeycloakConfig.clientId)
                        append("client_secret", KeycloakConfig.clientSecret)
                        append("username", username)
                        append("password", password)
                        append("scope", scope)
                    })
                )
            }
            val body = response.bodyAsText()
            logger.debug { "Token request: ${KeycloakConfig.tokenUrl}" }
            logger.debug { "Token response: $body" }
            accessToken = Json.parseToJsonElement(body)
                .jsonObject["access_token"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("No access token in response: $body")
            return accessToken!!
        } finally {
            tempClient.close()
        }
    }

    suspend fun getUserInfo(): Map<String, Any> {
        val token = accessToken ?: throw IllegalStateException("Not logged in")
        val response: HttpResponse = client.get(KeycloakConfig.userInfoUrl) {
            headers { append(HttpHeaders.Authorization, "Bearer $token") }
        }
        val body = response.bodyAsText()
        logger.debug { "UserInfo response: $body" }
        val json = Json.parseToJsonElement(body).jsonObject
        return mapOf(
            "roles" to (json["roles"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList<String>()),
            "groups" to (json["groups"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList<String>()),
            "username" to (json["preferred_username"]?.jsonPrimitive?.content ?: ""),
            "sub" to (json["sub"]?.jsonPrimitive?.content ?: "")
        )
    }

    fun close() {
        accessToken = null
        client.close()
    }

    fun currentAccessToken(): String? = accessToken
}
