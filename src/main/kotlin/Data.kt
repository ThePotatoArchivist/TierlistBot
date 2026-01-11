
package archives.tater.bot.tierlist

import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Snowflake
import dev.kord.core.KordObject
import dev.kord.core.behavior.channel.ChannelBehavior
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val tierlists: MutableMap<Snowflake, ReferencedTierlist> = mutableMapOf(),
) {
    operator fun get(channel: ChannelBehavior) = tierlists[channel.id]!!
    operator fun set(channel: ChannelBehavior, tierlist: ReferencedTierlist) {
        tierlists[channel.id] = tierlist
    }
    operator fun contains(channel: ChannelBehavior) = channel.id in tierlists
    fun remove(channel: ChannelBehavior) = tierlists.remove(channel.id)
}

@OptIn(KordUnsafe::class, KordExperimental::class)
@Serializable
data class ReferencedTierlist(
    val channelId: Snowflake,
    val messageId: Snowflake,
    val threadId: Snowflake,
    val tierlist: Tierlist,
    var selectedEntry: String? = null,
) {
    @Transient val message = contextualMemoize<KordObject, _> { it.kord.unsafe.message(channelId, messageId) }
    @Transient val thread = contextualMemoize<KordObject, _> { it.kord.unsafe.messageChannel(threadId) }
}

@Serializable
data class Tierlist(
    val name: String,
    val tiers: MutableList<Tier> = mutableListOf(
        Tier("S", 0xff0000),
        Tier("A", 0xff5500),
        Tier("B", 0xffaa00),
        Tier("C", 0xffff00),
        Tier("D", 0x7fff00),
        Tier("F", 0x00ff00),
    ),
)

@Serializable
data class Tier(
    var name: String,
    var color: Int,
    val entries: MutableSet<String> = mutableSetOf(),
)
