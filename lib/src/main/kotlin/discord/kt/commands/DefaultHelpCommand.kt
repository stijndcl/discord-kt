package discord.kt.commands

import dev.kord.common.Color
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.rest.builder.message.EmbedBuilder
import discord.kt.data.HelpCommandData

class DefaultHelpCommand(private val embedColour: Color) : Command() {
    override val name: String = "Help"

    override suspend fun process(context: Context) {
        if (context.args.isEmpty()) return this.sendModulesList(context)

        val data = this.findCommand(context)

        // No command found
        if (data.module == null) return this.notFound(context)

        if (data.command == null) { // Command is null but module isn't -> send module help
            this.sendModuleHelp(context, data)
        } else {
            this.sendCommandHelp(context, data)
        }
    }

    /**
     * Find the command for which the help page is being called
     */
    private fun findCommand(context: Context): HelpCommandData {
        // The very first argument, this can match modules too for module help
        val first = context.args.split(" ")[0]

        this.bot.getInstalledModules().forEach { module ->
            if (module.name.equals(first, true)) {
                return HelpCommandData(module)
            }

            val match = module.getChildWithName(context.args, true)

            if (match != null && match.visibleInHelp(context)) return HelpCommandData(module, match)
        }

        return HelpCommandData()
    }

    private suspend fun notFound(context: Context) {
        context.messageCreateEvent.message.channel.createEmbed {
            author {name = "Help"}
            color = Color(0xe74c3c)
            description = "No command found."
        }
    }

    /**
     * Group modules in fields to show as many as possible
     */
    private fun groupModules(context: Context): List<String> {
        val l = mutableListOf<String>()

        var modules = this.bot.getInstalledModules()
            .filter { it.visibleInHelp(context) } // Remove invisible ones
            .map { it.name } // Only need the name
            .sorted() // Sort alphabetically

        // Add as many as the field count allows
        for (i: Int in 0 until EmbedBuilder.Limits.fieldCount) {
            if (modules.isEmpty()) break

            var group = ""

            var dropCount = 0

            // Add as many as possible
            modules.forEach f@{ module ->
                if ((group + module).length <= EmbedBuilder.Field.Limits.value) {
                    group += module
                    dropCount++
                } else return@f

                // Add newline for next command if it fits
                if ((group + "\n").length <= EmbedBuilder.Field.Limits.value) {
                    group += "\n"
                } else return@f
            }

            // Remove from the list of modules
            modules = modules.drop(dropCount)

            l.add(group.trim())
        }

        return l
    }

    private suspend fun sendModulesList(context: Context) {
        val modules = this.bot.getInstalledModules()
            .filter { it.visibleInHelp(context) }
            .map { it.name }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
            .joinToString("\n")

//        val groups = this.groupModules(context)
//
//        val embedFields = mutableListOf<EmbedBuilder.Field>()
//
//        groups.forEach { group ->
//            val field = EmbedBuilder.Field()
//            field.value = group
//            embedFields.add(field)
//        }

        context.channel.createEmbed {
            title = "Modules"
            color = embedColour
            author {name = "Help"}
            description = modules
//            fields = embedFields
        }
    }

    private suspend fun sendModuleHelp(context: Context, data: HelpCommandData) {
        val commands = data.module!!
            .filter { it.visibleInHelp(context) } // Only get commands that are visible to the user
            .map { it.name }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
            .joinToString("\n")

        context.channel.createEmbed {
            title = "${data.module.name} Module"
            color = embedColour
            author {name = "Help"}
            description = commands
        }
    }

    private fun formatAliases(data: HelpCommandData): String {
        if (data.command!!.aliases.isEmpty()) return ""

        return data.command.aliases.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it }).joinToString(", ")
    }

    private suspend fun sendCommandHelp(context: Context, data: HelpCommandData) {
        context.channel.createEmbed {
            title = data.command!!.name
            color = embedColour
            author {name = "Help"}
            description = this@DefaultHelpCommand.formatAliases(data)

            field {
                name = "Usage"
                value = data.command.helpUsage.ifEmpty { data.command.name }
                inline = false
            }

            field {
                name = "Description"
                value = data.command.helpDescription.ifEmpty { "No description available." }
                inline = false
            }
        }
    }
}