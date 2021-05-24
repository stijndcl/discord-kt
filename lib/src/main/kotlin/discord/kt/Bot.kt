package discord.kt

import dev.kord.common.entity.DiscordUser
import dev.kord.gateway.DefaultGateway
import dev.kord.gateway.GatewayConfigurationBuilder
import dev.kord.gateway.MessageCreate
import dev.kord.gateway.Ready
import discord.kt.utils.InitOnce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ObsoleteCoroutinesApi::class)
open class Bot(private val prefixMatcher: PrefixMatcher) {
    private val gateway = DefaultGateway()

    private val _initUserOnce = InitOnce<DiscordUser>("user")
    val user: DiscordUser by _initUserOnce

    init {
        gateway.events.filterIsInstance<Ready>().flowOn(Dispatchers.IO).onEach {
            this.setup(it)
            this.onReady()
        }.launchIn(GlobalScope)

        gateway.events.filterIsInstance<MessageCreate>().flowOn(Dispatchers.IO).onEach {

        }.launchIn(GlobalScope)
    }

    /**
     * Start the bot & authenticate with a given token
     */
    @OptIn(ObsoleteCoroutinesApi::class)
    suspend fun run(token: String) {
        gateway.start(GatewayConfigurationBuilder(token).build())
    }

    /**
     * Setup initial things when the bot is ready, different from onReady in that
     * it can't be overridden
     */
    private fun setup(ready: Ready) {
        this._initUserOnce.initWith(ready.data.user)
        this.prefixMatcher.installUser(this.user)
    }

    /**
     * Parse the message that was created and invoke commands if necessary
     */
    private fun invoke(messageCreate: MessageCreate) {
        val message = messageCreate.message.content

        // Try to match a prefix against this message
        val prefixUsed = this.prefixMatcher.check(message) ?: return

        val commands = messageCreate.message.content.drop(prefixUsed.length) // Strip prefix
            .trimStart() // Strip optional whitespace after prefix
            .split(" ") // Split string based on spaces

//        TODO find command & match it
    }

    // Functions called by gateway events

    /**
     * Called when the bot is ready
     */
    open fun onReady() {
        println("Ready!")
    }
}