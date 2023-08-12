## vauk

Kotlin/JVM implementation of the VAU encryption protocol

### Aim

Extract the code capable of de-/encoding Strings (or more precisely ByteArrays) according to VAU's symmetric and asymmetric way.

#### Status

check out the implementation of [VauEncryptionService](https://github.com/simon-void/vauk/blob/master/src/main/kotlin/VauEncryptionService.kt).

### History

The initial code has been directly copied over from the [E-Rezept-App-Android repository](https://github.com/gematik/E-Rezept-App-Android/tree/master/common/src/commonMain/kotlin/de/gematik/ti/erp/app/vau)
which was written within german health data company [gematik GmbH](https://www.gematik.de/).

The vau code within the gematik's repository is explained in [this blog post](https://code.gematik.de/tech/2022/10/12/ecies-vau.html).