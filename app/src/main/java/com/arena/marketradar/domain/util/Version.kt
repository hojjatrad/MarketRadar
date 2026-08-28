package com.arena.marketradar.domain.util

/** Simple semantic-version comparison (e.g. "1.3" vs "1.4"). */
object Version {

    fun parse(v: String): List<Int> =
        v.trim().trimStart('v').split('.', '-', '_').mapNotNull { it.toIntOrNull() }

    fun isNewer(candidate: String, current: String): Boolean {
        if (candidate.equals(current, true)) return false
        val c = parse(candidate); val k = parse(current)
        val n = maxOf(c.size, k.size)
        for (i in 0 until n) {
            val a = c.getOrElse(i) { 0 }; val b = k.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
