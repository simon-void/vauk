package de.gematik.titus.erezept.vauproxy.vau

import de.gematik.titus.erezept.vau.AesKey
import org.junit.jupiter.api.Test
import kotlin.test.fail

class AesKeyTest {
    @Test
    fun `test hexValue too short fails`() {
        val hex = "00 ff 00 00 00 00 00 00 00 00 f0 10 0f 0b 02".replace(" ", "")
        try {
            AesKey(hex)
            fail("hex value ist too short and should have failed")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `test hexValue too long fails`() {
        val hex = "00 ff 00 00 00 00 00 00 00 00 f0 10 0f 0b 02 01 00".replace(" ", "")
        try {
            AesKey(hex)
            fail("hex value ist too long and should have failed")
        } catch (_: IllegalArgumentException) {
        }
    }



    @Test
    fun `test invalid hexValue  fails`() {
        val hex = "00 XX 00 00 00 00 00 00 00 00 F0 10 0F 0B 02 01".replace(" ", "")
        try {
            AesKey(hex)
            fail("only lowercase hexValue is ok")
        } catch (_: IllegalArgumentException) {
        }
    }
}