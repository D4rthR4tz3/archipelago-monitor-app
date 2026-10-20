package io.github.d4rthr4tz3.apviewer.protocol.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/** Priority of a hint, set by its receiving player. */
enum class HintStatus(val value: Int) {
    UNSPECIFIED(0),
    NO_PRIORITY(10),
    AVOID(20),
    PRIORITY(30),
    FOUND(40),
    ;

    companion object {
        /** Returns the status with [value], or [UNSPECIFIED] for unknown values. */
        fun of(value: Int): HintStatus = entries.firstOrNull { it.value == value } ?: UNSPECIFIED
    }
}

/**
 * A hint as stored by the server: [item] of [receivingPlayer] is placed at [location] in the world
 * of [findingPlayer]. [status] is a [HintStatus] value; servers before 0.6 do not send it.
 */
@Serializable
data class Hint(
    @SerialName("receiving_player") val receivingPlayer: Int,
    @SerialName("finding_player") val findingPlayer: Int,
    val location: Long,
    val item: Long,
    val found: Boolean = false,
    val entrance: String = "",
    @SerialName("item_flags") val itemFlags: Int = 0,
    val status: Int = HintStatus.UNSPECIFIED.value,
) {
    /** The hint's status; [HintStatus.FOUND] once the item was found, whatever [status] says. */
    val hintStatus: HintStatus
        get() = if (found) HintStatus.FOUND else HintStatus.of(status)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        /** Returns the hints stored in a `_read_hints` data storage [value], empty if it has none. */
        fun listFrom(value: JsonElement?): List<Hint> {
            if (value !is JsonArray) return emptyList()
            return try {
                json.decodeFromJsonElement(ListSerializer(serializer()), value)
            } catch (e: SerializationException) {
                emptyList()
            } catch (e: IllegalArgumentException) {
                emptyList()
            }
        }

        /** Returns the data storage key that holds the hints of [slot] in [team]. */
        fun storageKey(team: Int, slot: Int): String = "_read_hints_${team}_$slot"
    }
}
