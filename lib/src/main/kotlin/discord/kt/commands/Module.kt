package discord.kt.commands

import discord.kt.Bot
import discord.kt.annotations.AddCommands
import discord.kt.errors.DuplicateCommandNameException
import discord.kt.utils.InitOnce
import java.util.function.Consumer

//TODO allow checks to be added with annotations above class & functions (add to list)
abstract class Module: CommandContainer {
    // Name of the module
    abstract val name: String

    // Checks that run when a command in this module is invoked,
    // command will only run if all of them return true
    open val checks: ArrayList<(Context) -> Boolean> = arrayListOf()

    open var helpDescription: String = ""

    // Commands stored in this module
    private val _commands = arrayListOf<Command>()
    val commands: List<Command> = _commands

    // Function that checks if this module should be visible
    // in the help page in the given context
    // For example: hiding things like mod commands from normal users
    // or only showing dev commands in the developer server
    open fun visibleInHelp(context: Context): Boolean = true

    /**
     * Function that gets called BEFORE an installed command is invoked
     * only called in case checks succeeded
     */
    open fun runBeforeCommand(context: Context, command: Command): Unit = Unit

    /**
     * Function that gets called AFTER an installed command is invoked
     * only called in case checks succeeded
     */
    open fun runAfterCommand(context: Context, command: Command): Unit = Unit

    private val commandNames: MutableList<String> = mutableListOf()

    // Reference to the bot
    private val _initBotOnce = InitOnce<Bot>("bot")
    private val _bot: Bot by _initBotOnce

    /**
     * This uses properties that have not yet been initialized in the init()
     * HAS to be called by a subclass
     */
//    TODO check that this only runs once
//    TODO check that this was called (in BOT)
    fun setup() {
        this.processAnnotations()
    }

    private fun processAnnotations() {
        this::class.java.annotations.forEach foreach@{ annotation ->
            if (annotation is AddCommands) {
                annotation.commands.forEach { command ->
                    // Create a Command from the KClass
                    val instance = command.constructors.first().call()

                    // Add command, the "add" function performs checks & throws exceptions
                    this.add(instance)
                }
            }
        }
    }

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

    fun add(element: Command): Boolean {
        // Duplicate names
        if (!this.isValid(element)) return false

        // Add to superclass (ArrayList)
        val added = this._commands.add(element)
        if (!added) return false

        // If adding to superclass succeeded, add all new names
        commandNames.add(element.name)
        commandNames.addAll(element.aliases)

        return true
    }

    fun forEach(action: Consumer<Command>) = this._commands.forEach(action)

    fun find(predicate: (Command) -> Boolean): Command? = this._commands.find(predicate)

    fun filter(predicate: (Command) -> Boolean) = this._commands.filter(predicate)

    // Called when the module is added to the bot
    fun installed(bot: Bot) {
        this._initBotOnce.initWith(bot)
        this.forEach { command -> command.installed(bot) }
    }

    /**
     * Invoke a command installed in this module
     * Don't invoke the command directly as this can run extra checks
     */
    suspend fun invokeCommand(context: Context) {
        val command = context.command

        // Checks failed
        if (this.checks.any { c -> !c(context) }) return
        if (!command.canRun(context)) return

        // Invoke all parents first (if necessary)
        // Go top-down for all the BEFORE functions
        this.runBeforeCommand(context, command)
        val chain = command.getParentChain()

        chain.reversed().forEach { parent -> parent.runBeforeSubcommand(context) }

        // Invoke commands itself
        command.process(context)

        // Run all AFTERs, this time bottom-up
        chain.forEach { parent -> parent.runAfterSubcommand(context) }

        // Finally, run the AFTER for the module
        this.runAfterCommand(context, command)
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

    final override fun getChildWithName(name: String, matchEntire: Boolean): Command? {
        // Create a path of all the steps towards the possible subcommand
        // Remove empty entries & extra whitespace on either side
        val path = name.split(" ").map { it.trim() }.filter { it.isNotEmpty() }

        return this.getChildWithName(path, 0, matchEntire)
    }

    final override fun getChildWithName(path: List<String>, index: Int, matchEntire: Boolean): Command? {
        this._commands.forEach { command ->
            val match = command.getChildWithName(path, index, matchEntire)

            if (match != null) return match
        }

        return null
    }
}