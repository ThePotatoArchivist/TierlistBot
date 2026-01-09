@file:JvmName("Main")

import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import io.github.cdimascio.dotenv.Dotenv


suspend fun main() {
    val dotenv = Dotenv.load()

    with (Kord(dotenv["BOT_TOKEN"])) {

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