package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Result of [RoomProbe.probe]. */
sealed interface ProbeResult {
    /** The server answered with `RoomInfo` at [url]. */
    data class Reachable(
        val url: String,
        val serverVersion: String,
        val passwordRequired: Boolean,
    ) : ProbeResult {
        val secure: Boolean get() = url.startsWith("wss://")
    }

    data class Unreachable(val message: String?) : ProbeResult
}

/** Checks whether an Archipelago server answers, without connecting a slot. */
class RoomProbe(
    private val socketFactory: PacketSocketFactory,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    /** Tries the candidate URLs of [address] in order and returns the first that answers. */
    suspend fun probe(address: String): ProbeResult {
        var lastMessage: String? = null
        for (url in SlotConnection.candidateUrls(address)) {
            val socket = try {
                socketFactory.open(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastMessage = e.message
                continue
            }
            try {
                val roomInfo = withTimeoutOrNull(timeoutMillis) {
                    socket.incoming.first { frame -> frame.any { it is ServerPacket.RoomInfo } }
                        .filterIsInstance<ServerPacket.RoomInfo>()
                        .first()
                }
                if (roomInfo != null) {
                    return ProbeResult.Reachable(
                        url,
                        roomInfo.version.toString(),
                        roomInfo.password,
                    )
                }
                lastMessage = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastMessage = e.message
            } finally {
                socket.close()
            }
        }
        return ProbeResult.Unreachable(lastMessage)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 10_000L
    }
}
