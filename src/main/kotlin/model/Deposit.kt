package server.model


import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

data class Deposit(
    val depositId: Int,
    val accountId: Int,
    val depositAmount: Double,
    val depositType: String,  // E.g., "cash", "check", etc.
    val status: String,       // E.g., "completed", "pending"
    val depositDate: String   // Store the date as String or Timestamp
)


object DepositGsonConverter {

    private val gson: Gson by lazy {
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")  // Adjust format if necessary
            .create()
    }

    // Convert Deposit object to JSON string
    fun toJson(deposit: Deposit): String? = try {
        gson.toJson(deposit)
    } catch (e: Exception) {
        e.printStackTrace()  // Use proper logging in production
        null
    }

    // Convert JSON string to Deposit object
    fun fromJson(jsonString: String): Deposit? = try {
        gson.fromJson(jsonString, Deposit::class.java)
    } catch (e: Exception) {
        e.printStackTrace()  // Use proper logging in production
        null
    }

    // Convert list of Deposit objects to JSON string
    fun toJsonList(deposits: List<Deposit>): String? = try {
        gson.toJson(deposits)
    } catch (e: Exception) {
        e.printStackTrace()  // Use proper logging in production
        null
    }

    // Convert JSON string to list of Deposit objects
    fun fromJsonList(jsonString: String): List<Deposit>? = try {
        val listType: Type = object : TypeToken<List<Deposit>>() {}.type
        gson.fromJson(jsonString, listType)
    } catch (e: Exception) {
        e.printStackTrace()  // Use proper logging in production
        null
    }
}

