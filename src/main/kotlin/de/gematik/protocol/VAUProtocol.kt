@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
/*
 * Copyright (c) 2020 gematik - Gesellschaft für Telematikanwendungen der Gesundheitskarte mbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.gematik.titus.protocol

import de.gematik.protocol.EciesUtil
import de.gematik.protocol.signencrypt.AESGCM128
import de.gematik.protocol.signencrypt.AESGCM128.generateRandomBytes
import de.gematik.ti.erp.app.vau.TraceId
import de.gematik.ti.erp.app.vau.getLogger
import de.gematik.titus.erezept.vau.AccessCode
import de.gematik.titus.erezept.vau.AesKey
import de.gematik.titus.erezept.vau.L1VauReqEnvelopeAkaInnerVau
import de.gematik.titus.erezept.vau.L2VauReqEnvelopeAkaInnerVau
import de.gematik.titus.erezept.vau.L2VauResEnvelope
import de.gematik.titus.erezept.vau.L3VauResEnvelopeAkaEncryptedL2
import de.gematik.titus.erezept.vau.L4VauReqEnvelopeAkaOuterVau
import de.gematik.titus.erezept.vau.RequestId
import org.apache.commons.codec.binary.Hex
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

/**
 * Implementiert das Kommunikationsprotokoll zwischen E-Rezept-VAU und E-Rezept-Clients gemäß gemSpec_Krypt_V2 Kapitel 7.
 * Spezifikationsbasis gemSpec_Krypt_V2, Version 2.18.0, 12.11.2020
 */
class VAUProtocol {
    /**
     * 2. Der Client erzeugt einen HTTP-Request mit Request-Body und Request-Header als Datenstrukturen
     * (= innerer HTTP-Request) (A_20161).
     */

    companion object {
        val logger = getLogger<VAUProtocol>()

        init {
            Security.addProvider(BouncyCastleProvider())
        }

        fun vauDecryptResponse(
            aesKey: AesKey,
            l3VauRes: L3VauResEnvelopeAkaEncryptedL2,
            traceId: TraceId,
        ): L2VauResEnvelope {
            val rawBytes = AESGCM128.decrypt(l3VauRes.bytes, aesKey.asBytes())
            return L2VauResEnvelope.from(rawBytes, traceId)
        }

        fun vauEncryptRequest(
            serverEncPublicKey: BCECPublicKey,
            l1VauReq: L1VauReqEnvelopeAkaInnerVau,
        ): Pair<L4VauReqEnvelopeAkaOuterVau, AesKey> {

            val (l2VauReq, aesKey) = run {
                val symmetricKeyForResponse = AESGCM128.generateSymmetricKey(16)
                val aesKey = AesKey(Hex.encodeHexString(symmetricKeyForResponse.encoded))
                val accessToken = l1VauReq.headers.jwt ?: error("couldn't find single jwt token in vau request headers")

                L2VauReqEnvelopeAkaInnerVau(
                    accessToken = AccessCode(accessToken),
                    requestId = RequestId(Hex.encodeHexString(generateRandomBytes(16))),
                    aesKey = aesKey,
                    l1InnerVau = l1VauReq,
                )to aesKey
            }
            val body = l2VauReq.toByteArray()

            val encryptedBody: ByteArray = EciesUtil.eciesEncrypt(serverEncPublicKey, body)

            return L4VauReqEnvelopeAkaOuterVau(encryptedBody) to aesKey
        }
    }
}
