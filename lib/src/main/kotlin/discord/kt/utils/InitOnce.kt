package discord.kt.utils

import kotlin.reflect.KProperty

class InitOnce<T: Any>(private val name: String) {
    private var value: T? = null

    fun initWith(value: T) {
        if (this.value != null) {
            throw IllegalStateException("Value $name has already been initialized")
        }

        this.value = value
    }

    fun isInitialized(): Boolean = this.value != null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        value ?: throw IllegalStateException("Value $name has not yet been initialized")
}