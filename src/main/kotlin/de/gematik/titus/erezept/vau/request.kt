package de.gematik.titus.erezept.vau

import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.text.Charsets.UTF_8

/**
 * A L1 inner http request looks like this:
 *
 * POST /Task/$create HTTP/1.1
 * content-type: application/fhir+xml
 * authorization: Bearer eyJhbGciOiJCUDI1NlIxIiwia2lkIjoicHVrX2lkcF9zaWciLCJ0eXAiOiJhdCtKV1QifQ.eyJhdXRoX3RpbWUiOjE2OTMzOTIwNDQsInNjb3BlIjoiZS1yZXplcHQgb3BlbmlkIiwiY2xpZW50X2lkIjoiZ2VtYXRpa1Rlc3RQcyIsImdpdmVuX25hbWUiOm51bGwsImZhbWlseV9uYW1lIjpudWxsLCJkaXNwbGF5X25hbWUiOm51bGwsIm9yZ2FuaXphdGlvbk5hbWUiOiJBcnp0cHJheGlzIERyLiBtZWQuIEfDvG5kw7xsYSBHdW50aGVyIFRFU1QtT05MWSIsInByb2Zlc3Npb25PSUQiOiIxLjIuMjc2LjAuNzYuNC41MCIsImlkTnVtbWVyIjoiMS0yLVJFWkVQVE9SLUFSWlQtMDEiLCJhenAiOiJnZW1hdGlrVGVzdFBzIiwiYWNyIjoiZ2VtYXRpay1laGVhbHRoLWxvYS1oaWdoIiwiYW1yIjpbIm1mYSIsInNjIiwicGluIl0sImF1ZCI6Imh0dHBzOi8vZXJwLXJlZi56ZW50cmFsLmVycC5zcGxpdGRucy50aS1kaWVuc3RlLmRlLyIsInN1YiI6ImM5MTQyMzMyNjhhYWU0NjY3Y2E1MTE5ZGE2YzExMTVkYWNhYWQyMWI1MDgyZTY3NDQ0ZmFmNjBjNjNiYzM3MzQiLCJpc3MiOiJodHRwczovL2lkcC1yZWYuYXBwLnRpLWRpZW5zdGUuZGUiLCJpYXQiOjE2OTMzOTIwNDQsImV4cCI6MTY5MzM5MjM0NCwianRpIjoiYzhhMTU4YjItZDQ2ZC00NDJjLWE0MjMtODFmMWVkOWZkODYxIn0.c6tcPJagbL0hVWGn1YHa6FdsvrXugF4JBEaG4vomYT-FC4o_LqYRAlE-F0_KWtbeU8I78JNm31I182nko806Qg
 * user-agent: PostmanRuntime/7.29.3
 * postman-token: 0dad21d7-bb31-49e4-820d-1a1dd6dd193e
 * host: 127.0.0.1:15081
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
    val headers: Map<String, List<String>>,
    val body: PlainBody,
) {
    override fun toString(): String = """
        |L1VauReqEnvelopeAkaInnerVau:
        |$method $path $httpVersion
        |${headers.entries.joinToString("\n") { (name, values) -> "$name: ${values.joinToString(", ")}" }}
        |nrOfBodyBytes=${body.bytes.size}
    """.trimMargin()

    companion object {
        private val bodySeparator = "\r\n\r\n".toByteArray()

        fun from(bytes: ByteArray): L1VauReqEnvelopeAkaInnerVau {
            val (preamble: String, body: PlainBody) = run {
                val bodySplitIndex = bytes.indexOf(bodySeparator)
                require(bodySplitIndex >= 0) { "Invalid InnerHttpRequest bytes: no body found" }
                val preambleBytes = bytes.copyOf(bodySplitIndex)
                val bodyBytes = bytes.copyOfRange(bodySplitIndex + bodySeparator.size, bytes.size)

                preambleBytes.toString(UTF_8) to PlainBody(bytes = bodyBytes)
            }

            val (firstLine: String, headerLines: Iterator<String>) = run {
                val lines = preamble.split("\r\n").iterator()
                require(lines.hasNext()) { "Invalid InnerHttpRequest preamble: no first line found" }
                val firstLine = lines.next()
                firstLine to lines
            }

            val (method: String, path: String, httpVersion: String) = run {
                val parts = firstLine.split(" ")
                require(parts.size == 3) { """Invalid InnerHttpRequest first line structure! Expected "[method] [path] [httpVersion] but was": $firstLine""" }
                parts
            }

            val headers: Map<String, List<String>> = headerLines.asSequence()
                .map { line ->
                    val (name, values) = line.split(':', limit = 2)
                    name.trim() to values.split(',').map { it.trim() }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { it.value.flatten() }

            return L1VauReqEnvelopeAkaInnerVau(
                method = HttpMethod.parse(method),
                path = path,
                httpVersion = HttpVersion.parse(httpVersion),
                headers = headers,
                body = body,
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
 * host: 127.0.0.1:15081
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
    val version: Int,
    val accessToken: AccessCode,
    val requestId: RequestId,
    val aesKey: AesKey,
    val l1InnerVau: L1VauReqEnvelopeAkaInnerVau,
) {
    init {
        require(version == 1) { "Invalid version: $version, supported version: 1" }
    }

    override fun toString(): String = """
        |L2VauReqEnvelopeAkaInnerVau:
        |$version $accessToken $requestId $aesKey $l1InnerVau
    """.trimMargin()

    companion object {
        fun from(bytes: ByteArray): L2VauReqEnvelopeAkaInnerVau {
            val spaceByte = ' '.code.toByte()
            val space1Pos = bytes.indexOf(spaceByte, 0).assertIndexFound()
            val space2Pos = bytes.indexOf(spaceByte, space1Pos+1).assertIndexFound()
            val space3Pos = bytes.indexOf(spaceByte, space2Pos+1).assertIndexFound()
            val space4Pos = bytes.indexOf(spaceByte, space3Pos+1).assertIndexFound()

            val version = bytes.copyOf(space1Pos).decodeToString().toInt()
            val accessToken = AccessCode(bytes.copyOfRange(space1Pos+1, space2Pos).decodeToString())
            val requestId = RequestId(bytes.copyOfRange(space2Pos+1, space3Pos).decodeToString())
            val aesKey = AesKey(bytes.copyOfRange(space3Pos+1, space4Pos).decodeToString())

            val l1InnerVau = L1VauReqEnvelopeAkaInnerVau.from(bytes.copyOfRange(space4Pos+1, bytes.size))

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
value class L3VauReqEnvelopeAkaEncryptedL2(val bytes: ByteArray) {
    override fun toString(): String = "L3VauReqEnvelopeAkaEncryptedL2(nrOfBytes=${bytes.size})"
}

class L4VauReqEnvelopeAkaOuterVau(
    val version: String,
    val xCoordinate: XCoordinate,
    val yCoordinate: YCoordinate,
    val iV: InitialisationVector,
    val l3EncryptedL2: L3VauReqEnvelopeAkaEncryptedL2,
) {
    init {
        require(version == "01") { "Invalid version: '$version', supported version: '01'" }
    }

    override fun toString(): String = "L4VauReqEnvelopeAkaOuterVau(version=$version, nrOfEncryptingBytes=${l3EncryptedL2.bytes.size})"

    companion object {
        @OptIn(ExperimentalUnsignedTypes::class)
        fun from(bytes: ByteArray): L4VauReqEnvelopeAkaOuterVau {
            val version = bytes.copyOf(2).decodeToString()
            val xCoordinate = bytes.copyOfRange(2, 34)
            val yCoordinate = bytes.copyOfRange(34, 66)
            val iV = bytes.copyOfRange(66, 78)
            val l3_encryptedL2 = L3VauReqEnvelopeAkaEncryptedL2(bytes.copyOfRange(78, bytes.size))

            return L4VauReqEnvelopeAkaOuterVau(
                version = version,
                xCoordinate = XCoordinate(xCoordinate.toUByteArray()),
                yCoordinate = YCoordinate(yCoordinate.toUByteArray()),
                iV = InitialisationVector(iV.toUByteArray()),
                l3EncryptedL2 = l3_encryptedL2,
            )
        }
    }
}

@JvmInline
value class PlainBody(val bytes: ByteArray) {
    override fun toString(): String = bytes.decodeToString()

    companion object {
        fun fromUtf8(value: String): PlainBody = PlainBody(bytes = value.encodeToByteArray())
    }
}

@JvmInline
value class AccessCode(val value: String) {
    init {
        require(value.isNotBlank()) { "accessCode must not be blank" }
    }
}

private val hexRegex = Regex("[0-9a-fA-F]+")
private fun String.assertIsHexEncoded(paramName: String) {
    require(this.matches(hexRegex)) { "param $paramName is not hex encoded: $this" }
}

@JvmInline
value class RequestId(val value: String) {
    init {
        value.assertIsHexEncoded("requestId")
    }
}

@JvmInline
value class AesKey(val value: String) {
    init {
        value.assertIsHexEncoded("aesKey")
        require(value.length == 32) { "Invalid aes key length: ${value.length}, expected 32" }
    }


    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    fun toUByteArray(): UByteArray = value.hexToUByteArray()
}

@JvmInline
@OptIn(ExperimentalUnsignedTypes::class)
value class XCoordinate(val uBytes: UByteArray) {
    init {
        require(uBytes.size == 32) { "Invalid byte array size: ${uBytes.size}, expected 32" }
    }
}

@JvmInline
@OptIn(ExperimentalUnsignedTypes::class)
value class YCoordinate(val uBytes: UByteArray) {
    init {
        require(uBytes.size == 32) { "Invalid byte array size: ${uBytes.size}, expected 32" }
    }
}

@JvmInline
value class InitialisationVector(val uBytes: UByteArray) {
    init {
        require(uBytes.size == 12) { "Invalid byte array size: ${uBytes.size}, expected 12" }
    }
}

fun ByteArray.indexOf(sequence: ByteArray, startFrom: Int = 0): Int {
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

fun ByteArray.indexOf(byte: Byte, startFrom: Int = 0): Int {
    if(startFrom < 0 ) throw IllegalArgumentException("startFrom must be non-negative")
    for(i in startFrom until size) {
        if( this[i] == byte ) return i
    }
    return -1
}

fun Int.assertIndexFound(): Int {
    require(this != -1) { "index not found" }
    return this
}
