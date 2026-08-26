package com.example.data.local

import com.example.data.model.BrowserBookmark
import com.example.data.model.CastPayload
import com.example.data.model.FileCategory
import com.example.data.model.MediaType
import com.example.data.model.SharedFile
import com.example.data.model.TransferStatus
import com.example.data.model.TvDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TvRepository(private val tvDao: TvDao) {

    val allDevices: Flow<List<TvDevice>> = tvDao.getAllDevices().map { list ->
        list.map { it.toModel() }
    }

    val allSharedFiles: Flow<List<SharedFile>> = tvDao.getAllSharedFiles().map { list ->
        list.map { it.toModel() }
    }

    val allBookmarks: Flow<List<BrowserBookmark>> = tvDao.getAllBookmarks().map { list ->
        list.map { it.toModel() }
    }

    val castHistory: Flow<List<CastPayload>> = tvDao.getCastHistory().map { list ->
        list.map { it.toModel() }
    }

    suspend fun saveDevice(device: TvDevice) {
        tvDao.insertDevice(device.toEntity())
    }

    suspend fun removeDevice(id: String) {
        tvDao.deleteDevice(id)
    }

    suspend fun saveSharedFile(file: SharedFile) {
        tvDao.insertSharedFile(file.toEntity())
    }

    suspend fun deleteSharedFile(id: String) {
        tvDao.deleteSharedFile(id)
    }

    suspend fun clearSharedFiles() {
        tvDao.clearSharedFiles()
    }

    suspend fun saveBookmark(bookmark: BrowserBookmark) {
        tvDao.insertBookmark(bookmark.toEntity())
    }

    suspend fun deleteBookmark(id: String) {
        tvDao.deleteBookmark(id)
    }

    suspend fun saveCastHistory(cast: CastPayload) {
        tvDao.insertCastHistory(cast.toHistoryEntity())
    }

    // Mapping extensions
    private fun TvDeviceEntity.toModel(): TvDevice = TvDevice(
        id = id,
        name = name,
        ipAddress = ipAddress,
        port = port,
        model = model,
        isPaired = isPaired,
        lastSeenTimestamp = lastSeenTimestamp,
        pin = pin,
        signalStrength = signalStrength
    )

    private fun TvDevice.toEntity(): TvDeviceEntity = TvDeviceEntity(
        id = id,
        name = name,
        ipAddress = ipAddress,
        port = port,
        model = model,
        isPaired = isPaired,
        lastSeenTimestamp = lastSeenTimestamp,
        pin = pin,
        signalStrength = signalStrength
    )

    private fun SharedFileEntity.toModel(): SharedFile = SharedFile(
        id = id,
        fileName = fileName,
        fileSize = fileSize,
        formattedSize = formattedSize,
        category = try { FileCategory.valueOf(category) } catch (e: Exception) { FileCategory.OTHER },
        localUriOrPath = localUriOrPath,
        downloadUrl = downloadUrl,
        progress = 1.0f,
        speedMbps = 0.0,
        isIncoming = isIncoming,
        status = try { TransferStatus.valueOf(status) } catch (e: Exception) { TransferStatus.COMPLETED },
        timestamp = timestamp
    )

    private fun SharedFile.toEntity(): SharedFileEntity = SharedFileEntity(
        id = id,
        fileName = fileName,
        fileSize = fileSize,
        formattedSize = formattedSize,
        category = category.name,
        localUriOrPath = localUriOrPath,
        downloadUrl = downloadUrl,
        isIncoming = isIncoming,
        status = status.name,
        timestamp = timestamp
    )

    private fun BrowserBookmarkEntity.toModel(): BrowserBookmark = BrowserBookmark(
        id = id,
        title = title,
        url = url,
        faviconLetter = faviconLetter,
        category = category
    )

    private fun BrowserBookmark.toEntity(): BrowserBookmarkEntity = BrowserBookmarkEntity(
        id = id,
        title = title,
        url = url,
        faviconLetter = faviconLetter,
        category = category
    )

    private fun CastHistoryEntity.toModel(): CastPayload = CastPayload(
        id = id,
        title = title,
        subtitle = subtitle,
        mediaType = try { MediaType.valueOf(mediaType) } catch (e: Exception) { MediaType.VIDEO },
        streamUrl = streamUrl,
        durationMs = durationMs
    )

    private fun CastPayload.toHistoryEntity(): CastHistoryEntity = CastHistoryEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        mediaType = mediaType.name,
        streamUrl = streamUrl,
        durationMs = durationMs
    )
}
