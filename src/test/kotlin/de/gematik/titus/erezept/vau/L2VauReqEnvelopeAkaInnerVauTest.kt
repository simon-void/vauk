package de.gematik.titus.erezept.vauproxy.vau

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class L2VauReqEnvelopeAkaInnerVauTest {
    @Test
    fun `test toByteArray`() {
        val path = "/somePath"
        val body = "some Body"
        val l1vauReq = L1VauReqEnvelopeAkaInnerVau(
            method = HttpMethod.POST,
            path = path,
            httpVersion = HttpVersion.HTTP_1_1,
            headers = VauHeaders.fromConcatenatedHeaderValues(emptyMap()),
            body = BodyBytes(body.toByteArray()),
        )

        val jwt = "accessCode123"
        val requestIdHex = "0123456789abcdef4123"
        val aesKeyHex = "0123456789abcdef0123456789abcdef"
        val l2VauReq = L2VauReqEnvelopeAkaInnerVau(
            accessToken = AccessCode(jwt),
            requestId = RequestId(requestIdHex),
            aesKey = AesKey(aesKeyHex),
            l1InnerVau = l1vauReq,
        )

        val output = l2VauReq.toByteArray().decodeToString()
        val firstLine = output.substringBefore("\r\n\r\n")
        val bodyFound = output.substringAfter("\r\n\r\n")

        assertEquals("1 $jwt $requestIdHex $aesKeyHex POST $path ${HttpVersion.HTTP_1_1.value}", firstLine)
        assertEquals(body, bodyFound)
    }
}