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

private const val NO_BREAK_SPACE = '\u00A0'

/**
 * A list such as "2열 오른쪽 점유 · 3열 오른쪽 점유" breaks only after [separator], never as
 * "… · 3열 / 오른쪽 점유". Text without the separator falls back to [keepWordsTogether],
 * so a single long phrase can still wrap between its words.
 */
fun String.keepPhrasesTogether(separator: String = " · "): String {
    if (separator !in this) return keepWordsTogether()
    return split(separator).joinToString(separator) { phrase ->
        phrase.keepWordsTogether().replace(' ', NO_BREAK_SPACE)
    }
}
