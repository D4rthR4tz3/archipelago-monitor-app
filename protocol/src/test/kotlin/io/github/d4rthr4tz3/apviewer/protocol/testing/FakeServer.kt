package io.github.d4rthr4tz3.apviewer.protocol.testing

import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocket
import io.github.d4rthr4tz3.apviewer.protocol.client.PacketSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkItem
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkPlayer
import io.github.d4rthr4tz3.apviewer.protocol.model.NetworkVersion
import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** In-memory [PacketSocket] that a test drives frame by frame. */
class FakeSocket(val url: String = "") : PacketSocket {
    private val frames = Channel<List<ServerPacket>>(Channel.UNLIMITED)
    val sent = mutableListOf<ClientPacket>()
    var closed = false
        private set

    override val incoming: Flow<List<ServerPacket>> = frames.receiveAsFlow()

    /** Delivers [packets] to the client as one frame. */
    fun receive(vararg packets: ServerPacket) {
        frames.trySend(packets.toList())
    }

    /** Closes the connection from the server side. */
    fun drop() {
        frames.close()
    }

    override fun send(packets: List<ClientPacket>): Boolean {
        if (closed) return false
        sent += packets
        return true
    }

    override fun close() {
        closed = true
        frames.close()
    }

    inline fun <reified T : ClientPacket> sentOf(): List<T> = sent.filterIsInstance<T>()
}

/** [PacketSocketFactory] that hands out a new [FakeSocket] for every connection attempt. */
class FakeSocketFactory(private val reachable: (String) -> Boolean = { true }) :
    PacketSocketFactory {
    val opened = mutableListOf<String>()
    val sockets = mutableListOf<FakeSocket>()

    override suspend fun open(url: String): PacketSocket {
        opened += url
        if (!reachable(url)) throw java.io.IOException("unreachable $url")
        return FakeSocket(url).also { sockets += it }
    }

    val last: FakeSocket get() = sockets.last()
}

/** Test data of a room with the slots 1 (Link), 2 (Samus) and 3 (Zelda). */
object TestRoom {
    val roomInfo = ServerPacket.RoomInfo(
        version = NetworkVersion(major = 0, minor = 6, build = 1),
        games = listOf("A Link to the Past"),
        dataPackageChecksums = mapOf("A Link to the Past" to "abc"),
        seedName = "seed",
    )

    val players = listOf(
        NetworkPlayer(team = 0, slot = 1, alias = "Link", name = "Link"),
        NetworkPlayer(team = 0, slot = 2, alias = "Samus", name = "Samus"),
        NetworkPlayer(team = 0, slot = 3, alias = "Zelda", name = "Zelda"),
    )

    fun connected(slot: Int) = ServerPacket.Connected(team = 0, slot = slot, players = players)

    fun item(id: Long, from: Int, flags: Int = 0) =
        NetworkItem(item = id, location = 100 + id, player = from, flags = flags)
}
