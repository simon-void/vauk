package de.gematik.titus.erezept.vau

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class L1VauReqEnvelopeAkaInnerVauTest {
    @Test
    fun `test parse InnerHttpRequest from bytes`() {
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

        val l1VauReq = L1VauReqEnvelopeAkaInnerVau.from(innerHttpRequestContent.toByteArray(Charsets.UTF_8))

        assertEquals("POST", l1VauReq.method.name)
        assertEquals("/Task/${'$'}create", l1VauReq.path)
        assertEquals("HTTP/1.1", l1VauReq.httpVersion.value)
        l1VauReq.headers.let { headers ->
            assertEquals(8, headers.size, "headers found: ${headers.keys}")
            assertEquals("application/fhir+xml", headers["content-type"]?.singleOrNull())
            assertEquals("PostmanRuntime/7.29.3", headers["user-agent"]?.singleOrNull())
            assertEquals("0dad21d7-bb31-49e4-820d-1a1dd6dd193e", headers["postman-token"]?.singleOrNull())
            assertEquals("127.0.0.1:15081", headers["host"]?.singleOrNull())
            assertEquals("keep-alive", headers["connection"]?.singleOrNull())
            assertEquals("270", headers["content-length"]?.singleOrNull())
            assertEquals("gzip, deflate, br", headers["accept-encoding"]?.joinToString(", "))
            assertEquals("Bearer ey.JhbG.example", headers["authorization"]?.singleOrNull())
        }

        assertEquals(body, l1VauReq.body.toString())
    }
}