/*
 * Copyright (c) 2023 gematik GmbH
 * 
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the Licence);
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 *     https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * 
 */

package de.gematik.ti.erp.app.vau

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.jce.ECNamedCurveTable
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Provider
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Configuration enabling a custom security provider and secure random.
 * e.g.:
 * object: VauCryptoConfig {
 *     override val provider = BouncyCastleProvider(),
 *     override val random = SecureRandom(),
 * }
 */
interface VauCryptoConfig {
    val provider: Provider
    val random: SecureRandom
}

/**
 * Refer to gemSpec_Krypt A_20161.
 */
class VauEciesSpec(
    val version: Byte,
    val info: ByteArray,
    /**
     * IV size in bytes.
     */
    val ivSize: BinarySize,
    /**
     * Symmetrical key size in bytes.
     */
    val aesSize: BinarySize
) {
    companion object {
        @JvmField
        val V1 = VauEciesSpec(
            version = 0x01.toByte(),
            info = "ecies-vau-transport".toByteArray(),
            ivSize = BinarySize(nrOfBytes = 12),
            aesSize = BinarySize(nrOfBytes = 16),
        )
    }
}

/**
 * Refer to gemSpec_Krypt `A_20161-01`
 */
object Ecies {
    fun encrypt(
        otherECPublicKey: ECPublicKey,
        spec: VauEciesSpec,
        plaintext: ByteArray,
        cryptoConfig: VauCryptoConfig,
    ): ByteArray {
        val ivBytes = ByteArray(spec.ivSize.nrOfBytes).apply {
            cryptoConfig.random.nextBytes(this)
        }
        val ivSpec = IvParameterSpec(ivBytes)

        val eKp = KeyPairGenerator.getInstance("EC", cryptoConfig.provider)
            .apply { initialize(ECGenParameterSpec("brainpoolP256r1"), cryptoConfig.random) }
            .generateKeyPair()

        val cipher =
            generateCipher(ivSpec, eKp, otherECPublicKey, spec, Cipher.ENCRYPT_MODE, cryptoConfig)
        return encrypt(spec, plaintext, ivSpec, eKp.public as ECPublicKey, cipher)
    }

    internal fun encrypt(
        spec: VauEciesSpec,
        plaintext: ByteArray,
        ivSpec: IvParameterSpec,
        ourPublicKey: ECPublicKey,
        cipher: Cipher
    ): ByteArray {
        val ciphertext = cipher.doFinal(plaintext)

        require(ciphertext.size - 16 == plaintext.size) { "ECIES encryption failed!" }

        val x = ourPublicKey.w.affineX.toByteArray()
        val y = ourPublicKey.w.affineY.toByteArray()

        return ByteArray(1 + 32 * 2 + spec.ivSize.nrOfBytes + ciphertext.size).apply {
            // due to two's-complement representation, x & y may contain leading zeros resulting
            // in a byte array of 33 elements;
            // therefore we copy them in reverse order to ignore the first byte in this case
            y.copyInto(this, 1 + 32 + 32 - y.size)
            x.copyInto(this, 1 + 32 - x.size)
            set(0, spec.version)

            ivSpec.iv.copyInto(this, 1 + 32 + 32)
            ciphertext.copyInto(this, 1 + 32 + 32 + spec.ivSize.nrOfBytes)
        }
    }

    fun decrypt(
        ourECKeyPair: KeyPair,
        spec: VauEciesSpec,
        ciphertext: ByteArray,
        cryptoConfig: VauCryptoConfig,
    ): ByteArray =
        ciphertext.let {
            require(it[0] == spec.version) { "Invalid version byte: ${it[0]} != ${spec.version}" }
            require(it.size > (1 + 32 * 2 + spec.ivSize.nrOfBytes)) { "Ciphertext too small!" }

            val x = BigInteger(1, it.copyOfRange(1, 1 + 32))
            val y = BigInteger(1, it.copyOfRange(1 + 32, 1 + 32 * 2))

            val curveSpec = ECNamedCurveTable.getParameterSpec("brainpoolP256r1")
            val otherPublicKey = org.bouncycastle.jce.spec.ECPublicKeySpec(
                curveSpec.curve.createPoint(x, y),
                curveSpec
            ).let { pubKeySpec ->
                KeyFactory.getInstance("EC", cryptoConfig.provider).generatePublic(pubKeySpec) as ECPublicKey
            }

            val ivSpec = IvParameterSpec(it, 1 + 32 * 2, spec.ivSize.nrOfBytes)

            generateCipher(ivSpec, ourECKeyPair, otherPublicKey, spec, Cipher.DECRYPT_MODE, cryptoConfig)
                .doFinal(ciphertext, 1 + 32 * 2 + spec.ivSize.nrOfBytes, it.size - (1 + 32 * 2 + spec.ivSize.nrOfBytes))
        }

    internal fun generateCipher(
        ivSpec: IvParameterSpec,
        ourECKeyPair: KeyPair,
        otherECPublicKey: ECPublicKey,
        spec: VauEciesSpec,
        mode: Int,
        cryptoConfig: VauCryptoConfig,
    ): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding", cryptoConfig.provider).apply {
            val secret = KeyAgreement.getInstance("ECDH", cryptoConfig.provider).apply {
                init(ourECKeyPair.private, cryptoConfig.random)
                doPhase(otherECPublicKey, true)
            }.generateSecret()

            val aesKey = ByteArray(spec.aesSize.nrOfBytes).apply {
                HKDFBytesGenerator(SHA256Digest()).apply {
                    init(HKDFParameters(secret, null, spec.info))
                }.generateBytes(this, 0, this.size)
            }

            init(mode, SecretKeySpec(aesKey, "AES"), ivSpec)
        }
}

class VauAesGcmSpec(
    /**
     * IV size in bytes.
     */
    val ivSize: BinarySize,
    /**
     * Tag length in bytes.
     */
    val tagSize: BinarySize,
) {
    companion object {
        @JvmField
        val V1 = VauAesGcmSpec(
            ivSize = BinarySize(nrOfBytes = 12),
            tagSize = BinarySize(nrOfBytes = 16),
        )
    }
}

object AesGcm {
    fun encrypt(
        aesKey: SecretKey,
        spec: VauAesGcmSpec,
        cleartext: ByteArray,
        cryptoConfig: VauCryptoConfig,
    ): ByteArray {
        val ivBytes = ByteArray(spec.ivSize.nrOfBytes).apply {
            cryptoConfig.random.nextBytes(this)
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding", cryptoConfig.provider).apply {
            init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(spec.tagSize.nrOfBits, ivBytes))
        }.doFinal(cleartext)

        return ivBytes + cipher
    }

    fun decrypt(
        aesKey: SecretKey,
        spec: VauAesGcmSpec,
        ciphertext: ByteArray,
        cryptoConfig: VauCryptoConfig,
    ): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding", cryptoConfig.provider).apply {
            init(
                Cipher.DECRYPT_MODE,
                aesKey,
                GCMParameterSpec(spec.tagSize.nrOfBits, ciphertext, 0, spec.ivSize.nrOfBytes)
            )
        }.doFinal(ciphertext, spec.ivSize.nrOfBytes, ciphertext.size - spec.ivSize.nrOfBytes)
}
