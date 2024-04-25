package com.firestartermc.charcoal

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val version: String,

    @SerialName("published_at")
    val releasedAt: Instant,

    @SerialName("tarball_url")
    val tarball: String,
)
