package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TvDao {
    // Devices
    @Query("SELECT * FROM tv_devices ORDER BY lastSeenTimestamp DESC")
    fun getAllDevices(): Flow<List<TvDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: TvDeviceEntity)

    @Query("DELETE FROM tv_devices WHERE id = :id")
    suspend fun deleteDevice(id: String)

    // Shared Files
    @Query("SELECT * FROM shared_files ORDER BY timestamp DESC")
    fun getAllSharedFiles(): Flow<List<SharedFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedFile(file: SharedFileEntity)

    @Query("DELETE FROM shared_files WHERE id = :id")
    suspend fun deleteSharedFile(id: String)

    @Query("DELETE FROM shared_files")
    suspend fun clearSharedFiles()

    // Bookmarks
    @Query("SELECT * FROM browser_bookmarks ORDER BY title ASC")
    fun getAllBookmarks(): Flow<List<BrowserBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BrowserBookmarkEntity)

    @Query("DELETE FROM browser_bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)

    // Cast History
    @Query("SELECT * FROM cast_history ORDER BY timestamp DESC LIMIT 20")
    fun getCastHistory(): Flow<List<CastHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCastHistory(history: CastHistoryEntity)
}
