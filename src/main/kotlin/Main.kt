@file:JvmName("Main")

package archives.tater.bot.tierlist

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.cache.data.toData
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.json.Json
import java.io.File

val STATES_FILE = File("state.json")

val STATE = try {
    Json.decodeFromString<State>(STATES_FILE.readText())
} catch (_: FileNotFoundException) {
    State()
}

suspend fun saveState(states: State = STATE) {
    withContext(Dispatchers.IO) {
        STATES_FILE.writeText(Json.encodeToString(states))
    }
}

suspend fun main() {
    val dotenv = Dotenv.load()

    with (Kord(dotenv["BOT_TOKEN"])) {

        val tierlistCommand = createGlobalChatInputCommand("tierlist", "Create a tier list")

        on<ChatInputCommandInteractionCreateEvent> {
            if (interaction.command.rootId != tierlistCommand.id) return@on

            if (interaction.channel in STATE) {
                interaction.respondEphemeral {
                    content = "This channel already has a tierlist in it!"
                }
                return@on
            }

            val tierlist = Tierlist()

            val response = interaction.respondPublic {
                initTierList(tierlist)
            }
            val responseData = kord.rest.interaction.getInteractionResponse(response.applicationId, response.token).toData()

            STATE[interaction.channel] = createThread("Tierlist", tierlist, interaction.channel, responseData.id)
            saveState()
        }

        on<ComponentInteractionCreateEvent> {
            when (interaction.componentId) {
                "add" -> onAdd()
                "done" -> onDone()
                "move_entry" -> onMoveEntry()
                "move_tier" -> onMoveTier()
            }
        }

        on<ModalSubmitInteractionCreateEvent> {
            when (interaction.modalId) {
                "add" -> onAdd()
            }
        }

        on<ReadyEvent> {
            editPresence {
                playing("/tierlist")
            }
            println("Logged in!")
        }

        login {
//            intents += Intent.GuildMessages + Intent.Guilds
        }
    }
}