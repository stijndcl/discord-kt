package discord.kt.errors

class DuplicateCommandNameException(command: String, module: String): IllegalArgumentException("Duplicate command name \"$command\" has already been registered in module $module")