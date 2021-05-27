package discord.kt.commands

import dev.kord.core.entity.User
import discord.kt.Bot
import discord.kt.utils.InitOnce

abstract class Command(val parent: Command? = null) : CommandContainer {
    // The name of this command
    abstract val name: String

    // The aliases that can invoke this command
    open val aliases: List<String> = listOf()

    // Boolean indicating if this command can be triggered case-insensitively
    open val caseInsensitive: Boolean = true

    // Optional list of subcommands that this command has
    open val subCommands: List<Command> = listOf()

    // In case this function has subcommands:
    // if a subcommand was invoked, should this one still run?
    open val runWhenSubcommandInvoked: Boolean = false

    // Function signature in the help command
    open val helpSignature: String = ""

    // Command description in the help command
    open val helpDescription: String = ""

    // Function that runs before the invocation of a subcommand
    open fun runBeforeSubcommand(context: Context) {}

    // Function that runs after the invocation of a subcommand
    open fun runAfterSubcommand(context: Context) {}

    // Function that checks if the current user can see
    // this command in the help page
    // Useful when hiding things like mod commands from normal users
    open fun visibleInHelp(user: User): Boolean = true

    // Process the arguments given by the user that invoked the command
//    TODO first go recursively to find subcommands, then call this
    abstract suspend fun process(context: Context)

    // Reference to the bot
    private val _initBotOnce = InitOnce<Bot>("bot")
    private val _bot: Bot by _initBotOnce

    // Check if an argument can trigger this command
    fun triggeredBy(arg: String): Boolean {
        if (this.name.equals(arg, this.caseInsensitive)) return true

        if (this.caseInsensitive) {
            return this.aliases.map { it.toLowerCase() }.contains(arg.toLowerCase())
        }

        return this.aliases.contains(arg)
    }

    // Called when the command is added to the bot
    fun installed(bot: Bot) {
        this._initBotOnce.initWith(bot)
    }

    final override fun getChildWithName(name: String,  matchEntire: Boolean): Command? {
        // Create a path of all the steps towards the possible subcommand
        // Remove empty entries & extra whitespace on either side
        val path = name.split(" ").map { it.trim() }.filter { it.isNotEmpty() }

        return this.getChildWithName(path, 0, matchEntire)
    }

    final override fun getChildWithName(path: List<String>, index: Int, matchEntire: Boolean): Command? {
        // Reached end of list, no match found
        if (index == path.size) return null

        // Next item in the path does not trigger this one
        // -> stop looking
        if (!this.triggeredBy(path[index])) return null

        // Reached end of path & this command is a match
        if (index == path.size - 1) return this

        // Continue through with all subcommands
        this.subCommands.forEach { sub ->
            val matched = sub.getChildWithName(path, index + 1, matchEntire)

            // Found a match -> return it
            if (matched != null) return sub
        }

        // No subcommand was matched
        // Check if this command can be the match

        // Not reached the end yet, so the next items in the path could be arguments
        if (index < path.size - 1) {
            // If arguments are allowed in the search, then this command matches
            if (!matchEntire) return this
        }

        return null
    }
}