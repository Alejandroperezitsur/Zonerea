package com.apvlabs.zonerea.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.apvlabs.zonerea.model.Playlist
import com.apvlabs.zonerea.model.PlaylistSongCrossRef
import com.apvlabs.zonerea.model.PlaylistWithSongs
import com.apvlabs.zonerea.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    // Song methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): Song?

    // Playlist methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToPlaylist(playlistSongCrossRef: PlaylistSongCrossRef)

    @Delete
    suspend fun removeFromPlaylist(playlistSongCrossRef: PlaylistSongCrossRef)

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getPlaylists(): Flow<List<Playlist>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?>
}
