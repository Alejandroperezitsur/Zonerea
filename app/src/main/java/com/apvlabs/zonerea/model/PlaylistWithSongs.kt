package com.apvlabs.zonerea.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(PlaylistSongCrossRef::class, parentColumn = "playlistId", entityColumn = "songId")
    )
    val songs: List<Song>
)
