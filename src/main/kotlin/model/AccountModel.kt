package model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

data class BankAccount(
    val accountId: Int,
    val accountHolder: String,
    val accountTypeId: Int,
    val balance: Double,
    val dateCreated: String,
    val status: String,
    val currencyId: Int,
    val userId: Int
)

object AccountGsonConverter {

    private val gson: Gson by lazy {
        GsonBuilder()
            .create()
    }

    // Convert Account object to JSON string
    fun toJson(account: BankAccount): String? = try {
        gson.toJson(account)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }

    // Convert JSON string to Account object
    fun fromJson(jsonString: String): BankAccount? = try {
        gson.fromJson(jsonString, BankAccount::class.java)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }

    // Convert a list of Account objects to JSON string
    fun toJsonList(accounts: List<BankAccount>): String? = try {
        gson.toJson(accounts)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }

    // Convert JSON string to list of Account objects
    fun fromJsonList(jsonString: String): List<BankAccount>? = try {
        val listType = object : TypeToken<List<BankAccount>>() {}.type
        gson.fromJson(jsonString, listType)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }
}