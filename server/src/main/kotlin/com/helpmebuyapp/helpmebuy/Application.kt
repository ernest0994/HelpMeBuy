package com.helpmebuyapp.helpmebuy

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.*
import java.net.URL

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val jwkProvider = JwkProviderBuilder(URL("http://localhost:8081/realms/HelpMeBuyRealm/protocol/openid-connect/certs")).build()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "HelpMeBuyRealm"
            verifier(jwkProvider, "http://localhost:8081/realms/HelpMeBuyRealm") {
                acceptLeeway(3)
            }
            validate { credential ->
                if (credential.payload.getClaim("roles").asList(String::class.java).contains("user")) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid or missing token")
            }
        }
    }

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        get("/api/health") {
            call.respondText("OK")
        }
        authenticate("auth-jwt") {
            get("/api/protected") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal?.payload?.getClaim("preferred_username")?.asString()
                val roles = principal?.payload?.getClaim("roles")?.asList(String::class.java)
                val groups = principal?.payload?.getClaim("groups")?.asList(String::class.java)
                call.respondText("Protected: Hello, $username! Roles: $roles, Groups: $groups")
            }
        }
    }
}