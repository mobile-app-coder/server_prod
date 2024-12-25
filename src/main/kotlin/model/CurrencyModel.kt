package server.models

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

data class Currency(
    val currencyId: Int,
    val code: String,
    val name: String,
    val symbol: String,
    val exchangeRate: Double,
    val status: String
)

object CurrencyGsonConverter {

    private val gson: Gson by lazy {
        GsonBuilder()
            .create()
    }

    // Converts a single Currency object to JSON
    fun toJson(currency: Currency): String? = try {
        gson.toJson(currency)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }

    // Converts a JSON string to a single Currency object
    fun fromJson(jsonString: String): Currency? = try {
        gson.fromJson(jsonString, Currency::class.java)
    } catch (e: JsonSyntaxException) {
        e.printStackTrace() // Use proper logging in production
        null
    }

    // Converts a list of Currency objects to JSON
    fun toJsonList(currencies: List<Currency>): String? = try {
        gson.toJson(currencies)
    } catch (e: Exception) {
        e.printStackTrace() // Use proper logging in production
        null
    }

    // Converts a JSON string to a list of Currency objects
    fun fromJsonList(jsonString: String): List<Currency>? = try {
        val listType = object : TypeToken<List<Currency>>() {}.type
        gson.fromJson(jsonString, listType)
    } catch (e: JsonSyntaxException) {
        e.printStackTrace() // Use proper logging in production
        null
    }
}