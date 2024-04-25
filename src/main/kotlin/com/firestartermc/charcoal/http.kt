package com.firestartermc.charcoal

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = false
                ignoreUnknownKeys = true
            },
        )
    }
}

fun fetchLatestRelease() = runBlocking {
    try {
        client.get("https://api.github.com/repos/firestarter/bonfire/releases/latest") {
            header("X-GitHub-Api-Version", "2022-11-28")
        }.body<GitHubRelease>()
    } catch (x: Throwable) {
        x.printStackTrace()
        null
    }
}