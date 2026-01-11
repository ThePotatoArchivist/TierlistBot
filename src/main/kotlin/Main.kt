@file:JvmName("Main")

package archives.tater.bot.tierlist

import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
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

        val tierlistCommand = createGlobalChatInputCommand("tierlist", "Create a tier list") {
            string("name", "Name of the tier list") {
                minLength = 1
                required = true
            }
        }

        on<ChatInputCommandInteractionCreateEvent> {
            when (interaction.command.rootId) {
                tierlistCommand.id -> onTierlist()
            }
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
