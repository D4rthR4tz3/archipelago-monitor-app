package io.github.d4rthr4tz3.apviewer.protocol.client

import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import kotlinx.coroutines.flow.Flow

/** An open connection to an Archipelago server. */
interface PacketSocket {
    /**
     * Packets received from the server. Completes when the server closes the connection and
     * fails when the connection breaks. Can be collected only once.
     */
    val incoming: Flow<ServerPacket>

    /** Queues [packets] for sending. Returns `false` if the connection is already closed. */
    fun send(packets: List<ClientPacket>): Boolean

    /** Closes the connection. */
    fun close()
}

/** Opens a [PacketSocket] to a WebSocket URL such as `wss://archipelago.gg:38281`. */
fun interface PacketSocketFactory {
    suspend fun open(url: String): PacketSocket
}
