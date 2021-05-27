package discord.kt.commands

import dev.kord.common.Color
import dev.kord.core.entity.User

class DefaultHelpModule(embedColour: Color): Module() {
    override val name: String = "Help"
    override fun visibleInHelp(user: User): Boolean = false

    init {
        this.add(DefaultHelpCommand(embedColour))
    }
}