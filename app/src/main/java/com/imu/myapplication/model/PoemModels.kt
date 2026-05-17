package com.imu.myapplication.model

import kotlinx.serialization.Serializable

@Serializable
data class Poem(
    val id: Int = 0,
    val title: String,
    val author: String,
    val verse: String,
    val meanings: Map<String, Meaning> = emptyMap(),
    val bhavartha: String,
    val aiInsight: String? = null
)

@Serializable
data class Meaning(
    val definition: String,
    val pronunciation: String
)
