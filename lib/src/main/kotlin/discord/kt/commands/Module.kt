package discord.kt.commands

import dev.kord.core.entity.User
import discord.kt.Bot
import discord.kt.errors.DuplicateCommandNameException
import discord.kt.utils.InitOnce

abstract class Module: ArrayList<Command>() {
    // Name of the module
    abstract val name: String

    // Function that checks if the current user can see
    // this module in the help page
    // Useful when hiding things like mod commands from normal users
    open fun visibleInHelp(user: User): Boolean = true

    private val commandNames: MutableList<String> = mutableListOf()

    // Reference to the bot
    private val _initBotOnce = InitOnce<Bot>("bot")
    private val _bot: Bot by _initBotOnce

    /**
     * Check if a command's name is not a duplicate before adding
     */
    private fun isValid(command: Command): Boolean {
        val lowerName = command.name.toLowerCase()
        val lowerAliases = command.aliases.map { it.toLowerCase() }

        // Check if command doesn't use the name of the module
        if (lowerName == this.name.toLowerCase()) {
            throw IllegalArgumentException("Command name ${command.name} overlaps with module name")
        }

        if (lowerAliases.contains(this.name.toLowerCase())) {
            throw IllegalArgumentException("Command ${command.name} has an alias ${this.name} which overlaps with module name")
        }

        // Check if name and all aliases are not yet being used anywhere
        this.commandNames.forEach {
            if (it == lowerName || lowerAliases.contains(it)) {
                throw DuplicateCommandNameException(command.name, this.name)
            }
        }

        return true
    }

    override fun add(element: Command): Boolean {
        // Duplicate names
        if (!this.isValid(element)) return false

        // Add to superclass (ArrayList)
        val added = super.add(element)
        if (!added) return false

        // If adding to superclass succeeded, add all new names
        commandNames.add(element.name)
        commandNames.addAll(element.aliases)

        return true
    }

    // Called when the module is added to the bot
    fun installed(bot: Bot) {
        this._initBotOnce.initWith(bot)
        this.forEach { command -> command.installed(bot) }
    }

    /**
     * Return the list of command names without making the property public
     */
    fun getCommandNames(): List<String> {
        return this.commandNames
    }

    /**
     * Find the installed command that matches a given name
     */
    fun getCommandMatching(name: String): Command?  = this.find { command -> command.triggeredBy(name) }
}