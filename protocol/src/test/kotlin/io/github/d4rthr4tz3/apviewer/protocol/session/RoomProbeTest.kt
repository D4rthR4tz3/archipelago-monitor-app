package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.testing.FakeSocketFactory
import io.github.d4rthr4tz3.apviewer.protocol.testing.TestRoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RoomProbeTest {
    @Test
    fun `reports the first url that answers with RoomInfo`() = runTest {
        val factory = FakeSocketFactory(reachable = { it.startsWith("ws://") })
        val result = async { RoomProbe(factory).probe("localhost") }
        runCurrent()
        factory.last.receive(TestRoom.roomInfo)
        runCurrent()

        val reachable = assertIs<ProbeResult.Reachable>(result.await())
        assertEquals("ws://localhost:38281", reachable.url)
        assertEquals("0.6.1", reachable.serverVersion)
        assertEquals(false, reachable.secure)
        assertTrue(factory.last.closed)
    }

    @Test
    fun `reports unreachable when no url can be opened`() = runTest {
        val factory = FakeSocketFactory(reachable = { false })

        val result = RoomProbe(factory).probe("localhost")

        assertIs<ProbeResult.Unreachable>(result)
    }

    @Test
    fun `gives up after the timeout`() = runTest {
        val factory = FakeSocketFactory()

        val result = RoomProbe(factory, timeoutMillis = 1_000).probe("localhost")

        assertIs<ProbeResult.Unreachable>(result)
        assertEquals(2, factory.opened.size)
    }
}
