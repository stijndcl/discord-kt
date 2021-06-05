package discord.kt.annotations

import discord.kt.commands.Command
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class InstallCommands(vararg val commands: KClass<out Command>)