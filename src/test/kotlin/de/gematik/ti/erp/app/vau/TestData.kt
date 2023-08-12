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

@file:Suppress("ktlint:max-line-length")

package de.gematik.ti.erp.app.vau

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom

val BCProvider = BouncyCastleProvider()

val TestCryptoConfig = object : VauCryptoConfig {
    override val provider = BCProvider
    override val random = SecureRandom()
}

object TestCrypto {
    const val CertPublicKeyX = "8634212830dad457ca05305e6687134166b9c21a65ffebf555f4e75dfb048888"
    const val CertPublicKeyY = "66e4b6843624cbda43c97ea89968bc41fd53576f82c03efa7d601b9facac2b29"

    const val Message = "Hallo Test"

    const val EccPrivateKey = "5bbba34d47502bd588ed680dfa2309ca375eb7a35ddbbd67cc7f8b6b687a1c1d"
    const val EphemeralPublicKeyX =
        "754e548941e5cd073fed6d734578a484be9f0bbfa1b6fa3168ed7ffb22878f0f"
    const val EphemeralPublicKeyY =
        "9aef9bbd932a020d8828367bd080a3e72b36c41ee40c87253f9b1b0beb8371bf"

    const val IVBytes = "257db4604af8ae0dfced37ce"
    val CipherText =
        "01 754e548941e5cd073fed6d734578a484be9f0bbfa1b6fa3168ed7ffb22878f0f 9aef9bbd932a020d8828367bd080a3e72b36c41ee40c87253f9b1b0beb8371bf 257db4604af8ae0dfced37ce 86c2b491c7a8309e750b 4e6e307219863938c204dfe85502ee0a".replace(
            " ",
            ""
        )
}
