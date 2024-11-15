package de.gematik.protocol.signencrypt

import de.gematik.ti.erp.app.vau.getLogger
import java.util.concurrent.ThreadLocalRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESGCM128 {
    private val logger = getLogger<AESGCM128>()
    private const val AES_KEY_SIZE = 16 // in  bytes
    private const val GCM_IV_LENGTH = 12 // in bytes
    private const val GCM_TAG_LENGTH = 16 // in  bytes

    private fun getGCMParameterSpec(iv: ByteArray): GCMParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

    fun encrypt(input: ByteArray, key: ByteArray, associatedData: ByteArray? = null): ByteArray {
        val iv = generateIV()
        val secretKey: SecretKey = SecretKeySpec(key, "AES")
        val cipherTextPlusTag: ByteArray = newCipher().let { cipher->
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, getGCMParameterSpec(iv))
            if (associatedData != null) {
                cipher.updateAAD(associatedData)
            }
            cipher.doFinal(input)
        }
        val encMessage: ByteArray = iv.copyOf(GCM_IV_LENGTH + cipherTextPlusTag.size)
        System.arraycopy(cipherTextPlusTag, 0, encMessage, GCM_IV_LENGTH, cipherTextPlusTag.size)
        return encMessage
    }

    fun decrypt(encMessage: ByteArray, key: ByteArray, associatedData: ByteArray? = null): ByteArray {
        val secretKey: SecretKey = SecretKeySpec(key, "AES")
        val iv = encMessage.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = encMessage.copyOfRange(GCM_IV_LENGTH, encMessage.size)
        return newCipher().let { cipher->
            cipher.init(Cipher.DECRYPT_MODE, secretKey, getGCMParameterSpec(iv))
            associatedData?.let { cipher.updateAAD(it) }
            // plainText
            cipher.doFinal(cipherText)
        }
    }

    private fun newCipher(): Cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")

    private fun generateIV(): ByteArray = generateRandomBytes(GCM_IV_LENGTH)

    fun generateRandomBytes(numberOfBytes: Int): ByteArray {
        val bytes = ByteArray(numberOfBytes)
        ThreadLocalRandom.current().nextBytes(bytes)
        return bytes
    }

    fun generateSymmetricKey(keySizeInBytes: Int = AES_KEY_SIZE): SecretKey = try {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(keySizeInBytes * 8)
        keyGen.generateKey()
    } catch (e: Exception) {
        logger.error(e.message, e)
        throw e
    }
}
