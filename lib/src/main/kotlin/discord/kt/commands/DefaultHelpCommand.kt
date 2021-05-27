package discord.kt.commands

import dev.kord.common.Color
import dev.kord.core.behavior.channel.createEmbed

class DefaultHelpCommand(private val embedColour: Color) : Command() {
    override val name: String = "Help"

    override suspend fun process(context: Context) {
        context.messageCreateEvent.message.channel.createEmbed {
            title = "Title"
            color = embedColour
            author {name = "Author"}
        }
    }
}