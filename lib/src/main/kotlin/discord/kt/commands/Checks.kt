package discord.kt.commands

import dev.kord.common.entity.Permission

/**
 * Check if the person invoking the command has all permissions
 *
 * This returns a function because all Checks should only take Context
 * as a parameter, so this makes it easier for users to work with
 */
fun hasPermissions(vararg permissions: Permission): suspend (Context) -> Boolean {
    suspend fun _inner(context: Context): Boolean {
        // No permissions required, always return true
        if (permissions.isEmpty()) return true

        val member = context.messageCreateEvent.member ?: return false
        val memberPerms = member.getPermissions()

        // Check if the user has all permissions
        return permissions.all { perm -> memberPerms.contains(perm) }
    }

    return { c -> _inner(c) }
}