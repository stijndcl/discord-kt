package discord.kt.errors

class DuplicateModuleNameException(name: String): IllegalArgumentException("Duplicate module name: $name")