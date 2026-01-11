package archives.tater.bot.tierlist

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.StringSelectBuilder
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun createThread(name: String, tierlist: Tierlist, channel: MessageChannelBehavior, messageId: Snowflake): ReferencedTierlist {
    val thread = (channel.fetchChannel() as? TextChannel)?.startPublicThreadWithMessage(
        messageId, name
    ) ?: channel

    return ReferencedTierlist(channel.id, messageId, thread.id, tierlist)
}

fun MessageBuilder.initTierList(tierlist: ReferencedTierlist) {
    initTierList(tierlist.tierlist, tierlist.selectedEntry)
}

fun MessageBuilder.initTierList(tierlist: Tierlist, selected: String? = null, controls: Boolean = true) {
    for (tier in tierlist.tiers) {
        embed {
            title = tier.name
            color = Color(tier.color)
            description = tier.entries.joinToString("\n")
        }
    }
    if (!controls) {
        components = mutableListOf()
        return
    }
    actionRow {
        interactionButton(ButtonStyle.Success, "add") {
            label = "Add"
        }
        interactionButton(ButtonStyle.Danger, "done") {
            label = "Done"
        }
    }
    actionRow {
        entriesSelection("move_entry", tierlist, selected) {
            placeholder = "Choose entry to move"
        }
    }
    actionRow {
        tiersSelection("move_tier", tierlist) {
            placeholder = "Move to tier"
            disabled = selected == null
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
    val entry = interaction.textInputs["name"]?.value ?: return

    interaction.deferPublicMessageUpdate()
    val tierlist = STATE[interaction.channel]
    val tier = tierlist.tierlist.tiers.first()
    tier.entries.add(entry)

    coroutineScope {
        launch {
            saveState()
        }
        launch {
            tierlist.message().edit {
                initTierList(tierlist)
            }
        }
        launch {
            tierlist.thread().createMessage {
                content = "**${interaction.user.displayName}** added **$entry** to **${tier.name}**"
            }
        }
    }
}

suspend fun ComponentInteractionCreateEvent.onMoveEntry() {
    val entry = (interaction as SelectMenuInteraction).values.firstOrNull() ?: return
    interaction.deferPublicMessageUpdate()
    val tierlist = STATE[interaction.channel]
    tierlist.selectedEntry = entry
    saveState()
    tierlist.message().edit {
        initTierList(tierlist)
    }
}

suspend fun ComponentInteractionCreateEvent.onMoveTier() {
    val tierId = (interaction as SelectMenuInteraction).values.firstOrNull() ?: return
    val tierlist = STATE[interaction.channel]
    val entry = tierlist.selectedEntry ?: return
    val tier = tierlist.tierlist.tiers[tierId.toInt()]
    interaction.deferPublicMessageUpdate()
    for (tier in tierlist.tierlist.tiers)
        tier.entries.remove(entry)
    tier.entries.add(entry)
    coroutineScope {
        launch {
            saveState()
        }
        launch {
            tierlist.message().edit {
                initTierList(tierlist)
            }
        }
        launch {
            tierlist.thread().createMessage {
                content = "**${interaction.user.displayName}** moved **$entry** to **${tier.name}**"
            }
        }
    }
}

suspend fun ComponentInteractionCreateEvent.onDone() {
    val tierlist = STATE[interaction.channel]
    interaction.deferPublicMessageUpdate()
    STATE.remove(interaction.channel)
    coroutineScope {
        launch {
            tierlist.message().edit {
                initTierList(tierlist.tierlist, controls = false)
            }
        }
        launch {
            with(tierlist.thread()) {
                createMessage {
                    content = "**${interaction.user.displayName}** finalized the list"
                }
                (fetchChannel() as? ThreadChannel)?.leave()
            }
        }
        launch {
            saveState()
        }
    }
}

fun ActionRowBuilder.entriesSelection(customId: String, tierlist: Tierlist, selected: String?, builder: StringSelectBuilder.() -> Unit = {}) {
    stringSelect(customId) {
        val entries = tierlist.tiers.flatMap { it.entries }
        if (entries.isEmpty()) {
            option("none", "none")
            disabled = true
        } else
            for (entry in entries) {
                option(entry, entry) {
                    if (entry == selected)
                        default = true
                }
            }
        builder()
    }
}

fun ActionRowBuilder.tiersSelection(customId: String, tierlist: Tierlist, builder: StringSelectBuilder.() -> Unit = {}) {
    stringSelect(customId) {
        tierlist.tiers.forEachIndexed { index, tier ->
            option(tier.name, index.toString())
        }
        builder()
    }
}
