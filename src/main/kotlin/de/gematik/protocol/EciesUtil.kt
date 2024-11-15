package de.gematik.protocol

import de.gematik.protocol.signencrypt.AESGCM128.decrypt
import de.gematik.protocol.signencrypt.AESGCM128.encrypt
import de.gematik.protocol.signencrypt.KeyPairGenerator.generateECCKeyPair
import de.gematik.protocol.signencrypt.Utils.ECKA
import de.gematik.protocol.signencrypt.Utils.HKDF
import de.gematik.ti.erp.app.vau.getLogger
import org.apache.commons.codec.binary.Hex
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

object EciesUtil {
    private val LOGGER = getLogger<EciesUtil>()
    
    fun eciesEncrypt(serverEncPublicKey: BCECPublicKey, bytes: ByteArray): ByteArray = try {

        //6. Die Zeichenkette p MUSS mittels des ECIES-Verfahrens [SEC1-2009] und mit folgenden Vorgaben verschlüsselt werden:

        //a. Er MUSS ein ephemeres ECDH-Schlüsselpaar erzeugen und mit diesem und dem VAU-Schlüssel aus A_20160-* ein
        // ECDH gemäß [NIST-800-56-A] durchgeführen. Das somit erzeugte gemeinsame Geheimnis ist Grundlage für die folgende Schlüsselableitung.
        val spec = serverEncPublicKey.params as ECNamedCurveSpec
        val ephemeralKeyPair = generateECCKeyPair(spec.name)
        val share = ECKA(ephemeralKeyPair.private, serverEncPublicKey)
        LOGGER.debug("share on encryption: " + Hex.encodeHexString(share))

        //b. Als Schlüsselableitungsfunktion MUSS er die HKDF nach [RFC-5869] auf Basis von SHA-256 verwenden.
        //c. Dabei MUSS er den Ableitungsvektor "ecies-vau-transport" verwenden, d. h. in der Fomulierung von
        // [RFC-5869] info="ecies-vau-transport" .

        //d. Er MUSS mit dieser Schlüsselableitung einen AES-128-Bit Content-Encryption-Key für die Verwendung von AES/GCM ableiten.
        val transportKey = HKDF(share, "ecies-vau-transport", 128)
        LOGGER.debug("transportkey on encryption: " + Hex.encodeHexString(transportKey))

        //e. Er MUSS für Verschlüsselung mittels AES/GCM einen 96 Bit langen IV zufällig erzeugen.
        //f. Er MUSS mit dem CEK und dem IV mittels AES/GCM p verschlüsseln, wobei dabei ein 128 Bit langer
        // Authentication-Tag zu verwenden ist.
        val aesCipher = encrypt(bytes, transportKey) // iv | cipher | tag

        //g. Er MUSS das Ergebnis wie folgt kodieren:
        // chr(0x01) ||
        // <32 Byte X-Koordinate von öffentlichen Schlüssel aus (a) > || <32 Byte Y-Koordinate> || <12 Byte IV> || <AES-GCM-Chiffrat> ||
        // <16 Byte AuthenticationTag> (vgl. auch Tab_KRYPT_ERP und folgende die Beispielverschlüsselung).
        // Die Koordinaten sind (wie üblich) vorne mit chr(0) zu padden solange bis sie eine Kodierungslänge von 32 Byte erreichen.
        val ephemeralPublicKey = ephemeralKeyPair.public as BCECPublicKey
        val fullCipher = concatenateBytes(
            byteArrayOf(0x01),
            ephemeralPublicKey.q.xCoord.encoded,  // getEncoded() does the required padding
            ephemeralPublicKey.q.yCoord.encoded,
            aesCipher
        )
        LOGGER.debug("Gesamt-Chiffrat als Hex-Dump: " + Hex.encodeHexString(fullCipher))
        fullCipher
    } catch (e: Exception) {
        e.printStackTrace()
        throw RuntimeException(e)
    }

    fun eciesDecrypt(privateKey: BCECPrivateKey, request: ByteArray): ByteArray = try {
        run {
            val version = request[0].toInt()
            if (version != 1) {
                error("version of vauchannel not supported. Expected 1 but was $version")
            }
        }
        val ephemeralPublicKeyX = ByteArray(32)
        val ephemeralPublicKeyY = ByteArray(32)
        val aesCipher = ByteArray(request.size - 65) // iv | cipher | tag
        var base = 1
        System.arraycopy(request, base, ephemeralPublicKeyX, 0, ephemeralPublicKeyX.size)
        base += ephemeralPublicKeyX.size
        System.arraycopy(request, base, ephemeralPublicKeyY, 0, ephemeralPublicKeyY.size)
        base += ephemeralPublicKeyY.size
        System.arraycopy(request, base, aesCipher, 0, aesCipher.size)
        val spec = privateKey.params as ECNamedCurveSpec
        val params = AlgorithmParameters.getInstance("EC", "BC")
        params.init(ECGenParameterSpec(spec.name))
        val ecParameterSpec = params.getParameterSpec(
            ECParameterSpec::class.java
        )
        val ecPoint = ECPoint(BigInteger(1, ephemeralPublicKeyX), BigInteger(1, ephemeralPublicKeyY))
        val keyFactory = KeyFactory.getInstance("EC", "BC")
        val ephemeralPublicKey =
            keyFactory.generatePublic(ECPublicKeySpec(ecPoint, ecParameterSpec)) as BCECPublicKey
        val share = ECKA(privateKey, ephemeralPublicKey)
        LOGGER.debug("share on decryption: " + Hex.encodeHexString(share))
        val transportKey = HKDF(share, "ecies-vau-transport", 128)
        LOGGER.debug("transportkey on encryption: " + Hex.encodeHexString(transportKey))
        decrypt(aesCipher, transportKey)
    } catch (e: Exception) {
        e.printStackTrace()
        throw RuntimeException(e)
    }

    private fun concatenateBytes(vararg arrays: ByteArray): ByteArray = ByteBuffer.allocate(arrays.sumOf { it.size }).apply {
        arrays.forEach { put(it) }
    }.array()
}
