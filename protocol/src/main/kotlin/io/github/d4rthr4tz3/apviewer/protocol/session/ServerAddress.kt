package io.github.d4rthr4tz3.apviewer.protocol.session

/** A server address split into host and port, as the user edits it. */
data class ServerAddress(val host: String, val port: String) {
    /** Joins host and port to an address for [SlotConnection.candidateUrls]. */
    override fun toString(): String {
        val trimmedHost = host.trim()
        val trimmedPort = port.trim()
        return if (trimmedPort.isEmpty()) trimmedHost else "$trimmedHost:$trimmedPort"
    }

    companion object {
        /** Splits [address] at its last colon if a numeric port follows; IPv6 brackets stay. */
        fun parse(address: String): ServerAddress {
            val trimmed = address.trim().removeSuffix("/")
            val colon = trimmed.lastIndexOf(':')
            val port = if (colon >= 0) trimmed.substring(colon + 1) else ""
            return if (port.isNotEmpty() && port.all(Char::isDigit) && ']' !in port) {
                ServerAddress(trimmed.substring(0, colon), port)
            } else {
                ServerAddress(trimmed, "")
            }
        }
    }
}
