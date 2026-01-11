package archives.tater.bot.tierlist

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.cache.data.toData
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.entity.interaction.SelectMenuInteraction
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
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

suspend fun ChatInputCommandInteractionCreateEvent.onTierlist() {
    if (interaction.channel in STATE) {
        interaction.respondEphemeral {
            content = "This channel already has an active tierlist in it!"
        }
        return
    }

    val name = interaction.command.strings["name"] ?: return
    val tierlist = Tierlist(name)

    val response = interaction.respondPublic {
        initTierList(tierlist)
    }
    val responseData = kord.rest.interaction.getInteractionResponse(response.applicationId, response.token).toData()

    val thread = (interaction.channel.fetchChannel() as? TextChannel)
        ?.startPublicThreadWithMessage(responseData.id, name) ?: interaction.channel
    STATE[interaction.channel] = ReferencedTierlist(interaction.channel.id, responseData.id, thread.id, tierlist)
    saveState()
}

fun MessageBuilder.initTierList(tierlist: ReferencedTierlist) {
    initTierList(tierlist.tierlist, tierlist.selectedTier)
}

fun MessageBuilder.initTierList(tierlist: Tierlist, selected: Tier = tierlist.tiers.first(), controls: Boolean = true) {
    embed {
        description = "# ${tierlist.name}"
    }
    for (tier in tierlist.tiers) {
        embed {
            title = tier.name
            color = Color(tier.color)
            description = tier.entries.joinToString(", ")
        }
    }
    if (!controls) {
        components = mutableListOf()
        return
    }
    actionRow {
        tiersSelection("tier", tierlist, selected)
    }
    actionRow {
        interactionButton(ButtonStyle.Success, "add") {
            label = "Add"
        }
    }
    actionRow {
        entriesSelection("move_entry", tierlist) {
            placeholder = "Move entry to tier"
        }
    }
    actionRow {
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
    val entry = interaction.textInputs["name"]?.value ?: return

    interaction.deferPublicMessageUpdate()
    val tierlist = STATE[interaction.channel]
    val tier = tierlist.selectedTier
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

suspend fun ComponentInteractionCreateEvent.onTier() {
    val tierId = (interaction as SelectMenuInteraction).values.firstOrNull() ?: return
    val tierlist = STATE[interaction.channel]
    val tier = tierlist.tierlist.tiers[tierId.toInt()]
    interaction.deferPublicMessageUpdate()
    tierlist.selectedTier = tier
    saveState()
    tierlist.message().edit {
        initTierList(tierlist)
    }
}

suspend fun ComponentInteractionCreateEvent.onMoveEntry() {
    val entry = (interaction as SelectMenuInteraction).values.firstOrNull() ?: return
    val tierlist = STATE[interaction.channel]
    val tier = tierlist.selectedTier
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

fun ActionRowBuilder.entriesSelection(customId: String, tierlist: Tierlist, selected: String? = null, builder: StringSelectBuilder.() -> Unit = {}) {
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

fun ActionRowBuilder.tiersSelection(customId: String, tierlist: Tierlist, selected: Tier? = null, builder: StringSelectBuilder.() -> Unit = {}) {
    stringSelect(customId) {
        tierlist.tiers.forEachIndexed { index, tier ->
            option(tier.name, index.toString()) {
                default = tier == selected
            }
        }
        builder()
    }
}
