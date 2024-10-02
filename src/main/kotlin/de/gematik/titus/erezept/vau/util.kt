package de.gematik.titus.erezept.vau

enum class HttpMethod {
    GET, POST, DELETE;

    override fun toString(): String = name

    companion object {
        fun parse(method: String): HttpMethod = method.uppercase().let { method ->
            entries.firstOrNull() { it.name == method } ?: error("Unknown method: $method (known methods: ${entries.joinToString(", ")})")
        }
    }
}

enum class HttpVersion(val value: String) {
    HTTP_1_1("HTTP/1.1");

    override fun toString(): String = name

    companion object {
        fun parse(version: String): HttpVersion = version.uppercase().let { version ->
            HttpVersion.entries.firstOrNull() { it.value == version } ?: error("Unknown http protocol version: $version (known versions: ${HttpMethod.entries.joinToString(", ")})")
        }
    }
}