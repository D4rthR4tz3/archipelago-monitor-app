package io.github.d4rthr4tz3.apviewer.protocol.session

import kotlin.test.Test
import kotlin.test.assertEquals

class ServerAddressTest {
    @Test
    fun `splits host and port`() {
        assertEquals(
            ServerAddress("archipelago.gg", "38281"),
            ServerAddress.parse("archipelago.gg:38281"),
        )
        assertEquals(ServerAddress("localhost", ""), ServerAddress.parse("localhost"))
        assertEquals(
            ServerAddress("wss://example.org", "1234"),
            ServerAddress.parse("wss://example.org:1234/"),
        )
        assertEquals(ServerAddress("[::1]", "38281"), ServerAddress.parse("[::1]:38281"))
        assertEquals(ServerAddress("[::1]", ""), ServerAddress.parse("[::1]"))
    }

    @Test
    fun `joins host and port`() {
        assertEquals("archipelago.gg:38281", ServerAddress(" archipelago.gg ", "38281 ").toString())
        assertEquals("localhost", ServerAddress("localhost", "").toString())
    }
}
