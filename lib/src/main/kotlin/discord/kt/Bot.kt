package discord.kt

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.builder.PresenceBuilder
import dev.kord.rest.builder.guild.GuildCreateBuilder
import discord.kt.commands.Context
import discord.kt.commands.DefaultHelpModule
import discord.kt.commands.Module
import discord.kt.errors.DuplicateCommandNameException
import discord.kt.errors.DuplicateModuleNameException
import discord.kt.utils.InitOnce
import kotlinx.coroutines.ObsoleteCoroutinesApi

//TODO provide shorthand for internal Kord methods
@OptIn(ObsoleteCoroutinesApi::class)
open class Bot(
    private val prefixMatcher: PrefixMatcher, // PrefixMatcher used by the bot
    val ignoreSelf: Boolean = true, // Should the bot ignore itself?
    val ignoreBots: Boolean = true, // Should the bot ignore other bots? (excludes self)
    val ignoreDms: Boolean = true, // Should the bot ignore DM's?
    private val createDefaultHelp: Boolean = true, // Should a default help command be added?
    private val mainEmbedColour: Color = Color(0x3498db) // Colour of the default help page
) {
    private lateinit var kord: Kord

    private val _initUserOnce = InitOnce<User>("user")
    val user: User by _initUserOnce

    private val _modules = mutableListOf<Module>()

    /**
     * Start the bot & authenticate with a given token
     */
    suspend fun run(token: String) {
        // This has to be done here because the builder is a suspend function
        // which can't be invoked in an init {}
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

            // One of the command names is being used by a module
            if (commandNames.contains(it.name.toLowerCase())) {
                throw IllegalArgumentException("Command name ${it.name} is already being used by a module")
            }

            // New module name is already being used by an old command
            if (it.getCommandNames().contains(nameLower)) {
                throw IllegalArgumentException("Module name ${module.name} is already being used by a command in module ${it.name}")
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
        module.installed(this)

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
     * Return a list of all modules that are currently installed
     */
    fun getInstalledModules(): List<Module> = this._modules

    /**
     * Setup initial things when the bot is ready, different from onReady in that
     * it can't be overridden
     */
    private fun setup(ready: ReadyEvent) {
        this._initUserOnce.initWith(ready.self)
        this.prefixMatcher.installUser(this.user)

        if (this.createDefaultHelp) {
            this.installModule(DefaultHelpModule(mainEmbedColour))
        }
    }

    /**
     * Parse the message that was created and invoke commands if necessary
     */
    private suspend fun invoke(messageCreate: MessageCreateEvent) {
//        TODO allow commands & modules to overrule these restrictions
//         (eg. some commands work in DM, ...)
//         -> don't check them here
//         use lateinit vars to see if user changed them, else use bot vars as defaults
        // Check if command invocation should proceed
        if (!this.checkInvocationRestrictions(messageCreate)) return

        val message = messageCreate.message.content

        // Try to match a prefix against this message
        val prefixUsed = this.prefixMatcher.check(message) ?: return

//        TODO split args into mutable list of strings & pass those instead
        val commands = messageCreate.message.content.drop(prefixUsed.length) // Strip prefix
            .trimStart() // Strip optional whitespace after prefix
            .split(" ") // Split string based on spaces

        this._modules.forEach { module ->
            val command = module.getCommandMatching(commands[0])

            if (command != null) {
                // TODO context
                command.process(Context(messageCreate))
                return@invoke
            }
        }
    }

    /**
     * On command invocation, check if the bot should respond
     * to given cases
     */
    private fun checkInvocationRestrictions(messageCreate: MessageCreateEvent): Boolean {
        // Check if bot responds to itself
        if (this.ignoreSelf && messageCreate.member == this.user) {
            return false
        }

        // Check if bot responds to bots that aren't itself
        if (messageCreate.member != this.user && this.ignoreBots && messageCreate.member?.isBot == true) {
            return false
        }

        // Check if bot responds in DM
        if (this.ignoreDms && messageCreate.guildId == null) {
            return false
        }

        return true
    }

    // ==================================
    // Functions called by gateway events
    // ==================================

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

    // ===========================
    // Shorthands for Kord methods
    // ===========================
    suspend fun logout() = this.kord.logout()
    suspend fun shutdown() = this.kord.shutdown()
    suspend fun createGuild(
        name: String, builder: GuildCreateBuilder.() -> Unit
    ): Guild = this.kord.createGuild(name, builder)
    suspend fun getChannel(
        id: Snowflake,
        strategy: EntitySupplyStrategy<*> =
            this.kord.resources.defaultStrategy,
    ): Channel? = this.kord.getChannel(id, strategy)
    suspend fun getGuild(
        id: Snowflake,
        strategy: EntitySupplyStrategy<*> =
            this.kord.resources.defaultStrategy,
    ): Guild? = this.kord.getGuild(id, strategy)
    suspend fun getUser(
        id: Snowflake,
        strategy: EntitySupplyStrategy<*> = this.kord.resources.defaultStrategy
    ): User? = this.kord.getUser(id, strategy)
    suspend fun editPresence(builder: PresenceBuilder.() -> Unit) = this.kord.editPresence(builder)
}