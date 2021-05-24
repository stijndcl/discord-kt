package discord.kt.commands

import discord.kt.Bot
import discord.kt.utils.InitOnce

abstract class Command {
    // The name of this command
    abstract val name: String

    // The aliases that can invoke this command
    open val aliases: List<String> = listOf()

    // Boolean indicating if this command can be triggered case-insensitively
    open val caseInsensitive: Boolean = true

    // Optional list of subcommands that this command has
    open val subCommands: List<Command> = listOf()

    // Process the arguments given by the user that invoked the command
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
}