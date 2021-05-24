package discord.kt.data

import dev.kord.common.entity.Snowflake

enum class Regexes(private val format: String) {
    USER("<@!?%s>");

    /**
     * Add an argument (like an id) into an enum value
     * using format strings
     */
    fun addArgument(arg: String? = null): String {
        if (arg == null) {
            return this.format;
        }

        return java.lang.String.format(this.format, arg)
    }

    fun addArgument(arg: Snowflake): String = this.addArgument(arg.asString)
}