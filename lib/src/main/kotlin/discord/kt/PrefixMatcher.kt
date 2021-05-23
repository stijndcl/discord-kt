package discord.kt

class PrefixMatcher(
    val prefix: List<String>,
    val whenMentioned: Boolean = false,
    val caseInsensitive: Boolean = false
) {
    constructor(prefix: String, whenMentioned: Boolean, caseInsensitive: Boolean = false)
            : this(listOf(prefix), whenMentioned, caseInsensitive)
}