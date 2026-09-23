package com.jumincho.beatingyesterday.core.food

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** All JSON here is synthetic, shaped like the documented `I2790` responses. */
class I2790ResponseParserTest {

    @Test
    fun `parses rows and the total count`() {
        val body = """
            {"I2790": {
              "total_count": "42",
              "row": [
                {"DESC_KOR": "Test Stew", "SERVING_SIZE": "300", "NUTR_CONT1": "412.5", "MAKER_NAME": "Example Foods"},
                {"DESC_KOR": "Test Rice", "SERVING_SIZE": "210", "NUTR_CONT1": "310", "MAKER_NAME": ""}
              ],
              "RESULT": {"CODE": "INFO-000", "MSG": "OK"}
            }}
        """.trimIndent()

        assertEquals(
            FoodSearchResult.Success(
                items = listOf(
                    FoodItem("Test Stew", kcalPerServing = 412.5, servingSize = "300", maker = "Example Foods"),
                    FoodItem("Test Rice", kcalPerServing = 310.0, servingSize = "210", maker = null),
                ),
                totalCount = 42,
            ),
            I2790ResponseParser.parse(body),
        )
    }

    @Test
    fun `blank or missing numbers become null and nameless rows are skipped`() {
        val body = """
            {"I2790": {
              "total_count": "",
              "row": [
                {"DESC_KOR": "Test Soup", "SERVING_SIZE": " ", "NUTR_CONT1": ""},
                {"DESC_KOR": "Test Tea", "NUTR_CONT1": "n/a"},
                {"DESC_KOR": "  ", "NUTR_CONT1": "100"}
              ],
              "RESULT": {"CODE": "INFO-000", "MSG": "OK"}
            }}
        """.trimIndent()

        assertEquals(
            FoodSearchResult.Success(
                items = listOf(
                    FoodItem("Test Soup", kcalPerServing = null, servingSize = null, maker = null),
                    FoodItem("Test Tea", kcalPerServing = null, servingSize = null, maker = null),
                ),
                totalCount = 2,
            ),
            I2790ResponseParser.parse(body),
        )
    }

    @Test
    fun `accepts numbers that are not quoted`() {
        val body = """
            {"I2790": {"total_count": 1, "row": [{"DESC_KOR": "Test Bar", "NUTR_CONT1": 95}],
                       "RESULT": {"CODE": "INFO-000", "MSG": "OK"}}}
        """.trimIndent()

        assertEquals(
            FoodSearchResult.Success(listOf(FoodItem("Test Bar", 95.0, null, null)), totalCount = 1),
            I2790ResponseParser.parse(body),
        )
    }

    @Test
    fun `INFO-200 means no matching food`() {
        val body = """{"I2790": {"total_count": "0", "RESULT": {"CODE": "INFO-200", "MSG": "No data"}}}"""

        assertEquals(FoodSearchResult.Success(emptyList(), totalCount = 0), I2790ResponseParser.parse(body))
    }

    @Test
    fun `other result codes are API errors, also when RESULT is at the top level`() {
        val body = """{"RESULT": {"CODE": "INFO-100", "MSG": "Invalid key"}}"""

        assertEquals(
            FoodSearchResult.Failure(FoodSearchError.Api("INFO-100", "Invalid key")),
            I2790ResponseParser.parse(body),
        )
    }

    @Test
    fun `bodies that are not the expected JSON are invalid`() {
        val invalid = FoodSearchResult.Failure(FoodSearchError.InvalidResponse)

        assertEquals(invalid, I2790ResponseParser.parse("<html>Service unavailable</html>"))
        assertEquals(invalid, I2790ResponseParser.parse("""{"I2790": {"row": []}}"""))
        assertEquals(
            invalid,
            I2790ResponseParser.parse("""{"I2790": {"row": [{"DESC_KOR": ["x"]}], "RESULT": {"CODE": "INFO-000"}}}"""),
        )
    }
}
