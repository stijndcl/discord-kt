package discord.kt

import dev.kord.common.entity.DiscordUser
import dev.kord.gateway.DefaultGateway
import dev.kord.gateway.GatewayConfigurationBuilder
import dev.kord.gateway.MessageCreate
import dev.kord.gateway.Ready
import discord.kt.commands.Context
import discord.kt.commands.Module
import discord.kt.errors.DuplicateCommandNameException
import discord.kt.errors.DuplicateModuleNameException
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

    private val _modules = mutableListOf<Module>()

    init {
        gateway.events.filterIsInstance<Ready>().flowOn(Dispatchers.IO).onEach {
            this.setup(it)
            this.onReady()
        }.launchIn(GlobalScope)

        gateway.events.filterIsInstance<MessageCreate>().flowOn(Dispatchers.IO).onEach {
            this.invoke(it)
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
     * Add a module to the bot
     *
     * Returns itself to allow chaining
     */
    fun installModule(module: Module): Bot {
        val nameLower = module.name.toLowerCase()
        val commandNames = module.getCommandNames()

        // Check if this module doesn't add any duplicate names
        this._modules.forEach {
            // Module name is not unique
            if (it.name.toLowerCase() == nameLower) {
                throw DuplicateModuleNameException(module.name)
            }

            // One of its command names is not unique
            it.getCommandNames().forEach { name ->
                if (commandNames.contains(name)) {
                    throw DuplicateCommandNameException(name, it.name)
                }
            }
        }

        // Everything is okay -> add the module in
        this._modules.add(module)

        return this
    }

    /**
     * Add an entire collection of modules to the bot
     */
    fun installAll(modules: Collection<Module>): Bot {
        modules.forEach { this.installModule(it) }

        return this
    }

    /**
     * Remove a module from the bot. If no module was found, nothing happens.
     *
     * Returns itself to allow chaining
     */
    fun uninstallModule(name: String): Bot {
        this._modules.removeIf { it.name == name }

        return this
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

        this._modules.forEach { module ->
            module.forEach { command ->
                if (command.triggeredBy(commands[0])) {
                    // TODO context
                    command.process(Context())
                    return@invoke
                }
            }
        }
    }

    // Functions called by gateway events

    /**
     * Called when the bot is ready
     */
    open fun onReady() {
        println("Ready!")
    }
}