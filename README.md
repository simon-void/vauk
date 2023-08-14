## vauk

Kotlin/JVM implementation of the VAU encryption protocol

### Aim

Extract the code capable of de-/encoding Strings (or more precisely ByteArrays) according to VAU's symmetric and asymmetric way.

#### Status

check out the implementation of [VauEncryptionService](https://github.com/simon-void/vauk/blob/master/src/main/kotlin/VauEncryptionService.kt).

### Information on VAU protocol

- [VAU-Transport](https://github.com/gematik/api-erp/blob/master/docs/authentisieren.adoc#verschl%C3%BCsselter-transportkanal-zur-vertrauensw%C3%BCrdigen-ausf%C3%BChrungsumgebung-vau-transport)
- [Die statuslose Variante des VAU-Protokolls für das E-Rezept](https://bitbucket.org/andreas_hallof/vau-protokoll/src/master/erp/)

### In case of forking

If you fork the projekt to work on a different JDK notice that you'll probably have to
adjust the BouncyCastle dependency. There are [different versions](https://mvnrepository.com/search?q=org.bouncycastle%3Abcprov-jdk15on&ref=opensearch)
depending on which JDK you're targeting.

### History

The initial code has been directly copied over from the [vau package of the E-Rezept-App-Android repository](https://github.com/gematik/E-Rezept-App-Android/tree/master/common/src/commonMain/kotlin/de/gematik/ti/erp/app/vau) 
which was written within german health data company [gematik GmbH](https://www.gematik.de/).
The following classes/files are especially interesting:
- [Crypto](https://github.com/gematik/E-Rezept-App-Android/blob/master/common/src/commonMain/kotlin/de/gematik/ti/erp/app/vau/Crypto.kt):
contains the low level code to symmetrically en-/decrypt with AesGcm and asymmetrically en-/decrypt with Ecies.
- [ClientCrypto](https://github.com/gematik/E-Rezept-App-Android/blob/master/common/src/commonMain/kotlin/de/gematik/ti/erp/app/vau/ClientCrypto.kt):
contains the functionality for an HttpClient to handle its side of the VAU communication.

The vau code within the gematik's repository is explained in [this blog post](https://code.gematik.de/tech/2022/10/12/ecies-vau.html).