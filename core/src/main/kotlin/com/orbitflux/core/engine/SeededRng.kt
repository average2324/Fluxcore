package com.orbitflux.core.engine

class SeededRng(seed: Long) {
    private var state: Long = if (seed != 0L) seed else 0x9E3779B97F4A7C15UL.toLong()

    fun nextLong(): Long {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        state = x
        return x
    }

    fun nextInt(boundExclusive: Int): Int {
        require(boundExclusive > 0) { "boundExclusive must be > 0" }
        val positive = nextLong() and Long.MAX_VALUE
        return (positive % boundExclusive.toLong()).toInt()
    }

    fun nextFloat(): Float {
        val bits = ((nextLong() ushr 40) and 0xFFFFFF).toInt()
        return bits / 16_777_216f
    }

    fun nextFloat(minInclusive: Float, maxInclusive: Float): Float {
        return minInclusive + (maxInclusive - minInclusive) * nextFloat()
    }
}
