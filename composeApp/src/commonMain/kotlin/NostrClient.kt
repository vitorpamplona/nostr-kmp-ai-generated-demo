import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NostrClient {
    private val _events = MutableStateFlow<List<NostrEvent>>(emptyList())
    val events: StateFlow<List<NostrEvent>> = _events

    private val client =
        HttpClient {
            install(WebSockets)
        }

    suspend fun connectAndListen() {
        client.webSocket("wss://nos.lol") {
            val subscribeMessage = """["REQ", "my-sub", {"kinds": [1], "limit": 10}]"""
            send(Frame.Text(subscribeMessage))
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val elem = Json.parseToJsonElement(frame.readText()).jsonArray
                    if (elem[0].jsonPrimitive.content == "EVENT") {
                        _events.update {
                            listOf(Json.decodeFromJsonElement<NostrEvent>(elem[2])) + it
                        }
                    }
                }
            }
        }
    }
}
