package io.github.d4rthr4tz3.apviewer.protocol.client

import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.PacketCodec
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/** [PacketSocket] backed by an OkHttp [WebSocket]. */
class OkHttpPacketSocket private constructor(private val codec: PacketCodec) :
    WebSocketListener(),
    PacketSocket {
    private val packets = Channel<ServerPacket>(Channel.UNLIMITED)

    @Volatile private var opening: CancellableContinuation<OkHttpPacketSocket>? = null

    // Set by the factory and again in onOpen, which may run before newWebSocket() returns.
    @Volatile private lateinit var webSocket: WebSocket

    override val incoming: Flow<ServerPacket> = packets.receiveAsFlow()

    override fun send(packets: List<ClientPacket>): Boolean = webSocket.send(codec.encode(packets))

    override fun close() {
        webSocket.close(NORMAL_CLOSURE, null)
        packets.close()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket = webSocket
        opening?.resume(this)
        opening = null
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            codec.decode(text).forEach(packets::trySend)
        } catch (e: SerializationException) {
            fail(webSocket, e)
        } catch (e: IllegalArgumentException) {
            fail(webSocket, e)
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        packets.close()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        val pending = opening
        opening = null
        if (pending != null) {
            pending.resumeWithException(t)
        } else {
            packets.close(t)
        }
    }

    private fun fail(webSocket: WebSocket, cause: Exception) {
        webSocket.cancel()
        packets.close(cause)
    }

    /** Opens [OkHttpPacketSocket]s with a shared [OkHttpClient]. */
    class Factory(
        private val httpClient: OkHttpClient,
        private val codec: PacketCodec = PacketCodec(),
    ) : PacketSocketFactory {
        override suspend fun open(url: String): PacketSocket =
            suspendCancellableCoroutine { continuation ->
                val socket = OkHttpPacketSocket(codec)
                socket.opening = continuation
                socket.webSocket =
                    httpClient.newWebSocket(Request.Builder().url(url).build(), socket)
                continuation.invokeOnCancellation { socket.webSocket.cancel() }
            }
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}
