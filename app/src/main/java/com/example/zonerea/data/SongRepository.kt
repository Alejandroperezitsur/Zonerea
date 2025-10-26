package com.example.zonerea.data

import android.content.Context
import android.provider.MediaStore
import com.example.zonerea.data.local.SongDao
import com.example.zonerea.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongRepository(private val context: Context, private val songDao: SongDao) {

    val songs = songDao.getAllSongs()

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
                MediaStore.Audio.Media.DATE_ADDED
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
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
                    val title = cursor.getString(titleColumn)
                    val artist = cursor.getString(artistColumn)
                    val album = cursor.getString(albumColumn)
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
}