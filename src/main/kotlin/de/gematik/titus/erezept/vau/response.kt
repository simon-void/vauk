package de.gematik.titus.erezept.vau

/*
 * A Vau Response Envelope looks like this:
 *
 * 1 0123456789abcdef0123456789abcdef HTTP/1.1 201 Created
 * Content-Type: application/fhir+xml;charset=utf-8
 * Content-Length: 1286
 * <?xml version="1.0" encoding="utf-8"?>
 * <Task xmlns="http://hl7.org/fhir"><id value="160.000.226.640.861.41"/><meta><profile value="https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Task|1.2"/></meta><extension url="https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_EX_PrescriptionType"><valueCoding><system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_FlowType"/><code value="160"/><display value="Muster 16 (Apothekenpflichtige Arzneimittel)"/></valueCoding></extension><identifier><use value="official"/><system value="https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_PrescriptionId"/><value value="160.000.226.640.861.41"/></identifier><identifier><use value="official"/><system value="https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_AccessCode"/><value value="6d3aff43cb7f1db78e78dde37e6ae8db6f94e6c4291eb2b7bc8876a645ee3475"/></identifier><status value="draft"/><intent value="order"/><authoredOn value="2024-09-26T13:35:54.741+00:00"/><lastModified value="2024-09-26T13:35:54.741+00:00"/><performerType><coding><system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_OrganizationType"/><code value="urn:oid:1.2.276.0.76.4.54"/><display value="Öffentliche Apotheke"/></coding><text value="Öffentliche Apotheke"/></performerType></Task>
 */
class VauResEnvelope(
    val version: Int,
    val requestId: RequestId,
    val httpVersion: HttpVersion,
    val statusCode: StatusCode,
    val contentType: String,
    val body: EncryptedBody,
) {
    fun toByteArray(): ByteArray = """
        |$version $requestId $httpVersion ${statusCode.code} ${statusCode.text}
        |Content-Type: $contentType
        |Content-Length: ${body.bytes.size}
        |
    """.trimMargin().replace("\n", "\r\n").encodeToByteArray() + body.bytes

    override fun toString(): String = """
        |L1VauResEnvelope:
        |${toByteArray().decodeToString()}
    """.trimMargin()
}

data class StatusCode(val code: Int, val text: String)

@JvmInline
value class EncryptedBody(val bytes: ByteArray) {
    override fun toString(): String = "EncryptedBody(nrOfBytes=${bytes.size})"
}