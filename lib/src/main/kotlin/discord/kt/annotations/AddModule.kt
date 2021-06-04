package discord.kt.annotations

import discord.kt.commands.Module
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class AddModules(vararg val modules: KClass<Module>)
