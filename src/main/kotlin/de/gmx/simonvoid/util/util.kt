package de.gmx.simonvoid.util

import org.slf4j.Logger
import kotlin.let
import kotlin.random.Random
import org.apache.commons.codec.binary.Base64.encodeBase64

@JvmInline
value class TraceId(private val value: Int) {
    override fun toString(): String = "(traceId: $value)"

    companion object {
        private val random = Random(System.currentTimeMillis())
        // since this is a concurrent function, let's generate a random traceId to allow request tracing
        // 5 digits should make collisions unlikely enough
        fun next() = TraceId(random.nextInt(from = 10_000, until = 100_000))
    }
}

inline fun <reified T> getLogger(): Logger = T::class.let { clazz ->
    TODO("implement this function with the libs your project uses")
}

fun ByteArray.encodeBase64(): ByteArray = encodeBase64(this)