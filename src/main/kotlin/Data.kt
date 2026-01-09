
package archives.tater.bot.tierlist

import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val tierlists: MutableMap<Snowflake, Tierlist> = mutableMapOf(),
)

@OptIn(KordUnsafe::class, KordExperimental::class)
@Serializable
data class Tierlist(
    val channelId: Snowflake,
    val messageId: Snowflake,
    val guildId: Snowflake,
    val threadId: Snowflake,
    val tiers: MutableList<Tier>,
) {
    @Transient val message = contextualMemoize<Kord, _> { it.unsafe.message(channelId, messageId) }
    @Transient val thread = contextualMemoize<Kord, _> { it.unsafe.textChannel(guildId, threadId) }
}

@Serializable
data class Tier(
    var name: String,
    var color: Int,
    val entries: MutableSet<TierEntry>,
)

@Serializable
data class TierEntry(
    val name: String,
    val icon: String,
)
