package com.leshoraa.kore.core.ble

import com.leshoraa.kore.domain.model.NotificationEvent
import java.nio.ByteBuffer
import java.util.zip.CRC32

/**
 * Handles binary packing of notification payloads and MTU-based fragmentation.
 */
object BlePacketFraming {
    private const val HEADER: Byte = 0xAA.toByte()

    /**
     * Packs a NotificationEvent into a raw binary frame.
     * Frame Format: [Header][Flags][AppIDLen][AppID][TitleLen][Title][BodyLen][Body][CRC32]
     */
    fun pack(event: NotificationEvent): ByteArray {
        val appBytes = event.packageName.toByteArray()
        val titleBytes = event.title.toByteArray()
        val bodyBytes = event.text.toByteArray()

        // 1 (Header) + 1 (Flags) + 1 (AppLen) + AppBytes + 1 (TitleLen) + TitleBytes + 2 (BodyLen) + BodyBytes + 4 (CRC32)
        val totalSize = 1 + 1 + 1 + appBytes.size + 1 + titleBytes.size + 2 + bodyBytes.size + 4
        val buffer = ByteBuffer.allocate(totalSize)

        buffer.put(HEADER)
        buffer.put(if (event.isGroupSummary) 0x01 else 0x00) // Flags
        
        buffer.put(appBytes.size.toByte())
        buffer.put(appBytes)
        
        buffer.put(titleBytes.size.toByte())
        buffer.put(titleBytes)
        
        buffer.putShort(bodyBytes.size.toShort())
        buffer.put(bodyBytes)

        // Calculate CRC32 for integrity (excluding the CRC field itself)
        val crc = CRC32()
        crc.update(buffer.array(), 0, buffer.position())
        buffer.putInt(crc.value.toInt())

        return buffer.array()
    }

    /**
     * Splits a large binary frame into indexed chunks based on the negotiated MTU.
     * Chunk format: [Index (1B)][Total (1B)][Payload]
     */
    fun fragment(frame: ByteArray, mtu: Int): List<ByteArray> {
        val payloadPerChunk = mtu - 2 // 2 bytes overhead for index and total
        val totalChunks = Math.ceil(frame.size.toDouble() / payloadPerChunk).toInt()
        val chunks = mutableListOf<ByteArray>()

        for (i in 0 until totalChunks) {
            val start = i * payloadPerChunk
            val end = Math.min(frame.size, start + payloadPerChunk)
            val chunkPayload = frame.copyOfRange(start, end)
            
            val chunk = ByteArray(2 + chunkPayload.size)
            chunk[0] = i.toByte()
            chunk[1] = totalChunks.toByte()
            System.arraycopy(chunkPayload, 0, chunk, 2, chunkPayload.size)
            chunks.add(chunk)
        }

        return chunks
    }
}
