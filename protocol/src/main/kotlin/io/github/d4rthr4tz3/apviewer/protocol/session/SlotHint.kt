package io.github.d4rthr4tz3.apviewer.protocol.session

import io.github.d4rthr4tz3.apviewer.protocol.model.HintStatus

/** A hint that concerns a slot, with its ids resolved to names. */
data class SlotHint(
    val receivingSlot: Int,
    val receiverName: String,
    val findingSlot: Int,
    val finderName: String,
    val itemId: Long,
    val itemName: String,
    val itemFlags: Int,
    val locationId: Long,
    val locationName: String,
    /** Entrance that leads to the location in entrance randomizers, empty if not known. */
    val entrance: String,
    val status: HintStatus,
)

/**
 * A hint of a multiworld. [receiverIsOwn] and [finderIsOwn] tell whether one of the user's
 * connected slots receives or finds the item; only the receiver can change the [hint]'s status.
 */
data class MultiworldHint(
    val hint: SlotHint,
    val receiverIsOwn: Boolean,
    val finderIsOwn: Boolean,
) {
    /** Whether the user can change the status: as receiver, while the item is not found. */
    val editable: Boolean get() = receiverIsOwn && hint.status != HintStatus.FOUND
}
