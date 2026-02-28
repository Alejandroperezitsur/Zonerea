package com.apvlabs.zonerea.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    primaryKeys = ["playlistId", "songId"],
    indices = [
        Index(value = ["songId"]),
        Index(value = ["playlistId"]) // útil para consultas por playlist
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long
)
