package io.github.d4rthr4tz3.apviewer.protocol.store

import io.github.d4rthr4tz3.apviewer.protocol.message.FeedMessage
import io.github.d4rthr4tz3.apviewer.protocol.model.GameData
import kotlinx.serialization.json.Json

/**
 * JSON encoding of everything the app persists. Unknown keys are ignored and missing keys take
 * their defaults, so data written by an older or newer app version can still be read.
 */
object StoreCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encodeProfiles(profiles: Profiles): String = json.encodeToString(profiles)

    /** Decodes [text]; throws [kotlinx.serialization.SerializationException] if it is invalid. */
    fun decodeProfiles(text: String): Profiles = json.decodeFromString(text)

    fun encodeFeedMessage(message: FeedMessage): String = json.encodeToString(message)

    fun decodeFeedMessage(text: String): FeedMessage = json.decodeFromString(text)

    fun encodeGameData(data: GameData): String = json.encodeToString(data)

    fun decodeGameData(text: String): GameData = json.decodeFromString(text)
}
