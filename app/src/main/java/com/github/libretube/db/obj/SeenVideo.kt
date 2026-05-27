package com.github.libretube.db.obj

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seenVideo")
data class SeenVideo(
    @PrimaryKey
    val videoId: String,
    val seenAt: Long
)
