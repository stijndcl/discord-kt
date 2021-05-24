package discord.kt

import dev.kord.common.entity.DiscordUser
import dev.kord.common.entity.Snowflake
import discord.kt.data.Regexes
import discord.kt.utils.InitOnce

class PrefixMatcher(
    var prefix: List<String>,
    private val whenMentioned: Boolean = false,
    private val caseInsensitive: Boolean = false
) {
    constructor(prefix: String, whenMentioned: Boolean = false, caseInsensitive: Boolean = false)
            : this(listOf(prefix), whenMentioned, caseInsensitive)

    private val _initUserIdOnce = InitOnce<Snowflake>("userId")
    private val userId: Snowflake by _initUserIdOnce

    /**
     * Custom matcher function that can be overridden to perform
     * additional checks, or do more advanced things such as
     * custom guild prefixes
     */
    var customMatcher: (String) -> String? = {_ -> null}

    /**
     * Init the user id stored internally
     */
    fun installUser(user: DiscordUser) {
        this._initUserIdOnce.initWith(user.id)
    }

    /**
     * Main matching function that checks an input message
     * against all prefixes
     */
    fun check(message: String): String? {
        var prefixes = this.prefix.toList()

        if (this.caseInsensitive) {
            prefixes = prefixes.map { it.toLowerCase() }
        }

        // Check all prefixes to find one that matched
        prefixes.forEach {
            if (message.startsWith(it)) {
                // Return splice of original string to give the cased version
                return message.take(it.length)
            }
        }

        // @Mention is an allowed prefix
        if (this.whenMentioned) {
            val mentioned = this.wasMentioned(message)

            if (mentioned != null) {
                return mentioned
            }
        }

        // Return user's custom parser
        return customMatcher(message)
    }

    private fun wasMentioned(message: String): String? {
        // User id hasn't been initialized yet
        if (!this._initUserIdOnce.isInitialized()) return null

        // Compose regex & check match
        val regexArg = Regexes.USER.addArgument(this.userId)
        val regex = Regex("^$regexArg")

        return regex.find(message)?.value
    }
}