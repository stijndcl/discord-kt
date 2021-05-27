package discord.kt.commands

interface CommandContainer {
    /**
     * Get a child command that matches a given name
     *
     * @param matchEntire: when true, this will only return a command
     * in case the "name" parameter contains the entire path to the command,
     * so this does NOT allow arguments to be passed.
     */
    fun getChildWithName(name: String, matchEntire: Boolean = false): Command?

    fun getChildWithName(path: List<String>, index: Int, matchEntire: Boolean): Command?
}