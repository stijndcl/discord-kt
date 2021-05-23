package discord.kt

import dev.kord.gateway.DefaultGateway
import dev.kord.gateway.GatewayConfigurationBuilder
import kotlinx.coroutines.ObsoleteCoroutinesApi

class Bot(prefixMatcher: PrefixMatcher) {
    private val gateway = DefaultGateway()

    @OptIn(ObsoleteCoroutinesApi::class)
    suspend fun run(token: String) {
        gateway.start(GatewayConfigurationBuilder(token).build())
    }
}