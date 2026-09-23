package com.jumincho.beatingyesterday.core.food

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses `I2790` JSON responses:
 *
 * ```json
 * {"I2790": {"total_count": "2",
 *            "row": [{"DESC_KOR": "…", "SERVING_SIZE": "…", "NUTR_CONT1": "<kcal>", "MAKER_NAME": "…"}],
 *            "RESULT": {"CODE": "INFO-000", "MSG": "…"}}}
 * ```
 *
 * `INFO-000` means success and `INFO-200` means no matching data. Numeric fields are strings that
 * may be blank. Errors such as an invalid key may put `RESULT` at the top level instead.
 */
internal object I2790ResponseParser {
    private const val CODE_SUCCESS = "INFO-000"
    private const val CODE_NO_DATA = "INFO-200"

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): FoodSearchResult {
        val envelope = try {
            json.decodeFromString(Envelope.serializer(), body)
        } catch (e: SerializationException) {
            return FoodSearchResult.Failure(FoodSearchError.InvalidResponse)
        } catch (e: IllegalArgumentException) {
            return FoodSearchResult.Failure(FoodSearchError.InvalidResponse)
        }
        val service = envelope.service
        val result = service?.result ?: envelope.result
            ?: return FoodSearchResult.Failure(FoodSearchError.InvalidResponse)
        return when (result.code.trim()) {
            CODE_SUCCESS -> {
                val items = service?.rows.orEmpty().mapNotNull { it.toFoodItem() }
                val totalCount = service?.totalCount?.trim()?.toIntOrNull() ?: items.size
                FoodSearchResult.Success(items, totalCount)
            }

            CODE_NO_DATA -> FoodSearchResult.Success(emptyList(), totalCount = 0)

            else -> FoodSearchResult.Failure(
                FoodSearchError.Api(result.code.trim(), result.message?.trim()?.takeIf { it.isNotEmpty() }),
            )
        }
    }

    private fun Row.toFoodItem(): FoodItem? {
        val name = name?.trim().orEmpty()
        if (name.isEmpty()) return null
        return FoodItem(
            name = name,
            kcalPerServing = kcal?.trim()?.replace(",", "")?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 },
            servingSize = servingSize?.trim()?.takeIf { it.isNotEmpty() },
            maker = maker?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    @Serializable
    private class Envelope(
        @SerialName("I2790") val service: ServiceBody? = null,
        @SerialName("RESULT") val result: ResultBody? = null,
    )

    @Serializable
    private class ServiceBody(
        @SerialName("total_count") @Serializable(with = StringOrNumberSerializer::class) val totalCount: String? = null,
        @SerialName("row") val rows: List<Row> = emptyList(),
        @SerialName("RESULT") val result: ResultBody? = null,
    )

    @Serializable
    private class Row(
        @SerialName("DESC_KOR") @Serializable(with = StringOrNumberSerializer::class) val name: String? = null,
        @SerialName(
            "SERVING_SIZE",
        ) @Serializable(with = StringOrNumberSerializer::class) val servingSize: String? = null,
        @SerialName("NUTR_CONT1") @Serializable(with = StringOrNumberSerializer::class) val kcal: String? = null,
        @SerialName("MAKER_NAME") @Serializable(with = StringOrNumberSerializer::class) val maker: String? = null,
    )

    @Serializable
    private class ResultBody(@SerialName("CODE") val code: String, @SerialName("MSG") val message: String? = null)

    /** Accepts a JSON string or number and keeps its textual content. */
    private object StringOrNumberSerializer : KSerializer<String> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringOrNumber", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): String =
            (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonPrimitive?.content ?: decoder.decodeString()

        override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
    }
}
