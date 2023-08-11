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

package de.gematik.ti.erp.app

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@Requirement(
    "A_19179",
    "A_21323",
    "GS-A_4368",
    "GS-A_4367",
    sourceSpecification = "gemSpec_eRp_FdV",
    rationale = "Entropy is ensured by using SecureRandom for generation."
)
fun generateRandomAES256Key(): SecretKey =
    KeyGenerator.getInstance("AES").apply {
        init(256, secureRandomInstance())
    }.generateKey()
