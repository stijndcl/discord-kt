package discord.kt.errors.handling

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder

open class ErrorHandler(
    private val logToTerminal: Boolean = true,
    private val errorChannel: Snowflake? = null,
    private val errorEmbedColour: Color = Color(0xe74c3c)
) {

    open suspend fun onException(message: MessageCreateEvent, e: Exception, kord: Kord) {
        if (this.logToTerminal) {
            System.err.println(e.stackTraceToString())
        }

        if (this.errorChannel == null) return

        val channel = MessageChannelBehavior(this.errorChannel, kord)
        this.sendErrorEmbed(message, e, channel)
    }

    open suspend fun sendErrorEmbed(message: MessageCreateEvent, e: Exception, channel: MessageChannelBehavior) {
        val fullTrace = e.stackTraceToString()
        val fullLines = fullTrace.splitToSequence("\n").count() - 1

        var truncated = fullTrace

        // Truncate stacktrace if necessary
        if (truncated.length > EmbedBuilder.Field.Limits.value) {
            truncated = truncated.take(EmbedBuilder.Field.Limits.value - 25).trim()
            truncated += "...\n\n${truncated.splitToSequence("\n").count() - 1 - fullLines} more lines"
        }

        val context = "${message.member?.displayName ?: "Unknown"} in ${message.getGuild()?.name ?: "DM"}"

        channel.createEmbed {
            title = "Error"
            color = errorEmbedColour

            field {
                name = "Command"
                value = message.message.content
                inline = true
            }

            field {
                name = "Caused by"
                value = context
                inline = true
            }

            field {
                name = "Traceback"
                value = truncated
                inline = false
            }
        }
    }
}