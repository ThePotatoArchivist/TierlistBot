package archives.tater.bot.tierlist

fun <T, U> contextualMemoize(function: (T) -> U): context(T) () -> U {
    val cache = mutableMapOf<T, U>()
    return fun(it: T): U = cache.getOrPut(it) { function(it) }
}