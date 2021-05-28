package discord.kt.commands

import dev.kord.core.event.message.MessageCreateEvent

// TODO add properties & hide MessageCreateEvent
//  TODO split args into list
class Context(val messageCreateEvent: MessageCreateEvent, private val prefix: String, val command: Command) {
    private val _noPrefix = messageCreateEvent.message.content
        .drop(prefix.length) // Remove prefix
        .trim() // Remove whitespace
    val alias = this._noPrefix.split(" ").first()
    val content = this._noPrefix
    val args = this._noPrefix.split(" ")
        .drop(command.depth + 1) // Remove all command invocations
        .joinToString { " " }
    val channel = messageCreateEvent.message.channel
}