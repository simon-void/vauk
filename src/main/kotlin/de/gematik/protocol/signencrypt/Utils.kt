/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.gematik.protocol.signencrypt

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import javax.crypto.KeyAgreement


@Suppress("FunctionName")
object Utils {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun ECKA(prk: PrivateKey, puk: PublicKey): ByteArray = KeyAgreement.getInstance("ECDH", "BC").apply {
        init(prk)
        doPhase(puk, true)
    }.generateSecret()

    fun HKDF(ikm: ByteArray, info: String, length: Int): ByteArray = HKDF(ikm, info.toByteArray(), length)

    fun HKDF(ikm: ByteArray, info: ByteArray, length: Int): ByteArray = ByteArray(length / 8).apply {
        val hkdf = HKDFBytesGenerator(SHA256Digest()).apply {
            init(HKDFParameters(ikm, null, info))
        }
        hkdf.generateBytes(this, 0, this.size)
    }
}
