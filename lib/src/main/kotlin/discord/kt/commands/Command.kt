package discord.kt.commands

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
    abstract fun process(context: Context)
}