package archives.tater.bot.tierlist

import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.entity.effectiveName

fun <T, U> contextualMemoize(function: (T) -> U): context(T) () -> U {
    val cache = mutableMapOf<T, U>()
    return fun(it: T): U = cache.getOrPut(it) { function(it) }
}

val User.displayName get() = if (this is Member) effectiveName else effectiveName