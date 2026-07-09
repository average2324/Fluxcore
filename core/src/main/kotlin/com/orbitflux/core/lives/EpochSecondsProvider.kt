package com.luminadigitale.fluxcore.core.lives

fun interface EpochSecondsProvider {
    fun nowEpochSeconds(): Long
}

object SystemEpochSecondsProvider : EpochSecondsProvider {
    override fun nowEpochSeconds(): Long {
        return System.currentTimeMillis() / 1000L
    }
}
