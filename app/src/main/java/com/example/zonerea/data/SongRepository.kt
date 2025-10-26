package com.example.zonerea.data

import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.zonerea.data.local.SongDao
import com.example.zonerea.model.Playlist
import com.example.zonerea.model.PlaylistSongCrossRef
import com.example.zonerea.model.PlaylistWithSongs
import com.example.zonerea.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SongRepository(private val context: Context, private val songDao: SongDao) {

    val songs = songDao.getAllSongs()
    val playlists = songDao.getPlaylists()

    suspend fun scanForSongs() {
        withContext(Dispatchers.IO) {
            val newSongs = mutableListOf<Song>()
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.MIME_TYPE
            )
            val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0) AND (${MediaStore.Audio.Media.MIME_TYPE} = ? OR ${MediaStore.Audio.Media.MIME_TYPE} = ?)"
            val selectionArgs = arrayOf("audio/mpeg", "audio/mp4") // MP3 and M4A

            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Title"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val albumArtUri = "content://media/external/audio/albumart/$albumId"
                    val uri = "${collection}/$id"

                    val existingSong = songDao.getSongById(id)
                    if (existingSong != null) {
                        // Preserve existing data
                        newSongs.add(existingSong.copy(title = title, artist = artist, album = album, duration = duration, albumArtUri = albumArtUri, dateAdded = dateAdded))
                    } else {
                        newSongs.add(Song(id, uri, title, artist, album, duration, albumArtUri, dateAdded))
                    }
                }
            }
            songDao.insertSongs(newSongs)
        }
    }

    suspend fun setFavorite(songId: Long, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            val song = songDao.getSongById(songId)
            song?.let {
                songDao.updateSong(it.copy(isFavorite = isFavorite))
            }
        }
    }

    suspend fun updatePlayStatistics(songId: Long) {
        withContext(Dispatchers.IO) {
            val song = songDao.getSongById(songId)
            song?.let {
                songDao.updateSong(it.copy(playCount = it.playCount + 1, lastPlayed = System.currentTimeMillis()))
            }
        }
    }

    suspend fun createPlaylist(name: String): Long {
        return withContext(Dispatchers.IO) {
            songDao.insertPlaylist(Playlist(name = name))
        }
    }

    suspend fun addSongToPlaylist(songId: Long, playlistId: Long) {
        withContext(Dispatchers.IO) {
            songDao.addToPlaylist(PlaylistSongCrossRef(playlistId, songId))
        }
    }

    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs> {
        return songDao.getPlaylistWithSongs(playlistId)
    }

    suspend fun deleteSong(song: Song) {
        withContext(Dispatchers.IO) {
            // Delete from Android MediaStore
            try {
                context.contentResolver.delete(song.uri.toUri(), null, null)
            } catch (e: Exception) {
                // Log error or handle case where file might not exist
                e.printStackTrace()
            }
            // Delete from local database
            songDao.deleteSong(song)
        }
    }
}
