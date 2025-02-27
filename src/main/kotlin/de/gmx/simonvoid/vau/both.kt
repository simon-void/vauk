package de.gmx.simonvoid.vau

import kotlin.text.Charsets.UTF_8

// level 1 support classes

internal data class VauMessage(
    val firstLine: String,
    val headers: VauHeaders,
    val body: BodyBytes,
) {
    companion object {
        private const val BODY_SEPARATOR = "\r\n\r\n"
        private const val LINE_SEPARATOR = "\r\n"

        fun from(bytes: ByteArray): VauMessage {
            val (preamble: String, body: BodyBytes) = run {
                val bodySplitIndex = bytes.indexOf(BODY_SEPARATOR.toByteArray())
                if(bodySplitIndex==-1) {
                    bytes.decodeToString() to BodyBytes.EMPTY
                } else {
                    val preambleBytes = bytes.copyOf(bodySplitIndex)
                    val bodyBytes = bytes.copyOfRange(bodySplitIndex + BODY_SEPARATOR.length, bytes.size)

                    preambleBytes.toString(UTF_8) to BodyBytes(bytes = bodyBytes)
                }
            }

            val (firstLine: String, headerLines: Iterator<String>) = run {
                val lines = preamble.split(LINE_SEPARATOR).iterator()
                require(lines.hasNext()) { "Invalid InnerHttpRequest preamble: no first line found" }
                val firstLine = lines.next()
                require(!firstLine.contains('\n')) {
                    "the first line of a Vau message is not allowed to contain a \\n but was: $firstLine"
                }
                firstLine to lines
            }

            val headers: VauHeaders = headerLines.asSequence()
                .map { line ->
                    val colonIndex = line.indexOf(':')
                    require(colonIndex != -1) { "header expected, but no ':' separator is present in: $line" }
                    line.substring(0, colonIndex).trim() to line.substring(colonIndex + 1).trim()
                }.toMap().let { VauHeaders.fromConcatenatedHeaderValues(it) }

            return VauMessage(firstLine, headers, body)
        }

        fun concatenatePreambleBytes(firstLine: String, headers: VauHeaders): ByteArray = buildString {
            append(firstLine)
            headers.asMapWithConcatenatedValues().entries.forEach { (name, values) -> append("$LINE_SEPARATOR$name: $values") }
            append(BODY_SEPARATOR)
        }.toByteArray()
    }
}

enum class HttpVersion(val value: String) {
    HTTP_1_1("HTTP/1.1");

    override fun toString(): String = name

    companion object {
        fun parse(version: String): HttpVersion = version.uppercase().let { upVersion ->
            HttpVersion.entries.firstOrNull() { it.value == upVersion } ?: error(
                "Unknown http protocol version: $upVersion (known versions: ${
                    HttpVersion.entries.joinToString(
                        ", "
                    )
                })"
            )
        }
    }
}

internal fun ByteArray.splitOfTokens(nrOfTokens: Int): Pair<List<String>, ByteArray> {
    require(nrOfTokens>0) {"nrOfTokens must be >=1 but was: $nrOfTokens"}
    val bytes = this
    val spaceByte = ' '.code.toByte()

    val spacePositions: List<Int> = buildList {
        var startIndex = 0
        (0..<nrOfTokens).forEach {
            val nextIndex = bytes.indexOf(spaceByte, startIndex).assertIndexFound()
            add(nextIndex)
            startIndex = nextIndex + 1
        }
    }

    val tokens: List<String> = buildList {
        add(bytes.copyOf(spacePositions.first()).decodeToString())

        val positionIter = spacePositions.iterator()
        var startPos = positionIter.next() + 1
        while (positionIter.hasNext()) {
            val endPos = positionIter.next()
            add(bytes.copyOfRange(startPos, endPos).decodeToString())
            startPos = endPos + 1
        }
    }

    return tokens to bytes.copyOfRange(spacePositions.last()+1, bytes.size)
}

@JvmInline
value class BodyBytes(val bytes: ByteArray) {
    override fun toString(): String = "BodyBytes(nrOfBytes=${bytes.size})"

    companion object {
        val EMPTY = BodyBytes(ByteArray(0))
    }
}

@JvmInline
value class VauHeaders private constructor(private val map: Map<String, List<String>>) {
    val contentType: String?
        get() = map.entries.firstOrNull { (key, _) ->
            "content-type".equals(
                key,
                ignoreCase = true
            )
        }?.value?.first{it.startsWith("application/") || it.startsWith("text/")}?.trim()
    val jwt: String?
        get() = map.entries.singleOrNull { (key, _) ->
            "authorization".equals(
                key,
                ignoreCase = true
            )
        }?.value?.singleOrNull()?.trim()?.let {
            if(it.startsWith("Bearer ")) {
                it.substring(7)
            } else null
        }
    val contentLength: Int?
        get() = map.entries.firstOrNull { (key, _) ->
            "content-length".equals(
                key,
                ignoreCase = true
            )
        }?.value?.singleOrNull()?.trim()?.toIntOrNull()

    val size: Int get() = map.size

    fun asMapWithListOfValues(): Map<String, List<String>> = map
    fun asMapWithConcatenatedValues(splitStrategy: HeaderValueConcatenationStrategy = defaultValueConcatenationStrategy): Map<String, String> =
        map.mapValues { (key, values) ->
            splitStrategy.concatenateValues(key, values)
        }

    override fun toString(): String = map.toString()

    companion object {
        fun fromConcatenatedHeaderValues(
            map: Map<String, String>,
        ): VauHeaders = VauHeaders(
            map.mapValues { (_, possiblyConcatenatedValues) -> listOf(possiblyConcatenatedValues) }
        )

        fun fromSeparateHeaderValues(map: Map<String, List<String>>): VauHeaders = VauHeaders(map)

        // joining header values can be non-trivial, because (only) Set-Cookie Header uses ',' in values
        val defaultValueConcatenationStrategy = HeaderValueConcatenationStrategy { _, values ->
            values.joinToString(separator = ", ")
        }
    }

    fun interface HeaderValueConcatenationStrategy {
        fun concatenateValues(key: String, values: List<String>): String
    }
}

internal fun Int.assertIndexFound(): Int {
    require(this != -1) { "index not found" }
    return this
}

private fun ByteArray.indexOf(sequence: ByteArray, startFrom: Int = 0): Int {
    if(sequence.isEmpty()) throw IllegalArgumentException("non-empty byte sequence is required")
    if(startFrom < 0 ) throw IllegalArgumentException("startFrom must be non-negative")
    var matchOffset = 0
    var start = startFrom
    var offset = startFrom
    while( offset < size ) {
        if( this[offset] == sequence[matchOffset]) {
            if( matchOffset++ == 0 ) start = offset
            if( matchOffset == sequence.size ) return start
        }
        else
            matchOffset = 0
        offset++
    }
    return -1
}

private fun ByteArray.indexOf(byte: Byte, startFrom: Int = 0): Int {
    if(startFrom < 0 ) throw IllegalArgumentException("startFrom must be non-negative")
    for(i in startFrom until size) {
        if( this[i] == byte ) return i
    }
    return -1
}

// level 2 support classes

private val hexRegex: Regex = "[0-9a-f]+".toRegex() // by spec only lowercase is allowed
internal fun String.assertIsHexEncoded(paramName: String) {
    require(this.matches(hexRegex)) { "param $paramName is not lowercase hex encoded: $this" }
}

@JvmInline
value class RequestId(val hexValue: String) {
    init {
        hexValue.assertIsHexEncoded("requestId")
    }
}
