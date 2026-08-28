package com.arena.marketradar.data.model

/** A single release as returned by the GitHub Releases API. */
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val publishedAt: Long,
    val body: String?,
    val assets: List<GitHubAsset>,
    val versionCode: Int,
) {
    /** Clean semantic version string (e.g. "1.4") derived from the tag. */
    fun versionName(): String = tagName.trimStart('v').trim()
}

data class GitHubAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
)
