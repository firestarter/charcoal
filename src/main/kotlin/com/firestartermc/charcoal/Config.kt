package com.firestartermc.charcoal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
}

@Serializable
data class Config(
    val packVersion: String,
    val serverAddress: String,
    val overrideTitleMenu: Boolean = true
)
