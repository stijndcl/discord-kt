# discord-kt
Kotlin API Wrapper for Discord, based on Kord's Gateway implementation. This library makes things like prefixes, error handling, and commands a lot easier, more manageable, and more readable.

Kord: https://github.com/kordlib

Kord Gateway: https://github.com/kordlib/kord/tree/0.7.x/gateway

## Examples
### 1. Bot
The `Bot` class is, as the name suggests, the main Bot class that will initialize your Discord Bot. You can extend this class to override most of the methods and properties.

A `Bot` can receive a `PrefixMatcher` to handle prefixes, and an `ErrorHandler` to specify what should happen to exceptions. Both of these classes can also be overridden.

The sample below shows how to create a Bot that responds to both `?` and being `@mention`ed, and sends exceptions in an embed in Discord.

```kotlin
suspend fun main() {
    // Matches ? and @mention
    val prefixes = PrefixMatcher("?", whenMentioned = true)
    
    // Send exceptions to a Discord channel
    val errorHandler = ErrorHandler(errorChannel = "ERROR_CHANNEL_ID")
    
    val token = "TOKEN_GOES_HERE"
    Bot(prefixes, errorHandler).run(token)
}
```