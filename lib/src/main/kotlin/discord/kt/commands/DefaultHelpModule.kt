package discord.kt.commands

import dev.kord.common.Color

class DefaultHelpModule(embedColour: Color): Module() {
    override val name: String = "HelpModule"
    override fun visibleInHelp(context: Context): Boolean = false

    init {
        this.add(DefaultHelpCommand(embedColour))
    }
}