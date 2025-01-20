package de.gmx.simonvoid.vau

import de.gmx.simonvoid.util.TraceId
import de.gmx.simonvoid.util.encodeBase64
import de.gmx.simonvoid.util.getLogger


/*
 * A Level 1 Vau Response Envelope looks like this:
 *
 * {transport protocol} {status code} {status text}
 * HTTP/1.1 201 Created
 * Content-Type: application/fhir+xml;charset=utf-8
 * Content-Length: 1286
 *
 * <?xml version="1.0" encoding="utf-8"?>
 * <Task xmlns="http://hl7.org/fhir"><id value="160.000.226.640.861.41"/><meta><profile value="https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Task|1.2"/></meta><extension url="https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_EX_PrescriptionType"><valueCoding><system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_FlowType"/><code value="160"/><display value="Muster 16 (Apothekenpflichtige Arzneimittel)"/></valueCoding></extension><identifier><use value="official"/><system value="https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_PrescriptionId"/><value value="160.000.226.640.861.41"/></identifier><identifier><use value="official"/><system value="https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_AccessCode"/><value value="6d3aff43cb7f1db78e78dde37e6ae8db6f94e6c4291eb2b7bc8876a645ee3475"/></identifier><status value="draft"/><intent value="order"/><authoredOn value="2024-09-26T13:35:54.741+00:00"/><lastModified value="2024-09-26T13:35:54.741+00:00"/><performerType><coding><system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_OrganizationType"/><code value="urn:oid:1.2.276.0.76.4.54"/><display value="Öffentliche Apotheke"/></coding><text value="Öffentliche Apotheke"/></performerType></Task>
 */
class L1VauResEnvelope(
    val httpVersion: HttpVersion,
    val statusCode: StatusCode,
    val headers: VauHeaders,
    val body: BodyBytes,
) {
    private val preambleBytes: ByteArray
        get() = VauMessage.concatenatePreambleBytes("${httpVersion.value} ${statusCode.code} ${statusCode.text}", headers)

    fun toByteArray(): ByteArray = preambleBytes + body.bytes

    override fun toString(): String = """
        |L1VauResEnvelope:
        |${preambleBytes.decodeToString()}${body.bytes.encodeBase64()}
    """.trimMargin()

    companion object {
        private val log = getLogger<L1VauResEnvelope>()

        fun from(bytes: ByteArray, traceId: TraceId): L1VauResEnvelope {
            val msg = try {
                VauMessage.from(bytes)
            } catch (e: Throwable) {
                log.error("$traceId failed to parse vau message from decrypted bytes (as base64): " + bytes.encodeBase64())
                throw IllegalArgumentException("couldn't parse vau message. Probably decryption failed.", e)
            }

            val (httpVersion: HttpVersion, statusCode: StatusCode) = msg.firstLine.let {
                runCatching {
                    val parts = it.split(" ", limit = 3)
                    require(parts.size == 3) { """Invalid InnerHttpRequest first line structure! Expected "[httpVersion] [statusCode] [statusText]" but was": "$it""" }
                    HttpVersion.parse(parts[0]) to StatusCode(parts[1].toInt(), parts[2])
                }.getOrElse { e ->
                    log.error("""$traceId failed to parse "[httpVersion] [statusCode] [statusText]" from first line of Vau message: "$it" because $e""")
                    log.error("$traceId complete decrypted vau message (as base64): " + bytes.encodeBase64())
                    throw IllegalArgumentException("not valid L1VauResEnvelope data", e)
                }
            }

            return L1VauResEnvelope(
                httpVersion = httpVersion,
                statusCode = statusCode,
                headers = msg.headers,
                body = msg.body,
            )
        }
    }
}

data class StatusCode(val code: Int, val text: String)


/*
 * A Level 2 Vau Response Envelope looks like this:
 *
 * {vau_version} {requestId} {l1_vau_response}
 * 1 0123456789abcdef0123456789abcdef HTTP/1.1 201 Created
 * Content-Type: application/fhir+xml;charset=utf-8
 * Content-Length: 1286
 *
 * <?xml version="1.0" encoding="utf-8"?>
 * <Task xmlns="http://hl7.org/fhir"><id value="160.000.226.640.861.41"/><meta><profile value="https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Task|1.2"/></meta><extension url="https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_EX_PrescriptionType"><valueCoding><system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_FlowType"/><code value="160"/><display value="Muster 16 (Apothekenpflichtige Arzneimittel)"/></valueCoding></extension><identifier><use value="official"/><system value="https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_PrescriptionId"/><value value="160.000.226.640.861.41"/></identifier><identifier><use value="official"/><system value="https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_AccessCode"/><value value="6d3aff43cb7f1db78e78dde37e6ae8db6f94e6c4291eb2b7bc8876a645ee3475"/></identifier><status value="draft"/><intent value="order"/><authoredOn value="2024-09-26T13:35:54.741+00:00"/><lastModified value="2024-09-26T13:35:54.741+00:00"/><performerType><coding><system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_OrganizationType"/><code value="urn:oid:1.2.276.0.76.4.54"/><display value="Öffentliche Apotheke"/></coding><text value="Öffentliche Apotheke"/></performerType></Task>
 */
class L2VauResEnvelope(
    val version: Int = 1,
    val requestId: RequestId,
    val l1VauRes: L1VauResEnvelope,
) {
    fun toByteArray(): ByteArray = "$version ${requestId.hexValue} ".encodeToByteArray() + l1VauRes.toByteArray()

    override fun toString(): String = """
        |L2VauResEnvelope:
        |${toByteArray().decodeToString()}
    """.trimMargin()

    companion object {
        fun from(bytes: ByteArray, traceId: TraceId): L2VauResEnvelope {
            val (tokens, restBytes) = bytes.splitOfTokens(2)

            val version = tokens[0].toInt()
            val requestId = RequestId(tokens[1])

            val l1VauRes = L1VauResEnvelope.from(restBytes, traceId)

            return L2VauResEnvelope(
                version = version,
                requestId = requestId,
                l1VauRes = l1VauRes,
            )
        }
    }
}

@JvmInline
value class L3VauResEnvelopeAkaEncryptedL2(val bytes: ByteArray) {
    override fun toString(): String = "L3VauResEnvelopeAkaEncryptedL2(nrOfBytes=${bytes.size})"
}
