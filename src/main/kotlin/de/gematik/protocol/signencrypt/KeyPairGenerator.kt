package de.gematik.protocol.signencrypt

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.*
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

object KeyPairGenerator {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private val SECURE_RANDOM: SecureRandom = try {
        SecureRandom.getInstance("SHA1PRNG")
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException("Startup exception", e)
    }

    fun generateECCKeyPair(spec: String = "brainpoolp256r1"): KeyPair = KeyPairGenerator.getInstance("EC", "BC").apply {
        val namedParameterSpec = ECGenParameterSpec(spec)
        initialize(namedParameterSpec, SECURE_RANDOM)
    }.generateKeyPair()
}
