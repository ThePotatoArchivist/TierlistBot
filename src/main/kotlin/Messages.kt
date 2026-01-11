package archives.tater.bot.tierlist

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed

suspend fun createThread(name: String, tierlist: Tierlist, channel: MessageChannelBehavior, messageId: Snowflake): ReferencedTierlist {
    val thread = (channel.fetchChannel() as? TextChannel)?.startPublicThreadWithMessage(
        messageId, name
    ) ?: channel

    return ReferencedTierlist(channel.id, messageId, thread.id, tierlist)
}

fun MessageBuilder.initTierList(tierlist: Tierlist) {
    for (tier in tierlist.tiers) {
        embed {
            title = tier.name
            color = Color(tier.color)
            description = tier.entries.joinToString(" ")
        }
    }
    actionRow {
        interactionButton(ButtonStyle.Success, "add") {
            label = "Add"
        }
        interactionButton(ButtonStyle.Secondary, "move") {
            label = "Move"
        }
        interactionButton(ButtonStyle.Danger, "done") {
            label = "Done"
        }
    }
}

suspend fun ComponentInteractionCreateEvent.onAdd() {
    interaction.modal("Add Entry", "add") {
        actionRow {
            textInput(TextInputStyle.Short, "name", "Name") {
                required = true
            }
        }
    }
}

suspend fun ModalSubmitInteractionCreateEvent.onAdd() {
    interaction.deferPublicMessageUpdate()
    val tierlist = STATE[interaction.channel]
    val entry = TierEntry(interaction.textInputs["name"]?.value ?: return, "")
    val tier = tierlist.tierlist.tiers.first()
    tier.entries.add(entry)
    saveState()
    tierlist.message().edit {
        initTierList(tierlist.tierlist)
    }
    tierlist.thread().createMessage {
        content = "**${interaction.user.displayName}** added **$entry** to **${tier.name}**"
    }
}

fun ActionRowBuilder.entriesSelection(customId: String, tierlist: Tierlist) {
    stringSelect(customId) {
        val entries = tierlist.tiers.flatMap { it.entries }
        if (entries.isEmpty()) {
            option("none", "none")
            disabled = true
        } else
            for (entry in entries) {
                option(entry.name, entry.name) {
                    emoji = DiscordPartialEmoji(name = entry.icon)
                }
            }
    }
}

fun ActionRowBuilder.tiersSelection(customId: String, tierlist: Tierlist) {
    stringSelect(customId) {
        tierlist.tiers.forEachIndexed { index, tier ->
            option(tier.name, index.toString())
        }
    }
}
