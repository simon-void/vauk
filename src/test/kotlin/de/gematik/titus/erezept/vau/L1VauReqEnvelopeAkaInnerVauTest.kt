package de.gematik.titus.erezept.vauproxy.vau

import de.gematik.titus.erezept.vauproxy.util.TraceId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class L1VauReqEnvelopeAkaInnerVauTest {
    @Test
    fun `test parse L1VauReqEnvelopeAkaInnerVau from bytes`() {
        val body = """
             |<Parameters xmlns="http://hl7.org/fhir">
             |    <parameter>
             |        <name value="workflowType"/>
             |        <valueCoding>
             |            <system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_FlowType"/>
             |            <code value="163"/>
             |        </valueCoding>
             |    </parameter>
             |</Parameters>
        """.trimMargin()
        val innerHttpRequestContent = """
             |POST /Task/${'$'}create HTTP/1.1
             |content-type: application/fhir+xml
             |authorization: Bearer ey.JhbG.example
             |user-agent: PostmanRuntime/7.29.3
             |postman-token: 0dad21d7-bb31-49e4-820d-1a1dd6dd193e
             |host: 127.0.0.1:15081
             |accept-encoding: gzip, deflate, br
             |connection: keep-alive
             |content-length: 270
             |
             |
        """.trimMargin().replace("\n", "\r\n") + body

        val l1vauReqEnvelopInnerVau = L1VauReqEnvelopeAkaInnerVau.from(
            innerHttpRequestContent.toByteArray(),
            TraceId.next(),
        )

        assertEquals("POST", l1vauReqEnvelopInnerVau.method.toString())
        assertEquals("/Task/${'$'}create", l1vauReqEnvelopInnerVau.path)
        assertEquals("HTTP/1.1", l1vauReqEnvelopInnerVau.httpVersion.value)
        l1vauReqEnvelopInnerVau.headers.let { headers ->
            val map = headers.asMapWithConcatenatedValues()
            assertEquals(8, headers.size, "headers found: ${map.keys}")
            assertEquals("application/fhir+xml", map["content-type"])
            assertEquals("application/fhir+xml", headers.contentType)
            assertEquals("PostmanRuntime/7.29.3", map["user-agent"])
            assertEquals("0dad21d7-bb31-49e4-820d-1a1dd6dd193e", map["postman-token"])
            assertEquals("127.0.0.1:15081", map["host"])
            assertEquals("keep-alive", map["connection"])
            assertEquals("270", map["content-length"])
            assertEquals(270, headers.contentLength)
            assertEquals("gzip, deflate, br", map["accept-encoding"])
            assertEquals("Bearer ey.JhbG.example", map["authorization"])
        }

        assertEquals(body, l1vauReqEnvelopInnerVau.body.bytes.decodeToString())
    }

    @Test
    fun `test toByteArray`() {
        val path = "/somePath"
        val body = "some Body"
        val l1vauReq = L1VauReqEnvelopeAkaInnerVau(
            method = HttpMethod.POST,
            path = path,
            httpVersion = HttpVersion.HTTP_1_1,
            headers = mapOf(
                "header1" to "value1",
                "header2" to "value2a, value2b"
            ).let { VauHeaders.fromConcatenatedHeaderValues(it) },
            body = BodyBytes(body.toByteArray()),
        )

        assertFalse(l1vauReq.toByteArray().decodeToString().contains("\r\n\r\n\r\n"))

        val (firstLine, headerLines: List<String>, bodyFound) = run {
            val output = l1vauReq.toByteArray().decodeToString()
            val firstLineAndHeaders = output.substringBefore("\r\n\r\n")
            val body = output.substringAfter("\r\n\r\n")

            val lineIter = firstLineAndHeaders.split("\r\n").iterator()

            val firstLine = lineIter.next()
            val headerLines = buildList<String> {
                lineIter.forEach { add(it) }
            }
            Triple(firstLine, headerLines, body)
        }

        assertEquals("POST $path ${HttpVersion.HTTP_1_1.value}", firstLine)
        assertEquals(body, bodyFound)
        assert(headerLines.singleOrNull { it.startsWith("header1") } != null)
        assert(headerLines.singleOrNull { it.startsWith("header2") } != null)
    }
}