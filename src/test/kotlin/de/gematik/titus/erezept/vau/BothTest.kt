package de.gematik.titus.erezept.vauproxy.vau

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BothTest {
    @Test
    fun `test splitOfTokens from ByteArray`() {
        val bytes = "5 bkSD alskdf kla3223 a;ldkfj  asdf;\r\nlakf<{ ".toByteArray()
        val (tokens, restOfBytes) = bytes.splitOfTokens(4)
        assertEquals(4, tokens.size, "nr of tokens")

        assertEquals("5", tokens[0])
        assertEquals("bkSD", tokens[1])
        assertEquals("alskdf", tokens[2])
        assertEquals("kla3223", tokens[3])

        assertEquals("a;ldkfj  asdf;\r\nlakf<{ ", restOfBytes.decodeToString())
    }

    @Test
    fun `VauMessage from vau request`() {
        val preamble = """
             |POST /Task/${'$'}create HTTP/1.1
             |content-type: application/fhir+xml
             |authorization: Bearer eyJhbGciOiJCUDI1NlIxIiwia2lkIjoicHVrX2lkcF9zaWciLCJ0eXAiOiJhdCtKV1QifQ.eyJhdXRoX3RpbWUiOjE2OTMzOTIwNDQsInNjb3BlIjoiZS1yZXplcHQgb3BlbmlkIiwiY2xpZW50X2lkIjoiZ2VtYXRpa1Rlc3RQcyIsImdpdmVuX25hbWUiOm51bGwsImZhbWlseV9uYW1lIjpudWxsLCJkaXNwbGF5X25hbWUiOm51bGwsIm9yZ2FuaXphdGlvbk5hbWUiOiJBcnp0cHJheGlzIERyLiBtZWQuIEfDvG5kw7xsYSBHdW50aGVyIFRFU1QtT05MWSIsInByb2Zlc3Npb25PSUQiOiIxLjIuMjc2LjAuNzYuNC41MCIsImlkTnVtbWVyIjoiMS0yLVJFWkVQVE9SLUFSWlQtMDEiLCJhenAiOiJnZW1hdGlrVGVzdFBzIiwiYWNyIjoiZ2VtYXRpay1laGVhbHRoLWxvYS1oaWdoIiwiYW1yIjpbIm1mYSIsInNjIiwicGluIl0sImF1ZCI6Imh0dHBzOi8vZXJwLXJlZi56ZW50cmFsLmVycC5zcGxpdGRucy50aS1kaWVuc3RlLmRlLyIsInN1YiI6ImM5MTQyMzMyNjhhYWU0NjY3Y2E1MTE5ZGE2YzExMTVkYWNhYWQyMWI1MDgyZTY3NDQ0ZmFmNjBjNjNiYzM3MzQiLCJpc3MiOiJodHRwczovL2lkcC1yZWYuYXBwLnRpLWRpZW5zdGUuZGUiLCJpYXQiOjE2OTMzOTIwNDQsImV4cCI6MTY5MzM5MjM0NCwianRpIjoiYzhhMTU4YjItZDQ2ZC00NDJjLWE0MjMtODFmMWVkOWZkODYxIn0.c6tcPJagbL0hVWGn1YHa6FdsvrXugF4JBEaG4vomYT-FC4o_LqYRAlE-F0_KWtbeU8I78JNm31I182nko806Qg
             |user-agent: PostmanRuntime/7.29.3
             |postman-token: 0dad21d7-bb31-49e4-820d-1a1dd6dd193e
             |host: 127.0.0.1:15081
             |accept-encoding: gzip, deflate, br 
             |connection : keep-alive 
             |content-length: 270
             |
             |
             """.trimMargin().replace("\n", "\r\n")
        val body = """
             |<Parameters xmlns="http://hl7.org/fhir">
             |<parameter>
             |<name value="workflowType"/>
             |<valueCoding>
             |<system value="https://gematik.de/fhir/erp/CodeSystem/GEM_ERP_CS_FlowType"/>
             |<code value="163"/>
             |</valueCoding>
             |</parameter>
             |</Parameters>
        """.trimMargin()
        val bodyBytes = body.toByteArray()

        assert(preamble.endsWith("270\r\n\r\n")) {"""preamble should end with "270\r\n\r\n""""}

        val msg = VauMessage.from(preamble.toByteArray() + bodyBytes)

        assertEquals("POST /Task/${'$'}create HTTP/1.1", msg.firstLine, "first line")

        with(msg.headers) {
            assertEquals(8, this.size, "nr of headers")
            assertEquals("application/fhir+xml", this.contentType)
            assertEquals(270, this.contentLength)
            val map = asMapWithListOfValues()
            assertEquals(listOf("127.0.0.1:15081"), map["host"])
            assertEquals(listOf("keep-alive"), map["connection"])
            assertEquals(listOf("gzip", "deflate", "br"), map["accept-encoding"])
        }

        assertEquals(body, msg.body.bytes.decodeToString())
    }
}