package discord.kt.commands

import discord.kt.errors.DuplicateCommandNameException

abstract class Module: ArrayList<Command>() {
    abstract val name: String

    private val commandNames: MutableList<String> = mutableListOf()

    /**
     * Check if a command's name is not a duplicate before adding
     */
    private fun isValid(command: Command): Boolean {
        val lowerName = command.name.toLowerCase()
        val lowerAliases = command.aliases.map { it.toLowerCase() }

        // Check if name and all aliases are not yet being used anywhere
        this.commandNames.forEach {
            if (it == lowerName || lowerAliases.contains(it)) {
                throw DuplicateCommandNameException(command.name, this.name)            }
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

    /**
     * Return the list of command names without making the property public
     */
    fun getCommandNames(): List<String> {
        return this.commandNames
    }
}