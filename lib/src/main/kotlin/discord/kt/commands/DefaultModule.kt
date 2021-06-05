package discord.kt.commands

class DefaultModule(defaultName: String): Module() {
    override val name: String = defaultName

    // Don't show an empty "DEFAULT" in the help page
    override fun visibleInHelp(context: Context): Boolean = this.commands.isNotEmpty()
}