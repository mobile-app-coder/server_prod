package models

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

data class Account(
    val accountHolderName: String,
    val accountType: AccountType,
    val initialDeposit: Double
)

object AccountGsonConverter {

    private val gson: Gson by lazy {
        GsonBuilder()
            .create()
    }

    // Convert Account object to JSON string
    fun toJson(account: Account): String? = try {
        gson.toJson(account)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }

    // Convert JSON string to Account object
    fun fromJson(jsonString: String): Account? = try {
        gson.fromJson(jsonString, Account::class.java)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }

    // Convert a list of Account objects to JSON string
    fun toJsonList(accounts: List<Account>): String? = try {
        gson.toJson(accounts)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }

    // Convert JSON string to list of Account objects
    fun fromJsonList(jsonString: String): List<Account>? = try {
        val listType = object : TypeToken<List<Account>>() {}.type
        gson.fromJson(jsonString, listType)
    } catch (e: Exception) {
        e.printStackTrace() // Handle error properly in production
        null
    }
}