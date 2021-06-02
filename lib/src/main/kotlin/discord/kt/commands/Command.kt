package discord.kt.commands

import discord.kt.Bot
import discord.kt.utils.InitOnce

//TODO allow checks to be added with annotations above class & functions (add to list)
abstract class Command(private val parent: Command? = null) : CommandContainer {
    // The name of this command
    abstract val name: String

    // The aliases that can invoke this command
    open val aliases: List<String> = listOf()

    // Checks that run when this command is invoked,
    // command will only run if all of them return true
    // This is also called when subcommands are invoked
    open val checks: ArrayList<suspend (Context) -> Boolean> = arrayListOf()

    // Boolean indicating if this command can be triggered case-insensitively
    open val caseInsensitive: Boolean = true

//    TODO make adding subcommands easier (this won't always be passed, ...)
//      Decorator above function?
    // Optional list of subcommands that this command has
    open val subCommands: List<Command> = listOf()

    // Function usage in the help command
    open var helpUsage: String = ""

    // Command description in the help command
    open var helpDescription: String = ""

    // Function that runs before the invocation of a subcommand
    open fun runBeforeSubcommand(context: Context) {}

    // Function that runs after the invocation of a subcommand
    open fun runAfterSubcommand(context: Context) {}

    // Function that checks if this command should be visible
    // in the help page in the given context
    // For example: hiding things like mod commands from normal users
    // or only showing dev commands in the developer server
    open fun visibleInHelp(context: Context): Boolean = true

    // Process the arguments given by the user that invoked the command
    abstract suspend fun process(context: Context)

    // Reference to the bot
    private val _initBotOnce = InitOnce<Bot>("bot")
    val bot: Bot by _initBotOnce

    val depth: Int = if (parent == null) 0 else parent.depth + 1

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

    /**
     * Return all parents of this command
     */
    fun getParentChain(): List<Command> {
        val l = arrayListOf<Command>()
        var current = this.parent

        while (current != null) {
            l.add(current)
            current = current.parent
        }

        return l.toList()
    }

    // Check if all checks + parent checks for this command pass
    suspend fun canRun(context: Context): Boolean {
        // Parent checks
        if (parent != null) {
            if (!parent.canRun(context)) return false
        }

        // Own checks
        if (this.checks.any { c -> !c(context) }) return false

        // Nothing failed -> everything is okay
        return true
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