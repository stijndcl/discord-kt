package discord.kt

import dev.kord.core.Kord
import dev.kord.core.entity.User
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import discord.kt.commands.Context
import discord.kt.commands.Module
import discord.kt.errors.DuplicateCommandNameException
import discord.kt.errors.DuplicateModuleNameException
import discord.kt.utils.InitOnce
import kotlinx.coroutines.ObsoleteCoroutinesApi

@OptIn(ObsoleteCoroutinesApi::class)
open class Bot(private val prefixMatcher: PrefixMatcher) {
    private lateinit var kord: Kord

    private val _initUserOnce = InitOnce<User>("user")
    val user: User by _initUserOnce

    private val _modules = mutableListOf<Module>()

    /**
     * Start the bot & authenticate with a given token
     */
    suspend fun run(token: String) {
        // This has to be done here because the builder is a suspend function
        this.kord = Kord(token)

        // Register all gateway event listeners
        this.registerListeners()

        // Sign in to Discord
        this.kord.login()
    }

    /**
     * Register all the Kord Event listeners
     */
    private fun registerListeners() {
        this.kord.on<ReadyEvent> {
            this@Bot.setup(this)
            this@Bot.onReady()
        }

        this.kord.on<MessageCreateEvent> {
            this@Bot.invoke(this)
            this@Bot.onMessage(this)
        }
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
    private fun setup(ready: ReadyEvent) {
        this._initUserOnce.initWith(ready.self)
        this.prefixMatcher.installUser(this.user)
    }

    /**
     * Parse the message that was created and invoke commands if necessary
     */
    private suspend fun invoke(messageCreate: MessageCreateEvent) {
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
                    command.process(Context(messageCreate))
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

    /**
     * Called whenever the bot gets a message
     */
    open fun onMessage(messageCreate: MessageCreateEvent) {}
}