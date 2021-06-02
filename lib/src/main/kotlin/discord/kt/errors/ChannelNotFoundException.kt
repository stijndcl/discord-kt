package discord.kt.errors

import dev.kord.common.entity.Snowflake

class ChannelNotFoundException(id: Snowflake): RuntimeException("No channel found for Snowflake ${id.asString}")