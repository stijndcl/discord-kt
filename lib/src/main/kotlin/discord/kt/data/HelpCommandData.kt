package discord.kt.data

import discord.kt.commands.Command
import discord.kt.commands.Module

data class HelpCommandData(val module: Module? = null, val command: Command? = null)
