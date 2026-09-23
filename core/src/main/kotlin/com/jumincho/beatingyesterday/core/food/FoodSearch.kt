package com.jumincho.beatingyesterday.core.food

/**
 * A food from the nutrition database.
 *
 * @property kcalPerServing energy per serving, or `null` when the database has no value.
 * @property servingSize the serving size exactly as the database reports it, if any.
 * @property maker the manufacturer, if any.
 */
data class FoodItem(val name: String, val kcalPerServing: Double?, val servingSize: String?, val maker: String?)

/** Outcome of a [FoodSearchService.search]. */
sealed interface FoodSearchResult {
    /** The search worked; [items] may be empty. [totalCount] counts all matches, not just [items]. */
    data class Success(val items: List<FoodItem>, val totalCount: Int) : FoodSearchResult

    /** The search failed; manual entry is still possible. */
    data class Failure(val error: FoodSearchError) : FoodSearchResult
}

/** Why a food search failed. */
sealed interface FoodSearchError {
    /** The server did not answer in time. */
    data object Timeout : FoodSearchError

    /** The server could not be reached. */
    data object Network : FoodSearchError

    /** The server answered with a non-2xx HTTP status [code]. */
    data class Http(val code: Int) : FoodSearchError

    /** The API reported an error [code] such as an invalid key or an exceeded quota. */
    data class Api(val code: String, val message: String?) : FoodSearchError

    /** The response could not be understood. */
    data object InvalidResponse : FoodSearchError
}

/** Looks up calories by food name. */
fun interface FoodSearchService {
    /** Searches for foods whose name contains [query], which must not be blank. */
    suspend fun search(query: String): FoodSearchResult
}
