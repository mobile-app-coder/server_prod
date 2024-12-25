package server.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.*

data class AccountTransfer(
    val transferId: Int,                // Unique identifier for the transfer
    val senderAccountId: Int,           // ID of the sender's account
    val receiverAccountId: Int,         // ID of the receiver's account
    val amount: Double,                 // Transfer amount
    val timestamp: Date,                // Timestamp of the transfer
    val description: String? = null     // Optional description for the transfer
)

object AccountTransferGsonConverter {

    private val gson: Gson by lazy {
        GsonBuilder()
            // Optional Gson configuration
            // .serializeNulls() // Uncomment if you want to serialize null values as well
            .setDateFormat("yyyy-MM-dd HH:mm:ss")  // Optional: if you want to set date format
            .create()
    }

    // Convert a single AccountTransfer object to JSON
    fun toJson(accountTransfer: AccountTransfer): String? = try {
        gson.toJson(accountTransfer)
    } catch (e: Exception) {
        e.printStackTrace() // Log exception for better error handling in production
        null
    }

    // Convert a JSON string to a single AccountTransfer object
    fun fromJson(jsonString: String): AccountTransfer? = try {
        gson.fromJson(jsonString, AccountTransfer::class.java)
    } catch (e: Exception) {
        e.printStackTrace() // Log exception for better error handling in production
        null
    }

    // Convert a list of AccountTransfer objects to JSON
    fun toJsonList(accountTransfers: List<AccountTransfer>): String? = try {
        gson.toJson(accountTransfers)
    } catch (e: Exception) {
        e.printStackTrace() // Log exception for better error handling in production
        null
    }

    // Convert a JSON string to a list of AccountTransfer objects
    fun fromJsonList(jsonString: String): List<AccountTransfer>? = try {
        val listType = object : TypeToken<List<AccountTransfer>>() {}.type
        gson.fromJson(jsonString, listType)
    } catch (e: Exception) {
        e.printStackTrace() // Log exception for better error handling in production
        null
    }
}