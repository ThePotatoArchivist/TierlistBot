@file:JvmName("Main")

package archives.tater.bot.tierlist

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.on
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.json.Json
import java.io.File

val STATES_FILE = File("state.json")

val STATE = try {
    Json.decodeFromString<State>(STATES_FILE.readText())
} catch (_: FileNotFoundException) {
    State().also(::saveState)
}

fun saveState(states: State = STATE) {
    STATES_FILE.writeText(Json.encodeToString(states))
}

suspend fun main() {
    println(STATE)

    val dotenv = Dotenv.load()

    with (Kord(dotenv["BOT_TOKEN"])) {

        val tierlistCommand = createGlobalChatInputCommand("tierlist", "Create a tier list") {

        }

        on<ChatInputCommandInteractionCreateEvent> {
            if (interaction.command != tierlistCommand) return@on

            if (interaction.channelId in STATE.tierlists) {
                interaction.respondEphemeral {
                    content = "This channel already has a tierlist in it!"
                }
                return@on
            }

            interaction.respondPublic {
                content = "Tier list!"
            }
        }

        on<ComponentInteractionCreateEvent> {

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