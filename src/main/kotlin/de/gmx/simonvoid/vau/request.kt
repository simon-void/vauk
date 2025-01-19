@file:OptIn(ExperimentalUnsignedTypes::class)

package de.gmx.simonvoid.vau

import de.gmx.simonvoid.util.TraceId
import de.gmx.simonvoid.util.encodeBase64
import de.gmx.simonvoid.util.getLogger
import org.apache.commons.codec.binary.Hex

/**
 * A L1 inner http request looks like this:
 *
 * POST /Task/$create HTTP/1.1
 * content-type: application/fhir+xml
 * authorization: Bearer eyJhbGciOiJCUDI1NlIxIiwia2lkIjoicHVrX2lkcF9zaWciLCJ0eXAiOiJhdCtKV1QifQ.eyJhdXRoX3RpbWUiOjE2OTMzOTIwNDQsInNjb3BlIjoiZS1yZXplcHQgb3BlbmlkIiwiY2xpZW50X2lkIjoiZ2VtYXRpa1Rlc3RQcyIsImdpdmVuX25hbWUiOm51bGwsImZhbWlseV9uYW1lIjpudWxsLCJkaXNwbGF5X25hbWUiOm51bGwsIm9yZ2FuaXphdGlvbk5hbWUiOiJBcnp0cHJheGlzIERyLiBtZWQuIEfDvG5kw7xsYSBHdW50aGVyIFRFU1QtT05MWSIsInByb2Zlc3Npb25PSUQiOiIxLjIuMjc2LjAuNzYuNC41MCIsImlkTnVtbWVyIjoiMS0yLVJFWkVQVE9SLUFSWlQtMDEiLCJhenAiOiJnZW1hdGlrVGVzdFBzIiwiYWNyIjoiZ2VtYXRpay1laGVhbHRoLWxvYS1oaWdoIiwiYW1yIjpbIm1mYSIsInNjIiwicGluIl0sImF1ZCI6Imh0dHBzOi8vZXJwLXJlZi56ZW50cmFsLmVycC5zcGxpdGRucy50aS1kaWVuc3RlLmRlLyIsInN1YiI6ImM5MTQyMzMyNjhhYWU0NjY3Y2E1MTE5ZGE2YzExMTVkYWNhYWQyMWI1MDgyZTY3NDQ0ZmFmNjBjNjNiYzM3MzQiLCJpc3MiOiJodHRwczovL2lkcC1yZWYuYXBwLnRpLWRpZW5zdGUuZGUiLCJpYXQiOjE2OTMzOTIwNDQsImV4cCI6MTY5MzM5MjM0NCwianRpIjoiYzhhMTU4YjItZDQ2ZC00NDJjLWE0MjMtODFmMWVkOWZkODYxIn0.c6tcPJagbL0hVWGn1YHa6FdsvrXugF4JBEaG4vomYT-FC4o_LqYRAlE-F0_KWtbeU8I78JNm31I182nko806Qg
 * user-agent: PostmanRuntime/7.29.3
 * postman-token: 0dad21d7-bb31-49e4-820d-1a1dd6dd193e
 * accept-encoding: gzip, deflate, br
 * connection: keep-alive
 * content-length: 270
 *
 * <Parameters xmlns="http://hl7.org/fhir">
 * <parameter>
 * <name value="workflowType"/>
 * <valueCoding>
 * <system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_FlowType"/>
 * <code value="163"/>
 * </valueCoding>
 * </parameter>
 * </Parameters>
 */

class L1VauReqEnvelopeAkaInnerVau(
    val method: HttpMethod,
    val path: String,
    val httpVersion: HttpVersion,
    val headers: VauHeaders,
    val body: BodyBytes,
) {
    private val preambleBytes: ByteArray
        get() = VauMessage.concatenatePreambleBytes("$method $path ${httpVersion.value}", headers)

    fun toByteArray(): ByteArray = preambleBytes + body.bytes

    override fun toString(): String = """
        |L1VauReqEnvelopeAkaInnerVau:
        |${preambleBytes.decodeToString()}bodyAsBase64: ${body.bytes.encodeBase64()}
    """.trimMargin()

    companion object {
        private val log = getLogger<L1VauReqEnvelopeAkaInnerVau>()

        private val lowercaseHeaderNamesToDrop = listOf("host")
        fun filterHeaders(headers: Map<String, List<String>>):Map<String, List<String>> = buildMap(headers.size) {
            headers.forEach { name, values ->
                if (name.lowercase() !in lowercaseHeaderNamesToDrop) {
                    put(name, values)
                }
            }
        }

        fun from(bytes: ByteArray, traceId: TraceId): L1VauReqEnvelopeAkaInnerVau {
            val msg = try {
                VauMessage.from(bytes)
            } catch (e: Throwable) {
                log.error("$traceId failed to parse vau message from decrypted bytes (as base64): " + bytes.encodeBase64())
                throw IllegalArgumentException("couldn't parse vau message. Probably decryption failed.", e)
            }

            val (method: HttpMethod, path: String, httpVersion: HttpVersion) = msg.firstLine.let {
                runCatching {
                    val parts = it.split(" ")
                    require(parts.size == 3) { """Invalid InnerHttpRequest first line structure! Expected "[method] [path] [httpVersion] but was": $it""" }
                    Triple(HttpMethod.parse(parts[0]), parts[1], HttpVersion.parse(parts[2]))
                }.getOrElse { e ->
                    log.error("""$traceId failed to parse "[method] [path] [httpVersion]" from first line of Vau message: "$it" because $e""")
                    log.error("$traceId complete decrypted vau message (as base64): " + bytes.encodeBase64())
                    throw IllegalArgumentException("not valid L1VauReqEnvelope data", e)
                }
            }

            val filteredHeaders = VauHeaders.fromSeparateHeaderValues(
                filterHeaders(msg.headers.asMapWithListOfValues())
            )

            return L1VauReqEnvelopeAkaInnerVau(
                method = method,
                path = path,
                httpVersion = httpVersion,
                headers = filteredHeaders,
                body = msg.body,
            )
        }
    }
}

/**
 * A L2 inner http request looks like this:
 *
 * 1 {accessToken} {requestId} {aesKey} POST /Task/$create HTTP/1.1
 * content-type: application/fhir+xml
 * authorization: Bearer eyJhbGciOiJCUDI1NlIxIiwia2lkIjoicHVrX2lkcF9zaWciLCJ0eXAiOiJhdCtKV1QifQ.eyJhdXRoX3RpbWUiOjE2OTMzOTIwNDQsInNjb3BlIjoiZS1yZXplcHQgb3BlbmlkIiwiY2xpZW50X2lkIjoiZ2VtYXRpa1Rlc3RQcyIsImdpdmVuX25hbWUiOm51bGwsImZhbWlseV9uYW1lIjpudWxsLCJkaXNwbGF5X25hbWUiOm51bGwsIm9yZ2FuaXphdGlvbk5hbWUiOiJBcnp0cHJheGlzIERyLiBtZWQuIEfDvG5kw7xsYSBHdW50aGVyIFRFU1QtT05MWSIsInByb2Zlc3Npb25PSUQiOiIxLjIuMjc2LjAuNzYuNC41MCIsImlkTnVtbWVyIjoiMS0yLVJFWkVQVE9SLUFSWlQtMDEiLCJhenAiOiJnZW1hdGlrVGVzdFBzIiwiYWNyIjoiZ2VtYXRpay1laGVhbHRoLWxvYS1oaWdoIiwiYW1yIjpbIm1mYSIsInNjIiwicGluIl0sImF1ZCI6Imh0dHBzOi8vZXJwLXJlZi56ZW50cmFsLmVycC5zcGxpdGRucy50aS1kaWVuc3RlLmRlLyIsInN1YiI6ImM5MTQyMzMyNjhhYWU0NjY3Y2E1MTE5ZGE2YzExMTVkYWNhYWQyMWI1MDgyZTY3NDQ0ZmFmNjBjNjNiYzM3MzQiLCJpc3MiOiJodHRwczovL2lkcC1yZWYuYXBwLnRpLWRpZW5zdGUuZGUiLCJpYXQiOjE2OTMzOTIwNDQsImV4cCI6MTY5MzM5MjM0NCwianRpIjoiYzhhMTU4YjItZDQ2ZC00NDJjLWE0MjMtODFmMWVkOWZkODYxIn0.c6tcPJagbL0hVWGn1YHa6FdsvrXugF4JBEaG4vomYT-FC4o_LqYRAlE-F0_KWtbeU8I78JNm31I182nko806Qg
 * user-agent: PostmanRuntime/7.29.3
 * postman-token: 0dad21d7-bb31-49e4-820d-1a1dd6dd193e
 * accept-encoding: gzip, deflate, br
 * connection: keep-alive
 * content-length: 270
 *
 * <Parameters xmlns="http://hl7.org/fhir">
 * <parameter>
 * <name value="workflowType"/>
 * <valueCoding>
 * <system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_FlowType"/>
 * <code value="163"/>
 * </valueCoding>
 * </parameter>
 * </Parameters>
 */

class L2VauReqEnvelopeAkaInnerVau(
    val version: Int = 1,
    val accessToken: AccessCode,
    val requestId: RequestId,
    val aesKey: AesKey,
    val l1InnerVau: L1VauReqEnvelopeAkaInnerVau,
) {
    init {
        require(version == 1) { "Invalid version: $version, supported version: 1" }
    }

    private val preambleBytes: ByteArray
        get() =
            "$version ${accessToken.value} ${requestId.hexValue} ${aesKey.hexValue} ".toByteArray()

    fun toByteArray(): ByteArray = preambleBytes + l1InnerVau.toByteArray()

    override fun toString(): String = """
        |L2VauReqEnvelopeAkaInnerVau:
        |${preambleBytes.decodeToString()} $l1InnerVau
    """.trimMargin()

    companion object {
        fun from(bytes: ByteArray, traceId: TraceId): L2VauReqEnvelopeAkaInnerVau {
            val (tokens, restBytes) = bytes.splitOfTokens(4)

            val version = tokens[0].toInt()
            val accessToken = AccessCode(tokens[1])
            val requestId = RequestId(tokens[2])
            val aesKey = AesKey(tokens[3])

            val l1InnerVau = L1VauReqEnvelopeAkaInnerVau.from(restBytes, traceId)

            return L2VauReqEnvelopeAkaInnerVau(
                version = version,
                accessToken = accessToken,
                requestId = requestId,
                aesKey = aesKey,
                l1InnerVau = l1InnerVau,
            )
        }
    }
}

@JvmInline
value class Cipher(val bytes: ByteArray) {
    override fun toString(): String = "Cipher(nrOfBytes=${bytes.size})"

    // initialisation Vector
    val iV: InitialisationVector get() = InitialisationVector(bytes.copyOf(12))
    val taggedCipherText: ByteArray get() = bytes.copyOfRange(12, bytes.size)
}

@ExperimentalUnsignedTypes
class L3VauReqEnvelopeAkaOuterVau(
    val bytes: ByteArray,
) {
    val version: VauVersion = VauVersion(bytes[0])

    init {
        version.assertIsValid()
    }

    val xCoordinate: XCoordinate get() = XCoordinate(bytes.copyOfRange(1, 33))
    val yCoordinate: YCoordinate get() = YCoordinate(bytes.copyOfRange(33, 65))
    val cipher: Cipher
        get() = Cipher(
            bytes.copyOfRange(
                65,
                bytes.size
            )
        )

    override fun toString(): String = "L3VauReqEnvelopeAkaOuterVau(nrBytes=${bytes.size})"
}

@JvmInline
value class AccessCode(val value: String) {
    init {
        require(value.isNotBlank()) { "accessCode must not be blank" }
    }
}

private val hexRegex: Regex = "[0-9a-f]+".toRegex() // by spec only lowercase
private fun String.assertIsHexEncoded(paramName: String) {
    require(this.matches(hexRegex)) { "param $paramName is not lowercase hex encoded: $this" }
}

@JvmInline
value class RequestId(val hexValue: String) {
    init {
        hexValue.assertIsHexEncoded("requestId")
    }
}

@JvmInline
value class AesKey(val hexValue: String) {
    init {
        hexValue.assertIsHexEncoded("aesKey")
        require(hexValue.length == 32) { "Invalid aes key length: ${hexValue.length}, expected 32" }
    }

    fun asBytes(): ByteArray = Hex.decodeHex(hexValue)
}

@JvmInline
value class VauVersion(val value: Byte) {
    fun assertIsValid() {
        require(value.toInt() == 1) {
            "Invalid version: $value, expected 1"
        }
    }

    companion object {
        val V1 = VauVersion(0xb)
    }
}

@ExperimentalUnsignedTypes
@JvmInline
value class XCoordinate(val bytes: ByteArray) {
    init {
        require(bytes.size == 32) { "Invalid byte array size: ${bytes.size}, expected 32" }
    }
}

@ExperimentalUnsignedTypes
@JvmInline
value class YCoordinate(val bytes: ByteArray) {
    init {
        require(bytes.size == 32) { "Invalid byte array size: ${bytes.size}, expected 32" }
    }
}

@ExperimentalUnsignedTypes
@JvmInline
value class InitialisationVector(val bytes: ByteArray) {
    init {
        require(bytes.size == 12) { "Invalid byte array size: ${bytes.size}, expected 12" }
    }
}

enum class HttpMethod {
    GET, POST, DELETE;

    override fun toString(): String = name

    companion object {
        fun parse(method: String): HttpMethod = method.uppercase().let { upMethod ->
            entries.firstOrNull() { it.name == upMethod }
                ?: error("Unknown method: $upMethod (known methods: ${entries.joinToString(", ")})")
        }
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
