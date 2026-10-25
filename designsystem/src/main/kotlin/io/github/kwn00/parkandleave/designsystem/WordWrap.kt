package io.github.kwn00.parkandleave.designsystem

private const val WORD_JOINER = '\u2060'

/**
 * Hangul allows a line break between any two syllables, so "있습니다" can split as
 * "있습니 / 다". Word joiners keep each space-separated word whole on every API level;
 * phrase-based breaking needs API 33 and this app supports API 28.
 */
fun String.keepWordsTogether(): String {
    if (length < 2) return this
    val out = StringBuilder(length * 2)
    for (index in indices) {
        val char = this[index]
        if (index > 0 && !char.isWhitespace() && !this[index - 1].isWhitespace()) {
            out.append(WORD_JOINER)
        }
        out.append(char)
    }
    return out.toString()
}
