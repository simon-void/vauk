package de.gematik.titus.erezept.vauproxy.vau

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class L1VauResEnvelopeTest {
    @Test
    fun `test parse L1VauReqEnvelopeAkaInnerVau from bytes`() {
        val body = """
            |<?xml version="1.0" encoding="utf-8"?>
            |<Task xmlns="http://hl7.org/fhir"><id value="160.000.226.785.959.83"/><meta><profile value="https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_PR_Task|1.3"/></meta><extension url="https://gematik.de/fhir/erp/StructureDefinition/GEM_ERP_EX_PrescriptionType"><valueCoding><system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_FlowType"/><code value="160"/><display value="Muster 16 (Apothekenpflichtige Arzneimittel)"/></valueCoding></extension><identifier><use value="official"/><system value="https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_PrescriptionId"/><value value="160.000.226.785.959.83"/></identifier><identifier><use value="official"/><system value="https://gematik.de/fhir/erp/NamingSystem/GEM_ERP_NS_AccessCode"/><value value="822a9f44569cdf3a5dc3d2e83fe285f03badf346324a49f1d38fb6863fcca7eb"/></identifier><status value="draft"/><intent value="order"/><authoredOn value="2024-11-11T09:37:47.964+00:00"/><lastModified value="2024-11-11T09:37:47.964+00:00"/><performerType><coding><system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_OrganizationType"/><code value="urn:oid:1.2.276.0.76.4.54"/><display value="Öffentliche Apotheke"/></coding><text value="Öffentliche Apotheke"/></performerType></Task>
        """.trimMargin().toByteArray()
        val version = HttpVersion.HTTP_1_1
        val status = StatusCode(201, "Created")

        val headers: Map<String, String> = buildMap {
            put("Content-Type", "application/xml")
            put("Content-Length", body.size.toString())
        }

        val l1vauRes = L1VauResEnvelope(
            httpVersion = version,
            statusCode = status,
            headers = VauHeaders.fromConcatenatedHeaderValues(headers),
            body = BodyBytes(body),
        )

        val l1vauResString = l1vauRes.toByteArray().decodeToString()

        assertEquals("HTTP/1.1 201 Created", l1vauResString.substringBefore("\r\n"))
        assertTrue(l1vauResString.contains("Content-Type: application/xml"), "response should contain Content-Type header")
        assertTrue(l1vauResString.contains("Content-Length: ${body.size}"), "response should contain Content-Length header")
        assertEquals(body.decodeToString(), l1vauResString.substringAfter("\r\n\r\n", missingDelimiterValue = "body not found"), "body")
    }
}