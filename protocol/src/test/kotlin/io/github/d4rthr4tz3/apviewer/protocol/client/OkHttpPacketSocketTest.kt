package io.github.d4rthr4tz3.apviewer.protocol.client

import io.github.d4rthr4tz3.apviewer.protocol.packet.ClientPacket
import io.github.d4rthr4tz3.apviewer.protocol.packet.ServerPacket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class OkHttpPacketSocketTest {
    private val server = MockWebServer()
    private val httpClient = OkHttpClient()

    @AfterTest
    fun tearDown() {
        server.close()
        httpClient.dispatcher.executorService.shutdown()
    }

    @Test
    fun `exchanges packets with a websocket server`() = runBlocking {
        val received = LinkedBlockingQueue<String>()
        server.enqueue(
            MockResponse.Builder()
                .webSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            webSocket.send("""[{"cmd":"PrintJSON","data":[{"text":"hi"}]}]""")
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            received += text
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            webSocket.close(code, null)
                        }
                    },
                )
                .build(),
        )
        server.start()
        val url = server.url("/").toString().replaceFirst("http", "ws")

        val socket = OkHttpPacketSocket.Factory(httpClient).open(url)
        val packet = withTimeout(TIMEOUT_MILLIS) { socket.incoming.first() }
        socket.send(listOf(ClientPacket.Sync))

        assertEquals("hi", assertIs<ServerPacket.PrintJson>(packet).data.single().text)
        assertEquals("""[{"cmd":"Sync"}]""", received.poll(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        socket.close()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
