package discord.kt.commands

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
                throw IllegalArgumentException("Duplicate command name \"$lowerName\" has already been registered in module $name")
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
}